package cn.blockforge.generated.mode539c988.dilemma;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import cn.blockforge.generated.mode539c988.advancement.TrolleyAdvancements;
import cn.blockforge.generated.mode539c988.dimension.TerminalDimension;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;

/** Random ethics encounters, turnout status UI, and the timed trolley event. */
public final class DilemmaEvents {
    private static final int COUNTDOWN_TICKS = 300;
    private static final int MAX_SCENES = 128;
    private static final int MIN_RANDOM_DISTANCE_SQR = 190 * 190;
    private static final int MIN_COMMAND_DISTANCE_SQR = 39 * 39;

    private DilemmaEvents() { }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().equals(Level.OVERWORLD) && !TerminalDimension.is(level)) continue;
            DilemmaWorldData data = DilemmaWorldData.get(level);
            // Check actual lever POWERED state after vanilla has applied the click.
            // Polling is intentional: it also covers redstone and avoids a false UI
            // message when an interaction event is cancelled by another mod.
            for (DilemmaWorldData.Scenario scene : data.scenarios) {
                if (level.hasChunkAt(DilemmaStructures.switchPos(scene))) observeSwitch(level, scene);
            }
            if (level.getGameTime() % 20 != 0) continue;
            for (DilemmaWorldData.Scenario scene : data.scenarios) {
                if (level.hasChunkAt(scene.origin)) tickScene(level, data, scene);
            }
            if (level.dimension().equals(Level.OVERWORLD) && level.getGameTime() % 400 == 0 && data.scenarios.size() < MAX_SCENES
                    && level.random.nextInt(3) == 0) {
                for (ServerPlayer player : level.players()) {
                    if (!player.isSpectator() && trySpawnNearPlayer(level, data, player)) break;
                }
            }
        }
    }

    private static void observeSwitch(ServerLevel level, DilemmaWorldData.Scenario scene) {
        BlockPos pos = DilemmaStructures.switchPos(scene);
        if (!level.getBlockState(pos).is(Blocks.LEVER)) {
            scene.leverStateKnown = false;
            return;
        }
        boolean powered = level.getBlockState(pos).getValue(LeverBlock.POWERED);
        if (!scene.leverStateKnown) {
            scene.leverStateKnown = true;
            scene.observedLeverPowered = powered;
            return;
        }
        if (scene.observedLeverPowered == powered) return;
        scene.observedLeverPowered = powered;

        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS,
                1.4F, powered ? 1.45F : 0.7F);
        Component title = Component.translatable(powered
                ? "message.mod_e539c988.switch_on_title"
                : "message.mod_e539c988.switch_off_title");
        Component detail = scene.state == DilemmaWorldData.FINISHED
                ? Component.translatable("message.mod_e539c988.switch_finished")
                : Component.translatable(powered
                    ? "message.mod_e539c988.switch_red"
                    : "message.mod_e539c988.switch_green", scene.variant.label(powered));
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 48 * 48) continue;
            player.displayClientMessage(detail, true); // Action bar
            player.connection.send(new ClientboundSetTitlesAnimationPacket(2, 30, 8));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(detail));
        }
    }

    private static boolean trySpawnNearPlayer(ServerLevel level, DilemmaWorldData data, ServerPlayer player) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double radius = 48 + level.random.nextInt(29);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * radius);
            if (!level.hasChunkAt(new BlockPos(x, player.getBlockY(), z))) continue;
            BlockPos near = new BlockPos(x, player.getBlockY(), z);
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(level.random);
            DilemmaVariant variant = DilemmaVariant.random(level.random);
            if (spawnAt(level, data,
                    new DilemmaWorldData.Scenario(near, facing.get2DDataValue(), variant), false) != null) {
                return true;
            }
        }
        return false;
    }

    private static DilemmaWorldData.Scenario spawnAt(ServerLevel level, DilemmaWorldData data,
            DilemmaWorldData.Scenario request, boolean manual) {
        if (data.scenarios.size() >= MAX_SCENES) return null;
        int minDistance = TerminalDimension.is(level) ? 65 * 65
                : manual ? MIN_COMMAND_DISTANCE_SQR : MIN_RANDOM_DISTANCE_SQR;
        for (DilemmaWorldData.Scenario existing : data.scenarios) {
            int dx = existing.origin.getX() - request.origin.getX();
            int dz = existing.origin.getZ() - request.origin.getZ();
            if (dx * dx + dz * dz < minDistance) return null;
        }
        DilemmaWorldData.Scenario ready = DilemmaStructures.resolveSite(level, request, manual);
        if (ready == null) return null;
        DilemmaStructures.build(level, ready);
        data.scenarios.add(ready);
        data.setDirty();
        return ready;
    }

    private static void tickScene(ServerLevel level, DilemmaWorldData data,
                                  DilemmaWorldData.Scenario scene) {
        if (scene.state == DilemmaWorldData.FINISHED) return;
        if (scene.state == DilemmaWorldData.WAITING) {
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && player.distanceToSqr(
                        Vec3.atCenterOf(scene.origin)) <= 22 * 22) {
                    scene.state = DilemmaWorldData.COUNTDOWN;
                    scene.ticks = COUNTDOWN_TICKS;
                    message(level, scene, "message.mod_e539c988.dilemma_start",
                            scene.variant.displayName(), scene.variant.label(true), scene.variant.label(false));
                    level.playSound(null, scene.origin, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.3F, 0.8F);
                    data.setDirty();
                    return;
                }
            }
        } else if (scene.state == DilemmaWorldData.COUNTDOWN) {
            scene.ticks -= 20;
            if (scene.ticks == 200 || scene.ticks == 100) {
                message(level, scene, "message.mod_e539c988.dilemma_seconds", scene.ticks / 20);
            }
            if (scene.ticks <= 0) {
                boolean powered = level.getBlockState(DilemmaStructures.switchPos(scene)).is(Blocks.LEVER)
                        && level.getBlockState(DilemmaStructures.switchPos(scene)).getValue(LeverBlock.POWERED);
                scene.leftSelected = powered;
                spawnDecisionTrain(level, scene);
                scene.state = DilemmaWorldData.AFTER_TRAIN;
                scene.ticks = 80;
                message(level, scene, "message.mod_e539c988.dilemma_decision", scene.variant.label(powered));
                level.playSound(null, scene.origin, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.5F, 0.8F);
            }
            data.setDirty();
        } else if (scene.state == DilemmaWorldData.AFTER_TRAIN) {
            scene.ticks -= 20;
            if (scene.ticks <= 0) {
                // Mark finished first: one settlement and one credit per structure.
                scene.state = DilemmaWorldData.FINISHED;
                finish(level, data, scene);
            }
            data.setDirty();
        }
    }

    private static void spawnDecisionTrain(ServerLevel level, DilemmaWorldData.Scenario scene) {
        int destination = scene.leftSelected ? -4 : 4;
        Vec3 start = DilemmaStructures.point(scene, 0, -10);
        List<Vec3> path = List.of(
                DilemmaStructures.point(scene, 0, 0),
                DilemmaStructures.point(scene, destination, 0),
                DilemmaStructures.point(scene, destination, 16));
        TrainEntity train = TrainEntity.createDilemmaTrain(level, start, path);
        level.addFreshEntity(train);
    }

    private static void finish(ServerLevel level, DilemmaWorldData data, DilemmaWorldData.Scenario scene) {
        int alive = 0;
        for (java.util.UUID uuid : scene.captives) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof Mob mob && mob.isAlive()) {
                alive++;
                mob.setNoAi(false);
                mob.setCustomNameVisible(false);
            }
        }
        message(level, scene, "message.mod_e539c988.dilemma_result",
                scene.variant.displayName(), alive, scene.captives.size() - alive);
        DilemmaRewards.creditNearbyPlayers(level, data, scene, alive);
    }

    private static void message(ServerLevel level, DilemmaWorldData.Scenario scene, String key, Object... args) {
        Component text = Component.translatable(key, args);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(Vec3.atCenterOf(scene.origin)) <= 65 * 65) {
                player.sendSystemMessage(text);
            }
        }
    }

    /** Two one-time secret advancements. Only the player who dealt the fatal blow earns them. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        // Works for both naturally generated philosophers and spawn-item philosophers,
        // including those summoned in other dimensions.
        if (event.getEntity().getPersistentData().getBoolean(TrainEntity.BIOETHICIST_TAG)) {
            TrolleyAdvancements.award(killer, "silence_the_philosopher");
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD) && !TerminalDimension.is(level)) return;

        // Match the actual captive's UUID, not a generic villager/cat/wolf in the vicinity.
        // WAITING or COUNTDOWN means the scenario train has not departed yet.
        DilemmaWorldData data = DilemmaWorldData.get(level);
        for (DilemmaWorldData.Scenario scene : data.scenarios) {
            if ((scene.state == DilemmaWorldData.WAITING || scene.state == DilemmaWorldData.COUNTDOWN)
                    && scene.captives.contains(event.getEntity().getUUID())) {
                TrolleyAdvancements.award(killer, "early_answer");
                return;
            }
        }
    }

    /** /trolleyproblem spawn or /trolleyproblem spawn criminal, cat, creeper, etc. */
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        var root = Commands.literal("trolleyproblem");
        var spawn = Commands.literal("spawn").requires(source -> source.hasPermission(2))
                .executes(context -> commandSpawn(context, null));
        for (DilemmaVariant variant : DilemmaVariant.values()) {
            spawn.then(Commands.literal(variant.id).executes(context -> commandSpawn(context, variant)));
        }
        root.then(spawn);
        root.then(Commands.literal("enter").executes(context -> {
            TerminalDimension.enter(context.getSource().getPlayerOrException());
            return Command.SINGLE_SUCCESS;
        }));
        root.then(Commands.literal("leave").executes(context -> {
            TerminalDimension.leave(context.getSource().getPlayerOrException());
            return Command.SINGLE_SUCCESS;
        }));
        // Players can check their own progress without needing operator permissions.
        root.then(Commands.literal("progress").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            DilemmaRewards.showProgress(player, DilemmaWorldData.get(context.getSource().getServer().overworld()));
            return Command.SINGLE_SUCCESS;
        }));
        event.getDispatcher().register(root);
    }

    private static int commandSpawn(CommandContext<CommandSourceStack> context, DilemmaVariant choice)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        if (!level.dimension().equals(Level.OVERWORLD) && !TerminalDimension.is(level)) {
            source.sendFailure(Component.translatable("message.mod_e539c988.dilemma_overworld"));
            return 0;
        }
        DilemmaWorldData data = DilemmaWorldData.get(level);
        Direction facing = player.getDirection();
        Direction sideways = facing.getClockWise();
        for (int distance : new int[]{24, 38, 54, 14}) {
            for (int lateral : new int[]{0, -15, 15}) {
                BlockPos nearby = player.blockPosition().relative(facing, distance)
                        .relative(sideways, lateral);
                DilemmaVariant variant = choice == null ? DilemmaVariant.random(level.random) : choice;
                DilemmaWorldData.Scenario scene = spawnAt(level, data,
                        new DilemmaWorldData.Scenario(nearby, facing.get2DDataValue(), variant), true);
                if (scene != null) {
                    source.sendSuccess(() -> Component.translatable("message.mod_e539c988.dilemma_created",
                            scene.variant.displayName(), scene.origin.getX(), scene.origin.getY(), scene.origin.getZ()), true);
                    return Command.SINGLE_SUCCESS;
                }
            }
        }
        source.sendFailure(Component.translatable("message.mod_e539c988.dilemma_no_space"));
        return 0;
    }
}
