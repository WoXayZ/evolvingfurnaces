package com.evolvingfurnaces.machine;

import net.minecraft.world.item.ItemStack;

public final class EFStacks {

    private EFStacks() {
    }

    public static ItemStack copyWithCount(ItemStack stack, int n) {
        ItemStack copy = stack.copy();
        copy.setCount(n);
        return copy;
    }
}
