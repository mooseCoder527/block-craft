package blockcraft;

import java.util.List;

public class Store {
    private static final Item[] CATALOG = new Item[]{
            Item.WOOD_AXE,
            Item.IRON_SWORD
    };

    public static Item[] getCatalog() {
        return CATALOG;
    }

    public StorePurchaseResult buyItem(Item item , Player player){
        if (!player.inventory().take(TileType.GOLD , item.price)){
            return StorePurchaseResult.fail("you are too broke!");
        };
        if (player.hasEquipment(item)){
            return StorePurchaseResult.fail("you already own this!");
        };

        player.unlockEquipment(item);
        player.equip(item);
        return StorePurchaseResult.ok("Enjoy your new" + item.name);


    }
}
