package blockcraft;

import com.badlogic.gdx.math.MathUtils;

/**
 * Player entity.
 *
 * Keeps player state + behavior together:
 * - position
 * - inventory
 * - selected hotbar index
 * - movement + collision
 */
public final class Player implements Entity {

    private float x;
    private float y;

    private final Inventory inventory = new Inventory();
    private int selectedIndex = 0;

    private static final float SPEED_TILES_PER_SEC = 5.0f;
    private static final float COLLIDER_RADIUS = 0.28f; // tile units
    private static final float REACH_TILES = 4.0f;

    public Player(float startX, float startY) {
        this.x = startX;
        this.y = startY;
    }

    @Override
    public float x() { return x; }

    @Override
    public float y() { return y; }

    @Override
    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void update(World world, float dt) {
        // Player update is driven by input in BlockCraftGame.
    }

    @Override
    public EntityColor color() {
        return EntityColor.PLAYER;
    }

    public Inventory inventory() { return inventory; }

    public int selectedIndex() { return selectedIndex; }

    public void setSelectedIndex(int idx, int hotbarLen) {
        if (hotbarLen <= 0) {
            selectedIndex = 0;
            return;
        }
        selectedIndex = Math.floorMod(idx, hotbarLen);
    }

    public float speed() { return SPEED_TILES_PER_SEC; }

    public float reach() { return REACH_TILES; }

    public void move(World world, float dirX, float dirY, float dt) {
        if (dirX == 0 && dirY == 0) return;

        // normalize direction
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        dirX /= len;
        dirY /= len;

        float dx = dirX * SPEED_TILES_PER_SEC * dt;
        float dy = dirY * SPEED_TILES_PER_SEC * dt;

        // Separate axis collision
        float nx = x + dx;
        float ny = y;
        if (!collides(world, nx, ny)) x = nx;

        nx = x;
        ny = y + dy;
        if (!collides(world, nx, ny)) y = ny;
    }

    public boolean canReach(int tileX, int tileY) {
        float cx = tileX + 0.5f;
        float cy = tileY + 0.5f;
        float dx = x - cx;
        float dy = y - cy;
        return (dx * dx + dy * dy) <= (REACH_TILES * REACH_TILES);
    }

    private boolean collides(World world, float px, float py) {
        int minX = MathUtils.floor(px - COLLIDER_RADIUS);
        int maxX = MathUtils.floor(px + COLLIDER_RADIUS);
        int minY = MathUtils.floor(py - COLLIDER_RADIUS);
        int maxY = MathUtils.floor(py + COLLIDER_RADIUS);

        for (int ty = minY; ty <= maxY; ty++) {
            for (int tx = minX; tx <= maxX; tx++) {
                if (world.get(tx, ty).solid) return true;
            }
        }
        return false;
    }
}
