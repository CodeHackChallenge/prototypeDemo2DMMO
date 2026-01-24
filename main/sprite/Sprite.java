package dev.main.sprite;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import dev.main.input.Component;

/**
 * OPTIMIZED: Cached current animation to eliminate HashMap lookups every frame
 */
public class Sprite implements Component {
    // Idle animation constants
    public static final String ANIM_IDLE_DOWN = "idle_down";
    public static final String ANIM_IDLE_UP = "idle_up";
    public static final String ANIM_IDLE_LEFT = "idle_left";
    public static final String ANIM_IDLE_RIGHT = "idle_right";
    public static final String ANIM_IDLE_DOWN_LEFT = "idle_down_left";
    public static final String ANIM_IDLE_DOWN_RIGHT = "idle_down_right";
    public static final String ANIM_IDLE_UP_LEFT = "idle_up_left";
    public static final String ANIM_IDLE_UP_RIGHT = "idle_up_right";
     
    //victory idle
    public static final String ANIM_VICTORY_IDLE_DOWN = "victory_idle_down";
    public static final String ANIM_VICTORY_IDLE_UP = "victory_idle_up";
    public static final String ANIM_VICTORY_IDLE_LEFT = "victory_idle_left";
    public static final String ANIM_VICTORY_IDLE_RIGHT = "victory_idle_right";
    public static final String ANIM_VICTORY_IDLE_DOWN_LEFT = "victory_idle_down_left";
    public static final String ANIM_VICTORY_IDLE_DOWN_RIGHT = "victory_idle_down_right";
    public static final String ANIM_VICTORY_IDLE_UP_LEFT = "victory_idle_up_left";
    public static final String ANIM_VICTORY_IDLE_UP_RIGHT = "victory_idle_up_right";
    
    // Walk
    public static final String ANIM_IDLE = "idle";
    public static final String ANIM_WALK_DOWN = "walk_down";
    public static final String ANIM_WALK_UP = "walk_up";
    public static final String ANIM_WALK_LEFT = "walk_left";
    public static final String ANIM_WALK_RIGHT = "walk_right";
    public static final String ANIM_WALK_DOWN_LEFT = "walk_down_left";
    public static final String ANIM_WALK_DOWN_RIGHT = "walk_down_right";
    public static final String ANIM_WALK_UP_LEFT = "walk_up_left";
    public static final String ANIM_WALK_UP_RIGHT = "walk_up_right";
    public static final String ANIM_ATTACK = "attack";
    public static final String ANIM_DEAD = "dead";
        
    // Run
    public static final String ANIM_RUN_DOWN = "run_down";
    public static final String ANIM_RUN_UP = "run_up";
    public static final String ANIM_RUN_LEFT = "run_left";
    public static final String ANIM_RUN_RIGHT = "run_right";
    public static final String ANIM_RUN_DOWN_LEFT = "run_down_left";
    public static final String ANIM_RUN_DOWN_RIGHT = "run_down_right";
    public static final String ANIM_RUN_UP_LEFT = "run_up_left";
    public static final String ANIM_RUN_UP_RIGHT = "run_up_right";
    
    //attack

    public static final String ANIM_ATTACK_DOWN = "attack_down";
    public static final String ANIM_ATTACK_UP = "attack_up";
    public static final String ANIM_ATTACK_LEFT = "attack_left";
    public static final String ANIM_ATTACK_RIGHT = "attack_right";
    public static final String ANIM_ATTACK_DOWN_LEFT = "attack_down_left";
    public static final String ANIM_ATTACK_DOWN_RIGHT = "attack_down_right";
    public static final String ANIM_ATTACK_UP_LEFT = "attack_up_left";
    public static final String ANIM_ATTACK_UP_RIGHT = "attack_up_right";
    
    private BufferedImage spriteSheet;
    private int frameWidth;
    private int frameHeight;
    
    // Animation state
    private int currentFrame;
    private float animationTimer;
    private float frameDuration;
    
    // Current animation
    private String currentAnimation;
    private boolean loopAnimation;
    private Animation cachedAnimation;  // ⭐ NEW: Cache current animation
    private boolean isStatic; // true for single-image sprites (no animation)
    
    // Animation definitions
    private Map<String, Animation> animations;
    
    
    // Inner class to hold animation data
    private static class Animation {
        int row;
        int frameCount;
        
        Animation(int row, int frameCount) {
            this.row = row;
            this.frameCount = frameCount;
        }
    }
    
    public Sprite(String spriteSheetPath, int frameWidth, int frameHeight, float frameDuration) {
        this.spriteSheet = TextureManager.load(spriteSheetPath);
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameDuration = frameDuration;
        this.currentFrame = 0;
        this.animationTimer = 0;
        this.animations = new HashMap<>();
        this.loopAnimation = true;
        this.cachedAnimation = null;  // ⭐ NEW
        this.isStatic = frameDuration <= 0f;

        if (!this.isStatic) {
            setupAnimations();
            // Start with idle animation
            this.currentAnimation = ANIM_IDLE_DOWN;
            this.cachedAnimation = animations.get(this.currentAnimation);  // ⭐ NEW: Cache initial
        } else {
            // Single-image sprite: use first row, single frame
            this.currentAnimation = null;
            this.cachedAnimation = new Animation(0, 1);
            this.loopAnimation = true;
        }
    }
     
    
    // ⭐ OPTIMIZED: Use cached animation instead of HashMap lookup
    public void update(float delta) {
        if (isStatic) return; // no animation update for static sprites
        if (cachedAnimation == null) return;  // ⭐ Use cache
        
        animationTimer += delta;
        
        if (animationTimer >= frameDuration) {
            animationTimer -= frameDuration;
            
            if (loopAnimation) {
                currentFrame = (currentFrame + 1) % cachedAnimation.frameCount;  // ⭐ Use cache
            } else {
                if (currentFrame < cachedAnimation.frameCount - 1) {  // ⭐ Use cache
                    currentFrame++;
                }
            }
        }
    }
    
    // ⭐ OPTIMIZED: Use cached animation
    public void renderAtPixel(Graphics2D g, int screenX, int screenY) {
        if (spriteSheet == null || cachedAnimation == null) return;  // ⭐ Use cache
        
        int srcX = currentFrame * frameWidth;
        int srcY = cachedAnimation.row * frameHeight;  // ⭐ Use cache
        
        int destX = screenX - frameWidth / 2;
        int destY = screenY - frameHeight / 2;
        
        g.drawImage(
            spriteSheet,
            destX, destY, destX + frameWidth, destY + frameHeight,
            srcX, srcY, srcX + frameWidth, srcY + frameHeight,
            null
        );
    }
    
    public void render(Graphics2D g, float x, float y, float cameraX, float cameraY) {
        if (spriteSheet == null || cachedAnimation == null) return;  // ⭐ Use cache
        
        int srcX = currentFrame * frameWidth;
        int srcY = cachedAnimation.row * frameHeight;  // ⭐ Use cache
        
        int destX = (int)Math.round(x - cameraX - frameWidth / 2f);
        int destY = (int)Math.round(y - cameraY - frameHeight / 2f);
        
        g.drawImage(
            spriteSheet,
            destX, destY, destX + frameWidth, destY + frameHeight,
            srcX, srcY, srcX + frameWidth, srcY + frameHeight,
            null
        );
    }
    
    // ⭐ OPTIMIZED: Cache animation on change
    public void setAnimation(String animationName) {
        if (animationName == null) return;
        if (animationName.equals(currentAnimation)) return;

        this.currentAnimation = animationName;
        Animation a = animations.get(animationName);
        if (a != null) {
            this.cachedAnimation = a;
        } else {
            // requested animation not found
            if (isStatic) {
                // ignore for static sprites
                return;
            } else {
                // fallback to a single-frame at row 0
                this.cachedAnimation = new Animation(0, 1);
            }
        }
        this.currentFrame = 0;
        this.animationTimer = 0;
        this.loopAnimation = !"dead".equals(animationName);
    }
    
    public boolean isAnimationFinished() {
        if (isStatic) return true;
        if (cachedAnimation == null) return true;  // ⭐ Use cache
        return !loopAnimation && currentFrame >= cachedAnimation.frameCount - 1;  // ⭐ Use cache
    }
    
    private void setupAnimations() {
        // Use helper to add animations so row/frameCount are explicit and centralized
        
    	addAnimation(ANIM_IDLE_DOWN, 0, 8);
        addAnimation(ANIM_IDLE_UP, 2, 8);
        addAnimation(ANIM_IDLE_LEFT, 1, 8);
        addAnimation(ANIM_IDLE_RIGHT, 0, 8);
        addAnimation(ANIM_IDLE_DOWN_LEFT, 1, 8);
        addAnimation(ANIM_IDLE_DOWN_RIGHT, 0, 8);
        addAnimation(ANIM_IDLE_UP_LEFT, 3, 8);
        addAnimation(ANIM_IDLE_UP_RIGHT, 2, 8);
        
        // Walk animations
        addAnimation(ANIM_WALK_DOWN, 4, 4);
        addAnimation(ANIM_WALK_UP, 6, 4);
        addAnimation(ANIM_WALK_LEFT, 4, 4);
        addAnimation(ANIM_WALK_RIGHT, 5, 4);
        addAnimation(ANIM_WALK_DOWN_LEFT, 4, 4);
        addAnimation(ANIM_WALK_DOWN_RIGHT, 5, 4);
        addAnimation(ANIM_WALK_UP_LEFT, 7, 4);
        addAnimation(ANIM_WALK_UP_RIGHT, 6, 4);
        // Victory idle animations
        addAnimation(ANIM_VICTORY_IDLE_DOWN, 13, 3);
        addAnimation(ANIM_VICTORY_IDLE_UP, 13, 3);
        addAnimation(ANIM_VICTORY_IDLE_LEFT, 14, 3);
        addAnimation(ANIM_VICTORY_IDLE_RIGHT, 13, 4);
        addAnimation(ANIM_VICTORY_IDLE_DOWN_LEFT, 14, 3);
        addAnimation(ANIM_VICTORY_IDLE_DOWN_RIGHT, 13, 3);
        addAnimation(ANIM_VICTORY_IDLE_UP_LEFT, 15, 3);
        addAnimation(ANIM_VICTORY_IDLE_UP_RIGHT, 16, 3);
        
        // Run animations
        addAnimation(ANIM_RUN_DOWN, 4, 4);
        addAnimation(ANIM_RUN_UP, 6, 4);
        addAnimation(ANIM_RUN_LEFT, 4, 4);
        addAnimation(ANIM_RUN_RIGHT, 5, 4);
        addAnimation(ANIM_RUN_DOWN_LEFT, 4, 4);
        addAnimation(ANIM_RUN_DOWN_RIGHT, 5, 4);
        addAnimation(ANIM_RUN_UP_LEFT, 7, 4);
        addAnimation(ANIM_RUN_UP_RIGHT, 6, 4);
        
        //attack
        addAnimation(ANIM_ATTACK_DOWN, 8, 5);
        addAnimation(ANIM_ATTACK_UP, 10, 5);
        addAnimation(ANIM_ATTACK_LEFT, 9, 5);
        addAnimation(ANIM_ATTACK_RIGHT, 8, 5);
        addAnimation(ANIM_ATTACK_DOWN_LEFT, 9, 5);
        addAnimation(ANIM_ATTACK_DOWN_RIGHT, 8, 5);
        addAnimation(ANIM_ATTACK_UP_LEFT, 11, 5);
        addAnimation(ANIM_ATTACK_UP_RIGHT, 10, 5);
        /*
        // Run animations
        addAnimation(ANIM_RUN_DOWN, 40, 8);
        addAnimation(ANIM_RUN_UP, 38, 8);
        addAnimation(ANIM_RUN_LEFT, 39, 8);
        addAnimation(ANIM_RUN_RIGHT, 41, 8);
        addAnimation(ANIM_RUN_DOWN_LEFT, 39, 8);
        addAnimation(ANIM_RUN_DOWN_RIGHT, 41, 8);
        addAnimation(ANIM_RUN_UP_LEFT, 39, 8);
        addAnimation(ANIM_RUN_UP_RIGHT, 41, 8);
        
        // Idle animations 
        /*addAnimation(ANIM_IDLE_DOWN, 24, 2);
        addAnimation(ANIM_IDLE_UP, 22, 2);
        addAnimation(ANIM_IDLE_LEFT, 23, 2);
        addAnimation(ANIM_IDLE_RIGHT, 25, 2);
        addAnimation(ANIM_IDLE_DOWN_LEFT, 23, 2);
        addAnimation(ANIM_IDLE_DOWN_RIGHT, 25, 2);
        addAnimation(ANIM_IDLE_UP_LEFT, 23, 2);
        addAnimation(ANIM_IDLE_UP_RIGHT, 25, 2);
         */
        /*
        // Walk animations
        addAnimation(ANIM_WALK_DOWN, 10, 9);
        addAnimation(ANIM_WALK_UP, 8, 9);
        addAnimation(ANIM_WALK_LEFT, 9, 9);
        addAnimation(ANIM_WALK_RIGHT, 11, 9);
        addAnimation(ANIM_WALK_DOWN_LEFT, 9, 9);
        addAnimation(ANIM_WALK_DOWN_RIGHT, 11, 9);
        addAnimation(ANIM_WALK_UP_LEFT, 9, 9);
        addAnimation(ANIM_WALK_UP_RIGHT, 11, 9);
		 
        
        

        addAnimation(ANIM_ATTACK_DOWN, 14, 6);
        addAnimation(ANIM_ATTACK_UP, 12, 6);
        addAnimation(ANIM_ATTACK_LEFT, 13, 6);
        addAnimation(ANIM_ATTACK_RIGHT, 15, 6);
        addAnimation(ANIM_ATTACK_DOWN_LEFT, 13, 6);
        addAnimation(ANIM_ATTACK_DOWN_RIGHT, 15, 6);
        addAnimation(ANIM_ATTACK_UP_LEFT, 13, 6);
        addAnimation(ANIM_ATTACK_UP_RIGHT, 15, 6);
	*/
        addAnimation(ANIM_DEAD, 12, 3);
    }

    // Helper to add animation entries with flexible row and frame count
    private void addAnimation(String name, int row, int frameCount) {
        animations.put(name, new Animation(row, frameCount));
    }

}