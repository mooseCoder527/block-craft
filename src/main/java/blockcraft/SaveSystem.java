package blockcraft;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SaveSystem {
    private static final String FORMAT_V1 = "BC2D:1";
    private static final String FORMAT_V2 = "BC2D:2";

    private SaveSystem() {
    }

    public static void save(String name, GameState s) {
        FileHandle f = Gdx.files.local(name);

        StringBuilder sb = new StringBuilder();
        sb.append(FORMAT_V2).append('\n');
        sb.append("w=").append(s.world.width).append('\n');
        sb.append("h=").append(s.world.height).append('\n');
        sb.append("seed=").append(s.seed).append('\n');
        sb.append("player=").append(s.player.x()).append(',').append(s.player.y()).append('\n');
        sb.append("sel=").append(s.selectedIndex).append('\n');
        sb.append("inv=").append(serializeInventory(s.player.inventory().snapshot())).append('\n');
        sb.append("owned=").append(serializeItems(s.player.ownedEquipmentSnapshot())).append('\n');
        sb.append("weapon=").append(s.player.equippedWeapon().name()).append('\n');
        sb.append("tool=").append(s.player.equippedTool().name()).append('\n');
        sb.append("mob=").append(s.mob.x()).append(',').append(s.mob.y()).append(',').append(s.mob.hp()).append('\n');

        sb.append("tiles:\n");
        for (int y = 0; y < s.world.height; y++) {
            for (int x = 0; x < s.world.width; x++) {
                sb.append(toChar(s.world.get(x, y)));
            }
            sb.append('\n');
        }

        f.writeString(sb.toString(), false, "UTF-8");
    }

    public static GameState load(String name) {
        FileHandle f = Gdx.files.local(name);
        if (!f.exists()) return null;

        String[] lines = f.readString("UTF-8").split("\\R");
        if (lines.length < 6) return null;

        String format = lines[0].trim();
        boolean v2 = FORMAT_V2.equals(format);
        if (!v2 && !FORMAT_V1.equals(format)) return null;

        int w = parseInt(value(lines, "w"));
        int h = parseInt(value(lines, "h"));
        long seed = Long.parseLong(value(lines, "seed"));

        GameState s = new GameState(w, h, seed);

        String[] p = value(lines, "player").split(",");
        s.player.setPos(Float.parseFloat(p[0]), Float.parseFloat(p[1]));
        s.setSelectedIndex(parseInt(value(lines, "sel")));
        s.player.inventory().replaceWith(parseInventory(value(lines, "inv")));

        if (v2) {
            s.player.restoreOwnedEquipment(parseItems(value(lines, "owned")));
            Item weapon = parseItem(value(lines, "weapon"));
            if (weapon != null) {
                s.player.setEquippedWeapon(weapon);
            }
            Item tool = parseItem(value(lines, "tool"));
            if (tool != null) {
                s.player.setEquippedTool(tool);
            }

            String mobValue = value(lines, "mob");
            if (!mobValue.isBlank()) {
                String[] mobParts = mobValue.split(",");
                if (mobParts.length == 3) {
                    s.mob.restoreState(
                            Float.parseFloat(mobParts[0]),
                            Float.parseFloat(mobParts[1]),
                            parseInt(mobParts[2])
                    );
                }
            }
        }

        int idx = findLine(lines, "tiles:");
        if (idx < 0) return null;
        idx++;

        for (int y = 0; y < h; y++) {
            String row = lines[idx + y];
            for (int x = 0; x < w; x++) {
                s.world.set(x, y, fromChar(row.charAt(x)));
            }
        }

        return s;
    }

    private static Map<TileType, Integer> parseInventory(String raw) {
        Map<TileType, Integer> snapshot = new EnumMap<>(TileType.class);
        if (raw == null || raw.isBlank()) {
            return snapshot;
        }
        for (String part : raw.split(",")) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            TileType type = TileType.valueOf(kv[0].trim().toUpperCase(Locale.ROOT));
            int count = parseInt(kv[1]);
            if (count > 0) {
                snapshot.put(type, count);
            }
        }
        return snapshot;
    }

    private static String serializeInventory(Map<TileType, Integer> snapshot) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<TileType, Integer> entry : snapshot.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(entry.getKey().name()).append(':').append(entry.getValue());
        }
        return sb.toString();
    }

    private static Set<Item> parseItems(String raw) {
        EnumSet<Item> items = EnumSet.noneOf(Item.class);
        if (raw == null || raw.isBlank()) {
            items.add(Item.HANDS);
            return items;
        }
        for (String part : raw.split(",")) {
            Item item = parseItem(part);
            if (item != null) {
                items.add(item);
            }
        }
        items.add(Item.HANDS);
        return items;
    }

    private static String serializeItems(Set<Item> items) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Item item : items) {
            if (!first) sb.append(',');
            first = false;
            sb.append(item.name());
        }
        return sb.toString();
    }

    private static Item parseItem(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Item.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String value(String[] lines, String key) {
        String pref = key + "=";
        for (String l : lines) {
            if (l.startsWith(pref)) return l.substring(pref.length()).trim();
        }
        return "";
    }

    private static int findLine(String[] lines, String exact) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals(exact)) return i;
        }
        return -1;
    }

    private static int parseInt(String s) {
        return Integer.parseInt(s.trim());
    }

    private static char toChar(TileType t) {
        return switch (t) {
            case AIR -> '.';
            case GRASS -> 'g';
            case DIRT -> 'd';
            case STONE -> 's';
            case WATER -> 'w';
            case LOG -> 'l';
            case PLANKS -> 'p';
            case GOLD -> 'a';
        };
    }

    private static TileType fromChar(char c) {
        return switch (c) {
            case '.' -> TileType.AIR;
            case 'g' -> TileType.GRASS;
            case 'd' -> TileType.DIRT;
            case 's' -> TileType.STONE;
            case 'w' -> TileType.WATER;
            case 'l' -> TileType.LOG;
            case 'p' -> TileType.PLANKS;
            case 'a' -> TileType.GOLD;
            default -> TileType.AIR;
        };
    }
}
