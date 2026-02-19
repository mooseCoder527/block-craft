package blockcraft;

import java.util.Random;

public final class Mob implements Entity {
    private float x, y; // tile coordinates (center)
    private float moveCooldown = 0f;

    public Mob(float x, float y) {
        this.x = x;
        this.y = y;
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
        // AI tick is driven externally with a Random for determinism; no-op here.
    }

    @Override
    public EntityColor color() {
        return EntityColor.MOB;
    }

    public void update(World world, float dt, Random rng, Player player) {
        moveCooldown -= dt;
        if (moveCooldown > 0f) return;

        moveCooldown = 0.25f + rng.nextFloat() * 0.25f;

        int dir = rng.nextInt(4);
        int dx = 0, dy = 0;
        switch (dir) {
            case 0 -> dx = 1;
            case 1 -> dx = -1;
            case 2 -> dy = 1;
            case 3 -> dy = -1;
        }

        int nx = Math.round(x + dx);
        int ny = Math.round(y + dy);

        if (!world.inBounds(nx, ny)) return;
        if (world.get(nx, ny).solid) return;

        // don't step into the player
        if (Math.abs(player.x() - nx) < 0.5f && Math.abs(player.y() - ny) < 0.5f) return;

        x = nx;
        y = ny;
    }
}
