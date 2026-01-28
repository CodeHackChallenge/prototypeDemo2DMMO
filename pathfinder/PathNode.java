package dev.main.pathfinder;
 
public class PathNode implements Comparable<PathNode> {
    public int x, y;  // Tile coordinates
    
    public float gCost;  // Distance from start
    public float hCost;  // Estimated distance to goal (heuristic)
    public float fCost;  // Total cost (g + h)
    
    public PathNode parent;  // For reconstructing the path
    
    public PathNode(int x, int y) {
        this.x = x;
        this.y = y;
        this.gCost = 0;
        this.hCost = 0;
        this.fCost = 0;
        this.parent = null;
    }
    
    public void calculateFCost() {
        fCost = gCost + hCost;
    }
    
    @Override
    public int compareTo(PathNode other) {
        return Float.compare(this.fCost, other.fCost);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PathNode other = (PathNode) obj;
        return x == other.x && y == other.y;
    }
    
    @Override
    public int hashCode() {
        return x * 1000 + y;
    }
}