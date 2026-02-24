package com.artillexstudios.axvaults.listeners;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.database.impl.MySQL;
import com.artillexstudios.axvaults.vaults.VaultManager;
import com.artillexstudios.axvaults.vaults.VaultPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerListeners implements Listener {

    public PlayerListeners() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            VaultManager.loadPlayer(player);
        }
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // In theory this is just second checking if it fails to remove cache after leaving (99% sure
        VaultPlayer existing = VaultManager.getPlayerOrNull(player);
        if (existing != null) {
            VaultManager.getPlayers().remove(player.getUniqueId());
            if (player.isOp()) {
                player.sendMessage("§c[AXVAULTS DEBUG] Old cache found and cleared!");
            }
        }

        VaultManager.loadPlayer(event.getPlayer());


        if (AxVaults.getDatabase() instanceof MySQL db) db.checkForChanges();
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        VaultPlayer vaultPlayer = VaultManager.getPlayerOrNull(event.getPlayer());
        if (vaultPlayer == null) return;
        final String uuid = event.getPlayer().getUniqueId().toString();


        if (AxVaults.getDatabase() instanceof MySQL db) {
            db.tryAcquireSaveLock(uuid);
        }

        AxVaults.getThreadedQueue().submit(() -> {
            if (AxVaults.getDatabase() instanceof MySQL db) {
                try {
                    vaultPlayer.save();
                } finally {
                    db.releaseSaveLock(uuid);
                    db.checkForChanges();
                    // Clear cache
                    VaultManager.getPlayers().remove(event.getPlayer().getUniqueId());
                }
            } else {
                vaultPlayer.save();
                VaultManager.getPlayers().remove(event.getPlayer().getUniqueId());
            }
        });
    }
}
