package cn.blockforge.generated.mode539c988.item;

import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public final class BioethicistSpawnItem extends Item {
    public BioethicistSpawnItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Vec3 spawnPosition = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        Villager villager = EntityType.VILLAGER.create(serverLevel);
        if (villager == null) {
            return InteractionResult.FAIL;
        }
        villager.moveTo(spawnPosition.x, spawnPosition.y, spawnPosition.z,
                context.getLevel().getRandom().nextFloat() * 360.0F, 0.0F);

        villager.getPersistentData().putBoolean(TrainEntity.BIOETHICIST_TAG, true);
        villager.setCustomName(Component.translatable("entity.mod_e539c988.bioethicist"));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        serverLevel.addFreshEntity(villager);

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
