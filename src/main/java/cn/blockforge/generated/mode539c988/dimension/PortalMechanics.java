package cn.blockforge.generated.mode539c988.dimension;

import cn.blockforge.generated.mode539c988.GeneratedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A deliberately small frame detector: 4x4 complete frames (exactly 12 blocks),
 * plus standard Nether-style 4x5 frames whose four corners may be missing.
 * The frame's width can run along either horizontal axis.
 */
public final class PortalMechanics {
    private static final int FRAME_WIDTH = 4;
    private static final String PORTAL_COOLDOWN = "mod_e539c988_portal_until";
    private static final BlockPos RETURN_FRAME = new BlockPos(11, 0, 0);

    public record Frame(BlockPos bottomLeft, Direction.Axis axis, int height) {
        BlockPos position(int horizontal, int vertical) {
            return bottomLeft.relative(axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH,
                    horizontal).above(vertical);
        }
    }

    private PortalMechanics() { }

    private static boolean isFrame(LevelAccessor level, Frame frame) {
        int height = frame.height();
        for (int h = 0; h < FRAME_WIDTH; h++) {
            for (int v = 0; v < height; v++) {
                boolean boundary = h == 0 || h == FRAME_WIDTH - 1 || v == 0 || v == height - 1;
                if (boundary) {
                    boolean corner = (h == 0 || h == FRAME_WIDTH - 1) && (v == 0 || v == height - 1);
                    // A conventional 4x5 Nether gate does not require its four corners.
                    if (corner && height == 5) continue;
                    if (!level.getBlockState(frame.position(h, v)).is(GeneratedMod.TROLLEY_BLOCK.get())) {
                        return false;
                    }
                } else {
                    BlockState inside = level.getBlockState(frame.position(h, v));
                    if (!inside.isAir() && !inside.is(Blocks.FIRE) && !inside.is(Blocks.SOUL_FIRE)
                            && !inside.is(GeneratedMod.TROLLEY_PORTAL.get())) return false;
                }
            }
        }
        return true;
    }

    private static Frame findFrame(LevelAccessor level, BlockPos inside, Direction.Axis axis) {
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        for (int height : new int[]{4, 5}) {
            for (int column = 1; column <= 2; column++) {
                for (int row = 1; row < height - 1; row++) {
                    Frame frame = new Frame(inside.relative(along, -column).below(row), axis, height);
                    if (isFrame(level, frame)) return frame;
                }
            }
        }
        return null;
    }

    /** Called by the flint-and-steel right-click event, before ordinary fire is placed. */
    public static boolean tryIgnite(ServerLevel level, BlockPos inside) {
        if (!level.getBlockState(inside).isAir() && !level.getBlockState(inside).is(Blocks.FIRE)
                && !level.getBlockState(inside).is(Blocks.SOUL_FIRE)) return false;
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Frame frame = findFrame(level, inside, axis);
            if (frame == null) continue;
            fillPortal(level, frame);
            level.playSound(null, inside, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.9F, 1.3F);
            return true;
        }
        return false;
    }

    private static void fillPortal(ServerLevel level, Frame frame) {
        BlockState portal = GeneratedMod.TROLLEY_PORTAL.get().defaultBlockState()
                .setValue(TrolleyPortalBlock.AXIS, frame.axis());
        for (int y = 1; y < frame.height() - 1; y++) {
            for (int x = 1; x < FRAME_WIDTH - 1; x++) {
                level.setBlock(frame.position(x, y), portal, 3);
            }
        }
    }

    /** Validate a portal tile when its neighbors change, to collapse broken gates. */
    public static boolean validPortalAt(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        return findFrame(level, pos, axis) != null;
    }

    public static void handleEntrance(ServerLevel level, ServerPlayer player) {
        if (!TerminalDimension.is(level) && !level.dimension().equals(Level.OVERWORLD)) return;
        long tick = level.getServer().overworld().getGameTime();
        if (tick < player.getPersistentData().getLong(PORTAL_COOLDOWN)) return;
        player.getPersistentData().putLong(PORTAL_COOLDOWN, tick + 100L);
        if (TerminalDimension.is(level)) TerminalDimension.leave(player);
        else TerminalDimension.enter(player);
    }

    /** Back in the Overworld, aim for a solid, two-block-high open space beside the gate. */
    public static TerminalWorldData.Home findReturnLocation(ServerPlayer player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        BlockState state = level.getBlockState(feet);
        if (!state.is(GeneratedMod.TROLLEY_PORTAL.get())) {
            state = level.getBlockState(feet.above());
        }
        if (state.is(GeneratedMod.TROLLEY_PORTAL.get())) {
            Direction.Axis axis = state.getValue(TrolleyPortalBlock.AXIS);
            Direction normal = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
            for (Direction side : new Direction[]{normal, normal.getOpposite()}) {
                for (int distance = 2; distance <= 4; distance++) {
                    BlockPos candidate = feet.relative(side, distance);
                    if (level.getBlockState(candidate).isAir()
                            && level.getBlockState(candidate.above()).isAir()
                            && !level.getBlockState(candidate.below()).isAir()) {
                        return new TerminalWorldData.Home(candidate.getX() + 0.5, candidate.getY(),
                                candidate.getZ() + 0.5, player.getYRot(), player.getXRot());
                    }
                }
            }
        }
        return new TerminalWorldData.Home(player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
    }

    /** Adds one return gate to the existing white arrival platform, including old r17 saves. */
    public static boolean installLobbyReturnPortal(ServerLevel level) {
        Frame frame = new Frame(RETURN_FRAME, Direction.Axis.X, 4);
        boolean existing = isFrame(level, frame);
        if (!existing) {
            for (int x = 0; x < FRAME_WIDTH; x++) {
                for (int y = 0; y < 4; y++) {
                    BlockPos pos = frame.position(x, y);
                    if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir()) return false;
                }
            }
            for (int x = 0; x < FRAME_WIDTH; x++) {
                for (int y = 0; y < 4; y++) {
                    if (x == 0 || x == 3 || y == 0 || y == 3)
                        level.setBlock(frame.position(x, y), GeneratedMod.TROLLEY_BLOCK.get().defaultBlockState(), 3);
                }
            }
        }
        if (!isFrame(level, frame)) return false;
        fillPortal(level, frame);
        // A short, safe concrete walk from the already-built arrival platform.
        for (int x = 9; x <= 15; x++) {
            for (int z = 1; z <= 3; z++) {
                BlockPos tile = new BlockPos(x, 0, z);
                if (level.getBlockState(tile).isAir())
                    level.setBlock(tile, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            }
        }
        return true;
    }
}
