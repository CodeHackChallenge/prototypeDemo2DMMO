package dev.main.pathfinder;

import java.util.*;

import dev.main.tile.TileMap;

public class Pathfinder {
    
    private TileMap map;
    
    private static final float DIAGONAL_COST = 1.414f;
    private static final float STRAIGHT_COST = 1.0f;
    
    public Pathfinder(TileMap map) {
        this.map = map;
    }
    
    public List<int[]> findPath(int startX, int startY, int goalX, int goalY) {
        
        if (map.isSolid(goalX, goalY)) {
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
            
            // Check all 8 neighbors with diagonal blocking
            for (Neighbor neighbor : getValidNeighbors(current.x, current.y)) {
                int nx = neighbor.x;
                int ny = neighbor.y;
                
                if (map.isSolid(nx, ny)) {
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
        // This prevents corner-cutting through solid tiles
        
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
/*
```

## How It Works

**Before (Buggy):**
```
0 0 0 0
0 3 H 3
0 T 3 0
```
Hero could path diagonally from H to the tile below-left, cutting through the corner between the two `3` tiles.

**After (Fixed):**
```
0 0 0 0
0 3 H 3
0 T 3 0
*/