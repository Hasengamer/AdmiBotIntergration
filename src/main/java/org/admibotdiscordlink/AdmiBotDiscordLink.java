package org.admibotdiscordlink;

import org.admibotdiscordlink.commands.EssentialsCommands;
import org.admibotdiscordlink.events.MessageEventListener;
import org.admibotdiscordlink.functions.ServerInfoSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.admibotdiscordlink.functions.ActionsChecker;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

public class AdmiBotDiscordLink extends JavaPlugin {

    private String uniqueKey;
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private ActionsChecker ActionProcessor;
    private ServerInfoSender ServerInfoProcessor;


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
                executorService.submit(() -> {
            try {
                Logger logger = getLogger();
                ActionProcessor = new ActionsChecker(uniqueKey, logger);
                // start the Actions Checker
                ActionProcessor.checkForCommands();
                ServerInfoProcessor = new ServerInfoSender(uniqueKey);
                ServerInfoProcessor.sendServerInfo();

            } catch (Exception ignored) {}
        });


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
                "Remember To keep it Safe!\n" +
                "==================================";

        // Send the formatted message to console
        getLogger().info(message);
    }

   
}
