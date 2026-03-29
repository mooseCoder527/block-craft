package blockcraft;

import com.badlogic.gdx.graphics.Color;

public enum Item {
    HANDS("Your Hands!", 0, ItemSlot.WEAPON, new Color(0.95f, 0.35f, 0.30f, 1f),
            1, 1f, "These are your hands! You were born with them! Please don't Sell them!"),
    WOOD_AXE("Wood Axe", 3, ItemSlot.WEAPON, new Color(0.63f, 0.42f, 0.24f, 1f),
            2, 1.35f, "A cheap starter weapon. Good first upgrade."),
    IRON_SWORD("Iron Sword", 7, ItemSlot.WEAPON, new Color(0.82f, 0.84f, 0.87f, 1f),
            3, 1.60f, "Reliable melee weapon for fighting mobs.");

    public final String name;
    public final int price;
    public final ItemSlot item_slot;
    public final Color color;
    public final int damage;
    public final float mining_multiplier;
    public final String description;

    Item( String name, int price, ItemSlot item_slot , Color color , int damage , float mining_multiplier , String description) {
                this.name = name;
                this.price = price;
                this.item_slot = item_slot;
                this.color = color;
                this.damage = damage;
                this.mining_multiplier = mining_multiplier;
                this.description = description;
    };

    public boolean boostsMining(TileType tileType) {
        return switch (this) {
            case IRON_SWORD -> tileType == TileType.STONE || tileType == TileType.GOLD;
            case WOOD_AXE -> tileType == TileType.LOG || tileType == TileType.PLANKS;
            default -> false;
        };
    }



    public float miningMultiplierFor(TileType tileType) {
        return boostsMining(tileType) ? mining_multiplier : 1.0f;
    }
}
