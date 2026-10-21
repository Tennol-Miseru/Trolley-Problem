package cn.blockforge.generated.mode539c988.item;

import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Absurd one-time boss loot: a plunger which drops a giant trolley from the sky. */
public final class SupremePlungerItem extends Item {
    public SupremePlungerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() < 0.0001) flat = new Vec3(0, 0, 1);
            Vec3 location = player.position().add(flat.normalize().scale(12));
            BlockPos ground = BlockPos.containing(location);
            if (!server.hasChunkAt(ground)) return InteractionResultHolder.fail(stack);
            int y = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    ground.getX(), ground.getZ());
            double groundY = Math.max(player.getY() - 8, y);
            TrainEntity train = TrainEntity.createSkyfall(server,
                    new Vec3(location.x, groundY, location.z), groundY, player);
            if (!server.addFreshEntity(train)) return InteractionResultHolder.fail(stack);
            server.sendParticles(ParticleTypes.CLOUD, location.x, groundY + 20, location.z,
                    25, 2, 0.5, 2, 0.05);
            server.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.PLAYERS, 1.3F, 0.6F);
            player.getCooldowns().addCooldown(this, 20 * 3);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
