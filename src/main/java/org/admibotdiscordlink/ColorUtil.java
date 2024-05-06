package org.admibotdiscordlink;

import org.bukkit.ChatColor;

public class ColorUtil {

    public static String translateMessage(String message){
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
