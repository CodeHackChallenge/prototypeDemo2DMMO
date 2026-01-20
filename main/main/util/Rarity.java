package dev.main.util;
 

/**
 * Enum representing item rarity tiers with weighted drop chances
 */
public enum Rarity {
    COMMON(0.50),      // 50% drop chance
    RARE(0.30),        // 30% drop chance
    EPIC(0.15),        // 15% drop chance
    LEGENDARY(0.04),   // 4% drop chance
    MYTHIC(0.01);      // 1% drop chance
    
    private final double dropChance;
    
    Rarity(double dropChance) {
        this.dropChance = dropChance;
    }
    
    public double getDropChance() {
        return dropChance;
    }
    
    /**
     * Get color code for UI display
     */
    public String getColorCode() {
        switch(this) {
            case COMMON: return "\u001B[37m";    // White
            case RARE: return "\u001B[34m";      // Blue
            case EPIC: return "\u001B[35m";      // Magenta
            case LEGENDARY: return "\u001B[33m"; // Yellow
            case MYTHIC: return "\u001B[31m";    // Red
            default: return "\u001B[0m";         // Reset
        }
    }
}