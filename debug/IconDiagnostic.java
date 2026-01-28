package dev.main.debug;

import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Diagnostic tool to check icon loading
 * Run this to verify your icons are accessible
 */
public class IconDiagnostic {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║    ICON LOADING DIAGNOSTIC TOOL        ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println();
        
        // Test paths
        String[] testPaths = {
            "/items/icons/wooden_short_sword.png",
            "/items/icons/health_potion.png",
            "/items/icons/fire_rune.png",
            "/items/icons/iron_sword.png"
        };
        
        System.out.println("Testing icon paths...");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        for (String path : testPaths) {
            testIconPath(path);
        }
        
        System.out.println();
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║    RESOURCE FOLDER CHECK               ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println();
        
        checkResourceFolder();
    }
    
    private static void testIconPath(String path) {
        System.out.println("\n📁 Testing: " + path);
        
        // Method 1: getResource
        URL url = IconDiagnostic.class.getResource(path);
        if (url != null) {
            System.out.println("   ✅ getResource() found: " + url);
        } else {
            System.out.println("   ❌ getResource() returned null");
        }
        
        // Method 2: getResourceAsStream
        InputStream stream = IconDiagnostic.class.getResourceAsStream(path);
        if (stream != null) {
            System.out.println("   ✅ getResourceAsStream() found stream");
            try {
                BufferedImage img = ImageIO.read(stream);
                if (img != null) {
                    System.out.println("   ✅ ImageIO.read() successful!");
                    System.out.println("      Size: " + img.getWidth() + "x" + img.getHeight());
                } else {
                    System.out.println("   ❌ ImageIO.read() returned null");
                }
                stream.close();
            } catch (Exception e) {
                System.out.println("   ❌ Error reading image: " + e.getMessage());
            }
        } else {
            System.out.println("   ❌ getResourceAsStream() returned null");
        }
        
        // Check common path variations
        System.out.println("   ℹ️  Checking variations:");
        checkPathVariation("   ", path.substring(1)); // Remove leading /
        checkPathVariation("   ", "/" + path); // Add extra /
        checkPathVariation("   ", path.replace("/items/", "/resources/items/"));
    }
    
    private static void checkPathVariation(String prefix, String path) {
        URL url = IconDiagnostic.class.getResource(path);
        if (url != null) {
            System.out.println(prefix + "   ✅ Found at: " + path);
        }
    }
    
    private static void checkResourceFolder() {
        String[] folders = {
            "/items",
            "/items/icons",
            "/ui",
            "/ui/icons",
            "/icon"
        };
        
        for (String folder : folders) {
            URL url = IconDiagnostic.class.getResource(folder);
            if (url != null) {
                System.out.println("✅ Folder exists: " + folder);
                System.out.println("   Path: " + url);
            } else {
                System.out.println("❌ Folder NOT found: " + folder);
            }
        }
        
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("RECOMMENDATIONS:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("1. Check your project structure:");
        System.out.println("   src/main/resources/items/icons/*.png");
        System.out.println("   OR");
        System.out.println("   resources/items/icons/*.png");
        System.out.println();
        System.out.println("2. Verify PNG files are in the right location");
        System.out.println();
        System.out.println("3. Make sure resources folder is marked as");
        System.out.println("   'Resources Root' in your IDE");
        System.out.println();
        System.out.println("4. Clean and rebuild your project");
        System.out.println();
    }
}