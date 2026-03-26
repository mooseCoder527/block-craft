package blockcraft;

public final class Store {
    private static final Item[] CATALOG = {
            Item.WOOD_AXE,
            Item.IRON_SWORD
    };

    public Item[] getCatalog() {
        return CATALOG;
    }

    public StorePurchaseResult buyItem(Item item, Player player) {
        if (item == null) {
            return StorePurchaseResult.fail("Invalid item.");
        }
        if (player.hasEquipment(item)) {
            return StorePurchaseResult.fail("You already own " + item.name + ".");
        }
        if (!player.inventory().take(TileType.GOLD, item.price)) {
            return StorePurchaseResult.fail("Not enough gold for " + item.name + ".");
        }

        player.unlockEquipment(item);
        player.equip(item);
        return StorePurchaseResult.ok("Purchased " + item.name + ".");
    }
}
