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
    private static final int W = 410, H = 240, HEADER = 34, SIDE = 88, ROW_H = 28, PER_COL = 7;
    private static final int TAB_STEP = 30, TAB_H = 24;
    private static final int BG = 0xF00B0E17;
    private static final int SIDEBG = 0xFF0F1320;
    private static final int CARD = 0xFF161B2B;
    private static final int CARD_HOVER = 0xFF1E2540;
    private static final int ACCENT = 0xFF7C5CFF;
    private static final int ACCENT_LIGHT = 0xFFB59CFF;
    private static final int GREEN = 0xFF34D399;
    private static final int OFF = 0xFF3A4156;
    private static final int TEXT = 0xFFE6E9F2;
    private static final int DIM = 0xFF7F889D;
    private static final String[] TABS = {"Combate", "Jogador", "Visual", "HUD", "CPvP", "Scanner"};
    private static final String[] ICONS = {"C", "J", "V", "H", "P", "S"};
    private static final int[] ICON_COLORS = {
            0xFFEF4444, 0xFF34D399, 0xFF60A5FA, 0xFFFBBF24, 0xFFF472B6, 0xFFA78BFA};

    private static int tab = 0;
    private int left, top;

    private static final class Row {
        final String name, desc;
        final BooleanSupplier on;
        final Supplier<String> value;
        final Runnable click;

        Row(String name, String desc, BooleanSupplier on, Supplier<String> value, Runnable click) {
            this.name = name;
            this.desc = desc;
            this.on = on;
            this.value = value;
            this.click = click;
        }
    }

    private static Row toggle(String name, String desc, BooleanSupplier get, Runnable flip) {
        return new Row(name, desc, get, null, flip);
    }

    private static Row cycle(String name, String desc, Supplier<String> val, Runnable next) {
        return new Row(name, desc, null, val, next);
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
                r.add(toggle("KillAura", "Ataca o alvo mais perto", () -> s.killAura, () -> s.killAura = !s.killAura));
                r.add(cycle("Alcance KillAura", "Distancia do ataque", () -> s.killAuraRange + " bl",
                        () -> s.killAuraRange = next(s.killAuraRange, new int[]{3, 4, 5, 6})));
                r.add(toggle("So jogadores", "Ignora mobs", () -> s.killAuraPlayersOnly,
                        () -> s.killAuraPlayersOnly = !s.killAuraPlayersOnly));
                r.add(toggle("Auto Crystal", "Poe e quebra crystals", () -> s.autoCrystal, () -> s.autoCrystal = !s.autoCrystal));
                r.add(cycle("Alcance Crystal", "Distancia do crystal", () -> s.crystalRange + " bl",
                        () -> s.crystalRange = next(s.crystalRange, new int[]{3, 4, 5, 6})));
                r.add(cycle("Delay Crystal", "Espera entre acoes", () -> s.crystalDelay + " tk",
                        () -> s.crystalDelay = next(s.crystalDelay, new int[]{0, 1, 2, 3, 5})));
                r.add(toggle("Auto Obsidian", "Poe a base do crystal", () -> s.autoObsidian, () -> s.autoObsidian = !s.autoObsidian));
                r.add(toggle("TriggerBot", "Ataca ao mirar", () -> s.triggerBot, () -> s.triggerBot = !s.triggerBot));
                r.add(toggle("AutoClicker", "Clica segurando o botao", () -> s.autoClicker, () -> s.autoClicker = !s.autoClicker));
                r.add(cycle("CPS Clicker", "Cliques por segundo", () -> s.autoClickerCps + " cps",
                        () -> s.autoClickerCps = next(s.autoClickerCps, new int[]{6, 8, 10, 12, 15})));
                r.add(toggle("Aim Assist", "Ajuda a mirar", () -> s.aimAssist, () -> s.aimAssist = !s.aimAssist));
                r.add(cycle("Forca Aim", "Intensidade da ajuda", () -> s.aimStrength + "%",
                        () -> s.aimStrength = next(s.aimStrength, new int[]{10, 20, 30, 50})));
                r.add(toggle("Hit Sounds", "Som ao acertar", () -> s.hitSounds, () -> s.hitSounds = !s.hitSounds));
            }
            case 1 -> {
                r.add(toggle("Auto Sprint", "Corre sozinho", () -> s.autoSprint, () -> s.autoSprint = !s.autoSprint));
                r.add(toggle("Toggle Sprint", "Sprint travado", () -> s.toggleSprint, () -> s.toggleSprint = !s.toggleSprint));
                r.add(toggle("Toggle Sneak", "Agachar travado", () -> s.toggleSneak, () -> s.toggleSneak = !s.toggleSneak));
                r.add(toggle("Auto Eat", "Come com fome", () -> s.autoEat, () -> s.autoEat = !s.autoEat));
                r.add(toggle("Auto Refill", "Repoe a hotbar", () -> s.autoRefill, () -> s.autoRefill = !s.autoRefill));
                r.add(toggle("Auto Respawn", "Renasce sozinho", () -> s.autoRespawn, () -> s.autoRespawn = !s.autoRespawn));
                r.add(toggle("Fullbright", "Ve no escuro", () -> s.fullbright, () -> s.fullbright = !s.fullbright));
                r.add(toggle("Zoom", "Segure a tecla C", () -> s.zoom, () -> s.zoom = !s.zoom));
            }
            case 2 -> {
                r.add(toggle("ESP Jogadores", "Caixa nos jogadores", () -> s.playerEsp, () -> s.playerEsp = !s.playerEsp));
                r.add(toggle("ESP Entidades", "Mobs e animais", () -> s.entityEsp, () -> s.entityEsp = !s.entityEsp));
                r.add(toggle("ESP Itens", "Itens no chao", () -> s.itemEsp, () -> s.itemEsp = !s.itemEsp));
                r.add(toggle("ESP Baus", "Baus e ender chest", () -> s.chestEsp, () -> s.chestEsp = !s.chestEsp));
                r.add(toggle("ESP Storage", "Barril, shulker, funil", () -> s.storageEsp, () -> s.storageEsp = !s.storageEsp));
                r.add(toggle("Tracers", "Linhas ate os alvos", () -> s.tracers, () -> s.tracers = !s.tracers));
                r.add(toggle("Name Tags", "Nome, vida e distancia", () -> s.nameTags, () -> s.nameTags = !s.nameTags));
                r.add(toggle("Hitbox", "Caixa 3D do alvo", () -> s.hitboxViz, () -> s.hitboxViz = !s.hitboxViz));
                r.add(cycle("Alcance ESP", "Distancia maxima", () -> s.espRange + " bl",
                        () -> s.espRange = next(s.espRange, new int[]{32, 64, 96, 128})));
                r.add(toggle("HUD modulos", "Lista de ativos", () -> s.hudModules, () -> s.hudModules = !s.hudModules));
                r.add(toggle("HUD achados", "Resultados do scanner", () -> s.hudFindings, () -> s.hudFindings = !s.hudFindings));
                r.add(toggle("Mira custom", "Mira colorida", () -> s.customCrosshair, () -> s.customCrosshair = !s.customCrosshair));
            }
            case 3 -> {
                r.add(toggle("FPS", "Quadros por segundo", () -> s.hudFps, () -> s.hudFps = !s.hudFps));
                r.add(toggle("Ping", "Latencia do servidor", () -> s.hudPing, () -> s.hudPing = !s.hudPing));
                r.add(toggle("Coordenadas", "Posicao XYZ", () -> s.hudCoords, () -> s.hudCoords = !s.hudCoords));
                r.add(toggle("Direcao", "Para onde olha", () -> s.hudDirection, () -> s.hudDirection = !s.hudDirection));
                r.add(toggle("Relogio", "Hora do aparelho", () -> s.hudClock, () -> s.hudClock = !s.hudClock));
                r.add(toggle("Memoria", "RAM do jogo", () -> s.hudMemory, () -> s.hudMemory = !s.hudMemory));
                r.add(toggle("Bioma", "Bioma atual", () -> s.hudBiome, () -> s.hudBiome = !s.hudBiome));
                r.add(toggle("Velocimetro", "Blocos por segundo", () -> s.hudSpeed, () -> s.hudSpeed = !s.hudSpeed));
                r.add(toggle("CPS", "Seus cliques", () -> s.hudCps, () -> s.hudCps = !s.hudCps));
                r.add(toggle("Keystrokes", "Teclas WASD", () -> s.hudKeystrokes, () -> s.hudKeystrokes = !s.hudKeystrokes));
                r.add(toggle("Armadura", "Peças e durabilidade", () -> s.hudArmor, () -> s.hudArmor = !s.hudArmor));
                r.add(toggle("Efeitos", "Efeitos de pocao", () -> s.hudEffects, () -> s.hudEffects = !s.hudEffects));
                r.add(toggle("Inventario", "Itens do inventario", () -> s.hudInventory, () -> s.hudInventory = !s.hudInventory));
            }
            case 4 -> {
                r.add(toggle("Crystals", "Conta seus crystals", () -> s.cntCrystals, () -> s.cntCrystals = !s.cntCrystals));
                r.add(toggle("Totems", "Conta seus totems", () -> s.cntTotems, () -> s.cntTotems = !s.cntTotems));
                r.add(toggle("Obsidian", "Conta obsidian", () -> s.cntObsidian, () -> s.cntObsidian = !s.cntObsidian));
                r.add(toggle("Ender Pearls", "Conta pearls", () -> s.cntPearls, () -> s.cntPearls = !s.cntPearls));
                r.add(toggle("Anchors", "Conta anchors", () -> s.cntAnchors, () -> s.cntAnchors = !s.cntAnchors));
                r.add(toggle("Hole HUD", "Esta num hole?", () -> s.holeHud, () -> s.holeHud = !s.holeHud));
                r.add(toggle("Dano explosao", "Estimativa do crystal", () -> s.explosionDmg, () -> s.explosionDmg = !s.explosionDmg));
                r.add(toggle("Target HUD", "Vida do alvo", () -> s.targetHud, () -> s.targetHud = !s.targetHud));
                r.add(toggle("Combo", "Acertos seguidos", () -> s.comboHud, () -> s.comboHud = !s.comboHud));
                r.add(toggle("Reach", "Distancia do ultimo hit", () -> s.reachHud, () -> s.reachHud = !s.reachHud));
                r.add(toggle("Criticals", "Pronto pra critico", () -> s.critHud, () -> s.critHud = !s.critHud));
                r.add(toggle("Aviso totem", "Sem totem na mao", () -> s.totemWarning, () -> s.totemWarning = !s.totemWarning));
                r.add(toggle("Aviso armadura", "Durabilidade baixa", () -> s.durabilityWarning,
                        () -> s.durabilityWarning = !s.durabilityWarning));
            }
            default -> {
                r.add(toggle("Base Finder", "Acha bases (pesado)", () -> s.baseFinder, () -> s.baseFinder = !s.baseFinder));
                r.add(toggle("Chunk Finder", "Chunks com atividade", () -> s.chunkFinder, () -> s.chunkFinder = !s.chunkFinder));
                r.add(toggle("Amethyst", "Procura ametista", () -> s.amethystScanner, () -> s.amethystScanner = !s.amethystScanner));
                r.add(cycle("Modo Finder", "Tipo de busca", () -> s.finderMode.name(), s::cycleFinderMode));
                r.add(cycle("Modo Amethyst", "Tipo de busca", () -> s.amethystMode.name(), s::cycleAmethystMode));
                r.add(cycle("Alcance", "Chunks escaneados", () -> s.rangeChunks + " ch",
                        () -> s.rangeChunks = next(s.rangeChunks, new int[]{2, 3, 4, 6, 8})));
            }
        }
        return r;
    }

    public ZyonScreen() { super(Text.literal("Zyon Client")); }

    private int cardX(int i, int cw) { return left + SIDE + 8 + (i / PER_COL) * (cw + 6); }
    private int cardY(int i) { return top + HEADER + 8 + (i % PER_COL) * ROW_H; }
    private int cardW() { return (W - SIDE - 16 - 6) / 2; }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        // botoes invisiveis (so recebem o toque); o visual e desenhado em render()
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> { tab = idx; clearAndInit(); })
                    .dimensions(left + 6, top + HEADER + 8 + i * TAB_STEP, SIDE - 12, TAB_H).build());
        }
        List<Row> rows = rows();
        int cw = cardW();
        for (int i = 0; i < rows.size(); i++) {
            final Row row = rows.get(i);
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> row.click.run())
                    .dimensions(cardX(i, cw), cardY(i), cw, ROW_H - 3).build());
        }
    }

    private void border(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1, y1, x2, y1 + 1, color);
        ctx.fill(x1, y2 - 1, x2, y2, color);
        ctx.fill(x1, y1, x1 + 1, y2, color);
        ctx.fill(x2 - 1, y1, x2, y2, color);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int x0 = left, y0 = top;
        // sombra + painel
        ctx.fill(x0 + 3, y0 + 3, x0 + W + 3, y0 + H + 3, 0x66000000);
        ctx.fill(x0, y0, x0 + W, y0 + H, BG);
        // cabecalho com degrade
        for (int i = 0; i < HEADER; i++) {
            int a = 0xFF;
            int rr = 0x1A + (0x0E - 0x1A) * i / HEADER;
            int gg = 0x14 + (0x10 - 0x14) * i / HEADER;
            int bb = 0x3A + (0x1A - 0x3A) * i / HEADER;
            ctx.fill(x0, y0 + i, x0 + W, y0 + i + 1, (a << 24) | (rr << 16) | (gg << 8) | bb);
        }
        ctx.fill(x0, y0 + HEADER, x0 + W, y0 + HEADER + 1, ACCENT);
        ctx.fill(x0 + 12, y0 + 11, x0 + 20, y0 + 19, ACCENT);
        ctx.fill(x0 + 14, y0 + 13, x0 + 18, y0 + 17, 0xFFFFFFFF);
        ctx.drawText(textRenderer, "ZYON", x0 + 26, y0 + 12, ACCENT_LIGHT, false);
        ctx.drawText(textRenderer, "CLIENT", x0 + 58, y0 + 12, TEXT, false);
        String ver = "1.21.11";
        ctx.drawText(textRenderer, ver, x0 + W - 14 - textRenderer.getWidth(ver), y0 + 12, DIM, false);
        border(ctx, x0, y0, x0 + W, y0 + H, 0xFF232A44);

        // abas
        ctx.fill(x0 + 1, y0 + HEADER + 1, x0 + SIDE, y0 + H - 1, SIDEBG);
        for (int i = 0; i < TABS.length; i++) {
            int tx = x0 + 6, ty = y0 + HEADER + 8 + i * TAB_STEP, tw = SIDE - 12;
            boolean sel = i == tab;
            boolean hov = mouseX >= tx && mouseX < tx + tw && mouseY >= ty && mouseY < ty + TAB_H;
            ctx.fill(tx, ty, tx + tw, ty + TAB_H, sel ? 0xFF241C4A : (hov ? 0xFF1A2036 : SIDEBG));
            if (sel) ctx.fill(tx, ty, tx + 2, ty + TAB_H, ACCENT);
            ctx.fill(tx + 8, ty + 6, tx + 20, ty + 18, ICON_COLORS[i]);
            ctx.drawText(textRenderer, ICONS[i], tx + 12, ty + 8, 0xFF0B0E17, false);
            ctx.drawText(textRenderer, TABS[i], tx + 26, ty + 8, sel ? TEXT : DIM, false);
        }

        // cartoes
        List<Row> rows = rows();
        int cw = cardW();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int cx = cardX(i, cw), cy = cardY(i), ch = ROW_H - 3;
            boolean hov = mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch;
            boolean on = row.on != null && row.on.getAsBoolean();
            ctx.fill(cx, cy, cx + cw, cy + ch, hov ? CARD_HOVER : CARD);
            ctx.fill(cx, cy, cx + 2, cy + ch, row.on == null ? ACCENT : (on ? GREEN : OFF));

            int rightW;
            if (row.on != null) {
                int px = cx + cw - 8 - 28, py = cy + (ch - 14) / 2;
                ctx.fill(px, py, px + 28, py + 14, on ? GREEN : OFF);
                int kx = on ? px + 16 : px + 2;
                ctx.fill(kx, py + 2, kx + 10, py + 12, 0xFFFFFFFF);
                rightW = 40;
            } else {
                String v = row.value.get();
                int vw = textRenderer.getWidth(v) + 12;
                int vx = cx + cw - 6 - vw, vy = cy + (ch - 14) / 2;
                ctx.fill(vx, vy, vx + vw, vy + 14, 0xFF2A3045);
                ctx.fill(vx, vy, vx + vw, vy + 1, ACCENT);
                ctx.drawText(textRenderer, v, vx + 6, vy + 3, ACCENT_LIGHT, false);
                rightW = vw + 10;
            }
            int maxText = cw - 10 - rightW;
            ctx.drawText(textRenderer, textRenderer.trimToWidth(row.name, maxText), cx + 8, cy + 4,
                    TEXT, false);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(row.desc, maxText), cx + 8, cy + 14,
                    DIM, false);
        }
        ctx.drawText(textRenderer, "ESC - fechar    toque nos cartoes para ligar", x0 + SIDE + 8, y0 + H - 12, DIM, false);
    }

    @Override
    public boolean shouldPause() { return false; }
}
