package blockcraft;

import java.util.EnumMap;
import java.util.Map;

/** hotbar-style inventory */
public final class Inventory {
    private static final EnumMap<TileType, Integer> counts = new EnumMap<>(TileType.class);

    public Inventory() {
        add(TileType.DIRT, 25);
        add(TileType.PLANKS, 12);
    }
    public Inventory(int goldCount){
        add(TileType.DIRT, 25);
        add(TileType.PLANKS, 12);
        add(TileType.GOLD, goldCount);
    }

    public static int get(TileType t) {
        return counts.getOrDefault(t, 0);
    }

    public static void add(TileType t, int amount) {
        if (amount <= 0) return;
        counts.put(t, get(t) + amount);
    }

    public static boolean take(TileType t, int amount) {
        int have = get(t);
        if (have < amount) return false;
        int left = have - amount;
        if (left == 0) counts.remove(t);
        else counts.put(t, left);
        return true;
    }

    public Map<TileType, Integer> snapshot() {
        return new EnumMap<>(counts);
    }
}
