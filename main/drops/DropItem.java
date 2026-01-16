package dev.main.drops;

import dev.main.item.Item;

/**
 * Template for items that can be dropped
 */
public class DropItem {
    private final String itemName;
    private final DropRarity rarity;
    private final int minQuantity;
    private final int maxQuantity;
    private final ItemCreator itemCreator;
    
    /**
     * Functional interface for creating items
     */
    @FunctionalInterface
    public interface ItemCreator {
        Item create();
    }
    
    public DropItem(String itemName, DropRarity rarity, int minQuantity, int maxQuantity, ItemCreator itemCreator) {
        this.itemName = itemName;
        this.rarity = rarity;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.itemCreator = itemCreator;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public DropRarity getRarity() {
        return rarity;
    }
    
    public int getMinQuantity() {
        return minQuantity;
    }
    
    public int getMaxQuantity() {
        return maxQuantity;
    }
    
    /**
     * Create an actual Item instance
     */
    public Item createItem() {
        return itemCreator.create();
    }
    
    @Override
    public String toString() {
        return itemName + " [" + rarity + "] (" + minQuantity + "-" + maxQuantity + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DropItem other = (DropItem) obj;
        return itemName.equals(other.itemName);
    }
    
    @Override
    public int hashCode() {
        return itemName.hashCode();
    }
}