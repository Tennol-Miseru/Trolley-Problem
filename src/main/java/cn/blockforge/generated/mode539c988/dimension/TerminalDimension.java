package cn.blockforge.generated.mode539c988.dimension;

import cn.blockforge.generated.mode539c988.GeneratedMod;
import cn.blockforge.generated.mode539c988.advancement.TrolleyAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** Tiny dimension entry/exit service. A paper ticket is reusable and returns to the saved location. */
public final class TerminalDimension {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "white_terminal"));

    private TerminalDimension() { }

    public static boolean is(Level level) {
        return level.dimension().equals(KEY);
    }

    public static void toggle(ServerPlayer player) {
        if (is(player.serverLevel())) {
            leave(player);
        } else {
            enter(player);
        }
    }

    public static void enter(ServerPlayer player) {
        if (!player.serverLevel().dimension().equals(Level.OVERWORLD)) {
            player.sendSystemMessage(Component.translatable("message.mod_e539c988.terminal_overworld_only"));
            return;
        }
        ServerLevel terminal = player.serverLevel().getServer().getLevel(KEY);
        if (terminal == null) {
            player.sendSystemMessage(Component.translatable("message.mod_e539c988.terminal_unavailable"));
            return;
        }
        TerminalWorldData data = TerminalWorldData.get(terminal);
        // Always create the arrival platform BEFORE teleporting. Terrain is a flat white surface.
        TerminalStructures.ensureLobby(terminal, data);
        data.homes.put(player.getUUID(), PortalMechanics.findReturnLocation(player));
        data.setDirty();
        player.stopRiding();
        player.teleportTo(terminal, 6.5, 2.0, 0.5, player.getYRot(), player.getXRot());
        terminal.playSound(null, new BlockPos(0, 1, 0), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.AMBIENT, 0.55F, 1.5F);
        TrolleyAdvancements.award(player, "white_terminal");
        player.sendSystemMessage(Component.translatable("message.mod_e539c988.terminal_enter"));
        player.sendSystemMessage(Component.translatable("message.mod_e539c988.boss_hint"));
    }

    public static void leave(ServerPlayer player) {
        if (!is(player.serverLevel())) {
            player.sendSystemMessage(Component.translatable("message.mod_e539c988.terminal_not_here"));
            return;
        }
        ServerLevel homeLevel = player.serverLevel().getServer().overworld();
        TerminalWorldData data = TerminalWorldData.get(player.serverLevel());
        TerminalWorldData.Home home = data.homes.get(player.getUUID());
        player.stopRiding();
        if (home == null) {
            BlockPos fallback = homeLevel.getSharedSpawnPos();
            player.teleportTo(homeLevel, fallback.getX() + 0.5, fallback.getY() + 1.0,
                    fallback.getZ() + 0.5, player.getYRot(), player.getXRot());
        } else {
            player.teleportTo(homeLevel, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        }
        player.sendSystemMessage(Component.translatable("message.mod_e539c988.terminal_leave"));
    }
}
