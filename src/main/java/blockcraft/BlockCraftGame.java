package blockcraft;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public final class BlockCraftGame extends ApplicationAdapter {

    // Rendering
    private SpriteBatch batch;
    private Texture white;
    private BitmapFont font;
    private OrthographicCamera camera;

    // State
    private GameState state;
    private boolean showHelp = true;

    // Constants (rendering)
    private static final int TILE_PX = 24;

    @Override
    public void create() {
        batch = new SpriteBatch();
        white = makeWhiteTex();
        font = new BitmapFont(); // default font, readable
        font.getData().setScale(1.2f);

        camera = new OrthographicCamera();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        state = new GameState(220, 120, 1337L);

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) Gdx.app.exit();
                if (keycode == Input.Keys.H) showHelp = !showHelp;
                if (keycode == Input.Keys.F5) {
                    SaveSystem.save("savegame.bc2d", state);
                }
                if (keycode == Input.Keys.F9) {
                    GameState loaded = SaveSystem.load("savegame.bc2d");
                    if (loaded != null) state = loaded;
                }
                if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_6) {
                    state.setSelectedIndex(keycode - Input.Keys.NUM_1);
                }
                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                int len = state.hotbar.length;
                int next = state.selectedIndex + (amountY > 0 ? 1 : -1);
                state.setSelectedIndex(next);
                return true;
            }
        });
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);

        update(dt);

        Gdx.gl.glClearColor(0.07f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.position.set(state.player.x() * TILE_PX, state.player.y() * TILE_PX, 0f);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawWorld(dt);
        drawEntity(state.mob);
        drawPlayerFace(state.player);
        batch.end();

        // UI in screen space
        batch.setProjectionMatrix(camera.projection);
        batch.begin();
        drawUI();
        batch.end();
    }

    private void update(float dt) {
        // Movement input
        float vx = 0, vy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) vy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) vy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) vx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) vx += 1;

        if (vx != 0 || vy != 0) {
            state.player.move(state.world, vx, vy, dt);
        }

        // Mob update
        state.mob.update(state.world, dt, state.rng, state.player);

        // Mouse actions
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            mineAtMouse();
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            placeAtMouse();
        }
    }

    private void mineAtMouse() {
        TileHit hit = tileUnderMouse();
        if (hit == null) return;

        TileType t = state.world.get(hit.tx, hit.ty);
        if (t == TileType.AIR) return;
        if (!t.mineable) return;

        state.world.set(hit.tx, hit.ty, TileType.AIR);
        state.player.inventory().add(t, 1);
    }

    private void placeAtMouse() {
        TileHit hit = tileUnderMouse();
        if (hit == null) return;

        TileType cur = state.world.get(hit.tx, hit.ty);
        if (cur != TileType.AIR) return;

        TileType place = state.selectedTile();
        if (place == TileType.AIR) return;

        if (!state.player.inventory().take(place, 1)) return;
        state.world.set(hit.tx, hit.ty, place);
    }

    private TileHit tileUnderMouse() {
        Vector3 v = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(v);

        int tx = MathUtils.floor(v.x / TILE_PX);
        int ty = MathUtils.floor(v.y / TILE_PX);

        if (!state.player.canReach(tx, ty)) return null;

        return new TileHit(tx, ty);
    }

    private void drawWorld(float dt) {
        // visible bounds in tile coords
        float leftPx = camera.position.x - camera.viewportWidth / 2f;
        float rightPx = camera.position.x + camera.viewportWidth / 2f;
        float botPx = camera.position.y - camera.viewportHeight / 2f;
        float topPx = camera.position.y + camera.viewportHeight / 2f;

        int minX = MathUtils.floor(leftPx / TILE_PX) - 1;
        int maxX = MathUtils.floor(rightPx / TILE_PX) + 1;
        int minY = MathUtils.floor(botPx / TILE_PX) - 1;
        int maxY = MathUtils.floor(topPx / TILE_PX) + 1;

        float time = (Gdx.graphics.getFrameId() % 10_000) * (1f / 60f);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                TileType t = state.world.get(x, y);
                if (t == TileType.AIR) continue;

                Color c = t.color;

                if (t == TileType.WATER) {
                    float pulse = 0.08f * MathUtils.sin(time * 2.8f + (x * 0.7f) + (y * 0.5f));
                    batch.setColor(c.r + pulse, c.g + pulse, c.b + pulse, 1f);
                } else {
                    // subtle shading grid for depth
                    float shade = ((x + y) & 1) == 0 ? 0.96f : 1.00f;
                    batch.setColor(c.r * shade, c.g * shade, c.b * shade, 1f);
                }

                batch.draw(white, x * TILE_PX, y * TILE_PX, TILE_PX, TILE_PX);

                // outline for readability
                batch.setColor(0, 0, 0, 0.12f);
                batch.draw(white, x * TILE_PX, y * TILE_PX, TILE_PX, 2);
                batch.draw(white, x * TILE_PX, y * TILE_PX, 2, TILE_PX);
                batch.setColor(1, 1, 1, 1);
            }
        }

        // hover highlight (if in reach)
        TileHit hit = tileUnderMouse();
        if (hit != null) {
            batch.setColor(1f, 1f, 1f, 0.22f);
            batch.draw(white, hit.tx * TILE_PX, hit.ty * TILE_PX, TILE_PX, TILE_PX);
            batch.setColor(1, 1, 1, 1);
        }
    }

    private void drawEntity(Entity e) {
        float ex = (e.x() - 0.5f) * TILE_PX;
        float ey = (e.y() - 0.5f) * TILE_PX;

        Color c = e.color().color;
        batch.setColor(c.r, c.g, c.b, 1f);
        batch.draw(white, ex + 5, ey + 5, TILE_PX - 10, TILE_PX - 10);

        batch.setColor(0.1f, 0.1f, 0.1f, 1f);
        batch.draw(white, ex + 9, ey + 15, 3, 3);
        batch.draw(white, ex + 14, ey + 15, 3, 3);

        batch.setColor(1, 1, 1, 1);
    }

    private void drawPlayerFace(Player p) {
        float px = (p.x() - 0.5f) * TILE_PX;
        float py = (p.y() - 0.5f) * TILE_PX;

        // body
        Color c = p.color().color;
        batch.setColor(c.r, c.g, c.b, 1f);
        batch.draw(white, px + 4, py + 3, TILE_PX - 8, TILE_PX - 6);

        // face
        batch.setColor(0.12f, 0.12f, 0.12f, 1f);
        batch.draw(white, px + 8, py + 14, 4, 4);
        batch.draw(white, px + 14, py + 14, 4, 4);

        batch.setColor(1, 1, 1, 1);
    }

    private void drawUI() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // background panel
        batch.setColor(0f, 0f, 0f, 0.30f);
        batch.draw(white, 10, h - 160, 420, 150);
        batch.setColor(1, 1, 1, 1);

        font.draw(batch, "BlockCraft 2D  |  WASD move  |  LMB mine  RMB place  |  F5 save  F9 load  |  H help", 20, h - 25);

        // hotbar
        float barY = 20;
        float barX = (w - (state.hotbar.length * 64)) / 2f;
        for (int i = 0; i < state.hotbar.length; i++) {
            TileType t = state.hotbar[i];
            float x = barX + i * 64;

            // slot bg
            batch.setColor(0, 0, 0, 0.55f);
            batch.draw(white, x, barY, 58, 58);

            // tile color
            batch.setColor(t.color.r, t.color.g, t.color.b, 1f);
            batch.draw(white, x + 8, barY + 8, 42, 42);

            // selection border
            if (i == state.selectedIndex) {
                batch.setColor(1f, 1f, 1f, 0.9f);
                batch.draw(white, x, barY + 56, 58, 2);
                batch.draw(white, x, barY, 58, 2);
                batch.draw(white, x, barY, 2, 58);
                batch.draw(white, x + 56, barY, 2, 58);
            }

            // count + key
            batch.setColor(1, 1, 1, 1);
            int count = state.player.inventory().get(t);
            font.draw(batch, "" + (i + 1), x + 4, barY + 14);
            font.draw(batch, "x" + count, x + 28, barY + 14);
        }

        if (showHelp) {
            float boxW = 420;
            float boxH = 220;
            float bx = 10;
            float by = h - 390;

            batch.setColor(0, 0, 0, 0.30f);
            batch.draw(white, bx, by, boxW, boxH);
            batch.setColor(1, 1, 1, 1);

            font.draw(batch, "Help", bx + 12, by + boxH - 12);
            font.draw(batch, "- Move: WASD", bx + 12, by + boxH - 40);
            font.draw(batch, "- Mine: Left Mouse (within reach)", bx + 12, by + boxH - 62);
            font.draw(batch, "- Place: Right Mouse (uses selected block)", bx + 12, by + boxH - 84);
            font.draw(batch, "- Select: 1..6 or mouse wheel", bx + 12, by + boxH - 106);
            font.draw(batch, "- Save/Load: F5 / F9", bx + 12, by + boxH - 128);
            font.draw(batch, "- Toggle this help: H", bx + 12, by + boxH - 150);
            font.draw(batch, "- Quit: ESC", bx + 12, by + boxH - 172);
        }
    }

    private Texture makeWhiteTex() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    @Override
    public void dispose() {
        batch.dispose();
        white.dispose();
        font.dispose();
    }

    private record TileHit(int tx, int ty) {}
}
