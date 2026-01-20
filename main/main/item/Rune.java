package dev.main.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.main.entity.Entity;
import dev.main.input.Position;
import dev.main.state.GameState;
import dev.main.util.DamageText;
import dev.main.util.DamageText.Type;

/**
 * Rune - Magical consumable item with buff effects
 * Different from accessories - runes are consumed on use
 */
public class Rune {
    
    public enum RuneType {
        RETURN,     // Teleport to last save point
        SPAWN       // Teleport to spawn point
        // Future: HASTE, STRENGTH, PROTECTION, etc.
    }
    
    public enum RuneSource {
        GRAND_TRADE,    // Purchased from grand trade
        CRAFT,          // Crafted by player
        QUEST,          // Quest reward
        DROP            // Monster drop
    }
    
    public enum RuneRarity {
        COMMON,
        UNCOMMON,
        RARE,
        EPIC,
        LEGENDARY
    }
    
    // Basic properties
    private RuneType type;
    private String name;
    private String description;
    private RuneRarity rarity;
    
    // Item properties
    private boolean craftable;
    private boolean tradable;
    private boolean dismantlable;
    private RuneSource source;
    
    // Crafting requirements
    private Map<String, Integer> craftingMaterials;  // Material name -> quantity
    
    // Effect properties
    private float cooldown;          // Cooldown after use (seconds)
    private int maxStack;            // Max stack size
    private boolean consumeOnUse;    // Whether rune is consumed
    
    // Visual
    private String iconPath;
    
    public Rune(RuneType type, String name, String description, RuneRarity rarity,
                boolean craftable, boolean tradable, boolean dismantlable, RuneSource source) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.craftable = craftable;
        this.tradable = tradable;
        this.dismantlable = dismantlable;
        this.source = source;
        
        this.craftingMaterials = new HashMap<>();
        this.cooldown = 0f;
        this.maxStack = 1;  // Most runes don't stack
        this.consumeOnUse = true;
        this.iconPath = null;
        
        // Initialize based on type
        initializeRuneProperties();
    }
    
    /**
     * Initialize properties based on rune type
     */
    private void initializeRuneProperties() {
        switch (type) {
            case RETURN:
                initializeReturnRune();
                break;
                
            case SPAWN:
                initializeSpawnRune();
                break;
        }
    }
    
    /**
     * Initialize Rune of Return
     * Teleports player to last save point
     */
    private void initializeReturnRune() {
        this.cooldown = 10f;  // 10 second cooldown
        this.maxStack = 1;
        this.consumeOnUse = true;
        
        // Crafting materials: 1 wooden tablet, 1 clay, 1 essence, 1 verdant shard
        if (craftable) {
            craftingMaterials.put("Wooden Tablet", 1);
            craftingMaterials.put("Clay", 1);
            craftingMaterials.put("Essence", 1);
            craftingMaterials.put("Verdant Shard", 1);
        }
    }
    
    /**
     * Initialize Rune of Spawn
     * Teleports player to world spawn point
     */
    private void initializeSpawnRune() {
        this.cooldown = 5f;  // 5 second cooldown
        this.maxStack = 1;
        this.consumeOnUse = true;
        
        // Crafting materials: 1 wooden tablet, 1 clay, 1 essence
        if (craftable) {
            craftingMaterials.put("Wooden Tablet", 1);
            craftingMaterials.put("Clay", 1);
            craftingMaterials.put("Essence", 1);
        }
    }
    
    /**
     * Use the rune - apply its effect
     * Returns true if successfully used
     */
    public boolean use(Entity caster, GameState gameState) {
        if (caster == null || gameState == null) {
            return false;
        }
        
        System.out.println("Using " + name + "...");
        
        boolean success = false;
        
        switch (type) {
            case RETURN:
                success = useReturnRune(caster, gameState);
                break;
                
            case SPAWN:
                success = useSpawnRune(caster, gameState);
                break;
        }
        
        if (success) {
            System.out.println(name + " activated!");
        } else {
            System.out.println(name + " failed to activate.");
        }
        
        return success;
    }
    
    /**
     * Rune of Return effect - teleport to last save point
     */
    private boolean useReturnRune(Entity caster, GameState gameState) {
        Position pos = caster.getComponent(Position.class);
        if (pos == null) return false;
        
        // TODO: Implement save point system
        // For now, teleport to a default location
        float returnX = 8 * 64;  // Default return point
        float returnY = 5 * 64;
        
        pos.x = returnX;
        pos.y = returnY;
        
        // Visual effect
        spawnTeleportEffect(caster, gameState);
        
        System.out.println("Teleported to last save point!");
        return true;
    }
    
    /**
     * Rune of Spawn effect - teleport to spawn point
     */
    private boolean useSpawnRune(Entity caster, GameState gameState) {
        Position pos = caster.getComponent(Position.class);
        if (pos == null) return false;
        
        // Teleport to spawn point
        float spawnX = 8 * 64;  // World spawn
        float spawnY = 5 * 64;
        
        pos.x = spawnX;
        pos.y = spawnY;
        
        // Visual effect
        spawnTeleportEffect(caster, gameState);
        
        System.out.println("Teleported to spawn point!");
        return true;
    }
    
    /**
     * Spawn visual teleport effect
     */
    private void spawnTeleportEffect(Entity caster, GameState gameState) {
        Position pos = caster.getComponent(Position.class);
        if (pos == null) return;
        
        // Spawn damage text as effect notification
        DamageText effect = new DamageText(
            "TELEPORT!",
            DamageText.Type.HEAL,
            pos.x,
            pos.y - 40
        );
        gameState.addDamageText(effect);
        
        // TODO: Add particle effects, sound, animation
    }
    
    /**
     * Check if player has required materials to craft
     */
    public boolean canCraft(Map<String, Integer> playerInventory) {
        if (!craftable) return false;
        
        for (Map.Entry<String, Integer> requirement : craftingMaterials.entrySet()) {
            String material = requirement.getKey();
            int required = requirement.getValue();
            
            int playerHas = playerInventory.getOrDefault(material, 0);
            if (playerHas < required) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get crafting cost as formatted string
     */
    public String getCraftingCostString() {
        if (!craftable || craftingMaterials.isEmpty()) {
            return "Not craftable";
        }
        
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Integer> entry : craftingMaterials.entrySet()) {
            sb.append(entry.getValue()).append("x ").append(entry.getKey());
            if (i < craftingMaterials.size() - 1) {
                sb.append(", ");
            }
            i++;
        }
        return sb.toString();
    }
    
    /**
     * Get dismantle rewards
     */
    public List<String> getDismantleRewards() {
        List<String> rewards = new ArrayList<>();
        
        if (!dismantlable) {
            return rewards;
        }
        
        // Return 50% of crafting materials
        for (Map.Entry<String, Integer> entry : craftingMaterials.entrySet()) {
            int amount = Math.max(1, entry.getValue() / 2);
            rewards.add(amount + "x " + entry.getKey());
        }
        
        return rewards;
    }
    
    /**
     * Convert to Item for inventory system
     */
    public Item toItem() {
        return new Item(
            name,
            Item.ItemType.CONSUMABLE,  // Runes are consumables
            convertRarity(),
            0, 0, 0, 0,  // No stat bonuses
            1,  // Single use durability
            false,  // Not upgradable
            false,  // Can't infuse elemental
            tradable,
            false,  // Not sellable (special items)
            dismantlable ? 1 : 0  // Dismantle threshold
        );
    }
    
    /**
     * Convert RuneRarity to Item.Rarity
     */
    private Item.Rarity convertRarity() {
        switch (rarity) {
            case COMMON: return Item.Rarity.COMMON;
            case UNCOMMON: return Item.Rarity.UNCOMMON;
            case RARE: return Item.Rarity.RARE;
            case EPIC: return Item.Rarity.EPIC;
            case LEGENDARY: return Item.Rarity.LEGENDARY;
            default: return Item.Rarity.COMMON;
        }
    }
    
    /**
     * Get detailed description for tooltip
     */
    public String getDetailedDescription() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== ").append(name).append(" ===\n");
        sb.append("Type: ").append(type).append("\n");
        sb.append("Rarity: ").append(rarity).append("\n");
        sb.append("\n");
        sb.append(description).append("\n");
        sb.append("\n");
        
        if (cooldown > 0) {
            sb.append("Cooldown: ").append(cooldown).append("s\n");
        }
        
        if (consumeOnUse) {
            sb.append("Single use (consumed)\n");
        }
        
        sb.append("\n");
        sb.append("--- Properties ---\n");
        sb.append("Craftable: ").append(craftable ? "Yes" : "No").append("\n");
        sb.append("Tradable: ").append(tradable ? "Yes" : "No").append("\n");
        sb.append("Dismantlable: ").append(dismantlable ? "Yes" : "No").append("\n");
        sb.append("Source: ").append(source).append("\n");
        
        if (craftable && !craftingMaterials.isEmpty()) {
            sb.append("\n");
            sb.append("--- Crafting Cost ---\n");
            sb.append(getCraftingCostString()).append("\n");
        }
        
        if (dismantlable) {
            sb.append("\n");
            sb.append("--- Dismantle Rewards ---\n");
            for (String reward : getDismantleRewards()) {
                sb.append("• ").append(reward).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════
    
    public RuneType getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public RuneRarity getRarity() { return rarity; }
    
    public boolean isCraftable() { return craftable; }
    public boolean isTradable() { return tradable; }
    public boolean isDismantlable() { return dismantlable; }
    public RuneSource getSource() { return source; }
    
    public Map<String, Integer> getCraftingMaterials() { return new HashMap<>(craftingMaterials); }
    public float getCooldown() { return cooldown; }
    public int getMaxStack() { return maxStack; }
    public boolean isConsumeOnUse() { return consumeOnUse; }
    
    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }
    
    @Override
    public String toString() {
        return name + " (" + type + ", " + rarity + ")";
    }
}