package cn.blockforge.generated.mode539c988.client;

import cn.blockforge.generated.mode539c988.GeneratedMod;
import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import cn.blockforge.generated.mode539c988.network.TrainInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME)
public final class ClientGameEvents {
    private ClientGameEvents() { }

    private static CameraType previousCamera;
    private static boolean cameraForcedForTrain;
    // Input state is sent immediately on change and otherwise refreshed every 4 ticks.
    // The server accepts the last input for 5 ticks, so driving remains smooth
    // without broadcasting 20 identical network messages per driver per second.
    private static int previousTrainId = -1;
    private static float previousSteering;
    private static float previousThrottle;
    private static int inputTicksSinceSend;

    @SubscribeEvent
    public static void sendTrainInput(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean driving = player != null && player.getVehicle() instanceof TrainEntity train
                && train.getMode() == TrainEntity.Mode.DRIVABLE;
        if (driving && !cameraForcedForTrain) {
            previousCamera = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            cameraForcedForTrain = true;
        } else if (!driving && cameraForcedForTrain) {
            minecraft.options.setCameraType(previousCamera != null ? previousCamera : CameraType.FIRST_PERSON);
            cameraForcedForTrain = false;
            previousCamera = null;
        }
        if (!driving) {
            while (TrainFlingKey.TOGGLE_FLING.consumeClick()) { /* discard stale input */ }
            previousTrainId = -1;
            previousSteering = 0.0F;
            previousThrottle = 0.0F;
            inputTicksSinceSend = 0;
            return;
        }
        boolean toggleFling = false;
        while (TrainFlingKey.TOGGLE_FLING.consumeClick()) {
            if (minecraft.screen == null) toggleFling = true;
        }
        TrainEntity train = (TrainEntity) player.getVehicle();

        float steering = (minecraft.options.keyLeft.isDown() ? 1.0F : 0.0F)
                - (minecraft.options.keyRight.isDown() ? 1.0F : 0.0F);
        float throttle = (minecraft.options.keyUp.isDown() ? 1.0F : 0.0F)
                - (minecraft.options.keyDown.isDown() ? 1.0F : 0.0F);
        inputTicksSinceSend++;
        boolean changed = train.getId() != previousTrainId
                || steering != previousSteering || throttle != previousThrottle;
        if (changed || toggleFling || inputTicksSinceSend >= 4) {
            PacketDistributor.sendToServer(new TrainInputPayload(train.getId(), steering, throttle, toggleFling));
            previousTrainId = train.getId();
            previousSteering = steering;
            previousThrottle = throttle;
            inputTicksSinceSend = 0;
        }
    }
}
