package org.admibotdiscordlink.functions;

import org.apache.commons.lang3.CharUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActionsChecker {

    private final String uniqueKey;
    private final ExecutorService executorService;
    private final Logger logger;

    public ActionsChecker(String uniqueKey, Logger logger) {
        this.uniqueKey = uniqueKey;
        this.logger = logger;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void checkForCommands() {
        executorService.submit(() -> {
            try {
                URL url = new URL("http://eu3.diresnode.com:3151/loop");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("uniqueKey", uniqueKey);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String commandResponse = response.toString().trim();
                if (!commandResponse.isEmpty()) {
                    processCommand(commandResponse);
                }

            } catch (Exception e) {}
        });
    }

    private void processCommand(String commandResponse) {
        try {
            JSONObject json = new JSONObject(commandResponse);
            if (json.has("action")) {
                String action = json.getString("action");
                switch (action.toLowerCase()) {
                    case "command":
                        String value = json.optString("value");
                        if (!value.isEmpty()) {
                            executeCommand(value);
                        } else {
                            logger.warning("Received empty command value.");
                        }
                        break;
                    case "shutdown":
                        logger.info("Received shutdown command. Shutting down server...");
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "stop");
                        break;
                    case "teleport":
                        String playerName = json.optString("player");
                        double x = json.optDouble("x", 0.0);
                        double y = json.optDouble("y", 0.0);
                        double z = json.optDouble("z", 0.0);
                        Player player = Bukkit.getPlayer(playerName);
                        if (player != null) {
                            player.teleport(new Location(player.getWorld(), x, y, z));
                        } else {
                            logger.warning("Player " + playerName + " not found.");
                        }
                        break;
                    case "broadcast":
                        String message = json.optString("message");
                        if (!message.isEmpty()) {
                            broadcastMessage(message);
                        } else {
                            logger.warning("Received empty broadcast message.");
                        }
                        break;
                    case "discordsend":
                        String discordUser = json.optString("user");
                        String discordMessage = json.optString("message");
                        if (!discordUser.isEmpty() && !discordMessage.isEmpty()) {
                            String formattedMessage = String.format("§7[§b§lDiscord§7] §6%s: §f%s", discordUser, discordMessage);
                            discordBroadcastMessage(formattedMessage);
                        } else {
                            logger.warning("Received empty user or message for discordsend action.");
                        }
                        break;
                    default:
                        break;
                }
            } else {
                logger.warning("Command response missing action field.");
            }
        } catch (JSONException e) {}
    }

    private void broadcastMessage(String message) {
        String styledMessage = ChatColor.translateAlternateColorCodes('&', message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.GOLD + "[ALERT]: " + ChatColor.RESET + styledMessage);
        }
    }

    private void discordBroadcastMessage(String message) {
        String styledMessage = ChatColor.translateAlternateColorCodes('&', message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(styledMessage);
        }
    }

    private void executeCommand(String command) {
        Bukkit.getServer().getScheduler().runTask(Bukkit.getPluginManager().getPlugin("AdmiBotDiscordLink"), () -> {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

            // Broadcast to staff members
            logger.info("[AdmiBotLink] An Admin from Discord executed: '" + command + "' as Console");
        });
    }
}
