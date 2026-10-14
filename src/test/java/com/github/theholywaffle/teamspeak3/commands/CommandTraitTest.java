package com.github.theholywaffle.teamspeak3.commands;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CommandTraitTest {

    @Test
    void builtAndRawCommandsShareOneDefinition() {
        var expected = Map.of(
                QueryCommands.logIn("serveradmin", "secret"),
                        EnumSet.of(CommandTrait.SESSION_STATE, CommandTrait.CONNECTION),
                QueryCommands.logOut(), EnumSet.of(CommandTrait.SESSION_STATE, CommandTrait.CONNECTION),
                QueryCommands.useId(1, null), EnumSet.of(CommandTrait.SESSION_STATE, CommandTrait.CONNECTION),
                QueryCommands.quit(), EnumSet.of(CommandTrait.CONNECTION, CommandTrait.ENDS_CONNECTION),
                ClientCommands.clientUpdate(Map.of()), EnumSet.of(CommandTrait.SESSION_STATE),
                QueryCommands.serverNotifyRegister(TS3EventType.SERVER, -1),
                        EnumSet.of(CommandTrait.SESSION_STATE, CommandTrait.NOTIFICATIONS),
                QueryCommands.serverNotifyUnregister(),
                        EnumSet.of(CommandTrait.SESSION_STATE, CommandTrait.NOTIFICATIONS),
                FileCommands.ftList(), EnumSet.of(CommandTrait.FILES),
                QueryCommands.rawCommand("help channellist"), EnumSet.of(CommandTrait.HELP),
                ChannelCommands.channelList(), EnumSet.noneOf(CommandTrait.class));
        expected.forEach((command, traits) -> {
            assertEquals(traits, command.getTraits(), command.getName());
            assertEquals(traits, QueryCommands.rawCommand(command.getName()).getTraits(), command.getName());
        });
        for (String name : List.of("ftgetfilelist", "ftinitupload", "ftrenamefile")) {
            assertEquals(
                    Set.of(CommandTrait.FILES), QueryCommands.rawCommand(name).getTraits(), name);
        }
        assertThrows(
                UnsupportedOperationException.class,
                () -> ChannelCommands.channelList().getTraits().add(CommandTrait.HELP));
    }
}
