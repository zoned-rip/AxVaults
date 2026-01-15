package com.artillexstudios.axvaults.guis;

import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axapi.reflection.ClassUtils;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.utils.SoundUtils;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultPlayer;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.artillexstudios.axvaults.AxVaults.CONFIG;
import static com.artillexstudios.axvaults.AxVaults.MESSAGES;

public class ItemPicker {
    private final Player player;
    private final VaultPlayer vaultPlayer;

    public ItemPicker(Player player, VaultPlayer vaultPlayer) {
        this.player = player;
        this.vaultPlayer = vaultPlayer;
    }

    public void open(@NotNull Vault vault) {
        open(vault, 1, 1);
    }

    public void open(@NotNull Vault vault, int oldPage, int cPage) {
        int rows = Math.max(6, CONFIG.getInt("item-picker-rows", 6));
        int pageSize = rows * 9 - 9;

        String title = MESSAGES.getString("guis.item-picker.title");
        if (ClassUtils.INSTANCE.classExists("me.clip.placeholderapi.PlaceholderAPI")) {
            title = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, title);
        }

        final PaginatedGui gui = Gui.paginated()
                .title(StringUtils.format(title))
                .rows(rows)
                .pageSize(pageSize)
                .disableAllInteractions()
                .create();

        int itemCount = 0;
        for (Material material : Material.values()) {
            ItemStack it = null;
            try {
                it = ItemBuilder.create(material).glow(Objects.equals(vault.getIcon(), material)).get();
            } catch (Exception ignored) {}
            if (it == null) continue;
            final ItemMeta meta = it.hasItemMeta() ? it.getItemMeta() : Bukkit.getItemFactory().getItemMeta(it.getType());
            if (meta == null) continue;

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(meta);

            final GuiItem guiItem = new GuiItem(it);
            guiItem.setAction(event -> {
                if (vault.getIcon().equals(material)) vault.setIcon(null);
                else vault.setIcon(material);
                SoundUtils.playSound(player, MESSAGES.getString("sounds.select-icon"));
                if (CONFIG.getBoolean("selector-stay-open", true))
                    open(vault, oldPage, gui.getCurrentPageNum());
                else
                    new VaultSelector(player, vaultPlayer).open(oldPage);
            });
            gui.addItem(guiItem);
            itemCount++;
        }

        final int totalPages = Math.max(1, (itemCount + pageSize - 1) / pageSize);

        final Section filler;
        if ((filler = MESSAGES.getSection("gui-items.filler")) != null) {
            final ItemStack fillerItem = buildConfiguredItem("gui-items.filler", null, ItemBuilder.create(filler).get());
            for (int col = 1; col <= 9; col++) {
                gui.setItem(rows, col, new GuiItem(fillerItem.clone()));
            }
        } else {
            final ItemStack fallbackFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            for (int col = 1; col <= 9; col++) {
                gui.setItem(rows, col, new GuiItem(fallbackFiller.clone()));
            }
        }

        final Section indicator;
        if ((indicator = MESSAGES.getSection("gui-items.page-indicator")) != null) {
            gui.setItem(rows, 4, new GuiItem(buildPageIndicator(indicator, cPage, totalPages)));
        } else {
            gui.setItem(rows, 4, new GuiItem(new ItemStack(Material.PAPER)));
        }

        final Section prev;
        if ((prev = MESSAGES.getSection("gui-items.previous-page")) != null) {
            final GuiItem item1 = new GuiItem(buildConfiguredItem("gui-items.previous-page", null, ItemBuilder.create(prev).get()));
            item1.setAction(event -> {
                gui.previous();
                updatePageIndicator(gui, rows, totalPages);
            });
            gui.setItem(rows, 1, item1);
        } else {
            final GuiItem item1 = new GuiItem(new ItemStack(Material.ARROW));
            item1.setAction(event -> {
                gui.previous();
                updatePageIndicator(gui, rows, totalPages);
            });
            gui.setItem(rows, 1, item1);
        }

        final Section next;
        if ((next = MESSAGES.getSection("gui-items.next-page")) != null) {
            final GuiItem item2 = new GuiItem(buildConfiguredItem("gui-items.next-page", null, ItemBuilder.create(next).get()));
            item2.setAction(event -> {
                gui.next();
                updatePageIndicator(gui, rows, totalPages);
            });
            gui.setItem(rows, 9, item2);
        } else {
            final GuiItem item2 = new GuiItem(new ItemStack(Material.ARROW));
            item2.setAction(event -> {
                gui.next();
                updatePageIndicator(gui, rows, totalPages);
            });
            gui.setItem(rows, 9, item2);
        }

        final Section back;
        if ((back = MESSAGES.getSection("gui-items.back")) != null) {
            final GuiItem item3 = new GuiItem(buildConfiguredItem("gui-items.back", null, ItemBuilder.create(back).get()));
            item3.setAction(event -> new VaultSelector(player, vaultPlayer).open(oldPage));
            gui.setItem(rows, 5, item3);
        } else {
            final GuiItem item3 = new GuiItem(new ItemStack(Material.BARRIER));
            item3.setAction(event -> new VaultSelector(player, vaultPlayer).open(oldPage));
            gui.setItem(rows, 5, item3);
        }

        gui.open(player, cPage);
    }

    private void updatePageIndicator(@NotNull PaginatedGui gui, int rows, int totalPages) {
        final Section indicator = MESSAGES.getSection("gui-items.page-indicator");
        if (indicator == null) return;

        final int currentPage = gui.getCurrentPageNum() + 1;
        gui.updateItem(rows, 4, buildPageIndicator(indicator, currentPage, totalPages));
        gui.update();
    }

    private @NotNull ItemStack buildPageIndicator(@NotNull Section indicatorSection, int currentPage, int totalPages) {
        final java.util.HashMap<String, String> replacements = new java.util.HashMap<>();
        replacements.put("%page%", String.valueOf(currentPage));
        replacements.put("%pages%", String.valueOf(totalPages));

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

    private @NotNull ItemStack buildConfiguredItem(@NotNull String path, java.util.HashMap<String, String> replacements, @NotNull ItemStack fallback) {
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
}
