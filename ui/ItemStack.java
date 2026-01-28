package dev.main.ui;

import dev.main.item.Item;

/**
 * ★ NEW: Wrapper to store item + stack count together
 */
public class ItemStack {
    private Item item;
    private int stackCount;
    
    public ItemStack(Item item, int stackCount) {
        this.item = item;
        this.stackCount = Math.max(1, stackCount);
    }
    
    public Item getItem() {
        return item;
    }
    
    public int getStackCount() {
        return stackCount;
    }
    
    public void setStackCount(int count) {
        this.stackCount = Math.max(0, count);
    }
    
    public boolean addToStack(int amount) {
        if (item == null || !item.isStackable()) {
            return false;
        }
        
        int maxStack = item.getMaxStackSize();
        int newCount = stackCount + amount;
        
        if (newCount <= maxStack) {
            stackCount = newCount;
            return true;
        }
        
        return false;
    }
    
    public int removeFromStack(int amount) {
        int removed = Math.min(amount, stackCount);
        stackCount -= removed;
        return removed;
    }
    
    public boolean hasRoom(int amount) {
        if (item == null || !item.isStackable()) {
            return false;
        }
        return (stackCount + amount) <= item.getMaxStackSize();
    }
    
    public int getRemainingSpace() {
        if (item == null || !item.isStackable()) {
            return 0;
        }
        return item.getMaxStackSize() - stackCount;
    }
    
    public boolean canStackWith(Item other) {
        return item != null && item.canStackWith(other);
    }
}