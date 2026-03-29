package blockcraft;

import java.util.Random;

/** game state */
public final class GameState {
    public final long seed;
    public final World world;
    public final Store store;
    public final NPC npc;
    public final Player player;

    public int selectedIndex = 0; // hotbar index (mirrors player selection)
    public final TileType[] hotbar = new TileType[]{
            TileType.DIRT, TileType.GRASS, TileType.STONE, TileType.LOG, TileType.PLANKS, TileType.GOLD
    };

    public final Mob mob;
    public final Random rng;

    public GameState(int w, int h, long seed) {
        this.seed = seed;
        this.world = new World(w, h);
        this.world.generate(seed);
        this.store = new Store();
        this.rng = new Random(seed ^ 0x9E3779B97F4A7C15L);

        SpawnPoint spawn = findSafeSpawn(w / 2, h / 2);
        this.player = new Player(spawn.worldX(), spawn.worldY());
        this.npc = createStoreNpcNearSpawn(spawn.tileX(), spawn.tileY());
        this.mob = createMobNearSpawn(spawn.tileX(), spawn.tileY());
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

    private SpawnPoint findSafeSpawn(int startX, int startY) {
        for (int radius = 0; radius < 40; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int x = startX + dx;
                    int y = startY + dy;
                    if (!isWalkableTile(x, y)) continue;
                    return new SpawnPoint(x, y);
                }
            }
        }
        throw new IllegalStateException("Unable to find a safe spawn tile.");
    }

    private NPC createStoreNpcNearSpawn(int spawnTileX, int spawnTileY) {
        for (int radius = 2; radius <= 12; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int tx = spawnTileX + dx;
                    int ty = spawnTileY + dy;
                    if (!isWalkableTile(tx, ty)) continue;
                    if (Math.abs(dx) + Math.abs(dy) < 3) continue;
                    return new NPC(tx + 0.5f, ty + 0.5f);
                }
            }
        }
        throw new IllegalStateException("Unable to place the store NPC near spawn.");
    }

    private Mob createMobNearSpawn(int spawnTileX, int spawnTileY) {
        for (int radius = 6; radius <= 18; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int tx = spawnTileX + dx;
                    int ty = spawnTileY + dy;
                    if (!isWalkableTile(tx, ty)) continue;
                    if (Math.abs(dx) + Math.abs(dy) < 6) continue;
                    if (Math.abs(tx + 0.5f - npc.x()) < 0.5f && Math.abs(ty + 0.5f - npc.y()) < 0.5f) continue;
                    return new Mob(tx + 0.5f, ty + 0.5f);
                }
            }
        }
        return new Mob(player.x() + 6f, player.y());
    }

    private boolean isWalkableTile(int x, int y) {
        if (!world.inBounds(x, y)) return false;
        TileType tile = world.get(x, y);
        return !tile.solid && tile != TileType.WATER;
    }

    private record SpawnPoint(int tileX, int tileY) {
        float worldX() {
            return tileX + 0.5f;
        }

        float worldY() {
            return tileY + 0.5f;
        }
    }
}
