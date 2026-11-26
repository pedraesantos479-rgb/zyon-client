package com.zyon.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ESP em 2D: projeta a posicao 3D na tela e desenha por cima do HUD.
 * Funciona atraves de paredes e nao mexe no pipeline 3D do jogo.
 * So e preciso em primeira pessoa.
 */
public final class ZyonEsp {
    private static final int PLAYER = 0xFFFF4D6D;
    private static final int HOSTILE = 0xFFFBBF24;
    private static final int PASSIVE = 0xFF34D399;
    private static final int ITEM = 0xFF60A5FA;
    private static final int CHEST = 0xFFF59E0B;
    private static final int STORAGE = 0xFFA78BFA;
    private static final int WHITE = 0xFFFFFFFF;

    private record Spot(BlockPos pos, int kind) {}

    private static volatile List<Spot> spots = new ArrayList<>();
    private static int scanTimer;
    private static volatile List<Spot> xrayList = new ArrayList<>();
    private static int xrayTimer;

    // camera
    private static double cx, cy, cz, fx, fy, fz, rx, ry, rz, ux, uy, uz, scale;
    private static int sw, sh;
    private static int tracerCount;

    private ZyonEsp() {}

    // ---------- varredura de baus (a cada 1s) ----------

    public static void tickScan(MinecraftClient mc, ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!(s.chestEsp || s.storageEsp)) {
            if (!spots.isEmpty()) spots = new ArrayList<>();
            return;
        }
        if (scanTimer-- > 0) return;
        scanTimer = 20;
        List<Spot> n = new ArrayList<>();
        int r = Math.max(1, Math.min(8, s.espRange / 16));
        int pcx = p.getChunkPos().x;
        int pcz = p.getChunkPos().z;
        for (int x = pcx - r; x <= pcx + r; x++) {
            for (int z = pcz - r; z <= pcz + r; z++) {
                WorldChunk c = w.getChunkManager().getWorldChunk(x, z);
                if (c == null) continue;
                for (BlockEntity be : c.getBlockEntities().values()) {
                    int kind = classify(be);
                    if (kind < 0) continue;
                    if (kind <= 1 && !(s.chestEsp || s.storageEsp)) continue;
                    if (kind >= 2 && !s.storageEsp) continue;
                    n.add(new Spot(be.getPos(), kind));
                }
            }
        }
        spots = n;
    }

    // ---------- X-Ray (marca minerios atraves das paredes) ----------

    public static void tickXray(ClientPlayerEntity p, ClientWorld w, DiscoverySettings s) {
        if (!s.xray) {
            if (!xrayList.isEmpty()) xrayList = new ArrayList<>();
            return;
        }
        if (xrayTimer-- > 0) return;
        xrayTimer = 30;
        int r = Math.max(4, Math.min(24, s.xrayRange));
        List<Spot> n = new ArrayList<>();
        BlockPos.Mutable m = new BlockPos.Mutable();
        int px = p.getBlockX();
        int py = p.getBlockY();
        int pz = p.getBlockZ();
        int minY = Math.max(w.getBottomY(), py - r);
        int maxY = Math.min(w.getBottomY() + w.getHeight() - 1, py + r);
        outer:
        for (int x = px - r; x <= px + r; x++) {
            for (int z = pz - r; z <= pz + r; z++) {
                if (w.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) continue;
                for (int y = minY; y <= maxY; y++) {
                    m.set(x, y, z);
                    BlockState st = w.getBlockState(m);
                    int col = oreColor(st.getBlock(), s.xrayRare);
                    if (col != 0) {
                        n.add(new Spot(m.toImmutable(), col));
                        if (n.size() >= 400) break outer;
                    }
                }
            }
        }
        xrayList = n;
    }

    public static boolean isOre(Block b) {
        return oreColor(b, false) != 0;
    }

    private static int oreColor(Block b, boolean rareOnly) {
        if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) return 0xFF22D3EE;
        if (b == Blocks.ANCIENT_DEBRIS) return 0xFFD97706;
        if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) return 0xFF22C55E;
        if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE) return 0xFFFACC15;
        if (rareOnly) return 0;
        if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) return 0xFFD6B79B;
        if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) return 0xFF9CA3AF;
        if (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE) return 0xFFF97316;
        if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) return 0xFF3B82F6;
        if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) return 0xFFEF4444;
        if (b == Blocks.NETHER_QUARTZ_ORE) return 0xFFF3F4F6;
        return 0;
    }

    private static int classify(BlockEntity be) {
        if (be instanceof ChestBlockEntity) return 0;
        if (be instanceof EnderChestBlockEntity) return 1;
        if (be instanceof BarrelBlockEntity) return 2;
        if (be instanceof ShulkerBoxBlockEntity) return 3;
        if (be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity
                || be instanceof AbstractFurnaceBlockEntity) return 4;
        return -1;
    }

    // ---------- desenho ----------

    public static void render(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) return;
        DiscoverySettings s = ZyonClient.SETTINGS;
        if (!(s.playerEsp || s.entityEsp || s.itemEsp || s.chestEsp || s.storageEsp
                || s.nameTags || s.hitboxViz || s.xray)) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;

        TextRenderer tr = mc.textRenderer;
        sw = mc.getWindow().getScaledWidth();
        sh = mc.getWindow().getScaledHeight();
        tracerCount = 0;

        Vec3d lp = p.getLerpedPos(delta);
        cx = lp.x;
        cy = lp.y + p.getEyeHeight(p.getPose());
        cz = lp.z;
        double yaw = Math.toRadians(p.getYaw());
        double pit = Math.toRadians(p.getPitch());
        fx = -Math.sin(yaw) * Math.cos(pit);
        fy = -Math.sin(pit);
        fz = Math.cos(yaw) * Math.cos(pit);
        rx = -Math.cos(yaw);
        ry = 0.0;
        rz = -Math.sin(yaw);
        ux = ry * fz - rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy - ry * fx;
        scale = (sh / 2.0) / Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);

        double range = s.espRange;

        // jogadores
        if (s.playerEsp || s.nameTags) {
            for (PlayerEntity e : w.getPlayers()) {
                if (e == p || !e.isAlive() || e.isSpectator()) continue;
                double d = p.distanceTo(e);
                if (d > range) continue;
                Box b = lerpBox(e, delta);
                if (s.playerEsp) drawTarget(ctx, b, PLAYER, true, s);
                if (s.nameTags) nameTag(ctx, tr, e, b, d);
            }
        }

        // entidades e itens
        if (s.entityEsp || s.itemEsp) {
            for (Entity e : w.getEntities()) {
                if (e == p || e instanceof PlayerEntity) continue;
                double d = p.distanceTo(e);
                if (d > range) continue;
                if (e instanceof ItemEntity ie) {
                    if (!s.itemEsp) continue;
                    Box b = lerpBox(e, delta);
                    drawTarget(ctx, b, ITEM, false, s);
                    if (d <= 24) {
                        ItemStack st = ie.getStack();
                        String label = st.getName().getString() + (st.getCount() > 1 ? " x" + st.getCount() : "");
                        labelAbove(ctx, tr, b, label, ITEM);
                    }
                    continue;
                }
                if (s.entityEsp && e instanceof LivingEntity && e.isAlive()) {
                    Box b = lerpBox(e, delta);
                    int col = e instanceof HostileEntity ? HOSTILE : PASSIVE;
                    drawTarget(ctx, b, col, false, s);
                    if (d <= 24) labelAbove(ctx, tr, b, e.getName().getString(), col);
                }
            }
        }

        // baus e storage
        if (s.chestEsp || s.storageEsp) {
            for (Spot sp : spots) {
                BlockPos bp = sp.pos();
                double dx = bp.getX() + 0.5 - lp.x;
                double dy = bp.getY() + 0.5 - lp.y;
                double dz = bp.getZ() + 0.5 - lp.z;
                if (dx * dx + dy * dy + dz * dz > range * range) continue;
                drawTarget(ctx, new Box(bp), sp.kind() <= 1 ? CHEST : STORAGE, false, s);
            }
        }

        // x-ray (minerios)
        if (s.xray) {
            double xr = Math.min(24, s.xrayRange) + 2.0;
            for (Spot sp : xrayList) {
                BlockPos bp = sp.pos();
                double dx = bp.getX() + 0.5 - lp.x;
                double dy = bp.getY() + 0.5 - lp.y;
                double dz = bp.getZ() + 0.5 - lp.z;
                if (dx * dx + dy * dy + dz * dz > xr * xr * 3) continue;
                drawTarget(ctx, new Box(bp), sp.kind(), true, s);
            }
        }

        // hitbox 3D do alvo mais perto
        if (s.hitboxViz) {
            PlayerEntity t = ZyonModules.nearestEnemy(p, w, 12);
            if (t != null) wire(ctx, lerpBox(t, delta), WHITE);
        }
    }

    private static Box lerpBox(Entity e, float delta) {
        Vec3d lp = e.getLerpedPos(delta);
        return e.getBoundingBox().offset(lp.x - e.getX(), lp.y - e.getY(), lp.z - e.getZ());
    }

    private static double[] project(double x, double y, double z) {
        double dx = x - cx;
        double dy = y - cy;
        double dz = z - cz;
        double zz = dx * fx + dy * fy + dz * fz;
        if (zz < 0.1) return null;
        double xx = dx * rx + dy * ry + dz * rz;
        double yy = dx * ux + dy * uy + dz * uz;
        return new double[]{sw / 2.0 + xx / zz * scale, sh / 2.0 - yy / zz * scale, zz};
    }

    private static double[] projectBox(Box b) {
        double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
        for (int i = 0; i < 8; i++) {
            double[] pt = project((i & 1) == 0 ? b.minX : b.maxX,
                    (i & 2) == 0 ? b.minY : b.maxY,
                    (i & 4) == 0 ? b.minZ : b.maxZ);
            if (pt == null) return null;
            minX = Math.min(minX, pt[0]);
            maxX = Math.max(maxX, pt[0]);
            minY = Math.min(minY, pt[1]);
            maxY = Math.max(maxY, pt[1]);
        }
        return new double[]{minX, minY, maxX, maxY};
    }

    private static int clamp(double v) {
        return (int) Math.max(-4000, Math.min(4000, v));
    }

    private static void drawTarget(DrawContext ctx, Box box, int color, boolean filled, DiscoverySettings s) {
        double[] r = projectBox(box);
        if (r == null) return;
        int x1 = clamp(r[0]);
        int y1 = clamp(r[1]);
        int x2 = Math.max(x1 + 2, clamp(r[2]));
        int y2 = Math.max(y1 + 2, clamp(r[3]));
        if (x2 < -50 || x1 > sw + 50 || y2 < -50 || y1 > sh + 50) return;
        if (filled) ctx.fill(x1, y1, x2, y2, (color & 0x00FFFFFF) | 0x28000000);
        ctx.fill(x1, y1, x2, y1 + 1, color);
        ctx.fill(x1, y2 - 1, x2, y2, color);
        ctx.fill(x1, y1, x1 + 1, y2, color);
        ctx.fill(x2 - 1, y1, x2, y2, color);
        if (s.tracers && tracerCount < 14) {
            tracerCount++;
            line(ctx, sw / 2, sh, (x1 + x2) / 2, (y1 + y2) / 2, color);
        }
    }

    private static void labelAbove(DrawContext ctx, TextRenderer tr, Box box, String text, int color) {
        double[] t = project((box.minX + box.maxX) / 2.0, box.maxY + 0.15, (box.minZ + box.maxZ) / 2.0);
        if (t == null) return;
        int w = tr.getWidth(text);
        int x = clamp(t[0]) - w / 2;
        int y = clamp(t[1]) - 11;
        ctx.drawText(tr, text, x, y, color, true);
    }

    private static void nameTag(DrawContext ctx, TextRenderer tr, PlayerEntity e, Box box, double dist) {
        double[] t = project((box.minX + box.maxX) / 2.0, box.maxY + 0.3, (box.minZ + box.maxZ) / 2.0);
        if (t == null) return;
        String text = e.getName().getString() + "  "
                + String.format(Locale.ROOT, "%.0f", e.getHealth()) + "hp  " + (int) dist + "m";
        int w = tr.getWidth(text);
        int x = clamp(t[0]) - w / 2;
        int y = clamp(t[1]) - 12;
        ctx.fill(x - 3, y - 2, x + w + 3, y + 10, 0x99000000);
        ctx.drawText(tr, text, x, y, 0xFFFFFFFF, false);
    }

    private static void wire(DrawContext ctx, Box b, int color) {
        double[][] pts = new double[8][];
        for (int i = 0; i < 8; i++) {
            pts[i] = project((i & 1) == 0 ? b.minX : b.maxX,
                    (i & 2) == 0 ? b.minY : b.maxY,
                    (i & 4) == 0 ? b.minZ : b.maxZ);
        }
        int[] bits = {1, 2, 4};
        for (int i = 0; i < 8; i++) {
            for (int bit : bits) {
                if ((i & bit) != 0) continue;
                double[] a = pts[i];
                double[] c = pts[i | bit];
                if (a == null || c == null) continue;
                line(ctx, clamp(a[0]), clamp(a[1]), clamp(c[0]), clamp(c[1]), color);
            }
        }
    }

    private static void line(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int len = Math.max(Math.abs(dx), Math.abs(dy));
        if (len == 0) return;
        int steps = Math.min(400, len / 2 + 1);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            int x = (int) (x1 + dx * t);
            int y = (int) (y1 + dy * t);
            ctx.fill(x, y, x + 2, y + 2, color);
        }
    }
}
