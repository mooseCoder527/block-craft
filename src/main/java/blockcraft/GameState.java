package blockcraft;

import java.util.Random;

/** game state */
public final class GameState {
    public final long seed;
    public final World world;
    public final Store store ;
    public final NPC npc;

    public final Player player;

    public int selectedIndex = 0; // hotbar index (mirrors player selection)
    public final TileType[] hotbar = new TileType[] {
            TileType.DIRT, TileType.GRASS, TileType.STONE, TileType.LOG, TileType.PLANKS, TileType.GOLD
    };

    public final Mob mob;
    public final Random rng;

    public GameState(int w, int h, long seed) {
        this.seed = seed;
        this.world = new World(w, h);
        this.world.generate(seed);
        this.store = new Store();
        this.npc = createStoreNpcNearSpawn();

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
    private NPC createStoreNpcNearSpawn() {
        for (int radius = 2; radius <= 12; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int tx = Math.round(player.x() - 0.5f) + dx;
                    int ty = Math.round(player.y() - 0.5f) + dy;
                    if (!world.inBounds(tx, ty)) continue;
                    if (world.get(tx, ty).solid) continue;
                    if (world.get(tx, ty) == TileType.WATER) continue;
                    if (Math.abs(dx) + Math.abs(dy) < 3) continue;
                    return new NPC(tx + 0.5f, ty + 0.5f);
                }
            }
        }
        return new NPC(player.x() + 3f, player.y());
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
