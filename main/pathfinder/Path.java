package dev.main.pathfinder;

import java.util.List;

import dev.main.input.Component;

/**
 * ★ IMPROVED: Added stuck detection fields
 */
public class Path implements Component {
    public List<int[]> waypoints;
    public int currentWaypoint;
    public boolean isFollowing;
    
    // ★ NEW: Stuck detection
    public float stuckTimer = 0f;
    public float lastPositionX = 0f;
    public float lastPositionY = 0f;
    
    public Path() {
        this.waypoints = null;
        this.currentWaypoint = 0;
        this.isFollowing = false;
    }
    
    public void setPath(List<int[]> newPath) {
        this.waypoints = newPath;
        this.currentWaypoint = 0;
        this.isFollowing = (newPath != null && !newPath.isEmpty());
        
        // ★ NEW: Reset stuck detection on new path
        this.stuckTimer = 0f;
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
        
        // ★ NEW: Reset stuck timer when advancing waypoint
        stuckTimer = 0f;
    }
    
    public void clear() {
        waypoints = null;
        currentWaypoint = 0;
        isFollowing = false;
        
        // ★ NEW: Reset stuck detection
        stuckTimer = 0f;
    }
}