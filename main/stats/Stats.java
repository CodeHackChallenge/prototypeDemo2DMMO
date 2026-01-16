package dev.main.stats;

import dev.main.entity.Experience;
import dev.main.input.Component;

public class Stats implements Component {
    // Base stats (set at character creation, don't change with level)
    public int baseMaxHp;
    public int baseAttack;
    public int baseDefense;
    public int baseAccuracy;
    public int baseMaxMana;
    
    // Current stats (calculated from base + level bonuses)
    public int hp;
    public int maxHp;
    public int attack;
    public int defense;
    public int accuracy;
    
    // Mana system
    public int mana;
    public int maxMana;
    public float manaRegenRate;
    
    // ☆ NEW: Stamina system with multipliers
    public static final float BASE_MAX_STAMINA = 500f;  // Fixed base, doesn't scale with level
    public float stamina;
    public float maxStamina;
    public float maxStaminaBonus;      // Percentage bonus (0.0 = 0%, 0.5 = 50%)
    public float staminaRegenBonus;    // Percentage bonus for regen (0.0 = 0%, 0.5 = 50%)
    public float staminaCostReduction; // Percentage reduction (0.0 = 0%, 0.3 = 30%)
    
    // Stamina constants
    public static final float STAMINA_DRAIN_RUNNING = 10f;   // Per second when running
    public static final float STAMINA_REGEN_WALKING = 8f;    // Per second when walking
    public static final float STAMINA_REGEN_IDLE = 15f;      // Per second when idle
    public static final float STAMINA_COST_BASIC_ATTACK = 20f; // Per basic attack
    
    // Stats that don't grow with level
    public int evasion;
    public int magicAttack;
    public int magicDefense;
    
    // Element resistances (percentage 0-100)
    public int fireResistance;
    public int lightningResistance;
    public int poisonResistance;
    
    // Debuff resistances (percentage 0-100)
    public int silenceResistance;
    public int blindResistance;
    public int curseResistance;
    
    //  Add dirty flag
    private boolean staminaDirty = true;
    
    /**
     * Constructor with base stats (for player at level 1)
     */
    public Stats(int baseMaxHp, int baseAttack, int baseDefense, int baseAccuracy, int baseMaxMana) {
        // Store base stats
        this.baseMaxHp = baseMaxHp;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.baseAccuracy = baseAccuracy;
        this.baseMaxMana = baseMaxMana;
        
        // Initialize current stats to base (will be updated by applyLevelStats)
        this.maxHp = baseMaxHp;
        this.hp = baseMaxHp;
        this.attack = baseAttack;
        this.defense = baseDefense;
        this.accuracy = baseAccuracy;
        
        // Initialize mana
        this.maxMana = baseMaxMana;
        this.mana = baseMaxMana;
        this.manaRegenRate = baseMaxMana * 0.01f;
        
        // ☆ NEW: Initialize stamina with multipliers
        this.maxStaminaBonus = 0f;      // No bonus by default
        this.staminaRegenBonus = 0f;    // No bonus by default
        this.staminaCostReduction = 0f; // No reduction by default
        
        calculateMaxStamina();
        this.stamina = this.maxStamina;
        
        // Stats that don't grow with level
        this.evasion = 0;
        this.magicAttack = 0;
        this.magicDefense = 0;
        
        // Resistances
        this.fireResistance = 0;
        this.lightningResistance = 0;
        this.poisonResistance = 0;
        
        this.silenceResistance = 0;
        this.blindResistance = 0;
        this.curseResistance = 0;
    }
    
    public void setMaxStaminaBonus(float bonus) {
        if (this.maxStaminaBonus != bonus) {
            this.maxStaminaBonus = bonus;
            staminaDirty = true;
        }
    }
    
    public float getMaxStamina() {
        if (staminaDirty) {
            calculateMaxStamina();
            staminaDirty = false;
        }
        return maxStamina;
    }
    /**
     * Old constructor for backwards compatibility (monsters, no mana/stamina)
     */
    public Stats(int maxHp, float maxStamina, int attack, int defense) {
        this.baseMaxHp = maxHp;
        this.maxHp = maxHp;
        this.hp = maxHp;
        
        this.baseAttack = attack;
        this.attack = attack;
        
        this.baseDefense = defense;
        this.defense = defense;
        
        this.baseAccuracy = 0;
        this.accuracy = 0;
        
        this.baseMaxMana = 0;
        this.maxMana = 0;
        this.mana = 0;
        this.manaRegenRate = 0f;
        
        // Stamina (for monsters - simple system)
        this.maxStamina = maxStamina;
        this.stamina = maxStamina;
        this.maxStaminaBonus = 0f;
        this.staminaRegenBonus = 0f;
        this.staminaCostReduction = 0f;
        
        // Stats that don't grow with level
        this.evasion = 0;
        this.magicAttack = 0;
        this.magicDefense = 0;
        
        // Resistances
        this.fireResistance = 0;
        this.lightningResistance = 0;
        this.poisonResistance = 0;
        
        this.silenceResistance = 0;
        this.blindResistance = 0;
        this.curseResistance = 0;
    }
    
    /**
     * ☆ NEW: Calculate max stamina with bonuses
     * Formula: MaxStamina = BaseStamina × (1 + bonus%)
     */
    public void calculateMaxStamina() {
        this.maxStamina = BASE_MAX_STAMINA * (1f + maxStaminaBonus);
    }
    
    /**
     * ☆ NEW: Get effective stamina regen rate with bonuses
     * Formula: EffectiveRegen = BaseRegen × (1 + regen%)
     */
    public float getEffectiveStaminaRegen(float baseRegen) {
        return baseRegen * (1f + staminaRegenBonus);
    }
    
    /**
     * ☆ NEW: Get effective stamina cost with reduction
     * Formula: EffectiveCost = BaseCost × (1 - reduction%)
     */
    public float getEffectiveStaminaCost(float baseCost) {
        return baseCost * (1f - staminaCostReduction);
    }
    
    /**
     * ☆ NEW: Consume stamina for basic attack
     * Returns false if not enough stamina
     */
    public boolean consumeStaminaForAttack() {
        float cost = getEffectiveStaminaCost(STAMINA_COST_BASIC_ATTACK);
        return consumeStamina(cost);
    }
    
    /**
     * ☆ NEW: Regenerate stamina based on movement state
     * @param state "idle", "walking", or "running"
     */
    public void regenerateStaminaByState(String state, float delta) {
        float baseRegen;
        
        switch (state) {
            case "idle":
                baseRegen = STAMINA_REGEN_IDLE;
                break;
            case "walking":
                baseRegen = STAMINA_REGEN_WALKING;
                break;
            case "running":
                // Running drains stamina
                float drain = getEffectiveStaminaCost(STAMINA_DRAIN_RUNNING);
                consumeStamina(drain * delta);
                return;  // No regen while running
            default:
                baseRegen = STAMINA_REGEN_IDLE;
        }
        
        float effectiveRegen = getEffectiveStaminaRegen(baseRegen);
        stamina = Math.min(maxStamina, stamina + effectiveRegen * delta);
    }
    
    /**
     * Apply level-based stat bonuses from Experience component
     */
    public void applyLevelStats(Experience exp, boolean fullHeal) {
        // Calculate new max stats
        this.maxHp = exp.calculateMaxHP(baseMaxHp);
        this.attack = exp.calculateAttack(baseAttack);
        this.defense = exp.calculateDefense(baseDefense);
        this.accuracy = exp.calculateAccuracy(baseAccuracy);
        
        // Calculate max mana
        this.maxMana = exp.calculateMaxMana(baseMaxMana);
        this.manaRegenRate = maxMana * 0.01f;
        
        // ☆ Recalculate max stamina (in case bonuses changed)
        calculateMaxStamina();
        
        // Full heal on level up
        if (fullHeal) {
            this.hp = this.maxHp;
            this.stamina = this.maxStamina;
            this.mana = this.maxMana;
        } else {
            // Just cap values at new max if not healing
            if (this.hp > this.maxHp) {
                this.hp = this.maxHp;
            }
            if (this.mana > this.maxMana) {
                this.mana = this.maxMana;
            }
            if (this.stamina > this.maxStamina) {
                this.stamina = this.maxStamina;
            }
        }
        
        staminaDirty = true;  // ★ Mark for recalc
        if (fullHeal) {
            calculateMaxStamina();  // Recalc now if healing
            staminaDirty = false;
        }
    }
    
    /**
     * Apply level stats without healing (for initialization)
     */
    public void applyLevelStats(Experience exp) {
        applyLevelStats(exp, false);
    }
    
    /**
     * Fully restore HP, stamina, and mana
     */
    public void fullHeal() {
        this.hp = this.maxHp;
        this.stamina = this.maxStamina;
        this.mana = this.maxMana;
    }
    
    /**
     * Consume mana, returns false if not enough
     */
    public boolean consumeMana(int amount) {
        if (mana >= amount) {
            mana -= amount;
            return true;
        }
        return false;
    }
    
    /**
     * Regenerate mana
     */
    public void regenerateMana(float delta) {
        if (mana < maxMana) {
            mana = Math.min(maxMana, (int)(mana + manaRegenRate * delta));
        }
    }
    
    /**
     * Consume stamina, returns false if not enough
     */
    public boolean consumeStamina(float amount) {
        if (stamina >= amount) {
            stamina -= amount;
            if (stamina < 0) stamina = 0;
            return true;
        }
        return false;
    }
    
    /**
     * Get hit chance against target
     */
    public float getHitChance(Stats targetStats) {
        float baseHitChance = 0.95f;
        float accModifier = this.accuracy * 0.01f;
        float evaModifier = targetStats.evasion * 0.01f;
        
        float hitChance = baseHitChance + accModifier - evaModifier;
        
        return Math.max(0.05f, Math.min(1.0f, hitChance));
    }
}