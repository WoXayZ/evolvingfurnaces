package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.tier.EFCostIngredient;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class EFPlayerCosts {

    private EFPlayerCosts() {
    }

    public static boolean canAffordItems(Player player, List<EFCostIngredient> costs) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        for (EFCostIngredient cost : costs) {
            if (countMatching(player, cost) < cost.count()) {
                return false;
            }
        }
        return true;
    }

    public static boolean canAffordXp(Player player, int levels) {
        if (player.getAbilities().instabuild || levels <= 0) {
            return true;
        }
        return player.experienceLevel >= levels;
    }

    public static void pay(Player player, List<EFCostIngredient> costs, int experienceLevels) {
        if (player.getAbilities().instabuild) {
            return;
        }
        for (EFCostIngredient cost : costs) {
            extract(player, cost, cost.count());
        }
        if (experienceLevels > 0) {
            player.giveExperienceLevels(-experienceLevels);
        }
    }

    public static int countMatching(Player player, EFCostIngredient cost) {
        int total = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (cost.matches(stack)) {
                total += stack.getCount();
            }
        }
        ItemStack offhand = inventory.getItem(Inventory.SLOT_OFFHAND);
        if (cost.matches(offhand)) {
            total += offhand.getCount();
        }
        return total;
    }

    private static void extract(Player player, EFCostIngredient cost, int amount) {
        int remaining = amount;
        Inventory inventory = player.getInventory();
        remaining -= extractSlots(inventory, 0, Inventory.INVENTORY_SIZE, cost, remaining);
        if (remaining > 0) {
            extractSlots(inventory, Inventory.SLOT_OFFHAND, Inventory.SLOT_OFFHAND + 1, cost, remaining);
        }
    }

    public static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        player.getInventory().placeItemBackInInventory(stack);
    }

    private static int extractSlots(Inventory inventory, int from, int to, EFCostIngredient cost, int amount) {
        int taken = 0;
        for (int i = from; i < to && taken < amount; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!cost.matches(stack)) {
                continue;
            }
            int remove = Math.min(stack.getCount(), amount - taken);
            stack.shrink(remove);
            taken += remove;
        }
        return taken;
    }
}
