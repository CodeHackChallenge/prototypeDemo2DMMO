package dev.main.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

public class DiamondRenderer {

    public static void renderDiamond(Graphics2D g, int screenX, int screenY, float scale, float alpha) {
        // Save original state
        AffineTransform originalTransform = g.getTransform();
        Stroke originalStroke = g.getStroke();  // NEW: Save stroke
        
        g.translate(screenX, screenY);
        g.scale(scale, scale);
        
        int size = 16 / 2;
        int[] xPoints = {0, size, 0, -size};
        int[] yPoints = {-size, 0, size, 0};
        Polygon diamond = new Polygon(xPoints, yPoints, 4);
        
        int alphaValue = (int)(alpha * 255);
        
        // Draw outer glow
        for (int i = 3; i > 0; i--) {
            int glowAlpha = (int)(alphaValue * 0.3f * (i / 3f));
            g.setColor(new Color(100, 200, 255, glowAlpha));
            g.setStroke(new BasicStroke(i * 2f));
            g.drawPolygon(diamond);
        }
        
        // Fill
        g.setColor(new Color(150, 220, 255, (int)(alphaValue * 0.6f)));
        g.fillPolygon(diamond);
        
        // Inner bright core
        int[] innerXPoints = {0, size/2, 0, -size/2};
        int[] innerYPoints = {-size/2, 0, size/2, 0};
        Polygon innerDiamond = new Polygon(innerXPoints, innerYPoints, 4);
        g.setColor(new Color(200, 240, 255, alphaValue));
        g.fillPolygon(innerDiamond);
        
        // Crystal highlights
        g.setColor(new Color(255, 255, 255, alphaValue));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(-size/4, -size/2, -size/4, -size/4);
        g.drawLine(size/3, -size/3, size/2, -size/4);
        
        // Sharp border
        g.setColor(new Color(80, 160, 220, alphaValue));
        g.setStroke(new BasicStroke(2f));
        g.drawPolygon(diamond);
        
        // Restore original state
        g.setStroke(originalStroke);  // NEW: Restore stroke
        g.setTransform(originalTransform);
    }
    
    public static void renderFlatDiamond(Graphics2D g, int screenX, int screenY, float scale, Color color) {
        AffineTransform originalTransform = g.getTransform();
        Stroke originalStroke = g.getStroke();  // NEW
        
        g.translate(screenX, screenY);
        g.scale(scale, scale);
        
        int size = 14 / 2;
        int[] xPoints = {0, size, 0, -size};
        int[] yPoints = {-size, 0, size, 0};
        Polygon diamond = new Polygon(xPoints, yPoints, 4);
        
        // Fill
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
        g.fillPolygon(diamond);
        
        // Border
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
        g.setStroke(new BasicStroke(2f));
        g.drawPolygon(diamond);
        
        g.setStroke(originalStroke);  // NEW
        g.setTransform(originalTransform);
    }
    
    public static void renderRotatingDiamond(Graphics2D g, int screenX, int screenY, float scale, float rotation) {
        AffineTransform originalTransform = g.getTransform();
        Stroke originalStroke = g.getStroke();  // NEW
        
        g.translate(screenX, screenY);
        g.rotate(rotation);
        g.scale(scale, scale);
        
        int size = 16 / 2;
        int[] xPoints = {0, size, 0, -size};
        int[] yPoints = {-size, 0, size, 0};
        Polygon diamond = new Polygon(xPoints, yPoints, 4);
        
        // Outer ring
        g.setColor(new Color(100, 200, 255, 100));
        g.setStroke(new BasicStroke(3f));
        g.drawPolygon(diamond);
        
        // Fill
        g.setColor(new Color(150, 220, 255, 150));
        g.fillPolygon(diamond);
        
        // Inner diamond
        int innerSize = size / 2;
        int[] innerX = {0, innerSize, 0, -innerSize};
        int[] innerY = {-innerSize, 0, innerSize, 0};
        Polygon inner = new Polygon(innerX, innerY, 4);
        g.setColor(new Color(200, 240, 255, 200));
        g.fillPolygon(inner);
        
        // Border
        g.setColor(new Color(80, 160, 220, 220));
        g.setStroke(new BasicStroke(2f));
        g.drawPolygon(diamond);
        
        g.setStroke(originalStroke);  // NEW
        g.setTransform(originalTransform);
    }
}