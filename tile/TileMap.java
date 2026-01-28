package dev.main.tile;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import dev.main.Engine;
import dev.main.input.CollisionBox;
import dev.main.sprite.TextureManager;
import dev.main.util.JsonMapParser;
import dev.main.util.MapData;

public class TileMap {
    
    public static final int TILE_SIZE = 64;
    
    private int width;   // Map width in tiles
    private int height;  // Map height in tiles
    
    // Map rendering
    private BufferedImage mapImage;  // NEW: Full rendered map image
    
    // Collision data
    private int[][] collisionMap;  // NEW: 0 = walkable, 1 = solid
    
    private MapData mapData;
    
    public TileMap(String mapImagePath, String collisionMapPath) {
        loadMapImage(mapImagePath);
        loadCollisionMap(collisionMapPath);
    }
    
    public TileMap(String jsonMapPath) {
        MapData data = JsonMapParser.parse(jsonMapPath);
        
        if (data != null) {
            this.width = data.width;
            this.height = data.height;
            this.collisionMap = data.tiles;
            
            System.out.println("JSON map loaded: " + data.mapId);
            System.out.println("  Size: " + width + "x" + height + " tiles");
            System.out.println("  Portals: " + data.portals.size());
            System.out.println("  Spawns: " + data.monsterSpawns.size());
            
            // Store for later use
            storeMapData(data);
            
            // Load map image based on mapId
            String imagePath = "/maps/" + data.mapId + ".png";
            loadMapImage(imagePath);
        } else {
            System.err.println("Failed to load JSON map, using defaults");
            width = 50;
            height = 50;
            createEmptyCollisionMap();
        }
    }
    /**
     * Load the full map image
     */
    private void loadMapImage(String path) {
        mapImage = TextureManager.load(path);
        
        if (mapImage != null) {
            width = mapImage.getWidth() / TILE_SIZE;
            height = mapImage.getHeight() / TILE_SIZE;
            System.out.println("Map image loaded: " + width + "x" + height + " tiles (" + 
                             mapImage.getWidth() + "x" + mapImage.getHeight() + " pixels)");
        } else {
            System.err.println("Failed to load map image: " + path);
            width = 50;
            height = 50;
        }
    }
    
    /**
     * Load collision map from text file
     * Format:
     * Line 1: width height
     * Remaining lines: 0 (walkable) or 1 (solid)
     */
    private void loadCollisionMap(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                System.err.println("Collision map not found: " + path);
                createEmptyCollisionMap();
                return;
            }
            
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            
            // First line: width height
            String[] dimensions = br.readLine().trim().split(" ");
            int colWidth = Integer.parseInt(dimensions[0]);
            int colHeight = Integer.parseInt(dimensions[1]);
            
            // Verify dimensions match map image
            if (colWidth != width || colHeight != height) {
                System.err.println("WARNING: Collision map size (" + colWidth + "x" + colHeight + 
                                 ") doesn't match map image (" + width + "x" + height + ")");
            }
            
            collisionMap = new int[colHeight][colWidth];
            
            // Read collision data
            for (int row = 0; row < colHeight; row++) {
                String line = br.readLine();
                if (line == null) break;
                
                String[] values = line.trim().split(" ");
                for (int col = 0; col < colWidth && col < values.length; col++) {
                    collisionMap[row][col] = Integer.parseInt(values[col]);
                }
            }
            
            br.close();
            System.out.println("Collision map loaded: " + colWidth + "x" + colHeight);
            
        } catch (IOException e) {
            System.err.println("Failed to load collision map: " + path);
            e.printStackTrace();
            createEmptyCollisionMap();
        }
    }
    
    /**
     * Create empty collision map (all walkable) as fallback
     */
    private void createEmptyCollisionMap() {
        collisionMap = new int[height][width];
        System.out.println("Created empty collision map: " + width + "x" + height + " (all walkable)");
    }
    
    /**
     * Render the map - now just draws the image in view
     */
    public void render(Graphics2D g, float cameraX, float cameraY) {
        if (mapImage == null) return;
        
        // Calculate which part of the map to draw
        int srcX = (int)cameraX;
        int srcY = (int)cameraY;
        int srcWidth = Math.min(Engine.WIDTH, mapImage.getWidth() - srcX);
        int srcHeight = Math.min(Engine.HEIGHT, mapImage.getHeight() - srcY);
        
        // Clamp to map bounds
        srcX = Math.max(0, Math.min(srcX, mapImage.getWidth() - Engine.WIDTH));
        srcY = Math.max(0, Math.min(srcY, mapImage.getHeight() - Engine.HEIGHT));
        
        // Draw the visible portion of the map
        g.drawImage(
            mapImage,
            0, 0, Engine.WIDTH, Engine.HEIGHT,  // Destination (screen)
            srcX, srcY, srcX + Engine.WIDTH, srcY + Engine.HEIGHT,  // Source (map region)
            null
        );
    }
    
    public boolean isSolid(int tileX, int tileY) {
        // Out of bounds = solid
        if (tileX < 0 || tileX >= width || tileY < 0 || tileY >= height) {
            return true;
        }
        
        // ★ NEW: Extra safety check for collision map bounds
        if (collisionMap == null) {
            System.err.println("⚠ Collision map is null!");
            return true;
        }
        
        if (tileY >= collisionMap.length) {
            System.err.println("⚠ Row " + tileY + " exceeds collision map height " + collisionMap.length);
            return true;
        }
        
        if (tileX >= collisionMap[tileY].length) {
            System.err.println("⚠ Col " + tileX + " exceeds collision map width " + collisionMap[tileY].length);
            return true;
        }
        
        // Check collision map
        return collisionMap[tileY][tileX] == 1;
    }
    
    public boolean isSolidAtWorldPos(float worldX, float worldY) {
        int tileX = (int)(worldX / TILE_SIZE);
        int tileY = (int)(worldY / TILE_SIZE);
        return isSolid(tileX, tileY);
    }
    
    /**
     * Check if a collision box collides with any solid tiles
     */
    public boolean collidesWithTiles(CollisionBox box, float entityX, float entityY) {
        float left = box.getLeft(entityX);
        float right = box.getRight(entityX);
        float top = box.getTop(entityY);
        float bottom = box.getBottom(entityY);
        
        int startTileX = (int)(left / TILE_SIZE);
        int endTileX = (int)(right / TILE_SIZE);
        int startTileY = (int)(top / TILE_SIZE);
        int endTileY = (int)(bottom / TILE_SIZE);
        
        for (int ty = startTileY; ty <= endTileY; ty++) {
            for (int tx = startTileX; tx <= endTileX; tx++) {
                if (isSolid(tx, ty)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void storeMapData(MapData data) {
    	System.out.println("::::::inside storeMapData()="+data);
        this.mapData = data;
    }

    // ★ ADD GETTERS for accessing map data
    public MapData getMapData() {
        return mapData;
    }
    
    public List<MapData.Portal> getPortals() {
        return mapData != null ? mapData.portals : new ArrayList<>();
    }

    public List<MapData.MonsterSpawn> getMonsterSpawns() {
        return mapData != null ? mapData.monsterSpawns : new ArrayList<>();
    }
    
    public int getWidth() { 
        return width; 
    }
    
    public int getHeight() { 
        return height; 
    }
    
    public int getWidthInPixels() { 
        return width * TILE_SIZE; 
    }
    
    public int getHeightInPixels() { 
        return height * TILE_SIZE; 
    }
}