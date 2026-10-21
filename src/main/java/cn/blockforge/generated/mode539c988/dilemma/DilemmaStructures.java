package cn.blockforge.generated.mode539c988.dilemma;

import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** A small rail set built above the landscape, not a 20x30 level-grass requirement. */
public final class DilemmaStructures {
    private DilemmaStructures() { }

    public static BlockPos local(DilemmaWorldData.Scenario s, int right, int forward) {
        Direction f = Direction.from2DDataValue(s.facing);
        return s.origin.offset(-right * f.getStepZ() + forward * f.getStepX(), 0,
                right * f.getStepX() + forward * f.getStepZ());
    }

    public static Vec3 point(DilemmaWorldData.Scenario s, int right, int forward) {
        return Vec3.atBottomCenterOf(local(s, right, forward)).add(0, 0.12D, 0);
    }

    public static BlockPos switchPos(DilemmaWorldData.Scenario s) {
        return local(s, 2, -3);
    }

    private static List<BlockPos> footprint(DilemmaWorldData.Scenario scene) {
        List<BlockPos> positions = new ArrayList<>();
        for (int v = -11; v < 0; v++) positions.add(local(scene, 0, v));
        for (int u = -4; u <= 4; u++) positions.add(local(scene, u, 0));
        for (int v = 1; v <= 16; v++) {
            positions.add(local(scene, -4, v));
            positions.add(local(scene, 4, v));
        }
        // Short standing area next to the lever; also part of the footprint check.
        for (int u = 1; u <= 3; u++) {
            for (int v = -5; v <= -1; v++) positions.add(local(scene, u, v));
        }
        positions.add(switchPos(scene));
        positions.add(local(scene, -6, 11));
        positions.add(local(scene, 6, 11));
        positions.add(local(scene, -6, 13));
        positions.add(local(scene, 6, 13));
        positions.add(local(scene, 9, -3)); // Philosopher's standing platform
        return positions;
    }

    /**
     * The old generator rejected ANY hill, stone or sand. Now we inspect only the
     * columns we actually modify. On commands we can elevate the scaffold above
     * the highest column; normal random encounters use a modest terrain tolerance.
     * A null return means the location is unsafe or not loaded.
     */
    public static DilemmaWorldData.Scenario resolveSite(ServerLevel level,
            DilemmaWorldData.Scenario request, boolean manual) {
        List<BlockPos> columns = footprint(request);
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (BlockPos column : columns) {
            if (!level.hasChunkAt(column)) return null;
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    column.getX(), column.getZ());
            BlockPos floor = new BlockPos(column.getX(), top - 1, column.getZ());
            if (top <= level.getMinBuildHeight() + 2 || top >= level.getMaxBuildHeight() - 12) return null;
            // Random encounters avoid water and player-made floors. Commands can
            // place a raised structure above water but still avoid tile entities.
            if (!manual && (!level.getFluidState(floor).isEmpty() || !naturalFloor(level.getBlockState(floor)))) {
                return null;
            }
            min = Math.min(min, top);
            max = Math.max(max, top);
        }
        if (!manual && max - min > 6) return null;
        // For hills, fill only thin supporting piers, rather than carving the mountain.
        // The manually summoned scene gains headroom to clear grass, shrubs and trees.
        for (int lift : (manual ? new int[]{1, 4, 9} : new int[]{0, 2})) {
            int y = max + lift;
            if (y > level.getMaxBuildHeight() - 5) continue;
            DilemmaWorldData.Scenario ready = new DilemmaWorldData.Scenario(
                    new BlockPos(request.origin.getX(), y, request.origin.getZ()),
                    request.facing, request.variant);
            if (clearAtHeight(level, footprint(ready))) return ready;
        }
        return null;
    }

    private static boolean naturalFloor(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.TERRACOTTA) || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.MOSS_BLOCK) || state.is(BlockTags.LOGS);
    }

    private static boolean clearAtHeight(ServerLevel level, List<BlockPos> footprint) {
        for (BlockPos at : footprint) {
            if (!level.hasChunkAt(at) || !canClear(level, at) || !canClear(level, at.above())) return false;
        }
        return true;
    }

    private static boolean canClear(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) != null || !level.getFluidState(pos).isEmpty()) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES);
    }

    public static void build(ServerLevel level, DilemmaWorldData.Scenario scene) {
        Direction forward = Direction.from2DDataValue(scene.facing);
        Direction right = forward.getClockWise();
        RailShape along = forward.getAxis() == Direction.Axis.Z
                ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;
        RailShape across = right.getAxis() == Direction.Axis.Z
                ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;

        // Footbridge for the player. The original rail system is not disturbed.
        for (int u = 1; u <= 3; u++) {
            for (int v = -5; v <= -1; v++) foundation(level, local(scene, u, v));
        }
        for (int v = -11; v < 0; v++) rail(level, scene, 0, v, along);
        for (int u = -3; u <= 3; u++) rail(level, scene, u, 0, across);
        rail(level, scene, -4, 0, corner(right, forward));
        rail(level, scene, 4, 0, corner(right.getOpposite(), forward));
        for (int v = 1; v <= 16; v++) {
            rail(level, scene, -4, v, along);
            rail(level, scene, 4, v, along);
        }

        // The train is a scripted entity, and follows waypoints at this turnout.
        BlockPos lever = switchPos(scene);
        foundation(level, lever);
        level.setBlock(lever, Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, forward)
                .setValue(LeverBlock.POWERED, false), 3);
        marker(level, local(scene, -6, 11), Blocks.RED_WOOL.defaultBlockState());
        marker(level, local(scene, 6, 11), Blocks.LIME_WOOL.defaultBlockState());
        marker(level, local(scene, -6, 13), Blocks.REDSTONE_LAMP.defaultBlockState());
        marker(level, local(scene, 6, 13), Blocks.SEA_LANTERN.defaultBlockState());
        foundation(level, local(scene, 9, -3));

        scene.variant.createCaptives(level, scene);
        spawnPhilosopher(level, scene);
    }

    private static RailShape corner(Direction a, Direction b) {
        boolean n = a == Direction.NORTH || b == Direction.NORTH;
        boolean s = a == Direction.SOUTH || b == Direction.SOUTH;
        boolean e = a == Direction.EAST || b == Direction.EAST;
        if (n) return e ? RailShape.NORTH_EAST : RailShape.NORTH_WEST;
        return e ? RailShape.SOUTH_EAST : RailShape.SOUTH_WEST;
    }

    private static void rail(ServerLevel level, DilemmaWorldData.Scenario scene,
                             int u, int v, RailShape shape) {
        BlockPos pos = local(scene, u, v);
        foundation(level, pos);
        level.setBlock(pos, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape), 3);
    }

    private static void foundation(ServerLevel level, BlockPos pos) {
        // Narrow supports are deliberately restricted to air/replaceable blocks.
        // Existing buildings, ores and tile entities below the bridge are untouched.
        for (int i = 1; i <= 24; i++) {
            BlockPos support = pos.below(i);
            if (support.getY() <= level.getMinBuildHeight()) break;
            BlockState existing = level.getBlockState(support);
            if (!existing.isAir() && !existing.canBeReplaced() && !existing.is(BlockTags.LEAVES)) break;
            if (!level.getFluidState(support).isEmpty()) break;
            level.setBlock(support, Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
    }

    private static void marker(ServerLevel level, BlockPos pos, BlockState state) {
        foundation(level, pos);
        level.setBlock(pos, state, 3);
    }

    private static void spawnPhilosopher(ServerLevel level, DilemmaWorldData.Scenario scene) {
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager == null) return;
        Vec3 pos = point(scene, 9, -3);
        villager.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        villager.getPersistentData().putBoolean(TrainEntity.BIOETHICIST_TAG, true);
        villager.setNoAi(true);
        villager.setPersistenceRequired();
        villager.setCustomName(Component.translatable("entity.mod_e539c988.bioethicist"));
        villager.setCustomNameVisible(true);
        level.addFreshEntity(villager);
    }
}
