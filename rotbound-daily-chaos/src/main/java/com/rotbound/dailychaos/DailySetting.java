package com.rotbound.dailychaos;

import java.util.Arrays;

/** The complete list of settings that Rotbound Daily Chaos may advance. */
public enum DailySetting {
    KEEP_INVENTORY("keep_inventory", "Keep Inventory", "DISABLED", "ENABLED"),
    PVP("pvp", "PvP", "DISABLED", "ENABLED"),
    ENCHANTMENTS("enchantments", "Enchantments", "DISABLED", "ENABLED"),
    MOB_SPAWNING("mob_spawning", "Mob Spawning", "DISABLED", "ENABLED"),
    REGENERATION("regeneration", "Regeneration", "DISABLED", "ENABLED"),
    DAY_NIGHT_CYCLE("day_night_cycle", "Day/Night Cycle", "DISABLED", "ENABLED"),
    DIFFICULTY("difficulty", "Difficulty", "PEACEFUL", "EASY", "NORMAL", "HARD"),
    WEATHER("weather", "Weather", "CLEAR", "RAIN", "THUNDER"),
    COMMAND_BLOCKS("command_blocks", "Command Blocks", "DISABLED", "ENABLED"),
    FIRE_TICK_DELAY("fire_tick_delay", "Fire Tick Delay", "NORMAL", "FAST", "INSTANT"),
    PLAYER_INTERACTION("player_interaction", "Player Interaction", "DISABLED", "ENABLED"),
    EXPLOSION_BLOCK_BREAKING("explosion_block_breaking", "Explosion Block Breaking", "DISABLED", "ENABLED");

    private final String configKey;
    private final String displayName;
    private final String[] cycle;

    DailySetting(String configKey, String displayName, String... cycle) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.cycle = cycle;
    }

    public String configKey() {
        return configKey;
    }

    public String displayName() {
        return displayName;
    }

    public String firstValue() {
        return cycle[0];
    }

    public boolean accepts(String value) {
        return Arrays.asList(cycle).contains(value);
    }

    public String next(String current) {
        int index = Arrays.asList(cycle).indexOf(current);
        if (index < 0) {
            return firstValue();
        }
        return cycle[(index + 1) % cycle.length];
    }
}
