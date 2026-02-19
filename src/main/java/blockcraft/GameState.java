package blockcraft;

import java.util.Random;

/** game state */
public final class GameState {
    public final long seed;
    public final World world;

    public final Player player;

    public int selectedIndex = 0; // hotbar index (mirrors player selection)
    public final TileType[] hotbar = new TileType[] {
            TileType.DIRT, TileType.GRASS, TileType.STONE, TileType.LOG, TileType.PLANKS, TileType.WATER
    };

    public final Mob mob;
    public final Random rng;

    public GameState(int w, int h, long seed) {
        this.seed = seed;
        this.world = new World(w, h);
        this.world.generate(seed);

        this.rng = new Random(seed ^ 0x9E3779B97F4A7C15L);

        // spawn player somewhere safe near center
        float px = (w / 2f) + 0.5f;
        float py = (h / 2f) + 0.5f;
        int sx = w / 2;
        int sy = h / 2;

        outer:
        for (int r = 0; r < 40; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int x = sx + dx;
                    int y = sy + dy;
                    if (!world.inBounds(x, y)) continue;
                    if (world.get(x, y).solid) continue;
                    if (world.get(x, y) == TileType.WATER) continue;
                    px = x + 0.5f;
                    py = y + 0.5f;
                    break outer;
                }
            }
        }

        this.player = new Player(px, py);

        // mob spawn (same as before)
        mob = new Mob(player.x() + 6f, player.y());
        syncSelectionToPlayer();
    }

    public TileType selectedTile() {
        return hotbar[Math.max(0, Math.min(hotbar.length - 1, selectedIndex))];
    }

    public void setSelectedIndex(int idx) {
        selectedIndex = Math.floorMod(idx, hotbar.length);
        syncSelectionToPlayer();
    }

    public void syncSelectionToPlayer() {
        player.setSelectedIndex(selectedIndex, hotbar.length);
    }
}
