package cn.blockforge.generated.mode539c988;

import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import cn.blockforge.generated.mode539c988.dimension.TerminalEvents;
import cn.blockforge.generated.mode539c988.dimension.BossVisibilityEvents;
import cn.blockforge.generated.mode539c988.dimension.TrolleyPortalBlock;
import cn.blockforge.generated.mode539c988.dimension.PortalEvents;
import cn.blockforge.generated.mode539c988.dimension.WhiteTicketItem;
import cn.blockforge.generated.mode539c988.dilemma.DilemmaEvents;
import net.neoforged.neoforge.common.NeoForge;
import cn.blockforge.generated.mode539c988.item.BioethicistSpawnItem;
import cn.blockforge.generated.mode539c988.item.TrainSummonerItem;
import cn.blockforge.generated.mode539c988.item.SupremePlungerItem;
import cn.blockforge.generated.mode539c988.network.TrainInputPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "mod_e539c988";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredBlock<Block> TROLLEY_BLOCK = BLOCKS.register("trolley_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F).sound(SoundType.METAL)));
    public static final DeferredBlock<TrolleyPortalBlock> TROLLEY_PORTAL = BLOCKS.register("trolley_portal",
            () -> new TrolleyPortalBlock(BlockBehaviour.Properties.of()
                    .noCollission().noOcclusion().strength(-1.0F).lightLevel(state -> 11)));
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<BlockItem> TROLLEY_BLOCK_ITEM = ITEMS.register("trolley_block",
            () -> new BlockItem(TROLLEY_BLOCK.get(), new Item.Properties()));
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<TrainEntity>> TRAIN = ENTITY_TYPES.register(
            "train",
            () -> EntityType.Builder.<TrainEntity>of(TrainEntity::new, MobCategory.MISC)
                    .sized(3.0F, 2.35F)
                    .passengerAttachments(1.25F)
                    .clientTrackingRange(12)
                    // Two ticks are enough for 0.82-block/tick trains. Client lerp
                    // smooths visual motion between authoritative updates.
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(MOD_ID + ":train")
    );

    public static final DeferredItem<Item> TROLLEY_LEVER = ITEMS.register(
            "trolley_lever",
            () -> new TrainSummonerItem(TrainEntity.Mode.AUTOMATIC, false, new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> TROLLEY_STEERING_WHEEL = ITEMS.register(
            "trolley_steering_wheel",
            () -> new TrainSummonerItem(TrainEntity.Mode.DRIVABLE, true, new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> FLING_LEVER = ITEMS.register(
            "fling_lever",
            () -> new TrainSummonerItem(TrainEntity.Mode.FLING, false, new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> WILD_LEVER = ITEMS.register(
            "wild_lever",
            () -> new TrainSummonerItem(TrainEntity.Mode.WILD, false, new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> SUPREME_PLUNGER = ITEMS.register(
            "supreme_plunger", () -> new SupremePlungerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WHITE_TICKET = ITEMS.register(
            "white_ticket", () -> new WhiteTicketItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> BIOETHICIST_SPAWN_EGG = ITEMS.register(
            "bioethicist_spawn_egg",
            () -> new BioethicistSpawnItem(new Item.Properties().stacksTo(16))
    );

    public GeneratedMod(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener(this::addCreativeItems);
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(DilemmaEvents.class);
        NeoForge.EVENT_BUS.register(TerminalEvents.class);
        NeoForge.EVENT_BUS.register(BossVisibilityEvents.class);
        NeoForge.EVENT_BUS.register(PortalEvents.class);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToServer(
                TrainInputPayload.TYPE,
                TrainInputPayload.STREAM_CODEC,
                TrainInputPayload::handle);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TROLLEY_LEVER.get());
            event.accept(TROLLEY_STEERING_WHEEL.get());
            event.accept(WILD_LEVER.get());
            event.accept(FLING_LEVER.get());
            event.accept(WHITE_TICKET.get());
            event.accept(SUPREME_PLUNGER.get());
            event.accept(TROLLEY_BLOCK_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(BIOETHICIST_SPAWN_EGG.get());
        }
    }
}
