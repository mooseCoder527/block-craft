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

import java.util.Locale;

public final class BlockCraftGame extends ApplicationAdapter {

    private static final int TILE_PX = 24;
    private static final float BASE_MINE_TIME = 0.45f;
    private static final float ATTACK_COOLDOWN = 0.28f;
    private static final float MESSAGE_DURATION = 2.6f;

    private SpriteBatch batch;
    private Texture white;
    private BitmapFont font;
    private OrthographicCamera camera;

    private GameState state;
    private boolean showHelp = true;
    private boolean storeOpen = false;
    private int storeSelection = 0;
    private float attackCooldownLeft = 0f;

    private int miningTileX = Integer.MIN_VALUE;
    private int miningTileY = Integer.MIN_VALUE;
    private float miningProgress = 0f;
    private String statusMessage = "Mine gold, then press E near the merchant to buy weapons and tools.";
    private float statusMessageTimer = 5f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        white = makeWhiteTex();
        font = new BitmapFont();
        font.getData().setScale(1.2f);

        camera = new OrthographicCamera();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        state = new GameState(220, 120, 1337L);

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    if (storeOpen) {
                        closeStore();
                    } else {
                        Gdx.app.exit();
                    }
                }
                if (keycode == Input.Keys.H) showHelp = !showHelp;
                if (keycode == Input.Keys.F5) {
                    SaveSystem.save("savegame.bc2d", state);
                    setStatus("Game saved.");
                }
                if (keycode == Input.Keys.F9) {
                    GameState loaded = SaveSystem.load("savegame.bc2d");
                    if (loaded != null) {
                        state = loaded;
                        closeStore();
                        resetMining();
                        setStatus("Save loaded.");
                    } else {
                        setStatus("No save file found.");
                    }
                }
                if (keycode == Input.Keys.E) {
                    toggleStoreIfNearby();
                    return true;
                }
                if (storeOpen) {
                    return handleStoreKeyDown(keycode);
                }
                if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_6) {
                    state.setSelectedIndex(keycode - Input.Keys.NUM_1);
                }
                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (storeOpen) {
                    int len = state.store.getCatalog().length;
                    storeSelection = Math.floorMod(storeSelection + (amountY > 0 ? 1 : -1), len);
                    return true;
                }
                int next = state.selectedIndex + (amountY > 0 ? 1 : -1);
                state.setSelectedIndex(next);
                return true;
            }
        });
    }

    private boolean handleStoreKeyDown(int keycode) {
        Item[] catalog = state.store.getCatalog();
        if (keycode == Input.Keys.UP) {
            storeSelection = Math.floorMod(storeSelection - 1, catalog.length);
            return true;
        }
        if (keycode == Input.Keys.DOWN) {
            storeSelection = Math.floorMod(storeSelection + 1, catalog.length);
            return true;
        }
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
            buySelectedStoreItem();
            return true;
        }
        if (keycode >= Input.Keys.NUM_1 && keycode < Input.Keys.NUM_1 + catalog.length) {
            storeSelection = keycode - Input.Keys.NUM_1;
            buySelectedStoreItem();
            return true;
        }
        return true;
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
        drawWorld();
        if (state.mob.alive()) drawEntity(state.mob);
        drawNPC(state.npc);
        drawPlayerFace(state.player);
        drawMiningProgress();
        drawMobHealth();
        batch.end();

        batch.setProjectionMatrix(camera.projection);
        batch.begin();
        drawUI();
        if (storeOpen) drawStoreOverlay();
        batch.end();
    }

    private void update(float dt) {
        attackCooldownLeft = Math.max(0f, attackCooldownLeft - dt);
        statusMessageTimer = Math.max(0f, statusMessageTimer - dt);

        if (!storeOpen) {
            float vx = 0, vy = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.W)) vy += 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) vy -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) vx -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) vx += 1;
            if (vx != 0 || vy != 0) {
                state.player.move(state.world, vx, vy, dt);
            }
        }

        state.mob.update(state.world, dt, state.rng, state.player);
        updateMining(dt);

        if (!storeOpen && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (!attackMobAtMouse()) {
                tryStartMiningAtMouse();
            }
        }
        if (!storeOpen && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            updateHeldMining();
        }
        if (!storeOpen && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            placeAtMouse();
        }
    }

    private void updateHeldMining() {
        TileHit hit = tileUnderMouse();
        if (hit == null) {
            resetMining();
            return;
        }
        if (hit.tx != miningTileX || hit.ty != miningTileY) {
            tryStartMining(hit.tx, hit.ty);
        }
    }

    private void tryStartMiningAtMouse() {
        TileHit hit = tileUnderMouse();
        if (hit == null) {
            resetMining();
            return;
        }
        tryStartMining(hit.tx, hit.ty);
    }

    private void tryStartMining(int tx, int ty) {
        TileType tileType = state.world.get(tx, ty);
        if (tileType == TileType.AIR || !tileType.mineable) {
            resetMining();
            return;
        }
        miningTileX = tx;
        miningTileY = ty;
        miningProgress = 0f;
    }

    private void updateMining(float dt) {
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)) return;
        if (miningTileX == Integer.MIN_VALUE) return;
        if (storeOpen) {
            resetMining();
            return;
        }

        TileHit hit = tileUnderMouse();
        if (hit == null || hit.tx != miningTileX || hit.ty != miningTileY) {
            resetMining();
            return;
        }

        TileType tileType = state.world.get(miningTileX, miningTileY);
        if (tileType == TileType.AIR || !tileType.mineable) {
            resetMining();
            return;
        }

        float requiredTime = mineDuration(tileType);
        miningProgress += dt;
        if (miningProgress >= requiredTime) {
            state.world.set(miningTileX, miningTileY, TileType.AIR);
            state.player.inventory().add(tileType, 1);
            setStatus("Mined " + titleCase(tileType.name()) + ".");
            resetMining();
        }
    }

    private float mineDuration(TileType tileType) {
        float hardness = switch (tileType) {
            case STONE -> 1.7f;
            case GOLD -> 2.0f;
            case LOG, PLANKS -> 1.3f;
            case GRASS, DIRT -> 1.0f;
            default -> 1.1f;
        };
        float multiplier = Math.max(1f, state.player.miningSpeedMultiplier(tileType));
        return (BASE_MINE_TIME * hardness) / multiplier;
    }

    private boolean attackMobAtMouse() {
        if (!state.mob.alive()) return false;
        if (attackCooldownLeft > 0f) return false;

        TileHit hit = tileUnderMouse();
        if (hit == null) return false;

        float mx = hit.tx + 0.5f;
        float my = hit.ty + 0.5f;
        float dx = state.mob.x() - mx;
        float dy = state.mob.y() - my;
        if ((dx * dx + dy * dy) > 0.55f * 0.55f) return false;

        int damage = state.player.weaponDamage();
        attackCooldownLeft = ATTACK_COOLDOWN;
        boolean died = state.mob.takeDamage(damage);
        setStatus("Hit mob with " + state.player.equippedWeapon().name + " for " + damage + ".");
        if (died) {
            state.player.inventory().add(TileType.GOLD, 2);
            setStatus("Mob defeated. +2 gold.");
            state.mob.respawn(state.world, state.rng, state.player);
        }
        return true;
    }

    private void placeAtMouse() {
        TileHit hit = tileUnderMouse();
        if (hit == null) return;

        TileType cur = state.world.get(hit.tx, hit.ty);
        if (cur != TileType.AIR) return;

        if (Math.abs(state.npc.x() - (hit.tx + 0.5f)) < 0.55f && Math.abs(state.npc.y() - (hit.ty + 0.5f)) < 0.55f) {
            setStatus("Cannot place blocks on the merchant.");
            return;
        }

        TileType place = state.selectedTile();
        if (place == TileType.AIR) return;

        if (!state.player.inventory().take(place, 1)) return;
        state.world.set(hit.tx, hit.ty, place);
    }

    private void toggleStoreIfNearby() {
        if (!playerNearStore()) {
            setStatus("Move closer to the merchant to open the store.");
            return;
        }
        storeOpen = !storeOpen;
        resetMining();
        if (storeOpen) setStatus("Store opened.");
    }

    private boolean playerNearStore() {
        return state.player.canReach(state.npc.x(), state.npc.y()) &&
                distanceSq(state.player.x(), state.player.y(), state.npc.x(), state.npc.y()) <= 2.25f * 2.25f;
    }

    private void closeStore() {
        storeOpen = false;
    }

    private void buySelectedStoreItem() {
        Item item = state.store.getCatalog()[storeSelection];
        StorePurchaseResult result = state.store.buyItem(item , state.player);
        setStatus(result.message());
    }

    private void resetMining() {
        miningTileX = Integer.MIN_VALUE;
        miningTileY = Integer.MIN_VALUE;
        miningProgress = 0f;
    }

    private void setStatus(String message) {
        statusMessage = message;
        statusMessageTimer = MESSAGE_DURATION;
    }

    private TileHit tileUnderMouse() {
        Vector3 v = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(v);

        int tx = MathUtils.floor(v.x / TILE_PX);
        int ty = MathUtils.floor(v.y / TILE_PX);

        if (!state.player.canReach(tx, ty)) return null;

        return new TileHit(tx, ty);
    }

    private void drawWorld() {
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
                    float shade = ((x + y) & 1) == 0 ? 0.96f : 1.00f;
                    batch.setColor(c.r * shade, c.g * shade, c.b * shade, 1f);
                }

                batch.draw(white, x * TILE_PX, y * TILE_PX, TILE_PX, TILE_PX);

                batch.setColor(0, 0, 0, 0.12f);
                batch.draw(white, x * TILE_PX, y * TILE_PX, TILE_PX, 2);
                batch.draw(white, x * TILE_PX, y * TILE_PX, 2, TILE_PX);
                batch.setColor(1, 1, 1, 1);
            }
        }

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

    private void drawNPC(NPC npc) {
        float px = (npc.x() - 0.5f) * TILE_PX;
        float py = (npc.y() - 0.5f) * TILE_PX;

        batch.setColor(0.55f, 0.35f, 0.12f, 1f);
        batch.draw(white, px + 4, py + 2, TILE_PX - 8, TILE_PX - 4);
        batch.setColor(0.95f, 0.84f, 0.62f, 1f);
        batch.draw(white, px + 7, py + 13, TILE_PX - 14, 8);
        batch.setColor(0.15f, 0.15f, 0.15f, 1f);
        batch.draw(white, px + 9, py + 16, 3, 3);
        batch.draw(white, px + 14, py + 16, 3, 3);
        batch.setColor(1f, 0.86f, 0.2f, 1f);
        batch.draw(white, px + 3, py + TILE_PX - 2, TILE_PX - 6, 3);
        batch.setColor(1, 1, 1, 1);
    }

    private void drawPlayerFace(Player p) {
        float px = (p.x() - 0.5f) * TILE_PX;
        float py = (p.y() - 0.5f) * TILE_PX;

        Color c = p.color().color;
        batch.setColor(c.r, c.g, c.b, 1f);
        batch.draw(white, px + 4, py + 3, TILE_PX - 8, TILE_PX - 6);

        batch.setColor(0.12f, 0.12f, 0.12f, 1f);
        batch.draw(white, px + 8, py + 14, 4, 4);
        batch.draw(white, px + 14, py + 14, 4, 4);

        Color itemColor = state.player.equippedWeapon().color;
        batch.setColor(itemColor.r, itemColor.g, itemColor.b, 1f);
        batch.draw(white, px + 18, py + 7, 4, 10);

        batch.setColor(1, 1, 1, 1);
    }

    private void drawMiningProgress() {
        if (miningTileX == Integer.MIN_VALUE) return;
        TileType tileType = state.world.get(miningTileX, miningTileY);
        if (tileType == TileType.AIR) return;

        float ratio = Math.min(1f, miningProgress / mineDuration(tileType));
        float x = miningTileX * TILE_PX + 2;
        float y = miningTileY * TILE_PX + 2;

        batch.setColor(0, 0, 0, 0.45f);
        batch.draw(white, x, y, TILE_PX - 4, 4);
        batch.setColor(1f, 0.9f, 0.2f, 1f);
        batch.draw(white, x, y, (TILE_PX - 4) * ratio, 4);
        batch.setColor(1, 1, 1, 1);
    }

    private void drawMobHealth() {
        if (!state.mob.alive()) return;
        float x = (state.mob.x() - 0.5f) * TILE_PX;
        float y = (state.mob.y() + 0.32f) * TILE_PX;
        float ratio = state.mob.hp() / (float) state.mob.maxHp();

        batch.setColor(0, 0, 0, 0.5f);
        batch.draw(white, x + 2, y, TILE_PX - 4, 4);
        batch.setColor(0.9f, 0.2f, 0.2f, 1f);
        batch.draw(white, x + 2, y, (TILE_PX - 4) * ratio, 4);
        batch.setColor(1, 1, 1, 1);
    }

    private void drawUI() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        batch.setColor(0f, 0f, 0f, 0.32f);
        batch.draw(white, 10, h - 185, 560, 175);
        batch.setColor(1, 1, 1, 1);

        font.draw(batch, "BlockCraft 2D  |  WASD move  |  Hold LMB mine / attack  |  RMB place  |  E store  |  F5/F9 save/load", 20, h - 25);
        font.draw(batch, "Gold: " + state.player.inventory().get(TileType.GOLD)
                        + "  |  Weapon: " + state.player.equippedWeapon().name
                        + "  |  Tool: " + state.player.equippedTool().name,
                20, h - 55);

        float barY = 20;
        float barX = (w - (state.hotbar.length * 64)) / 4f;
        for (int i = 0; i < state.hotbar.length; i++) {
            TileType t = state.hotbar[i];
            float x = barX + i * 64;

            batch.setColor(0, 0, 0, 0.55f);
            batch.draw(white, x, barY, 58, 58);

            batch.setColor(t.color.r, t.color.g, t.color.b, 1f);
            batch.draw(white, x + 8, barY + 8, 42, 42);

            if (i == state.selectedIndex) {
                batch.setColor(1f, 1f, 1f, 0.9f);
                batch.draw(white, x, barY + 56, 58, 2);
                batch.draw(white, x, barY, 58, 2);
                batch.draw(white, x, barY, 2, 58);
                batch.draw(white, x + 56, barY, 2, 58);
            }

            batch.setColor(1, 1, 1, 1);
            int count = state.player.inventory().get(t);
            font.draw(batch, "" + (i + 1), x + 4, barY + 14);
            font.draw(batch, "x" + count, x + 24, barY + 14);
        }

        if (statusMessageTimer > 0f && statusMessage != null) {
            batch.setColor(0, 0, 0, 0.52f);
            batch.draw(white, 10, h - 225, 520, 28);
            batch.setColor(1, 1, 1, 1);
            font.draw(batch, statusMessage, 20, h - 206);
        }

        if (showHelp) {
            float boxW = 520;
            float boxH = 210;
            float bx = 10;
            float by = h - 450;

            batch.setColor(0, 0, 0, 0.30f);
            batch.draw(white, bx, by, boxW, boxH);
            batch.setColor(1, 1, 1, 1);

            font.draw(batch, "Help", bx + 12, by + boxH - 12);
            font.draw(batch, "- Move: WASD", bx + 12, by + boxH - 40);
            font.draw(batch, "- Mine: hold Left Mouse on a block within reach", bx + 12, by + boxH - 62);
            font.draw(batch, "- Attack: Left Mouse on the mob tile", bx + 12, by + boxH - 84);
            font.draw(batch, "- Place: Right Mouse (uses selected block)", bx + 12, by + boxH - 106);
            font.draw(batch, "- Store: stand near merchant and press E", bx + 12, by + boxH - 128);
            font.draw(batch, "- Store buy: arrows or 1..6, Enter/Space", bx + 12, by + boxH - 150);
            font.draw(batch, "- Save/Load: F5 / F9  |  Toggle help: H  |  Quit: ESC", bx + 12, by + boxH - 172);
        }
    }

    private void drawStoreOverlay() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        Item[] items = state.store.getCatalog();

        batch.setColor(0, 0, 0, 0.70f);
        batch.draw(white, 120, 90, w - 240, h - 180);
        batch.setColor(1, 1, 1, 1);

        font.draw(batch, "Merchant Store", 145, h - 120);
        font.draw(batch, "Gold: " + state.player.inventory().get(TileType.GOLD), 145, h - 150);
        font.draw(batch, "Use Up/Down or 1..6, Enter to buy, Esc/E to close", 145, h - 180);

        float startY = h - 230;
        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
            float y = startY - i * 62;
            boolean selected = i == storeSelection;
            boolean owned = state.player.hasEquipment(item);
            boolean equipped = state.player.equippedWeapon() == item || state.player.equippedTool() == item;

            batch.setColor(selected ? 0.22f : 0.10f, selected ? 0.22f : 0.10f, selected ? 0.30f : 0.10f, 0.92f);
            batch.draw(white, 140, y - 34, w - 280, 48);

            batch.setColor(item.color.r, item.color.g, item.color.b, 1f);
            batch.draw(white, 152, y - 26, 28, 28);

            batch.setColor(1, 1, 1, 1);
            font.draw(batch, (i + 1) + ". " + item.name + "  -  " + item.price + " gold", 194, y - 6);
            font.draw(batch, item.description, 194, y - 28);
            String suffix = owned ? (equipped ? "OWNED / EQUIPPED" : "OWNED") : "BUY";
            font.draw(batch, suffix, w - 300, y - 6);
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

    private static float distanceSq(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }

    private static String titleCase(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private record TileHit(int tx, int ty) {}
}
