package cn.blockforge.generated.mode539c988.client;

import cn.blockforge.generated.mode539c988.GeneratedMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Rebindable V key; no Shift needed (Shift remains dismount). */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public final class TrainFlingKey {
    private TrainFlingKey() { }

    public static final KeyMapping TOGGLE_FLING = new KeyMapping(
            "key.mod_e539c988.toggle_fling", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, "key.categories.mod_e539c988");

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_FLING);
    }
}
