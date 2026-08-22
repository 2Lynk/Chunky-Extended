package codx.chunkyextended.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import codx.chunkyextended.ChunkyExtendedCommon;
import codx.chunkyextended.schedule.ScheduleRule;
import codx.chunkyextended.util.ModUtil;
import codx.codxlib.api.UpdateChecker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.Optional;

public final class ChunkyExtendCommand {
    private ChunkyExtendCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(net.minecraft.commands.Commands.literal("chunky-extend")
                .executes(context -> {
                    sendToCaller(context, "chunky-extend enable - Enable auto stop and start");
                    sendToCaller(context, "chunky-extend disable - Disable auto stop and start");
                    sendToCaller(context, "chunky-extend status - Displays status");
                    sendToCaller(context, "chunky-extend settings - Shows current settings");
                    sendToCaller(context, "chunky-extend schedule - Manage scheduled pause/continue rules");
                    return 0;
                })
                .then(net.minecraft.commands.Commands.literal("enable").executes(ChunkyExtendCommand::enable))
                .then(net.minecraft.commands.Commands.literal("disable").executes(ChunkyExtendCommand::disable))
                .then(net.minecraft.commands.Commands.literal("status").executes(ChunkyExtendCommand::status))
                .then(net.minecraft.commands.Commands.literal("update")
                    .executes(ChunkyExtendCommand::updateStatus)
                    .then(net.minecraft.commands.Commands.literal("check")
                        .executes(ChunkyExtendCommand::updateCheck)))
                .then(net.minecraft.commands.Commands.literal("settings")
                        .executes(ChunkyExtendCommand::settings)
                        .then(net.minecraft.commands.Commands.literal("autopause")
                                .then(net.minecraft.commands.Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setAutoPause(context, BoolArgumentType.getBool(context, "value")))))
                        .then(net.minecraft.commands.Commands.literal("autocontinue")
                                .then(net.minecraft.commands.Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setAutoContinue(context, BoolArgumentType.getBool(context, "value")))))
                        .then(net.minecraft.commands.Commands.literal("scheduler")
                                .then(net.minecraft.commands.Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setSchedulerEnabled(context, BoolArgumentType.getBool(context, "value")))))
                        .then(net.minecraft.commands.Commands.literal("skip-online")
                                .then(net.minecraft.commands.Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setSkipOnline(context, BoolArgumentType.getBool(context, "value")))))
                        .then(net.minecraft.commands.Commands.literal("timezone")
                                .then(net.minecraft.commands.Commands.argument("zone", StringArgumentType.word())
                                        .executes(context -> setTimezone(context, StringArgumentType.getString(context, "zone"))))))
                .then(net.minecraft.commands.Commands.literal("schedule")
                        .executes(ChunkyExtendCommand::scheduleStatus)
                        .then(net.minecraft.commands.Commands.literal("enable")
                                .executes(ChunkyExtendCommand::scheduleEnable))
                        .then(net.minecraft.commands.Commands.literal("disable")
                                .executes(ChunkyExtendCommand::scheduleDisable))
                        .then(net.minecraft.commands.Commands.literal("list")
                                .executes(ChunkyExtendCommand::scheduleList))
                        .then(net.minecraft.commands.Commands.literal("next")
                            .executes(ChunkyExtendCommand::scheduleNext))
                        .then(net.minecraft.commands.Commands.literal("clear")
                                .executes(ChunkyExtendCommand::scheduleClear))
                        .then(net.minecraft.commands.Commands.literal("remove")
                                .then(net.minecraft.commands.Commands.argument("id", IntegerArgumentType.integer(1))
                                        .executes(context -> scheduleRemove(context, IntegerArgumentType.getInteger(context, "id")))))
                        .then(net.minecraft.commands.Commands.literal("preset")
                            .then(net.minecraft.commands.Commands.literal("nightly")
                                .executes(context -> schedulePresetNightly(context, "all"))
                                .then(net.minecraft.commands.Commands.argument("days", StringArgumentType.word())
                                    .executes(context -> schedulePresetNightly(
                                        context,
                                        StringArgumentType.getString(context, "days")
                                    ))))
                            .then(net.minecraft.commands.Commands.literal("weekend")
                                .executes(context -> schedulePresetWeekend(context))
                                .then(net.minecraft.commands.Commands.argument("days", StringArgumentType.word())
                                    .executes(context -> schedulePresetWeekend(
                                        context,
                                        StringArgumentType.getString(context, "days")
                                    ))))
                            .then(net.minecraft.commands.Commands.literal("window")
                                .then(net.minecraft.commands.Commands.argument("start", StringArgumentType.word())
                                    .then(net.minecraft.commands.Commands.argument("end", StringArgumentType.word())
                                        .executes(context -> schedulePresetWindow(
                                            context,
                                            StringArgumentType.getString(context, "start"),
                                            StringArgumentType.getString(context, "end"),
                                            "all"
                                        ))
                                        .then(net.minecraft.commands.Commands.argument("days", StringArgumentType.word())
                                            .executes(context -> schedulePresetWindow(
                                                context,
                                                StringArgumentType.getString(context, "start"),
                                                StringArgumentType.getString(context, "end"),
                                                StringArgumentType.getString(context, "days")
                                            )))))))
                        .then(net.minecraft.commands.Commands.literal("add")
                                .then(net.minecraft.commands.Commands.argument("time", StringArgumentType.word())
                                        .then(net.minecraft.commands.Commands.argument("action", StringArgumentType.word())
                                                .executes(context -> scheduleAdd(context,
                                                        StringArgumentType.getString(context, "time"),
                                                        StringArgumentType.getString(context, "action"),
                                                        "all"))
                                                .then(net.minecraft.commands.Commands.argument("days", StringArgumentType.word())
                                                        .executes(context -> scheduleAdd(context,
                                                                StringArgumentType.getString(context, "time"),
                                                                StringArgumentType.getString(context, "action"),
                                                                StringArgumentType.getString(context, "days"))))))))
        );
    }

    private static int enable(CommandContext<CommandSourceStack> context) {
        if (!ModUtil.returnModEnabled()) {
            ModUtil.enableMod();
            ChunkyExtendedCommon.persistState();
            sendToCaller(context, "Ce: enabled!");
        }
        return 0;
    }

    private static int disable(CommandContext<CommandSourceStack> context) {
        if (ModUtil.returnModEnabled()) {
            ModUtil.disableMod();
            ChunkyExtendedCommon.persistState();
            sendToCaller(context, "Ce: disabled!");
        }
        return 0;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        sendToCaller(context, "Ce: Chunky extend is " + (ModUtil.returnModEnabled() ? "enabled" : "disabled"));
        sendToCaller(context, "Ce: scheduler is " + (ModUtil.isSchedulerEnabled() ? "enabled" : "disabled")
                + " | timezone=" + ModUtil.getTimezone()
                + " | rules=" + ModUtil.getRules().size());
        return 0;
    }

    private static int updateStatus(CommandContext<CommandSourceStack> context) {
        sendToCaller(context, "Ce update checker: use '/chunky-extend update check' to query Modrinth.");
        return 0;
    }

    private static int updateCheck(CommandContext<CommandSourceStack> context) {
        sendToCaller(context, "Ce: checking Modrinth for updates...");

        if (context.getSource().getServer() == null) {
            sendToCaller(context, "Ce: server unavailable for update check.");
            return 0;
        }

        var source = context.getSource();
        var server = context.getSource().getServer();
        String current = ChunkyExtendedCommon.getCurrentModVersion();

        // CodxLib runs the check and invokes the callback on the server thread.
        UpdateChecker.checkVersionAsync(server, ChunkyExtendedCommon.modInfo(), (hasUpdate, latest) -> {
            if (hasUpdate && latest != null) {
                source.sendSuccess(() -> Component.literal("Ce: update available " + latest
                        + " (current: " + current + ")"), false);
                source.sendSuccess(() -> Component.literal("Ce: https://modrinth.com/project/"
                        + ChunkyExtendedCommon.modInfo().modrinthSlug()), false);
            } else {
                source.sendSuccess(() -> Component.literal("Ce: up to date (" + current + ")"), false);
            }
        });

        return 1;
    }

    private static int settings(CommandContext<CommandSourceStack> context) {
        sendToCaller(context, "Ce settings:");
        sendToCaller(context, "- enabled=" + ModUtil.returnModEnabled());
        sendToCaller(context, "- autoPauseOnFirstJoin=" + ModUtil.isAutoPauseOnFirstJoin());
        sendToCaller(context, "- autoContinueOnLastLeave=" + ModUtil.isAutoContinueOnLastLeave());
        sendToCaller(context, "- schedulerEnabled=" + ModUtil.isSchedulerEnabled());
        sendToCaller(context, "- skipScheduleWhenPlayersOnline=" + ModUtil.isSkipScheduleWhenPlayersOnline());
        sendToCaller(context, "- timezone=" + ModUtil.getTimezone());
        return 0;
    }

    private static int setAutoPause(CommandContext<CommandSourceStack> context, boolean value) {
        ModUtil.setAutoPauseOnFirstJoin(value);
        ChunkyExtendedCommon.persistState();
        sendToCaller(context, "Ce: autoPauseOnFirstJoin=" + value);
        return 0;
    }

    private static int setAutoContinue(CommandContext<CommandSourceStack> context, boolean value) {
        ModUtil.setAutoContinueOnLastLeave(value);
        ChunkyExtendedCommon.persistState();
        sendToCaller(context, "Ce: autoContinueOnLastLeave=" + value);
        return 0;
    }

    private static int setSchedulerEnabled(CommandContext<CommandSourceStack> context, boolean value) {
        ModUtil.setSchedulerEnabled(value);
        ChunkyExtendedCommon.persistState();
        sendToCaller(context, "Ce: schedulerEnabled=" + value);
        return 0;
    }

    private static int setSkipOnline(CommandContext<CommandSourceStack> context, boolean value) {
        ModUtil.setSkipScheduleWhenPlayersOnline(value);
        ChunkyExtendedCommon.persistState();
        sendToCaller(context, "Ce: skipScheduleWhenPlayersOnline=" + value);
        return 0;
    }

    private static int setTimezone(CommandContext<CommandSourceStack> context, String zone) {
        try {
            ZoneId.of(zone);
        } catch (Exception e) {
            sendToCaller(context, "Ce: invalid timezone. Example: Europe/Amsterdam");
            return 0;
        }

        ModUtil.setTimezone(zone);
        ChunkyExtendedCommon.persistState();
        sendToCaller(context, "Ce: timezone=" + zone);
        return 0;
    }

    private static int scheduleStatus(CommandContext<CommandSourceStack> context) {
        sendToCaller(context, "Ce scheduler: " + (ModUtil.isSchedulerEnabled() ? "enabled" : "disabled"));
        sendToCaller(context, "Rules: " + ModUtil.getRules().size() + " | timezone=" + ModUtil.getTimezone());
        return 0;
    }

    private static int scheduleEnable(CommandContext<CommandSourceStack> context) {
        ModUtil.setSchedulerEnabled(true);
        ChunkyExtendedCommon.persistState();
        sendToCaller(context, "Ce scheduler enabled");
        return 0;
    }

    private static int scheduleDisable(CommandContext<CommandSourceStack> context) {
        ModUtil.setSchedulerEnabled(false);
        ChunkyExtendedCommon.persistState();
        sendToCaller(context, "Ce scheduler disabled");
        return 0;
    }

    private static int scheduleList(CommandContext<CommandSourceStack> context) {
        if (ModUtil.getRules().isEmpty()) {
            sendToCaller(context, "Ce: no schedule rules.");
            return 0;
        }

        sendToCaller(context, "Ce schedule rules:");
        ModUtil.getRules().stream()
                .sorted(Comparator.comparingInt(ScheduleRule::id))
                .forEach(rule -> sendToCaller(context, "#" + rule.id() + " " + rule.timeString()
                        + " -> chunky " + rule.action() + " (" + rule.daysString() + ")"));
        return 0;
    }

    private static int scheduleAdd(CommandContext<CommandSourceStack> context, String time, String action, String days) {
        try {
            LocalTime parsedTime = ScheduleRule.parseTime(time);
            String normalizedAction = ScheduleRule.normalizeAction(action);
            var parsedDays = ScheduleRule.parseDays(days);

            Optional<ScheduleRule> duplicate = ModUtil.findDuplicateRule(parsedTime, normalizedAction, parsedDays);
            if (duplicate.isPresent()) {
                ScheduleRule existing = duplicate.get();
                sendToCaller(context, "Ce: duplicate rule exists as #" + existing.id() + " (" + existing.timeString() + " -> chunky "
                        + existing.action() + " " + existing.daysString() + ")");
                return 0;
            }

            ScheduleRule rule = ModUtil.addRule(parsedTime, normalizedAction, parsedDays);
            ChunkyExtendedCommon.persistState();
            sendToCaller(context, "Ce: added rule #" + rule.id() + " " + rule.timeString() + " -> chunky " + rule.action()
                    + " (" + rule.daysString() + ")");
        } catch (IllegalArgumentException exception) {
            sendToCaller(context, "Ce: " + exception.getMessage());
        }
        return 0;
    }

    private static int scheduleNext(CommandContext<CommandSourceStack> context) {
        if (ModUtil.getRules().isEmpty()) {
            sendToCaller(context, "Ce: no schedule rules.");
            return 0;
        }

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(ModUtil.getTimezone());
        } catch (Exception exception) {
            zoneId = ZoneId.systemDefault();
        }

        LocalDateTime now = LocalDateTime.now(zoneId).withSecond(0).withNano(0);
        ScheduleRule nextRule = null;
        LocalDateTime nextDateTime = null;

        for (ScheduleRule rule : ModUtil.getRules()) {
            for (int dayOffset = 0; dayOffset < 14; dayOffset++) {
                LocalDate date = now.toLocalDate().plusDays(dayOffset);
                if (!rule.days().contains(date.getDayOfWeek())) {
                    continue;
                }

                LocalDateTime candidate = LocalDateTime.of(date, rule.time());
                if (!candidate.isAfter(now)) {
                    continue;
                }

                if (nextDateTime == null || candidate.isBefore(nextDateTime)) {
                    nextDateTime = candidate;
                    nextRule = rule;
                }
                break;
            }
        }

        if (nextRule == null || nextDateTime == null) {
            sendToCaller(context, "Ce: no upcoming schedule hit found within 14 days.");
            return 0;
        }

        Duration until = Duration.between(now, nextDateTime);
        long totalMinutes = until.toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        String when = nextDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        sendToCaller(context, "Ce next schedule: #" + nextRule.id() + " -> chunky " + nextRule.action()
                + " at " + when + " " + zoneId + " (in " + hours + "h " + minutes + "m)");
        return 0;
    }

    private static int scheduleRemove(CommandContext<CommandSourceStack> context, int id) {
        if (!ModUtil.removeRule(id)) {
            sendToCaller(context, "Ce: no rule found with id=" + id);
            return 0;
        }

        ChunkyExtendedCommon.persistState();
        sendToCaller(context, "Ce: removed rule #" + id);
        return 0;
    }

    private static int scheduleClear(CommandContext<CommandSourceStack> context) {
        ModUtil.clearRules();
        ChunkyExtendedCommon.persistState();
        sendToCaller(context, "Ce: cleared all schedule rules");
        return 0;
    }

    private static int schedulePresetWindow(CommandContext<CommandSourceStack> context, String start, String end, String days) {
        try {
            ScheduleRule startRule = ModUtil.addRule(start, "continue", days);
            ScheduleRule endRule = ModUtil.addRule(end, "pause", days);
            ChunkyExtendedCommon.persistState();

            sendToCaller(context, "Ce: added window preset rules:");
            sendToCaller(context, "- #" + startRule.id() + " " + startRule.timeString() + " -> chunky continue (" + startRule.daysString() + ")");
            sendToCaller(context, "- #" + endRule.id() + " " + endRule.timeString() + " -> chunky pause (" + endRule.daysString() + ")");
            sendToCaller(context, "Tip: use '/chunky-extend schedule remove <id>' to delete one rule.");
        } catch (IllegalArgumentException exception) {
            sendToCaller(context, "Ce: " + exception.getMessage());
        }

        return 0;
    }

    private static int schedulePresetNightly(CommandContext<CommandSourceStack> context, String days) {
        sendToCaller(context, "Ce: applying nightly preset (continue 01:00, pause 07:00)...");
        return schedulePresetWindow(context, "01:00", "07:00", days);
    }

    private static int schedulePresetWeekend(CommandContext<CommandSourceStack> context) {
        return schedulePresetWeekend(context, "sat,sun");
    }

    private static int schedulePresetWeekend(CommandContext<CommandSourceStack> context, String days) {
        sendToCaller(context, "Ce: applying weekend preset (continue 01:00, pause 07:00)...");
        return schedulePresetWindow(context, "01:00", "07:00", days);
    }

    private static void sendToCaller(CommandContext<CommandSourceStack> context, String message) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
            return;
        }

        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }
}