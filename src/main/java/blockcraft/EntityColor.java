package blockcraft;

import com.badlogic.gdx.graphics.Color;

public enum EntityColor {
    PLAYER(new Color(0.95f, 0.35f, 0.30f, 1f)),
    MOB(new Color(0.70f, 0.30f, 0.85f, 1f));

    public final Color color;

    EntityColor(Color color) {
        this.color = color;
    }
}
