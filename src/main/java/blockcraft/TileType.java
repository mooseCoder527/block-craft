package blockcraft;

import com.badlogic.gdx.graphics.Color;

public enum TileType {
    AIR(false, false, new Color(0,0,0,0)),
    GRASS(true, true, new Color(0.25f, 0.75f, 0.25f, 1f)),
    DIRT(true, true, new Color(0.55f, 0.37f, 0.20f, 1f)),
    STONE(true, true, new Color(0.55f, 0.55f, 0.60f, 1f)),
    WATER(false, false, new Color(0.20f, 0.45f, 0.90f, 1f)),
    LOG(true, true, new Color(0.55f, 0.30f, 0.12f, 1f)),
    PLANKS(true, true, new Color(0.80f, 0.65f, 0.35f, 1f));

    public final boolean solid;
    public final boolean mineable;
    public final Color color;

    TileType(boolean solid, boolean mineable, Color color) {
        this.solid = solid;
        this.mineable = mineable;
        this.color = color;
    }
}
