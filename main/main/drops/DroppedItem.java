package dev.main.drops;

import dev.main.item.Item;

/**
 * Represents an actual dropped item with quantity
 */
public class DroppedItem {
    private final DropItem dropTemplate;
    private final int quantity;
    
    public DroppedItem(DropItem dropTemplate, int quantity) {
        this.dropTemplate = dropTemplate;
        this.quantity = quantity;
    }
    
    public DropItem getDropTemplate() {
        return dropTemplate;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public String getItemName() {
        return dropTemplate.getItemName();
    }
    
    public DropRarity getRarity() {
        return dropTemplate.getRarity();
    }
    
    /**
     * Create the actual Item instances for the player's inventory
     */
    public Item[] createItems() {
        Item[] items = new Item[quantity];
        for (int i = 0; i < quantity; i++) {
            items[i] = dropTemplate.createItem();
        }
        return items;
    }
    
    @Override
    public String toString() {
        return quantity + "x " + dropTemplate.getItemName() + " [" + dropTemplate.getRarity() + "]";
    }
}