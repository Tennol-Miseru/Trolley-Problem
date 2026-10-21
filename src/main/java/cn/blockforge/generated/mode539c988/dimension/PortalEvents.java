package cn.blockforge.generated.mode539c988.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Ignite a trolley-block gate with ordinary flint and steel. */
public final class PortalEvents {
    private PortalEvents() { }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !event.getItemStack().is(Items.FLINT_AND_STEEL)) return;
        BlockPos clicked = event.getPos();
        BlockPos intended = clicked.relative(event.getFace());
        boolean success = PortalMechanics.tryIgnite(level, intended);
        // Also allow lighting by clicking a side of the frame, not only its inner edge.
        if (!success) {
            for (var side : net.minecraft.core.Direction.values()) {
                if (PortalMechanics.tryIgnite(level, clicked.relative(side))) {
                    success = true;
                    break;
                }
            }
        }
        if (!success) return;
        if (!event.getEntity().getAbilities().instabuild) {
            EquipmentSlot slot = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            event.getItemStack().hurtAndBreak(1, event.getEntity(), slot);
        }
        level.playSound(null, clicked, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
