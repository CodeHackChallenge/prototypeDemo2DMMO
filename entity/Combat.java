package dev.main.entity;

import dev.main.Engine;
import dev.main.input.Component;
import dev.main.stats.Stats;

public class Combat implements Component {
    public float attackCooldown;
    public float attackTimer;
    
    // Attack stats
    public float critChance;
    public float critMultiplier;
    public float evasionChance;
    
    // Animation state
    public boolean isAttacking;
    public float attackAnimationTimer;
    public float attackAnimationDuration;
    public float hitFrame;  // NEW: Which frame triggers damage (0.0 to 1.0)
    public boolean damageApplied;  // NEW: Track if damage already dealt this attack
    
    public Entity attackTarget;  // NEW: Store who we're attacking
    
    public Combat(float attackCooldown, float critChance, float evasionChance) {
	/*
	 * 	attackCooldown = 0.5f;  // Fast attacker (2 attacks per second)
		attackCooldown = 1.5f;  // Normal attacker
		attackCooldown = 3.0f;  // Slow, heavy attacker
	 */
        this.attackCooldown = attackCooldown; // ⭐ Time between attacks (cooldown)  
        this.attackTimer = 0;
        this.critChance = critChance;
        this.critMultiplier = 2.0f;
        this.evasionChance = evasionChance;
        this.isAttacking = false;
        this.attackAnimationTimer = 0; 
    /*
     *  attackAnimationDuration = 0.3f;  // Quick jab animation
		attackAnimationDuration = 0.5f;  // Normal swing
		attackAnimationDuration = 0.8f;  // Slow, heavy slam animation
     * */
        this.attackAnimationDuration = 0.5f;  // 500ms attack animation // ⭐ Length of attack animation
        
    /*
     * hitFrame = 0.3f;  // Damage early in animation (30%)
	   hitFrame = 0.5f;  // Damage mid-swing (50%)
	   hitFrame = 0.7f;  // Damage late in animation (70% - big wind-up
     * */
        this.hitFrame = 0.5f;  // Damage at 50% through animation (mid-swing) // ⭐ When during animation to deal damage
        this.damageApplied = false;
        this.attackTarget = null;
    }
    
    public boolean canAttack() {
        return attackTimer <= 0 && !isAttacking;
    }
    // ☆ NEW: Check if can attack (cooldown + stamina)
    public boolean canAttackWithStamina(Stats stats) {
        if (!canAttack()) return false;
        
        // Check if have enough stamina
        float staminaCost = stats.getEffectiveStaminaCost(Stats.STAMINA_COST_BASIC_ATTACK);
        return stats.stamina >= staminaCost;
    }
    
    public void startAttack(Entity target) {
        isAttacking = true;
        attackAnimationTimer = 0;
        attackTimer = attackCooldown;
        damageApplied = false;  // Reset damage flag // ⭐ Flag: "haven't dealt damage yet"
        attackTarget = target;  // Store target // ⭐ Remember who we're hitting
    }
    
    public void update(float delta) {
        if (attackTimer > 0) {
            attackTimer -= delta; // Increase timer
            // attackAnimationTimer goes from 0.0 → 0.5 (500ms animation)
        }
        
        if (isAttacking) {
            attackAnimationTimer += delta;
            if (attackAnimationTimer >= attackAnimationDuration) {
                isAttacking = false;
                attackAnimationTimer = 0;
                attackTarget = null;  // Clear target
            }
        }
    }
    
    public float getAttackProgress() {
        if (!isAttacking) return 0f;
     // Returns 0.0 at start → 1.0 at end
        return Math.min(1f, attackAnimationTimer / attackAnimationDuration);
     // Example: 0.25 seconds into 0.5 second animation = 0.5 (50% done)
    }
    
    // FIX:
    public boolean shouldDealDamage() {
        if (!isAttacking || damageApplied) return false;
        
        // Store previous progress to detect crossing
        float prevProgress = Math.max(0, 
            (attackAnimationTimer - (1f / Engine.UPS)) / attackAnimationDuration);
        float currProgress = getAttackProgress();
        
        // Check if we crossed the hit frame THIS update
        if (prevProgress < hitFrame && currProgress >= hitFrame) {
            damageApplied = true;
            return true;
        }
        return false;
    }
    /*
     * **Example Timeline:**
```
Frame 1: progress = 0.10 (10%) → hitFrame is 0.5 → NO, keep animating
Frame 2: progress = 0.25 (25%) → hitFrame is 0.5 → NO, keep animating
Frame 3: progress = 0.50 (50%) → hitFrame is 0.5 → YES! DEAL DAMAGE!
Frame 4: progress = 0.75 (75%) → damageApplied = true → Skip (already hit)
Frame 5: progress = 1.00 (100%) → Animation done

**Full Attack Cycle:**
```
T=0.0s: Player clicks attack
  ├─> attackAnimationTimer starts (0.0 → 0.5s)
  └─> attackTimer = 1.5s (cooldown starts immediately)

T=0.25s: Hit frame reached! (50% of 0.5s animation)
  └─> Damage applied, text spawns

T=0.5s: Animation complete
  ├─> isAttacking = false
  └─> attackTimer still at 1.0s (still cooling down)

T=1.0s: attackTimer = 0.5s
  └─> Still can't attack (cooldown not done)

T=1.5s: attackTimer = 0.0s
  └─> ✓ Can attack again! (cooldown complete)
  
  // Dagger - fast attacks, quick animation, early hit
Combat daggerCombat = new Combat(0.8f, 0.15f, 0.05f);
daggerCombat.attackAnimationDuration = 0.3f;  // Quick stab
daggerCombat.hitFrame = 0.4f;                 // Hit early in stab

// Greatsword - slow attacks, long animation, late hit
Combat swordCombat = new Combat(2.5f, 0.20f, 0.02f);
swordCombat.attackAnimationDuration = 0.9f;   // Big swing
swordCombat.hitFrame = 0.7f;                  // Hit late in swing (wind-up)
     * */
}