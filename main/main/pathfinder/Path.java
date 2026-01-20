package dev.main.pathfinder;

import java.util.List;

import dev.main.input.Component;

public class Path implements Component {
    public List<int[]> waypoints;  // List of tile positions
    public int currentWaypoint;
    public boolean isFollowing;
    
    public Path() {
        this.waypoints = null;
        this.currentWaypoint = 0;
        this.isFollowing = false;
    }
    
    public void setPath(List<int[]> newPath) {
        this.waypoints = newPath;
        this.currentWaypoint = 0;
        this.isFollowing = (newPath != null && !newPath.isEmpty());
    }
    
    public int[] getCurrentWaypoint() {
        if (waypoints != null && currentWaypoint < waypoints.size()) {
            return waypoints.get(currentWaypoint);
        }
        return null;
    }
    
    public void advanceWaypoint() {
        currentWaypoint++;
        if (currentWaypoint >= waypoints.size()) {
            isFollowing = false;
        }
    }
    
    public void clear() {
        waypoints = null;
        currentWaypoint = 0;
        isFollowing = false;
    }
}