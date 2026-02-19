package blockcraft;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Simple save/load:
 * - Stores: seed, player, selected, inventory counts, world tiles (chars)
 *
 * Format v1:
 *   BC2D:1
 *   w=<width>
 *   h=<height>
 *   seed=<seed>
 *   player=<x>,<y>
 *   sel=<index>
 *   inv=<tile>:<count>,...
 *   tiles:
 *   <row0>
 *   ...
 */
public final class SaveSystem {

    private SaveSystem() {}

    public static void save(String name, GameState s) {
        FileHandle f = Gdx.files.local(name);

        StringBuilder sb = new StringBuilder();
        sb.append("BC2D:1\n");
        sb.append("w=").append(s.world.width).append("\n");
        sb.append("h=").append(s.world.height).append("\n");
        sb.append("seed=").append(s.seed).append("\n");
        sb.append("player=").append(s.player.x()).append(",").append(s.player.y()).append("\n");
        sb.append("sel=").append(s.selectedIndex).append("\n");

        sb.append("inv=");
        boolean first = true;
        for (var e : s.player.inventory().snapshot().entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(e.getKey().name()).append(":").append(e.getValue());
        }
        sb.append("\n");

        sb.append("tiles:\n");
        for (int y = 0; y < s.world.height; y++) {
            for (int x = 0; x < s.world.width; x++) {
                sb.append(toChar(s.world.get(x, y)));
            }
            sb.append("\n");
        }

        f.writeString(sb.toString(), false, "UTF-8");
    }

    public static GameState load(String name) {
        FileHandle f = Gdx.files.local(name);
        if (!f.exists()) return null;

        String[] lines = f.readString("UTF-8").split("\\R");
        if (lines.length < 6 || !lines[0].trim().equals("BC2D:1")) return null;

        int w = parseInt(value(lines, "w"));
        int h = parseInt(value(lines, "h"));
        long seed = Long.parseLong(value(lines, "seed"));

        GameState s = new GameState(w, h, seed);

        String[] p = value(lines, "player").split(",");
        s.player.setPos(Float.parseFloat(p[0]), Float.parseFloat(p[1]));
        s.setSelectedIndex(parseInt(value(lines, "sel")));

        String inv = value(lines, "inv");
        if (!inv.isBlank()) {
            for (String part : inv.split(",")) {
                String[] kv = part.split(":");
                TileType t = TileType.valueOf(kv[0].trim());
                int c = parseInt(kv[1]);
                s.player.inventory().add(t, c);
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

    private static String value(String[] lines, String key) {
        String pref = key + "=";
        for (String l : lines) if (l.startsWith(pref)) return l.substring(pref.length()).trim();
        return "";
    }

    private static int findLine(String[] lines, String exact) {
        for (int i = 0; i < lines.length; i++) if (lines[i].trim().equals(exact)) return i;
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
            default -> TileType.AIR;
        };
    }
}
