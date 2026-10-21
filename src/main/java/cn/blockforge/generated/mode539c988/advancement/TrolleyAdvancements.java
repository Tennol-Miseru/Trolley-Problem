package cn.blockforge.generated.mode539c988.advancement;

import cn.blockforge.generated.mode539c988.GeneratedMod;
import cn.blockforge.generated.mode539c988.dilemma.DilemmaVariant;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Vanilla advancement tree for trolley encounters and train shenanigans. */
public final class TrolleyAdvancements {
    private TrolleyAdvancements() { }

    /** Award a one-time criterion on the server; no client packet or extra save file is needed. */
    public static void award(ServerPlayer player, String id) {
        AdvancementHolder entry = player.serverLevel().getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, id));
        if (entry == null) return;
        if (!"root".equals(id)) {
            award(player, "root");
        }
        player.getAdvancements().award(entry, "triggered");
    }

    /** Called only once per finished scene for every eligible participant. */
    public static void onCompleted(ServerPlayer player, DilemmaVariant variant,
                                   boolean choseRed, int survivors, int cycleCount) {
        award(player, "first_experiment");
        if (variant == DilemmaVariant.CLASSIC && choseRed && survivors == 5) {
            award(player, "one_for_five");
        }
        if (!choseRed && survivors == 1) {
            award(player, "five_for_one");
        }
        if (survivors == 6) {
            award(player, "third_answer");
        }
        if (variant == DilemmaVariant.PETS) {
            award(player, "cats_and_dogs");
        }
        if (cycleCount == 10) {
            award(player, "ten_trials");
        }
    }
}
