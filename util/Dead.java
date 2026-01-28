package dev.main.util;

import dev.main.input.Component;

public class Dead implements Component {
    public float deathTimer;
    public float corpseLifetime;
    
    public Dead(float corpseLifetime) {
        this.deathTimer = 0;
        this.corpseLifetime = corpseLifetime;
    }
    
    public void update(float delta) {
        deathTimer += delta;
    }
    
    public boolean shouldRemove() {
        return deathTimer >= corpseLifetime;
    }
}