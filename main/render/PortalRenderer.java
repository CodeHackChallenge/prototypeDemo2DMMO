package dev.main.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

import dev.main.entity.Portal;

/**
 * Renders animated portal visuals
 */
public class PortalRenderer {
    
    public static void renderPortal(Graphics2D g, int screenX, int screenY, Portal portal) {
        if (!portal.isActive) return;
        
        // Save original composite
        AlphaComposite originalComposite = (AlphaComposite) g.getComposite();
        
        // Pulsing alpha effect
        float pulseAlpha = 0.3f + 0.2f * (float)Math.sin(portal.animationTimer);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulseAlpha));
        
        // Outer glow (blue)
        int outerRadius = 48;
        Point2D center = new Point2D.Float(screenX, screenY);
        float[] fractions = {0f, 0.5f, 1f};
        Color[] colors = {
            new Color(100, 150, 255, 200),
            new Color(50, 100, 200, 100),
            new Color(0, 50, 150, 0)
        };
        
        RadialGradientPaint gradient = new RadialGradientPaint(
            center, outerRadius, fractions, colors
        );
        
        g.setPaint(gradient);
        g.fillOval(screenX - outerRadius, screenY - outerRadius, 
                   outerRadius * 2, outerRadius * 2);
        
        // Inner core (bright white-blue)
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        int innerRadius = 24;
        Color[] innerColors = {
            new Color(200, 220, 255, 255),
            new Color(100, 150, 255, 100),
            new Color(50, 100, 200, 0)
        };
        
        RadialGradientPaint innerGradient = new RadialGradientPaint(
            center, innerRadius, fractions, innerColors
        );
        
        g.setPaint(innerGradient);
        g.fillOval(screenX - innerRadius, screenY - innerRadius, 
                   innerRadius * 2, innerRadius * 2);
        
        // Rotating particles
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        int numParticles = 8;
        int orbitRadius = 32;
        
        for (int i = 0; i < numParticles; i++) {
            float angle = portal.animationTimer + (i * (float)Math.PI * 2 / numParticles);
            int px = screenX + (int)(Math.cos(angle) * orbitRadius);
            int py = screenY + (int)(Math.sin(angle) * orbitRadius);
            
            g.setColor(new Color(150, 200, 255, 200));
            g.fillOval(px - 3, py - 3, 6, 6);
        }
        
        // Restore original composite
        g.setComposite(originalComposite);
    }
}
