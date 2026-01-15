package com.artillexstudios.axvaults.guis;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.reflection.ClassUtils;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.hooks.HookManager;
import com.artillexstudios.axvaults.utils.SoundUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.artillexstudios.axvaults.AxVaults.CONFIG;
import static com.artillexstudios.axvaults.AxVaults.MESSAGES;

/**
 * A GUI wrapper for Vault storage that adds navigation controls to switch between vaults.
 */
public class VaultGui implements InventoryHolder {
    private static final int ROWS = 6;
    private static final int STORAGE_ROWS = 5;
    private static final int STORAGE_SLOTS = STORAGE_ROWS * 9; 
    private static final int NAV_ROW_START = STORAGE_SLOTS; 

    private static final Map<UUID, VaultGui> activeGuis = new ConcurrentHashMap<>();

    private final Player player;
    private final VaultPlayer vaultPlayer;
    private Vault vault;
    private Inventory displayInventory;

    public VaultGui(Player player, VaultPlayer vaultPlayer, Vault vault) {
        this.player = player;
        this.vaultPlayer = vaultPlayer;
        this.vault = vault;
    }

    public static VaultGui getActiveGui(UUID playerId) {
        return activeGuis.get(playerId);
    }

    public static void removeActiveGui(UUID playerId) {
        activeGuis.remove(playerId);
    }

    public void open() {
        String title = MESSAGES.getString("guis.vault.title").replace("%num%", "" + vault.getId());
        title = HookManager.getPlaceholderParser().setPlaceholders(Bukkit.getOfflinePlayer(vaultPlayer.getUUID()), title);
        if (ClassUtils.INSTANCE.classExists("me.clip.placeholderapi.PlaceholderAPI")) {
            title = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, title);
        }

        displayInventory = Bukkit.createInventory(this, ROWS * 9, StringUtils.formatToString(title));

        loadVaultItems();
        setupNavigationRow();
        activeGuis.put(player.getUniqueId(), this);
        SoundUtils.playSound(player, MESSAGES.getString("sounds.open"));
        vault.hasChanged().set(true);

        player.openInventory(displayInventory);
    }

    private void loadVaultItems() {
        ItemStack[] vaultContents = vault.getStorage().getContents();
        int maxSlots = Math.min(STORAGE_SLOTS, vaultContents.length);

        for (int i = 0; i < maxSlots; i++) {
            displayInventory.setItem(i, vaultContents[i]);
        }
    }

    public void saveVaultItems() {
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            if (i < vault.getStorage().getSize()) {
                vault.getStorage().setItem(i, displayInventory.getItem(i));
            }
        }
        vault.hasChanged().set(true);
    }

    private void setupNavigationRow() {
        final Section filler = MESSAGES.getSection("gui-items.filler");
        ItemStack fillerItem;
        if (filler != null) {
            fillerItem = buildConfiguredItem("gui-items.filler", null, ItemBuilder.create(filler).get());
        } else {
            fillerItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = fillerItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                fillerItem.setItemMeta(meta);
            }
        }
        for (int i = NAV_ROW_START; i < NAV_ROW_START + 9; i++) {
            displayInventory.setItem(i, fillerItem.clone());
        }

        final Section prev = MESSAGES.getSection("gui-items.previous-page");
        ItemStack prevItem;
        if (prev != null) {
            prevItem = buildConfiguredItem("gui-items.previous-page", null, ItemBuilder.create(prev).get());
        } else {
            prevItem = new ItemStack(Material.ARROW);
            ItemMeta meta = prevItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§ePrevious Vault");
                prevItem.setItemMeta(meta);
            }
        }
        displayInventory.setItem(NAV_ROW_START, prevItem);

        displayInventory.setItem(NAV_ROW_START + 3, buildVaultIndicator());

        final Section back = MESSAGES.getSection("gui-items.back");
        ItemStack backItem;
        if (back != null) {
            backItem = buildConfiguredItem("gui-items.back", null, ItemBuilder.create(back).get());
        } else {
            backItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = backItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§cBack");
                backItem.setItemMeta(meta);
            }
        }
        displayInventory.setItem(NAV_ROW_START + 4, backItem);

        final Section next = MESSAGES.getSection("gui-items.next-page");
        ItemStack nextItem;
        if (next != null) {
            nextItem = buildConfiguredItem("gui-items.next-page", null, ItemBuilder.create(next).get());
        } else {
            nextItem = new ItemStack(Material.ARROW);
            ItemMeta meta = nextItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eNext Vault");
                nextItem.setItemMeta(meta);
            }
        }
        displayInventory.setItem(NAV_ROW_START + 8, nextItem);
    }

    private ItemStack buildVaultIndicator() {
        final Section indicator = MESSAGES.getSection("gui-items.page-indicator");
        int maxVaults = CONFIG.getInt("max-vault-amount", -1);
        String totalVaults = maxVaults == -1 ? "∞" : String.valueOf(maxVaults);

        if (indicator != null) {
            final HashMap<String, String> replacements = new HashMap<>();
            replacements.put("%page%", String.valueOf(vault.getId()));
            replacements.put("%pages%", totalVaults);

            final ItemBuilder builder = ItemBuilder.create(indicator);
            builder.setName(MESSAGES.getString("gui-items.page-indicator.name"), replacements);
            builder.setLore(MESSAGES.getStringList("gui-items.page-indicator.lore"), replacements);
            final ItemStack item = builder.get();
            applyCustomModelData(item, "gui-items.page-indicator");
            return item;
        } else {
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§7Vault " + vault.getId() + "/" + totalVaults);
                paper.setItemMeta(meta);
            }
            return paper;
        }
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot >= ROWS * 9) {
            return;
        }
        if (slot < NAV_ROW_START) {
            vault.hasChanged().set(true);
            return;
        }

        event.setCancelled(true);

        // Previous vault (slot 45)
        if (slot == NAV_ROW_START) {
            int prevVaultId = vault.getId() - 1;
            if (prevVaultId >= 1) {
                Vault prevVault = vaultPlayer.getVault(prevVaultId);
                if (prevVault != null) {
                    switchToVault(prevVault);
                }
            }
            return;
        }

        // Back button (slot 49)
        if (slot == NAV_ROW_START + 4) {
            saveVaultItems();
            new VaultSelector(player, vaultPlayer).open();
            return;
        }

        // Next vault (slot 53)
        if (slot == NAV_ROW_START + 8) {
            int nextVaultId = vault.getId() + 1;
            int maxVaults = CONFIG.getInt("max-vault-amount", -1);
            if (maxVaults == -1 || nextVaultId <= maxVaults) {
                Vault nextVault = vaultPlayer.getVault(nextVaultId);
                if (nextVault != null) {
                    switchToVault(nextVault);
                }
            }
        }
    }

    private void switchToVault(Vault newVault) {
        saveVaultItems();
        this.vault = newVault;
        activeGuis.remove(player.getUniqueId());
        open();
    }

    public void onClose() {
        saveVaultItems();
        activeGuis.remove(player.getUniqueId());
    }

    public Vault getVault() {
        return vault;
    }

    private void applyCustomModelData(@NotNull ItemStack item, @NotNull String path) {
        final String raw = MESSAGES.getString(path + ".custom-model-data");
        if (raw == null) return;
        final int cmd;
        try {
            cmd = Integer.parseInt(raw);
        } catch (Exception ignored) {
            return;
        }
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setCustomModelData(cmd);
        item.setItemMeta(meta);
    }

    private @NotNull ItemStack buildConfiguredItem(@NotNull String path, HashMap<String, String> replacements, @NotNull ItemStack fallback) {
        final Section section = MESSAGES.getSection(path);
        if (section == null) return fallback;

        final ItemBuilder builder = ItemBuilder.create(section);
        if (replacements != null) {
            final String name = MESSAGES.getString(path + ".name");
            if (name != null) builder.setName(name, replacements);
            builder.setLore(MESSAGES.getStringList(path + ".lore"), replacements);
        }
        final ItemStack item = builder.get();
        applyCustomModelData(item, path);
        return item;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return displayInventory;
    }
}
