package dev.main.entity;

import dev.main.input.Component;

public class LevelUpEffect implements Component {
    public boolean active;
    public float timer;
    public float duration;
    public int newLevel;
    
    public LevelUpEffect() {
        this.active = false;
        this.timer = 0;
        this.duration = 2.0f; // Effect lasts 2 seconds
        this.newLevel = 1;
    }
    
    public void trigger(int level) {
        this.active = true;
        this.timer = 0;
        this.newLevel = level;
    }
    
    public void update(float delta) {
        if (active) {
            timer += delta;
            if (timer >= duration) {
                active = false;
                timer = 0;
            }
        }
    }
    
    public float getAlpha() {
        if (!active) return 0f;
        // Fade out over duration
        return 1.0f - (timer / duration);
    }
}