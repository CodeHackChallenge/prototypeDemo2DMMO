package dev.main.render;

public enum RenderLayer {
    GROUND(0),           // Tiles, ground effects
    GROUND_DECOR(1),     // Shadows, ground markers
    ENTITIES(2),         // Players, monsters, NPCs
    EFFECTS(3),          // Particles, spell effects
    UI_WORLD(4),         // Health bars, name tags, alerts (world-space UI)
    UI_SCREEN(5);        // Screen-space UI (minimap, inventory, etc.)
    
    public final int priority;
    
    RenderLayer(int priority) {
        this.priority = priority;
    }
}