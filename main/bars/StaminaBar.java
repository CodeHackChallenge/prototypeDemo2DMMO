package dev.main.bars;

import java.awt.Color;

import dev.main.input.Component;

public class StaminaBar implements Component {
    public int width;
    public int height;
    public int offsetY;  // Distance below sprite
    
    // Colors
    public static final Color STAMINA_COLOR = new Color(205, 133, 63);  // Light brown/peru
    public static final Color BG_COLOR = new Color(60, 60, 60);
    
    public StaminaBar(int width, int height, int offsetY) {
        this.width = width;
        this.height = height;
        this.offsetY = offsetY;
    }
}