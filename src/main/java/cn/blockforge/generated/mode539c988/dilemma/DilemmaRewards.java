package cn.blockforge.generated.mode539c988.dilemma;

import cn.blockforge.generated.mode539c988.GeneratedMod;
import cn.blockforge.generated.mode539c988.advancement.TrolleyAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Server-side, per-player cyclic rewards for completed dilemma events. */
public final class DilemmaRewards {
    private static final double REWARD_RADIUS_SQR = 32 * 32;

    private DilemmaRewards() { }

    /** Called exactly once when a scene goes from AFTER_TRAIN to FINISHED. */
    public static void creditNearbyPlayers(ServerLevel level, DilemmaWorldData data,
                                           DilemmaWorldData.Scenario scene, int survivors) {
        // One reward counter across the overworld AND white terminal.
        DilemmaWorldData rewardData = DilemmaWorldData.get(level.getServer().overworld());
        Vec3 center = Vec3.atCenterOf(scene.origin);
        for (ServerPlayer player : level.players()) {
            // Each nearby participant earns one credit. Spectators do not.
            if (player.isSpectator() || player.distanceToSqr(center) > REWARD_RADIUS_SQR) continue;
            int completed = progress(rewardData, player) + 1;
            int stored = completed == 10 ? 0 : completed;
            rewardData.rewardProgress.put(player.getUUID(), stored);
            rewardData.setDirty();
            TrolleyAdvancements.onCompleted(player, scene.variant, scene.leftSelected, survivors, completed);

            Item prize = switch (completed) {
                case 3 -> GeneratedMod.TROLLEY_STEERING_WHEEL.get();
                case 5 -> GeneratedMod.TROLLEY_LEVER.get();
                case 10 -> GeneratedMod.WILD_LEVER.get();
                default -> null;
            };
            if (prize != null) {
                ItemStack stack = new ItemStack(prize);
                Component prizeName = stack.getHoverName();
                // Inventory full? Drop the reward at the player's feet instead of losing it.
                if (!player.getInventory().add(stack)) player.drop(stack, false);
                player.sendSystemMessage(Component.translatable(
                        completed == 10
                                ? "message.mod_e539c988.dilemma_reward_cycle"
                                : "message.mod_e539c988.dilemma_reward",
                        completed, prizeName));
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 1.0F, 1.1F);
            } else {
                player.sendSystemMessage(Component.translatable(
                        "message.mod_e539c988.dilemma_progress", completed,
                        nextRewardAt(completed) - completed));
            }
        }
    }

    public static void showProgress(ServerPlayer player, DilemmaWorldData data) {
        int completed = progress(data, player);
        player.sendSystemMessage(Component.translatable(
                "message.mod_e539c988.dilemma_progress_status", completed,
                nextRewardAt(completed), nextRewardAt(completed) - completed));
    }

    private static int progress(DilemmaWorldData data, ServerPlayer player) {
        return Math.floorMod(data.rewardProgress.getOrDefault(player.getUUID(), 0), 10);
    }

    private static int nextRewardAt(int completed) {
        if (completed < 3) return 3;
        if (completed < 5) return 5;
        return 10;
    }
}
