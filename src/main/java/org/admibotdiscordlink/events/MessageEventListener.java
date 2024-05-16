package org.admibotdiscordlink.events;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MessageEventListener implements Listener {

    private final JavaPlugin plugin;

    public MessageEventListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        String message = event.getMessage();

 

        // Retrieve uniqueKey from config.yml
        FileConfiguration config = plugin.getConfig();
        String uniqueKey = config.getString("uniqueKey");

        if (uniqueKey == null || uniqueKey.isEmpty()) {
            plugin.getLogger().warning("Error: uniqueKey is missing in config.yml");
            return;
        }

        // Prepare the JSON payload to send
        String jsonPayload = String.format("{\"uniqueKey\": \"%s\", \"playerName\": \"%s\", \"message\": \"%s\"}",
                uniqueKey, playerName, message);

        // Send the JSON payload to the server asynchronously
        sendPayloadToServer(jsonPayload);
    }

    private void sendPayloadToServer(String jsonPayload) {
        // URL of the server endpoint
        String serverUrl = "http://eu3.diresnode.com:3151/messages";

        // Asynchronous task to send payload to server
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(serverUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setDoOutput(true);

                // Write JSON payload to the server
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Check response code
                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {

                }

                conn.disconnect();
            } catch (Exception e) {

            }
        });
    }
}
