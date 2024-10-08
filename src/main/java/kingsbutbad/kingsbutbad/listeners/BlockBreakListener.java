package kingsbutbad.kingsbutbad.listeners;

import kingsbutbad.kingsbutbad.KingsButBad;
import kingsbutbad.kingsbutbad.keys.Keys;
import kingsbutbad.kingsbutbad.utils.CreateText;
import kingsbutbad.kingsbutbad.utils.Role;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class BlockBreakListener implements Listener {
   @EventHandler
   @SuppressWarnings("deprecation")
   public void onBlockBreakEvent(BlockBreakEvent event) {
      if (event.getBlock().getType().equals(Material.DEEPSLATE_COAL_ORE)) {
         event.setDropItems(false);
         event.setCancelled(true);
         KingsButBad.prisonQuota.put(event.getPlayer(), KingsButBad.prisonQuota.get(event.getPlayer()) - 1);
         event.getBlock().setType(Material.DEEPSLATE);
         if(KingsButBad.roles.get(event.getPlayer()).equals(Role.PRISONER)){
            KingsButBad.prisonTimer.put(event.getPlayer(), KingsButBad.prisonTimer.getOrDefault(event.getPlayer(), 0) - 10);
            event.getPlayer().sendTitle("", CreateText.addColors("<gray>-10s"), 0, 3,0);
            event.getPlayer().getInventory().addItem(new ItemStack(Material.COAL));
         }
         Bukkit.getScheduler()
            .runTaskLater(
               KingsButBad.getPlugin(KingsButBad.class),
               () -> {
                  event.getBlock().setType(Material.DEEPSLATE_COAL_ORE);
                  if (KingsButBad.coalCompactor) {
                     if(Keys.showMineMessages.get(KingsButBad.king, false))
                        KingsButBad.king.sendMessage(CreateText.addColors("<green>+5$ Prisoner mined a block"));
                     if(Keys.showMineMessages.get(KingsButBad.king2, false))
                        KingsButBad.king2.sendMessage(CreateText.addColors("<green>+5$ Prisoner mined a block"));
                     Keys.money.addDouble(KingsButBad.king2, 5.0);
                     Keys.money.addDouble(KingsButBad.king, 5.0);
                  }
               },
               80L
            );
      }

      if (event.getBlock().getType().equals(Material.BROWN_CONCRETE_POWDER)) {
         event.setDropItems(false);
         event.setCancelled(true);
         if (event.getPlayer().hasCooldown(Material.BONE)) {
            return;
         }

         event.getBlock().setType(Material.BEDROCK);
         event.getPlayer().getInventory().addItem(new ItemStack(Material.BROWN_DYE));
         Bukkit.getScheduler().runTaskLater(KingsButBad.getPlugin(KingsButBad.class), () -> event.getBlock().setType(Material.BROWN_CONCRETE_POWDER), 80L);
      }

      if (event.getBlock().getType().equals(Material.COAL_ORE)) {
         event.setDropItems(false);
         event.setCancelled(true);
         if (event.getPlayer().hasCooldown(Material.STONE_PICKAXE)) {
            return;
         }

         event.getBlock().setType(Material.CHISELED_STONE_BRICKS);
         event.getPlayer().getInventory().addItem(new ItemStack(Material.COAL_ORE));
         Bukkit.getScheduler().runTaskLater(KingsButBad.getPlugin(KingsButBad.class), () -> event.getBlock().setType(Material.COAL_ORE), 80L);
      }

      if (event.getBlock().getType().equals(Material.WHEAT_SEEDS) || event.getBlock().getType().equals(Material.WHEAT)) {
         if (event.getPlayer().hasCooldown(Material.WOODEN_HOE)) {
            event.setCancelled(true);
            return;
         }

         event.setDropItems(false);
         event.getPlayer().getInventory().addItem(new ItemStack(Material.WHEAT));
         if (event.getPlayer().getItemInHand().getEnchantments().containsKey(Enchantment.LOOT_BONUS_BLOCKS)) {
            event.getPlayer().getInventory().addItem(new ItemStack(Material.WHEAT, new Random().nextInt(0, 5)));
         }

         Bukkit.getScheduler().runTaskLater(KingsButBad.getPlugin(KingsButBad.class), () -> {
            event.getBlock().setType(Material.WHEAT);
            BlockState seedState = event.getBlock().getState();
            Ageable seed = (Ageable)event.getBlock().getBlockData();
            seed.setAge(7);
            seedState.setBlockData(seed);
            seedState.update();
         }, 80L);
      }
   }
}
