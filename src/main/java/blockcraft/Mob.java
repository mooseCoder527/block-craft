package blockcraft;



import java.util.Random;



public final class Mob implements Entity {
    private static final int MAX_HP = 10;



    private float x, y; // tile coordinates (center)
    private float moveCooldown = 0f;
    private int hp = MAX_HP;



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



    public int hp() {
        return hp;
    }



    public int maxHp() {
        return MAX_HP;
    }



    public boolean alive() {
        return hp > 0;
    }



    public boolean takeDamage(int amount) {
        hp = Math.max(0, hp - Math.max(0, amount));
        return hp == 0;
    }



    public void respawn(World world, Random rng, Player player) {
        hp = MAX_HP;
        moveCooldown = 0f;



        for (int i = 0; i < 400; i++) {
            int tx = rng.nextInt(world.width);
            int ty = rng.nextInt(world.height);
            if (world.get(tx, ty).solid) continue;
            if (world.get(tx, ty) == TileType.WATER) continue;
            float nx = tx + 0.5f;
            float ny = ty + 0.5f;
            if (!player.canReach(nx, ny)) {
                x = nx;
                y = ny;
                return;
            }
        }



        x = player.x() + 6f;
        y = player.y();
    }



    public void update(World world, float dt, Random rng, Player player) {
        if (!alive()) return;



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



        int currentTileX = Math.round(x - 0.5f);
        int currentTileY = Math.round(y - 0.5f);
        int nx = currentTileX + dx;
        int ny = currentTileY + dy;



        if (!world.inBounds(nx, ny)) return;
        if (world.get(nx, ny).solid) return;



        if (Math.abs(player.x() - nx) < 0.5f && Math.abs(player.y() - ny) < 0.5f) return;



        x = nx + 0.5f;
        y = ny + 0.5f;
    }
}