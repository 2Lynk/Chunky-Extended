package codx.chunkyextended.network;

import codx.chunkyextended.ChunkyExtendedCommon;
import codx.chunkyextended.schedule.ScheduleRule;
import codx.chunkyextended.util.ModUtil;
import codx.codxlib.api.network.CodxNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registers the {@code /cemenu} payloads with CodxLib and handles the serverbound
 * ones. Call {@link #register()} once during common init (from each loader's main
 * entrypoint) — early enough that CodxLib's per-loader wiring picks the payloads up.
 *
 * <p>Serverbound handlers run on the server thread (CodxLib hops there). Edits
 * require operator permission (command level {@code GAMEMASTERS} / 2), matching what
 * a server admin would expect from a config UI; the singleplayer host always counts
 * as able to edit. Read requests are open to anyone — the response simply reports
 * {@code canEdit=false} so the client renders read-only.
 */
public final class CeNetworking {

    private static final Permission OP_LEVEL = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private CeNetworking() {
    }

    public static void register() {
        CodxNetwork.registerServerbound(CeMenuPackets.Request.TYPE, CeMenuPackets.Request.CODEC,
                (payload, sender) -> sendState(sender));
        CodxNetwork.registerServerbound(CeMenuPackets.Settings.TYPE, CeMenuPackets.Settings.CODEC,
                CeNetworking::onSettings);
        CodxNetwork.registerServerbound(CeMenuPackets.AddRule.TYPE, CeMenuPackets.AddRule.CODEC,
                CeNetworking::onAddRule);
        CodxNetwork.registerServerbound(CeMenuPackets.RemoveRule.TYPE, CeMenuPackets.RemoveRule.CODEC,
                CeNetworking::onRemoveRule);

        // Type + codec must be registered on both sides so the server can send it;
        // the actual client behaviour is installed via CeClientHook.setHandler.
        CodxNetwork.registerClientbound(CeMenuPackets.State.TYPE, CeMenuPackets.State.CODEC,
                CeClientHook::onState);
    }

    private static void onSettings(CeMenuPackets.Settings payload, ServerPlayer sender) {
        if (canEdit(sender)) {
            ModUtil.setEnabled(payload.enabled());
            ModUtil.setAutoPauseOnFirstJoin(payload.autoPause());
            ModUtil.setAutoContinueOnLastLeave(payload.autoContinue());
            ModUtil.setSchedulerEnabled(payload.scheduler());
            ModUtil.setSkipScheduleWhenPlayersOnline(payload.skipOnline());
            try {
                ZoneId.of(payload.timezone());
                ModUtil.setTimezone(payload.timezone());
            } catch (Exception ignored) {
                // Keep the previous timezone if the client somehow sent a bad one.
            }
            ChunkyExtendedCommon.persistState();
        }
        sendState(sender);
    }

    private static void onAddRule(CeMenuPackets.AddRule payload, ServerPlayer sender) {
        if (canEdit(sender)) {
            try {
                ModUtil.addRule(payload.time(), payload.action(), payload.days());
                ChunkyExtendedCommon.persistState();
            } catch (IllegalArgumentException ignored) {
                // Invalid input (the client validates too); just resend current state.
            }
        }
        sendState(sender);
    }

    private static void onRemoveRule(CeMenuPackets.RemoveRule payload, ServerPlayer sender) {
        if (canEdit(sender) && ModUtil.removeRule(payload.id())) {
            ChunkyExtendedCommon.persistState();
        }
        sendState(sender);
    }

    private static void sendState(ServerPlayer player) {
        List<String> rules = new ArrayList<>();
        ModUtil.getRules().stream()
                .sorted(Comparator.comparingInt(ScheduleRule::id))
                .forEach(rule -> rules.add(rule.id() + "|" + rule.timeString() + "|" + rule.action() + "|" + rule.daysString()));

        CeMenuPackets.State state = new CeMenuPackets.State(
                ModUtil.returnModEnabled(),
                ModUtil.isAutoPauseOnFirstJoin(),
                ModUtil.isAutoContinueOnLastLeave(),
                ModUtil.isSchedulerEnabled(),
                ModUtil.isSkipScheduleWhenPlayersOnline(),
                ModUtil.getTimezone(),
                canEdit(player),
                rules);

        CodxNetwork.sendToPlayer(player, state);
    }

    private static boolean canEdit(ServerPlayer player) {
        if (player.permissions().hasPermission(OP_LEVEL)) {
            return true;
        }
        MinecraftServer server = player.level().getServer();
        return server != null && server.isSingleplayerOwner(player.nameAndId());
    }
}
