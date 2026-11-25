package com.zyon.client;

public final class DiscoverySettings {
    public enum FinderMode { CLASSIC, ANBS_PLUS, ACTIVITY }
    public enum AmethystMode { BYPASS, LIGHT, ANBS_PLUS }

    // Scanner (comecam desligados para nao pesar no celular)
    public boolean baseFinder = false;
    public boolean chunkFinder = false;
    public boolean amethystScanner = false;
    public int rangeChunks = 4;
    public FinderMode finderMode = FinderMode.CLASSIC;
    public AmethystMode amethystMode = AmethystMode.ANBS_PLUS;

    // Jogador
    public boolean autoSprint = false;
    public boolean fullbright = false;

    // Visual
    public boolean hudModules = true;
    public boolean hudFindings = true;
    public boolean playerEsp = false;

    // Combate
    public boolean killAura = false;
    public int killAuraRange = 4;
    public boolean killAuraPlayersOnly = true;
    public boolean autoCrystal = false;
    public int crystalRange = 5;
    public int crystalDelay = 2;
    public boolean autoObsidian = false;

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
