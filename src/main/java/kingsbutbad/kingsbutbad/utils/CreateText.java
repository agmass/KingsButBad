package kingsbutbad.kingsbutbad.utils;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public class CreateText {
   public static String addColors(String a) {
      return LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(ChatColor.stripColor(a)));
   }

   public static String convertAmpersandToMiniMessage(String text) {
      return text.replace("&0", "<black>")
         .replace("&1", "<dark_blue>")
         .replace("&2", "<dark_green>")
         .replace("&3", "<dark_aqua>")
         .replace("&4", "<dark_red>")
         .replace("&5", "<dark_purple>")
         .replace("&6", "<gold>")
         .replace("&7", "<gray>")
         .replace("&8", "<dark_gray>")
         .replace("&9", "<blue>")
         .replace("&a", "<green>")
         .replace("&b", "<aqua>")
         .replace("&c", "<red>")
         .replace("&d", "<light_purple>")
         .replace("&e", "<yellow>")
         .replace("&f", "<white>")
         .replace("&k", "<obfuscated>")
         .replace("&l", "<bold>")
         .replace("&m", "<strikethrough>")
         .replace("&n", "<underline>")
         .replace("&o", "<italic>")
         .replace("&r", "<reset>");
   }
}
