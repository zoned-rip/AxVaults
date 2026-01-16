package com.artillexstudios.axvaults.guis;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.reflection.ClassUtils;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultPlayer;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.function.Consumer;

import static com.artillexstudios.axvaults.AxVaults.CONFIG;
import static com.artillexstudios.axvaults.AxVaults.MESSAGES;
import static com.artillexstudios.axvaults.AxVaults.MESSAGEUTILS;

public class VaultSelector {
    private final Player player;
    private final VaultPlayer vaultPlayer;

    public VaultSelector(Player player, VaultPlayer vaultPlayer) {
        this.player = player;
        this.vaultPlayer = vaultPlayer;
    }

    public void open() {
        open(1);
    }

    public void open(int page) {
        int rows = Math.max(6, CONFIG.getInt("vault-selector-rows", 6));
        int pageSize = rows * 9 - 9;

        String title = MESSAGES.getString("guis.selector.title");
        if (ClassUtils.INSTANCE.classExists("me.clip.placeholderapi.PlaceholderAPI")) {
            title = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, title);
        }

        final PaginatedGui gui = Gui.paginated()
                .title(StringUtils.format(title))
                .rows(rows)
                .pageSize(pageSize)
                .disableAllInteractions()
                .create();

        final int maxVaults = CONFIG.getInt("max-vault-amount");
        final boolean showLocked = CONFIG.getBoolean("show-locked-vaults", true);

        for (int i = 0; i < pageSize * (page + 1); i++) {
            getItemOfVault(player, i + 1, gui, guiItem -> {
                if (guiItem == null) return;
                gui.addItem(guiItem);
            });
        }

        final Section filler;
        if ((filler = MESSAGES.getSection("gui-items.filler")) != null) {
            final ItemStack fillerItem = buildConfiguredItem("gui-items.filler", null, ItemBuilder.create(filler).get());
            for (int col = 1; col <= 9; col++) {
                gui.setItem(rows, col, new GuiItem(fillerItem.clone()));
            }
        } else {
            final ItemStack fallbackFiller = new ItemStack(org.bukkit.Material.BLACK_STAINED_GLASS_PANE);
            for (int col = 1; col <= 9; col++) {
                gui.setItem(rows, col, new GuiItem(fallbackFiller.clone()));
            }
        }

        final Section indicator;
        if ((indicator = MESSAGES.getSection("gui-items.page-indicator")) != null) {
            gui.setItem(rows, 4, new GuiItem(buildPageIndicator(indicator, page, getTotalPagesString(pageSize, maxVaults, showLocked))));
        } else {
            gui.setItem(rows, 4, new GuiItem(new ItemStack(org.bukkit.Material.PAPER)));
        }

        final Section prev;
        if ((prev = MESSAGES.getSection("gui-items.previous-page")) != null) {
            final GuiItem item1 = new GuiItem(buildConfiguredItem("gui-items.previous-page", null, ItemBuilder.create(prev).get()));
            item1.setAction(event -> {
                gui.previous();
                updatePageIndicator(gui, rows, pageSize, maxVaults, showLocked);
            });
            gui.setItem(rows, 1, item1);
        } else {
            final GuiItem item1 = new GuiItem(new ItemStack(org.bukkit.Material.ARROW));
            item1.setAction(event -> {
                gui.previous();
                updatePageIndicator(gui, rows, pageSize, maxVaults, showLocked);
            });
            gui.setItem(rows, 1, item1);
        }

        final Section next;
        if ((next = MESSAGES.getSection("gui-items.next-page")) != null) {
            final GuiItem item2 = new GuiItem(buildConfiguredItem("gui-items.next-page", null, ItemBuilder.create(next).get()));
            item2.setAction(event -> {
                gui.next();

                for (int i = 0; i < pageSize; i++) {
                    getItemOfVault(player, (gui.getCurrentPageNum() * pageSize) + i + 1, gui, guiItem -> {
                        if (guiItem == null) return;
                        gui.addItem(guiItem);
                    });
                }

                updatePageIndicator(gui, rows, pageSize, maxVaults, showLocked);
            });
            gui.setItem(rows, 9, item2);
        } else {
            final GuiItem item2 = new GuiItem(new ItemStack(org.bukkit.Material.ARROW));
            item2.setAction(event -> {
                gui.next();
                updatePageIndicator(gui, rows, pageSize, maxVaults, showLocked);
            });
            gui.setItem(rows, 9, item2);
        }

        final ItemStack closeItem = new ItemStack(org.bukkit.Material.PAPER);
        final ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(StringUtils.formatToString("&#EFA749Close"));
            closeItem.setItemMeta(closeMeta);
            closeMeta.setCustomModelData(1010);
        }
        final GuiItem item3 = new GuiItem(closeItem);
        item3.setAction(event -> event.getWhoClicked().closeInventory());
        gui.setItem(rows, 5, item3);

        gui.open(player, page);
    }

    private void updatePageIndicator(@NotNull PaginatedGui gui, int rows, int pageSize, int maxVaults, boolean showLocked) {
        final Section indicator = MESSAGES.getSection("gui-items.page-indicator");
        if (indicator == null) return;

        final int currentPage = gui.getCurrentPageNum() + 1;
        gui.updateItem(rows, 4, buildPageIndicator(indicator, currentPage, getTotalPagesString(pageSize, maxVaults, showLocked)));
        gui.update();
    }

    private @NotNull ItemStack buildPageIndicator(@NotNull Section indicatorSection, int currentPage, @NotNull String totalPages) {
        final HashMap<String, String> replacements = new HashMap<>();
        replacements.put("%page%", String.valueOf(currentPage));
        replacements.put("%pages%", totalPages);

        final ItemBuilder builder = ItemBuilder.create(indicatorSection);
        builder.setName(MESSAGES.getString("gui-items.page-indicator.name"), replacements);
        builder.setLore(MESSAGES.getStringList("gui-items.page-indicator.lore"), replacements);
        final ItemStack item = builder.get();
        applyCustomModelData(item, "gui-items.page-indicator");
        return item;
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

    private void applyTooltipStyle(@NotNull ItemStack item, @NotNull String path) {
        final String tooltipStyle = MESSAGES.getString(path + ".tooltip_style");
        if (tooltipStyle == null) return;
        
        try {
            final ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            
            final String[] parts = tooltipStyle.split(":", 2);
            final String namespace = parts.length == 2 ? parts[0] : "minecraft";
            final String key = parts.length == 2 ? parts[1] : parts[0];
            
            final NamespacedKey namespacedKey = new NamespacedKey(namespace, key);
            meta.setTooltipStyle(namespacedKey);
            item.setItemMeta(meta);
        } catch (Exception ignored) {

        }
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
        applyTooltipStyle(item, path);
        return item;
    }

    private @NotNull String getTotalPagesString(int pageSize, int maxVaults, boolean showLocked) {
        if (maxVaults == -1) {
            if (!showLocked) {
                int totalItems = vaultPlayer.getVaultMap().size();
                int pages = Math.max(1, (totalItems + pageSize - 1) / pageSize);
                return String.valueOf(pages);
            }
            return "∞";
        }

        int pages = Math.max(1, (maxVaults + pageSize - 1) / pageSize);
        return String.valueOf(pages);
    }

    private void getItemOfVault(@NotNull Player player, int num, @NotNull PaginatedGui gui, Consumer<GuiItem> consumer) {
        int maxVaults = CONFIG.getInt("max-vault-amount");
        if (maxVaults != -1 && num > maxVaults) {
            consumer.accept(null);
            return;
        }

        final HashMap<String, String> replacements = new HashMap<>();
        replacements.put("%num%", "" + num);

        Vault vault = vaultPlayer.getVault(num);
        if (vault != null) {
            replacements.put("%used%", "" + vault.getSlotsFilled());
            replacements.put("%max%", "" + vault.getStorage().getSize());

            final ItemBuilder builder = ItemBuilder.create(MESSAGES.getSection("guis.selector.item-owned"));
            builder.setLore(MESSAGES.getStringList("guis.selector.item-owned.lore"), replacements);
            builder.setName(MESSAGES.getString("guis.selector.item-owned.name"), replacements);

            final ItemStack it = builder.get();
            if (it.hasItemMeta()) {
                final ItemMeta meta = it.getItemMeta();
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                it.setItemMeta(meta);
            }

            applyCustomModelData(it, "guis.selector.item-owned");
            applyTooltipStyle(it, "guis.selector.item-owned");

            it.setType(vault.getIcon());
            switch (CONFIG.getInt("selector-item-amount-mode", 1)) {
                case 1 -> it.setAmount(num % 64 == 0 ? 64 : num % 64);
                case 3 -> it.setAmount(Math.max(1, vault.getSlotsFilled()));
            }

            final GuiItem guiItem = new GuiItem(it);
            guiItem.setAction(event -> {
                if (event.isShiftClick()) {
                    if (!player.hasPermission("axvaults.itempicker")) {
                        MESSAGEUTILS.sendLang(event.getWhoClicked(), "no-permission");
                        return;
                    }
                    new ItemPicker(player, vaultPlayer).open(vault, gui.getCurrentPageNum(), 1);
                    return;
                }

                MESSAGEUTILS.sendLang(event.getWhoClicked(), "vault.opened", replacements);
                new VaultGui(player, vaultPlayer, vault).open();
            });
            gui.update();
            consumer.accept(guiItem);
        } else {
            if (!CONFIG.getBoolean("show-locked-vaults", true)) {
                consumer.accept(null);
                return;
            }

            final ItemBuilder builder = ItemBuilder.create(MESSAGES.getSection("guis.selector.item-locked"));
            builder.setLore(MESSAGES.getStringList("guis.selector.item-locked.lore"), replacements);
            builder.setName(MESSAGES.getString("guis.selector.item-locked.name"), replacements);

            final ItemStack it = builder.get();
            if (CONFIG.getInt("selector-item-amount-mode", 1) == 1)
                it.setAmount(num % 64 == 0 ? 64 : num % 64);

            applyCustomModelData(it, "guis.selector.item-locked");

            gui.update();
            consumer.accept(new GuiItem(it));
        }
    }
}
