package agmas.kingsbutbad.CommandsCompleters.Dev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import agmas.kingsbutbad.keys.Keys;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class SetMoneyTabCompleter implements TabCompleter {
   public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 1) {
         for (Player player : sender.getServer().getOnlinePlayers()) {
            if(Keys.vanish.get(player, false)) continue;
            completions.add(player.getName());
         }
      } else if (args.length == 2) {
         completions.addAll(Arrays.asList("inf", "-inf"));
      }

      return completions;
   }
}
