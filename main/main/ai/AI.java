package dev.main.ai;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import dev.main.entity.Entity;
import dev.main.input.Component;

public class AI implements Component {
    
    public enum State {
        IDLE,
        ROAMING,
        CHASING,
        RETURNING,
        ATTACKING,
        DEAD,
        VICTORY_IDLE  // NEW: Idle after player dies
    }
    
    // AI.java - Add caching fields
    public List<int[]> cachedPath;
    public float pathUpdateTimer;
    public float pathUpdateInterval = 0.5f; // Update every 0.5s
    
    public State currentState;
    public String behaviorType;
    
    public float homeX;
    public float homeY;
    public float roamRadius;
    
    public float detectionRange;
    public Entity target;
    
    public float roamTimer;
    public float roamInterval;
    
    public float returnThreshold;
    
    public float attackRange;
    public float attackCooldown;
    public float attackTimer;
    
    public float victoryIdleTimer;  // NEW
    public float victoryIdleDuration;  // NEW
    
    public AI(String behaviorType, float homeX, float homeY, float roamRadius, float detectionRange) {
        this.behaviorType = behaviorType;
        this.currentState = State.IDLE;
        
        this.homeX = homeX;
        this.homeY = homeY;
        this.roamRadius = roamRadius;
        
        this.detectionRange = detectionRange;
        this.target = null;
        
        this.roamTimer = 0;
        this.roamInterval = ThreadLocalRandom.current().nextFloat(3f, 6f);
        
        this.returnThreshold = roamRadius + 64f;
        
        this.attackRange = 32f;
        this.attackCooldown = 1.5f;
        this.attackTimer = 0;
        
        this.victoryIdleTimer = 0;
        this.victoryIdleDuration = 3f;  // Idle for 3 seconds after victory
    }
    
    public void update(float delta) {
        roamTimer += delta;
        
        if (attackTimer > 0) {
            attackTimer -= delta;
        }
        
        if (currentState == State.VICTORY_IDLE) {  
            victoryIdleTimer += delta;
        }
    }
    
    public boolean canAttack() {
        return attackTimer <= 0;
    }
    
    public void resetAttackCooldown() {
        attackTimer = attackCooldown;
    }
}