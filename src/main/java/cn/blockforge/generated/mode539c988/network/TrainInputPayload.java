package cn.blockforge.generated.mode539c988.network;

import cn.blockforge.generated.mode539c988.GeneratedMod;
import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TrainInputPayload(int trainId, float steering, float throttle, boolean toggleFling) implements CustomPacketPayload {
    public static final Type<TrainInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "train_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrainInputPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TrainInputPayload::trainId,
            ByteBufCodecs.FLOAT, TrainInputPayload::steering,
            ByteBufCodecs.FLOAT, TrainInputPayload::throttle,
            ByteBufCodecs.BOOL, TrainInputPayload::toggleFling,
            TrainInputPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TrainInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(payload.trainId());
            if (entity instanceof TrainEntity train && train.getMode() == TrainEntity.Mode.DRIVABLE
                    && train.getFirstPassenger() == player) {
                train.setDriverInput(payload.steering(), payload.throttle());
                if (payload.toggleFling()) train.toggleDriveFling(player);
            }
        });
    }
}
