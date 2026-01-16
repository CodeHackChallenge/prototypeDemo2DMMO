package dev.main.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Generates placeholder UI icons programmatically
 * Replace with actual items images later
 */
public class UIIconGenerator {
    
    /**
     * Generate a simple colored items
     */
    public static BufferedImage generateIcon(int size, Color color, String letter) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Draw background
        g.setColor(color);
        g.fillRoundRect(0, 0, size, size, 8, 8);
        
        // Draw border
        g.setColor(Color.WHITE);
        g.drawRoundRect(0, 0, size - 1, size - 1, 8, 8);
        
        // Draw letter
        if (letter != null && !letter.isEmpty()) {
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, size / 2));
            java.awt.FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(letter);
            int textHeight = fm.getHeight();
            int x = (size - textWidth) / 2;
            int y = (size + textHeight / 2) / 2;
            
            g.setColor(Color.WHITE);
            g.drawString(letter, x, y);
        }
        
        g.dispose();
        return img;
    }
    
    /**
     * Generate hover version (brighter)
     */
    public static BufferedImage generateHoverIcon(int size, Color color, String letter) {
        Color brighter = color.brighter();
        return generateIcon(size, brighter, letter);
    }
    
    /**
     * Generate locked version (grayscale)
     */
    public static BufferedImage generateLockedIcon(int size, String letter) {
        Color gray = new Color(80, 80, 80);
        return generateIcon(size, gray, letter);
    }
}