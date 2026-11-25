package com.zyon.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ZyonModules {
    private static boolean addedNightVision;
    private static boolean espWasOn;
    private static int crystalTimer;
    private static int obsidianTimer;

    private ZyonModules() {}

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) return;
        DiscoverySettings s = ZyonClient.SETTINGS;

        sprint(mc, p, s);
        fullbright(p, s);
        esp(w, p, s);

        if (mc.currentScreen != null || mc.interactionManager == null) return;
        killAura(mc, p, w, s);
        autoObsidian(mc, p, w, s);
        autoCrystal(mc, p, w, s);
    }

    // ---------- Jogador ----------

    private static void sprint(MinecraftClient mc, ClientPlayerEntity p, DiscoverySettings s) {
        if (s.autoSprint && mc.options.forwardKey.isPressed() && !p.isSneaking()
                && !p.horizontalCollision && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }

    private static void fullbright(ClientPlayerEntity p, DiscoverySettings s) {
        if (s.fullbright) {
            if (!p.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 100000, 0, false, false, false));
                addedNightVision = true;
            }
        } else if (addedNightVision) {
            p.removeStatusEffect(StatusEffects.NIGHT_VISION);
            addedNightVision = false;
        }
    }

    // ---------- Visual ----------

    private static void esp(ClientWorld w, ClientPlayerEntity p, DiscoverySettings s) {
        if (s.playerEsp) {
            for (PlayerEntity e : w.getPlayers()) {
                if (e != p) e.setGlowing(true);
            }
            espWasOn = true;
        } else if (espWasOn) {
            for (PlayerEntity e : w.getPlayers()) {
                if (e != p) e.setGlowing(false);
            }
            espWasOn = false;
        }
    }

    public static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        DiscoverySettings s = ZyonClient.SETTINGS;
        TextRenderer tr = mc.textRenderer;

        if (s.hudModules) {
            int y = 4;
            ctx.drawText(tr, "Zyon", 4, y, 0xFF9B7BFF, true);
            y += 11;
            for (String n : activeModules(s)) {
                ctx.drawText(tr, n, 4, y, 0xFFE6E9F2, true);
                y += 10;
            }
        }

        if (s.hudFindings && s.anyScannerEnabled()) {
            int sw = mc.getWindow().getScaledWidth();
            List<DiscoveryScanner.Finding> list = new ArrayList<>(ZyonClient.SCANNER.findings().values());
            list.sort((a, b) -> Integer.compare(b.count(), a.count()));
            String title = "Achados";
            ctx.drawText(tr, title, sw - 4 - tr.getWidth(title), 4, 0xFF9B7BFF, true);
            int y = 15;
            int shown = 0;
            for (DiscoveryScanner.Finding f : list) {
                if (shown++ >= 6) break;
                String line = f.label() + " x" + f.count() + "  (" + f.pos().getX() + ", " + f.pos().getZ() + ")";
                ctx.drawText(tr, line, sw - 4 - tr.getWidth(line), y, 0xFFE6E9F2, true);
                y += 10;
            }
        }
    }

    private static List<String> activeModules(DiscoverySettings s) {
        List<String> l = new ArrayList<>();
        if (s.killAura) l.add("KillAura");
        if (s.autoCrystal) l.add("AutoCrystal");
        if (s.autoObsidian) l.add("AutoObsidian");
        if (s.autoSprint) l.add("AutoSprint");
        if (s.fullbright) l.add("Fullbright");
        if (s.playerEsp) l.add("ESP");
        if (s.baseFinder) l.add("BaseFinder");
        if (s.chunkFinder) l.add("ChunkFinder");
        if (s.amethystScanner) l.add("Amethyst");
        return l;
    }

    // ---------- Combate ----------

    private static void killAura(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!s.killAura) return;
        double r = s.killAuraRange;
        List<LivingEntity> list = w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && p.distanceTo(e) <= r
                        && (!s.killAuraPlayersOnly || e instanceof PlayerEntity));
        if (list.isEmpty()) return;
        LivingEntity t = list.stream().min(Comparator.comparingDouble(e -> p.distanceTo(e))).get();

        Vec3d c = t.getBoundingBox().getCenter();
        double dx = c.x - p.getX();
        double dy = c.y - p.getEyeY();
        double dz = c.z - p.getZ();
        double hd = Math.sqrt(dx * dx + dz * dz);
        p.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        p.setPitch((float) -Math.toDegrees(Math.atan2(dy, hd)));

        if (p.getAttackCooldownProgress(0.5f) < 0.95f) return;
        mc.interactionManager.attackEntity(p, t);
        p.swingHand(Hand.MAIN_HAND);
    }

    private static void autoCrystal(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!s.autoCrystal) return;
        if (crystalTimer-- > 0) return;
        crystalTimer = Math.max(0, s.crystalDelay);
        double range = s.crystalRange;

        PlayerEntity target = nearestEnemy(p, w, 10);
        if (target == null) return;

        // 1) quebra cristal perto do alvo
        List<EndCrystalEntity> crystals = w.getEntitiesByClass(EndCrystalEntity.class,
                target.getBoundingBox().expand(6),
                c -> c.isAlive() && p.distanceTo(c) <= range && c.distanceTo(target) <= 6);
        if (!crystals.isEmpty()) {
            EndCrystalEntity c = crystals.stream().min(Comparator.comparingDouble(e -> e.distanceTo(target))).get();
            mc.interactionManager.attackEntity(p, c);
            p.swingHand(Hand.MAIN_HAND);
            return;
        }

        // 2) coloca cristal na melhor base perto do alvo
        int slot = findHotbar(p, Items.END_CRYSTAL);
        if (slot < 0) return;
        BlockPos tp = target.getBlockPos();
        BlockPos best = null;
        double bestD = 1e9;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 1; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = tp.add(dx, dy, dz);
                    if (!isCrystalBase(w, pos)) continue;
                    double cx = pos.getX() + 0.5;
                    double cy = pos.getY() + 1.0;
                    double cz = pos.getZ() + 0.5;
                    double dEye = Math.sqrt(sq(cx - p.getX()) + sq(cy - p.getEyeY()) + sq(cz - p.getZ()));
                    if (dEye > range) continue;
                    double dSelf = Math.sqrt(sq(cx - p.getX()) + sq(cz - p.getZ()));
                    if (dSelf < 2.5 && Math.abs(cy - p.getY()) < 2.5) continue;
                    double dT = sq(cx - target.getX()) + sq(cy - target.getY()) + sq(cz - target.getZ());
                    if (dT < bestD) {
                        bestD = dT;
                        best = pos;
                    }
                }
            }
        }
        if (best == null) return;
        selectSlot(p, slot);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(best).add(0, 0.5, 0), Direction.UP, best, false);
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, hit);
        p.swingHand(Hand.MAIN_HAND);
    }

    private static void autoObsidian(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!s.autoObsidian) return;
        if (obsidianTimer-- > 0) return;
        obsidianTimer = 3;

        PlayerEntity target = nearestEnemy(p, w, 10);
        if (target == null) return;
        int slot = findHotbar(p, Items.OBSIDIAN);
        if (slot < 0) return;

        BlockPos tp = target.getBlockPos();
        // ja existe base de cristal perto do alvo? entao nao faz nada
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    if (isCrystalBase(w, tp.add(dx, dy, dz))) return;
                }
            }
        }

        BlockPos bestPlace = null;
        BlockPos bestSupport = null;
        Direction bestFace = null;
        double bestD = 1e9;
        Direction[] dirs = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos pos = tp.add(dx, -1, dz);
                if (!w.getBlockState(pos).isAir()) continue;
                if (!w.getBlockState(pos.up()).isAir() || !w.getBlockState(pos.up(2)).isAir()) continue;
                if (!w.getOtherEntities((Entity) null, new Box(pos)).isEmpty()) continue;
                double dEye = Math.sqrt(sq(pos.getX() + 0.5 - p.getX()) + sq(pos.getY() + 0.5 - p.getEyeY())
                        + sq(pos.getZ() + 0.5 - p.getZ()));
                if (dEye > 4.5) continue;
                for (Direction dir : dirs) {
                    BlockPos n = pos.offset(dir);
                    if (w.getBlockState(n).isAir()) continue;
                    double dt = sq(pos.getX() + 0.5 - target.getX()) + sq(pos.getZ() + 0.5 - target.getZ());
                    if (dt < bestD) {
                        bestD = dt;
                        bestPlace = pos;
                        bestSupport = n;
                        bestFace = dir.getOpposite();
                    }
                    break;
                }
            }
        }
        if (bestPlace == null) return;
        selectSlot(p, slot);
        Vec3d hitVec = Vec3d.ofCenter(bestSupport).add(
                bestFace.getOffsetX() * 0.5, bestFace.getOffsetY() * 0.5, bestFace.getOffsetZ() * 0.5);
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hitVec, bestFace, bestSupport, false));
        p.swingHand(Hand.MAIN_HAND);
    }

    // ---------- Utilidades ----------

    private static boolean isCrystalBase(ClientWorld w, BlockPos pos) {
        BlockState st = w.getBlockState(pos);
        if (!(st.isOf(Blocks.OBSIDIAN) || st.isOf(Blocks.BEDROCK))) return false;
        BlockPos a = pos.up();
        if (!w.getBlockState(a).isAir() || !w.getBlockState(a.up()).isAir()) return false;
        Box box = new Box(a.getX(), a.getY(), a.getZ(), a.getX() + 1, a.getY() + 2, a.getZ() + 1);
        return w.getOtherEntities((Entity) null, box).isEmpty();
    }

    private static PlayerEntity nearestEnemy(ClientPlayerEntity p, ClientWorld w, double r) {
        PlayerEntity best = null;
        double bd = 1e9;
        for (PlayerEntity e : w.getPlayers()) {
            if (e == p || !e.isAlive() || e.isSpectator()) continue;
            double d = p.distanceTo(e);
            if (d > r || d >= bd) continue;
            best = e;
            bd = d;
        }
        return best;
    }

    private static int findHotbar(ClientPlayerEntity p, Item item) {
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private static void selectSlot(ClientPlayerEntity p, int slot) {
        if (p.getInventory().getSelectedSlot() != slot) p.getInventory().setSelectedSlot(slot);
    }

    private static double sq(double v) { return v * v; }
}
