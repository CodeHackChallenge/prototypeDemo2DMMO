package dev.main.sprite;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import dev.main.ui.UIIconGenerator;

public class TextureManager {

    private static final Map<String, BufferedImage> cache = new HashMap<>();

    // Load and cache an image
    public static BufferedImage load(String path) {
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        try {
            BufferedImage img = ImageIO.read(TextureManager.class.getResourceAsStream(path));
            cache.put(path, img);
            return img;
        } catch (IOException | IllegalArgumentException | NullPointerException e) {
            // ☆ NEW: Generate placeholder items if file not found
            if (path.contains("/ui/icons/")) {
                System.out.println("Icon not found: " + path + " - using placeholder");
                return generatePlaceholderIcon(path);
            }
            
            System.err.println("Failed to load texture: " + path);
            return null;
        }
    }
    
    /**
     * ☆ NEW: Generate placeholder items for missing UI icons
     */
    private static BufferedImage generatePlaceholderIcon(String path) {
        // Extract button type from path
        String filename = path.substring(path.lastIndexOf('/') + 1);
        String buttonType = filename.replace(".png", "")
                                     .replace("_hover", "")
                                     .replace("_locked", "");
        
        // Determine items properties
        int size = 48;
        String letter = buttonType.substring(0, 1).toUpperCase();
        
        java.awt.Color color;
        
        // Assign colors based on button type
        switch (buttonType) {
            case "settings":
                color = new java.awt.Color(120, 120, 120);
                break;
            case "world":
                color = new java.awt.Color(100, 180, 100);
                break;
            case "trade":
                color = new java.awt.Color(220, 180, 60);
                break;
            case "message":
                color = new java.awt.Color(100, 150, 220);
                break;
            case "quest":
                color = new java.awt.Color(200, 150, 100);
                break;
            case "stats":
                color = new java.awt.Color(200, 100, 100);
                break;
            case "character":
                color = new java.awt.Color(150, 100, 200);
                break;
            case "skilltree":
                color = new java.awt.Color(100, 200, 150);
                break;
            case "gear":
                color = new java.awt.Color(180, 140, 100);
                break;
            case "inventory":
                color = new java.awt.Color(140, 100, 60);
                letter = "I";
                break;
            default:
                color = new java.awt.Color(100, 100, 100);
        }
        
        // Generate appropriate items type
        BufferedImage icon;
        if (path.contains("_locked")) {
            icon = UIIconGenerator.generateLockedIcon(size, letter);
        } else if (path.contains("_hover")) {
            icon = UIIconGenerator.generateHoverIcon(size, color, letter);
        } else {
            icon = UIIconGenerator.generateIcon(size, color, letter);
        }
        
        // Cache the generated items
        cache.put(path, icon);
        
        return icon;
    }

    // Optional: clear cache (useful for dev reloads)
    public static void clear() {
        cache.clear();
    }
}