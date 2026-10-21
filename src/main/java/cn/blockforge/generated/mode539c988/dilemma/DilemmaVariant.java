package cn.blockforge.generated.mode539c988.dilemma;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;

/** Data-driven set of ethical scenarios. Both branches contain actual, named mobs. */
public enum DilemmaVariant {
    CLASSIC("classic", Captive.VILLAGER, Captive.VILLAGER, 5),
    CAT("cat", Captive.CAT, Captive.VILLAGER, 5),
    DOG("dog", Captive.WOLF, Captive.VILLAGER, 5),
    PETS("pets", Captive.WOLF, Captive.CAT, 5),
    CREEPER("creeper", Captive.CREEPER, Captive.VILLAGER, 5),
    CRIMINAL("criminal", Captive.DOCTOR, Captive.MURDERER, 5),
    CHILD("child", Captive.CHILD, Captive.ADULT, 5),
    MENDING("mending", Captive.MENDING_LIBRARIAN, Captive.VILLAGER, 5);

    public final String id;
    private final Captive left;
    private final Captive right;
    private final int rightCount;

    DilemmaVariant(String id, Captive left, Captive right, int rightCount) {
        this.id = id;
        this.left = left;
        this.right = right;
        this.rightCount = rightCount;
    }

    public Component displayName() {
        return Component.translatable("dilemma.mod_e539c988." + id + ".name");
    }

    public Component label(boolean red) {
        return Component.translatable("dilemma.mod_e539c988." + id + (red ? ".red" : ".green"));
    }

    public static DilemmaVariant byId(String value) {
        // Preserve old worlds whose saved scenario still uses the removed mayor ID.
        if ("mayor".equalsIgnoreCase(value)) return MENDING;
        if (value != null) {
            for (DilemmaVariant type : values()) {
                if (type.id.equals(value.toLowerCase(Locale.ROOT))) return type;
            }
        }
        return CLASSIC;
    }

    public static DilemmaVariant random(RandomSource random) {
        return values()[random.nextInt(values().length)];
    }

    public void createCaptives(ServerLevel level, DilemmaWorldData.Scenario scene) {
        spawn(level, scene, left, -4, 11);
        for (int index = 0; index < rightCount; index++) {
            spawn(level, scene, right, 4, 11 - rightCount / 2 + index);
        }
    }

    private static void spawn(ServerLevel level, DilemmaWorldData.Scenario scene,
                              Captive captive, int right, int forward) {
        Mob mob = captive.type.create(level);
        if (mob == null) return;
        Vec3 at = DilemmaStructures.point(scene, right, forward);
        mob.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        if (mob instanceof Villager villager) {
            if (captive.child) villager.setBaby(true);
            if (captive == Captive.MENDING_LIBRARIAN) configureMendingLibrarian(level, villager);
        }
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.setCustomName(Component.translatable(captive.nameKey));
        mob.setCustomNameVisible(true);
        if (level.addFreshEntity(mob)) scene.captives.add(mob.getUUID());
    }

    /**
     * A real librarian with a guaranteed Mending I trade, not just a custom name.
     * The offer is written into the villager's saved merchant inventory, so the
     * surviving villager can be traded with after the dilemma ends.
     */
    private static void configureMendingLibrarian(ServerLevel level, Villager villager) {
        villager.setVillagerData(villager.getVillagerData()
                .setProfession(VillagerProfession.LIBRARIAN)
                .setLevel(1));
        ItemStack mendingBook = EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(level.registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.MENDING), 1));
        // A fixed offer rather than a random first-level librarian trade.
        villager.getOffers().clear();
        villager.getOffers().add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 20),
                Optional.of(new ItemCost(Items.BOOK, 1)),
                mendingBook, 12, 5, 0.05F));
    }

    private enum Captive {
        VILLAGER(EntityType.VILLAGER, "dilemma.mod_e539c988.captive.villager", false),
        CAT(EntityType.CAT, "dilemma.mod_e539c988.captive.cat", false),
        WOLF(EntityType.WOLF, "dilemma.mod_e539c988.captive.wolf", false),
        CREEPER(EntityType.CREEPER, "dilemma.mod_e539c988.captive.creeper", false),
        DOCTOR(EntityType.VILLAGER, "dilemma.mod_e539c988.captive.doctor", false),
        MURDERER(EntityType.VILLAGER, "dilemma.mod_e539c988.captive.murderer", false),
        CHILD(EntityType.VILLAGER, "dilemma.mod_e539c988.captive.child", true),
        ADULT(EntityType.VILLAGER, "dilemma.mod_e539c988.captive.adult", false),
        MENDING_LIBRARIAN(EntityType.VILLAGER, "dilemma.mod_e539c988.captive.mending_librarian", false);

        final EntityType<? extends Mob> type;
        final String nameKey;
        final boolean child;

        Captive(EntityType<? extends Mob> type, String nameKey, boolean child) {
            this.type = type;
            this.nameKey = nameKey;
            this.child = child;
        }
    }
}
