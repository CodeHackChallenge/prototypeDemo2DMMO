package dev.main.bars;

import java.awt.Color;

import dev.main.input.Component;

public class ManaBar implements Component {
    public int width;
    public int height;
    public int offsetY;  // Distance below sprite
    
    // Colors
    public static final Color MANA_COLOR = new Color(100, 149, 237);  // Cornflower blue
    public static final Color BG_COLOR = new Color(40, 40, 40);
    
    public ManaBar(int width, int height, int offsetY) {
        this.width = width;
        this.height = height;
        this.offsetY = offsetY;
    }
}