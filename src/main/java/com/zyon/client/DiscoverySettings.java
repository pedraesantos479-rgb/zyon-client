package com.zyon.client;

public final class DiscoverySettings {
    public enum FinderMode { CLASSIC, ANBS_PLUS, ACTIVITY }
    public enum AmethystMode { BYPASS, LIGHT, ANBS_PLUS }

    // Scanner (comecam desligados para nao pesar)
    public boolean baseFinder = false;
    public boolean chunkFinder = false;
    public boolean amethystScanner = false;
    public int rangeChunks = 4;
    public FinderMode finderMode = FinderMode.CLASSIC;
    public AmethystMode amethystMode = AmethystMode.ANBS_PLUS;

    // Combate
    public boolean killAura = false;
    public int killAuraRange = 4;
    public boolean killAuraPlayersOnly = true;
    public boolean autoCrystal = false;
    public int crystalRange = 5;
    public int crystalDelay = 2;
    public boolean autoObsidian = false;
    public boolean autoTotem = false;
    public int totemHealth = 0;          // 0 = sempre
    public boolean cpvpAll = false;      // crystal + obsidian + totem juntos
    public int minTargetDamage = 4;
    public int maxSelfDamage = 0;        // 0 = sem limite
    public int selfProtection = 60;      // % de reducao por encantamentos (so na estimativa)
    public boolean cpvpPlayersOnly = true;
    public int cpvpSpeed = 6;            // 1..10 (barra de velocidade)

    // Combate 2
    public boolean triggerBot = false;
    public boolean autoClicker = false;
    public int autoClickerCps = 10;
    public boolean aimAssist = false;
    public int aimStrength = 20;
    public boolean hitSounds = false;

    // Jogador
    public boolean autoSprint = false;
    public boolean toggleSprint = false;
    public boolean toggleSneak = false;
    public boolean autoEat = false;
    public boolean autoRefill = false;
    public boolean autoRespawn = false;
    public boolean autoBridge = false;
    public boolean nuker3x3x3 = false;
    public int bridgeSpeed = 8;          // 1..10
    public boolean fullbright = false;

    // HUD
    public boolean hudFps = false;
    public boolean hudPing = false;
    public boolean hudCoords = false;
    public boolean hudDirection = false;
    public boolean hudClock = false;
    public boolean hudMemory = false;
    public boolean hudBiome = false;

    // HUD 2
    public boolean hudSpeed = false;
    public boolean hudCps = false;
    public boolean hudKeystrokes = false;
    public boolean hudArmor = false;
    public boolean hudEffects = false;
    public boolean hudInventory = false;
    public boolean customCrosshair = false;

    // CPvP
    public boolean cntCrystals = false;
    public boolean cntTotems = false;
    public boolean cntObsidian = false;
    public boolean cntPearls = false;
    public boolean cntAnchors = false;
    public boolean holeHud = false;
    public boolean explosionDmg = false;

    // Alvo
    public boolean targetHud = false;
    public boolean comboHud = false;
    public boolean reachHud = false;
    public boolean critHud = false;
    public boolean totemWarning = false;
    public boolean durabilityWarning = false;

    // Visual
    public boolean hudModules = true;
    public boolean hudFindings = true;
    public boolean playerEsp = false;
    public boolean entityEsp = false;
    public boolean itemEsp = false;
    public boolean chestEsp = false;
    public boolean storageEsp = false;
    public boolean tracers = false;
    public boolean nameTags = false;
    public boolean hitboxViz = false;
    public int espRange = 64;
    public boolean zoom = false;
    public boolean xray = false;
    public boolean xrayRare = true;
    public int xrayRange = 16;

    public boolean anyScannerEnabled() {
        return baseFinder || chunkFinder || amethystScanner;
    }

    public void cycleFinderMode() {
        finderMode = FinderMode.values()[(finderMode.ordinal() + 1) % FinderMode.values().length];
    }

    public void cycleAmethystMode() {
        amethystMode = AmethystMode.values()[(amethystMode.ordinal() + 1) % AmethystMode.values().length];
    }
}
