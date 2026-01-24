package dev.main.quest;

import dev.main.input.Component;

public class QuestIndicator implements Component {
    public boolean active;
    public float animationTimer;
    public float bounceOffset;
    public float offsetY;
    
    private static final float BOUNCE_SPEED = 3.0f;
    private static final float BOUNCE_HEIGHT = 5.0f;
    
    public enum IndicatorType {
        AVAILABLE,
        COMPLETE,
        IN_PROGRESS
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
        this.bounceOffset = 0;
    }
    
    public void hide() {
        this.active = false;
        this.animationTimer = 0;
        this.bounceOffset = 0;
    }
    
    public void update(float delta) {
        if (!active) {
            bounceOffset = 0;
            return;
        }
        
        animationTimer += delta;
        bounceOffset = (float)Math.sin(animationTimer * BOUNCE_SPEED) * BOUNCE_HEIGHT;
    }
    
    public boolean isVisible() {
        return active;
    }
    
    public String getSymbol() {
        if (type == IndicatorType.AVAILABLE) {
            return "!";
        } else if (type == IndicatorType.COMPLETE) {
            return "?";
        } else if (type == IndicatorType.IN_PROGRESS) {
            return "...";
        } else {
            return "!";
        }
    }
    
    public java.awt.Color getColor() {
        if (type == IndicatorType.AVAILABLE) {
            return new java.awt.Color(255, 215, 0);
        } else if (type == IndicatorType.COMPLETE) {
            return new java.awt.Color(255, 215, 0);
        } else if (type == IndicatorType.IN_PROGRESS) {
            return new java.awt.Color(150, 150, 150);
        } else {
            return new java.awt.Color(255, 215, 0);
        }
    }
}