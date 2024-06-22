package it.multicoredev.nbtr.model;

import it.multicoredev.mbcore.spigot.Text;
import it.multicoredev.nbtr.utils.ChatFormat;
import it.multicoredev.nbtr.utils.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * BSD 3-Clause License
 * <p>
 * Copyright (c) 2023, Lorenzo Magni
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class Item {
    private final @Nullable Material material;
    private final @Nullable Integer amount;
    private final @Nullable String name;
    private final @Nullable List<String> lore;
    private final @Nullable String nbt;
    private final @Nullable String components;

    public Item(final @Nullable Material material, final @Nullable Integer amount, final @Nullable String name, final @Nullable List<String> lore, final @Nullable String nbt, final @Nullable String components) {
        this.material = material;
        this.amount = amount;
        this.name = name;
        this.lore = lore;
        this.nbt = nbt;
        this.components = components;
    }

    public @Nullable Material getMaterial() {
        return material;
    }

    public int getAmount() {
        if (amount != null) return amount > 0 ? amount : 1;
        return 1;
    }

    public @Nullable String getName() {
        return name;
    }

    public @Nullable List<String> getLore() {
        return lore;
    }

    public @Nullable String geNBT() {
        return nbt;
    }

    @SuppressWarnings("deprecation") // Suppressing @Deprecated warnings. It's Paper that deprecates ChatColor methods and they're called only when running Spigot. It's also Bukkit#getUnsafe which we must use at this point.
    public ItemStack toItemStack() throws IllegalArgumentException {
        final ItemStack item = new ItemStack(material);
        // Setting NBT/Components if specified. This is called first as it can be overridden by named properties in next steps.
        if (Bukkit.getUnsafe().getProtocolVersion() >= 766) {
            if (nbt != null)
                throw new IllegalArgumentException("Versions 1.20.5 and higher must not use \"nbt\" but use \"components\" instead.");
            // On versions that are equal to or higher than 1.20.5, we're using "components" property.
            else if (components != null && !components.trim().isEmpty())
                Bukkit.getUnsafe().modifyItemStack(item, material.key().asString() + components);
        // On versions lower than 1.20.5, we're using "nbt" property.
        } else if (nbt != null && !nbt.trim().isEmpty()) {
            Bukkit.getUnsafe().modifyItemStack(item, material.key().asString() + nbt);
        }
        // Setting amount if specified and greater than 0.
        if (amount != null && amount > 0)
            item.setAmount(Math.min(material.getMaxStackSize(), amount));
        // Checking whether item has item meta.
        if (item.getItemMeta() != null) {
            final ItemMeta meta = item.getItemMeta();
            // Setting name if specified.
            if (name != null)
                if (VersionUtils.isPaper && ChatFormat.containsMiniMessage(name))
                    meta.displayName(Text.deserialize(name));
                else meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            // Setting lore if specified.
            if (lore != null)
                if (VersionUtils.isPaper && ChatFormat.containsMiniMessage(lore))
                    meta.lore(lore.stream().map(Text::deserialize).toList());
                else meta.setLore(lore.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).toList());
            // Updating item meta.
            item.setItemMeta(meta);
        }
        // Finally, retuning the item.
        return item;
    }

    public boolean isValid() {
        return material != null;
    }
}
