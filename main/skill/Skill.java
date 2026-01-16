package dev.main.skill;

import java.awt.Color;

/**
 * Represents a skill that can be equipped in skill slots
 */
public class Skill {
    
    public enum SkillType {
        ATTACK,      // Offensive skill
        DEFENSE,     // Defensive skill
        BUFF,        // Buff/support skill
        HEAL,        // Healing skill
        PASSIVE      // Passive ability
    }
    
    private String id;
    private String name;
    private String description;
    private SkillType type;
    private String iconPath;  // Path to skill items image
    
    // Skill properties
    private float cooldown;
    private float currentCooldown;
    private int baseManaPercent;  // ☆ NEW: Base mana cost as % of max mana (e.g., 12 for 12%)
    private int levelRequired;
    
    // Leveling
    private int skillLevel;      // Current skill level (1-10)
    private int maxSkillLevel;   // Maximum level (usually 10)
    
    // Visual
    private Color iconColor;  // Fallback color if no items
    
    public Skill(String id, String name, String description, SkillType type, 
                 float cooldown, int baseManaPercent, int levelRequired) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.cooldown = cooldown;
        this.currentCooldown = 0;
        this.baseManaPercent = baseManaPercent;  // ☆ NEW: Store as percentage
        this.levelRequired = levelRequired;
        this.iconPath = null;
        
        // Leveling - start at level 1
        this.skillLevel = 1;
        this.maxSkillLevel = 10;
        
        // Default colors based on type
        switch (type) {
            case ATTACK:
                this.iconColor = new Color(220, 20, 60);  // Red
                break;
            case DEFENSE:
                this.iconColor = new Color(70, 130, 180);  // Blue
                break;
            case BUFF:
                this.iconColor = new Color(255, 215, 0);  // Gold
                break;
            case HEAL:
                this.iconColor = new Color(50, 205, 50);  // Green
                break;
            case PASSIVE:
                this.iconColor = new Color(138, 43, 226);  // Purple
                break;
            default:
                this.iconColor = Color.GRAY;
        }
    }
    
    /**
     * Update cooldown timer
     */
    public void update(float delta) {
        if (currentCooldown > 0) {
            currentCooldown -= delta;
            if (currentCooldown < 0) {
                currentCooldown = 0;
            }
        }
    }
    
    /**
     * Use the skill (start cooldown)
     */
    public boolean use() {
        if (!isReady()) return false;
        
        currentCooldown = cooldown;
        return true;
    }
    
    /**
     * Check if skill is ready to use
     */
    public boolean isReady() {
        return currentCooldown <= 0;
    }
    
    /**
     * Get cooldown progress (0.0 = ready, 1.0 = just used)
     */
    public float getCooldownProgress() {
        if (cooldown <= 0) return 0f;
        return currentCooldown / cooldown;
    }
    
    /**
     * Get remaining cooldown time
     */
    public float getRemainingCooldown() {
        return currentCooldown;
    }
    
    /**
     * ☆ NEW: Calculate mana cost based on player's max mana and skill level
     * Formula: ManaCost(s) = MaxMana × BaseCost% × (1 - 0.03s)
     * where s = skill level (1-10)
     * Reduction caps at 30% at skill level 10
     */
    public int calculateManaCost(int maxMana) {
        // Base cost as percentage of max mana
        float baseCost = maxMana * (baseManaPercent / 100f);
        
        // Apply skill level reduction: -3% per level
        float reduction = 0.03f * skillLevel;
        float multiplier = 1.0f - reduction;
        
        // Calculate final cost
        int finalCost = (int)(baseCost * multiplier);
        
        // Minimum cost of 1 mana
        return Math.max(1, finalCost);
    }
    
    /**
     * Get mana cost reduction percentage
     */
    public float getManaCostReduction() {
        return Math.min(0.30f, 0.03f * skillLevel);  // Cap at 30%
    }
    
    /**
     * Calculate upgrade cost for next level
     * Formula: 1 + (currentLevel / 3)
     */
    public int getUpgradeCost() {
        if (skillLevel >= maxSkillLevel) return 0;
        return 1 + (skillLevel / 3);  // Integer division (floor)
    }
    
    /**
     * Upgrade skill level
     * @return true if upgrade was successful
     */
    public boolean upgrade() {
        if (skillLevel >= maxSkillLevel) {
            return false;  // Already max level
        }
        
        skillLevel++;
        return true;
    }
    
    /**
     * Check if skill can be upgraded
     */
    public boolean canUpgrade() {
        return skillLevel < maxSkillLevel;
    }
    
    /**
     * Check if skill is max level
     */
    public boolean isMaxLevel() {
        return skillLevel >= maxSkillLevel;
    }
    
    /**
     * Get heal percent for HEAL type skills
     * Formula: 0.10 + 0.015 * skillLevel
     */
    public double getHealPercent() {
        return 0.10 + 0.015 * skillLevel;
    }
    
    /**
     * Calculate heal amount based on max HP
     */
    public int calculateHealAmount(int maxHp) {
        double healPercent = getHealPercent();
        return (int)(maxHp * healPercent);
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public SkillType getType() { return type; }
    public String getIconPath() { return iconPath; }
    public float getCooldown() { return cooldown; }
    public int getBaseManaPercent() { return baseManaPercent; }  // ☆ NEW
    public int getLevelRequired() { return levelRequired; }
    public Color getIconColor() { return iconColor; }
    public int getSkillLevel() { return skillLevel; }
    public int getMaxSkillLevel() { return maxSkillLevel; }
    
    // Setters
    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }
    
    public void setIconColor(Color color) {
        this.iconColor = color;
    }
    
    @Override
    public String toString() {
        return name + " Lv" + skillLevel + " (" + type + ")";
    }
}