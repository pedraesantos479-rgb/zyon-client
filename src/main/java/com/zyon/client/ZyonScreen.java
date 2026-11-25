package com.zyon.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class ZyonScreen extends Screen {
    private static final int W = 360, H = 240, HEADER = 32, SIDE = 84, ROW_H = 26;
    private static final int BG = 0xF00E111A;
    private static final int HEAD = 0xFF151A28;
    private static final int SIDEBG = 0xFF111522;
    private static final int CARD = 0xFF181D2D;
    private static final int CARD_HOVER = 0xFF202741;
    private static final int ACCENT = 0xFF7C5CFF;
    private static final int ACCENT_LIGHT = 0xFFB59CFF;
    private static final int GREEN = 0xFF34D399;
    private static final int OFF = 0xFF3A4156;
    private static final int TEXT = 0xFFE6E9F2;
    private static final int DIM = 0xFF8A93A8;
    private static final String[] TABS = {"Combate", "Jogador", "Visual", "Scanner"};

    private static int tab = 0;
    private int left, top;

    private static final class Row {
        final String name;
        final BooleanSupplier on;
        final Supplier<String> value;
        final Runnable click;

        Row(String name, BooleanSupplier on, Supplier<String> value, Runnable click) {
            this.name = name;
            this.on = on;
            this.value = value;
            this.click = click;
        }
    }

    private static Row toggle(String name, BooleanSupplier get, Runnable flip) {
        return new Row(name, get, null, flip);
    }

    private static Row cycle(String name, Supplier<String> val, Runnable next) {
        return new Row(name, null, val, next);
    }

    private static int next(int cur, int[] opts) {
        for (int i = 0; i < opts.length; i++) {
            if (opts[i] == cur) return opts[(i + 1) % opts.length];
        }
        return opts[0];
    }

    private List<Row> rows() {
        DiscoverySettings s = ZyonClient.SETTINGS;
        List<Row> r = new ArrayList<>();
        switch (tab) {
            case 0 -> {
                r.add(toggle("KillAura", () -> s.killAura, () -> s.killAura = !s.killAura));
                r.add(cycle("Alcance KillAura", () -> s.killAuraRange + " blocos",
                        () -> s.killAuraRange = next(s.killAuraRange, new int[]{3, 4, 5, 6})));
                r.add(toggle("KillAura so jogadores", () -> s.killAuraPlayersOnly,
                        () -> s.killAuraPlayersOnly = !s.killAuraPlayersOnly));
                r.add(toggle("Auto Crystal", () -> s.autoCrystal, () -> s.autoCrystal = !s.autoCrystal));
                r.add(cycle("Alcance Crystal", () -> s.crystalRange + " blocos",
                        () -> s.crystalRange = next(s.crystalRange, new int[]{3, 4, 5, 6})));
                r.add(cycle("Delay Crystal", () -> s.crystalDelay + " ticks",
                        () -> s.crystalDelay = next(s.crystalDelay, new int[]{0, 1, 2, 3, 5})));
                r.add(toggle("Auto Obsidian", () -> s.autoObsidian, () -> s.autoObsidian = !s.autoObsidian));
            }
            case 1 -> {
                r.add(toggle("Auto Sprint", () -> s.autoSprint, () -> s.autoSprint = !s.autoSprint));
                r.add(toggle("Fullbright", () -> s.fullbright, () -> s.fullbright = !s.fullbright));
            }
            case 2 -> {
                r.add(toggle("HUD modulos ativos", () -> s.hudModules, () -> s.hudModules = !s.hudModules));
                r.add(toggle("HUD achados do scanner", () -> s.hudFindings, () -> s.hudFindings = !s.hudFindings));
                r.add(toggle("ESP de jogadores", () -> s.playerEsp, () -> s.playerEsp = !s.playerEsp));
            }
            default -> {
                r.add(toggle("Base Finder", () -> s.baseFinder, () -> s.baseFinder = !s.baseFinder));
                r.add(toggle("Chunk Finder", () -> s.chunkFinder, () -> s.chunkFinder = !s.chunkFinder));
                r.add(toggle("Amethyst Scanner", () -> s.amethystScanner, () -> s.amethystScanner = !s.amethystScanner));
                r.add(cycle("Modo Finder", () -> s.finderMode.name(), s::cycleFinderMode));
                r.add(cycle("Modo Amethyst", () -> s.amethystMode.name(), s::cycleAmethystMode));
                r.add(cycle("Alcance scanner", () -> s.rangeChunks + " chunks",
                        () -> s.rangeChunks = next(s.rangeChunks, new int[]{2, 3, 4, 6, 8})));
            }
        }
        return r;
    }

    public ZyonScreen() { super(Text.literal("Zyon Client")); }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        // botoes invisiveis (so recebem o toque); o visual e desenhado em render()
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> { tab = idx; clearAndInit(); })
                    .dimensions(left + 6, top + HEADER + 10 + i * 26, SIDE - 12, 22).build());
        }
        List<Row> rows = rows();
        for (int i = 0; i < rows.size(); i++) {
            final Row row = rows.get(i);
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> row.click.run())
                    .dimensions(left + SIDE + 8, top + HEADER + 8 + i * ROW_H, W - SIDE - 16, ROW_H - 4).build());
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int x0 = left, y0 = top;
        ctx.fill(x0, y0, x0 + W, y0 + H, BG);
        ctx.fill(x0, y0, x0 + W, y0 + HEADER, HEAD);
        ctx.fill(x0, y0 + HEADER, x0 + W, y0 + HEADER + 1, ACCENT);
        ctx.drawText(textRenderer, "ZYON", x0 + 14, y0 + 12, ACCENT_LIGHT, false);
        ctx.drawText(textRenderer, "CLIENT", x0 + 46, y0 + 12, TEXT, false);
        String ver = "1.21.11";
        ctx.drawText(textRenderer, ver, x0 + W - 14 - textRenderer.getWidth(ver), y0 + 12, DIM, false);

        ctx.fill(x0, y0 + HEADER + 1, x0 + SIDE, y0 + H, SIDEBG);
        for (int i = 0; i < TABS.length; i++) {
            int tx = x0 + 6, ty = y0 + HEADER + 10 + i * 26, tw = SIDE - 12;
            boolean sel = i == tab;
            boolean hov = mouseX >= tx && mouseX < tx + tw && mouseY >= ty && mouseY < ty + 22;
            ctx.fill(tx, ty, tx + tw, ty + 22, sel ? 0xFF2A2350 : (hov ? 0xFF1E2333 : SIDEBG));
            if (sel) ctx.fill(tx, ty, tx + 2, ty + 22, ACCENT);
            ctx.drawText(textRenderer, TABS[i], tx + 10, ty + 7, sel ? TEXT : DIM, false);
        }

        List<Row> rows = rows();
        int cx = x0 + SIDE + 8, cw = W - SIDE - 16;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int ry = y0 + HEADER + 8 + i * ROW_H;
            boolean hov = mouseX >= cx && mouseX < cx + cw && mouseY >= ry && mouseY < ry + ROW_H - 4;
            ctx.fill(cx, ry, cx + cw, ry + ROW_H - 4, hov ? CARD_HOVER : CARD);
            ctx.drawText(textRenderer, row.name, cx + 10, ry + 7, TEXT, false);
            if (row.on != null) {
                boolean on = row.on.getAsBoolean();
                int px = cx + cw - 10 - 30, py = ry + 4;
                ctx.fill(px, py, px + 30, py + 14, on ? GREEN : OFF);
                int kx = on ? px + 17 : px + 3;
                ctx.fill(kx, py + 2, kx + 10, py + 12, 0xFFFFFFFF);
            } else {
                String v = row.value.get();
                int vw = textRenderer.getWidth(v) + 14;
                int vx = cx + cw - 8 - vw;
                ctx.fill(vx, ry + 3, vx + vw, ry + ROW_H - 7, 0xFF2A3045);
                ctx.drawText(textRenderer, v, vx + 7, ry + 7, ACCENT_LIGHT, false);
            }
        }
        ctx.drawText(textRenderer, "ESC - fechar", x0 + SIDE + 8, y0 + H - 12, DIM, false);
    }

    @Override
    public boolean shouldPause() { return false; }
}
