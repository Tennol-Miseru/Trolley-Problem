package cn.blockforge.generated.mode539c988.dimension;

import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import cn.blockforge.generated.mode539c988.dilemma.DilemmaStructures;
import cn.blockforge.generated.mode539c988.dilemma.DilemmaVariant;
import cn.blockforge.generated.mode539c988.dilemma.DilemmaWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Exactly one giant trolley encounter per saved White Terminal dimension. */
public final class TerminalBoss {
    public static final BlockPos ARENA = new BlockPos(0, 1, 256);
    private TerminalBoss() { }

    public static void tick(ServerLevel level, TerminalWorldData data) {
        if (data.bossDefeated) return;
        boolean playerNear = false;
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(Vec3.atCenterOf(ARENA)) < 74 * 74) {
                playerNear = true;
                break;
            }
        }
        if (!playerNear || !TerminalStructures.ensureBossArena(level, data)) return;
        if (data.bossId != null && level.getEntity(data.bossId) instanceof TrainEntity train
                && train.isBossTrain() && !train.isRemoved()) return;
        TrainEntity train = TrainEntity.createBoss(level, Vec3.atBottomCenterOf(ARENA).add(0, 0.12, 0));
        if (!level.addFreshEntity(train)) return;
        data.bossId = train.getUUID();
        data.setDirty();
        for (ServerPlayer player : level.players()) if (player.distanceToSqr(train) < 100 * 100)
            player.sendSystemMessage(Component.translatable("message.mod_e539c988.boss_appears"));
    }

    /** A real, timed, randomly chosen vanilla-style dilemma interrupts each boss stage. */
    public static BlockPos startTrial(ServerLevel level, int phase) {
        int x = phase == 1 ? -25 : 25;
        DilemmaWorldData data = DilemmaWorldData.get(level);
        DilemmaWorldData.Scenario request = new DilemmaWorldData.Scenario(
                new BlockPos(x, 1, 256), Direction.SOUTH.get2DDataValue(), DilemmaVariant.random(level.random));
        DilemmaWorldData.Scenario scene = DilemmaStructures.resolveSite(level, request, true);
        if (scene == null) return null;
        DilemmaStructures.build(level, scene);
        scene.state = DilemmaWorldData.COUNTDOWN;
        scene.ticks = 300;
        data.scenarios.add(scene);
        data.setDirty();
        for (ServerPlayer player : level.players()) if (player.distanceToSqr(Vec3.atCenterOf(ARENA)) < 95 * 95)
            player.sendSystemMessage(Component.translatable("message.mod_e539c988.boss_trial", phase,
                    scene.origin.getX(), scene.origin.getZ()));
        return scene.origin;
    }

    public static boolean trialFinished(ServerLevel level, BlockPos pos) {
        if (pos == null) return true;
        for (DilemmaWorldData.Scenario scene : DilemmaWorldData.get(level).scenarios)
            if (pos.equals(scene.origin)) return scene.state == DilemmaWorldData.FINISHED;
        return true; // World edited or experiment removed: do not soft-lock the fight.
    }

    public static void defeated(ServerLevel level, TrainEntity train) {
        TerminalWorldData data = TerminalWorldData.get(level);
        if (data.bossDefeated) return;
        data.bossDefeated = true;
        data.bossId = null;
        data.setDirty();
        for (ServerPlayer player : level.players()) if (player.distanceToSqr(train) < 110 * 110)
            player.sendSystemMessage(Component.translatable("message.mod_e539c988.boss_defeated"));
    }
}
