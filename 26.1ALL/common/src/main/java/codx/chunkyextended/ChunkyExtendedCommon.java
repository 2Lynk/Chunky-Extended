package codx.chunkyextended;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import codx.chunkyextended.command.ChunkyExtendCommand;
import codx.chunkyextended.network.CeNetworking;
import codx.chunkyextended.schedule.ScheduleRule;
import codx.chunkyextended.util.ModUtil;
import codx.codxlib.api.CodxLib;
import codx.codxlib.api.ModInfo;
import codx.codxlib.api.UpdateChecker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class ChunkyExtendedCommon {
    public static final String MOD_ID = "chunkyextended";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_PATH = Paths.get("ce.json");
    private static final Path LEGACY_DATA_PATH = Paths.get("ce.txt");
    private static String lastSchedulerMinuteKey = "";
    private static MinecraftServer currentServer;

    private ChunkyExtendedCommon() {
    }

    /** CodxLib identity (slug uses the Modrinth project id, which the API also accepts). */
    public static ModInfo modInfo() {
        return new ModInfo(MOD_ID, "LFJf0Klb", CodxLib.version(MOD_ID), "[ChunkyExtended]");
    }

    public static void logLoaded(String loaderName) {
        UpdateChecker.register(modInfo());
        CeNetworking.register();
        LOGGER.info("Chunky Extended loaded on {}", loaderName);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        ChunkyExtendCommand.register(dispatcher);
    }

    public static void onPlayerJoin(MinecraftServer server) {
        if (ModUtil.isAutoPauseOnFirstJoin() && server.getPlayerList().getPlayerCount() == 0) {
            executeServerCommand(server, "chunky pause");
        }
    }

    public static void onPlayerDisconnect(MinecraftServer server) {
        if (ModUtil.returnModEnabled() && ModUtil.isAutoContinueOnLastLeave() && server.getPlayerList().getPlayerCount() == 1) {
            executeServerCommand(server, "chunky continue");
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        currentServer = server;
        loadState();
        // Update notices are handled by CodxLib (registered in logLoaded) — it logs to
        // the server console on start and notifies operators on join.
    }

    public static void onServerStopping() {
        persistState();
        currentServer = null;
        ModUtil.resetExecutedKeys();
    }

    public static void onServerTick(MinecraftServer server) {
        if (!ModUtil.isSchedulerEnabled()) {
            return;
        }

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(ModUtil.getTimezone());
        } catch (Exception exception) {
            zoneId = ZoneId.systemDefault();
        }

        LocalDateTime now = LocalDateTime.now(zoneId);
        String minuteKey = now.getYear() + "-" + now.getDayOfYear() + "-" + now.getHour() + "-" + now.getMinute();
        if (minuteKey.equals(lastSchedulerMinuteKey)) {
            return;
        }
        lastSchedulerMinuteKey = minuteKey;

        if (ModUtil.isSkipScheduleWhenPlayersOnline() && server.getPlayerList().getPlayerCount() > 0) {
            return;
        }

        DayOfWeek day = now.getDayOfWeek();
        int hour = now.getHour();
        int minute = now.getMinute();
        LocalDate date = now.toLocalDate();

        for (ScheduleRule rule : ModUtil.getRules()) {
            if (!rule.matches(day, hour, minute)) {
                continue;
            }

            String executeKey = rule.id() + "|" + date;
            if (ModUtil.alreadyExecutedToday(executeKey)) {
                continue;
            }

            executeServerCommand(server, "chunky " + rule.action());
            ModUtil.setExecutedToday(executeKey);
            LOGGER.info("Executed CE schedule rule #{} at {} ({})", rule.id(), rule.timeString(), ModUtil.getTimezone());
        }
    }

    public static void persistState() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("enabled", ModUtil.returnModEnabled());
            root.addProperty("autoPauseOnFirstJoin", ModUtil.isAutoPauseOnFirstJoin());
            root.addProperty("autoContinueOnLastLeave", ModUtil.isAutoContinueOnLastLeave());
            root.addProperty("schedulerEnabled", ModUtil.isSchedulerEnabled());
            root.addProperty("skipScheduleWhenPlayersOnline", ModUtil.isSkipScheduleWhenPlayersOnline());
            root.addProperty("timezone", ModUtil.getTimezone());
            root.addProperty("nextRuleId", ModUtil.getNextRuleId());

            JsonArray ruleArray = new JsonArray();
            for (ScheduleRule rule : ModUtil.getRules()) {
                JsonObject ruleJson = new JsonObject();
                ruleJson.addProperty("id", rule.id());
                ruleJson.addProperty("time", rule.timeString());
                ruleJson.addProperty("action", rule.action());

                JsonArray daysJson = new JsonArray();
                for (DayOfWeek day : rule.days()) {
                    daysJson.add(day.name());
                }
                ruleJson.add("days", daysJson);
                ruleArray.add(ruleJson);
            }
            root.add("rules", ruleArray);

            Files.writeString(DATA_PATH, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to persist CE state file", e);
        }
    }

    private static void loadState() {
        if (!Files.exists(DATA_PATH)) {
            loadLegacyState();
            persistState();
            return;
        }

        try {
            String content = Files.readString(DATA_PATH, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(content);
            if (!parsed.isJsonObject()) {
                return;
            }

            JsonObject root = parsed.getAsJsonObject();
            ModUtil.setEnabled(readBoolean(root, "enabled", false));
            ModUtil.setAutoPauseOnFirstJoin(readBoolean(root, "autoPauseOnFirstJoin", true));
            ModUtil.setAutoContinueOnLastLeave(readBoolean(root, "autoContinueOnLastLeave", true));
            ModUtil.setSchedulerEnabled(readBoolean(root, "schedulerEnabled", false));
            ModUtil.setSkipScheduleWhenPlayersOnline(readBoolean(root, "skipScheduleWhenPlayersOnline", true));

            String timezone = readString(root, "timezone", ZoneId.systemDefault().getId());
            try {
                ModUtil.setTimezone(timezone);
            } catch (Exception ignored) {
                ModUtil.setTimezone(ZoneId.systemDefault().getId());
            }

            List<ScheduleRule> loadedRules = new ArrayList<>();
            int maxId = 0;
            if (root.has("rules") && root.get("rules").isJsonArray()) {
                JsonArray rulesJson = root.getAsJsonArray("rules");
                for (JsonElement element : rulesJson) {
                    if (!element.isJsonObject()) {
                        continue;
                    }

                    JsonObject ruleObj = element.getAsJsonObject();
                    int id = ruleObj.has("id") ? ruleObj.get("id").getAsInt() : 0;
                    String time = readString(ruleObj, "time", "00:00");
                    String action = readString(ruleObj, "action", "pause");

                    EnumSet<DayOfWeek> days = EnumSet.allOf(DayOfWeek.class);
                    if (ruleObj.has("days") && ruleObj.get("days").isJsonArray()) {
                        days.clear();
                        for (JsonElement dayElement : ruleObj.getAsJsonArray("days")) {
                            try {
                                days.add(DayOfWeek.valueOf(dayElement.getAsString()));
                            } catch (Exception ignored) {
                            }
                        }
                        if (days.isEmpty()) {
                            days = EnumSet.allOf(DayOfWeek.class);
                        }
                    }

                    try {
                        ScheduleRule rule = new ScheduleRule(
                                id,
                                ScheduleRule.parseTime(time),
                                ScheduleRule.normalizeAction(action),
                                days
                        );
                        loadedRules.add(rule);
                        if (id > maxId) {
                            maxId = id;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            int nextRuleId = root.has("nextRuleId") ? root.get("nextRuleId").getAsInt() : maxId + 1;
            ModUtil.setRules(loadedRules, nextRuleId);
        } catch (Exception e) {
            LOGGER.error("Failed to load CE state file", e);
        }
    }

    private static void loadLegacyState() {
        if (!Files.exists(LEGACY_DATA_PATH)) {
            return;
        }

        try {
            String value = Files.readString(LEGACY_DATA_PATH, StandardCharsets.UTF_8).trim();
            ModUtil.setEnabled("true".equalsIgnoreCase(value));
        } catch (IOException e) {
            LOGGER.error("Failed to load legacy CE state file", e);
        }
    }

    private static boolean readBoolean(JsonObject object, String key, boolean fallback) {
        return object.has(key) ? object.get(key).getAsBoolean() : fallback;
    }

    private static String readString(JsonObject object, String key, String fallback) {
        return object.has(key) ? object.get(key).getAsString() : fallback;
    }

    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }

    public static String getCurrentModVersion() {
        String version = ChunkyExtendedCommon.class.getPackage().getImplementationVersion();
        return (version == null || version.isBlank()) ? "0.0.0" : version;
    }

    private static void executeServerCommand(MinecraftServer server, String command) {
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        ParseResults<CommandSourceStack> parseResults = dispatcher.parse(command, server.createCommandSourceStack());
        try {
            dispatcher.execute(parseResults);
        } catch (CommandSyntaxException e) {
            LOGGER.error("Failed to execute command: {}", command, e);
        }
    }
}