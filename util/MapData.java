package dev.main.util;

//════════════════════════════════════════════════════════════════════════
//NEW FILE: MapData.java
//════════════════════════════════════════════════════════════════════════
 

import java.util.List;
import java.util.Map;

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
	 
	 public ZoneLootData zoneLoot;
	 
	 //internal class
	 public static class ZoneLootData {
		    public String lootTier;
		    public Map<String, Double> rarityMultipliers;
		    public List<ExtraDropData> extraDrops;
		    public List<GuaranteedDropData> guaranteedDrops;
		}
	 //internal class
	public static class ExtraDropData {
	    public String itemName;
	    public String rarity;
	    public int minQuantity;
	    public int maxQuantity;
	    public double dropChance;
	    public String itemCreator;
	    public String comment;
	}
	//internal class
	public static class GuaranteedDropData {
	    public String questId;
	    public String itemName;
	    public String rarity;
	    public int quantity;
	    public String itemCreator;
	    public boolean dropOnFirstKill;
	    public String comment;
	}
	//internal class	
	 public static class Portal {
	     public String id;
	     public int x;  // Tile X
	     public int y;  // Tile Y
	     public String targetMap;
	     public int targetX;
	     public int targetY;
	 }
	//internal class
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