package net.fabricmc.icmod.item;

import net.fabricmc.icmod.main;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {
    public static final Item MYTHRIL_INGOT = registerItem("mythril_ignot",
            new Item(new FabricItemSettings().group(ItemGroup.MISC)));

    private static Item registerItem(String name, Item item){
        return Registry.register(Registry.ITEM, new Identifier(main.MOD_ID, name), item);
    }

    public static void registerModItems(){
        main.LOGGER.info("Registering mod item for the " + main.MOD_ID);
    }
}
