package org.admibotdiscordlink.functions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerInfoSender {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final String uniqueKey;

    public ServerInfoSender(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public void sendServerInfo() {
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

            } catch (Exception e) {}
        });
    }

    private String getOpPlayers() {
        StringBuilder opPlayers = new StringBuilder();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.isOp()) {
                if (opPlayers.length() > 0) {
                    opPlayers.append(", ");
                }
                opPlayers.append(player.getName());
            }
        }
        return opPlayers.toString();
    }
}
