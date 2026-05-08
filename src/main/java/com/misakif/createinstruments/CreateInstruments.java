package com.misakif.createinstruments;

import org.slf4j.Logger;

import com.misakif.createinstruments.block.DashboardBlock;
import com.misakif.createinstruments.block.DashboardBlockEntity;
import com.misakif.createinstruments.display.DashboardDisplayTarget;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.behaviour.display.DisplayTarget;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CreateInstruments.MODID)
public class CreateInstruments {

    public static final String MODID = "createinstruments";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<DashboardBlock> DASHBOARD_BLOCK =
            BLOCKS.register("dashboard", () -> new DashboardBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5f, 6.0f)
                            .noOcclusion()));

    public static final DeferredItem<BlockItem> DASHBOARD_ITEM =
            ITEMS.registerSimpleBlockItem("dashboard", DASHBOARD_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DashboardBlockEntity>> DASHBOARD_BE =
            BLOCK_ENTITY_TYPES.register("dashboard", () ->
                    BlockEntityType.Builder.of(DashboardBlockEntity::new, DASHBOARD_BLOCK.get())
                            .build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> INSTRUMENTS_TAB =
            CREATIVE_MODE_TABS.register("instruments_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createinstruments"))
                    .withTabsBefore(CreativeModeTabs.REDSTONE)
                    .icon(() -> DASHBOARD_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(DASHBOARD_ITEM.get()))
                    .build());

    public CreateInstruments(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        modContainer.registerConfig(net.neoforged.fml.ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                DisplayTarget.BY_BLOCK_ENTITY.register(DASHBOARD_BE.get(), new DashboardDisplayTarget()));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE) {
            event.accept(DASHBOARD_ITEM);
        }
    }
}
