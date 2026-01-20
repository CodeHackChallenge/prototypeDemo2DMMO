package dev.main.quest;

import dev.main.input.Component;

/**
 * Quest indicator - shows animated symbol above NPC based on quest status
 * ★ REFACTORED: Added change tracking to debug unexpected resets
 */
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
        
        // ★ DEBUG: Track creation
        System.out.println("[QuestIndicator] NEW instance created: " + this);
        System.out.println("  Initial type: " + this.type);
        printStackTrace();
    }
    
    /**
     * ★ Show indicator with specific type
     */
    public void show(IndicatorType type) {
        System.out.println("[QuestIndicator] show() called on " + this);
        System.out.println("  Changing from: " + this.type + " to: " + type);
        printStackTrace();
        
        boolean wasActive = this.active;
        IndicatorType oldType = this.type;
        
        this.active = true;
        this.type = type;
        
        if (!wasActive || oldType != type) {
            this.animationTimer = 0;
            this.bounceOffset = 0;
        }
        
        System.out.println("  After change: " + this.type);
    }
    
    /**
     * ★ Hide indicator
     */
    public void hide() {
        System.out.println("[QuestIndicator] hide() called on " + this);
        System.out.println("  Was: " + this.type);
        printStackTrace();
        
        this.active = false;
        this.animationTimer = 0;
        this.bounceOffset = 0;
    }
    
    /**
     * ★ Update animation
     */
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
    
    /**
     * ★ DEBUG: Print stack trace to see who's calling
     */
    private void printStackTrace() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        System.out.println("  Called from:");
        
        // Print first 5 stack frames (skip getStackTrace and printStackTrace itself)
        for (int i = 3; i < Math.min(8, stack.length); i++) {
            System.out.println("    " + stack[i]);
        }
    }
}