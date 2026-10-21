package cn.blockforge.generated.mode539c988.dimension;

import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/** Slow server-only setpiece pass. Does not force-load chunks in ordinary exploration. */
public final class TerminalEvents {
    private TerminalEvents() { }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().getLevel(TerminalDimension.KEY);
        if (level == null || level.players().isEmpty()) return;
        long time = level.getGameTime();
        if (time % 60 != 0) return;
        TerminalWorldData data = TerminalWorldData.get(level);
        if (!data.returnPortalBuilt) TerminalStructures.ensureLobby(level, data);
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) TerminalStructures.aroundPlayer(level, data, player.blockPosition());
        }
        // PA every ~12-16 seconds at stations (previously ~2 minutes average).
        if (time % 240 == 0) {
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator()) tryStationAnnouncement(level, data, player);
            }
        }
        // A single harmless sky train passes and vanishes. No collision, drops, or smoke.
        if (time % 240 == 0) {
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && trySkyTrain(level, player)) break;
            }
        }
        TerminalBoss.tick(level, data);
        // Extremely rare passing train on ordinary rails; dilemmas handled separately.
        if (time % 1200 == 0 && level.random.nextInt(3) == 0) {
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && tryPassingTrain(level, data, player)) break;
            }
        }
    }

    private static void tryStationAnnouncement(ServerLevel level, TerminalWorldData data, ServerPlayer player) {
        for (TerminalWorldData.RailSite site : data.railSites) {
            if (Math.abs(player.getY() - 2.0D) > 12) return;
            if (Math.abs(player.getX() - site.x()) > 20 || Math.abs(player.getZ() - site.z()) > 22) continue;
            // Station roof exists at X+7, Y=7. X+6 at center Z is a sea lantern.
            BlockPos roof = new BlockPos(site.x() + 7, 7, site.z());
            if (!level.hasChunkAt(roof) || !level.getBlockState(roof).is(net.minecraft.world.level.block.Blocks.WHITE_CONCRETE)) continue;
            if (level.random.nextInt(8) == 0) return;
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                    SoundSource.AMBIENT, 0.85F, 1.4F);
            int number = level.random.nextInt(4) + 1;
            player.displayClientMessage(Component.translatable(
                    "message.mod_e539c988.station_announcement." + number), true);
            return;
        }
    }

    private static boolean trySkyTrain(ServerLevel level, ServerPlayer player) {
        // One train maximum near the player. These entities live ~3 seconds only.
        AABB nearby = player.getBoundingBox().inflate(110.0D, 85.0D, 110.0D);
        if (!level.getEntitiesOfClass(TrainEntity.class, nearby, TrainEntity::isSkyTrain).isEmpty()) return false;
        boolean alongZ = level.random.nextBoolean();
        boolean forward = level.random.nextBoolean();
        double side = 5.0D + level.random.nextInt(13);
        if (level.random.nextBoolean()) side = -side;
        double yy = player.getY() + 13.0D + level.random.nextInt(10);
        double distance = 24.0D;
        Vec3 center = new Vec3(player.getX(), yy, player.getZ());
        Vec3 begin = center.add(alongZ ? side : (forward ? -distance : distance), 0.0D,
                alongZ ? (forward ? -distance : distance) : side);
        Vec3 end = center.add(alongZ ? side : (forward ? distance : -distance), 0.0D,
                alongZ ? (forward ? distance : -distance) : side);
        // Only 24 blocks either side, within ordinary view distance; no force-loading.
        if (!level.hasChunkAt(BlockPos.containing(begin)) || !level.hasChunkAt(BlockPos.containing(end))) return false;
        TrainEntity train = TrainEntity.createDilemmaTrain(level, begin, List.of(end));
        train.setSkyTrain(true);
        if (!level.addFreshEntity(train)) return false;
        level.playSound(null, BlockPos.containing(begin), SoundEvents.MINECART_RIDING,
                SoundSource.AMBIENT, 0.75F, 0.45F);
        return true;
    }

    private static boolean tryPassingTrain(ServerLevel level, TerminalWorldData data, ServerPlayer player) {
        for (TerminalWorldData.RailSite site : data.railSites) {
            if (Math.abs(player.getY() - 2.0D) > 10) continue;
            double lateral = site.alongZ() ? Math.abs(player.getX() - site.x()) : Math.abs(player.getZ() - site.z());
            double longitudinal = site.alongZ() ? Math.abs(player.getZ() - site.z()) : Math.abs(player.getX() - site.x());
            if (lateral > 16 || longitudinal > site.halfLength() - 3) continue;
            BlockPos check = new BlockPos(site.x(), 1, site.z());
            if (!level.hasChunkAt(check)) continue;
            int half = site.halfLength();
            boolean forward = level.random.nextBoolean();
            Vec3 begin = new Vec3(site.x() + 0.5, 1.12, site.z() + 0.5)
                    .add(site.alongZ() ? 0 : (forward ? -half : half), 0,
                            site.alongZ() ? (forward ? -half : half) : 0);
            Vec3 end = new Vec3(site.x() + 0.5, 1.12, site.z() + 0.5)
                    .add(site.alongZ() ? 0 : (forward ? half : -half), 0,
                            site.alongZ() ? (forward ? half : -half) : 0);
            TrainEntity train = TrainEntity.createDilemmaTrain(level, begin, List.of(end));
            level.addFreshEntity(train);
            level.playSound(null, check, SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 0.8F, 0.6F);
            return true;
        }
        return false;
    }
}
