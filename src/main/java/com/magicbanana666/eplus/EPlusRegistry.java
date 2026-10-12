package com.magicbanana666.eplus;

import com.magicbanana666.eplus.block.AdvancedTableBlock;
import com.magicbanana666.eplus.block.entity.AdvancedTableBlockEntity;
import com.magicbanana666.eplus.menu.AdvancedTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EPlusRegistry {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnchantingPlus.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnchantingPlus.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnchantingPlus.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, EnchantingPlus.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnchantingPlus.MOD_ID);

    public static final DeferredBlock<AdvancedTableBlock> ADVANCED_TABLE = BLOCKS.registerBlock("advanced_table", AdvancedTableBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().lightLevel(s -> 7).strength(5.0F, 1200.0F));
    public static final DeferredItem<BlockItem> ADVANCED_TABLE_ITEM = ITEMS.registerSimpleBlockItem(ADVANCED_TABLE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedTableBlockEntity>> ADVANCED_TABLE_BE = BLOCK_ENTITIES.register("advanced_table",
            () -> new BlockEntityType<>(AdvancedTableBlockEntity::new, ADVANCED_TABLE.get()));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedTableMenu>> ADVANCED_TABLE_MENU = MENUS.register("advanced_table",
            () -> IMenuTypeExtension.create(AdvancedTableMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("eplus", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.eplus"))
            .icon(() -> ADVANCED_TABLE_ITEM.get().getDefaultInstance())
            .displayItems((params, output) -> output.accept(ADVANCED_TABLE_ITEM.get()))
            .build());

    private EPlusRegistry() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
    }
}
