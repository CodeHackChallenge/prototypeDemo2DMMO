package dev.main.drops;

import java.util.*;

import dev.main.item.ItemManager;

/**
 * Configuration for all available drops organized by rarity
 */
public class DropTable {
    private final Map<DropRarity, List<DropItem>> itemsByRarity;
    
    public DropTable() {
        itemsByRarity = new EnumMap<>(DropRarity.class);
        initializeDrops();
    }
    
    private void initializeDrops() {
        // Common items - materials that enemies drop
        List<DropItem> commonItems = new ArrayList<>();
        
        commonItems.add(new DropItem("Fruit Banana", DropRarity.COMMON, 1, 2, 
                ItemManager::createFruitBanana)); //make this a forest drop
        
        commonItems.add(new DropItem("Animal Skull", DropRarity.COMMON, 1, 2, 
            ItemManager::createAnimalSkull));
        commonItems.add(new DropItem("Raw Fish", DropRarity.COMMON, 1, 2, 
            ItemManager::createRawFish));
        commonItems.add(new DropItem("Broken Tooth", DropRarity.COMMON, 1, 2, 
            ItemManager::createBrokenTooth));
        commonItems.add(new DropItem("Animal Claws", DropRarity.COMMON, 1, 2, 
            ItemManager::createAnimalClaws));
        commonItems.add(new DropItem("Essence", DropRarity.COMMON, 1, 4, 
            ItemManager::createEssence));
                
        itemsByRarity.put(DropRarity.COMMON, commonItems);
        
        //uncommon 
        //List<DropItem> uncommonItems = new ArrayList<>();
        //uncommonItems.add(new DropItem("Lucky Pouch", DropRarity.UNCOMMON, 1, 1, 
             //   ItemManager::createLuckyPouch)); //make this map specific
        //itemsByRarity.put(DropRarity.UNCOMMON, uncommonItems);
        
        // Rare items - scrolls and special materials
        List<DropItem> rareItems = new ArrayList<>();
        // Note: Using Carved Wood as "Scroll" equivalent since your ItemManager has it
        rareItems.add(new DropItem("Verdant Shards", DropRarity.RARE, 1, 2, 
            ItemManager::createVerdantShard));
        itemsByRarity.put(DropRarity.RARE, rareItems);
        
        // Epic items - rare materials
        List<DropItem> epicItems = new ArrayList<>();
        epicItems.add(new DropItem("Scroll of Purity", DropRarity.EPIC, 1, 2, 
            ItemManager::createScrollOfPurity));
        //epicItems.add(new DropItem("Rune of Return", DropRarity.EPIC, 1, 1, 
         //   ItemManager::createRuneOfReturn));
        itemsByRarity.put(DropRarity.EPIC, epicItems);
        
        // Legendary items - powerful runes
        List<DropItem> legendaryItems = new ArrayList<>();
        legendaryItems.add(new DropItem("Fire Rune", DropRarity.EPIC, 1, 1, 
            ItemManager::createFireRune));
        itemsByRarity.put(DropRarity.EPIC, legendaryItems);
        
        // Mythic items (to be implemented later)
        itemsByRarity.put(DropRarity.MYTHIC, new ArrayList<>());
    }
    
    /**
     * Get all items for a specific rarity tier
     */
    public List<DropItem> getItemsForRarity(DropRarity rarity) {
        return itemsByRarity.getOrDefault(rarity, Collections.emptyList());
    }
    
    /**
     * Get all available items across all rarities
     */
    public List<DropItem> getAllItems() {
        List<DropItem> allItems = new ArrayList<>();
        for (DropRarity rarity : DropRarity.values()) {
            allItems.addAll(getItemsForRarity(rarity));
        }
        return allItems;
    }
    
    /**
     * Add a new item to the drop table
     */
    public void addItem(DropItem item) {
        itemsByRarity.get(item.getRarity()).add(item);
    }
}

/*commonItems.add(new DropItem("Wooden Tablet", DropRarity.COMMON, 1, 5, 
ItemManager::createWoodenTablet));
commonItems.add(new DropItem("Clay", DropRarity.COMMON, 1, 8, 
ItemManager::createClay));
commonItems.add(new DropItem("Carving Stone", DropRarity.COMMON, 1, 3, 
ItemManager::createCarvingStone));
commonItems.add(new DropItem("Essence", DropRarity.COMMON, 1, 4, 
ItemManager::createEssence));
*/