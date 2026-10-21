package cn.blockforge.generated.mode539c988.dimension;

import cn.blockforge.generated.mode539c988.dilemma.DilemmaStructures;
import cn.blockforge.generated.mode539c988.dilemma.DilemmaVariant;
import cn.blockforge.generated.mode539c988.dilemma.DilemmaWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.Random;

/** White-space set pieces, generated only inside loaded chunks and recorded in world save. */
public final class TerminalStructures {
    private static final int CELL = 112;
    private TerminalStructures() { }

    public static void ensureLobby(ServerLevel level, TerminalWorldData data) {
        if (!data.lobbyBuilt) {
            // The arrival station is loaded deliberately before dimension transfer.
            if (isOriginalWhiteFloor(level, 0, 0)) {
                station(level, 0, 0, true);
                data.railSites.add(new TerminalWorldData.RailSite(0, 0, true, 35));
            }
            data.lobbyBuilt = true;
            data.setDirty();
        }
        // Upgrade existing r17 worlds: install the exit portal only once, without
        // overwriting user builds. The chosen location is inside spawn's loaded chunk.
        if (!data.returnPortalBuilt && PortalMechanics.installLobbyReturnPortal(level)) {
            data.returnPortalBuilt = true;
            data.setDirty();
        }
    }

    public static void aroundPlayer(ServerLevel level, TerminalWorldData data, BlockPos position) {
        int cx = Math.floorDiv(position.getX(), CELL);
        int cz = Math.floorDiv(position.getZ(), CELL);
        // Nine cells, once per 3 seconds. Sites farther than 125 blocks wait until approached.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int ix = cx + dx, iz = cz + dz;
                long key = TerminalWorldData.cellKey(ix, iz);
                if (data.generatedCells.contains(key)) continue;
                Random random = new Random(level.getSeed() ^ (ix * 341873128712L) ^ (iz * 132897987541L));
                int choice = random.nextInt(10);
                int x = ix * CELL + CELL / 2;
                int z = iz * CELL + CELL / 2;
                int ddx = x - position.getX(), ddz = z - position.getZ();
                if (ddx * ddx + ddz * ddz > 125 * 125) continue;
                if (ddx * ddx + ddz * ddz < 34 * 34) continue; // Never pop a setpiece on top of a player.
                // The enlarged city is ~68x68 blocks; keep clear of adjacent 112-block cells.
                int radius = choice == 9 ? 38 : choice == 8 ? 36 : choice >= 6 ? 39 : 42;
                if (!loadedSquare(level, x, z, radius)) continue;
                // The unique boss occupies the two cells bordering its central arena.
                // Reserve these cells rather than spawning a city across the fight.
                boolean reservedBossCell = iz == 2 && (ix == -1 || ix == 0);
                if (!reservedBossCell && !freeSquare(level, x, z, radius)) {
                    // Respect existing builds. Mark this cell so it isn't retried indefinitely.
                    data.generatedCells.add(key);
                    data.setDirty();
                    continue;
                }
                if (reservedBossCell) {
                    // Empty plaza; connected track is still added after the cell is recorded.
                } else if (choice == 9) {
                    // Reuse all existing dilemma variants, countdowns, trains, rewards and captions.
                    DilemmaWorldData dilemmas = DilemmaWorldData.get(level);
                    DilemmaWorldData.Scenario request = new DilemmaWorldData.Scenario(
                            new BlockPos(x + 22, 1, z), Direction.SOUTH.get2DDataValue(), DilemmaVariant.random(level.random));
                    var ready = DilemmaStructures.resolveSite(level, request, true);
                    if (ready == null) continue; // May need neighboring chunks to finish loading.
                    DilemmaStructures.build(level, ready);
                    dilemmas.scenarios.add(ready);
                    dilemmas.setDirty();
                } else if (choice == 8) {
                    city(level, x, z, random);
                    data.railSites.add(new TerminalWorldData.RailSite(x, z, true, 30));
                } else if (choice >= 6) {
                    station(level, x, z, false);
                    data.railSites.add(new TerminalWorldData.RailSite(x, z, true, 35));
                } else if (choice >= 3) {
                    rail(level, x, z, true, 38);
                    data.railSites.add(new TerminalWorldData.RailSite(x, z, true, 38));
                }
                // 30% empty white space is intentional.
                data.generatedCells.add(key);
                data.setDirty();
            }
        }
        // Keep long, parallel north/south routes. Do not generate railway crossings:
        // vanilla rails cannot carry a true four-way junction, and trains clip through
        // one another at the old east/west connectors.
        // Existing r21 saves are migrated ONCE when each cell is loaded.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int ix = cx + dx, iz = cz + dz;
                long key = TerminalWorldData.cellKey(ix, iz);
                if (!data.generatedCells.contains(key)) continue;
                int x = ix * CELL + CELL / 2, z = iz * CELL + CELL / 2;
                if (!loadedSquare(level, x, z, 57)) continue;
                if (!data.railCells.contains(key)) {
                    railNetworkCell(level, ix, iz);
                    data.railCells.add(key);
                    data.normalizedRailCells.add(key);
                    data.setDirty();
                } else if (!data.normalizedRailCells.contains(key)) {
                    removeLegacyCrossings(level, data, ix, iz);
                    data.normalizedRailCells.add(key);
                    data.setDirty();
                }
            }
        }
        // Upgrade previously generated railway sites only once per saved world.
        // The old loop rebuilt every nearby site for EVERY player every 60 ticks,
        // causing repeated block-update traffic and server stalls during PvP.
        for (TerminalWorldData.RailSite site : data.railSites) {
            long siteKey = TerminalWorldData.cellKey(site.x(), site.z());
            if (data.decoratedRailSites.contains(siteKey)) continue;
            int ddx2 = site.x() - position.getX(), ddz2 = site.z() - position.getZ();
            if (ddx2 * ddx2 + ddz2 * ddz2 > 140 * 140) continue;
            if (!loadedSquare(level, site.x(), site.z(), site.halfLength() + 8)) continue;
            rail(level, site.x(), site.z(), site.alongZ(), site.halfLength());
            data.decoratedRailSites.add(siteKey);
            data.setDirty();
        }
        // The guaranteed line from the arrival platform to the only boss arena.
        bossApproach(level, position);
    }

    private static boolean loadedSquare(ServerLevel level, int x, int z, int radius) {
        for (int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++) {
            for (int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++) {
                if (!level.hasChunkAt(new BlockPos(cx << 4, 1, cz << 4))) return false;
            }
        }
        return true;
    }

    private static boolean isOriginalWhiteFloor(ServerLevel level, int x, int z) {
        return level.getBlockState(new BlockPos(x, -1, z)).is(Blocks.WHITE_CONCRETE)
                && level.getBlockState(new BlockPos(x, 1, z)).isAir();
    }

    private static boolean freeSquare(ServerLevel level, int x, int z, int radius) {
        // Sparse broad checks plus high points: this prevents replacing existing player construction.
        for (int u = -radius; u <= radius; u += 4) {
            for (int v = -radius; v <= radius; v += 4) {
                if (!isOriginalWhiteFloor(level, x + u, z + v)) return false;
                if (!level.getBlockState(new BlockPos(x + u, 5, z + v)).isAir()) return false;
            }
        }
        return isOriginalWhiteFloor(level, x, z);
    }

    private static void put(ServerLevel level, int x, int y, int z, BlockState state) {
        // A worldgen pass is not a reason to re-send unchanged blocks to every player.
        // Particularly important when multiple players explore the same loaded area.
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos).equals(state)) level.setBlock(pos, state, 2);
    }

    private static void putIfAir(ServerLevel level, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).isAir()) level.setBlock(pos, state, 2);
    }

    private static void rail(ServerLevel level, int x, int z, boolean alongZ, int half) {
        RailShape shape = alongZ ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;
        for (int t = -half; t <= half; t++) {
            int atX = x + (alongZ ? 0 : t), atZ = z + (alongZ ? t : 0);
            ballast(level, atX, atZ);
            put(level, atX, 1, atZ, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape));
            if (t % 6 == 0) {
                for (int side : new int[]{-1, 1}) {
                    put(level, atX + (alongZ ? side : 0), 0,
                            atZ + (alongZ ? 0 : side), Blocks.POLISHED_ANDESITE.defaultBlockState());
                }
            }
            if (Math.floorMod(t, 14) == 0) {
                catenaryPole(level, atX, atZ, alongZ, 1);
                catenaryPole(level, atX, atZ, alongZ, -1);
            }
            if (Math.floorMod(t + 7, 20) == 0) {
                signalPair(level, atX, atZ, alongZ);
            }
            if (Math.floorMod(t + 3, 28) == 0) {
                switchLever(level, atX, atZ, alongZ);
            }
        }
    }

    private static void ballast(ServerLevel level, int x, int z) {
        // Give tracks visible brick/stone support instead of floating directly above the white plane.
        put(level, x, -1, z, Blocks.STONE_BRICKS.defaultBlockState());
        put(level, x, 0, z, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
        for (int side : new int[]{-1, 1}) {
            put(level, x + side, -1, z, Blocks.STONE_BRICKS.defaultBlockState());
            put(level, x + side, 0, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
        }
    }

    private static void catenaryPole(ServerLevel level, int x, int z, boolean alongZ, int side) {
        int baseX = x + (alongZ ? side * 3 : 0);
        int baseZ = z + (alongZ ? 0 : side * 3);
        for (int y = 0; y <= 5; y++) putIfAir(level, baseX, y, baseZ, Blocks.GRAY_CONCRETE.defaultBlockState());
        if (alongZ) {
            for (int dx = -2; dx <= 2; dx++) putIfAir(level, baseX + dx, 5, baseZ, Blocks.IRON_BARS.defaultBlockState());
            for (int dx = -2; dx <= 0; dx++) putIfAir(level, x + dx, 4, z, Blocks.CHAIN.defaultBlockState());
            for (int dx = 0; dx <= 2; dx++) putIfAir(level, x + dx, 4, z, Blocks.CHAIN.defaultBlockState());
        } else {
            for (int dz = -2; dz <= 2; dz++) putIfAir(level, baseX, 5, baseZ + dz, Blocks.IRON_BARS.defaultBlockState());
            for (int dz = -2; dz <= 0; dz++) putIfAir(level, x, 4, z + dz, Blocks.CHAIN.defaultBlockState());
            for (int dz = 0; dz <= 2; dz++) putIfAir(level, x, 4, z + dz, Blocks.CHAIN.defaultBlockState());
        }
    }

    private static void signalPair(ServerLevel level, int x, int z, boolean alongZ) {
        signalMast(level, x, z, alongZ, 1);
        signalMast(level, x, z, alongZ, -1);
    }

    private static void signalMast(ServerLevel level, int x, int z, boolean alongZ, int side) {
        int sx = x + (alongZ ? side * 2 : 0);
        int sz = z + (alongZ ? 0 : side * 2);
        for (int y = 0; y <= 3; y++) putIfAir(level, sx, y, sz, Blocks.POLISHED_ANDESITE.defaultBlockState());
        putIfAir(level, sx, 4, sz, Blocks.SEA_LANTERN.defaultBlockState());
        if (alongZ) {
            putIfAir(level, sx, 4, sz - 1, Blocks.GREEN_STAINED_GLASS_PANE.defaultBlockState());
            putIfAir(level, sx, 5, sz - 1, Blocks.RED_STAINED_GLASS_PANE.defaultBlockState());
        } else {
            putIfAir(level, sx - 1, 4, sz, Blocks.GREEN_STAINED_GLASS_PANE.defaultBlockState());
            putIfAir(level, sx - 1, 5, sz, Blocks.RED_STAINED_GLASS_PANE.defaultBlockState());
        }
    }

    private static void switchLever(ServerLevel level, int x, int z, boolean alongZ) {
        int lx = x + (alongZ ? 2 : 0);
        int lz = z + (alongZ ? 0 : 2);
        putIfAir(level, lx, 0, lz, Blocks.POLISHED_ANDESITE.defaultBlockState());
        putIfAir(level, lx, 1, lz, Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, alongZ ? Direction.EAST : Direction.SOUTH));
    }

    private static void safeTrack(ServerLevel level, int x, int z, RailShape shape) {
        BlockPos at = new BlockPos(x, 1, z);
        // Do not tear out existing player constructions or blocks in stations.
        if (!level.getBlockState(at).isAir() && !level.getBlockState(at).is(Blocks.RAIL)) return;
        ballast(level, x, z);
        BlockState railState = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape);
        if (!level.getBlockState(at).equals(railState)) put(level, x, 1, z, railState);
        // Add sparse railway dressing to the always-generated network too.
        if (Math.floorMod(z + x, 16) == 0) {
            boolean alongZ = shape == RailShape.NORTH_SOUTH;
            catenaryPole(level, x, z, alongZ, 1);
            catenaryPole(level, x, z, alongZ, -1);
        }
        if (Math.floorMod(z + x, 24) == 0) {
            signalPair(level, x, z, shape == RailShape.NORTH_SOUTH);
        }
    }

    /** A single north/south line per column. No crossing tracks or junctions. */
    private static void railNetworkCell(ServerLevel level, int ix, int iz) {
        int x = ix * CELL + CELL / 2, z = iz * CELL + CELL / 2;
        for (int dz = -56; dz <= 56; dz++) {
            safeTrack(level, x, z + dz, RailShape.NORTH_SOUTH);
        }
    }

    /**
     * r21-r21.3 placed an east/west line at z + 48 of every generated cell.
     * Remove only its distinctive rail shape atop its generated light-gray ballast.
     * This avoids tearing down unrelated player tracks or structures.
     */
    private static void removeLegacyCrossings(ServerLevel level, TerminalWorldData data, int ix, int iz) {
        int x = ix * CELL + CELL / 2, z = iz * CELL + CELL / 2;
        for (int dx = -56; dx <= 56; dx++) {
            if (dx != 0) removeGeneratedHorizontal(level, x + dx, z + 48);
        }
        // The original connector code sometimes overwrote a north/south rail with
        // an east/west rail at the actual station. Restore only recorded N/S sites.
        for (TerminalWorldData.RailSite site : data.railSites) {
            if (!site.alongZ() || Math.abs(site.x() - x) > 10 || Math.abs(site.z() - z) > 10) continue;
            int joinZ = site.z() + Math.min(site.halfLength(), 35);
            if (joinZ >= z + 42) joinZ = site.z() - Math.min(site.halfLength(), 30);
            int left = Math.min(site.x(), x), right = Math.max(site.x(), x);
            for (int xx = left; xx <= right; xx++) {
                if (xx != site.x() && xx != x) removeGeneratedHorizontal(level, xx, joinZ);
            }
            // The site and grid lines both follow Z here; restore straight rails.
            restoreNorthSouth(level, site.x(), joinZ);
            restoreNorthSouth(level, x, joinZ);
        }
        // Keep the original north/south track continuous across the old cross-row.
        safeTrack(level, x, z + 48, RailShape.NORTH_SOUTH);
    }

    private static void removeGeneratedHorizontal(ServerLevel level, int x, int z) {
        BlockPos at = new BlockPos(x, 1, z);
        BlockState rail = level.getBlockState(at);
        if (!rail.is(Blocks.RAIL) || rail.getValue(RailBlock.SHAPE) != RailShape.EAST_WEST) return;
        BlockPos ballast = at.below();
        if (!level.getBlockState(ballast).is(Blocks.LIGHT_GRAY_CONCRETE)) return;
        put(level, x, 1, z, Blocks.AIR.defaultBlockState());
        put(level, x, 0, z, Blocks.WHITE_CONCRETE.defaultBlockState());
    }

    private static void restoreNorthSouth(ServerLevel level, int x, int z) {
        BlockPos at = new BlockPos(x, 1, z);
        BlockState state = level.getBlockState(at);
        if (state.is(Blocks.RAIL) && state.getValue(RailBlock.SHAPE) == RailShape.EAST_WEST
                && level.getBlockState(at.below()).is(Blocks.LIGHT_GRAY_CONCRETE)) {
            safeTrack(level, x, z, RailShape.NORTH_SOUTH);
        }
    }

    private static void bossApproach(ServerLevel level, BlockPos playerPos) {
        if (Math.abs(playerPos.getX()) > 100 || playerPos.getZ() < -60 || playerPos.getZ() > 330) return;
        // ~80 short rail placements per pass maximum, only chunks already loaded.
        int from = Math.max(36, playerPos.getZ() - 42);
        int to = Math.min(255, playerPos.getZ() + 42);
        for (int z = from; z <= to; z++) {
            BlockPos at = new BlockPos(0, 1, z);
            if (level.hasChunkAt(at)) safeTrack(level, 0, z, RailShape.NORTH_SOUTH);
        }
    }

    /** Empty, distinctly marked open-air arena reached by following the central railway. */
    public static boolean ensureBossArena(ServerLevel level, TerminalWorldData data) {
        if (data.bossArenaBuilt) return true;
        if (!loadedSquare(level, 0, 256, 42)) return false;
        // Understated square platform and corner lamps; preserve the boss trial spots.
        for (int x = -10; x <= 10; x++) for (int z = 246; z <= 266; z++) {
            put(level, x, 0, z, Blocks.SMOOTH_STONE.defaultBlockState());
        }
        for (int x : new int[]{-11,11}) for (int z : new int[]{245,267}) {
            for (int y = 1; y <= 6; y++) put(level, x, y, z, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
            put(level, x, 7, z, Blocks.SEA_LANTERN.defaultBlockState());
        }
        data.bossArenaBuilt = true;
        data.setDirty();
        return true;
    }

    private static void station(ServerLevel level, int x, int z, boolean lobby) {
        rail(level, x, z, true, 35);
        // Two deserted platforms; oversized roof, harsh white lights, no NPCs and no loot.
        for (int side : new int[]{-1, 1}) {
            for (int dz = -18; dz <= 18; dz++) {
                for (int dx = 3; dx <= 9; dx++) {
                    int xx = x + side * dx;
                    put(level, xx, 0, z + dz, Blocks.SMOOTH_STONE.defaultBlockState());
                    if (dx == 3) put(level, xx, 1, z + dz, Blocks.YELLOW_CONCRETE.defaultBlockState());
                }
            }
            for (int dz : new int[]{-17, 17}) {
                for (int dx : new int[]{4, 9}) {
                    int xx = x + side * dx;
                    for (int y = 1; y <= 6; y++) {
                        put(level, xx, y, z + dz, Blocks.GRAY_CONCRETE.defaultBlockState());
                    }
                }
            }
            for (int dz = -19; dz <= 19; dz++) {
                for (int dx = 3; dx <= 10; dx++) {
                    put(level, x + side * dx, 7, z + dz,
                            dz % 7 == 0 && dx == 6 ? Blocks.SEA_LANTERN.defaultBlockState()
                                    : Blocks.WHITE_CONCRETE.defaultBlockState());
                }
            }
            for (int dz : new int[]{-8, 8}) {
                put(level, x + side * 7, 1, z + dz, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
            }
            // Station-side signal post and a decorative turnout lever on each platform.
            signalMast(level, x + side * 2, z - 20, true, side);
            switchLever(level, x + side * 5, z + 15, true);
        }
        if (lobby) {
            // Arrivals always land at the origin, between these two bright platforms.
            for (int xx = -2; xx <= 2; xx++) for (int zz = -2; zz <= 2; zz++) {
                put(level, x + xx, 0, z + zz, Blocks.QUARTZ_BLOCK.defaultBlockState());
            }
            // Central spawn pad, with rails passing under its left/right edges.
            put(level, x, 0, z, Blocks.SEA_LANTERN.defaultBlockState());
        }
    }

    private static void city(ServerLevel level, int x, int z, Random random) {
        // A slightly bigger, intentionally lifeless architectural maquette (~66x66).
        // No villagers, no complex rooms, and no extra entities to tick.
        for (int dz = -32; dz <= 32; dz++) {
            for (int dx = -4; dx <= 4; dx++) {
                put(level, x + dx, 0, z + dz, Blocks.GRAY_CONCRETE.defaultBlockState());
            }
        }
        for (int dx = -32; dx <= 32; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                put(level, x + dx, 0, z + dz, Blocks.GRAY_CONCRETE.defaultBlockState());
            }
        }
        // Four building columns by four rows, with large gaps resembling empty streets.
        for (int col : new int[]{-27, -12, 12, 27}) {
            for (int row : new int[]{-27, -12, 12, 27}) {
                int height = 7 + random.nextInt(11);
                building(level, x + col, z + row, height, random.nextBoolean());
            }
        }
        // A train track cuts impossibly through an otherwise ordinary empty city.
        rail(level, x, z, true, 30);
        // Sparse sodium-white streetlights; still no artificial ticking objects.
        for (int dx : new int[]{-6, 6}) {
            for (int dz : new int[]{-26, -10, 10, 26}) {
                for (int y = 1; y <= 5; y++) put(level, x + dx, y, z + dz,
                        Blocks.POLISHED_ANDESITE.defaultBlockState());
                put(level, x + dx, 6, z + dz, Blocks.SEA_LANTERN.defaultBlockState());
            }
        }
    }

    private static void building(ServerLevel level, int cx, int cz, int height, boolean hasDoor) {
        final int hx = 4, hz = 4;
        for (int y = 0; y <= height; y++) {
            for (int dx = -hx; dx <= hx; dx++) {
                for (int dz = -hz; dz <= hz; dz++) {
                    boolean surface = y == 0 || y == height || Math.abs(dx) == hx || Math.abs(dz) == hz;
                    if (!surface) continue;
                    boolean window = y > 2 && y < height - 1 && y % 3 != 0
                            && ((Math.abs(dx) == hx && dz % 3 == 0)
                                || (Math.abs(dz) == hz && dx % 3 == 0));
                    // Concrete, glass and an empty shell. Interiors deliberately remain quiet.
                    put(level, cx + dx, y, cz + dz, window
                            ? Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState()
                            : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                }
            }
        }
        if (hasDoor) {
            // Doorless openings keep navigation cheap, allow players to actually enter.
            put(level, cx, 1, cz - hz, Blocks.AIR.defaultBlockState());
            put(level, cx, 2, cz - hz, Blocks.AIR.defaultBlockState());
            // A single unsettling white light inside, no block entity / AI.
            put(level, cx, height - 1, cz, Blocks.SEA_LANTERN.defaultBlockState());
        }
    }
}
