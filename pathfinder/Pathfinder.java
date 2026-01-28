package dev.main.pathfinder;

import java.util.*;

import dev.main.input.CollisionBox;
import dev.main.tile.TileMap;

/**
 * ★ IMPROVED: Collision-box-aware pathfinding
 * Now checks if the ENTIRE collision box fits, not just the center point
 */
public class Pathfinder {
    
    private TileMap map;
    private CollisionBox entityCollisionBox; // ★ NEW: Store entity's collision box
    
    private static final float DIAGONAL_COST = 1.414f;
    private static final float STRAIGHT_COST = 1.0f;
    
    public Pathfinder(TileMap map) {
        this.map = map;
    }
    
    /**
     * ★ NEW: Set the collision box to use for pathfinding
     * Call this before finding a path for entities with large collision boxes
     */
    public void setCollisionBox(CollisionBox box) {
        this.entityCollisionBox = box;
    }
    
    /**
     * ★ NEW: Clear collision box (for simple pathfinding)
     */
    public void clearCollisionBox() {
        this.entityCollisionBox = null;
    }
    
    /**
     * ★ IMPROVED: Find path considering entity's collision box
     */
    public List<int[]> findPath(int startX, int startY, int goalX, int goalY) {
        
        // Check if goal tile can fit the collision box
        if (entityCollisionBox != null) {
            float goalWorldX = goalX * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
            float goalWorldY = goalY * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
            
            if (map.collidesWithTiles(entityCollisionBox, goalWorldX, goalWorldY)) {
               // System.out.println("⚠ Goal tile can't fit collision box - trying nearby tiles...");
                
                // Try to find a nearby walkable tile
                int[] nearbyGoal = findNearestWalkableTile(goalX, goalY, 3);
                if (nearbyGoal != null) {
                    goalX = nearbyGoal[0];
                    goalY = nearbyGoal[1];
                   // System.out.println("✓ Using nearby tile: (" + goalX + ", " + goalY + ")");
                } else {
                    return null; // No nearby walkable tiles
                }
            }
        } else if (map.isSolid(goalX, goalY)) {
            return null;
        }
        
        if (startX == goalX && startY == goalY) {
            List<int[]> path = new ArrayList<>();
            path.add(new int[]{startX, startY});
            return path;
        }
        
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Set<PathNode> closedSet = new HashSet<>();
        Map<String, PathNode> nodeMap = new HashMap<>();
        
        PathNode startNode = new PathNode(startX, startY);
        PathNode goalNode = new PathNode(goalX, goalY);
        
        startNode.gCost = 0;
        startNode.hCost = heuristic(startNode, goalNode);
        startNode.calculateFCost();
        
        openSet.add(startNode);
        nodeMap.put(startX + "," + startY, startNode);
        
        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();
            
            if (current.x == goalX && current.y == goalY) {
                return reconstructPath(current);
            }
            
            closedSet.add(current);
            
            for (Neighbor neighbor : getValidNeighbors(current.x, current.y)) {
                int nx = neighbor.x;
                int ny = neighbor.y;
                
                // ★ IMPROVED: Check if collision box fits at this tile
                if (!canOccupyTile(nx, ny)) {
                    continue;
                }
                
                String key = nx + "," + ny;
                PathNode neighborNode = nodeMap.get(key);
                
                if (neighborNode == null) {
                    neighborNode = new PathNode(nx, ny);
                    nodeMap.put(key, neighborNode);
                }
                
                if (closedSet.contains(neighborNode)) {
                    continue;
                }
                
                float moveCost = neighbor.isDiagonal ? DIAGONAL_COST : STRAIGHT_COST;
                float tentativeGCost = current.gCost + moveCost;
                
                if (!openSet.contains(neighborNode) || tentativeGCost < neighborNode.gCost) {
                    neighborNode.parent = current;
                    neighborNode.gCost = tentativeGCost;
                    neighborNode.hCost = heuristic(neighborNode, goalNode);
                    neighborNode.calculateFCost();
                    
                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * ★ NEW: Check if entity's collision box can fit at this tile
     */
    private boolean canOccupyTile(int tileX, int tileY) {
        if (entityCollisionBox == null) {
            // No collision box - just check if tile is solid
            return canMove(tileX, tileY);
        }
        
        // Convert tile to world coordinates (center of tile)
        float worldX = tileX * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
        float worldY = tileY * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
        
        // Check if collision box at this position would collide
        return !map.collidesWithTiles(entityCollisionBox, worldX, worldY);
    }
    
    /**
     * ★ NEW: Find nearest walkable tile within radius
     */
    private int[] findNearestWalkableTile(int centerX, int centerY, int radius) {
        int bestX = -1;
        int bestY = -1;
        float bestDist = Float.MAX_VALUE;
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int tx = centerX + dx;
                int ty = centerY + dy;
                
                if (canOccupyTile(tx, ty)) {
                    float dist = (float)Math.sqrt(dx * dx + dy * dy);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestX = tx;
                        bestY = ty;
                    }
                }
            }
        }
        
        if (bestX == -1) return null;
        return new int[]{bestX, bestY};
    }
    
    private float heuristic(PathNode a, PathNode b) {
        int dx = Math.abs(a.x - b.x);
        int dy = Math.abs(a.y - b.y);
        return STRAIGHT_COST * Math.max(dx, dy);
    }
    
    /**
     * Get all valid neighbors, blocking diagonal movement through corners
     */
    private List<Neighbor> getValidNeighbors(int x, int y) {
        List<Neighbor> neighbors = new ArrayList<>();
        
        // Check 4 cardinal directions first
        boolean canGoNorth = canMove(x, y - 1);
        boolean canGoSouth = canMove(x, y + 1);
        boolean canGoEast = canMove(x + 1, y);
        boolean canGoWest = canMove(x - 1, y);
        
        // Add cardinal directions
        if (canGoNorth) neighbors.add(new Neighbor(x, y - 1, false));
        if (canGoSouth) neighbors.add(new Neighbor(x, y + 1, false));
        if (canGoEast) neighbors.add(new Neighbor(x + 1, y, false));
        if (canGoWest) neighbors.add(new Neighbor(x - 1, y, false));
        
        // Add diagonal directions ONLY if both adjacent cardinals are passable
        
        // Northeast: need North AND East to be clear
        if (canGoNorth && canGoEast && canMove(x + 1, y - 1)) {
            neighbors.add(new Neighbor(x + 1, y - 1, true));
        }
        
        // Southeast: need South AND East to be clear
        if (canGoSouth && canGoEast && canMove(x + 1, y + 1)) {
            neighbors.add(new Neighbor(x + 1, y + 1, true));
        }
        
        // Southwest: need South AND West to be clear
        if (canGoSouth && canGoWest && canMove(x - 1, y + 1)) {
            neighbors.add(new Neighbor(x - 1, y + 1, true));
        }
        
        // Northwest: need North AND West to be clear
        if (canGoNorth && canGoWest && canMove(x - 1, y - 1)) {
            neighbors.add(new Neighbor(x - 1, y - 1, true));
        }
        
        return neighbors;
    }
    
    /**
     * Check if a tile position is within bounds and not solid
     */
    private boolean canMove(int x, int y) {
        if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight()) {
            return false;
        }
        return !map.isSolid(x, y);
    }
    
    private List<int[]> reconstructPath(PathNode goalNode) {
        List<int[]> path = new ArrayList<>();
        PathNode current = goalNode;
        
        while (current != null) {
            path.add(0, new int[]{current.x, current.y});
            current = current.parent;
        }
        
        return path;
    }
    
    /**
     * Helper class to track neighbors and whether they're diagonal
     */
    private static class Neighbor {
        int x, y;
        boolean isDiagonal;
        
        Neighbor(int x, int y, boolean isDiagonal) {
            this.x = x;
            this.y = y;
            this.isDiagonal = isDiagonal;
        }
    }
}