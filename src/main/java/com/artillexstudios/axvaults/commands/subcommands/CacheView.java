package com.artillexstudios.axvaults.commands.subcommands;

import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import com.artillexstudios.axvaults.vaults.VaultPlayer;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public enum CacheView {
    INSTANCE;

    public void execute(CommandSender sender, OfflinePlayer target, Integer slot) {
        VaultPlayer vaultPlayer = VaultManager.getPlayers().get(target.getUniqueId());

        if (vaultPlayer == null) {
            sender.sendMessage("§c✗ Player §e" + target.getName() + " §cis not in cache!");
            return;
        }

        sender.sendMessage("§8§m------------------------------------");
        sender.sendMessage("§a§lCache §7for §e" + target.getName());
        sender.sendMessage("§7Loaded: §f" + vaultPlayer.isLoaded() + " §7| Vaults: §f" + vaultPlayer.getVaultMap().size());
        sender.sendMessage("§8§m------------------------------------");

        for (Vault vault : vaultPlayer.getVaultMap().values()) {
            if (vault.getSlotsFilled() == 0) continue;

            sender.sendMessage("§a Vault #" + vault.getId() +
                    " §8| §7Items: §f" + vault.getSlotsFilled() + "§7/§f" + vault.getStorage().getSize() +
                    " §8| §7Icon: §f" + vault.getRealIcon() +
                    " §8| §7Open: §f" + vault.isOpened() +
                    " §8| §7Changed: §f" + vault.hasChanged());

            if (slot != null && slot == vault.getId()) {
                sender.sendMessage("  §7§m--§r §e§lSlots for Vault #" + vault.getId() + " §7§m--");
                ItemStack[] contents = vault.getStorage().getContents();
                for (int i = 0; i < contents.length; i++) {
                    if (contents[i] == null) continue;
                    sender.sendMessage("  §8[§7" + i + "§8] §f" + contents[i].getType() + " §7x§a" + contents[i].getAmount());
                }
            }
        }

        sender.sendMessage("§8§m------------------------------------");
    }
}