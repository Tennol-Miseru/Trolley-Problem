package cn.blockforge.generated.mode539c988.dilemma;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-dimension persistent record: structures must not respawn or restart after a reload. */
public final class DilemmaWorldData extends SavedData {
    public static final int WAITING = 0, COUNTDOWN = 1, AFTER_TRAIN = 2, FINISHED = 3;
    private static final String DATA_KEY = "mod_e539c988_trolley_dilemmas_v1";
    public final List<Scenario> scenarios = new ArrayList<>();
    /** Per-player completions in the current ten-dilemma reward cycle. */
    public final Map<UUID, Integer> rewardProgress = new HashMap<>();

    public static DilemmaWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DilemmaWorldData::new, DilemmaWorldData::read, null), DATA_KEY);
    }

    private static DilemmaWorldData read(CompoundTag tag, HolderLookup.Provider registry) {
        DilemmaWorldData data = new DilemmaWorldData();
        ListTag list = tag.getList("Scenarios", 10);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag e = list.getCompound(i);
            Scenario scene = new Scenario(new BlockPos(e.getInt("X"), e.getInt("Y"), e.getInt("Z")),
                    e.getInt("Facing"));
            scene.state = e.getInt("State");
            scene.ticks = e.getInt("Ticks");
            scene.leftSelected = e.getBoolean("LeftSelected");
            scene.variant = DilemmaVariant.byId(e.getString("Variant")); // v1 saves had only the classic layout
            ListTag ids = e.getList("Captives", 10);
            for (int j = 0; j < ids.size(); j++) {
                CompoundTag id = ids.getCompound(j);
                if (id.hasUUID("Id")) scene.captives.add(id.getUUID("Id"));
            }
            data.scenarios.add(scene);
        }
        // Older worlds do not have RewardProgress; their progress starts at zero.
        ListTag rewards = tag.getList("RewardProgress", 10);
        for (int i = 0; i < rewards.size(); ++i) {
            CompoundTag entry = rewards.getCompound(i);
            if (entry.hasUUID("Player")) {
                int progress = entry.getInt("Count");
                data.rewardProgress.put(entry.getUUID("Player"), Math.floorMod(progress, 10));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registry) {
        ListTag list = new ListTag();
        for (Scenario scene : scenarios) {
            CompoundTag e = new CompoundTag();
            e.putInt("X", scene.origin.getX());
            e.putInt("Y", scene.origin.getY());
            e.putInt("Z", scene.origin.getZ());
            e.putInt("Facing", scene.facing);
            e.putInt("State", scene.state);
            e.putInt("Ticks", scene.ticks);
            e.putBoolean("LeftSelected", scene.leftSelected);
            e.putString("Variant", scene.variant.id);
            ListTag ids = new ListTag();
            for (UUID uuid : scene.captives) {
                CompoundTag id = new CompoundTag();
                id.putUUID("Id", uuid);
                ids.add(id);
            }
            e.put("Captives", ids);
            list.add(e);
        }
        tag.put("Scenarios", list);
        ListTag rewards = new ListTag();
        for (Map.Entry<UUID, Integer> entry : rewardProgress.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("Player", entry.getKey());
            item.putInt("Count", entry.getValue());
            rewards.add(item);
        }
        tag.put("RewardProgress", rewards);
        return tag;
    }

    public static final class Scenario {
        public final BlockPos origin;
        public final int facing;
        public final List<UUID> captives = new ArrayList<>();
        public int state = WAITING;
        public int ticks;
        public boolean leftSelected;
        public DilemmaVariant variant = DilemmaVariant.CLASSIC;
        // Runtime only. The first observed switch state after a reload is a baseline.
        public transient boolean leverStateKnown;
        public transient boolean observedLeverPowered;

        public Scenario(BlockPos origin, int facing) {
            this.origin = origin.immutable();
            this.facing = facing;
        }

        public Scenario(BlockPos origin, int facing, DilemmaVariant variant) {
            this(origin, facing);
            this.variant = variant;
        }
    }
}
