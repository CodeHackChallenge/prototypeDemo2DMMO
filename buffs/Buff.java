package dev.main.buffs;

import java.awt.Color;

/**
 * Buff - Temporary status effect that modifies player stats
 */
public class Buff {
    
    public enum BuffType {
        EVENT,              // Special event buffs (e.g., Fionne's blessing)
        EXP_BOOST,          // Experience gain boost
        MANA_REGEN,         // Mana regeneration
        STAMINA_REGEN,      // Stamina regeneration
        HEALTH_REGEN,       // Health regeneration
        ATTACK_BOOST,       // Attack power increase
        DEFENSE_BOOST,      // Defense increase
        SPEED_BOOST,        // Movement speed increase
        DAMAGE_REDUCTION    // Damage reduction
    }
    
    public enum DurationType {
        TIME_BASED,         // Duration decreases by delta time
        KILL_BASED,         // Duration decreases per monster kill
        INFINITE            // Never expires (until manually removed)
    }
    
    private String id;
    private String name;
    private String description;
    private BuffType type;
    private DurationType durationType;
    
    // Duration
    private float currentDuration;
    private float maxDuration;
    
    // Effect values
    private float expBoostPercent;      // 0.0 - 1.0 (0.20 = +20%)
    private float manaRegenBoost;       // Flat mana/sec increase
    private float staminaRegenBoost;    // Flat stamina/sec increase
    private float healthRegenRate;      // HP/sec
    private int attackBoost;            // Flat attack increase
    private int defenseBoost;           // Flat defense increase
    private float speedMultiplier;      // 1.0 = normal, 1.5 = +50% speed
    private float damageReduction;      // 0.0 - 1.0 (0.10 = -10% damage taken)
    
    // Visual
    private Color iconColor;
    private String iconPath;
    
    // Active flag
    private boolean active;
    
    public Buff(String id, String name, String description, BuffType type, 
                DurationType durationType, float duration) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.durationType = durationType;
        this.maxDuration = duration;
        this.currentDuration = duration;
        
        // Default values (no effect)
        this.expBoostPercent = 0f;
        this.manaRegenBoost = 0f;
        this.staminaRegenBoost = 0f;
        this.healthRegenRate = 0f;
        this.attackBoost = 0;
        this.defenseBoost = 0;
        this.speedMultiplier = 1.0f;
        this.damageReduction = 0f;
        
        // Default items color based on type
        this.iconColor = getDefaultIconColor(type);
        this.iconPath = null;
        
        this.active = true;
    }
    
    /**
     * Update buff duration (time-based)
     */
    public void update(float delta) {
        if (!active || durationType != DurationType.TIME_BASED) return;
        
        currentDuration -= delta;
        
        if (currentDuration <= 0) {
            currentDuration = 0;
            active = false;
        }
    }
    
    /**
     * Decrease duration by kill count (kill-based buffs)
     */
    public void onMonsterKill() {
        if (!active || durationType != DurationType.KILL_BASED) return;
        
        currentDuration -= 1;
        
        if (currentDuration <= 0) {
            currentDuration = 0;
            active = false;
        }
    }
    
    /**
     * Get remaining duration as percentage (0.0 - 1.0)
     */
    public float getDurationPercent() {
        if (maxDuration <= 0) return 1.0f;
        return Math.max(0f, Math.min(1.0f, currentDuration / maxDuration));
    }
    
    /**
     * Get formatted duration string
     */
    public String getDurationString() {
        switch (durationType) {
            case TIME_BASED:
                if (currentDuration >= 60) {
                    int minutes = (int)(currentDuration / 60);
                    int seconds = (int)(currentDuration % 60);
                    return String.format("%dm %ds", minutes, seconds);
                } else {
                    return String.format("%.0fs", currentDuration);
                }
                
            case KILL_BASED:
                return String.format("%.0f kills", currentDuration);
                
            case INFINITE:
                return "∞";
                
            default:
                return "";
        }
    }
    
    /**
     * Get detailed description for tooltip
     */
    public String getTooltipText() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(name).append("\n");
        sb.append("────────────\n");
        sb.append(description).append("\n\n");
        
        // Show effects
        sb.append("Effects:\n");
        
        if (expBoostPercent > 0) {
            sb.append("• EXP Gain: +").append((int)(expBoostPercent * 100)).append("%\n");
        }
        
        if (manaRegenBoost > 0) {
            sb.append("• Mana Regen: +").append(String.format("%.1f", manaRegenBoost)).append("/sec\n");
        }
        
        if (staminaRegenBoost > 0) {
            sb.append("• Stamina Regen: +").append(String.format("%.1f", staminaRegenBoost)).append("/sec\n");
        }
        
        if (healthRegenRate > 0) {
            sb.append("• Health Regen: +").append(String.format("%.1f", healthRegenRate)).append("/sec\n");
        }
        
        if (attackBoost > 0) {
            sb.append("• Attack: +").append(attackBoost).append("\n");
        }
        
        if (defenseBoost > 0) {
            sb.append("• Defense: +").append(defenseBoost).append("\n");
        }
        
        if (speedMultiplier > 1.0f) {
            int percent = (int)((speedMultiplier - 1.0f) * 100);
            sb.append("• Speed: +").append(percent).append("%\n");
        }
        
        if (damageReduction > 0) {
            sb.append("• Damage Reduction: -").append((int)(damageReduction * 100)).append("%\n");
        }
        
        sb.append("\n");
        sb.append("Duration: ").append(getDurationString());
        
        return sb.toString();
    }
    
    /**
     * Get default items color for buff type
     */
    private Color getDefaultIconColor(BuffType type) {
        switch (type) {
            case EVENT:
                return new Color(255, 215, 0);      // Gold
            case EXP_BOOST:
                return new Color(100, 200, 255);    // Light blue
            case MANA_REGEN:
                return new Color(100, 149, 237);    // Cornflower blue
            case STAMINA_REGEN:
                return new Color(205, 133, 63);     // Peru/brown
            case HEALTH_REGEN:
                return new Color(50, 205, 50);      // Lime green
            case ATTACK_BOOST:
                return new Color(220, 20, 60);      // Crimson
            case DEFENSE_BOOST:
                return new Color(70, 130, 180);     // Steel blue
            case SPEED_BOOST:
                return new Color(255, 255, 100);    // Yellow
            case DAMAGE_REDUCTION:
                return new Color(138, 43, 226);     // Blue violet
            default:
                return Color.GRAY;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // BUILDER PATTERN - Easy buff creation
    // ═══════════════════════════════════════════════════════════════
    
    public Buff setExpBoost(float percent) {
        this.expBoostPercent = percent;
        return this;
    }
    
    public Buff setManaRegenBoost(float regenPerSec) {
        this.manaRegenBoost = regenPerSec;
        return this;
    }
    
    public Buff setStaminaRegenBoost(float regenPerSec) {
        this.staminaRegenBoost = regenPerSec;
        return this;
    }
    
    public Buff setHealthRegen(float hpPerSec) {
        this.healthRegenRate = hpPerSec;
        return this;
    }
    
    public Buff setAttackBoost(int attack) {
        this.attackBoost = attack;
        return this;
    }
    
    public Buff setDefenseBoost(int defense) {
        this.defenseBoost = defense;
        return this;
    }
    
    public Buff setSpeedMultiplier(float multiplier) {
        this.speedMultiplier = multiplier;
        return this;
    }
    
    public Buff setDamageReduction(float reduction) {
        this.damageReduction = reduction;
        return this;
    }
    
    public Buff setIconColor(Color color) {
        this.iconColor = color;
        return this;
    }
    
    public Buff setIconPath(String path) {
        this.iconPath = path;
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BuffType getType() { return type; }
    public DurationType getDurationType() { return durationType; }
    
    public float getCurrentDuration() { return currentDuration; }
    public float getMaxDuration() { return maxDuration; }
    
    public float getExpBoostPercent() { return expBoostPercent; }
    public float getManaRegenBoost() { return manaRegenBoost; }
    public float getStaminaRegenBoost() { return staminaRegenBoost; }
    public float getHealthRegenRate() { return healthRegenRate; }
    public int getAttackBoost() { return attackBoost; }
    public int getDefenseBoost() { return defenseBoost; }
    public float getSpeedMultiplier() { return speedMultiplier; }
    public float getDamageReduction() { return damageReduction; }
    
    public Color getIconColor() { return iconColor; }
    public String getIconPath() { return iconPath; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    @Override
    public String toString() {
        return name + " (" + getDurationString() + ")";
    }
}