package dev.main.item;

import java.util.HashMap;
import java.util.Map;

/**
 * RuneManager - Factory for creating runes
 * Replaces ItemManager.createRuneOfReturn()
 */
public class RuneManager {
    
    // Registry of all rune types
    private static final Map<Rune.RuneType, RuneTemplate> RUNE_REGISTRY = new HashMap<>();
    
    static {
        // Initialize rune templates
        initializeRuneTemplates();
    }
    
    /**
     * Initialize all rune templates
     */
    private static void initializeRuneTemplates() {
        // Rune of Return
        registerRune(
            Rune.RuneType.RETURN,
            "Rune of Return",
            "A mystical rune that teleports you to your last save point. The magical energy dissipates after one use.",
            Rune.RuneRarity.RARE,
            true,   // craftable
            false,  // tradable
            true,   // dismantlable
            Rune.RuneSource.CRAFT
        );
        
        // Rune of Spawn
        registerRune(
            Rune.RuneType.SPAWN,
            "Rune of Spawn",
            "A basic teleportation rune that returns you to the world spawn point. Consumed upon use.",
            Rune.RuneRarity.UNCOMMON,
            true,   // craftable
            true,   // tradable
            true,   // dismantlable
            Rune.RuneSource.CRAFT
        );
    }
    
    /**
     * Register a rune template
     */
    private static void registerRune(Rune.RuneType type, String name, String description,
                                     Rune.RuneRarity rarity, boolean craftable, boolean tradable,
                                     boolean dismantlable, Rune.RuneSource source) {
        RUNE_REGISTRY.put(type, new RuneTemplate(
            name, description, rarity, craftable, tradable, dismantlable, source
        ));
    }
    
    /**
     * Create a rune by type
     */
    public static Rune createRune(Rune.RuneType type) {
        RuneTemplate template = RUNE_REGISTRY.get(type);
        
        if (template == null) {
            System.err.println("Unknown rune type: " + type);
            return null;
        }
        
        return new Rune(
            type,
            template.name,
            template.description,
            template.rarity,
            template.craftable,
            template.tradable,
            template.dismantlable,
            template.source
        );
    }
    
    /**
     * Create Rune of Return
     */
    public static Rune createRuneOfReturn() {
        return createRune(Rune.RuneType.RETURN);
    }
    
    /**
     * Create Rune of Spawn
     */
    public static Rune createRuneOfSpawn() {
        return createRune(Rune.RuneType.SPAWN);
    }
    
    /**
     * Create rune with custom source
     */
    public static Rune createRune(Rune.RuneType type, Rune.RuneSource source) {
        Rune rune = createRune(type);
        if (rune != null) {
            // Adjust properties based on source
            // (would need to expose setters or use builder pattern)
        }
        return rune;
    }
    
    /**
     * Check if player can craft a rune
     */
    public static boolean canCraftRune(Rune.RuneType type, Map<String, Integer> playerInventory) {
        Rune rune = createRune(type);
        if (rune == null) return false;
        
        return rune.canCraft(playerInventory);
    }
    
    /**
     * Get crafting materials for a rune type
     */
    public static Map<String, Integer> getCraftingMaterials(Rune.RuneType type) {
        Rune rune = createRune(type);
        if (rune == null) return new HashMap<>();
        
        return rune.getCraftingMaterials();
    }
    
    /**
     * Craft a rune (consumes materials)
     */
    public static Rune craftRune(Rune.RuneType type, Map<String, Integer> playerInventory) {
        Rune rune = createRune(type);
        
        if (rune == null) {
            System.out.println("Unknown rune type: " + type);
            return null;
        }
        
        if (!rune.isCraftable()) {
            System.out.println(rune.getName() + " is not craftable!");
            return null;
        }
        
        if (!rune.canCraft(playerInventory)) {
            System.out.println("Insufficient materials to craft " + rune.getName());
            System.out.println("Required: " + rune.getCraftingCostString());
            return null;
        }
        
        // Consume materials
        for (Map.Entry<String, Integer> entry : rune.getCraftingMaterials().entrySet()) {
            String material = entry.getKey();
            int required = entry.getValue();
            
            int currentAmount = playerInventory.get(material);
            playerInventory.put(material, currentAmount - required);
        }
        
        System.out.println("Successfully crafted " + rune.getName() + "!");
        return rune;
    }
    
    /**
     * Get all available rune types
     */
    public static Rune.RuneType[] getAllRuneTypes() {
        return Rune.RuneType.values();
    }
    
    /**
     * Get rune info without creating instance
     */
    public static String getRuneInfo(Rune.RuneType type) {
        Rune rune = createRune(type);
        if (rune == null) return "Unknown rune";
        
        return rune.getDetailedDescription();
    }
    
    /**
     * Convert Rune to Item for inventory system
     */
    public static Item runeToItem(Rune rune) {
        if (rune == null) return null;
        return rune.toItem();
    }
    
    /**
     * Create Item directly from rune type
     */
    public static Item createRuneItem(Rune.RuneType type) {
        Rune rune = createRune(type);
        if (rune == null) return null;
        return rune.toItem();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // HELPER CLASSES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Template for creating runes
     */
    private static class RuneTemplate {
        String name;
        String description;
        Rune.RuneRarity rarity;
        boolean craftable;
        boolean tradable;
        boolean dismantlable;
        Rune.RuneSource source;
        
        RuneTemplate(String name, String description, Rune.RuneRarity rarity,
                    boolean craftable, boolean tradable, boolean dismantlable,
                    Rune.RuneSource source) {
            this.name = name;
            this.description = description;
            this.rarity = rarity;
            this.craftable = craftable;
            this.tradable = tradable;
            this.dismantlable = dismantlable;
            this.source = source;
        }
    }
}