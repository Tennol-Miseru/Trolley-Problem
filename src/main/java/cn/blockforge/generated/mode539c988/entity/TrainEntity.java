package cn.blockforge.generated.mode539c988.entity;

import cn.blockforge.generated.mode539c988.GeneratedMod;
import cn.blockforge.generated.mode539c988.advancement.TrolleyAdvancements;
import cn.blockforge.generated.mode539c988.dimension.TerminalBoss;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.Collections;
import java.util.WeakHashMap;

public final class TrainEntity extends Entity {
    public enum Mode { AUTOMATIC, DRIVABLE, WILD, FLING }
    public enum PhysicsMode { VOID, PHYSICAL }

    public static final String BIOETHICIST_TAG = "mod_e539c988_bioethicist";
    private static final String BUMPER_EJECTION_GRACE = "mod_e539c988_bumper_ejection_grace_until";
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(TrainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHYSICS_MODE =
            SynchedEntityData.defineId(TrainEntity.class, EntityDataSerializers.INT);
    // The fleeting trains high above White Terminal are scenery only.
    private static final EntityDataAccessor<Boolean> SKY_TRAIN =
            SynchedEntityData.defineId(TrainEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BOSS_TRAIN =
            SynchedEntityData.defineId(TrainEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SKYFALL =
            SynchedEntityData.defineId(TrainEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLING_DRIVE =
            SynchedEntityData.defineId(TrainEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> BOSS_SPIN =
            SynchedEntityData.defineId(TrainEntity.class, EntityDataSerializers.FLOAT);
    private static final String[] COMMENTS = {
            "comment.mod_e539c988.bioethicist.1",
            "comment.mod_e539c988.bioethicist.2",
            "comment.mod_e539c988.bioethicist.3",
            "comment.mod_e539c988.bioethicist.4",
            "comment.mod_e539c988.bioethicist.5"
    };

    private static final int SUMMONED_LIFETIME_TICKS = 10 * 20;
    private static final int EMPTY_DRIVER_DESPAWN_TICKS = 15 * 20;
    private static final double MOVING_SOUND_THRESHOLD_SQR = 0.035D * 0.035D;
    private static final double GRAVITY = 0.08D;
    private static final double MAX_FORWARD_SPEED = 0.82D;
    private static final double MAX_REVERSE_SPEED = 0.34D;
    private static final double ACCELERATION = 0.055D;
    private static final double DRAG = 0.9D;
    private static final float TURN_SPEED = 3.2F;
    private static final float MAX_STEP_HEIGHT = 1.0F;
    // Keep the drivable train inside the White Terminal's narrow rail corridors.
    // Rendering stays full-size; this only affects physical block/entity collision.
    private static final float DRIVER_COLLISION_WIDTH = 1.25F;
    private static final float DRIVER_COLLISION_HEIGHT = 2.35F;
    private static final float MAX_DURABILITY = 40.0F;
    private static final float MAX_DAMAGE_PER_HIT = 4.0F;
    // Fling is playful but no longer harmless: 8 HP (four hearts) before armor.
    private static final float FLING_HIT_DAMAGE = 8.0F;
    // Driven trolley meshes are ~8.52 blocks long and ~2.13 wide at 1.35x render scale.
    // Two oriented rectangles make car-to-car impact happen when the visible bodies touch,
    // instead of waiting for the tiny 1.25-wide navigation hitboxes to overlap.
    private static final double BUMPER_HALF_LENGTH = 4.27D;
    private static final double BUMPER_HALF_WIDTH = 1.07D;
    private static final double BUMPER_CONTACT_PADDING = 0.35D;
    private static final int BUMPER_COOLDOWN_TICKS = 34;
    private static final int BUMPER_RECOIL_TICKS = 10;

    // Dedicated boss attacks: ordinary trolley one-hit kills NEVER apply.
    private static final float BOSS_MAX_HEALTH = 200.0F;
    private static final float BOSS_HEALTH_CAP = 500.0F;
    private static final double BOSS_PARTICIPANT_RANGE_SQR = 100.0D * 100.0D;
    // Weak keys avoid retaining unloaded boss entities when changing worlds.
    private static final Set<TrainEntity> BOSSES_WITH_BARS =
            Collections.newSetFromMap(new WeakHashMap<TrainEntity, Boolean>());
    // Patterns rotate after every completed cycle: fast ram, twin ram, turning pursuit.
    private static final int BOSS_WINDUP_TICKS = 22;      // Audible/visible warning, 1.1 s
    private static final int BOSS_FIRST_CHARGE_END = 51; // 29 ticks of acceleration
    private static final int BOSS_SECOND_AIM_END = 65;   // 14 ticks to react before second ram
    private static final int BOSS_SECOND_CHARGE_END = 88;
    private static final int BOSS_RECOVERY_END = 86;
    private static final int BOSS_DOUBLE_RECOVERY_END = 116;
    // The model's X span is ~25.25 blocks long when rendered at boss scale;
    // its sideways span is ~6.31 blocks. A single fixed 12x12 box misses the ends.
    private static final double BOSS_HALF_LENGTH = 12.9D;
    private static final double BOSS_HALF_WIDTH = 3.45D;
    private static final double BOSS_MIN_Y = -0.80D;
    private static final double BOSS_MAX_Y = 8.40D;
    private static final float BOSS_RAM_DAMAGE = 8.0F;   // 4 hearts before armor
    private static final float BOSS_TWIN_DAMAGE = 6.0F;  // Per rush: 3 hearts before armor
    private static final float BOSS_SWEEP_DAMAGE = 9.0F; // 4.5 hearts before armor
    private static final int BOSS_LEAP_WINDUP = 28;
    private static final int BOSS_LEAP_LAND_TICK = 67;
    private static final int BOSS_LEAP_RECOVERY_END = 111;
    private static final int BOSS_SPIN_WINDUP = 30;
    private static final int BOSS_SPIN_CHARGE_END = 81;
    private static final int BOSS_SPIN_RECOVERY_END = 112;
    private static final float BOSS_LEAP_DAMAGE = 9.0F;
    private static final float BOSS_SPIN_DAMAGE = 8.0F;
    // Boss attacks are scripted and have no vanilla gravity. Always return to the
    // original arena level when a leap is interrupted by a phase trial or reload.
    private static final double BOSS_GROUND_Y = TerminalBoss.ARENA.getY() + 0.12D;

    private Vec3 heading = new Vec3(0.0D, 0.0D, 1.0D);
    private int life;
    private float durability = MAX_DURABILITY;
    private double drivingSpeed;
    private float driverSteering;
    private float driverThrottle;
    private int driverInputAge = 100;
    private int driverlessTicks;
    private int lastFlingSwitchTick = -100;
    private final Set<UUID> alreadyHitPlayers = new HashSet<>();
    private final Set<UUID> alreadyFlungLiving = new HashSet<>();
    private final Map<UUID, Integer> flingTargetCooldown = new HashMap<>();
    private final Map<UUID, Integer> bumperCooldown = new HashMap<>();
    private Vec3 bumperRecoil = Vec3.ZERO;
    private int bumperRecoilTicks;
    private int clientLerpSteps;
    private double clientLerpX;
    private double clientLerpY;
    private double clientLerpZ;
    private float clientLerpYRot;
    private float clientLerpXRot;
    private UUID ownerId;
    // A scripted three-segment route is only used by world-generated dilemmas.
    // Ordinary automatic, wild and drivable train movement is unchanged.
    private final List<Vec3> dilemmaWaypoints = new ArrayList<>();
    private int dilemmaWaypointIndex;
    private boolean dilemmaRouteComplete;
    // Track re-entry into a column (not every stationary tick), for the loop advancement.
    private final Map<net.minecraft.core.BlockPos, Integer> visitedColumns = new HashMap<>();
    private net.minecraft.core.BlockPos previousColumn;
    private boolean awardedLoop;
    private float bossMaxHealth = BOSS_MAX_HEALTH;
    private int bossPhase;
    private int bossAttackTick;
    private int bossAttackPattern; // 0 ram, 1 twin, 2 pursuit, 3 jump slam, 4 spin charge
    private final Set<UUID> bossHitThisCharge = new HashSet<>();
    private ServerBossEvent bossBar;
    private Vec3 leapOrigin = Vec3.ZERO;
    private Vec3 leapTarget = Vec3.ZERO;
    private double leapGroundY;
    private boolean trialPending;
    private BlockPosHolder trialPoint;
    private double skyfallGroundY;
    private record BlockPosHolder(int x, int y, int z) {
        net.minecraft.core.BlockPos pos() { return new net.minecraft.core.BlockPos(x, y, z); }
    }

    public TrainEntity(EntityType<? extends TrainEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.noCulling = true;
    }

    public static TrainEntity create(Level level, Mode mode, PhysicsMode physicsMode,
                                     Vec3 position, Vec3 direction, Player owner) {
        TrainEntity train = GeneratedMod.TRAIN.get().create(level);
        if (train == null) {
            throw new IllegalStateException("无法创建电车实体");
        }
        train.setMode(mode);
        // Driver trains start in launch/fling mode; V still toggles normal driving.
        // Set this on the server-created entity before it is spawned/synced.
        if (mode == Mode.DRIVABLE) train.entityData.set(FLING_DRIVE, true);
        train.setPhysicsMode(physicsMode);
        train.setPos(position);
        train.setHeading(direction);
        train.ownerId = owner.getUUID();
        return train;
    }

    public static TrainEntity createDilemmaTrain(ServerLevel level, Vec3 spawn, List<Vec3> waypoints) {
        TrainEntity train = GeneratedMod.TRAIN.get().create(level);
        if (train == null) throw new IllegalStateException("Could not create trolley train");
        train.setMode(Mode.AUTOMATIC);
        train.setPhysicsMode(PhysicsMode.VOID);
        train.setPos(spawn);
        train.dilemmaWaypoints.addAll(waypoints);
        train.setHeading(waypoints.get(0).subtract(spawn));
        return train;
    }

    /** Giant unique hostile entity. It stays in the center arena. */
    public static TrainEntity createBoss(ServerLevel level, Vec3 spawn) {
        TrainEntity train = createDilemmaTrain(level, spawn, List.of(spawn.add(0, 0, 0.01)));
        train.dilemmaWaypoints.clear();
        train.entityData.set(BOSS_TRAIN, true);
        train.refreshDimensions();
        train.updateBossHitbox();
        train.bossMaxHealth = BOSS_MAX_HEALTH;
        train.durability = train.bossMaxHealth;
        train.setCustomName(Component.translatable("entity.mod_e539c988.giant_train"));
        train.setCustomNameVisible(false);
        return train;
    }

    /** A falling train is presentation plus one server-side mob-only area hit. */
    public static TrainEntity createSkyfall(ServerLevel level, Vec3 center, double groundY, Player owner) {
        TrainEntity train = create(level, Mode.AUTOMATIC, PhysicsMode.VOID,
                center.add(0, 27, 0), new Vec3(0, 0, 1), owner);
        train.entityData.set(SKYFALL, true);
        train.skyfallGroundY = groundY;
        return train;
    }

    public boolean isBossTrain() { return entityData.get(BOSS_TRAIN); }
    public float getBossSpinAngle() { return entityData.get(BOSS_SPIN); }
    public boolean isSkyfall() { return entityData.get(SKYFALL); }
    public boolean isFlingDriveMode() { return entityData.get(FLING_DRIVE); }

    /** Called only by an authenticated vehicle input packet on the server. */
    public void toggleDriveFling(ServerPlayer driver) {
        if (level().isClientSide() || getMode() != Mode.DRIVABLE || getFirstPassenger() != driver
                || life - lastFlingSwitchTick < 8) return;
        lastFlingSwitchTick = life;
        boolean enabled = !isFlingDriveMode();
        entityData.set(FLING_DRIVE, enabled);
        driver.displayClientMessage(Component.translatable(enabled
                ? "message.mod_e539c988.driving_fling_on"
                : "message.mod_e539c988.driving_fling_off"), true);
        level().playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundSource.PLAYERS, 1.1F, enabled ? 1.65F : 0.85F);
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                getX(), getY() + 1.0D, getZ(), 22, 1.15D, 0.6D, 1.15D, 0.15D);
    }


    public void setSkyTrain(boolean visualOnly) {
        this.entityData.set(SKY_TRAIN, visualOnly);
    }

    public boolean isSkyTrain() {
        return this.entityData.get(SKY_TRAIN);
    }

    public void setMode(Mode mode) {
        this.entityData.set(MODE, mode.ordinal());
        // Resize immediately for newly summoned and save-loaded driver trains.
        this.refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        // The client receives MODE after spawn, so update its collision box too.
        if (MODE.equals(accessor)) this.refreshDimensions();
    }

    public Mode getMode() {
        int index = this.entityData.get(MODE);
        return index >= 0 && index < Mode.values().length ? Mode.values()[index] : Mode.AUTOMATIC;
    }

    public void setPhysicsMode(PhysicsMode physicsMode) {
        this.entityData.set(PHYSICS_MODE, physicsMode.ordinal());
        this.noPhysics = physicsMode == PhysicsMode.VOID;
    }

    public PhysicsMode getPhysicsMode() {
        int index = this.entityData.get(PHYSICS_MODE);
        return index >= 0 && index < PhysicsMode.values().length
                ? PhysicsMode.values()[index] : PhysicsMode.VOID;
    }

    public void setDriverInput(float steering, float throttle) {
        if (!Float.isFinite(steering) || !Float.isFinite(throttle)) {
            return;
        }
        this.driverSteering = Math.max(-1.0F, Math.min(1.0F, steering));
        this.driverThrottle = Math.max(-1.0F, Math.min(1.0F, throttle));
        this.driverInputAge = 0;
    }

    @Override
    public boolean isControlledByLocalInstance() {
        // Movement is authoritative on the server; the client only sends key input.
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        // An entity's native dimensions are square in X/Z. This is only the
        // fallback until the accurate rotated bounds have been applied.
        if (this.entityData != null) {
            if (isBossTrain()) return EntityDimensions.scalable(25.8F, 9.25F);
            if (getMode() == Mode.DRIVABLE)
                return EntityDimensions.scalable(DRIVER_COLLISION_WIDTH, DRIVER_COLLISION_HEIGHT);
        }
        return super.getDimensions(pose);
    }

    /**
     * Fit the boss's hitbox to its actual 4x-rendered model.
     * Minecraft selection bounds must be axis-aligned; compute the tightest
     * axis-aligned box containing the rotated 25.3 x 6.3 block train.
     * Unlike a fixed 25x25 box, this follows the boss's facing direction.
     */
    private void updateBossHitbox() {
        if (!isBossTrain()) return;
        double alongX = Math.abs(heading.x);
        double alongZ = Math.abs(heading.z);
        double halfX = alongX * BOSS_HALF_LENGTH + alongZ * BOSS_HALF_WIDTH;
        double halfZ = alongZ * BOSS_HALF_LENGTH + alongX * BOSS_HALF_WIDTH;
        this.setBoundingBox(new AABB(getX() - halfX, getY() + BOSS_MIN_Y, getZ() - halfZ,
                getX() + halfX, getY() + BOSS_MAX_Y, getZ() + halfZ));
    }

    /** Reject damage from the empty corners of the rotated selection bounds. */
    private boolean touchesBossBody(LivingEntity target) {
        AABB targetBox = target.getBoundingBox();
        if (targetBox.maxY < getY() + BOSS_MIN_Y || targetBox.minY > getY() + BOSS_MAX_Y) {
            return false;
        }
        Vec3 delta = target.position().subtract(position());
        double forward = delta.x * heading.x + delta.z * heading.z;
        double side = delta.x * -heading.z + delta.z * heading.x;
        // Expand for a player/mob's own collision box to avoid edge misses.
        double targetPadding = Math.max(targetBox.getXsize(), targetBox.getZsize()) * 0.5D;
        return Math.abs(forward) <= BOSS_HALF_LENGTH + targetPadding
                && Math.abs(side) <= BOSS_HALF_WIDTH + targetPadding;
    }

    @Override
    public float maxUpStep() {
        return getPhysicsMode() == PhysicsMode.PHYSICAL ? MAX_STEP_HEIGHT : 0.0F;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (!this.level().isClientSide()) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
            return;
        }
        this.clientLerpX = x;
        this.clientLerpY = y;
        this.clientLerpZ = z;
        this.clientLerpYRot = yRot;
        this.clientLerpXRot = xRot;
        this.clientLerpSteps = Math.max(steps, 1);
    }

    public void setHeading(Vec3 direction) {
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() > 1.0E-5D) {
            this.heading = flat.normalize();
            this.setYRot((float) Math.toDegrees(Math.atan2(-this.heading.x, this.heading.z)));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, Mode.AUTOMATIC.ordinal());
        builder.define(PHYSICS_MODE, PhysicsMode.VOID.ordinal());
        builder.define(SKY_TRAIN, false);
        builder.define(BOSS_TRAIN, false);
        builder.define(SKYFALL, false);
        builder.define(BOSS_SPIN, 0.0F);
        builder.define(FLING_DRIVE, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            tickClientLerp();
            updateBossHitbox();
            makeClientEffects();
            return;
        }

        this.life++;
        this.driverInputAge++;
        if (isSkyfall()) { tickSkyfall(); return; }
        if (isBossTrain()) {
            tickBoss();
            updateBossHitbox();
            separateBossPlayers();
            return;
        }
        // Count only server ticks without a real driver. Riding again cancels cleanup.
        if (getMode() == Mode.DRIVABLE) {
            if (getFirstPassenger() instanceof ServerPlayer) driverlessTicks = 0;
            else if (++driverlessTicks >= EMPTY_DRIVER_DESPAWN_TICKS) {
                ejectPassengers();
                discard();
                return;
            }
        }
        Vec3 beforeMove = position();
        Vec3 movement = movementForMode();
        this.setDeltaMovement(movement);
        if (getPhysicsMode() == PhysicsMode.VOID) {
            this.setPos(this.position().add(movement));
        } else {
            this.move(MoverType.SELF, movement);
        }
        Vec3 actualMovement = position().subtract(beforeMove);
        if (!isSkyTrain()) {
            if (getMode() == Mode.DRIVABLE) bumpIntoOtherTrains();
            if (isRemoved()) return;
            // An abandoned vehicle is not a wandering trap for the next 15 seconds.
            if (getMode() != Mode.DRIVABLE || getFirstPassenger() instanceof ServerPlayer)
                crushLivingEntities();
            checkRepeatedPasses();
        }
        if (dilemmaRouteComplete) {
            discard();
            return;
        }

        if (!isSkyTrain() && this.life % 25 == 0
                && (getMode() != Mode.DRIVABLE
                    || (getFirstPassenger() instanceof ServerPlayer
                        && actualMovement.x * actualMovement.x + actualMovement.z * actualMovement.z
                                > MOVING_SOUND_THRESHOLD_SQR))) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.MINECART_RIDING,
                    SoundSource.NEUTRAL, 1.8F, 0.65F);
        }

        boolean summonedTrainExpired = getMode() != Mode.DRIVABLE && this.life >= SUMMONED_LIFETIME_TICKS;
        if (summonedTrainExpired || this.getY() < this.level().getMinBuildHeight() - 16) {
            ejectPassengers();
            discard();
        }
    }

    private void tickSkyfall() {
        // Spawns above the player's aimed point and falls visually for ~16 ticks.
        this.setPos(this.getX(), this.getY() - 1.8D, this.getZ());
        if (this.getY() <= skyfallGroundY + 1 || life > 55) {
            AABB area = new AABB(this.getX() - 14, skyfallGroundY - 24, this.getZ() - 14,
                    this.getX() + 14, skyfallGroundY + 16, this.getZ() + 14);
            for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, area,
                    living -> living.isAlive() && !(living instanceof Player))) {
                victim.kill();
            }
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.EXPLOSION, getX(), skyfallGroundY + 2, getZ(),
                        25, 7.0, 2.0, 7.0, 0.1);
                server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.PLAYERS, 2.0F, 0.55F);
            }
            discard();
        }
    }

    /** Five readable patterns: ram, twin ram, pursuit, leap+impact, spinning charge. */
    private void tickBoss() {
        if (!(level() instanceof ServerLevel server)) return;
        // Leaping uses setPos() rather than gravity. If an incoming hit triggers
        // the next dilemma phase halfway through the arc, tickBoss() returns
        // early and the next ground dash otherwise inherits the airborne Y.
        // Recover the ground height for interrupted leaps, idle/windup states,
        // and bosses loaded from an older save while floating.
        boolean activelyAirborne = !trialPending && bossAttackPattern == 3
                && bossAttackTick > BOSS_LEAP_WINDUP
                && bossAttackTick < BOSS_LEAP_LAND_TICK;
        if (!activelyAirborne && Math.abs(getY() - BOSS_GROUND_Y) > 0.001D) {
            setPos(getX(), BOSS_GROUND_Y, getZ());
            updateBossHitbox();
        }
        // Count only active survival/adventure participants currently in the arena.
        // Re-evaluate once per second to avoid frequent network/UI updates.
        if (life == 1 || life % 20 == 0) updateBossHealthForParticipants(server);
        updateBossBar(server);
        if (trialPending) {
            bossAttackTick = 0;
            bossHitThisCharge.clear();
            entityData.set(BOSS_SPIN, 0.0F);
            if (trialPoint == null) {
                net.minecraft.core.BlockPos pos = TerminalBoss.startTrial(server, bossPhase);
                if (pos != null) trialPoint = new BlockPosHolder(pos.getX(), pos.getY(), pos.getZ());
            } else if (TerminalBoss.trialFinished(server, trialPoint.pos())) {
                trialPending = false;
                trialPoint = null;
                server.playSound(null, blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                        SoundSource.HOSTILE, 1.5F, 0.7F);
            }
            return;
        }

        ServerPlayer target = null;
        double nearest = 75.0D * 75.0D;
        for (ServerPlayer player : server.players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) continue;
            double dist = player.distanceToSqr(this);
            if (dist < nearest) { nearest = dist; target = player; }
        }
        if (target == null) {
            bossAttackTick = 0;
            bossHitThisCharge.clear();
            entityData.set(BOSS_SPIN, 0.0F);
            return;
        }
        bossAttackTick++;
        if (bossAttackTick == 1) {
            bossHitThisCharge.clear();
            entityData.set(BOSS_SPIN, 0.0F);
            String warning = switch (bossAttackPattern) {
                case 1 -> "message.mod_e539c988.boss_twin_warning";
                case 2 -> "message.mod_e539c988.boss_sweep_warning";
                case 3 -> "message.mod_e539c988.boss_leap_warning";
                case 4 -> "message.mod_e539c988.boss_spin_warning";
                default -> "message.mod_e539c988.boss_charge_warning";
            };
            announceBossAttack(server, warning);
            server.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                    SoundSource.HOSTILE, 2.0F, bossAttackPattern == 3 ? 0.95F : 0.55F);
        }

        if (bossAttackPattern == 3) tickBossLeap(server, target);
        else if (bossAttackPattern == 4) tickBossSpin(server, target);
        else tickBossGroundCharge(server, target);

        int end = switch (bossAttackPattern) {
            case 1 -> BOSS_DOUBLE_RECOVERY_END;
            case 3 -> BOSS_LEAP_RECOVERY_END;
            case 4 -> BOSS_SPIN_RECOVERY_END;
            default -> BOSS_RECOVERY_END;
        };
        if (bossAttackTick >= end) {
            bossAttackTick = 0;
            bossHitThisCharge.clear();
            entityData.set(BOSS_SPIN, 0.0F);
            bossAttackPattern = (bossAttackPattern + 1) % 5;
        }
    }

    private void tickBossGroundCharge(ServerLevel server, ServerPlayer target) {
        if (bossAttackTick <= BOSS_WINDUP_TICKS) {
            aimBossAt(target);
            if (bossAttackTick % 5 == 0) {
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2.0D, getZ(),
                        9, 3.0D, 1.3D, 3.0D, 0.05D);
            }
        } else if (bossAttackTick <= BOSS_FIRST_CHARGE_END) {
            if (bossAttackTick == BOSS_WINDUP_TICKS + 1) startBossDash(server);
            int elapsed = bossAttackTick - BOSS_WINDUP_TICKS;
            double speed = Math.min(bossAttackPattern == 2 ? 1.95D : 2.15D,
                    1.25D + elapsed * 0.045D);
            if (bossAttackPattern == 2 && elapsed <= 21) {
                Vec3 toward = target.position().subtract(position());
                if (toward.horizontalDistanceSqr() > 0.05D)
                    setHeading(heading.scale(0.96D).add(toward.normalize().scale(0.04D)));
            }
            dashBoss(speed, bossAttackPattern == 1 ? BOSS_TWIN_DAMAGE
                    : bossAttackPattern == 2 ? BOSS_SWEEP_DAMAGE : BOSS_RAM_DAMAGE);
        } else if (bossAttackPattern == 1 && bossAttackTick <= BOSS_SECOND_AIM_END) {
            aimBossAt(target);
            if (bossAttackTick == BOSS_FIRST_CHARGE_END + 1) {
                bossHitThisCharge.clear();
                announceBossAttack(server, "message.mod_e539c988.boss_second_warning");
                server.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                        SoundSource.HOSTILE, 2.0F, 1.15F);
            }
            if (bossAttackTick % 4 == 0)
                server.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.5D, getZ(),
                        8, 2.0D, 0.6D, 2.0D, 0.08D);
        } else if (bossAttackPattern == 1 && bossAttackTick <= BOSS_SECOND_CHARGE_END) {
            if (bossAttackTick == BOSS_SECOND_AIM_END + 1) startBossDash(server);
            int elapsed = bossAttackTick - BOSS_SECOND_AIM_END;
            dashBoss(Math.min(2.2D, 1.5D + elapsed * 0.055D), BOSS_TWIN_DAMAGE);
        } else {
            int recoveryStart = bossAttackPattern == 1
                    ? BOSS_SECOND_CHARGE_END + 1 : BOSS_FIRST_CHARGE_END + 1;
            if (bossAttackTick == recoveryStart)
                server.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND,
                        SoundSource.HOSTILE, 1.0F, 0.72F);
        }
    }

    /** Jumps into the air, tracks one ground spot, then slams there with a shockwave. */
    private void tickBossLeap(ServerLevel server, ServerPlayer target) {
        if (bossAttackTick <= BOSS_LEAP_WINDUP) {
            aimBossAt(target);
            if (bossAttackTick % 5 == 0)
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.5D, getZ(),
                        14, 3.0D, 0.3D, 3.0D, 0.08D);
        } else if (bossAttackTick <= BOSS_LEAP_LAND_TICK) {
            int elapsed = bossAttackTick - BOSS_LEAP_WINDUP;
            int flight = BOSS_LEAP_LAND_TICK - BOSS_LEAP_WINDUP;
            if (elapsed == 1) {
                leapOrigin = position();
                leapGroundY = getY();
                Vec3 center = Vec3.atBottomCenterOf(TerminalBoss.ARENA);
                Vec3 desired = target.position();
                Vec3 offset = desired.subtract(center);
                if (offset.horizontalDistance() > 39.0D)
                    desired = center.add(offset.normalize().scale(39.0D));
                leapTarget = new Vec3(desired.x, leapGroundY, desired.z);
                bossHitThisCharge.clear();
                server.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                        SoundSource.HOSTILE, 2.5F, 0.65F);
            }
            double progress = Math.min(1.0D, elapsed / (double) flight);
            double arc = Math.sin(Math.PI * progress) * 10.0D;
            Vec3 point = leapOrigin.lerp(leapTarget, progress);
            setPos(point.x, leapGroundY + arc, point.z);
            // Ground warning indicates the actual impact position, rather than homing at the last moment.
            if (elapsed % 5 == 0 && elapsed < flight)
                server.sendParticles(ParticleTypes.CRIT, leapTarget.x, leapGroundY + 0.4D, leapTarget.z,
                        20, 3.5D, 0.1D, 3.5D, 0.08D);
            if (elapsed == flight) {
                server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.HOSTILE, 2.5F, 0.60F);
                server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5D, getZ(),
                        34, 5.0D, 0.4D, 5.0D, 0.1D);
                bossAreaImpact(BOSS_LEAP_DAMAGE, 8.0D, 1.75D);
                separateBossPlayers();
            }
        } else if (bossAttackTick == BOSS_LEAP_LAND_TICK + 1) {
            server.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND,
                    SoundSource.HOSTILE, 1.5F, 0.68F);
        }
    }

    /** Model spins rapidly while the train advances; no invisible full-length hits. */
    private void tickBossSpin(ServerLevel server, ServerPlayer target) {
        if (bossAttackTick <= BOSS_SPIN_WINDUP) {
            aimBossAt(target);
            if (bossAttackTick % 4 == 0)
                server.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.5D, getZ(),
                        12, 4.0D, 0.8D, 4.0D, 0.05D);
        } else if (bossAttackTick <= BOSS_SPIN_CHARGE_END) {
            if (bossAttackTick == BOSS_SPIN_WINDUP + 1) startBossDash(server);
            entityData.set(BOSS_SPIN, (entityData.get(BOSS_SPIN) + 31.0F) % 360.0F);
            if (bossAttackTick % 4 == 0) {
                Vec3 toward = target.position().subtract(position());
                if (toward.horizontalDistanceSqr() > 0.1D)
                    setHeading(heading.scale(0.87D).add(toward.normalize().scale(0.13D)));
            }
            Vec3 center = Vec3.atBottomCenterOf(TerminalBoss.ARENA);
            Vec3 proposed = position().add(heading.scale(0.82D));
            if (proposed.distanceToSqr(center) <= 47 * 47) setPos(proposed.x, getY(), proposed.z);
            bossAreaImpact(BOSS_SPIN_DAMAGE, 10.7D, 1.35D);
            if (bossAttackTick % 5 == 0)
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.2D, getZ(),
                        18, 5.0D, 0.7D, 5.0D, 0.07D);
        } else if (bossAttackTick == BOSS_SPIN_CHARGE_END + 1) {
            entityData.set(BOSS_SPIN, 0.0F);
            server.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND,
                    SoundSource.HOSTILE, 1.3F, 0.78F);
        }
    }

    /** A single impact per target per attack cycle, never a normal train kill(). */
    private void bossAreaImpact(float damage, double radius, double push) {
        AABB box = new AABB(getX() - radius, getY() - 3.0D, getZ() - radius,
                            getX() + radius, getY() + 4.0D, getZ() + radius);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            Vec3 diff = victim.position().subtract(position());
            if (diff.x * diff.x + diff.z * diff.z > radius * radius) continue;
            if (!bossHitThisCharge.add(victim.getUUID())) continue;
            if (victim.hurt(damageSources().generic(), damage)) {
                Vec3 away = new Vec3(diff.x, 0, diff.z);
                if (away.horizontalDistanceSqr() < 0.001D) away = heading;
                away = away.normalize();
                victim.push(away.x * push, 0.5D, away.z * push);
                victim.hurtMarked = true;
            }
        }
    }

    /** Prevent players from being dragged inside opaque, enlarged train meshes. */
    private void separateBossPlayers() {
        if (!isBossTrain() || !(level() instanceof ServerLevel server)) return;
        // While spinning, model sweeps a circle; otherwise use the rotated rectangular carriage.
        boolean spin = bossAttackPattern == 4 && bossAttackTick > BOSS_SPIN_WINDUP
                && bossAttackTick <= BOSS_SPIN_CHARGE_END;
        Vec3 right = new Vec3(-heading.z, 0, heading.x);
        for (ServerPlayer player : server.players()) {
            if (player.isSpectator() || !player.isAlive()
                    || player.getY() > getY() + BOSS_MAX_Y
                    || player.getY() + player.getBbHeight() < getY() + BOSS_MIN_Y) continue;
            Vec3 diff = player.position().subtract(position());
            double along = diff.dot(heading);
            double side = diff.dot(right);
            double radius = BOSS_HALF_LENGTH + 1.0D;
            if (spin) {
                if (diff.x * diff.x + diff.z * diff.z >= radius * radius) continue;
                Vec3 horizontal = new Vec3(diff.x, 0, diff.z);
                if (horizontal.lengthSqr() < 0.0001D) horizontal = right;
                Vec3 safe = position().add(horizontal.normalize().scale(radius + 0.9D));
                moveBossVictimOut(server, player, safe);
                continue;
            }
            if (Math.abs(along) >= BOSS_HALF_LENGTH + 0.35D
                    || Math.abs(side) >= BOSS_HALF_WIDTH + 0.4D) continue;
            double toSide = BOSS_HALF_WIDTH + 1.15D - Math.abs(side);
            double toEnd = BOSS_HALF_LENGTH + 0.95D - Math.abs(along);
            Vec3 safe = toSide < toEnd
                    ? player.position().add(right.scale(side >= 0 ? toSide : -toSide))
                    : player.position().add(heading.scale(along >= 0 ? toEnd : -toEnd));
            moveBossVictimOut(server, player, safe);
        }
    }

    private void moveBossVictimOut(ServerLevel server, ServerPlayer player, Vec3 safe) {
        Vec3 delta = safe.subtract(player.position());
        if (delta.horizontalDistanceSqr() < 0.015D) return;
        // Only move to a loaded, collision-free destination to avoid clipping into structures.
        if (!server.hasChunkAt(net.minecraft.core.BlockPos.containing(safe))) return;
        AABB destination = player.getBoundingBox().move(delta);
        if (!server.noCollision(player, destination)) return;
        player.teleportTo(safe.x, player.getY(), safe.z);
        player.setDeltaMovement(player.getDeltaMovement().x * 0.4D,
                Math.max(player.getDeltaMovement().y, 0.16D),
                player.getDeltaMovement().z * 0.4D);
        player.hurtMarked = true;
    }

    private void updateBossHealthForParticipants(ServerLevel server) {
        int participants = 0;
        for (ServerPlayer player : server.players()) {
            if (player.isAlive() && !player.isRemoved() && !player.isSpectator()
                    && !player.isCreative() && player.distanceToSqr(this) < BOSS_PARTICIPANT_RANGE_SQR) {
                participants++;
            }
        }
        float newMax = Math.min(BOSS_HEALTH_CAP, Math.max(1, participants) * BOSS_MAX_HEALTH);
        if (bossMaxHealth == newMax) return;
        // Maintain percentage remaining; joining cannot refill the boss or skip a trial.
        float fraction = Mth.clamp(durability / bossMaxHealth, 0.0F, 1.0F);
        bossMaxHealth = newMax;
        durability = Mth.clamp(fraction * bossMaxHealth, 0.0F, bossMaxHealth);
    }

    /** Remove a player's boss bar immediately even if the train's chunk is no longer ticking. */
    public static void clearBossBarsFor(ServerPlayer player) {
        for (TrainEntity boss : List.copyOf(BOSSES_WITH_BARS)) {
            if (boss.bossBar != null) boss.bossBar.removePlayer(player);
        }
    }

    private void updateBossBar(ServerLevel server) {
        if (bossBar == null) {
            bossBar = new ServerBossEvent(Component.translatable("entity.mod_e539c988.giant_train"),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
            bossBar.setDarkenScreen(false);
            BOSSES_WITH_BARS.add(this);
        }
        bossBar.setProgress(Mth.clamp(durability / bossMaxHealth, 0.0F, 1.0F));
        for (ServerPlayer tracked : List.copyOf(bossBar.getPlayers())) {
            if (tracked.level() != server || !tracked.isAlive() || tracked.isRemoved() || tracked.isSpectator()
                    || tracked.distanceToSqr(this) >= 100.0D * 100.0D)
                bossBar.removePlayer(tracked);
        }
        for (ServerPlayer player : server.players()) {
            if (player.isAlive() && !player.isRemoved() && !player.isSpectator()
                    && player.distanceToSqr(this) < BOSS_PARTICIPANT_RANGE_SQR)
                bossBar.addPlayer(player);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (bossBar != null) bossBar.removeAllPlayers();
        BOSSES_WITH_BARS.remove(this);
        super.remove(reason);
    }

    private void announceBossAttack(ServerLevel server, String translationKey) {
        for (ServerPlayer player : server.players()) {
            if (player.isAlive() && !player.isSpectator()
                    && player.distanceToSqr(this) <= 75.0D * 75.0D)
                player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }

    private void aimBossAt(ServerPlayer target) {
        Vec3 center = Vec3.atBottomCenterOf(TerminalBoss.ARENA);
        Vec3 direction = this.distanceToSqr(center) > 45.0D * 45.0D
                ? center.subtract(position()) : target.position().subtract(position());
        if (direction.horizontalDistanceSqr() > 0.01D) setHeading(direction);
    }

    private void startBossDash(ServerLevel server) {
        bossHitThisCharge.clear();
        server.playSound(null, blockPosition(), SoundEvents.MINECART_RIDING,
                SoundSource.HOSTILE, 3.0F, 0.7F);
    }

    private void dashBoss(double speed, float damage) {
        Vec3 proposed = position().add(heading.scale(speed));
        Vec3 center = Vec3.atBottomCenterOf(TerminalBoss.ARENA);
        if (proposed.distanceToSqr(center) <= 51.0D * 51.0D) {
            setPos(proposed.x, getY(), proposed.z);
            updateBossHitbox();
            damageBossChargeVictims(damage);
        }
    }

    private void damageBossChargeVictims(float damage) {
        AABB hitArea = getBoundingBox().inflate(0.2D, 0.15D, 0.2D);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, hitArea,
                living -> living.isAlive() && !(living instanceof Player player
                        && (player.isCreative() || player.isSpectator())))) {
            if (!touchesBossBody(victim)) continue;
            if (!bossHitThisCharge.add(victim.getUUID())) continue;
            if (victim.hurt(this.damageSources().generic(), damage)) {
                victim.push(heading.x * 1.35D, 0.30D, heading.z * 1.35D);
                victim.hurtMarked = true;
            }
        }
    }

    private void tickClientLerp() {
        if (clientLerpSteps <= 0) {
            return;
        }
        double progress = 1.0D / clientLerpSteps;
        double x = Mth.lerp(progress, this.getX(), clientLerpX);
        double y = Mth.lerp(progress, this.getY(), clientLerpY);
        double z = Mth.lerp(progress, this.getZ(), clientLerpZ);
        float yRot = this.getYRot() + (float) (Mth.wrapDegrees(clientLerpYRot - this.getYRot()) * progress);
        float xRot = Mth.lerp((float) progress, this.getXRot(), clientLerpXRot);
        this.setPos(x, y, z);
        this.setRot(yRot, xRot);
        updateHeadingFromRotation();
        clientLerpSteps--;
    }

    private void updateWildHeading() {
        if (getMode() == Mode.WILD && this.tickCount % 5 == 0) {
            AABB search = this.getBoundingBox().inflate(28.0D, 10.0D, 28.0D);
            this.level().getEntitiesOfClass(LivingEntity.class, search, this::isWildTarget)
                    .stream()
                    .min(Comparator.comparingDouble(this::distanceToSqr))
                    .ifPresent(target -> setHeading(target.position().subtract(position())));
        }
    }

    private boolean isWildTarget(LivingEntity living) {
        if (!living.isAlive() || living.getRootVehicle() == this) {
            return false;
        }
        return ownerId == null || !ownerId.equals(living.getUUID());
    }

    private Vec3 movementForMode() {
        if (!dilemmaWaypoints.isEmpty()) return scriptedDilemmaMovement();
        double verticalSpeed = getPhysicsMode() == PhysicsMode.VOID
                ? 0.0D
                : (this.onGround() ? 0.0D : this.getDeltaMovement().y - GRAVITY);
        if (getMode() == Mode.DRIVABLE) {
            return drivingMovement(verticalSpeed);
        }

        updateWildHeading();
        double speed = getMode() == Mode.WILD ? 0.95D : 1.25D;
        return heading.scale(speed).add(0.0D, verticalSpeed, 0.0D);
    }

    private Vec3 scriptedDilemmaMovement() {
        while (dilemmaWaypointIndex < dilemmaWaypoints.size()) {
            Vec3 difference = dilemmaWaypoints.get(dilemmaWaypointIndex).subtract(position());
            if (difference.lengthSqr() < 0.0025D) {
                dilemmaWaypointIndex++;
                continue;
            }
            setHeading(difference);
            // Never overshoot the turnout or corner. This also prevents lane-crossing
            // from accidentally killing captives on the unchosen branch.
            return difference.scale(Math.min((isSkyTrain() ? 0.55D : 1.25D) / difference.length(), 1.0D));
        }
        dilemmaRouteComplete = true;
        return Vec3.ZERO;
    }

    private Vec3 drivingMovement(double verticalSpeed) {
        // A real sideways recoil is independent of the train's forward-facing direction.
        // Briefly suspend steering/throttle so the cars visibly separate after a hit.
        if (bumperRecoilTicks > 0) {
            bumperRecoilTicks--;
            Vec3 movement = bumperRecoil;
            bumperRecoil = bumperRecoil.scale(0.80D);
            drivingSpeed *= 0.75D;
            return new Vec3(movement.x, verticalSpeed, movement.z);
        }
        if (getFirstPassenger() instanceof LivingEntity) {
            float steering = driverInputAge <= 5 ? driverSteering : 0.0F;
            float throttle = driverInputAge <= 5 ? driverThrottle : 0.0F;
            if (Math.abs(throttle) > 0.01F) {
                drivingSpeed += throttle * ACCELERATION;
            } else {
                drivingSpeed *= DRAG;
            }
            drivingSpeed = Math.max(-MAX_REVERSE_SPEED, Math.min(MAX_FORWARD_SPEED, drivingSpeed));

            if (Math.abs(steering) > 0.01F && Math.abs(drivingSpeed) > 0.02D) {
                float reverseDirection = drivingSpeed < 0.0D ? -1.0F : 1.0F;
                this.setYRot(this.getYRot() - steering * TURN_SPEED * reverseDirection);
                updateHeadingFromRotation();
            }
        } else {
            driverSteering = 0.0F;
            driverThrottle = 0.0F;
            drivingSpeed *= DRAG;
            if (Math.abs(drivingSpeed) < 0.005D) {
                drivingSpeed = 0.0D;
            }
        }
        return heading.scale(drivingSpeed).add(0.0D, verticalSpeed, 0.0D);
    }

    private void updateHeadingFromRotation() {
        double radians = Math.toRadians(this.getYRot());
        this.heading = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
    }

    private void checkRepeatedPasses() {
        if (awardedLoop || getMode() != Mode.DRIVABLE || drivingSpeed < 0.15D && drivingSpeed > -0.15D) return;
        if (!(getFirstPassenger() instanceof ServerPlayer passenger)) return;
        // Treat a position as an X/Z column; small vertical bumps do not break a loop.
        net.minecraft.core.BlockPos column = new net.minecraft.core.BlockPos(getBlockX(), 0, getBlockZ());
        if (column.equals(previousColumn)) return;
        previousColumn = column;
        int passes = visitedColumns.merge(column, 1, Integer::sum);
        if (passes >= 10) {
            awardedLoop = true;
            TrolleyAdvancements.award(passenger, "eternal_return");
        }
        // Bound server memory for someone driving endlessly without circling back.
        if (visitedColumns.size() > 4096) visitedColumns.clear();
    }

    /** Server-only PvP collision, using the actual rotated 8.5-by-2.1-block vehicle footprint. */
    private void bumpIntoOtherTrains() {
        if (!(level() instanceof ServerLevel server) || isRemoved()
                || !(getFirstPassenger() instanceof ServerPlayer) || bumperRecoilTicks > 0) return;
        // Find other driving trains via their drivers, not native entity collision boxes.
        // Drivable hitboxes are deliberately 1.25 blocks wide to fit on the rails;
        // querying physical boxes previously missed visible car-body contact.
        for (ServerPlayer opponent : server.players()) {
            if (!(opponent.getVehicle() instanceof TrainEntity other) || other == this
                    || other.isRemoved() || other.getMode() != Mode.DRIVABLE
                    || other.isBossTrain() || other.getFirstPassenger() != opponent) continue;
            if (Math.abs(other.getX() - getX()) > 11.0D
                    || Math.abs(other.getZ() - getZ()) > 11.0D
                    || Math.abs(other.getY() - getY()) > 2.7D) continue;
            if (bumperCooldown.getOrDefault(other.getUUID(), -1) > life
                    || other.bumperCooldown.getOrDefault(getUUID(), -1) > other.life
                    || other.bumperRecoilTicks > 0) continue;
            if (!touchesOtherBumper(other)) continue;

            Vec3 offset = other.position().subtract(position());
            Vec3 normal = new Vec3(offset.x, 0.0D, offset.z);
            normal = normal.lengthSqr() < 1.0E-4D ? heading : normal.normalize();
            Vec3 myVelocity = heading.scale(drivingSpeed);
            Vec3 otherVelocity = other.heading.scale(other.drivingSpeed);
            // A visible overlap should register even when the driver eases off the
            // throttle during contact. Ignore only two effectively parked vehicles.
            double closingSpeed = Math.max(0.0D, myVelocity.subtract(otherVelocity).dot(normal));
            if (Math.max(Math.abs(drivingSpeed), Math.abs(other.drivingSpeed)) < 0.055D) continue;

            bumperCooldown.put(other.getUUID(), life + BUMPER_COOLDOWN_TICKS);
            other.bumperCooldown.put(getUUID(), other.life + BUMPER_COOLDOWN_TICKS);
            double myMomentum = Math.abs(drivingSpeed);
            double otherMomentum = Math.abs(other.drivingSpeed);
            // More momentum deals more damage to the opponent than to yourself.
            // This breaks ties between different driving styles and rewards good rams.
            float damageToMe = Mth.clamp((float) ((1.5D + closingSpeed * 3.0D
                    + otherMomentum * 7.0D) * (other.isFlingDriveMode() ? 1.12D : 1.0D)), 2.0F, 14.0F);
            float damageToOther = Mth.clamp((float) ((1.5D + closingSpeed * 3.0D
                    + myMomentum * 7.0D) * (isFlingDriveMode() ? 1.12D : 1.0D)), 2.0F, 14.0F);
            durability = Math.max(0.0F, durability - damageToMe);
            other.durability = Math.max(0.0F, other.durability - damageToOther);

            // Do not move either vehicle here: the other train may still be in its own
            // server tick, and directly moving it here makes the mounted driver's
            // position jump before normal passenger synchronisation. Instead record
            // a bounded recoil that each train applies exactly once in its own tick.
            // The visual train still bounces apart over the next several ticks.
            bumperRecoil = normal.scale(-0.52D);
            other.bumperRecoil = normal.scale(0.52D);
            bumperRecoilTicks = BUMPER_RECOIL_TICKS;
            other.bumperRecoilTicks = BUMPER_RECOIL_TICKS;
            drivingSpeed *= -0.12D;
            other.drivingSpeed *= -0.12D;

            double cx = (getX() + other.getX()) * 0.5D;
            double cy = (getY() + other.getY()) * 0.5D + 1.1D;
            double cz = (getZ() + other.getZ()) * 0.5D;
            // Visual-only explosion; never breaks blocks, burns the station or hurts drivers.
            server.sendParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 2, 0.5D, 0.3D, 0.5D, 0.0D);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz,
                    22, 1.1D, 0.7D, 1.1D, 0.24D);
            server.sendParticles(ParticleTypes.CLOUD, cx, cy, cz,
                    14, 0.75D, 0.3D, 0.75D, 0.17D);
            server.playSound(null, new net.minecraft.core.BlockPos(Mth.floor(cx), Mth.floor(cy), Mth.floor(cz)),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4F, 1.2F);

            showBumperHealth(other);
            other.showBumperHealth(this);
            if (durability <= 0.0F) finishBumperTrain(server);
            if (other.durability <= 0.0F) other.finishBumperTrain(server);
            return;
        }
    }

    /** Separating-axis test for the two rotated visual train footprints. */
    private boolean touchesOtherBumper(TrainEntity other) {
        if (Math.abs(getY() - other.getY()) > 2.4D) return false;
        Vec3 delta = other.position().subtract(position());
        Vec3 mySide = new Vec3(-heading.z, 0.0D, heading.x);
        Vec3 otherSide = new Vec3(-other.heading.z, 0.0D, other.heading.x);
        return overlapsBumperAxis(delta, heading, other)
                && overlapsBumperAxis(delta, mySide, other)
                && overlapsBumperAxis(delta, other.heading, other)
                && overlapsBumperAxis(delta, otherSide, other);
    }

    private boolean overlapsBumperAxis(Vec3 delta, Vec3 axis, TrainEntity other) {
        double thisRadius = BUMPER_HALF_LENGTH * Math.abs(heading.dot(axis))
                + BUMPER_HALF_WIDTH * Math.abs(new Vec3(-heading.z, 0, heading.x).dot(axis));
        double otherRadius = BUMPER_HALF_LENGTH * Math.abs(other.heading.dot(axis))
                + BUMPER_HALF_WIDTH * Math.abs(new Vec3(-other.heading.z, 0, other.heading.x).dot(axis));
        return Math.abs(delta.dot(axis)) <= thisRadius + otherRadius + BUMPER_CONTACT_PADDING;
    }

    private void showBumperHealth(TrainEntity opponent) {
        if (getFirstPassenger() instanceof ServerPlayer driver) {
            driver.displayClientMessage(Component.translatable("message.mod_e539c988.bumper_health",
                    Mth.ceil(durability), (int) MAX_DURABILITY,
                    Mth.ceil(opponent.durability), (int) MAX_DURABILITY), true);
        }
    }

    /** Drop the original 14 portal blocks exactly once on durability destruction.
     * Natural timeout/despawn and visual-only trains must not generate items. */
    private void dropDestroyedTrainBlocks() {
        if (level().isClientSide() || isRemoved() || isBossTrain() || isSkyTrain() || isSkyfall()) return;
        level().addFreshEntity(new ItemEntity(level(), getX(), getY() + 0.65D, getZ(),
                new ItemStack(GeneratedMod.TROLLEY_BLOCK_ITEM.get(), 14)));
    }

    /** An eliminated bumper car explodes safely and throws its driver clear. */
    private void finishBumperTrain(ServerLevel server) {
        if (isRemoved()) return;
        if (getFirstPassenger() instanceof LivingEntity driver) {
            if (driver instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.mod_e539c988.bumper_destroyed"), true);
                // Avoid having the still-moving winning train kill the freshly ejected driver.
                player.getPersistentData().putLong(BUMPER_EJECTION_GRACE, server.getGameTime() + 40L);
            }
            ejectPassengers();
            Vec3 direction = bumperRecoil.lengthSqr() > 0.001D ? bumperRecoil.normalize() : heading;
            // Moderate launch prevents the client-side anti-cheat/vehicle correction
            // from seeing a sudden large change in the driver's movement.
            driver.setDeltaMovement(direction.x * 0.75D, 0.48D, direction.z * 0.75D);
            driver.hurtMarked = true;
            driver.fallDistance = 0;
            driver.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SLOW_FALLING, 100, 0, false, false));
        }
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.9D, getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.9D, getZ(),
                5, 1.65D, 0.65D, 1.65D, 0.0D);
        server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0D, getZ(),
                34, 2.0D, 1.0D, 2.0D, 0.19D);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.2D, getZ(),
                16, 1.6D, 0.75D, 1.6D, 0.09D);
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 2.0F, 0.7F);
        dropDestroyedTrainBlocks();
        discard();
    }

    /** An ordinary summoned trolley cannot repeatedly damage the same player after respawn. */
    private void crushLivingEntities() {
        if (isBossTrain()) return;
        AABB area = getBoundingBox().inflate(2.1D, 0.4D, 2.1D);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive() && living.getRootVehicle() != this)) {
            if (ownerId != null && ownerId.equals(victim.getUUID())) continue;
            if (victim instanceof Player player && player.getPersistentData().getLong(BUMPER_EJECTION_GRACE)
                    > level().getGameTime()) continue;
            // Two occupied train vehicles are handled by the bumper-car collision code.
            if (victim.getRootVehicle() instanceof TrainEntity other && other.getMode() == Mode.DRIVABLE) continue;
            boolean fling = getMode() == Mode.FLING || (getMode() == Mode.DRIVABLE && isFlingDriveMode());
            if (fling) {
                if (getMode() == Mode.DRIVABLE) {
                    if (flingTargetCooldown.getOrDefault(victim.getUUID(), -1) > life) continue;
                    flingTargetCooldown.put(victim.getUUID(), life + 26);
                } else if (!alreadyFlungLiving.add(victim.getUUID())) continue;
                // Cooldown/once-per-train tracking above prevents damage on every tick.
                // Normal summon: one hit per target; driven fling: one hit per 26 ticks.
                victim.hurt(this.damageSources().generic(), FLING_HIT_DAMAGE);
                if (victim.isAlive()) launchVictim(victim);
                continue;
            }
            if (victim instanceof Player && getMode() != Mode.DRIVABLE
                    && !alreadyHitPlayers.add(victim.getUUID())) continue;
            Vec3 deathPosition = victim.position();
            victim.kill();
            if (victim instanceof ServerPlayer player && !player.isAlive()) {
                TrolleyAdvancements.award(player, "part_of_problem");
            }
            notifyBioethicists(deathPosition, victim.getDisplayName());
        }
    }

    /** Strong launch velocity and slow falling; the impact damage is applied before this effect. */
    private void launchVictim(LivingEntity victim) {
        Vec3 difference = victim.position().subtract(position());
        Vec3 away = new Vec3(difference.x, 0.0D, difference.z);
        Vec3 pushDir = heading.scale(0.8D).add(
                away.lengthSqr() < 1.0E-4D ? Vec3.ZERO : away.normalize().scale(0.45D));
        if (pushDir.lengthSqr() < 0.001D) pushDir = new Vec3(0.0D, 0.0D, 1.0D);
        pushDir = pushDir.normalize();
        victim.setDeltaMovement(pushDir.x * 2.75D, 1.35D, pushDir.z * 2.75D);
        victim.hurtMarked = true;
        // Only players get a safety parachute. Mobs should follow normal fall physics.
        if (victim instanceof Player) {
            victim.fallDistance = 0;
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SLOW_FALLING, 130, 0, false, false));
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 0.9D,
                    victim.getZ(), 4, 0.65D, 0.5D, 0.65D, 0.0D);
            server.sendParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY() + 0.8D,
                    victim.getZ(), 34, 0.8D, 0.7D, 0.8D, 0.19D);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + 0.7D,
                    victim.getZ(), 25, 1.0D, 1.0D, 1.0D, 0.25D);
            server.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.15F, 1.4F);
        }
    }

    private void notifyBioethicists(Vec3 eventPosition, Component victimName) {
        AABB witnessRange = new AABB(eventPosition, eventPosition).inflate(24.0D);
        for (Villager villager : this.level().getEntitiesOfClass(Villager.class, witnessRange,
                candidate -> candidate.getPersistentData().getBoolean(BIOETHICIST_TAG))) {
            if (villager.getRandom().nextFloat() > 0.75F) {
                continue;
            }
            String key = COMMENTS[villager.getRandom().nextInt(COMMENTS.length)];
            Component comment = Component.translatable(key, villager.getDisplayName(), victimName);
            if (this.level() instanceof ServerLevel serverLevel) {
                for (ServerPlayer player : serverLevel.players()) {
                    if (player.distanceToSqr(villager) <= 32.0D * 32.0D) {
                        player.sendSystemMessage(comment);
                    }
                }
            }
            villager.playSound(SoundEvents.VILLAGER_AMBIENT, 1.0F, 0.8F);
        }
    }

    private void makeClientEffects() {
        if (isSkyTrain() || isSkyfall()) return; // Scenery has no smoke particles.
        if (getMode() == Mode.DRIVABLE && (getX() - xo) * (getX() - xo)
                + (getZ() - zo) * (getZ() - zo) < MOVING_SOUND_THRESHOLD_SQR) return;
        if ((getMode() == Mode.FLING || getMode() == Mode.DRIVABLE && isFlingDriveMode())
                && this.tickCount % 2 == 0) {
            Vec3 trail = this.position().subtract(this.heading.scale(2.1D));
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    trail.x, trail.y + 1.05D, trail.z, 0, 0.11D, 0);
            this.level().addParticle(ParticleTypes.CLOUD,
                    trail.x, trail.y + 0.5D, trail.z, 0, 0.01D, 0);
        }
        if (this.tickCount % 2 == 0) {
            Vec3 rear = this.position().subtract(this.heading.scale(2.8D));
            this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    rear.x, rear.y + 0.3D, rear.z, 0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (getMode() == Mode.DRIVABLE && !player.isSecondaryUseActive() && getFirstPassenger() == null) {
            if (!this.level().isClientSide() && player.startRiding(this, true)) {
                // Each new driving session starts in fling mode, including saved trains.
                // The driver can immediately switch back to normal with the V key.
                entityData.set(FLING_DRIVE, true);
                lastFlingSwitchTick = life - 8;
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getMode() == Mode.DRIVABLE && getPassengers().isEmpty();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        // Critical: this is a server-authoritative custom vehicle. The actual
        // rider is tracked by getFirstPassenger(), and W/A/S/D/V commands arrive
        // through the validated TrainInputPayload handler. Claiming the rider as
        // the vanilla controlling passenger also enables the vanilla vehicle
        // position upload/validation path; that can compete with our own server
        // movement, especially when bumper recoil rapidly changes direction.
        // Returning null opts out of vanilla client-authoritative vehicle motion
        // without dismounting the rider or disabling our custom input controls.
        return null;
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        double radians = Math.toRadians(this.getYRot());
        Vec3 facing = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
        return position().add(facing.scale(-2.1D)).add(0.0D, 0.72D, 0.0D);
    }

    @Override
    public boolean isPickable() {
        return !isSkyTrain() && !isSkyfall();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isSkyTrain() || isSkyfall()) return false; // Visual trains cannot be farmed.
        if (isBossTrain() && trialPending) return false;
        if (isRemoved() || isInvulnerableTo(source) || amount <= 0.0F) {
            return false;
        }
        if (!this.level().isClientSide()) {
            durability -= Math.min(amount, isBossTrain() ? 8.0F : MAX_DAMAGE_PER_HIT);
            this.markHurt();
            if (isBossTrain() && source.getEntity() instanceof ServerPlayer attacker) {
                attacker.displayClientMessage(Component.literal("§c至尊电车耐久 §f" +
                        Math.max(0, (int) Math.ceil(durability)) + "§7/" + (int) bossMaxHealth), true);
            }
            if (isBossTrain() && durability > 0.0F) {
                if (bossPhase == 0 && durability <= bossMaxHealth * (2.0F / 3.0F)) {
                    bossPhase = 1;
                    trialPending = true;
                } else if (bossPhase == 1 && durability <= bossMaxHealth / 3.0F) {
                    bossPhase = 2;
                    trialPending = true;
                }
            }
            if (durability > 0.0F) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND,
                        SoundSource.NEUTRAL, 0.8F, 1.3F);
            } else {
                if (getMode() == Mode.DRIVABLE && level() instanceof ServerLevel server) {
                    finishBumperTrain(server);
                    return true;
                }
                ejectPassengers();
                // Only the once-per-dimension boss grants the supreme plunger.
                if (isBossTrain() && level() instanceof ServerLevel server) {
                    TerminalBoss.defeated(server, this);
                    level().addFreshEntity(new ItemEntity(level(), getX() - 0.35D, getY() + 0.65D, getZ(),
                            new ItemStack(GeneratedMod.SUPREME_PLUNGER.get())));
                    level().addFreshEntity(new ItemEntity(level(), getX() + 0.35D, getY() + 0.65D, getZ(),
                            new ItemStack(GeneratedMod.WHITE_TICKET.get())));
                } else {
                    dropDestroyedTrainBlocks();
                }
                this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_DESTROY,
                        SoundSource.NEUTRAL, 1.0F, 0.8F);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0D, this.getZ(),
                            24, 1.5D, 0.9D, 1.5D, 0.08D);
                }
                discard();
            }
        }
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Mode", getMode().ordinal());
        tag.putBoolean("FlingDrive", isFlingDriveMode());
        ListTag hitPlayersTag = new ListTag();
        for (UUID uuid : alreadyHitPlayers) {
            CompoundTag hit = new CompoundTag();
            hit.putUUID("Id", uuid);
            hitPlayersTag.add(hit);
        }
        tag.put("HitPlayers", hitPlayersTag);
        tag.putInt("PhysicsMode", getPhysicsMode().ordinal());
        tag.putBoolean("SkyTrain", isSkyTrain());
        tag.putBoolean("BossTrain", isBossTrain());
        tag.putBoolean("Skyfall", isSkyfall());
        tag.putDouble("SkyfallGroundY", skyfallGroundY);
        tag.putInt("BossPhase", bossPhase);
        tag.putInt("BossAttackTick", bossAttackTick);
        tag.putInt("BossAttackPattern", bossAttackPattern);
        tag.putBoolean("BossHealthScaleV2", true);
        tag.putFloat("BossMaxHealth", bossMaxHealth);
        tag.putBoolean("TrialPending", trialPending);
        if (trialPoint != null) {
            tag.putInt("TrialX", trialPoint.x()); tag.putInt("TrialY", trialPoint.y());
            tag.putInt("TrialZ", trialPoint.z());
        }
        tag.putInt("Life", life);
        tag.putFloat("Durability", durability);
        tag.putDouble("DrivingSpeed", drivingSpeed);
        tag.putInt("DriverlessTicks", driverlessTicks);
        tag.putDouble("HeadingX", heading.x);
        tag.putDouble("HeadingZ", heading.z);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        if (!dilemmaWaypoints.isEmpty()) {
            ListTag points = new ListTag();
            for (Vec3 point : dilemmaWaypoints) {
                CompoundTag p = new CompoundTag();
                p.putDouble("X", point.x);
                p.putDouble("Y", point.y);
                p.putDouble("Z", point.z);
                points.add(p);
            }
            tag.put("DilemmaWaypoints", points);
            tag.putInt("DilemmaWaypointIndex", dilemmaWaypointIndex);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setSkyTrain(tag.getBoolean("SkyTrain"));
        entityData.set(FLING_DRIVE, tag.getBoolean("FlingDrive"));
        alreadyHitPlayers.clear();
        ListTag hits = tag.getList("HitPlayers", 10);
        for (int i = 0; i < hits.size() && i < 256; ++i) {
            CompoundTag entry = hits.getCompound(i);
            if (entry.hasUUID("Id")) alreadyHitPlayers.add(entry.getUUID("Id"));
        }
        entityData.set(BOSS_TRAIN, tag.getBoolean("BossTrain"));
        refreshDimensions();
        entityData.set(SKYFALL, tag.getBoolean("Skyfall"));
        skyfallGroundY = tag.getDouble("SkyfallGroundY");
        bossPhase = tag.getInt("BossPhase");
        bossAttackPattern = Mth.clamp(tag.getInt("BossAttackPattern"), 0, 4);
        bossAttackTick = 0; // Restart attacks safely after loading (especially aerial attacks).
        trialPending = tag.getBoolean("TrialPending");
        if (tag.contains("TrialY")) trialPoint = new BlockPosHolder(
                tag.getInt("TrialX"), tag.getInt("TrialY"), tag.getInt("TrialZ"));
        int savedMode = tag.getInt("Mode");
        setMode(savedMode >= 0 && savedMode < Mode.values().length ? Mode.values()[savedMode] : Mode.AUTOMATIC);
        int savedPhysicsMode = tag.getInt("PhysicsMode");
        setPhysicsMode(savedPhysicsMode >= 0 && savedPhysicsMode < PhysicsMode.values().length
                ? PhysicsMode.values()[savedPhysicsMode] : PhysicsMode.VOID);
        life = tag.getInt("Life");
        bossMaxHealth = isBossTrain() && tag.contains("BossMaxHealth")
                ? Mth.clamp(tag.getFloat("BossMaxHealth"), BOSS_MAX_HEALTH, BOSS_HEALTH_CAP)
                : BOSS_MAX_HEALTH;
        durability = tag.contains("Durability")
                ? Mth.clamp(tag.getFloat("Durability"), 0.0F, isBossTrain() ? bossMaxHealth : MAX_DURABILITY)
                : (isBossTrain() ? bossMaxHealth : MAX_DURABILITY);
        if (isBossTrain() && !tag.getBoolean("BossHealthScaleV2"))
            durability = Math.min(bossMaxHealth, durability * (BOSS_MAX_HEALTH / 160.0F));
        if (isBossTrain()) setCustomNameVisible(false);
        drivingSpeed = tag.getDouble("DrivingSpeed");
        driverlessTicks = Mth.clamp(tag.getInt("DriverlessTicks"), 0, EMPTY_DRIVER_DESPAWN_TICKS - 1);
        setHeading(new Vec3(tag.getDouble("HeadingX"), 0.0D, tag.getDouble("HeadingZ")));
        if (tag.hasUUID("Owner")) {
            ownerId = tag.getUUID("Owner");
        }
        dilemmaWaypoints.clear();
        ListTag points = tag.getList("DilemmaWaypoints", 10);
        for (int i = 0; i < points.size(); i++) {
            CompoundTag p = points.getCompound(i);
            dilemmaWaypoints.add(new Vec3(p.getDouble("X"), p.getDouble("Y"), p.getDouble("Z")));
        }
        dilemmaWaypointIndex = Mth.clamp(tag.getInt("DilemmaWaypointIndex"), 0, dilemmaWaypoints.size());
        updateBossHitbox();
    }
}
