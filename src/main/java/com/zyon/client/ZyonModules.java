package com.zyon.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;

public final class ZyonModules {
    // estado
    private static boolean addedNightVision;
    private static boolean espWasOn;
    private static int crystalTimer;
    private static int obsidianTimer;
    private static int clickTimer;
    private static int refillTimer;
    private static final Item[] lastHotbar = new Item[9];
    private static boolean eating;
    private static int eatSlot;
    private static int eatPrevSlot;
    private static boolean sneakApplied;
    private static boolean sprintApplied;
    private static boolean zooming;
    private static int savedFov = 70;

    // estatisticas (lidas pelo HUD)
    public static double speedH;
    public static double speedV;
    public static boolean critReady;
    public static int combo;
    public static double lastReach;
    private static double lastX, lastZ;
    private static boolean attackWasDown;
    private static final ArrayDeque<Long> clicks = new ArrayDeque<>();
    private static long lastAttackMs;
    private static int lastTargetHurt;
    private static int lastSelfHurt;

    private ZyonModules() {}

    public static void renderHud(DrawContext ctx) {
        ZyonHud.render(ctx);
    }

    public static int cps() { return clicks.size(); }

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) return;
        DiscoverySettings s = ZyonClient.SETTINGS;

        if (s.autoRespawn && p.isDead()) {
            p.requestRespawn();
            return;
        }

        trackStats(mc, p, w, s);
        sprint(mc, p, s);
        toggles(mc, s);
        fullbright(p, s);
        esp(w, p, s);
        zoom(mc, s);
        autoRefill(mc, p, s);

        if (mc.currentScreen != null || mc.interactionManager == null) {
            if (eating) stopEat(mc, p);
            return;
        }
        killAura(mc, p, w, s);
        triggerBot(mc, p, s);
        autoClicker(mc, p, s);
        aimAssist(mc, p, w, s);
        autoObsidian(mc, p, w, s);
        autoCrystal(mc, p, w, s);
        autoEat(mc, p, s);
    }

    // ---------- Estatisticas ----------

    private static void markAttack() { lastAttackMs = System.currentTimeMillis(); }

    private static void trackStats(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        speedH = Math.sqrt(sq(p.getX() - lastX) + sq(p.getZ() - lastZ)) * 20.0;
        speedV = p.getVelocity().y * 20.0;
        lastX = p.getX();
        lastZ = p.getZ();

        boolean down = mc.options.attackKey.isPressed();
        long now = System.currentTimeMillis();
        if (down && !attackWasDown) {
            clicks.addLast(now);
            lastAttackMs = now;
        }
        attackWasDown = down;
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.pollFirst();

        critReady = p.fallDistance > 0.0 && !p.isOnGround() && !p.isClimbing()
                && !p.isTouchingWater() && !p.hasVehicle();

        PlayerEntity t = nearestEnemy(p, w, 8);
        int th = t == null ? 0 : t.hurtTime;
        if (t != null && th > 0 && th > lastTargetHurt && now - lastAttackMs < 450) {
            combo++;
            lastReach = p.distanceTo(t);
            if (s.hitSounds) {
                w.playSound(p, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.6f);
            }
        }
        lastTargetHurt = th;
        int sh = p.hurtTime;
        if (sh > lastSelfHurt) combo = 0;
        lastSelfHurt = sh;
    }

    // ---------- Jogador ----------

    private static void sprint(MinecraftClient mc, ClientPlayerEntity p, DiscoverySettings s) {
        if (s.autoSprint && mc.options.forwardKey.isPressed() && !p.isSneaking()
                && !p.horizontalCollision && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }

    private static void toggles(MinecraftClient mc, DiscoverySettings s) {
        if (s.toggleSneak != sneakApplied) {
            mc.options.getSneakToggled().setValue(s.toggleSneak);
            sneakApplied = s.toggleSneak;
        }
        if (s.toggleSprint != sprintApplied) {
            mc.options.getSprintToggled().setValue(s.toggleSprint);
            sprintApplied = s.toggleSprint;
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

    private static void zoom(MinecraftClient mc, DiscoverySettings s) {
        boolean want = s.zoom && ZyonClient.zoomKey != null && ZyonClient.zoomKey.isPressed()
                && mc.currentScreen == null;
        if (want && !zooming) {
            savedFov = mc.options.getFov().getValue();
            mc.options.getFov().setValue(30);
            zooming = true;
        } else if (!want && zooming) {
            mc.options.getFov().setValue(savedFov);
            zooming = false;
        }
    }

    private static boolean goodFood(ItemStack st) {
        if (!st.contains(DataComponentTypes.FOOD)) return false;
        return !(st.isOf(Items.GOLDEN_APPLE) || st.isOf(Items.ENCHANTED_GOLDEN_APPLE)
                || st.isOf(Items.CHORUS_FRUIT) || st.isOf(Items.ROTTEN_FLESH)
                || st.isOf(Items.SPIDER_EYE) || st.isOf(Items.POISONOUS_POTATO)
                || st.isOf(Items.PUFFERFISH));
    }

    private static void autoEat(MinecraftClient mc, ClientPlayerEntity p, DiscoverySettings s) {
        if (!s.autoEat) {
            if (eating) stopEat(mc, p);
            return;
        }
        int food = p.getHungerManager().getFoodLevel();
        if (eating) {
            if (food >= 20 || !goodFood(p.getInventory().getStack(eatSlot))) {
                stopEat(mc, p);
            } else {
                selectSlot(p, eatSlot);
                mc.options.useKey.setPressed(true);
            }
            return;
        }
        if (food <= 14 && !p.isUsingItem()) {
            for (int i = 0; i < 9; i++) {
                if (goodFood(p.getInventory().getStack(i))) {
                    eatPrevSlot = p.getInventory().getSelectedSlot();
                    eatSlot = i;
                    selectSlot(p, i);
                    eating = true;
                    break;
                }
            }
        }
    }

    private static void stopEat(MinecraftClient mc, ClientPlayerEntity p) {
        mc.options.useKey.setPressed(false);
        selectSlot(p, eatPrevSlot);
        eating = false;
    }

    private static void autoRefill(MinecraftClient mc, ClientPlayerEntity p, DiscoverySettings s) {
        if (s.autoRefill && mc.currentScreen == null && mc.interactionManager != null && refillTimer-- <= 0) {
            refillTimer = 8;
            for (int h = 0; h < 9; h++) {
                ItemStack cur = p.getInventory().getStack(h);
                Item want = cur.isEmpty() ? lastHotbar[h]
                        : (cur.isStackable() && cur.getCount() <= 8 ? cur.getItem() : null);
                if (want == null) continue;
                int best = -1;
                int bestCount = cur.getCount();
                for (int i = 9; i < 36; i++) {
                    ItemStack o = p.getInventory().getStack(i);
                    if (o.isOf(want) && o.getCount() > bestCount) {
                        best = i;
                        bestCount = o.getCount();
                    }
                }
                if (best >= 0) {
                    mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, best, h, SlotActionType.SWAP, p);
                    break;
                }
            }
        }
        for (int h = 0; h < 9; h++) {
            ItemStack st = p.getInventory().getStack(h);
            if (!st.isEmpty()) lastHotbar[h] = st.getItem();
        }
    }

    // ---------- Combate ----------

    private static void faceTarget(ClientPlayerEntity p, Entity t, float strength) {
        Vec3d c = t.getBoundingBox().getCenter();
        double dx = c.x - p.getX();
        double dy = c.y - p.getEyeY();
        double dz = c.z - p.getZ();
        double hd = Math.sqrt(dx * dx + dz * dz);
        float ty = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float tp = (float) -Math.toDegrees(Math.atan2(dy, hd));
        float dyaw = MathHelper.wrapDegrees(ty - p.getYaw());
        float dpit = tp - p.getPitch();
        p.setYaw(p.getYaw() + dyaw * strength);
        p.setPitch(MathHelper.clamp(p.getPitch() + dpit * strength, -90.0f, 90.0f));
    }

    private static void killAura(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!s.killAura) return;
        double r = s.killAuraRange;
        List<LivingEntity> list = w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && p.distanceTo(e) <= r
                        && (!s.killAuraPlayersOnly || e instanceof PlayerEntity));
        if (list.isEmpty()) return;
        LivingEntity t = list.stream().min(Comparator.comparingDouble(e -> p.distanceTo(e))).get();
        faceTarget(p, t, 1.0f);
        if (p.getAttackCooldownProgress(0.5f) < 0.95f) return;
        mc.interactionManager.attackEntity(p, t);
        p.swingHand(Hand.MAIN_HAND);
        markAttack();
    }

    private static void triggerBot(MinecraftClient mc, ClientPlayerEntity p, DiscoverySettings s) {
        if (!s.triggerBot) return;
        Entity e = mc.targetedEntity;
        if (!(e instanceof LivingEntity le) || !le.isAlive() || le == p) return;
        if (s.killAuraPlayersOnly && !(le instanceof PlayerEntity)) return;
        if (p.getAttackCooldownProgress(0.5f) < 0.95f) return;
        mc.interactionManager.attackEntity(p, le);
        p.swingHand(Hand.MAIN_HAND);
        markAttack();
    }

    private static void autoClicker(MinecraftClient mc, ClientPlayerEntity p, DiscoverySettings s) {
        if (!s.autoClicker || !mc.options.attackKey.isPressed()) return;
        if (clickTimer-- > 0) return;
        clickTimer = Math.max(1, 20 / Math.max(1, s.autoClickerCps)) - 1;
        Entity e = mc.targetedEntity;
        if (e != null && e.isAlive() && e != p) {
            mc.interactionManager.attackEntity(p, e);
            p.swingHand(Hand.MAIN_HAND);
            markAttack();
        }
    }

    private static void aimAssist(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!s.aimAssist || !mc.options.attackKey.isPressed()) return;
        PlayerEntity t = nearestEnemy(p, w, 5);
        if (t == null) return;
        Vec3d c = t.getBoundingBox().getCenter();
        double dx = c.x - p.getX();
        double dy = c.y - p.getEyeY();
        double dz = c.z - p.getZ();
        double hd = Math.sqrt(dx * dx + dz * dz);
        float ty = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float tp = (float) -Math.toDegrees(Math.atan2(dy, hd));
        if (Math.abs(MathHelper.wrapDegrees(ty - p.getYaw())) > 70.0f || Math.abs(tp - p.getPitch()) > 50.0f) return;
        faceTarget(p, t, Math.max(0.02f, s.aimStrength / 200.0f));
    }

    private static void autoCrystal(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!s.autoCrystal) return;
        if (crystalTimer-- > 0) return;
        crystalTimer = Math.max(0, s.crystalDelay);
        double range = s.crystalRange;

        PlayerEntity target = nearestEnemy(p, w, 10);
        if (target == null) return;

        List<EndCrystalEntity> crystals = w.getEntitiesByClass(EndCrystalEntity.class,
                target.getBoundingBox().expand(6),
                c -> c.isAlive() && p.distanceTo(c) <= range && c.distanceTo(target) <= 6);
        if (!crystals.isEmpty()) {
            EndCrystalEntity c = crystals.stream().min(Comparator.comparingDouble(e -> e.distanceTo(target))).get();
            mc.interactionManager.attackEntity(p, c);
            p.swingHand(Hand.MAIN_HAND);
            markAttack();
            return;
        }

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

    public static int count(ClientPlayerEntity p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack st = p.getInventory().getStack(i);
            if (st.isOf(item)) n += st.getCount();
        }
        return n;
    }

    public static int durabilityPercent(ItemStack st) {
        if (st.isEmpty() || !st.isDamageable() || st.getMaxDamage() <= 0) return 100;
        return 100 * (st.getMaxDamage() - st.getDamage()) / st.getMaxDamage();
    }

    public static PlayerEntity nearestEnemy(ClientPlayerEntity p, ClientWorld w, double r) {
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

    private static boolean isCrystalBase(ClientWorld w, BlockPos pos) {
        BlockState st = w.getBlockState(pos);
        if (!(st.isOf(Blocks.OBSIDIAN) || st.isOf(Blocks.BEDROCK))) return false;
        BlockPos a = pos.up();
        if (!w.getBlockState(a).isAir() || !w.getBlockState(a.up()).isAir()) return false;
        Box box = new Box(a.getX(), a.getY(), a.getZ(), a.getX() + 1, a.getY() + 2, a.getZ() + 1);
        return w.getOtherEntities((Entity) null, box).isEmpty();
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
