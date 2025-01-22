package it.multicoredev.nbtr.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import it.multicoredev.nbtr.model.Item;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

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
public final class RecipeChoiceAdapter implements JsonDeserializer<RecipeChoice> {

    @Override
    public RecipeChoice deserialize(final @NotNull JsonElement json, final Type type, final JsonDeserializationContext context) throws JsonParseException {
        // Reading as single object.
        if (json.isJsonObject()) {
            if (json.getAsJsonObject().get("material") != null) {
                final Item item = context.deserialize(json, Item.class);
                // Throwing an exception if item validation fails.
                if (!item.isValid())
                    throw new JsonParseException("Required property \"material\" does not exist.");
                // Returning null if material was set to air. 1.20.5+ don't support air in recipes.
                if (item.getMaterial() == Material.AIR)
                    return RecipeChoice.empty();
                // Returning MaterialChoice if metadata is empty, or ExactChoice otherwise.
                try {
                    return (item.geNBT() == null && item.getComponents() == null && !item.toItemStack().hasItemMeta())
                            ? new RecipeChoice.MaterialChoice(item.toItemStack().getType())
                            : new RecipeChoice.ExactChoice(item.toItemStack());
                } catch (final IllegalArgumentException e) {
                    throw new JsonParseException("An error occurred while trying to convert to ItemStack.", e);
                }
            } else if (json.getAsJsonObject().get("tag") != null) {
                String key = json.getAsJsonObject().get("tag").getAsString();
                // Stripping '#' char from the beginning, if present.
                if (key.charAt(0) == '#')
                    key = key.substring(1);
                // Throwing an exception if key is null or NamespacedKey validation fails.
                if (NamespacedKey.fromString(key) == null)
                    throw new JsonParseException("Required property \"tag\" does not represent a valid namespaced key.");
                // Getting tag from the items registry.
                final Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, NamespacedKey.fromString(key), Material.class);
                // Throwing an exception if tag is null.
                if (tag == null) throw new JsonParseException("Required property \"tag\" does not represent a valid material tag.");
                // Returning MaterialChoice if metadata is empty, or ExactChoice otherwise.
                return new RecipeChoice.MaterialChoice(tag);
            }
            // Throwing exception for unexpected input.
            throw new JsonParseException("Expected JsonObject with either the \"material\" or \"tag\" property, but neither was found.");
        }
        // Reading as array of objects.
        else if (json.isJsonArray()) {
            final List<Item> items = context.deserialize(json, TypeToken.getParameterized(List.class, Item.class).getType());
            // Throwing an exception if validation of any item fails.
            if (!items.stream().filter(RecipeChoiceAdapter::isNotAir).allMatch(Item::isValid))
                throw new JsonParseException("Required property \"material\" does not exist on one or more elements.");
            // Returning MaterialChoice if metadata of all items is empty, or ExactChoice otherwise.
            try {
                return (items.stream().filter(RecipeChoiceAdapter::isNotAir).map(Item::toItemStack).noneMatch(ItemStack::hasItemMeta))
                        ? new RecipeChoice.MaterialChoice(items.stream().filter(RecipeChoiceAdapter::isNotAir).map(Item::toItemStack).map(ItemStack::getType).collect(Collectors.toList()))
                        : new RecipeChoice.ExactChoice(items.stream().filter(RecipeChoiceAdapter::isNotAir).map(Item::toItemStack).collect(Collectors.toList()));
            } catch (final IllegalArgumentException e) {
                throw new JsonParseException("An error occurred while trying to convert to ItemStack.", e);
            }
        }
        // Throwing exception for unexpected input.
        throw new JsonParseException("Expected JsonObject or JsonArray but found " + json.getClass().getSimpleName() + ".");
    }

    private static boolean isNotAir(final Item item) {
        return item.getMaterial() != Material.AIR;
    }

}
