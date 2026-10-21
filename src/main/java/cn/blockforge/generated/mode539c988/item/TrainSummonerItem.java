package cn.blockforge.generated.mode539c988.item;

import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import cn.blockforge.generated.mode539c988.advancement.TrolleyAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class TrainSummonerItem extends Item {
    private static final String PHYSICAL_MODE_TAG = "PhysicalTrain";

    private final TrainEntity.Mode mode;
    private final boolean mountImmediately;

    public TrainSummonerItem(TrainEntity.Mode mode, boolean mountImmediately, Properties properties) {
        super(properties);
        this.mode = mode;
        this.mountImmediately = mountImmediately;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                boolean physical = !usesPhysicalMode(stack);
                CustomData.update(DataComponents.CUSTOM_DATA, stack,
                        tag -> tag.putBoolean(PHYSICAL_MODE_TAG, physical));
                player.displayClientMessage(Component.translatable(physical
                        ? "message.mod_e539c988.mode_physical"
                        : "message.mod_e539c988.mode_void"), true);
                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(),
                        SoundSource.PLAYERS, 0.8F, physical ? 1.2F : 0.8F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z);
        if (direction.lengthSqr() < 1.0E-5D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        direction = direction.normalize();

        if (level instanceof ServerLevel serverLevel) {
            TrainEntity.PhysicsMode physicsMode = usesPhysicalMode(stack)
                    ? TrainEntity.PhysicsMode.PHYSICAL : TrainEntity.PhysicsMode.VOID;
            Vec3 spawnPosition;
            if (mountImmediately) {
                spawnPosition = player.position().add(direction.scale(1.8D)).add(0.0D, 0.15D, 0.0D);
            } else if (mode == TrainEntity.Mode.WILD) {
                spawnPosition = player.position().add(direction.scale(4.0D)).add(0.0D, 0.15D, 0.0D);
            } else {
                spawnPosition = player.position().subtract(direction.scale(11.0D)).add(0.0D, 0.3D, 0.0D);
            }

            TrainEntity train = TrainEntity.create(level, mode, physicsMode, spawnPosition, direction, player);
            if (!serverLevel.addFreshEntity(train)) {
                return InteractionResultHolder.fail(stack);
            }
            if (player instanceof ServerPlayer serverPlayer && mode == TrainEntity.Mode.AUTOMATIC) {
                TrolleyAdvancements.award(serverPlayer, "classic_problem");
            }
            serverLevel.sendParticles(ParticleTypes.PORTAL, spawnPosition.x, spawnPosition.y + 1.0D, spawnPosition.z,
                    80, 2.0D, 1.3D, 2.0D, 0.35D);
            level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.PLAYERS, 1.0F, 1.35F);

            if (mountImmediately) {
                if (player.startRiding(train, true) && player instanceof ServerPlayer serverPlayer) {
                    TrolleyAdvancements.award(serverPlayer, "train_driver");
                }
                player.displayClientMessage(Component.translatable("message.mod_e539c988.driving_hint",
                        Component.translatable(physicsMode == TrainEntity.PhysicsMode.PHYSICAL
                                ? "message.mod_e539c988.mode_name_physical"
                                : "message.mod_e539c988.mode_name_void")), true);
            }
            player.getCooldowns().addCooldown(this, mode == TrainEntity.Mode.WILD ? 120 : 80);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private boolean usesPhysicalMode(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.contains(PHYSICAL_MODE_TAG)) {
            return data.copyTag().getBoolean(PHYSICAL_MODE_TAG);
        }
        return mode != TrainEntity.Mode.AUTOMATIC && mode != TrainEntity.Mode.FLING;
    }
}
