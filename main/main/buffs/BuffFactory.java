package dev.main.buffs;

/**
 * BuffFactory - Creates pre-configured buffs
 */
public class BuffFactory {
    
    /**
     * Create Fionne's Blessing buff (Event buff from second quest)
     * Effect: +20% EXP gain
     * Duration: 20000 kills
     */
    public static Buff createFionnesBlessing() {
        return new Buff(
            "fionnes_blessing",
            "Fionne's Blessing",
            "The Divine Elf's enchantment aids your growth. Gain bonus experience from combat.",
            Buff.BuffType.EVENT,
            Buff.DurationType.KILL_BASED,
            20000  // 20000 kills
        ).setExpBoost(0.20f);  // +20% EXP
    }
    
    /**
     * Create EXP Boost Potion buff
     * Effect: +50% EXP gain
     * Duration: 30 minutes
     */
    public static Buff createExpBoostPotion() {
        return new Buff(
            "exp_boost_potion",
            "EXP Boost",
            "Gain bonus experience from all sources.",
            Buff.BuffType.EXP_BOOST,
            Buff.DurationType.TIME_BASED,
            1800  // 30 minutes
        ).setExpBoost(0.50f);  // +50% EXP
    }
    
    /**
     * Create Mana Regeneration buff
     * Effect: +5 mana/sec
     * Duration: 10 minutes
     */
    public static Buff createManaRegenBuff() {
        return new Buff(
            "mana_regen",
            "Mana Flow",
            "Mana regenerates at an accelerated rate.",
            Buff.BuffType.MANA_REGEN,
            Buff.DurationType.TIME_BASED,
            600  // 10 minutes
        ).setManaRegenBoost(5.0f);
    }
    
    /**
     * Create Stamina Regeneration buff
     * Effect: +10 stamina/sec
     * Duration: 5 minutes
     */
    public static Buff createStaminaRegenBuff() {
        return new Buff(
            "stamina_regen",
            "Endurance",
            "Stamina recovers more quickly.",
            Buff.BuffType.STAMINA_REGEN,
            Buff.DurationType.TIME_BASED,
            300  // 5 minutes
        ).setStaminaRegenBoost(10.0f);
    }
    
    /**
     * Create Health Regeneration buff
     * Effect: +2 HP/sec
     * Duration: 15 minutes
     */
    public static Buff createHealthRegenBuff() {
        return new Buff(
            "health_regen",
            "Regeneration",
            "Slowly restore health over time.",
            Buff.BuffType.HEALTH_REGEN,
            Buff.DurationType.TIME_BASED,
            900  // 15 minutes
        ).setHealthRegen(2.0f);
    }
    
    /**
     * Create Attack Boost buff
     * Effect: +10 Attack
     * Duration: 5 minutes
     */
    public static Buff createAttackBoost() {
        return new Buff(
            "attack_boost",
            "Strength",
            "Your attacks deal more damage.",
            Buff.BuffType.ATTACK_BOOST,
            Buff.DurationType.TIME_BASED,
            300  // 5 minutes
        ).setAttackBoost(10);
    }
    
    /**
     * Create Defense Boost buff
     * Effect: +10 Defense
     * Duration: 5 minutes
     */
    public static Buff createDefenseBoost() {
        return new Buff(
            "defense_boost",
            "Protection",
            "Increase your defensive capabilities.",
            Buff.BuffType.DEFENSE_BOOST,
            Buff.DurationType.TIME_BASED,
            300  // 5 minutes
        ).setDefenseBoost(10);
    }
    
    /**
     * Create Speed Boost buff
     * Effect: +30% movement speed
     * Duration: 3 minutes
     */
    public static Buff createSpeedBoost() {
        return new Buff(
            "speed_boost",
            "Haste",
            "Move faster than normal.",
            Buff.BuffType.SPEED_BOOST,
            Buff.DurationType.TIME_BASED,
            180  // 3 minutes
        ).setSpeedMultiplier(1.30f);  // +30% speed
    }
    
    /**
     * Create Damage Reduction buff
     * Effect: -15% damage taken
     * Duration: 10 minutes
     */
    public static Buff createDamageReduction() {
        return new Buff(
            "damage_reduction",
            "Fortitude",
            "Reduce incoming damage.",
            Buff.BuffType.DAMAGE_REDUCTION,
            Buff.DurationType.TIME_BASED,
            600  // 10 minutes
        ).setDamageReduction(0.15f);  // -15% damage
    }
}