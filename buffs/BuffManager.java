package dev.main.buffs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dev.main.input.Component;

/**
 * BuffManager Component - Manages active buffs on an entity
 */
public class BuffManager implements Component {
    
    private List<Buff> activeBuffs;
    private int maxBuffSlots;
    
    public BuffManager() {
        this.activeBuffs = new ArrayList<>();
        this.maxBuffSlots = 10;  // Maximum 10 buffs at once
    }
    
    /**
     * Update all active buffs
     */
    public void update(float delta) {
        Iterator<Buff> iterator = activeBuffs.iterator();
        while (iterator.hasNext()) {
            Buff buff = iterator.next();
            buff.update(delta);
            
            // Remove expired buffs
            if (!buff.isActive()) {
                System.out.println("Buff expired: " + buff.getName());
                iterator.remove();
            }
        }
    }
    
    /**
     * Add a buff
     */
    public boolean addBuff(Buff buff) {
        if (activeBuffs.size() >= maxBuffSlots) {
            System.out.println("Cannot add buff - max slots reached!");
            return false;
        }
        
        // Check if buff with same ID already exists
        Buff existing = getBuff(buff.getId());
        if (existing != null) {
            // Replace with new buff (refreshes duration)
            activeBuffs.remove(existing);
            activeBuffs.add(buff);
            System.out.println("Refreshing buff: " + buff.getName() + " - " + buff.getDurationString());
            return true;
        }
        
        activeBuffs.add(buff);
        System.out.println("Buff added: " + buff.getName() + " - " + buff.getDurationString());
        return true;
    }
    
    /**
     * Remove a buff by ID
     */
    public boolean removeBuff(String buffId) {
        Iterator<Buff> iterator = activeBuffs.iterator();
        while (iterator.hasNext()) {
            Buff buff = iterator.next();
            if (buff.getId().equals(buffId)) {
                iterator.remove();
                System.out.println("Buff removed: " + buff.getName());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get a buff by ID
     */
    public Buff getBuff(String buffId) {
        for (Buff buff : activeBuffs) {
            if (buff.getId().equals(buffId)) {
                return buff;
            }
        }
        return null;
    }
    
    /**
     * Notify all buffs of a monster kill (for kill-based duration)
     */
    public void onMonsterKill() {
        Iterator<Buff> iterator = activeBuffs.iterator();
        while (iterator.hasNext()) {
            Buff buff = iterator.next();
            buff.onMonsterKill();
            
            // Remove expired buffs
            if (!buff.isActive()) {
                System.out.println("Buff expired: " + buff.getName());
                iterator.remove();
            }
        }
    }
    
    /**
     * Get total EXP boost from all buffs
     */
    public float getTotalExpBoost() {
        float total = 0f;
        for (Buff buff : activeBuffs) {
            total += buff.getExpBoostPercent();
        }
        return total;
    }
    
    /**
     * Get total mana regen boost
     */
    public float getTotalManaRegenBoost() {
        float total = 0f;
        for (Buff buff : activeBuffs) {
            total += buff.getManaRegenBoost();
        }
        return total;
    }
    
    /**
     * Get total stamina regen boost
     */
    public float getTotalStaminaRegenBoost() {
        float total = 0f;
        for (Buff buff : activeBuffs) {
            total += buff.getStaminaRegenBoost();
        }
        return total;
    }
    
    /**
     * Get total health regen rate
     */
    public float getTotalHealthRegen() {
        float total = 0f;
        for (Buff buff : activeBuffs) {
            total += buff.getHealthRegenRate();
        }
        return total;
    }
    
    /**
     * Get total attack boost
     */
    public int getTotalAttackBoost() {
        int total = 0;
        for (Buff buff : activeBuffs) {
            total += buff.getAttackBoost();
        }
        return total;
    }
    
    /**
     * Get total defense boost
     */
    public int getTotalDefenseBoost() {
        int total = 0;
        for (Buff buff : activeBuffs) {
            total += buff.getDefenseBoost();
        }
        return total;
    }
    
    /**
     * Get combined speed multiplier
     */
    public float getCombinedSpeedMultiplier() {
        float multiplier = 1.0f;
        for (Buff buff : activeBuffs) {
            multiplier *= buff.getSpeedMultiplier();
        }
        return multiplier;
    }
    
    /**
     * Get total damage reduction (capped at 75%)
     */
    public float getTotalDamageReduction() {
        float total = 0f;
        for (Buff buff : activeBuffs) {
            total += buff.getDamageReduction();
        }
        return Math.min(0.75f, total);  // Cap at 75% reduction
    }
    
    /**
     * Get all active buffs
     */
    public List<Buff> getActiveBuffs() {
        return new ArrayList<>(activeBuffs);  // Return copy to prevent modification
    }
    
    /**
     * Get number of active buffs
     */
    public int getActiveBuffCount() {
        return activeBuffs.size();
    }
    
    /**
     * Check if has any active buffs
     */
    public boolean hasActiveBuffs() {
        return !activeBuffs.isEmpty();
    }
    
    /**
     * Clear all buffs
     */
    public void clearAllBuffs() {
        activeBuffs.clear();
        System.out.println("All buffs cleared");
    }
}