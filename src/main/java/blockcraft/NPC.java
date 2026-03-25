package blockcraft;

public final class NPC implements Entity {
    private final float x;
    private final float y;



    public NPC(float x, float y) {
        this.x = x;
        this.y = y;
    }



    @Override
    public float x() {
        return x;
    }



    @Override
    public float y() {
        return y;
    }



    @Override
    public void setPos(float x, float y) {
        throw new UnsupportedOperationException("StoreNpc is static");
    }

    @Override
    public void update(World world, float dt) {
// Static NPC.
    }

    @Override
    public EntityColor color() {
        return EntityColor.PLAYER;
    }
}