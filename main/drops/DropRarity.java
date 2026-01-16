package dev.main.drops;
 

/**
 * Rarity tiers for drop system (separate from Item.Rarity)
 */
public enum DropRarity {
    COMMON(0.50),      // 50% drop chance
    RARE(0.30),        // 30% drop chance
    EPIC(0.15),        // 15% drop chance
    LEGENDARY(0.04),   // 4% drop chance
    MYTHIC(0.01);      // 1% drop chance
    
    private final double dropChance;
    
    DropRarity(double dropChance) {
        this.dropChance = dropChance;
    }
    
    public double getDropChance() {
        return dropChance;
    }
}