package codx.chunkyextended.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class ScheduleRule {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final int id;
    private final LocalTime time;
    private final String action;
    private final Set<DayOfWeek> days;

    public ScheduleRule(int id, LocalTime time, String action, Set<DayOfWeek> days) {
        this.id = id;
        this.time = time;
        this.action = action;
        this.days = EnumSet.copyOf(days);
    }

    public int id() {
        return id;
    }

    public LocalTime time() {
        return time;
    }

    public String action() {
        return action;
    }

    public Set<DayOfWeek> days() {
        return EnumSet.copyOf(days);
    }

    public boolean matches(DayOfWeek day, int hour, int minute) {
        return days.contains(day) && time.getHour() == hour && time.getMinute() == minute;
    }

    public String timeString() {
        return time.format(TIME_FORMAT);
    }

    public String daysString() {
        if (days.size() == 7) {
            return "all";
        }

        return days.stream()
                .map(ScheduleRule::toShortDay)
                .collect(Collectors.joining(","));
    }

    public static LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format. Use HH:mm (24h).", e);
        }
    }

    public static Set<DayOfWeek> parseDays(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("all")) {
            return EnumSet.allOf(DayOfWeek.class);
        }

        EnumSet<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        String[] segments = value.toLowerCase(Locale.ROOT).split(",");

        for (String rawSegment : segments) {
            String segment = rawSegment.trim();
            if (segment.isEmpty()) {
                continue;
            }

            if (segment.contains("-")) {
                String[] range = segment.split("-");
                if (range.length != 2) {
                    throw new IllegalArgumentException("Invalid day range: " + segment);
                }
                DayOfWeek start = fromDayToken(range[0]);
                DayOfWeek end = fromDayToken(range[1]);

                int cursor = start.getValue();
                while (true) {
                    result.add(DayOfWeek.of(cursor));
                    if (cursor == end.getValue()) {
                        break;
                    }
                    cursor = cursor == 7 ? 1 : cursor + 1;
                }
            } else {
                result.add(fromDayToken(segment));
            }
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("No valid days provided.");
        }

        return result;
    }

    public static String normalizeAction(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.equals("pause") && !normalized.equals("continue")) {
            throw new IllegalArgumentException("Action must be 'pause' or 'continue'.");
        }
        return normalized;
    }

    private static DayOfWeek fromDayToken(String raw) {
        String token = raw.trim().toLowerCase(Locale.ROOT);
        return switch (token) {
            case "mon", "monday" -> DayOfWeek.MONDAY;
            case "tue", "tues", "tuesday" -> DayOfWeek.TUESDAY;
            case "wed", "wednesday" -> DayOfWeek.WEDNESDAY;
            case "thu", "thur", "thurs", "thursday" -> DayOfWeek.THURSDAY;
            case "fri", "friday" -> DayOfWeek.FRIDAY;
            case "sat", "saturday" -> DayOfWeek.SATURDAY;
            case "sun", "sunday" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Invalid day token: " + raw);
        };
    }

    private static String toShortDay(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "mon";
            case TUESDAY -> "tue";
            case WEDNESDAY -> "wed";
            case THURSDAY -> "thu";
            case FRIDAY -> "fri";
            case SATURDAY -> "sat";
            case SUNDAY -> "sun";
        };
    }
}