package cn.blockforge.generated.mode539c988.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Cyan, non-colliding plane, linked to the existing terminal's ticket teleport service. */
public final class TrolleyPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public TrolleyPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                     LevelAccessor level, BlockPos here, BlockPos neighborPos) {
        if (!PortalMechanics.validPortalAt(level, here, state.getValue(AXIS))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, side, neighbor, level, here, neighborPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player) {
            if (!PortalMechanics.validPortalAt(server, pos, state.getValue(AXIS))) {
                server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
            PortalMechanics.handleEntrance(server, player);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(7) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(),
                    0.0D, 0.005D, 0.0D);
        }
    }
}
