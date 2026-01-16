package dev.main.quest;

import dev.main.input.Component;

/**
 * Quest indicator - shows exclamation mark above NPC when quest is available
 */
public class QuestIndicator implements Component {
    public boolean active;
    public float animationTimer;
    public float bounceOffset;
    public float offsetY;
    
    public enum IndicatorType {
        AVAILABLE,    // Yellow ! (quest available)
        COMPLETE,     // Yellow ? (quest complete)
        IN_PROGRESS   // Gray ... (quest in progress)
    }
    
    public IndicatorType type;
    
    public QuestIndicator(float offsetY) {
        this.offsetY = offsetY;
        this.active = false;
        this.animationTimer = 0;
        this.bounceOffset = 0;
        this.type = IndicatorType.AVAILABLE;
    }
    
    public void show(IndicatorType type) {
        this.active = true;
        this.type = type;
        this.animationTimer = 0;
    }
    
    public void hide() {
        this.active = false;
        this.animationTimer = 0;
    }
    
    public void update(float delta) {
        if (!active) return;
        
        animationTimer += delta;
        
        // Bounce animation
        bounceOffset = (float)Math.sin(animationTimer * 3) * 5f;
    }
}