package com.zyon.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ZyonHud {
    private static final int ACCENT = 0xFF9B7BFF;
    private static final int TEXT = 0xFFE6E9F2;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ZyonHud() {}

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) return;
        DiscoverySettings s = ZyonClient.SETTINGS;
        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        // ---- coluna esquerda: modulos ativos + informacoes ----
        int y = 4;
        if (s.hudModules) {
            ctx.drawText(tr, "Zyon", 4, y, ACCENT, true);
            y += 11;
            for (String n : activeModules(s)) {
                ctx.drawText(tr, n, 4, y, TEXT, true);
                y += 10;
            }
            y += 4;
        }
        List<String> info = new ArrayList<>();
        if (s.hudFps) info.add("FPS: " + mc.getCurrentFps());
        if (s.hudPing) info.add("Ping: " + ping(mc, p) + " ms");
        if (s.hudCoords) info.add(String.format(Locale.ROOT, "XYZ: %d %d %d", p.getBlockX(), p.getBlockY(), p.getBlockZ()));
        if (s.hudDirection) info.add("Dir: " + direction(p));
        if (s.hudClock) info.add("Hora: " + LocalTime.now().format(TIME));
        if (s.hudMemory) info.add(memory());
        if (s.hudBiome) info.add("Bioma: " + biome(w, p));
        if (s.hudSpeed) info.add(String.format(Locale.ROOT, "Vel: %.1f b/s (v %.1f)", ZyonModules.speedH, ZyonModules.speedV));
        if (s.hudCps) info.add("CPS: " + ZyonModules.cps());
        if (s.comboHud) info.add("Combo: " + ZyonModules.combo);
        if (s.reachHud) info.add(String.format(Locale.ROOT, "Reach: %.2f", ZyonModules.lastReach));
        if (s.critHud) info.add(ZyonModules.critReady ? "CRIT: pronto" : "CRIT: --");
        for (String l : info) {
            ctx.drawText(tr, l, 4, y, TEXT, true);
            y += 10;
        }

        // ---- coluna direita: achados do scanner + contadores CPvP ----
        int ry = 4;
        if (s.hudFindings && s.anyScannerEnabled()) {
            List<DiscoveryScanner.Finding> list = new ArrayList<>(ZyonClient.SCANNER.findings().values());
            list.sort((a, b) -> Integer.compare(b.count(), a.count()));
            String title = "Achados";
            ctx.drawText(tr, title, sw - 4 - tr.getWidth(title), ry, ACCENT, true);
            ry += 11;
            int shown = 0;
            for (DiscoveryScanner.Finding f : list) {
                if (shown++ >= 6) break;
                String line = f.label() + " x" + f.count() + "  (" + f.pos().getX() + ", " + f.pos().getZ() + ")";
                ctx.drawText(tr, line, sw - 4 - tr.getWidth(line), ry, TEXT, true);
                ry += 10;
            }
            ry += 4;
        }
        List<String> cnt = new ArrayList<>();
        if (s.cntCrystals) cnt.add("Crystals: " + ZyonModules.count(p, Items.END_CRYSTAL));
        if (s.cntTotems) cnt.add("Totems: " + ZyonModules.count(p, Items.TOTEM_OF_UNDYING));
        if (s.cntObsidian) cnt.add("Obsidian: " + ZyonModules.count(p, Items.OBSIDIAN));
        if (s.cntPearls) cnt.add("Pearls: " + ZyonModules.count(p, Items.ENDER_PEARL));
        if (s.cntAnchors) cnt.add("Anchors: " + ZyonModules.count(p, Items.RESPAWN_ANCHOR));
        if (s.holeHud) cnt.add("Hole: " + hole(w, p));
        if (s.explosionDmg) cnt.add(explosion(w, p));
        for (String l : cnt) {
            ctx.drawText(tr, l, sw - 4 - tr.getWidth(l), ry, TEXT, true);
            ry += 10;
        }

        // ---- armadura (lado esquerdo, meio) ----
        if (s.hudArmor) {
            int ay = sh / 2 - 36;
            for (EquipmentSlot slot : ARMOR) {
                ItemStack st = p.getEquippedStack(slot);
                if (!st.isEmpty()) {
                    ctx.drawItem(st, 4, ay);
                    if (st.isDamageable()) {
                        ctx.drawText(tr, ZyonModules.durabilityPercent(st) + "%", 24, ay + 4, TEXT, true);
                    }
                }
                ay += 18;
            }
        }

        // ---- efeitos de pocao ----
        if (s.hudEffects) {
            int ey = sh / 2 + 44;
            for (StatusEffectInstance e : p.getStatusEffects()) {
                String name = e.getEffectType().value().getName().getString();
                int amp = e.getAmplifier() + 1;
                String time = e.isInfinite() ? "inf" : fmt(e.getDuration() / 20);
                ctx.drawText(tr, name + (amp > 1 ? " " + amp : "") + "  " + time, 4, ey, TEXT, true);
                ey += 10;
            }
        }

        // ---- inventario ----
        if (s.hudInventory) {
            int ix = sw - 9 * 17 - 6;
            int iy = sh / 2 - 30;
            ctx.fill(ix - 3, iy - 3, ix + 9 * 17 + 1, iy + 3 * 17 + 1, 0x90000000);
            for (int i = 0; i < 27; i++) {
                ItemStack st = p.getInventory().getStack(9 + i);
                if (!st.isEmpty()) ctx.drawItem(st, ix + (i % 9) * 17, iy + (i / 9) * 17);
            }
        }

        // ---- keystrokes ----
        if (s.hudKeystrokes) {
            int bx = sw - 66;
            int by = sh - 72;
            key(ctx, tr, "W", bx + 21, by, mc.options.forwardKey);
            key(ctx, tr, "A", bx, by + 21, mc.options.leftKey);
            key(ctx, tr, "S", bx + 21, by + 21, mc.options.backKey);
            key(ctx, tr, "D", bx + 42, by + 21, mc.options.rightKey);
            wideKey(ctx, tr, "ESPACO", bx, by + 42, 60, mc.options.jumpKey);
        }

        // ---- mira custom ----
        if (s.customCrosshair) {
            int cx = sw / 2;
            int cy = sh / 2;
            ctx.fill(cx - 8, cy - 1, cx - 3, cy + 1, ACCENT);
            ctx.fill(cx + 3, cy - 1, cx + 8, cy + 1, ACCENT);
            ctx.fill(cx - 1, cy - 8, cx + 1, cy - 3, ACCENT);
            ctx.fill(cx - 1, cy + 3, cx + 1, cy + 8, ACCENT);
        }

        // ---- target HUD ----
        if (s.targetHud) {
            PlayerEntity t = ZyonModules.nearestEnemy(p, w, 8);
            if (t != null) {
                int tx = sw / 2 + 16;
                int ty = sh / 2 + 16;
                ctx.fill(tx, ty, tx + 112, ty + 28, 0xA0000000);
                ctx.drawText(tr, t.getName().getString(), tx + 4, ty + 4, TEXT, false);
                float hp = t.getHealth();
                float max = Math.max(1.0f, t.getMaxHealth());
                float frac = MathHelper.clamp(hp / max, 0.0f, 1.0f);
                ctx.fill(tx + 4, ty + 16, tx + 108, ty + 23, 0xFF2A3045);
                int col = frac > 0.5f ? 0xFF34D399 : (frac > 0.25f ? 0xFFFBBF24 : 0xFFEF4444);
                ctx.fill(tx + 4, ty + 16, tx + 4 + (int) (104 * frac), ty + 23, col);
                String hs = String.format(Locale.ROOT, "%.1f", hp);
                ctx.drawText(tr, hs, tx + 108 - tr.getWidth(hs), ty + 4, TEXT, false);
            }
        }

        // ---- avisos ----
        int wy = sh / 2 - 44;
        if (s.totemWarning && !p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                && !p.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            center(ctx, tr, "SEM TOTEM NA MAO", sw, wy, 0xFFEF4444);
            wy -= 12;
        }
        if (s.durabilityWarning) {
            boolean low = false;
            for (EquipmentSlot slot : ARMOR) {
                ItemStack st = p.getEquippedStack(slot);
                if (!st.isEmpty() && st.isDamageable() && ZyonModules.durabilityPercent(st) < 15) low = true;
            }
            if (low) center(ctx, tr, "ARMADURA FRACA", sw, wy, 0xFFFBBF24);
        }
    }

    private static void center(DrawContext ctx, TextRenderer tr, String t, int sw, int y, int color) {
        ctx.drawText(tr, t, sw / 2 - tr.getWidth(t) / 2, y, color, true);
    }

    private static void key(DrawContext ctx, TextRenderer tr, String label, int x, int y, KeyBinding kb) {
        ctx.fill(x, y, x + 18, y + 18, kb.isPressed() ? 0xC07C5CFF : 0x90000000);
        ctx.drawText(tr, label, x + 9 - tr.getWidth(label) / 2, y + 5, 0xFFFFFFFF, false);
    }

    private static void wideKey(DrawContext ctx, TextRenderer tr, String label, int x, int y, int w, KeyBinding kb) {
        ctx.fill(x, y, x + w, y + 18, kb.isPressed() ? 0xC07C5CFF : 0x90000000);
        ctx.drawText(tr, label, x + w / 2 - tr.getWidth(label) / 2, y + 5, 0xFFFFFFFF, false);
    }

    private static List<String> activeModules(DiscoverySettings s) {
        List<String> l = new ArrayList<>();
        if (s.killAura) l.add("KillAura");
        if (s.autoCrystal) l.add("AutoCrystal");
        if (s.autoObsidian) l.add("AutoObsidian");
        if (s.autoTotem) l.add("AutoTotem");
        if (s.autoBridge) l.add("AutoBridge");
        if (s.xray) l.add("X-Ray");
        if (s.nuker) l.add("Nuker");
        if (s.cpvpAll) l.add("CPvP");
        if (s.triggerBot) l.add("TriggerBot");
        if (s.autoClicker) l.add("AutoClicker");
        if (s.aimAssist) l.add("AimAssist");
        if (s.autoSprint) l.add("AutoSprint");
        if (s.autoEat) l.add("AutoEat");
        if (s.autoRefill) l.add("AutoRefill");
        if (s.fullbright) l.add("Fullbright");
        if (s.playerEsp) l.add("ESP");
        if (s.zoom) l.add("Zoom");
        if (s.baseFinder) l.add("BaseFinder");
        if (s.chunkFinder) l.add("ChunkFinder");
        if (s.amethystScanner) l.add("Amethyst");
        return l;
    }

    private static String ping(MinecraftClient mc, ClientPlayerEntity p) {
        if (mc.getNetworkHandler() == null) return "-";
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
        return e == null ? "-" : String.valueOf(e.getLatency());
    }

    private static String direction(ClientPlayerEntity p) {
        Direction d = p.getHorizontalFacing();
        String n = switch (d) {
            case NORTH -> "Norte (-Z)";
            case SOUTH -> "Sul (+Z)";
            case EAST -> "Leste (+X)";
            case WEST -> "Oeste (-X)";
            default -> "?";
        };
        return n + " " + Math.round(MathHelper.wrapDegrees(p.getYaw()));
    }

    private static String memory() {
        Runtime r = Runtime.getRuntime();
        long used = (r.totalMemory() - r.freeMemory()) / 1048576L;
        long max = r.maxMemory() / 1048576L;
        return "Mem: " + used + "/" + max + " MB";
    }

    private static String biome(ClientWorld w, ClientPlayerEntity p) {
        return w.getBiome(p.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("?");
    }

    private static String hole(ClientWorld w, ClientPlayerEntity p) {
        BlockPos b = p.getBlockPos();
        int obs = 0;
        int bed = 0;
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction d : dirs) {
            BlockState st = w.getBlockState(b.offset(d));
            if (st.isOf(Blocks.BEDROCK)) bed++;
            else if (st.isOf(Blocks.OBSIDIAN)) obs++;
        }
        int t = obs + bed;
        if (t == 4) return bed == 4 ? "Bedrock (seguro)" : "Obsidian (seguro)";
        return t + "/4 paredes";
    }

    private static String explosion(ClientWorld w, ClientPlayerEntity p) {
        List<EndCrystalEntity> l = w.getEntitiesByClass(EndCrystalEntity.class, p.getBoundingBox().expand(12), e -> e.isAlive());
        EndCrystalEntity near = null;
        double bd = 1e9;
        for (EndCrystalEntity c : l) {
            double d = p.distanceTo(c);
            if (d < bd) {
                bd = d;
                near = c;
            }
        }
        if (near == null || bd >= 12.0) return "Crystal: nenhum perto";
        double impact = 1.0 - bd / 12.0;
        double dmg = (impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0;
        return String.format(Locale.ROOT, "Crystal %.1fb: ~%.0f dmg (sem armadura)", bd, dmg);
    }

    private static String fmt(int seconds) {
        return (seconds / 60) + ":" + String.format(Locale.ROOT, "%02d", seconds % 60);
    }
}
