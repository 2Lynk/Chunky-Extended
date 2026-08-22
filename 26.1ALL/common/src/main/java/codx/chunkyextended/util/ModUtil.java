package codx.chunkyextended.util;

import codx.chunkyextended.schedule.ScheduleRule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ModUtil {
    private static boolean enabled = false;
    private static boolean autoPauseOnFirstJoin = true;
    private static boolean autoContinueOnLastLeave = true;
    private static boolean schedulerEnabled = false;
    private static boolean skipScheduleWhenPlayersOnline = true;
    private static String timezone = ZoneId.systemDefault().getId();
    private static int nextRuleId = 1;
    private static final List<ScheduleRule> rules = new ArrayList<>();
    private static final Set<String> executedKeys = new HashSet<>();

    private ModUtil() {
    }

    public static void enableMod() {
        enabled = true;
    }

    public static void disableMod() {
        enabled = false;
    }

    public static boolean returnModEnabled() {
        return enabled;
    }

    public static boolean isAutoPauseOnFirstJoin() {
        return autoPauseOnFirstJoin;
    }

    public static void setAutoPauseOnFirstJoin(boolean value) {
        autoPauseOnFirstJoin = value;
    }

    public static boolean isAutoContinueOnLastLeave() {
        return autoContinueOnLastLeave;
    }

    public static void setAutoContinueOnLastLeave(boolean value) {
        autoContinueOnLastLeave = value;
    }

    public static boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public static void setSchedulerEnabled(boolean value) {
        schedulerEnabled = value;
    }

    public static boolean isSkipScheduleWhenPlayersOnline() {
        return skipScheduleWhenPlayersOnline;
    }

    public static void setSkipScheduleWhenPlayersOnline(boolean value) {
        skipScheduleWhenPlayersOnline = value;
    }

    public static String getTimezone() {
        return timezone;
    }

    public static void setTimezone(String zoneId) {
        ZoneId.of(zoneId);
        timezone = zoneId;
    }

    public static List<ScheduleRule> getRules() {
        return List.copyOf(rules);
    }

    public static ScheduleRule addRule(String timeText, String actionText, String daysText) {
        LocalTime time = ScheduleRule.parseTime(timeText);
        String action = ScheduleRule.normalizeAction(actionText);
        Set<DayOfWeek> days = ScheduleRule.parseDays(daysText);

        return addRule(time, action, days);
    }

    public static ScheduleRule addRule(LocalTime time, String action, Set<DayOfWeek> days) {
        String normalizedAction = ScheduleRule.normalizeAction(action);

        ScheduleRule rule = new ScheduleRule(nextRuleId++, time, normalizedAction, days);
        rules.add(rule);
        return rule;
    }

    public static Optional<ScheduleRule> findDuplicateRule(LocalTime time, String action, Set<DayOfWeek> days) {
        String normalizedAction = ScheduleRule.normalizeAction(action);
        return rules.stream()
                .filter(rule -> rule.time().equals(time)
                        && rule.action().equals(normalizedAction)
                        && rule.days().equals(days))
                .findFirst();
    }

    public static boolean removeRule(int id) {
        return rules.removeIf(rule -> rule.id() == id);
    }

    public static void clearRules() {
        rules.clear();
    }

    public static void setRules(List<ScheduleRule> newRules, int nextId) {
        rules.clear();
        rules.addAll(newRules);
        nextRuleId = Math.max(nextId, rules.stream().mapToInt(ScheduleRule::id).max().orElse(0) + 1);
    }

    public static int getNextRuleId() {
        return nextRuleId;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void setExecutedToday(String key) {
        executedKeys.add(key);
    }

    public static boolean alreadyExecutedToday(String key) {
        return executedKeys.contains(key);
    }

    public static void resetExecutedKeys() {
        executedKeys.clear();
    }
}