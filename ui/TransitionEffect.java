package dev.main.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;

import dev.main.Engine;

/**
 * Screen fade transition effect for map changes
 */
public class TransitionEffect {
    
    public enum TransitionState {
        IDLE,           // No transition
        FADE_OUT,       // Fading to black
        LOADING,        // Fully black (load new map here)
        FADE_IN         // Fading back from black
    }
    
    private TransitionState state;
    private float progress;
    private float fadeSpeed;
    
    // Callbacks
    private Runnable onFadeComplete;
    private Runnable onLoadPoint;
    
    public TransitionEffect() {
        this.state = TransitionState.IDLE;
        this.progress = 0f;
        this.fadeSpeed = 2.0f;  // Default: 0.5 seconds per fade
    }
    
    /**
     * Start a portal transition
     * @param onLoadPoint - Called when screen is fully black (load new map here)
     * @param onFadeComplete - Called when transition fully completes
     */
    public void startPortalTransition(Runnable onLoadPoint, Runnable onFadeComplete) {
        this.state = TransitionState.FADE_OUT;
        this.progress = 0f;
        this.onLoadPoint = onLoadPoint;
        this.onFadeComplete = onFadeComplete;
        
        System.out.println("🎬 Transition started: FADE_OUT");
    }
    
    /**
     * Update transition animation
     */
    public void update(float delta) {
        if (state == TransitionState.IDLE) return;
        
        switch (state) {
            case FADE_OUT:
                progress += delta * fadeSpeed;
                if (progress >= 1.0f) {
                    progress = 1.0f;
                    state = TransitionState.LOADING;
                    System.out.println("🎬 Transition: LOADING (calling onLoadPoint)");
                    
                    // Load new map at darkest point
                    if (onLoadPoint != null) {
                        onLoadPoint.run();
                    }
                    
                    // Brief pause at full black
                    progress = 0f;
                }
                break;
                
            case LOADING:
                // Brief pause (0.2 seconds)
                progress += delta * fadeSpeed;
                if (progress >= 0.2f) {
                    progress = 0f;
                    state = TransitionState.FADE_IN;
                    System.out.println("🎬 Transition: FADE_IN");
                }
                break;
                
            case FADE_IN:
                progress += delta * fadeSpeed;
                if (progress >= 1.0f) {
                    progress = 1.0f;
                    state = TransitionState.IDLE;
                    System.out.println("🎬 Transition: COMPLETE");
                    
                    // Transition complete
                    if (onFadeComplete != null) {
                        onFadeComplete.run();
                    }
                }
                break;
                
            case IDLE:
                // Do nothing
                break;
        }
    }
    
    /**
     * Render the fade overlay
     */
    public void render(Graphics2D g) {
        if (state == TransitionState.IDLE) return;
        
        float alpha = 0f;
        
        switch (state) {
            case FADE_OUT:
                // Fade from transparent to black
                alpha = progress;
                break;
                
            case LOADING:
                // Fully black
                alpha = 1.0f;
                break;
                
            case FADE_IN:
                // Fade from black to transparent
                alpha = 1.0f - progress;
                break;
                
            case IDLE:
                return;
        }
        
        // Draw black overlay with alpha
        AlphaComposite originalComposite = (AlphaComposite) g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, Engine.WIDTH, Engine.HEIGHT);
        g.setComposite(originalComposite);
    }
    
    /**
     * Check if transition is currently active
     */
    public boolean isActive() {
        return state != TransitionState.IDLE;
    }
    
    /**
     * Get current state
     */
    public TransitionState getState() {
        return state;
    }
    
    /**
     * Force stop transition (emergency)
     */
    public void reset() {
        state = TransitionState.IDLE;
        progress = 0f;
        System.out.println("🎬 Transition: RESET");
    }
    
    /**
     * Set fade speed (higher = faster)
     */
    public void setFadeSpeed(float speed) {
        this.fadeSpeed = speed;
    }
}