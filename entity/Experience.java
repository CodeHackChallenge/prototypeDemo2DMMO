package dev.main.entity;

import dev.main.input.Component;
import dev.main.skill.SkillLevel;

public class Experience implements Component {
    public int level;
    public float currentXP;
    public float xpToNextLevel;
    
    // Stat growth per level (can be customized per character)
    public int hpGrowth;      // HP gained per level
    public int attackGrowth;  // Attack gained per level
    public int defenseGrowth; // Defense gained per level
    public int accGrowth;     // Accuracy gained per level
    public int manaGrowth;    // ☆ NEW: Mana gained per level
    
    public Experience() {
        this.level = 1;
        this.currentXP = 0;
        this.xpToNextLevel = calculateXPForLevel(2);
        
        // Default growth rates
        this.hpGrowth = 10;
        this.attackGrowth = 2;
        this.defenseGrowth = 1;
        this.accGrowth = 1;
        this.manaGrowth = 5;  // ☆ NEW: +5 mana per level
    }
    
    /**
     * Calculate total XP needed to reach a specific level
     * Formula: XP(L) = 120 * L^2.7
     */
    public static float calculateXPForLevel(int targetLevel) {
        if (targetLevel <= 1) return 0;
        return (float)(120 * Math.pow(targetLevel, 2.7));
    }
    
    /**
     * Calculate XP needed to go from current level to next level
     */
    public float calculateXPForNextLevel() {
        return calculateXPForLevel(level + 1) - calculateXPForLevel(level);
    }
    
    /**
     * Add experience points and handle level-ups
     * Returns the number of levels gained (0 if none)
     */
    public int addExperience(float xp) {
        currentXP += xp;
        int levelsGained = 0;
        
        // Check for level-ups (can level up multiple times)
        while (currentXP >= xpToNextLevel && level < 99) {
            currentXP -= xpToNextLevel;
            level++;
            levelsGained++;
            xpToNextLevel = calculateXPForNextLevel();
        }
        
        // Cap at max level
        if (level >= 99) {
            currentXP = 0;
            xpToNextLevel = 0;
        }
        
        return levelsGained;
    }
    
    /**
     * Award skill points based on levels gained
     * Called after addExperience() in GameLogic
     */
    public void awardSkillPoints(int levelsGained, SkillLevel skillLevel) {
        if (levelsGained > 0 && skillLevel != null) {
            skillLevel.awardPoints(levelsGained);  // 1 point per level
        }
    }
    
    /**
     * Get current XP progress as percentage (0.0 to 1.0)
     */
    public float getXPProgress() {
        if (xpToNextLevel <= 0) return 1.0f;
        return Math.min(1.0f, currentXP / xpToNextLevel);
    }
    
    /**
     * Calculate total HP for current level
     */
    public int calculateMaxHP(int baseHP) {
        return baseHP + ((level - 1) * hpGrowth);
    }
    
    /**
     * Calculate total attack for current level
     */
    public int calculateAttack(int baseAttack) {
        return baseAttack + ((level - 1) * attackGrowth);
    }
    
    /**
     * Calculate total defense for current level
     */
    public int calculateDefense(int baseDefense) {
        return baseDefense + ((level - 1) * defenseGrowth);
    }
    
    /**
     * Calculate total accuracy for current level
     */
    public int calculateAccuracy(int baseAccuracy) {
        return baseAccuracy + ((level - 1) * accGrowth);
    }
    
    /**
     * ☆ NEW: Calculate total mana for current level
     */
    public int calculateMaxMana(int baseMana) {
        return baseMana + ((level - 1) * manaGrowth);
    }
}