package blockcraft;

import java.util.Random;

/**
 * - fixed grid (later to chunks)
 * - procedural generation using a seed
 */
public final class World {
    public final int width;
    public final int height;
    private final TileType[] tiles;// row-major
    private Object rng;

    public World(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new TileType[width * height];
        fill(TileType.AIR);
    }

    public void fill(TileType t) {
        for (int i = 0; i < tiles.length; i++) tiles[i] = t;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public TileType get(int x, int y) {
        if (!inBounds(x, y)) return TileType.STONE; // outside is solid wall
        return tiles[y * width + x];
    }

    public void set(int x, int y, TileType t) {
        if (!inBounds(x, y)) return;
        tiles[y * width + x] = t;
    }

    public void generate(long seed) {
        fill(TileType.AIR);
        Random rng = new Random(seed);

        int baseGround = (int) (height * 0.62f);

        for (int x = 0; x < width; x++) {
            int bump = rng.nextInt(5) - 2; // -2..2
            int gy = clamp(baseGround + bump, 4, height - 4);

            for (int y = gy; y < height; y++) {
                int goldChance = rng.nextInt(50);
                if (y == gy) set(x, y, TileType.GRASS);
                else if (y < gy + 3)  set(x, y, TileType.DIRT);
                else if (goldChance != 1) set(x, y, TileType.STONE);
                else set(x, y, TileType.GOLD);
            }

            // water pools above ground in random low spots
            if (rng.nextFloat() < 0.05f) {
                int poolY = gy - 1;
                int poolW = 4 + rng.nextInt(8);
                for (int px = x; px < Math.min(width, x + poolW); px++) {
                    if (poolY >= 0 && get(px, poolY) == TileType.AIR) set(px, poolY, TileType.WATER);
                }
            }

            // trees
            if (rng.nextFloat() < 0.07f) {
                int ty = gy - 1;
                if (ty >= 2 && get(x, ty) == TileType.AIR && get(x, ty + 1).solid) {
                    set(x, ty, TileType.LOG);
                    if (get(x, ty - 1) == TileType.AIR) set(x, ty - 1, TileType.LOG);
                }
            }
        }

        // scattered stones above ground
        for (int i = 0; i < 60; i++) {
            int cx = rng.nextInt(width);
            int cy = rng.nextInt(baseGround - 2);
            int r = 1 + rng.nextInt(2);
            for (int y = cy - r; y <= cy + r; y++) {
                for (int x = cx - r; x <= cx + r; x++) {
                    if (inBounds(x, y) && rng.nextFloat() < 0.65f && get(x, y) == TileType.AIR) {
                        set(x, y, TileType.STONE);
                    }
                }
            }
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
