package org.admibotdiscordlink;

import org.admibotdiscordlink.commands.EssentialsCommands;
import org.admibotdiscordlink.events.MessageEventListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;

public class AdmiBotDiscordLink extends JavaPlugin {

    private String uniqueKey;
    private ExecutorService executorService = Executors.newFixedThreadPool(2);


    @Override
    public void onEnable() {

        //Load Commands
        getCommand("checkstatus").setExecutor(new EssentialsCommands());
        getCommand("guide").setExecutor(new EssentialsCommands());

        // Load Events
        getServer().getPluginManager().registerEvents(new MessageEventListener(this), this);


        // The API Part:
        loadUniqueKey();

        // Schedule a repeating task to check for commands and send server info periodically
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForCommandsAndInfo();


            }
        }.runTaskTimer(this, 0L, 20L); // Every second (20 ticks/second)
    }
    @Override
    public void onDisable() {
        executorService.shutdown();
    }


    private void loadUniqueKey() {
        // Load or generate a unique key for this server
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            uniqueKey = UUID.randomUUID().toString();
            getConfig().set("uniqueKey", uniqueKey);
            saveConfig();
            sendServerKeyMessage();
        } else {
            uniqueKey = getConfig().getString("uniqueKey");
            sendServerKeyMessage();
        }

    }
    private String generateUniqueKey() {
        // Generate a new unique key (e.g., using UUID)
        return java.util.UUID.randomUUID().toString();
    }
    private void sendServerKeyMessage() {
        // Format the message with the server key
        String message = "\n==================================\n" +
                "Your Server Key is:\n" +
                uniqueKey + "\n" +
                "==================================";

        // Send the formatted message to console
        getLogger().info(message);
    }

    private void checkForCommandsAndInfo() {
        executorService.submit(() -> {
            try {
                checkForCommands();
                sendServerInfo();
            } catch (Exception e) {

            }
        });
    }

    private void checkForCommands() {
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

        } catch (Exception e) {

        }
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
                            getLogger().warning("Received empty command value.");
                        }
                        break;
                    case "shutdown":
                        getLogger().info("Received shutdown command. Shutting down server...");
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
                            getLogger().warning("Player " + playerName + " not found.");
                        }
                        break;
                    case "broadcast":
                        String message = json.optString("message");
                        if (!message.isEmpty()) {
                            broadcastMessage(message);
                        } else {
                            getLogger().warning("Received empty broadcast message.");
                        }
                        break;
                    case "discordsend":
                        String discordUser = json.optString("user");
                        String discordMessage = json.optString("message");
                        if (!discordUser.isEmpty() && !discordMessage.isEmpty()) {
                            String formattedMessage = String.format("§7[§b§lDiscord§7] §6%s: §f%s", discordUser, discordMessage);
                            discordbroadcastMessage(formattedMessage);
                        } else {
                            getLogger().warning("Received empty user or message for discordsend action.");
                        }
                        break;
                    default:
                        break;
                }
            } else {
                getLogger().warning("Command response missing action field.");
            }
        } catch (JSONException e) {
            getLogger().log(Level.WARNING, "Error parsing command response: " + e.getMessage());
        }
    }


    private void broadcastMessage(String message) {
        String styledMessage = ChatColor.translateAlternateColorCodes('&', message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.GOLD + "[ALERT]: " + ChatColor.RESET + styledMessage);
        }
    }
    private void discordbroadcastMessage(String message) {
        String styledMessage = ChatColor.translateAlternateColorCodes('&', message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(styledMessage);
        }
    }

    private void executeCommand(String command) {
        getServer().getScheduler().runTask(this, () -> {
            getServer().dispatchCommand(getServer().getConsoleSender(), command);

            // Broadcast to staff members
            getServer().broadcast("&7[&b&lAdmiBotLink&7] &7An Admin from Discord executed: " + command + " as Console", "staff.permission");

        });
    }


    private void sendServerInfo() {
        executorService.submit(() -> {

        try {
            JSONObject serverInfo = new JSONObject();
            serverInfo.put("uniqueKey", uniqueKey);
            serverInfo.put("status", "Online");
            serverInfo.put("playersOnline", Bukkit.getServer().getOnlinePlayers().size());
            serverInfo.put("maxPlayers", Bukkit.getServer().getMaxPlayers());
            serverInfo.put("opPlayers", getOpPlayers());

            // Collect names of online players
            JSONArray onlinePlayerNames = new JSONArray();
            for (Player player : Bukkit.getOnlinePlayers()) {
                onlinePlayerNames.put(player.getName());
            }
            serverInfo.put("OnlinePlayersNames", onlinePlayerNames);

            // Additional server info can be added here

            URL url = new URL("http://eu3.diresnode.com:3151/inforecieve");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            con.getOutputStream().write(serverInfo.toString().getBytes());

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

        } catch (Exception e) {

        }
        });
    }

    private String getOpPlayers() {
        StringBuilder opPlayers = new StringBuilder();
        for (String playerName : Bukkit.getServer().getOperators().stream().map(Object::toString).toArray(String[]::new)) {
            opPlayers.append(playerName).append(", ");
        }
        return opPlayers.toString();
    }
}
