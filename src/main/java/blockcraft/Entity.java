package blockcraft;

/**
 * Base interface for things that exist in the world and can be updated and drawn.
 * - "Entity" is a common game-dev abstraction.
 * - Player and Mob are both entities.
 */
public interface Entity {
    float x();
    float y();

    void setPos(float x, float y);

    /** Update per frame (dt = seconds since last frame). */
    void update(World world, float dt);

    /** Draw color tint (simple for now). */
    EntityColor color();
}
