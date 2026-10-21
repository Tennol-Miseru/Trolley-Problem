package cn.blockforge.generated.mode539c988.dimension;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Saved inside the terminal dimension. No cell or player return location is lost on reload. */
public final class TerminalWorldData extends SavedData {
    private static final String KEY = "mod_e539c988_terminal_v1";
    final Set<Long> generatedCells = new HashSet<>();
    final Set<Long> railCells = new HashSet<>();
    // Tracks generated or migrated after removing r21 grid intersections.
    final Set<Long> normalizedRailCells = new HashSet<>();
    // Decorative structures are saved so visited areas are not rebuilt every tick.
    final Set<Long> decoratedRailCells = new HashSet<>();
    final Set<Long> decoratedRailSites = new HashSet<>();
    boolean bossDefeated;
    boolean bossArenaBuilt;
    UUID bossId;
    final List<RailSite> railSites = new ArrayList<>();
    final Map<UUID, Home> homes = new HashMap<>();
    boolean lobbyBuilt;
    boolean returnPortalBuilt;

    public record RailSite(int x, int z, boolean alongZ, int halfLength) { }
    public record Home(double x, double y, double z, float yaw, float pitch) { }

    public static TerminalWorldData get(ServerLevel dimension) {
        return dimension.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TerminalWorldData::new, TerminalWorldData::read, null), KEY);
    }

    static long cellKey(int x, int z) {
        return (long) x << 32 | (z & 0xffffffffL);
    }

    private static TerminalWorldData read(CompoundTag tag, HolderLookup.Provider provider) {
        TerminalWorldData data = new TerminalWorldData();
        data.lobbyBuilt = tag.getBoolean("LobbyBuilt");
        data.bossDefeated = tag.getBoolean("BossDefeated");
        data.bossArenaBuilt = tag.getBoolean("BossArenaBuilt");
        if (tag.hasUUID("BossId")) data.bossId = tag.getUUID("BossId");
        for (long key : tag.getLongArray("RailCells")) data.railCells.add(key);
        for (long key : tag.getLongArray("NormalizedRailCells")) data.normalizedRailCells.add(key);
        for (long key : tag.getLongArray("DecoratedRailCells")) data.decoratedRailCells.add(key);
        for (long key : tag.getLongArray("DecoratedRailSites")) data.decoratedRailSites.add(key);
        data.returnPortalBuilt = tag.getBoolean("ReturnPortalBuilt");
        for (long key : tag.getLongArray("GeneratedCells")) data.generatedCells.add(key);
        ListTag rails = tag.getList("Rails", 10);
        for (int i = 0; i < rails.size(); ++i) {
            CompoundTag r = rails.getCompound(i);
            data.railSites.add(new RailSite(r.getInt("X"), r.getInt("Z"), r.getBoolean("AlongZ"), r.getInt("Half")));
        }
        ListTag homes = tag.getList("Homes", 10);
        for (int i = 0; i < homes.size(); ++i) {
            CompoundTag h = homes.getCompound(i);
            if (h.hasUUID("Player")) {
                data.homes.put(h.getUUID("Player"), new Home(h.getDouble("X"), h.getDouble("Y"),
                        h.getDouble("Z"), h.getFloat("Yaw"), h.getFloat("Pitch")));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("LobbyBuilt", lobbyBuilt);
        tag.putBoolean("BossDefeated", bossDefeated);
        tag.putBoolean("BossArenaBuilt", bossArenaBuilt);
        if (bossId != null) tag.putUUID("BossId", bossId);
        tag.putLongArray("RailCells", railCells.stream().mapToLong(Long::longValue).toArray());
        tag.putLongArray("NormalizedRailCells", normalizedRailCells.stream().mapToLong(Long::longValue).toArray());
        tag.putLongArray("DecoratedRailCells", decoratedRailCells.stream().mapToLong(Long::longValue).toArray());
        tag.putLongArray("DecoratedRailSites", decoratedRailSites.stream().mapToLong(Long::longValue).toArray());
        tag.putBoolean("ReturnPortalBuilt", returnPortalBuilt);
        tag.putLongArray("GeneratedCells", generatedCells.stream().mapToLong(Long::longValue).toArray());
        ListTag rails = new ListTag();
        for (RailSite r : railSites) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", r.x()); entry.putInt("Z", r.z());
            entry.putBoolean("AlongZ", r.alongZ()); entry.putInt("Half", r.halfLength());
            rails.add(entry);
        }
        tag.put("Rails", rails);
        ListTag homes = new ListTag();
        for (var e : this.homes.entrySet()) {
            CompoundTag h = new CompoundTag();
            h.putUUID("Player", e.getKey());
            Home p = e.getValue();
            h.putDouble("X", p.x()); h.putDouble("Y", p.y()); h.putDouble("Z", p.z());
            h.putFloat("Yaw", p.yaw()); h.putFloat("Pitch", p.pitch());
            homes.add(h);
        }
        tag.put("Homes", homes);
        return tag;
    }
}
