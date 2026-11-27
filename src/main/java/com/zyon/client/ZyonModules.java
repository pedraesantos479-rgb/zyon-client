package com.zyon.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.TntBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
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
import net.minecraft.world.Difficulty;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;

public final class ZyonModules {
    // estado
    private static boolean addedNightVision;
    private static int crystalTimer;
    private static int obsidianTimer;
    private static int totemTimer;
    private static int bridgeTimer;
    private static BlockPos nukerTarget;
    private static int nukerPrevSlot = -1;
    public static String crystalStatus = "-";
    private static ArmorStandEntity fcStand;
    private static ClientWorld fcWorld;
    private static double fcX, fcY, fcZ;
    private static final int FC_ID = -8421;
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

    public static void renderHud(DrawContext ctx, float delta) {
        ZyonEsp.render(ctx, delta);
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
        ZyonEsp.tickScan(mc, p, w, s);
        ZyonEsp.tickXray(p, w, s);
        zoom(mc, s);
        freecamTick(mc, p, w, s);
        autoRefill(mc, p, s);

        if (mc.currentScreen != null || mc.interactionManager == null) {
            if (eating) stopEat(mc, p);
            return;
        }
        autoTotem(mc, p, s);
        autoBridge(mc, p, w, s);
        nuker(mc, p, w, s);
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
        if (!(s.autoCrystal || s.cpvpAll)) {
            crystalStatus = "-";
            return;
        }
        if (crystalTimer-- > 0) return;
        crystalTimer = delayTicks(s.cpvpSpeed);
        double placeRange = Math.min(s.crystalRange, 4.5);
        double breakRange = Math.min(s.crystalRange, 4.0);

        LivingEntity target = nearestLiving(p, w, 10, s.cpvpPlayersOnly);
        if (target == null) {
            crystalStatus = s.cpvpPlayersOnly ? "sem jogador a 10 blocos" : "sem alvo a 10 blocos";
            return;
        }
        if (p.isUsingItem()) {
            crystalStatus = "pausado (usando item)";
            return;
        }

        // 1) quebra o crystal que mais machuca o alvo (e e seguro pra voce)
        List<EndCrystalEntity> crystals = w.getEntitiesByClass(EndCrystalEntity.class,
                target.getBoundingBox().expand(8),
                c -> c.isAlive() && p.distanceTo(c) <= breakRange);
        EndCrystalEntity bestC = null;
        double bestScore = -1e9;
        int unsafe = 0;
        for (EndCrystalEntity c : crystals) {
            if (!selfSafe(w, p, c.getX(), c.getY(), c.getZ(), s)) {
                unsafe++;
                continue;
            }
            double score = explosionDamage(w, target, c.getX(), c.getY(), c.getZ());
            if (score > bestScore) {
                bestScore = score;
                bestC = c;
            }
        }
        if (bestC != null) {
            lookAtPoint(p, bestC.getBoundingBox().getCenter());
            mc.interactionManager.attackEntity(p, bestC);
            p.swingHand(Hand.MAIN_HAND);
            markAttack();
            crystalStatus = "quebrando";
            if (s.cpvpSpeed < 9) return;
        }

        // 2) coloca crystal na melhor base
        boolean offhand = p.getOffHandStack().isOf(Items.END_CRYSTAL);
        int slot = findHotbar(p, Items.END_CRYSTAL);
        if (!offhand && slot < 0) {
            crystalStatus = "sem crystal na hotbar";
            return;
        }
        BlockPos tp = target.getBlockPos();
        BlockPos best = null;
        double bestPlace = -1e9;
        int bases = 0, inRange = 0, safe = 0, dmgOk = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 1; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = tp.add(dx, dy, dz);
                    if (!isCrystalBase(w, pos)) continue;
                    bases++;
                    double cx = pos.getX() + 0.5;
                    double cy = pos.getY() + 1.0;
                    double cz = pos.getZ() + 0.5;
                    double dEye = Math.sqrt(sq(cx - p.getX()) + sq(cy - p.getEyeY()) + sq(cz - p.getZ()));
                    if (dEye > placeRange) continue;
                    inRange++;
                    if (!selfSafe(w, p, cx, cy, cz, s)) continue;
                    safe++;
                    double tdmg = explosionDamage(w, target, cx, cy, cz);
                    if (tdmg < s.minTargetDamage) continue;
                    dmgOk++;
                    double score = tdmg - selfDamage(w, p, cx, cy, cz, s) * 0.5;
                    if (score > bestPlace) {
                        bestPlace = score;
                        best = pos;
                    }
                }
            }
        }
        if (best == null) {
            if (bestC == null) {
                crystalStatus = bases == 0 ? "sem base (obsidian) perto do alvo"
                        : "bases " + bases + " | alcance " + inRange + " | seguras " + safe + " | dano ok " + dmgOk;
            }
            return;
        }
        if (!offhand) selectSlot(p, slot);
        Vec3d hitVec = Vec3d.ofCenter(best).add(0, 0.5, 0);
        lookAtPoint(p, hitVec);
        Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        mc.interactionManager.interactBlock(p, hand, new BlockHitResult(hitVec, Direction.UP, best, false));
        p.swingHand(hand);
        crystalStatus = "colocando";
    }

    private static void lookAtPoint(ClientPlayerEntity p, Vec3d c) {
        double dx = c.x - p.getX();
        double dy = c.y - p.getEyeY();
        double dz = c.z - p.getZ();
        double hd = Math.sqrt(dx * dx + dz * dz);
        p.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        p.setPitch((float) -Math.toDegrees(Math.atan2(dy, hd)));
    }

    private static double selfDamage(ClientWorld w, ClientPlayerEntity p, double x, double y, double z, DiscoverySettings s) {
        return explosionDamage(w, p, x, y, z) * (1.0 - s.selfProtection / 100.0);
    }

    private static boolean selfSafe(ClientWorld w, ClientPlayerEntity p, double x, double y, double z, DiscoverySettings s) {
        double d = selfDamage(w, p, x, y, z, s);
        double hp = p.getHealth() + p.getAbsorptionAmount();
        if (s.antiSuicide && d >= hp) return false;
        if (s.maxSelfDamage == 0) return true;
        return d <= s.maxSelfDamage && d < hp - 1.0;
    }

    public static LivingEntity nearestLiving(ClientPlayerEntity p, ClientWorld w, double r, boolean playersOnly) {
        List<LivingEntity> l = w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && (!playersOnly || e instanceof PlayerEntity));
        LivingEntity best = null;
        double bd = 1e9;
        for (LivingEntity e : l) {
            double d = p.distanceTo(e);
            if (d <= r && d < bd) {
                best = e;
                bd = d;
            }
        }
        return best;
    }

    /** Dano estimado de um crystal (formula do jogo, sem checar paredes). */
    private static double explosionDamage(ClientWorld w, LivingEntity e, double x, double y, double z) {
        double q = 12.0;
        double dist = Math.sqrt(sq(e.getX() - x) + sq(e.getY() - y) + sq(e.getZ() - z));
        if (dist >= q) return 0.0;
        double impact = 1.0 - dist / q;
        double dmg = (impact * impact + impact) / 2.0 * 7.0 * q + 1.0;
        Difficulty d = w.getDifficulty();
        if (d == Difficulty.PEACEFUL) return 0.0;
        if (d == Difficulty.EASY) dmg = Math.min(dmg / 2.0 + 1.0, dmg);
        else if (d == Difficulty.HARD) dmg = dmg * 1.5;
        double armor = e.getArmor();
        double red = Math.min(20.0, Math.max(armor / 5.0, armor - dmg / 2.0));
        return dmg * (1.0 - red / 25.0);
    }

    private static void autoTotem(MinecraftClient mc, ClientPlayerEntity p, DiscoverySettings s) {
        if (!s.autoTotem) return;
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (s.totemHealth > 0 && p.getHealth() + p.getAbsorptionAmount() > s.totemHealth) return;
        if (totemTimer-- > 0) return;
        totemTimer = 2;
        int slotId = -1;
        for (int i = 9; i < 36 && slotId < 0; i++) {
            if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) slotId = i;
        }
        for (int i = 0; i < 9 && slotId < 0; i++) {
            if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) slotId = 36 + i;
        }
        if (slotId < 0) return;
        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slotId, 40, SlotActionType.SWAP, p);
    }

    private static void autoObsidian(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!(s.autoObsidian || s.cpvpAll)) return;
        if (obsidianTimer-- > 0) return;
        obsidianTimer = 1 + delayTicks(s.cpvpSpeed);

        LivingEntity target = nearestLiving(p, w, 10, s.cpvpPlayersOnly);
        if (target == null) return;
        int slot = findHotbar(p, Items.OBSIDIAN);
        if (slot < 0) return;
        double placeRange = Math.min(s.crystalRange, 4.5);
        BlockPos tp = target.getBlockPos();

        // ja existe uma base ao alcance perto do alvo? entao nao poe outra
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos b = tp.add(dx, dy, dz);
                    if (!isCrystalBase(w, b)) continue;
                    double d = Math.sqrt(sq(b.getX() + 0.5 - p.getX()) + sq(b.getY() + 1.0 - p.getEyeY())
                            + sq(b.getZ() + 0.5 - p.getZ()));
                    if (d <= placeRange) return;
                }
            }
        }

        // base nova: bloco em cima do chao, ao lado do alvo, com 2 blocos livres em cima
        BlockPos best = null;
        double bestD = 1e9;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos pos = tp.add(dx, 0, dz);
                if (!w.getBlockState(pos).isAir()) continue;
                if (!w.getBlockState(pos.up()).isAir() || !w.getBlockState(pos.up(2)).isAir()) continue;
                BlockPos below = pos.down();
                if (!w.getBlockState(below).isSolidBlock(w, below)) continue;
                if (!w.getOtherEntities((Entity) null, new Box(pos)).isEmpty()) continue;
                double dEye = Math.sqrt(sq(pos.getX() + 0.5 - p.getX()) + sq(pos.getY() + 0.5 - p.getEyeY())
                        + sq(pos.getZ() + 0.5 - p.getZ()));
                if (dEye > placeRange) continue;
                double dSelf = Math.sqrt(sq(pos.getX() + 0.5 - p.getX()) + sq(pos.getZ() + 0.5 - p.getZ()));
                if (dSelf < 1.5 && Math.abs(pos.getY() - p.getY()) < 2.0) continue;
                double dt = sq(pos.getX() + 0.5 - target.getX()) + sq(pos.getZ() + 0.5 - target.getZ());
                if (dt < bestD) {
                    bestD = dt;
                    best = pos;
                }
            }
        }
        if (best == null) return;
        placeBlock(mc, p, w, best, slot, true);
    }

    // ---------- Nuker (quebra blocos perto) ----------

    private static boolean nukerValid(ClientWorld w, ClientPlayerEntity p, DiscoverySettings s, BlockPos pos) {
        BlockState st = w.getBlockState(pos);
        if (st.isAir() || !st.getFluidState().isEmpty()) return false;
        if (st.getHardness(w, pos) < 0) return false;                 // bedrock etc.
        if (pos.equals(p.getBlockPos().down())) return false;         // nao quebra o chao onde voce pisa
        if (s.nukerOres && !ZyonEsp.isOre(st.getBlock())) return false;
        double d = Math.sqrt(sq(pos.getX() + 0.5 - p.getX()) + sq(pos.getY() + 0.5 - p.getEyeY())
                + sq(pos.getZ() + 0.5 - p.getZ()));
        return d <= Math.min(s.nukerRange, 6);
    }

    private static BlockPos nukerPick(ClientWorld w, ClientPlayerEntity p, DiscoverySettings s) {
        int r = Math.max(1, Math.min(6, s.nukerRange));
        BlockPos base = p.getBlockPos();
        BlockPos best = null;
        double bd = 1e9;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = base.add(dx, dy, dz);
                    if (!nukerValid(w, p, s, pos)) continue;
                    double d = sq(pos.getX() + 0.5 - p.getX()) + sq(pos.getY() + 0.5 - p.getEyeY())
                            + sq(pos.getZ() + 0.5 - p.getZ());
                    if (d < bd) {
                        bd = d;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    /** Troca para a melhor ferramenta da hotbar para esse bloco. */
    private static void nukerTool(ClientPlayerEntity p, BlockState st) {
        int bestSlot = -1;
        float bestSpeed = p.getInventory().getStack(p.getInventory().getSelectedSlot()).getMiningSpeedMultiplier(st);
        for (int i = 0; i < 9; i++) {
            float sp = p.getInventory().getStack(i).getMiningSpeedMultiplier(st);
            if (sp > bestSpeed + 0.01f) {
                bestSpeed = sp;
                bestSlot = i;
            }
        }
        if (bestSlot >= 0) {
            if (nukerPrevSlot < 0) nukerPrevSlot = p.getInventory().getSelectedSlot();
            selectSlot(p, bestSlot);
        }
    }

    private static void nukerRestore(ClientPlayerEntity p) {
        if (nukerPrevSlot >= 0) {
            selectSlot(p, nukerPrevSlot);
            nukerPrevSlot = -1;
        }
    }

    private static void nuker(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!s.nuker) {
            nukerTarget = null;
            nukerRestore(p);
            return;
        }
        boolean creative = p.isCreative();
        int count = creative ? Math.max(1, s.nukerPerTick) : 1;
        for (int k = 0; k < count; k++) {
            boolean fresh = false;
            if (nukerTarget == null || !nukerValid(w, p, s, nukerTarget)) {
                nukerTarget = nukerPick(w, p, s);
                fresh = true;
            }
            if (nukerTarget == null) {
                nukerRestore(p);
                return;
            }
            if (fresh && s.nukerAutoTool) nukerTool(p, w.getBlockState(nukerTarget));
            Direction face = Direction.getFacing(p.getX() - (nukerTarget.getX() + 0.5),
                    p.getEyeY() - (nukerTarget.getY() + 0.5), p.getZ() - (nukerTarget.getZ() + 0.5));
            // o jogo espera 5 ticks entre blocos; chamar de novo "gasta" essa espera no mesmo tick
            for (int c = 0; c < 6; c++) {
                mc.interactionManager.updateBlockBreakingProgress(nukerTarget, face);
                if (creative) {
                    if (w.getBlockState(nukerTarget).isAir()) break;
                } else if (mc.interactionManager.isBreakingBlock()) {
                    break;
                }
            }
            p.swingHand(Hand.MAIN_HAND);
            if (!creative) return;       // sobrevivencia: um bloco por vez
            nukerTarget = null;
        }
    }

    // ---------- Freecam ----------

    public static void startTick(MinecraftClient mc) {
        DiscoverySettings s = ZyonClient.SETTINGS;
        if (s.freecam && fcStand != null && mc.currentScreen == null) {
            // o corpo do jogador fica parado enquanto a camera voa
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);
            mc.options.attackKey.setPressed(false);
            mc.options.useKey.setPressed(false);
        }
    }

    private static void freecamStop(MinecraftClient mc, ClientPlayerEntity p) {
        if (fcStand != null && mc.getCameraEntity() == fcStand) mc.setCameraEntity(p);
        if (fcWorld != null && fcStand != null) {
            try {
                fcWorld.removeEntity(FC_ID, Entity.RemovalReason.DISCARDED);
            } catch (Throwable ignored) {
            }
        }
        fcStand = null;
        fcWorld = null;
    }

    private static void freecamTick(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!s.freecam) {
            if (fcStand != null) freecamStop(mc, p);
            return;
        }
        if (fcStand != null && fcWorld != w) {
            fcStand = null;
            fcWorld = null;
        }
        if (fcStand == null) {
            try {
                ArmorStandEntity st = new ArmorStandEntity(EntityType.ARMOR_STAND, w);
                st.setId(FC_ID);
                st.setNoGravity(true);
                st.setInvisible(true);
                fcX = p.getX();
                fcY = p.getEyeY();
                fcZ = p.getZ();
                st.setPosition(fcX, fcY - st.getEyeHeight(EntityPose.STANDING), fcZ);
                st.setYaw(p.getYaw());
                st.setPitch(p.getPitch());
                w.addEntity(st);
                mc.setCameraEntity(st);
                fcStand = st;
                fcWorld = w;
            } catch (Throwable t) {
                s.freecam = false;
            }
            return;
        }
        if (mc.currentScreen == null) {
            double fwd = (ZyonClient.fcForward.isPressed() ? 1 : 0) - (ZyonClient.fcBack.isPressed() ? 1 : 0);
            double str = (ZyonClient.fcRight.isPressed() ? 1 : 0) - (ZyonClient.fcLeft.isPressed() ? 1 : 0);
            double vert = (ZyonClient.fcUp.isPressed() ? 1 : 0) - (ZyonClient.fcDown.isPressed() ? 1 : 0);
            double yaw = Math.toRadians(p.getYaw());
            double mx = -Math.sin(yaw) * fwd - Math.cos(yaw) * str;
            double mz = Math.cos(yaw) * fwd - Math.sin(yaw) * str;
            double len = Math.sqrt(mx * mx + mz * mz);
            if (len > 1.0) {
                mx /= len;
                mz /= len;
            }
            double spd = 0.2 * Math.max(1, Math.min(10, s.freecamSpeed));
            fcX += mx * spd;
            fcZ += mz * spd;
            fcY += vert * spd;
        }
        fcStand.setPosition(fcX, fcY - fcStand.getEyeHeight(EntityPose.STANDING), fcZ);
        fcStand.setYaw(p.getYaw());
        fcStand.setPitch(p.getPitch());
        if (mc.getCameraEntity() != fcStand) mc.setCameraEntity(fcStand);
    }

    // ---------- Auto Bridge ----------

    /** Velocidade 1..10 vira espera em ticks (10 = mais rapido). */
    private static int delayTicks(int speed) {
        return Math.max(0, (10 - Math.max(1, Math.min(10, speed))) / 2);
    }

    private static void autoBridge(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!s.autoBridge) return;
        if (bridgeTimer-- > 0) return;
        if (!p.isOnGround()) return;
        int slot = findBuildBlock(p);
        if (slot < 0) return;
        Vec3d vel = p.getVelocity();
        BlockPos[] cands = {
                BlockPos.ofFloored(p.getX(), p.getY() - 1.0, p.getZ()),
                BlockPos.ofFloored(p.getX() + vel.x * 3.0, p.getY() - 1.0, p.getZ() + vel.z * 3.0)
        };
        for (BlockPos pos : cands) {
            if (!w.getBlockState(pos).isAir()) continue;
            if (!w.getOtherEntities((Entity) null, new Box(pos)).isEmpty()) continue;
            if (placeBlock(mc, p, w, pos, slot, false)) {
                bridgeTimer = delayTicks(s.bridgeSpeed);
                return;
            }
        }
    }

    private static boolean placeBlock(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, BlockPos pos, int slot, boolean rotate) {
        Direction[] dirs = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : dirs) {
            BlockPos n = pos.offset(dir);
            if (!w.getBlockState(n).isSolidBlock(w, n)) continue;
            Direction face = dir.getOpposite();
            Vec3d hitVec = Vec3d.ofCenter(n).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
            int prev = p.getInventory().getSelectedSlot();
            selectSlot(p, slot);
            if (rotate) lookAtPoint(p, hitVec);
            mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hitVec, face, n, false));
            p.swingHand(Hand.MAIN_HAND);
            selectSlot(p, prev);
            return true;
        }
        return false;
    }

    private static boolean usableBlock(ItemStack st) {
        if (st.isEmpty() || !(st.getItem() instanceof BlockItem bi)) return false;
        Block b = bi.getBlock();
        if (b == Blocks.OBSIDIAN || b == Blocks.BEDROCK || b == Blocks.COBWEB) return false;
        return !(b instanceof FallingBlock) && !(b instanceof TntBlock) && !b.getDefaultState().hasBlockEntity();
    }

    private static int findBuildBlock(ClientPlayerEntity p) {
        int cur = p.getInventory().getSelectedSlot();
        if (usableBlock(p.getInventory().getStack(cur))) return cur;
        for (int i = 0; i < 9; i++) {
            if (usableBlock(p.getInventory().getStack(i))) return i;
        }
        return -1;
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
