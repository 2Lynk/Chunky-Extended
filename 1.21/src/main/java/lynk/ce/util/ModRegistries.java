package lynk.ce.util;

import lynk.ce.command.Commands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
public class ModRegistries {
    public static void registerCommands(){
        CommandRegistrationCallback.EVENT.register(Commands::register);
    }
}
