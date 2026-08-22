package codx.chunkyextended.client;

import codx.chunkyextended.ChunkyExtendedCommon;
import codx.chunkyextended.network.CeMenuPackets;
import codx.codxlib.api.CodxLib;
import codx.codxlib.api.network.CodxNetwork;
import codx.codxlib.api.ui.CodxConfigScreen;
import codx.codxlib.api.ui.CodxWidgets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@code /cemenu} screen. Built on CodxLib's {@link CodxConfigScreen} (vanilla
 * "pack screen" styling) and {@link CodxWidgets}. The left column edits the toggle
 * settings + timezone; the right column lists schedule rules with remove buttons and
 * an add form. Every change is sent to the server, which persists it and echoes a
 * fresh {@link CeMenuPackets.State} back through {@link #applyState}.
 */
public final class CeMenuScreen extends CodxConfigScreen {

    private static final int ACCENT_GREEN = 0xFF5CC85C;

    private static final int HEADER_COLOR = 0xFFFFFFFF;
    private static final int RULE_COLOR = 0xFFD0D0D0;
    private static final int MUTED_COLOR = 0xFF9090A0;
    private static final int OK_COLOR = 0xFF6ACB6A;
    private static final int ERR_COLOR = 0xFFE06A6A;
    private static final int NOTE_COLOR = 0xFFE0A030;
    private static final int DAY_ON_COLOR = 0xFF5CC85C;
    private static final int DAY_OFF_COLOR = 0xFF808080;

    private static final DayOfWeek[] WEEK = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };

    // A small curated timezone list for the cycler (no text-entry widget available).
    private static final String[] BASE_ZONES = {
            "UTC", "Europe/London", "Europe/Amsterdam", "Europe/Berlin", "Europe/Moscow",
            "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
            "America/Sao_Paulo", "Asia/Kolkata", "Asia/Shanghai", "Asia/Tokyo", "Australia/Sydney"
    };

    // Working copy of the server state.
    private boolean enabled;
    private boolean autoPause;
    private boolean autoContinue;
    private boolean scheduler;
    private boolean skipOnline;
    private String timezone;
    private boolean canEdit;
    private List<String> ruleEncoded = List.of();
    private final List<RuleView> rules = new ArrayList<>();
    private String[] zones;

    // Add-form state (preserved across rebuilds).
    private int addHour;
    private int addMinute;
    private String addAction = "pause";
    private final EnumSet<DayOfWeek> addDays = EnumSet.allOf(DayOfWeek.class);
    private int ruleScroll;
    private Component status;

    // Geometry for the rule rows, computed in addContents() and read in drawExtras().
    private final List<RuleView> visibleRules = new ArrayList<>();
    private int visibleTop;
    private int visibleRowH = 22;
    private int rightColX;
    private int colWidth;

    public CeMenuScreen(Screen parent, CeMenuPackets.State state) {
        super(parent, Component.literal("Chunky Extended"),
                Component.literal("Auto-pause / continue controls"));
        setAccent(ACCENT_GREEN);
        copyFrom(state);
        this.ruleEncoded = state.rules();
        parseRules(state.rules());
    }

    private record RuleView(int id, String time, String action, String days) {
    }

    private void copyFrom(CeMenuPackets.State state) {
        this.enabled = state.enabled();
        this.autoPause = state.autoPause();
        this.autoContinue = state.autoContinue();
        this.scheduler = state.scheduler();
        this.skipOnline = state.skipOnline();
        this.timezone = state.timezone();
        this.canEdit = state.canEdit();
        this.zones = buildZones(state.timezone());
    }

    /** Re-applies a snapshot from the server. Rebuilds only if the rule list changed. */
    public void applyState(CeMenuPackets.State state) {
        copyFrom(state);
        if (!state.rules().equals(ruleEncoded)) {
            ruleEncoded = state.rules();
            parseRules(state.rules());
            if (ruleScroll > Math.max(0, rules.size() - 1)) {
                ruleScroll = 0;
            }
            rebuildWidgets();
        }
    }

    private void parseRules(List<String> encoded) {
        rules.clear();
        for (String entry : encoded) {
            String[] parts = entry.split("\\|", 4);
            if (parts.length < 4) {
                continue;
            }
            int id;
            try {
                id = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                continue;
            }
            rules.add(new RuleView(id, parts[1], parts[2], parts[3]));
        }
    }

    private static String[] buildZones(String current) {
        Set<String> set = new LinkedHashSet<>();
        if (current != null && !current.isBlank()) {
            set.add(current);
        }
        for (String zone : BASE_ZONES) {
            set.add(zone);
        }
        return set.toArray(new String[0]);
    }

    @Override
    protected void addContents() {
        int gap = 8;
        colWidth = (width - gap * 3) / 2;
        int leftX = gap;
        rightColX = gap * 2 + colWidth;

        addSettingsColumn(leftX, colWidth);
        addScheduleColumn(rightColX, colWidth);
    }

    private void addSettingsColumn(int x, int w) {
        int y = contentTop() + 18;
        int step = 24;

        addGated(CodxWidgets.toggle(x, y, w, 20, Component.literal("Mod enabled"),
                () -> enabled, v -> {
                    enabled = v;
                    sendSettings();
                }));
        y += step;
        addGated(CodxWidgets.toggle(x, y, w, 20, Component.literal("Auto-pause on join"),
                () -> autoPause, v -> {
                    autoPause = v;
                    sendSettings();
                }));
        y += step;
        addGated(CodxWidgets.toggle(x, y, w, 20, Component.literal("Auto-continue on leave"),
                () -> autoContinue, v -> {
                    autoContinue = v;
                    sendSettings();
                }));
        y += step;
        addGated(CodxWidgets.toggle(x, y, w, 20, Component.literal("Scheduler"),
                () -> scheduler, v -> {
                    scheduler = v;
                    sendSettings();
                }));
        y += step;
        addGated(CodxWidgets.toggle(x, y, w, 20, Component.literal("Skip when players online"),
                () -> skipOnline, v -> {
                    skipOnline = v;
                    sendSettings();
                }));
        y += step;
        addGated(CodxWidgets.cycle(x, y, w, 20,
                () -> timezone, v -> {
                    timezone = v;
                    sendSettings();
                },
                zone -> Component.literal("TZ: " + zone), zones));
    }

    private void addScheduleColumn(int x, int w) {
        int listTop = contentTop() + 18;
        int formHeight = canEdit ? 90 : 0;
        int listBottom = contentBottom() - formHeight - 12;
        visibleRowH = 22;
        int maxRows = Math.max(1, (listBottom - listTop) / visibleRowH);

        ruleScroll = Math.max(0, Math.min(ruleScroll, Math.max(0, rules.size() - maxRows)));

        visibleRules.clear();
        visibleTop = listTop;
        int end = Math.min(rules.size(), ruleScroll + maxRows);
        for (int i = ruleScroll; i < end; i++) {
            RuleView rule = rules.get(i);
            visibleRules.add(rule);
            int rowY = listTop + (i - ruleScroll) * visibleRowH;
            int btnX = x + w - 58;
            addGated(Button.builder(Component.literal("Remove"), b -> removeRule(rule.id()))
                    .bounds(btnX, rowY, 56, 18).build());
        }

        // Scroll buttons (always usable, even read-only) when the list overflows.
        if (rules.size() > maxRows) {
            addRenderableWidget(Button.builder(Component.literal("▲"), b -> scrollRules(-1))
                    .bounds(x + w - 40, contentTop() + 2, 18, 14).build());
            addRenderableWidget(Button.builder(Component.literal("▼"), b -> scrollRules(1))
                    .bounds(x + w - 20, contentTop() + 2, 18, 14).build());
        }

        if (!canEdit) {
            return;
        }

        // Add form (no text entry): hour/minute sliders, action cycle, day toggles, add button.
        int formY = listBottom + 8;
        int half = (w - 6) / 2;
        addRenderableWidget(CodxWidgets.intSlider(x, formY, half, 18, Component.literal("Hour"),
                0, 23, () -> addHour, v -> addHour = v));
        addRenderableWidget(CodxWidgets.intSlider(x + half + 6, formY, w - half - 6, 18, Component.literal("Min"),
                0, 59, () -> addMinute, v -> addMinute = v));

        int formY2 = formY + 22;
        addRenderableWidget(CodxWidgets.cycle(x, formY2, w, 18,
                () -> addAction, v -> addAction = v,
                a -> Component.literal("Action: " + a), new String[]{"pause", "continue"}));

        // Row of 7 day toggles; clicking flips a day, label colour shows selection.
        int formY3 = formY2 + 22;
        int dayGap = 2;
        int dayW = (w - dayGap * 6) / 7;
        for (int i = 0; i < WEEK.length; i++) {
            DayOfWeek day = WEEK[i];
            boolean on = addDays.contains(day);
            int dayX = x + i * (dayW + dayGap);
            Component label = Component.literal(shortDay(day)).withColor(on ? DAY_ON_COLOR : DAY_OFF_COLOR);
            addRenderableWidget(Button.builder(label, b -> toggleDay(day))
                    .bounds(dayX, formY3, dayW, 18).build());
        }

        int formY4 = formY3 + 22;
        addRenderableWidget(Button.builder(Component.literal("All"), b -> setAllDays(true))
                .bounds(x, formY4, 40, 18).build());
        addRenderableWidget(Button.builder(Component.literal("None"), b -> setAllDays(false))
                .bounds(x + 44, formY4, 44, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Add rule"), b -> addRule())
                .bounds(x + 92, formY4, w - 92, 18).build());
    }

    private void toggleDay(DayOfWeek day) {
        if (!addDays.add(day)) {
            addDays.remove(day);
        }
        rebuildWidgets();
    }

    private void setAllDays(boolean all) {
        addDays.clear();
        if (all) {
            addDays.addAll(List.of(WEEK));
        }
        rebuildWidgets();
    }

    private void scrollRules(int delta) {
        ruleScroll += delta;
        rebuildWidgets();
    }

    private void sendSettings() {
        CodxNetwork.sendToServer(new CeMenuPackets.Settings(
                enabled, autoPause, autoContinue, scheduler, skipOnline, timezone));
    }

    private void addRule() {
        if (addDays.isEmpty()) {
            status = Component.literal("Pick at least one day (or All)");
            return;
        }
        String time = String.format("%02d:%02d", addHour, addMinute);
        CodxNetwork.sendToServer(new CeMenuPackets.AddRule(time, addAction, daysString()));
        status = Component.literal("Added rule " + time + " → " + addAction);
    }

    private String daysString() {
        if (addDays.size() == WEEK.length) {
            return "all";
        }
        return java.util.Arrays.stream(WEEK)
                .filter(addDays::contains)
                .map(CeMenuScreen::shortDay)
                .collect(Collectors.joining(","));
    }

    private static String shortDay(DayOfWeek day) {
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

    private void removeRule(int id) {
        CodxNetwork.sendToServer(new CeMenuPackets.RemoveRule(id));
        status = Component.literal("Removed rule #" + id);
    }

    private <T extends AbstractWidget> T addGated(T widget) {
        if (!canEdit) {
            widget.active = false;
        }
        return addRenderableWidget(widget);
    }

    @Override
    protected Component headerTag() {
        return Component.literal("v" + CodxLib.version(ChunkyExtendedCommon.MOD_ID));
    }

    @Override
    protected void drawExtras(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.text(font, Component.literal("Settings"), 8, contentTop() + 5, HEADER_COLOR);
        graphics.text(font, Component.literal("Schedule rules"), rightColX, contentTop() + 5, HEADER_COLOR);

        if (rules.isEmpty()) {
            graphics.text(font, Component.literal("No rules yet."), rightColX, visibleTop + 4, MUTED_COLOR);
        } else {
            for (int i = 0; i < visibleRules.size(); i++) {
                RuleView rule = visibleRules.get(i);
                int rowY = visibleTop + i * visibleRowH + 5;
                Component line = Component.literal("#" + rule.id() + " " + rule.time()
                        + " → " + rule.action() + " (" + rule.days() + ")");
                graphics.text(font, line, rightColX, rowY, RULE_COLOR);
            }
        }

        if (!canEdit) {
            graphics.text(font, Component.literal("Operator only — read-only"),
                    8, contentBottom() - 10, NOTE_COLOR);
        }
        if (status != null) {
            graphics.text(font, status, rightColX, contentBottom() - 10,
                    status.getString().startsWith("Pick") ? ERR_COLOR : OK_COLOR);
        }
    }
}
