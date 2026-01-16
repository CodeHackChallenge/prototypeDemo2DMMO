package dev.main.util;

import dev.main.input.Component;

public class Alert implements Component {
    public boolean active;
    public float animationTimer;
    public float bounceOffset;  // For bounce animation
    public float offsetY;       // How far above entity to show
    
    public Alert(float offsetY) {
        this.offsetY = offsetY;
        this.active = false;
        this.animationTimer = 0;
        this.bounceOffset = 0;
    }
    
    public void show() {
        if (!this.active) {
            this.active = true;
            this.animationTimer = 0;
        }
    }
    
    public void hide() {
        this.active = false;
        this.animationTimer = 0;
    }
    
    public void update(float delta) {
        if (!active) return;
        
        animationTimer += delta;
        
        // Bounce animation: sin wave for smooth up/down motion
        bounceOffset = (float)Math.sin(animationTimer * 6) * 3f;  // 3 pixels bounce
    }
}