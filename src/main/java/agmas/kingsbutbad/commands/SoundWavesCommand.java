package agmas.kingsbutbad.commands;
import agmas.kingsbutbad.KingsButBad;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class SoundWavesCommand implements CommandExecutor {

    // silly little command to see where your proximity chat lands (toggle)
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            if (!KingsButBad.soundwaves.containsKey(p)) {
                p.sendMessage(ChatColor.GREEN + "turned sound waves on lmao");
                KingsButBad.soundwaves.put(p, true);
            } else {
                p.sendMessage(ChatColor.RED + "turned sound waves off lmao");
                KingsButBad.soundwaves.remove(p);
            }
        }
        return true;
    }
}