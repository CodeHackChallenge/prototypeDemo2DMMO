package dev.main.dialogue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads dialogue trees from JSON files
 * Simple JSON parser - no external libraries needed
 */
public class DialogueLoader {
    
    /**
     * Load dialogue tree from JSON file
     */
    public static DialogueTree loadFromFile(String filePath) {
        try {
            InputStream is = DialogueLoader.class.getResourceAsStream(filePath);

            // If not found, try common resource prefixes to account for different packaging
            if (is == null) {
                String[] prefixes = {"/dev/main/resources", "/dev/main", "/resources", ""};
                for (String p : prefixes) {
                    String candidate = p;
                    if (!candidate.endsWith("/") && !candidate.isEmpty()) candidate += "/";
                    candidate += filePath.replaceFirst("^/", "");
                    is = DialogueLoader.class.getResourceAsStream(candidate);
                    if (is != null) {
                        filePath = candidate; // update for logging
                        break;
                    }
                }
            }

            if (is == null) {
                System.err.println("Dialogue file not found: " + filePath);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder json = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                json.append(line.trim());
            }
            
            reader.close();
            
            return parseDialogueTree(json.toString());
            
        } catch (Exception e) {
            System.err.println("Failed to load dialogue: " + filePath);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Parse JSON into DialogueTree
     */
    private static DialogueTree parseDialogueTree(String json) {
        // Remove outer braces
        json = json.substring(json.indexOf('{') + 1, json.lastIndexOf('}'));
        
        // Extract fields
        String id = extractStringValue(json, "id");
        String name = extractStringValue(json, "name");
        String startNode = extractStringValue(json, "startNode");
        
        DialogueTree tree = new DialogueTree(id, name);
        
        // Extract nodes array
        String nodesJson = extractArrayValue(json, "nodes");
        List<String> nodeObjects = splitJsonArray(nodesJson);
        
        for (String nodeJson : nodeObjects) {
            DialogueNode node = parseDialogueNode(nodeJson);
            if (node != null) {
                tree.addNode(node);
            }
        }
        
        if (startNode != null && !startNode.isEmpty()) {
            tree.setStartNode(startNode);
        }
        
        return tree;
    }
    
    /**
     * Parse single dialogue node
     */
    private static DialogueNode parseDialogueNode(String json) {
        String nodeId = extractStringValue(json, "id");
        String typeStr = extractStringValue(json, "type");
        
        DialogueNode.NodeType type;
        try {
            type = DialogueNode.NodeType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            type = DialogueNode.NodeType.DIALOGUE;
        }
        
        DialogueNode node = new DialogueNode(nodeId, type);
        
        // Set basic properties
        String speaker = extractStringValue(json, "speaker");
        if (speaker != null) node.setSpeakerName(speaker);
        
        String text = extractStringValue(json, "text");
        if (text != null) node.setText(text);
        
        String nextNode = extractStringValue(json, "nextNode");
        if (nextNode != null) node.setNextNodeId(nextNode);
        
        String questId = extractStringValue(json, "questId");
        if (questId != null) node.setQuestId(questId);
        
        // Parse choices
        String choicesJson = extractArrayValue(json, "choices");
        if (choicesJson != null && !choicesJson.isEmpty()) {
            List<String> choiceObjects = splitJsonArray(choicesJson);
            for (String choiceJson : choiceObjects) {
                String choiceText = extractStringValue(choiceJson, "text");
                String targetNode = extractStringValue(choiceJson, "targetNode");
                
                if (choiceText != null && targetNode != null) {
                    node.addChoice(choiceText, targetNode);
                }
            }
        }
        
        return node;
    }
    
    /**
     * Extract string value from JSON
     */
    private static String extractStringValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(':', keyIndex);
        if (colonIndex == -1) return null;
        
        int valueStart = json.indexOf('"', colonIndex) + 1;
        if (valueStart == 0) return null;
        
        int valueEnd = json.indexOf('"', valueStart);
        if (valueEnd == -1) return null;
        
        return json.substring(valueStart, valueEnd);
    }
    
    /**
     * Extract array value from JSON
     */
    private static String extractArrayValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(':', keyIndex);
        if (colonIndex == -1) return null;
        
        int arrayStart = json.indexOf('[', colonIndex);
        if (arrayStart == -1) return null;
        
        // Find matching closing bracket
        int bracketCount = 1;
        int arrayEnd = arrayStart + 1;
        
        while (bracketCount > 0 && arrayEnd < json.length()) {
            char c = json.charAt(arrayEnd);
            if (c == '[') bracketCount++;
            else if (c == ']') bracketCount--;
            arrayEnd++;
        }
        
        return json.substring(arrayStart + 1, arrayEnd - 1);
    }
    
    /**
     * Split JSON array into individual objects
     */
    private static List<String> splitJsonArray(String arrayContent) {
        List<String> objects = new ArrayList<>();
        
        if (arrayContent == null || arrayContent.trim().isEmpty()) {
            return objects;
        }
        
        int braceCount = 0;
        int start = 0;
        
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            
            if (c == '{') {
                if (braceCount == 0) {
                    start = i;
                }
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    objects.add(arrayContent.substring(start, i + 1));
                }
            }
        }
        
        return objects;
    }
}