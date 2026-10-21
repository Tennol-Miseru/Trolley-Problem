package cn.blockforge.generated.mode539c988.dimension;

import cn.blockforge.generated.mode539c988.GeneratedMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Explicit white fog, independent of the user's biome-color or resource-pack settings. */
@EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class TerminalVisuals {
    private TerminalVisuals() { }

    @SubscribeEvent
    public static void whiteFog(ViewportEvent.ComputeFogColor event) {
        if (event.getCamera().getEntity() != null
                && TerminalDimension.is(event.getCamera().getEntity().level())) {
            event.setRed(1.0F);
            event.setGreen(1.0F);
            event.setBlue(1.0F);
        }
    }

    @SubscribeEvent
    public static void whiteHorizon(ViewportEvent.RenderFog event) {
        if (event.getCamera().getEntity() != null
                && TerminalDimension.is(event.getCamera().getEntity().level())) {
            event.setNearPlaneDistance(22.0F);
            event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), 110.0F));
            event.setCanceled(true);
        }
    }
}
