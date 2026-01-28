package dev.main.entity;

import dev.main.input.Component;

public class TargetIndicator implements Component {
    public float worldX;
    public float worldY;
    public boolean active;
    
    // Animation
    public float animationTimer;
    public float pulseScale;
    
    public TargetIndicator() {
        this.active = false;
        this.animationTimer = 0;
        this.pulseScale = 1.0f;
    }
    
    public void setTarget(float x, float y) {
        this.worldX = x;
        this.worldY = y;
        this.active = true;
        this.animationTimer = 0;
    }
    
    public void clear() {
        this.active = false;
    }
    
    public void update(float delta) {
        if (!active) return;
        
        animationTimer += delta;
        
        // Pulse effect: scale between 0.9 and 1.1
        pulseScale = 1.0f + (float)Math.sin(animationTimer * 4) * 0.1f;
    }
}