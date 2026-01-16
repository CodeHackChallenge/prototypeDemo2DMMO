package dev.main.util; 

import java.awt.Color;

import dev.main.input.Component;

public class XPBar implements Component {
    public int width;
    public int height;
    public int offsetY;  // Distance below sprite
    
    // Colors
    public static final Color XP_COLOR = new Color(255, 215, 0);  // Gold
    public static final Color BG_COLOR = new Color(40, 40, 40);
    
    public XPBar(int width, int height, int offsetY) {
        this.width = width;
        this.height = height;
        this.offsetY = offsetY;
    }
}