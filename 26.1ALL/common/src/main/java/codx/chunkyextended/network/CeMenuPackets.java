package codx.chunkyextended.network;

import codx.chunkyextended.ChunkyExtendedCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The play-phase payloads exchanged between the {@code /cemenu} client screen and
 * the server. Chunky Extended's settings live in server-side state ({@code ModUtil},
 * persisted to {@code ce.json}), so the client never reads them directly — it asks
 * the server for a {@link State} snapshot and sends edits back.
 *
 * <p>Registered with CodxLib's {@code CodxNetwork} in {@link CeNetworking}; the
 * codecs only touch buffer/network classes so they are safe to load on a dedicated
 * server.
 */
public final class CeMenuPackets {

    private CeMenuPackets() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> mkType(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ChunkyExtendedCommon.MOD_ID, path));
    }

    /** Client -> server: "send me the current settings + rules". */
    public record Request() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Request> TYPE = mkType("menu_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.unit(new Request());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server -> client: a full snapshot. {@code rules} are encoded one per string as
     * {@code id|HH:mm|action|days} so the codec stays a flat list of strings.
     * {@code canEdit} tells the client whether this player may change anything.
     */
    public record State(boolean enabled, boolean autoPause, boolean autoContinue,
                        boolean scheduler, boolean skipOnline, String timezone,
                        boolean canEdit, List<String> rules) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<State> TYPE = mkType("menu_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
                StreamCodec.ofMember(State::write, State::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(enabled);
            buf.writeBoolean(autoPause);
            buf.writeBoolean(autoContinue);
            buf.writeBoolean(scheduler);
            buf.writeBoolean(skipOnline);
            buf.writeUtf(timezone);
            buf.writeBoolean(canEdit);
            buf.writeVarInt(rules.size());
            for (String rule : rules) {
                buf.writeUtf(rule);
            }
        }

        private static State read(RegistryFriendlyByteBuf buf) {
            boolean enabled = buf.readBoolean();
            boolean autoPause = buf.readBoolean();
            boolean autoContinue = buf.readBoolean();
            boolean scheduler = buf.readBoolean();
            boolean skipOnline = buf.readBoolean();
            String timezone = buf.readUtf();
            boolean canEdit = buf.readBoolean();
            int count = buf.readVarInt();
            List<String> rules = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                rules.add(buf.readUtf());
            }
            return new State(enabled, autoPause, autoContinue, scheduler, skipOnline, timezone, canEdit, rules);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: apply the toggle/timezone settings. */
    public record Settings(boolean enabled, boolean autoPause, boolean autoContinue,
                           boolean scheduler, boolean skipOnline, String timezone) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Settings> TYPE = mkType("menu_settings");
        public static final StreamCodec<RegistryFriendlyByteBuf, Settings> CODEC =
                StreamCodec.ofMember(Settings::write, Settings::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(enabled);
            buf.writeBoolean(autoPause);
            buf.writeBoolean(autoContinue);
            buf.writeBoolean(scheduler);
            buf.writeBoolean(skipOnline);
            buf.writeUtf(timezone);
        }

        private static Settings read(RegistryFriendlyByteBuf buf) {
            return new Settings(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                    buf.readBoolean(), buf.readBoolean(), buf.readUtf());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: add a schedule rule. */
    public record AddRule(String time, String action, String days) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<AddRule> TYPE = mkType("menu_rule_add");
        public static final StreamCodec<RegistryFriendlyByteBuf, AddRule> CODEC =
                StreamCodec.ofMember(AddRule::write, AddRule::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(time);
            buf.writeUtf(action);
            buf.writeUtf(days);
        }

        private static AddRule read(RegistryFriendlyByteBuf buf) {
            return new AddRule(buf.readUtf(), buf.readUtf(), buf.readUtf());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: remove the schedule rule with this id. */
    public record RemoveRule(int id) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<RemoveRule> TYPE = mkType("menu_rule_remove");
        public static final StreamCodec<RegistryFriendlyByteBuf, RemoveRule> CODEC =
                StreamCodec.ofMember(RemoveRule::write, RemoveRule::read);

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(id);
        }

        private static RemoveRule read(RegistryFriendlyByteBuf buf) {
            return new RemoveRule(buf.readVarInt());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
