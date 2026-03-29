package blockcraft;

import com.badlogic.gdx.math.MathUtils;

import java.util.EnumSet;
import java.util.Set;

/**
 * Player entity.
 * <p>
 * Keeps player state + behavior together:
 * - position
 * - inventory
 * - selected hotbar index
 * - equipment ownership and loadout
 * - movement + collision
 */
public final class Player implements Entity {

    private float x;
    private float y;

    private final Inventory inventory = new Inventory(5);
    private int selectedIndex = 0;

    private final EnumSet<Item> ownedEquipment = EnumSet.of(Item.HANDS);
    private Item equippedWeapon = Item.HANDS;
    private Item equippedTool = Item.HANDS;

    private static final float SPEED_TILES_PER_SEC = 5.0f;
    private static final float COLLIDER_RADIUS = 0.28f; // tile units
    private static final float REACH_TILES = 4.0f;

    public Player(float startX, float startY) {
        this.x = startX;
        this.y = startY;
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
        this.x = x;
        this.y = y;
    }

    @Override
    public void update(World world, float dt) {
    }

    @Override
    public EntityColor color() {
        return EntityColor.PLAYER;
    }

    public Inventory inventory() {
        return inventory;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int idx, int hotbarLen) {
        if (hotbarLen <= 0) {
            selectedIndex = 0;
            return;
        }
        selectedIndex = Math.floorMod(idx, hotbarLen);
    }

    public float speed() {
        return SPEED_TILES_PER_SEC;
    }

    public float reach() {
        return REACH_TILES;
    }

    public int weaponDamage() {
        return Math.max(1, equippedWeapon.damage);
    }

    public Item equippedWeapon() {
        return equippedWeapon;
    }

    public Item equippedTool() {
        return equippedTool;
    }

    public boolean hasEquipment(Item item) {
        return ownedEquipment.contains(item);
    }

    public void unlockEquipment(Item item) {
        if (item == null) return;
        ownedEquipment.add(item);
    }

    public Set<Item> ownedEquipmentSnapshot() {
        return EnumSet.copyOf(ownedEquipment);
    }

    public void restoreOwnedEquipment(Set<Item> owned) {
        ownedEquipment.clear();
        ownedEquipment.add(Item.HANDS);
        if (owned != null) {
            ownedEquipment.addAll(owned);
        }

        if (!ownedEquipment.contains(equippedWeapon)) {
            equippedWeapon = Item.HANDS;
        }
        if (!ownedEquipment.contains(equippedTool)) {
            equippedTool = Item.HANDS;
        }
    }

    public void setEquippedWeapon(Item item) {
        if (item == null || item.item_slot != ItemSlot.WEAPON) return;
        if (!ownedEquipment.contains(item)) return;
        this.equippedWeapon = item;
    }

    public void setEquippedTool(Item item) {
        if (item == null || item.item_slot != ItemSlot.TOOL) return;
        if (!ownedEquipment.contains(item)) return;
        this.equippedTool = item;
    }

    public void equip(Item item) {
        if (item == null || !ownedEquipment.contains(item)) return;
        if (item.item_slot == ItemSlot.WEAPON) {
            equippedWeapon = item;
        } else {
            equippedTool = item;
        }
    }

    public float miningSpeedMultiplier(TileType tileType) {
        return Math.max(
                equippedTool.miningMultiplierFor(tileType),
                equippedWeapon.miningMultiplierFor(tileType)
        );
    }

    public void move(World world, float dirX, float dirY, float dt) {
        if (dirX == 0 && dirY == 0) return;

        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        dirX /= len;
        dirY /= len;

        float dx = dirX * SPEED_TILES_PER_SEC * dt;
        float dy = dirY * SPEED_TILES_PER_SEC * dt;

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

    public boolean canReach(float worldX, float worldY) {
        float dx = x - worldX;
        float dy = y - worldY;
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
