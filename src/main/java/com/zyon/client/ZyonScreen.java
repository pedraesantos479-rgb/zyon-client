package com.zyon.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class ZyonScreen extends Screen {
    private static final int HEADER = 34, SIDE = 88, ROW_H = 28, FOOT = 18;
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
    private static final String[] TABS = {"Combate", "Crystal", "Jogador", "Mundo", "Visual", "HUD", "CPvP HUD", "Scanner"};
    private static final String[] ICONS = {"C", "X", "J", "M", "V", "H", "P", "S"};
    private static final int[] ICON_COLORS = {
            0xFFEF4444, 0xFFF97316, 0xFF34D399, 0xFF22D3EE, 0xFF60A5FA, 0xFFFBBF24, 0xFFF472B6, 0xFFA78BFA};

    private static int tab = 0;
    private static int page = 0;

    private int left, top, W, H;
    private int perCol, perPage, tabStep, tabH;
    private Row activeSlider;
    private int activeCx, activeCw;

    private static final class Row {
        final String name, desc;
        final BooleanSupplier on;            // interruptor
        final Supplier<String> value;        // ciclo
        final Runnable click;
        final IntSupplier geti;              // barra
        final IntConsumer seti;
        final IntFunction<String> fmt;
        final int min, max, step;

        Row(String name, String desc, BooleanSupplier on, Supplier<String> value, Runnable click,
            IntSupplier geti, IntConsumer seti, IntFunction<String> fmt, int min, int max, int step) {
            this.name = name;
            this.desc = desc;
            this.on = on;
            this.value = value;
            this.click = click;
            this.geti = geti;
            this.seti = seti;
            this.fmt = fmt;
            this.min = min;
            this.max = max;
            this.step = step;
        }
    }

    private static Row toggle(String name, String desc, BooleanSupplier get, Runnable flip) {
        return new Row(name, desc, get, null, flip, null, null, null, 0, 0, 1);
    }

    private static Row cycle(String name, String desc, Supplier<String> val, Runnable next) {
        return new Row(name, desc, null, val, next, null, null, null, 0, 0, 1);
    }

    private static Row slider(String name, int min, int max, int step, IntSupplier get, IntConsumer set,
                              IntFunction<String> fmt) {
        return new Row(name, "", null, null, null, get, set, fmt, min, max, step);
    }

    private List<Row> rows() {
        DiscoverySettings s = ZyonClient.SETTINGS;
        List<Row> r = new ArrayList<>();
        switch (tab) {
            case 0 -> {
                r.add(toggle("KillAura", "Ataca o alvo mais perto", () -> s.killAura, () -> s.killAura = !s.killAura));
                r.add(slider("Alcance KillAura", 3, 6, 1, () -> s.killAuraRange, v -> s.killAuraRange = v, v -> v + " bl"));
                r.add(toggle("So jogadores", "Ignora mobs", () -> s.killAuraPlayersOnly,
                        () -> s.killAuraPlayersOnly = !s.killAuraPlayersOnly));
                r.add(toggle("TriggerBot", "Ataca ao mirar", () -> s.triggerBot, () -> s.triggerBot = !s.triggerBot));
                r.add(toggle("AutoClicker", "Clica segurando o botao", () -> s.autoClicker, () -> s.autoClicker = !s.autoClicker));
                r.add(slider("CPS Clicker", 4, 20, 2, () -> s.autoClickerCps, v -> s.autoClickerCps = v, v -> v + " cps"));
                r.add(toggle("Aim Assist", "Ajuda a mirar", () -> s.aimAssist, () -> s.aimAssist = !s.aimAssist));
                r.add(slider("Forca Aim", 5, 50, 5, () -> s.aimStrength, v -> s.aimStrength = v, v -> v + "%"));
                r.add(toggle("Hit Sounds", "Som ao acertar", () -> s.hitSounds, () -> s.hitSounds = !s.hitSounds));
            }
            case 1 -> {
                r.add(toggle("CPvP Tudo-em-1", "Obsidian + Crystal", () -> s.cpvpAll, () -> s.cpvpAll = !s.cpvpAll));
                r.add(toggle("Auto Crystal", "Poe e quebra crystals", () -> s.autoCrystal, () -> s.autoCrystal = !s.autoCrystal));
                r.add(toggle("Auto Obsidian", "Poe a base do crystal", () -> s.autoObsidian, () -> s.autoObsidian = !s.autoObsidian));
                r.add(toggle("So jogadores", "Desligue p/ testar em mobs", () -> s.cpvpPlayersOnly,
                        () -> s.cpvpPlayersOnly = !s.cpvpPlayersOnly));
                r.add(slider("Velocidade", 1, 10, 1, () -> s.cpvpSpeed, v -> s.cpvpSpeed = v, v -> v + "/10"));
                r.add(slider("Alcance", 3, 6, 1, () -> s.crystalRange, v -> s.crystalRange = v, v -> v + " bl"));
                r.add(slider("Dano minimo", 1, 12, 1, () -> s.minTargetDamage, v -> s.minTargetDamage = v, v -> v + " hp"));
                r.add(slider("Dano max (voce)", 0, 40, 2, () -> s.maxSelfDamage, v -> s.maxSelfDamage = v,
                        v -> v == 0 ? "sem limite" : v + " hp"));
                r.add(slider("Protecao armadura", 0, 80, 10, () -> s.selfProtection, v -> s.selfProtection = v, v -> v + "%"));
                r.add(toggle("Anti suicida", "Nao age se te mataria", () -> s.antiSuicide, () -> s.antiSuicide = !s.antiSuicide));
                r.add(toggle("Auto Totem", "Totem na mao", () -> s.autoTotem, () -> s.autoTotem = !s.autoTotem));
                r.add(slider("Totem quando", 0, 20, 2, () -> s.totemHealth, v -> s.totemHealth = v,
                        v -> v == 0 ? "sempre" : "<= " + v));
            }
            case 2 -> {
                r.add(toggle("Auto Sprint", "Corre sozinho", () -> s.autoSprint, () -> s.autoSprint = !s.autoSprint));
                r.add(toggle("Toggle Sprint", "Sprint travado", () -> s.toggleSprint, () -> s.toggleSprint = !s.toggleSprint));
                r.add(toggle("Toggle Sneak", "Agachar travado", () -> s.toggleSneak, () -> s.toggleSneak = !s.toggleSneak));
                r.add(toggle("Auto Eat", "Come com fome", () -> s.autoEat, () -> s.autoEat = !s.autoEat));
                r.add(toggle("Auto Refill", "Repoe a hotbar", () -> s.autoRefill, () -> s.autoRefill = !s.autoRefill));
                r.add(toggle("Auto Respawn", "Renasce sozinho", () -> s.autoRespawn, () -> s.autoRespawn = !s.autoRespawn));
                r.add(toggle("Fullbright", "Ve no escuro", () -> s.fullbright, () -> s.fullbright = !s.fullbright));
                r.add(toggle("Zoom", "Segure a tecla C", () -> s.zoom, () -> s.zoom = !s.zoom));
            }
            case 3 -> {
                r.add(toggle("Nuker", "Quebra blocos perto", () -> s.nuker, () -> s.nuker = !s.nuker));
                r.add(slider("Alcance Nuker", 1, 16, 1, () -> s.nukerRange, v -> s.nukerRange = v, v -> v + " bl"));
                r.add(toggle("Nuker so minerios", "Ignora pedra e terra", () -> s.nukerOres, () -> s.nukerOres = !s.nukerOres));
                r.add(toggle("Auto ferramenta", "Troca p/ a melhor", () -> s.nukerAutoTool, () -> s.nukerAutoTool = !s.nukerAutoTool));
                r.add(slider("Blocos por tick", 1, 300, 1, () -> s.nukerPerTick, v -> s.nukerPerTick = v, v -> v + "/tick"));
                r.add(toggle("Freecam", "Camera livre", () -> s.freecam, () -> s.freecam = !s.freecam));
                r.add(slider("Velocidade freecam", 1, 10, 1, () -> s.freecamSpeed, v -> s.freecamSpeed = v, v -> v + "/10"));
                r.add(toggle("X-Ray", "Marca minerios na parede", () -> s.xray, () -> s.xray = !s.xray));
                r.add(toggle("X-Ray so raros", "Diamante, debris, ouro", () -> s.xrayRare, () -> s.xrayRare = !s.xrayRare));
                r.add(slider("Alcance X-Ray", 8, 24, 4, () -> s.xrayRange, v -> s.xrayRange = v, v -> v + " bl"));
                r.add(toggle("Auto Bridge", "Bloco embaixo na ponte", () -> s.autoBridge, () -> s.autoBridge = !s.autoBridge));
                r.add(slider("Velocidade ponte", 1, 10, 1, () -> s.bridgeSpeed, v -> s.bridgeSpeed = v, v -> v + "/10"));
            }
            case 4 -> {
                r.add(toggle("ESP Jogadores", "Caixa nos jogadores", () -> s.playerEsp, () -> s.playerEsp = !s.playerEsp));
                r.add(toggle("ESP Entidades", "Mobs e animais", () -> s.entityEsp, () -> s.entityEsp = !s.entityEsp));
                r.add(toggle("ESP Itens", "Itens no chao", () -> s.itemEsp, () -> s.itemEsp = !s.itemEsp));
                r.add(toggle("ESP Baus", "Baus e ender chest", () -> s.chestEsp, () -> s.chestEsp = !s.chestEsp));
                r.add(toggle("ESP Storage", "Barril, shulker, funil", () -> s.storageEsp, () -> s.storageEsp = !s.storageEsp));
                r.add(toggle("Tracers", "Linhas ate os alvos", () -> s.tracers, () -> s.tracers = !s.tracers));
                r.add(toggle("Name Tags", "Nome, vida e distancia", () -> s.nameTags, () -> s.nameTags = !s.nameTags));
                r.add(toggle("Hitbox", "Caixa 3D do alvo", () -> s.hitboxViz, () -> s.hitboxViz = !s.hitboxViz));
                r.add(slider("Alcance ESP", 32, 128, 16, () -> s.espRange, v -> s.espRange = v, v -> v + " bl"));
                r.add(toggle("HUD modulos", "Lista de ativos", () -> s.hudModules, () -> s.hudModules = !s.hudModules));
                r.add(toggle("Mira custom", "Mira colorida", () -> s.customCrosshair, () -> s.customCrosshair = !s.customCrosshair));
            }
            case 5 -> {
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
                r.add(toggle("Armadura", "Pecas e durabilidade", () -> s.hudArmor, () -> s.hudArmor = !s.hudArmor));
                r.add(toggle("Efeitos", "Efeitos de pocao", () -> s.hudEffects, () -> s.hudEffects = !s.hudEffects));
                r.add(toggle("Inventario", "Itens do inventario", () -> s.hudInventory, () -> s.hudInventory = !s.hudInventory));
            }
            case 6 -> {
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
                r.add(toggle("HUD achados", "Resultados na tela", () -> s.hudFindings, () -> s.hudFindings = !s.hudFindings));
                r.add(slider("Alcance scanner", 2, 8, 1, () -> s.rangeChunks, v -> s.rangeChunks = v, v -> v + " ch"));
            }
        }
        return r;
    }

    public ZyonScreen() { super(Text.literal("Zyon Client")); }

    private int cardW() { return (W - SIDE - 16 - 6) / 2; }
    private int cardX(int j) { return left + SIDE + 8 + (j / perCol) * (cardW() + 6); }
    private int cardY(int j) { return top + HEADER + 8 + (j % perCol) * ROW_H; }

    private int pages(int n) { return Math.max(1, (n + perPage - 1) / perPage); }

    private void sliderSet(Row row, int cx, int cw) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
        double frac = (mx - (cx + 8)) / (double) (cw - 16);
        frac = Math.max(0.0, Math.min(1.0, frac));
        int steps = Math.max(1, (row.max - row.min) / row.step);
        int idx = (int) Math.round(frac * steps);
        row.seti.accept(row.min + idx * row.step);
    }

    @Override
    protected void init() {
        W = Math.min(430, width - 12);
        H = Math.min(250, height - 12);
        left = (width - W) / 2;
        top = (height - H) / 2;
        perCol = Math.max(3, (H - HEADER - 8 - FOOT) / ROW_H);
        perPage = perCol * 2;
        tabStep = Math.max(18, Math.min(26, (H - HEADER - 12) / TABS.length));
        tabH = tabStep - 3;

        // botoes invisiveis (so recebem o toque); o visual e desenhado em render()
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> { tab = idx; page = 0; clearAndInit(); })
                    .dimensions(left + 6, top + HEADER + 8 + i * tabStep, SIDE - 12, tabH).build());
        }
        List<Row> rows = rows();
        int pgs = pages(rows.size());
        if (page >= pgs) page = pgs - 1;
        int cw = cardW();
        int start = page * perPage;
        for (int i = start; i < Math.min(rows.size(), start + perPage); i++) {
            final Row row = rows.get(i);
            final int j = i - start;
            final int cx = cardX(j);
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
                        if (row.geti != null) {
                            activeSlider = row;
                            activeCx = cx;
                            activeCw = cw;
                            sliderSet(row, cx, cw);
                        } else {
                            row.click.run();
                        }
                    })
                    .dimensions(cx, cardY(j), cw, ROW_H - 3).build());
        }
        if (pgs > 1) {
            int fy = top + H - FOOT;
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> { page = Math.max(0, page - 1); clearAndInit(); })
                    .dimensions(left + W - 84, fy, 24, 14).build());
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> { page = Math.min(pgs - 1, page + 1); clearAndInit(); })
                    .dimensions(left + W - 30, fy, 24, 14).build());
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
        // arrastar a barra enquanto o dedo/botao esta pressionado
        if (activeSlider != null) {
            if (MinecraftClient.getInstance().mouse.wasLeftButtonClicked()) {
                sliderSet(activeSlider, activeCx, activeCw);
            } else {
                activeSlider = null;
            }
        }

        super.render(ctx, mouseX, mouseY, delta);

        int x0 = left, y0 = top;
        ctx.fill(x0 + 3, y0 + 3, x0 + W + 3, y0 + H + 3, 0x66000000);
        ctx.fill(x0, y0, x0 + W, y0 + H, BG);
        for (int i = 0; i < HEADER; i++) {
            int rr = 0x1A + (0x0E - 0x1A) * i / HEADER;
            int gg = 0x14 + (0x10 - 0x14) * i / HEADER;
            int bb = 0x3A + (0x1A - 0x3A) * i / HEADER;
            ctx.fill(x0, y0 + i, x0 + W, y0 + i + 1, (0xFF << 24) | (rr << 16) | (gg << 8) | bb);
        }
        ctx.fill(x0, y0 + HEADER, x0 + W, y0 + HEADER + 1, ACCENT);
        ctx.fill(x0 + 12, y0 + 11, x0 + 20, y0 + 19, ACCENT);
        ctx.fill(x0 + 14, y0 + 13, x0 + 18, y0 + 17, 0xFFFFFFFF);
        ctx.drawText(textRenderer, "ZYON", x0 + 26, y0 + 12, ACCENT_LIGHT, false);
        ctx.drawText(textRenderer, "CLIENT", x0 + 58, y0 + 12, TEXT, false);
        String ver = "1.21.11";
        ctx.drawText(textRenderer, ver, x0 + W - 14 - textRenderer.getWidth(ver), y0 + 12, DIM, false);
        border(ctx, x0, y0, x0 + W, y0 + H, 0xFF232A44);

        ctx.fill(x0 + 1, y0 + HEADER + 1, x0 + SIDE, y0 + H - 1, SIDEBG);
        for (int i = 0; i < TABS.length; i++) {
            int tx = x0 + 6, ty = y0 + HEADER + 8 + i * tabStep, tw = SIDE - 12;
            boolean sel = i == tab;
            boolean hov = mouseX >= tx && mouseX < tx + tw && mouseY >= ty && mouseY < ty + tabH;
            ctx.fill(tx, ty, tx + tw, ty + tabH, sel ? 0xFF241C4A : (hov ? 0xFF1A2036 : SIDEBG));
            if (sel) ctx.fill(tx, ty, tx + 2, ty + tabH, ACCENT);
            int iy = ty + (tabH - 12) / 2;
            ctx.fill(tx + 8, iy, tx + 20, iy + 12, ICON_COLORS[i]);
            ctx.drawText(textRenderer, ICONS[i], tx + 12, iy + 2, 0xFF0B0E17, false);
            ctx.drawText(textRenderer, TABS[i], tx + 26, iy + 2, sel ? TEXT : DIM, false);
        }

        List<Row> rows = rows();
        int pgs = pages(rows.size());
        if (page >= pgs) page = pgs - 1;
        int cw = cardW();
        int start = page * perPage;
        for (int i = start; i < Math.min(rows.size(), start + perPage); i++) {
            Row row = rows.get(i);
            int j = i - start;
            int cx = cardX(j), cy = cardY(j), ch = ROW_H - 3;
            boolean hov = mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch;
            ctx.fill(cx, cy, cx + cw, cy + ch, hov ? CARD_HOVER : CARD);

            if (row.geti != null) {
                int v = row.geti.getAsInt();
                ctx.fill(cx, cy, cx + 2, cy + ch, ACCENT);
                String vs = row.fmt.apply(v);
                int vw = textRenderer.getWidth(vs);
                ctx.drawText(textRenderer, textRenderer.trimToWidth(row.name, cw - vw - 22), cx + 8, cy + 3, TEXT, false);
                ctx.drawText(textRenderer, vs, cx + cw - 8 - vw, cy + 3, ACCENT_LIGHT, false);
                int tx1 = cx + 8, tx2 = cx + cw - 8, ty = cy + 16;
                ctx.fill(tx1, ty, tx2, ty + 4, 0xFF2A3045);
                double frac = (v - row.min) / (double) Math.max(1, row.max - row.min);
                int kx = tx1 + (int) Math.round(frac * (tx2 - tx1 - 4));
                ctx.fill(tx1, ty, kx + 2, ty + 4, ACCENT);
                ctx.fill(kx, ty - 2, kx + 4, ty + 6, 0xFFFFFFFF);
                continue;
            }

            boolean on = row.on != null && row.on.getAsBoolean();
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
            ctx.drawText(textRenderer, textRenderer.trimToWidth(row.name, maxText), cx + 8, cy + 4, TEXT, false);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(row.desc, maxText), cx + 8, cy + 14, DIM, false);
        }

        int fy = y0 + H - FOOT;
        ctx.drawText(textRenderer, "ESC fecha  |  arraste as barras", x0 + SIDE + 8, fy + 3, DIM, false);
        if (pgs > 1) {
            boolean h1 = mouseX >= x0 + W - 84 && mouseX < x0 + W - 60 && mouseY >= fy && mouseY < fy + 14;
            boolean h2 = mouseX >= x0 + W - 30 && mouseX < x0 + W - 6 && mouseY >= fy && mouseY < fy + 14;
            ctx.fill(x0 + W - 84, fy, x0 + W - 60, fy + 14, h1 ? CARD_HOVER : CARD);
            ctx.fill(x0 + W - 30, fy, x0 + W - 6, fy + 14, h2 ? CARD_HOVER : CARD);
            ctx.drawText(textRenderer, "<", x0 + W - 74, fy + 3, ACCENT_LIGHT, false);
            ctx.drawText(textRenderer, ">", x0 + W - 20, fy + 3, ACCENT_LIGHT, false);
            String pg = (page + 1) + "/" + pgs;
            ctx.drawText(textRenderer, pg, x0 + W - 57 + (27 - textRenderer.getWidth(pg)) / 2 - 3, fy + 3, TEXT, false);
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
