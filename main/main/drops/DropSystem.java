package dev.main.drops;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Main drop system that generates loot from enemies
 */
public class DropSystem {
    private final DropTable dropTable;
    private static final double LUCKY_DROP_CHANCE = 0.05; // 5% chance for lucky drop
    
    public DropSystem() {
        this.dropTable = new DropTable();
    }
    
    public DropSystem(DropTable customDropTable) {
        this.dropTable = customDropTable;
    }
    
    /**
     * Generate drops from a defeated enemy
     * @param maxDropCapacity Maximum number of different items that can drop
     * @return List of dropped items
     */
    public List<DroppedItem> generateDrops(int maxDropCapacity) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean isLuckyDrop = random.nextDouble() < LUCKY_DROP_CHANCE;
        
        List<DroppedItem> drops = new ArrayList<>();
        
        if (isLuckyDrop) {
            // Lucky drop: give all possible drops at maximum quantities
            drops.addAll(generateLuckyDrops());
        } else {
            // Regular drop: random items less than max capacity
            int numDrops = random.nextInt(1, maxDropCapacity);
            drops.addAll(generateRegularDrops(numDrops));
        }
        
        return drops;
    }
    
    /**
     * Check if the last drop was a lucky drop (for UI display)
     */
    public boolean isLuckyDrop(List<DroppedItem> drops) {
        // If drops contain items from all rarity tiers (except mythic), it's likely lucky
        Set<DropRarity> rarities = new HashSet<>();
        for (DroppedItem drop : drops) {
            rarities.add(drop.getRarity());
        }
        return rarities.size() >= 4; // Has Common, Rare, Epic, Legendary
    }
    
    /**
     * Generate lucky drops - all possible items with maximum quantities
     */
    private List<DroppedItem> generateLuckyDrops() {
        List<DroppedItem> drops = new ArrayList<>();
        
        for (DropRarity rarity : DropRarity.values()) {
            if (rarity == DropRarity.MYTHIC) continue; // Skip mythic for now
            
            List<DropItem> items = dropTable.getItemsForRarity(rarity);
            for (DropItem item : items) {
                drops.add(new DroppedItem(item, item.getMaxQuantity()));
            }
        }
        
        return drops;
    }
    
    /**
     * Generate regular drops with random selection
     */
    private List<DroppedItem> generateRegularDrops(int numDrops) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<DroppedItem> drops = new ArrayList<>();
        Set<DropItem> selectedItems = new HashSet<>();
        
        for (int i = 0; i < numDrops; i++) {
            DropRarity selectedRarity = rollRarity();
            List<DropItem> availableItems = dropTable.getItemsForRarity(selectedRarity);
            
            if (availableItems.isEmpty()) {
                i--; // Try again if no items available
                continue;
            }
            
            // Select a random item from the rarity tier
            DropItem selectedItem = availableItems.get(random.nextInt(availableItems.size()));
            
            // Avoid duplicate items in the same drop
            if (selectedItems.contains(selectedItem)) {
                i--;
                continue;
            }
            
            selectedItems.add(selectedItem);
            
            // Random quantity between min and max (exclusive of max for regular drops)
            int quantity;
            if (selectedItem.getMaxQuantity() > selectedItem.getMinQuantity()) {
                quantity = random.nextInt(selectedItem.getMinQuantity(), selectedItem.getMaxQuantity());
            } else {
                quantity = selectedItem.getMinQuantity();
            }
            
            drops.add(new DroppedItem(selectedItem, quantity));
        }
        
        return drops;
    }
    
    /**
     * Roll for item rarity based on weighted probabilities
     */
    private DropRarity rollRarity() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double roll = random.nextDouble();
        double cumulative = 0.0;
        
        // Roll through rarities from most common to rarest
        for (DropRarity rarity : DropRarity.values()) {
            if (rarity == DropRarity.MYTHIC) continue; // Skip mythic for now
            cumulative += rarity.getDropChance();
            if (roll <= cumulative) {
                return rarity;
            }
        }
        
        return DropRarity.COMMON; // Fallback
    }
    
    /**
     * Get the drop table (useful for inspection or modification)
     */
    public DropTable getDropTable() {
        return dropTable;
    }
}