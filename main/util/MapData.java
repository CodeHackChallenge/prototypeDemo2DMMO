package dev.main.util;

//════════════════════════════════════════════════════════════════════════
//NEW FILE: MapData.java
//════════════════════════════════════════════════════════════════════════
 

import java.util.List;

/**
* Data structure for map JSON format
*/
public class MapData {
 public String mapId;
 public int width;
 public int height;
 public int tileSize;
 public int[][] tiles;  // 2D array of tile data (0 = walkable, 1+ = solid/special)
 public List<Portal> portals;
 public List<MonsterSpawn> monsterSpawns;
 
 public static class Portal {
     public String id;
     public int x;  // Tile X
     public int y;  // Tile Y
     public String targetMap;
     public int targetX;
     public int targetY;
 }
 
 public static class MonsterSpawn {
     public String id;
     public String monsterType;
     public int x;  // World X in pixels
     public int y;  // World Y in pixels
     public int level;
     public String tier;  // "TRASH", "NORMAL", "ELITE", "MINIBOSS"
     public float respawnDelay;
 }
}