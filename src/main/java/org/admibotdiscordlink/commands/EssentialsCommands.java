package org.admibotdiscordlink.commands;

import org.admibotdiscordlink.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class EssentialsCommands implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (command.getName().equalsIgnoreCase("checkstatus")) {
            if (!(commandSender.hasPermission("admibot.statuscheck"))) {
                return true;
            }

            // Send HTTP GET request to check server status
            boolean serverStatus = checkServerStatus();
            String statusMessage = serverStatus ? "§aConnected" : "§cDisconnected";

            // Display the status message in Minecraft chat
            commandSender.sendMessage(ColorUtil.translateMessage("&b&lStatus: ") + statusMessage);
        }

        if (command.getName().equalsIgnoreCase("guide")) {
            if (commandSender instanceof Player) {
                Player p = (Player) commandSender;
                p.sendMessage(ColorUtil.translateMessage("&b&lAdmiBot Guide Book&b\n" +
                        "&bSend commands in your own commands channel to execute commands\n" +
                        "&busing our bot as equal as if you're using the console but in Discord!"));
            } else {
                commandSender.sendMessage("Only can be executed by players.");
            }
        }

        // Add more commands here...

        return true;
    }

    private boolean checkServerStatus() {
        String testEndpoint = "http://eu3.diresnode.com:3151/test"; // Update with your server's test endpoint
        try {
            URL url = new URL(testEndpoint);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            // Check HTTP response code
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Server is connected (200 OK response)
                return true;
            } else {
                // Server is disconnected or response not OK
                return false;
            }
        } catch (IOException e) {
            // Handle connection or request failure
            return false; // Server status check failed
        }
    }
}
