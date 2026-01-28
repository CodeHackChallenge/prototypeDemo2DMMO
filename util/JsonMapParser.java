package dev.main.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simple JSON parser for map files
 * No external dependencies - parses manually
 */
public class JsonMapParser {
 
    public static MapData parse(String jsonPath) {
        try {
            InputStream is = JsonMapParser.class.getResourceAsStream(jsonPath);
            if (is == null) {
                System.err.println("JSON map not found: " + jsonPath);
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder json = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                json.append(line.trim());
            }
            reader.close();
            
            return parseJson(json.toString());
            
        } catch (Exception e) {
            System.err.println("Failed to parse JSON map: " + jsonPath);
            e.printStackTrace();
            return null;
        }
    }
 
    private static MapData parseJson(String json) {
        MapData data = new MapData();
        data.portals = new ArrayList<>();
        data.monsterSpawns = new ArrayList<>();
        
        // Remove outer braces
        json = json.substring(json.indexOf('{') + 1, json.lastIndexOf('}'));
        
        // Parse fields
        data.mapId = extractString(json, "mapId");
        data.width = extractInt(json, "width");
        data.height = extractInt(json, "height");
        data.tileSize = extractInt(json, "tileSize");
        
        // Parse tiles array
        data.tiles = parseTilesArray(json);
        
        // Parse portals array
        String portalsJson = extractArray(json, "portals");
        if (portalsJson != null && !portalsJson.trim().isEmpty()) {
            parsePortals(portalsJson, data.portals);
        }
        
        // Parse monster spawns array
        String spawnsJson = extractArray(json, "monsterSpawns");
        if (spawnsJson != null && !spawnsJson.trim().isEmpty()) {
            parseMonsterSpawns(spawnsJson, data.monsterSpawns);
        }
        
        // ★ NEW: Parse zone loot object
        String zoneLootJson = extractObject(json, "zoneLoot");
        if (zoneLootJson != null && !zoneLootJson.trim().isEmpty()) {
            data.zoneLoot = parseZoneLoot(zoneLootJson);
        }
        
        return data;
    }
    
    // ★ NEW: Extract object (like extractArray but for {})
    private static String extractObject(String json, String key) {
        String search = "\"" + key + "\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        
        start = json.indexOf('{', start);
        if (start == -1) return null;
        
        int depth = 0;
        int end = start;
        
        do {
            char c = json.charAt(end);
            if (c == '{') depth++;
            if (c == '}') depth--;
            end++;
        } while (depth > 0 && end < json.length());
        
        return json.substring(start + 1, end - 1);
    }
    
    // ★ NEW: Parse zoneLoot object
    private static MapData.ZoneLootData parseZoneLoot(String json) {
        MapData.ZoneLootData zoneLoot = new MapData.ZoneLootData();
        
        // Parse loot tier
        zoneLoot.lootTier = extractString(json, "lootTier");
        
        // Parse rarity multipliers map
        String multipliersJson = extractObject(json, "rarityMultipliers");
        if (multipliersJson != null && !multipliersJson.trim().isEmpty()) {
            zoneLoot.rarityMultipliers = parseRarityMultipliers(multipliersJson);
        }
        
        // Parse extra drops array
        String extraDropsJson = extractArray(json, "extraDrops");
        if (extraDropsJson != null && !extraDropsJson.trim().isEmpty()) {
            zoneLoot.extraDrops = new ArrayList<>();
            parseExtraDrops(extraDropsJson, zoneLoot.extraDrops);
        }
        
        // Parse guaranteed drops array
        String guaranteedDropsJson = extractArray(json, "guaranteedDrops");
        if (guaranteedDropsJson != null && !guaranteedDropsJson.trim().isEmpty()) {
            zoneLoot.guaranteedDrops = new ArrayList<>();
            parseGuaranteedDrops(guaranteedDropsJson, zoneLoot.guaranteedDrops);
        }
        
        return zoneLoot;
    }
    
    // ★ NEW: Parse rarity multipliers map
    private static HashMap<String, Double> parseRarityMultipliers(String json) {
        HashMap<String, Double> map = new HashMap<>();
        
        // Parse key-value pairs like "COMMON": 1.5
        String[] rarities = {"COMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"};
        
        for (String rarity : rarities) {
            String search = "\"" + rarity + "\"";
            int start = json.indexOf(search);
            if (start != -1) {
                start = json.indexOf(':', start) + 1;
                
                // Skip whitespace
                while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
                    start++;
                }
                
                int end = start;
                while (end < json.length() && 
                       (Character.isDigit(json.charAt(end)) || 
                        json.charAt(end) == '.' || 
                        json.charAt(end) == '-')) {
                    end++;
                }
                
                double value = Double.parseDouble(json.substring(start, end));
                map.put(rarity, value);
            }
        }
        
        return map;
    }
    
    // ★ NEW: Parse extra drops array
    private static void parseExtraDrops(String json, java.util.List<MapData.ExtraDropData> drops) {
        int depth = 0;
        int objStart = -1;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart != -1) {
                    String objJson = json.substring(objStart + 1, i);
                    MapData.ExtraDropData drop = new MapData.ExtraDropData();
                    drop.itemName = extractString(objJson, "itemName");
                    drop.rarity = extractString(objJson, "rarity");
                    drop.minQuantity = extractInt(objJson, "minQuantity");
                    drop.maxQuantity = extractInt(objJson, "maxQuantity");
                    drop.dropChance = extractDouble(objJson, "dropChance");
                    drop.itemCreator = extractString(objJson, "itemCreator");
                    drop.comment = extractString(objJson, "comment");
                    drops.add(drop);
                }
            }
        }
    }
    
    // ★ NEW: Parse guaranteed drops array
    private static void parseGuaranteedDrops(String json, java.util.List<MapData.GuaranteedDropData> drops) {
        int depth = 0;
        int objStart = -1;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart != -1) {
                    String objJson = json.substring(objStart + 1, i);
                    MapData.GuaranteedDropData drop = new MapData.GuaranteedDropData();
                    drop.questId = extractString(objJson, "questId");
                    drop.itemName = extractString(objJson, "itemName");
                    drop.rarity = extractString(objJson, "rarity");
                    drop.quantity = extractInt(objJson, "quantity");
                    drop.itemCreator = extractString(objJson, "itemCreator");
                    drop.dropOnFirstKill = extractBoolean(objJson, "dropOnFirstKill");
                    drop.comment = extractString(objJson, "comment");
                    drops.add(drop);
                }
            }
        }
    }
    
    // ★ NEW: Extract double value
    private static double extractDouble(String json, String key) {
        String search = "\"" + key + "\"";
        int start = json.indexOf(search);
        if (start == -1) return 0.0;
        
        start = json.indexOf(':', start) + 1;
        
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        int end = start;
        while (end < json.length() && 
               (Character.isDigit(json.charAt(end)) || 
                json.charAt(end) == '.' || 
                json.charAt(end) == '-')) {
            end++;
        }
        
        return Double.parseDouble(json.substring(start, end));
    }
    
    // ★ NEW: Extract boolean value
    private static boolean extractBoolean(String json, String key) {
        String search = "\"" + key + "\"";
        int start = json.indexOf(search);
        if (start == -1) return false;
        
        start = json.indexOf(':', start) + 1;
        
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        // Check for "true" or "false"
        if (json.substring(start).startsWith("true")) {
            return true;
        } else if (json.substring(start).startsWith("false")) {
            return false;
        }
        
        return false;
    }
 
    private static String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        
        start = json.indexOf(':', start) + 1;
        start = json.indexOf('"', start) + 1;
        int end = json.indexOf('"', start);
        
        return json.substring(start, end);
    }
 
    private static int extractInt(String json, String key) {
        String search = "\"" + key + "\"";
        int start = json.indexOf(search);
        if (start == -1) return 0;
        
        start = json.indexOf(':', start) + 1;
        
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        
        return Integer.parseInt(json.substring(start, end));
    }
 
    private static String extractArray(String json, String key) {
        String search = "\"" + key + "\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        
        start = json.indexOf('[', start);
        if (start == -1) return null;
        
        int depth = 0;
        int end = start;
        
        do {
            char c = json.charAt(end);
            if (c == '[') depth++;
            if (c == ']') depth--;
            end++;
        } while (depth > 0 && end < json.length());
        
        return json.substring(start + 1, end - 1);
    }
 
    private static int[][] parseTilesArray(String json) {
        String tilesJson = extractArray(json, "tiles");
        if (tilesJson == null || tilesJson.trim().isEmpty()) {
            return new int[0][0];
        }
        
        // Split by rows (arrays within array)
        ArrayList<int[]> rows = new ArrayList<>();
        int depth = 0;
        int rowStart = 0;
        
        for (int i = 0; i < tilesJson.length(); i++) {
            char c = tilesJson.charAt(i);
            
            if (c == '[') {
                if (depth == 0) rowStart = i + 1;
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    String rowJson = tilesJson.substring(rowStart, i);
                    rows.add(parseIntArray(rowJson));
                }
            }
        }
        
        // Convert to 2D array
        int[][] tiles = new int[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            tiles[i] = rows.get(i);
        }
        
        return tiles;
    }
 
    private static int[] parseIntArray(String json) {
        String[] parts = json.split(",");
        int[] result = new int[parts.length];
        
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        
        return result;
    }
 
    private static void parsePortals(String json, java.util.List<MapData.Portal> portals) {
        // Split by objects
        int depth = 0;
        int objStart = -1;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart != -1) {
                    String objJson = json.substring(objStart + 1, i);
                    MapData.Portal portal = new MapData.Portal();
                    portal.id = extractString(objJson, "id");
                    portal.x = extractInt(objJson, "x");
                    portal.y = extractInt(objJson, "y");
                    portal.targetMap = extractString(objJson, "targetMap");
                    portal.targetX = extractInt(objJson, "targetX");
                    portal.targetY = extractInt(objJson, "targetY");
                    portals.add(portal);
                }
            }
        }
    }
 
    private static void parseMonsterSpawns(String json, java.util.List<MapData.MonsterSpawn> spawns) {
        // Split by objects
        int depth = 0;
        int objStart = -1;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart != -1) {
                    String objJson = json.substring(objStart + 1, i);
                    MapData.MonsterSpawn spawn = new MapData.MonsterSpawn();
                    spawn.id = extractString(objJson, "id");
                    spawn.monsterType = extractString(objJson, "monsterType");
                    spawn.x = extractInt(objJson, "x");
                    spawn.y = extractInt(objJson, "y");
                    spawn.level = extractInt(objJson, "level");
                    spawn.tier = extractString(objJson, "tier");
                    
                    // Parse float respawnDelay
                    String search = "\"respawnDelay\"";
                    int start = objJson.indexOf(search);
                    if (start != -1) {
                        start = objJson.indexOf(':', start) + 1;
                        while (start < objJson.length() && Character.isWhitespace(objJson.charAt(start))) {
                            start++;
                        }
                        int end = start;
                        while (end < objJson.length() && 
                               (Character.isDigit(objJson.charAt(end)) || 
                                objJson.charAt(end) == '.' || 
                                objJson.charAt(end) == '-')) {
                            end++;
                        }
                        spawn.respawnDelay = Float.parseFloat(objJson.substring(start, end));
                    }
                    
                    spawns.add(spawn);
                }
            }
        }
    }
}