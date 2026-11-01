package com.rotbound.dailychaos;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/** Owns the live setting states and their NightConfig TOML persistence. */
public final class DailyState {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();

    private final Map<DailySetting, String> values = new EnumMap<>(DailySetting.class);
    private CommentedFileConfig config;
    private LocalDate lastDateToggled;
    private boolean loaded;
    private int tickDivider;

    public void start(MinecraftServer server) {
        closeConfig();
        Path configDirectory = FMLPaths.CONFIGDIR.get();
        Path configPath = configDirectory.resolve("rotbound_daily_chaos.toml");

        try {
            Files.createDirectories(configDirectory);
            config = CommentedFileConfig.builder(configPath).sync().autosave().build();
            config.load();

            values.clear();
            boolean createdDefaults = false;
            for (DailySetting setting : DailySetting.values()) {
                Object stored = config.get(setting.configKey());
                String value = stored instanceof String text && setting.accepts(text)
                        ? text
                        : setting.firstValue();
                if (!(stored instanceof String text) || !setting.accepts(text)) {
                    createdDefaults = true;
                }
                values.put(setting, value);
                config.set(setting.configKey(), value);
            }

            Object storedDate = config.get("last_date_toggled_utc");
            if (storedDate instanceof String dateText) {
                try {
                    lastDateToggled = LocalDate.parse(dateText);
                } catch (RuntimeException invalidDate) {
                    lastDateToggled = LocalDate.now(ZoneOffset.UTC);
                    createdDefaults = true;
                }
            } else {
                lastDateToggled = LocalDate.now(ZoneOffset.UTC);
                createdDefaults = true;
            }

            config.set("last_date_toggled_utc", lastDateToggled.toString());
            config.setComment("last_date_toggled_utc", "Last UTC date on which Rotbound changed a setting.");
            config.setComment("keep_inventory", "DISABLED or ENABLED. Each setting value is persisted here.");
            config.save();
            loaded = true;

            // Reapply the saved state after a restart. No daily roll is performed here;
            // the UTC date check in the tick handler decides whether today's roll is due.
            applyAll(server);
            LOGGER.info("Rotbound Daily Chaos loaded. Last UTC change date: {}{}",
                    lastDateToggled, createdDefaults ? " (defaults initialised where needed)" : "");
        } catch (IOException | RuntimeException exception) {
            loaded = false;
            LOGGER.error("Could not load config/rotbound_daily_chaos.toml. Daily Chaos is disabled to avoid changing settings without persistence.", exception);
            closeConfig();
        }
    }

    public void tick(MinecraftServer server) {
        if (!loaded || ++tickDivider < 20) {
            return;
        }
        tickDivider = 0;

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        if (!todayUtc.isAfter(lastDateToggled)) {
            return;
        }

        DailySetting chosen = DailySetting.values()[RANDOM.nextInt(DailySetting.values().length)];
        String previous = values.get(chosen);
        LocalDate previousDate = lastDateToggled;
        String next = chosen.next(previous);
        values.put(chosen, next);

        try {
            apply(chosen, next, server);
            lastDateToggled = todayUtc;
            save();
            announce(server, chosen, next);
            LOGGER.info("Rotbound Daily Chaos advanced {} from {} to {} for UTC date {}.",
                    chosen.configKey(), previous, next, todayUtc);
        } catch (RuntimeException exception) {
            // Do not advance the saved date if application or persistence fails.
            values.put(chosen, previous);
            lastDateToggled = previousDate;
            LOGGER.error("Failed to apply Rotbound daily setting {}. It will be retried while the server remains online.", chosen, exception);
        }
    }

    public boolean isEnabled(DailySetting setting) {
        return "ENABLED".equals(values.get(setting));
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void stop() {
        if (loaded) {
            save();
        }
        loaded = false;
        closeConfig();
    }

    private void applyAll(MinecraftServer server) {
        for (DailySetting setting : DailySetting.values()) {
            apply(setting, values.get(setting), server);
        }
    }

    private void apply(DailySetting setting, String value, MinecraftServer server) {
        switch (setting) {
            case KEEP_INVENTORY -> gameRule(server, "keep_inventory", value);
            case PVP -> gameRule(server, "pvp", value);
            case ENCHANTMENTS, PLAYER_INTERACTION, EXPLOSION_BLOCK_BREAKING -> {
                // These are enforced by NeoForge events in RotboundDailyChaos.
            }
            case MOB_SPAWNING -> gameRule(server, "spawn_mobs", value);
            case REGENERATION -> gameRule(server, "natural_health_regeneration", value);
            case DAY_NIGHT_CYCLE -> gameRule(server, "advance_time", value);
            case DIFFICULTY -> runCommand(server, "difficulty " + value.toLowerCase());
            case WEATHER -> runCommand(server, "weather " + value.toLowerCase() + " 1000000");
            case COMMAND_BLOCKS -> gameRule(server, "command_blocks_work", value);
            case FIRE_TICK_DELAY -> {
                // Vanilla has no separate fire-delay rule. This supported gamerule
                // approximates faster fire updates, but affects all random-ticked blocks.
                int randomTickSpeed = switch (value) {
                    case "FAST" -> 12;
                    case "INSTANT" -> 100;
                    default -> 3;
                };
                runCommand(server, "gamerule random_tick_speed " + randomTickSpeed);
            }
        }
    }

    private void gameRule(MinecraftServer server, String rule, String value) {
        boolean enabled = "ENABLED".equals(value);
        runCommand(server, "gamerule " + rule + " " + enabled);
    }

    private void runCommand(MinecraftServer server, String command) {
        CommandSourceStack source = server.createCommandSourceStack();
        server.getCommands().performPrefixedCommand(source, command);
    }

    private void announce(MinecraftServer server, DailySetting setting, String value) {
        Component message = Component.literal("📢 New Day! ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(setting.displayName()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal(" is now ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(value).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        server.getPlayerList().broadcastSystemMessage(message, false);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE),
                    SoundSource.MASTER,
                    player.getX(), player.getY(), player.getZ(),
                    0.85F, 1.0F, player.getRandom().nextLong()
            ));
        }
    }

    private void save() {
        if (config == null) {
            return;
        }
        for (DailySetting setting : DailySetting.values()) {
            config.set(setting.configKey(), values.get(setting));
        }
        config.set("last_date_toggled_utc", lastDateToggled.toString());
        config.save();
    }

    private void closeConfig() {
        if (config != null) {
            try {
                config.close();
            } catch (RuntimeException exception) {
                LOGGER.warn("Error closing Rotbound Daily Chaos config.", exception);
            }
            config = null;
        }
    }
}
