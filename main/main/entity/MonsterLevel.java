package dev.main.entity;

import dev.main.input.Component;

/**
 * Component to track a monster's level and tier
 * Used for XP calculation and scaling
 */
public class MonsterLevel implements Component {
    public int level;
    public MobTier tier;
    private final int cachedXPReward;  // ★ NEW: Cache it
    
    public MonsterLevel(int level, MobTier tier) {
        this.level = level;
        this.tier = tier;
        this.cachedXPReward = calculateXPReward();  // ★ Calculate once
    }
    
    /**
     * Calculate XP reward for killing this monster
     * Formula: BaseXP × LevelFactor × TierMultiplier
     */
    private int calculateXPReward() {  // ★ Made private
        int baseXP = 50;
        double levelFactor = 1.0 + (level * 0.5);
        
        double tierMultiplier;
        switch (tier) {
            case TRASH: tierMultiplier = 0.5; break;
            case NORMAL: tierMultiplier = 1.0; break;
            case ELITE: tierMultiplier = 2.5; break;
            case MINIBOSS: tierMultiplier = 5.0; break;
            default: tierMultiplier = 1.0;
        }
        
        return Math.max(10, (int)(baseXP * levelFactor * tierMultiplier));
    }
    
    public int getXPReward() {  // ★ NEW: Just return cached value
        return cachedXPReward;
    }
    
    @Override
    public String toString() {
        return "Lv" + level + " " + tier;
    }
}