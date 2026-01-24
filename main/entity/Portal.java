package dev.main.entity;

// ════════════════════════════════════════════════════════════════════════
//NEW FILE: Portal.java
//Component for portal entities
//════════════════════════════════════════════════════════════════════════

import dev.main.input.Component;

/**
 * Portal component - triggers teleportation when player enters
 */
public class Portal implements Component {
    public String id;
    public String targetMap;
    public int targetX;  // Tile coordinates
    public int targetY;
    public boolean isActive;
    
    // Visual properties
    public float animationTimer;
    public float animationSpeed;
    
    public Portal(String id, String targetMap, int targetX, int targetY) {
        this.id = id;
        this.targetMap = targetMap;
        this.targetX = targetX;
        this.targetY = targetY;
        this.isActive = true;
        this.animationTimer = 0f;
        this.animationSpeed = 2f;
    }
    
    public void update(float delta) {
        animationTimer += delta * animationSpeed;
        if (animationTimer > Math.PI * 2) {
            animationTimer -= Math.PI * 2;
        }
    }
    
    public void activate() {
        isActive = true;
    }
    
    public void deactivate() {
        isActive = false;
    }
}