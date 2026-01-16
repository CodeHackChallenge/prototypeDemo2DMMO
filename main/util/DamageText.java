package dev.main.util;

import java.awt.Color;

import dev.main.input.Component;

public class DamageText implements Component {
    
    public enum Type {
        NORMAL,
        CRITICAL,
        MISS,
        HEAL,
        PLAYER_DAMAGE,  // ☆ NEW: Damage dealt to player (darker red)
        PLAYER_CRITICAL_DAMAGE // ☆ NEW: Critical damage to player (dark orange/red)
    }
    
    public String text;
    public Type type;
    public float worldX;
    public float worldY;
    public float velocityX;
    public float velocityY;
    public float lifetime;
    public float age;
    public Color color;
    
    public DamageText(String text, Type type, float worldX, float worldY) {
        this.text = text;
        this.type = type;
        this.worldX = worldX;
        this.worldY = worldY;
        this.age = 0;
        
        switch(type) {
            case NORMAL:
                this.color = Color.WHITE;
                this.lifetime = 1.0f;
                this.velocityX = (float)(Math.random() - 0.5) * 30f;
                this.velocityY = -60f;
                break;
                
            case CRITICAL:
                this.color = new Color(255, 140, 0);  // Orange
                this.lifetime = 1.5f;
                this.velocityX = (float)(Math.random() - 0.5) * 40f;
                this.velocityY = -80f;
                break;
                
            case MISS:
                this.color = new Color(200, 200, 200);  // Light gray
                this.lifetime = 1.2f;
                this.velocityX = 0;
                this.velocityY = -50f;
                break;
                
            case HEAL:
                this.color = new Color(0, 255, 100);  // Green
                this.lifetime = 1.0f;
                this.velocityX = 0;
                this.velocityY = -70f;
                break;
                
            case PLAYER_DAMAGE:  // ☆ NEW: Darker red for player damage
                this.color = new Color(180, 20, 20);  // Dark red
                this.lifetime = 1.2f;
                this.velocityX = (float)(Math.random() - 0.5) * 30f;
                this.velocityY = -65f;
                break;
                
            case PLAYER_CRITICAL_DAMAGE:  // ☆ NEW: Dark orange-red for player crits
                this.color = new Color(200, 60, 0);  // Dark orange-red
                this.lifetime = 1.5f;
                this.velocityX = (float)(Math.random() - 0.5) * 40f;
                this.velocityY = -80f;
                break;
        }
    }
    
    public void update(float delta) {
        age += delta;
        worldX += velocityX * delta;
        worldY += velocityY * delta;
        
        // Slow down over time
        velocityX *= 0.95f;
        velocityY *= 0.98f;
    }
    
    public boolean shouldRemove() {
        return age >= lifetime;
    }
    
    public float getAlpha() {
        // Fade out in last 30% of lifetime
        float fadeStart = lifetime * 0.7f;
        if (age < fadeStart) {
            return 1.0f;
        }
        return 1.0f - ((age - fadeStart) / (lifetime - fadeStart));
    }
}