package cn.blockforge.generated.mode539c988.dimension;

import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Clean BOSS bars immediately, without needing the boss chunk to be ticking. */
public final class BossVisibilityEvents {
    private BossVisibilityEvents() { }

    @SubscribeEvent
    public static void playerDies(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) TrainEntity.clearBossBarsFor(player);
    }

    @SubscribeEvent
    public static void playerCloned(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer oldPlayer) TrainEntity.clearBossBarsFor(oldPlayer);
    }

    @SubscribeEvent
    public static void playerRespawns(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) TrainEntity.clearBossBarsFor(player);
    }

    @SubscribeEvent
    public static void playerChangesDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) TrainEntity.clearBossBarsFor(player);
    }

    @SubscribeEvent
    public static void playerDisconnects(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) TrainEntity.clearBossBarsFor(player);
    }
}
