package dev.main.drops;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Main drop system with zone-specific loot support
 */
public class DropSystem {
    private final DropTable dropTable;
    private ZoneLootConfig zoneLootConfig;
    
    private static final double LUCKY_DROP_CHANCE = 0.05; // 5% chance for lucky drop
    
    public DropSystem() {
        this.dropTable = new DropTable();
        this.zoneLootConfig = null; // Set by GameState when loading map
    }
    
    public DropSystem(DropTable customDropTable) {
        this.dropTable = customDropTable;
        this.zoneLootConfig = null;
    }
    
    /**
     * Set zone-specific loot configuration
     */
    public void setZoneLootConfig(ZoneLootConfig config) {
        this.zoneLootConfig = config;
        System.out.println("Zone loot config set: " + 
            (config != null ? config.getLootTier() : "NONE"));
    }
    
    /**
     * Generate drops from a defeated enemy with zone bonuses
     * @param maxDropCapacity Maximum number of different items that can drop
     * @param monsterLevel Level of the monster
     * @param monsterType Type of monster (for guaranteed drops)
     * @param activeQuestId Optional quest ID for guaranteed drops
     * @return List of dropped items
     */
    public List<DroppedItem> generateDrops(int maxDropCapacity, int monsterLevel, 
                                          String monsterType, String activeQuestId) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean isLuckyDrop = random.nextDouble() < LUCKY_DROP_CHANCE;
        
        List<DroppedItem> drops = new ArrayList<>();
        System.out.println(":::::::::inside generateDrops(): zoneLootConfig="+zoneLootConfig+" activeQuestId="+activeQuestId);
        // ★ CHECK FOR GUARANTEED DROPS FIRST
        if (zoneLootConfig != null && activeQuestId != null) {
            ZoneLootConfig.GuaranteedDrop guaranteedDrop = 
                zoneLootConfig.checkGuaranteedDrop(activeQuestId, monsterType);
          //System.out.println(":::::::::guaranteedDrop: "+ guaranteedDrop.questId);
            if (guaranteedDrop != null) {
                drops.add(new DroppedItem(guaranteedDrop.dropItem, guaranteedDrop.quantity));
                System.out.println("✓ GUARANTEED DROP: " + guaranteedDrop.dropItem.getItemName() + 
                                 " x" + guaranteedDrop.quantity + " (Quest: " + activeQuestId + ")");
            }
        }
        
        if (isLuckyDrop) {
            // Lucky drop: give all possible drops at maximum quantities
            drops.addAll(generateLuckyDrops());
        } else {
            // Regular drop: random items less than max capacity
            int numDrops = random.nextInt(1, maxDropCapacity + 1);
            drops.addAll(generateRegularDrops(numDrops, monsterLevel));
        }
        
        // ★ ADD ZONE-SPECIFIC EXTRA DROPS
        if (zoneLootConfig != null) {
            drops.addAll(generateZoneExtraDrops(monsterLevel));
        }
        
        return drops;
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public List<DroppedItem> generateDrops(int maxDropCapacity) {
        return generateDrops(maxDropCapacity, 1, "", null);
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
     * ★ UPDATED: Generate regular drops with zone modifiers
     */
    private List<DroppedItem> generateRegularDrops(int numDrops, int monsterLevel) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<DroppedItem> drops = new ArrayList<>();
        Set<DropItem> selectedItems = new HashSet<>();
        
        for (int i = 0; i < numDrops; i++) {
            DropRarity selectedRarity = rollRarity(monsterLevel);
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
     * ★ NEW: Generate zone-specific extra drops
     */
    private List<DroppedItem> generateZoneExtraDrops(int monsterLevel) {
        List<DroppedItem> drops = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (ZoneLootConfig.ZoneDropItem zoneItem : zoneLootConfig.getExtraDrops()) {
            // Apply zone modifiers to drop chance
            double finalChance = zoneLootConfig.calculateFinalDropChance(
                zoneItem.dropChance, 
                zoneItem.dropItem.getRarity(),
                monsterLevel
            );
            
            if (random.nextDouble() < finalChance) {
                DropItem item = zoneItem.dropItem;
                int quantity = random.nextInt(item.getMinQuantity(), item.getMaxQuantity() + 1);
                drops.add(new DroppedItem(item, quantity));
                
                System.out.println("★ ZONE BONUS: " + item.getItemName() + " x" + quantity);
            }
        }
        
        return drops;
    }
    
    /**
     * ★ UPDATED: Roll for item rarity with zone modifiers
     */
    private DropRarity rollRarity(int monsterLevel) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double roll = random.nextDouble();
        double cumulative = 0.0;
        
        // Roll through rarities from most common to rarest
        for (DropRarity rarity : DropRarity.values()) {
            if (rarity == DropRarity.MYTHIC) continue; // Skip mythic for now
            
            double baseChance = rarity.getDropChance();
            
            // Apply zone multiplier if available
            double finalChance = baseChance;
            if (zoneLootConfig != null) {
                finalChance = zoneLootConfig.calculateFinalDropChance(baseChance, rarity, monsterLevel);
            }
            
            cumulative += finalChance;
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
    
    /**
     * Get zone loot config
     */
    public ZoneLootConfig getZoneLootConfig() {
        return zoneLootConfig;
    }
}