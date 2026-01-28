package dev.main.dialogue;

import java.io.*;
import java.util.*;

/**
 * Enhanced JSON loader with support for conditions and actions
 * 
 * JSON Format:
 * {
 *   "id": "fionne_intro",
 *   "name": "Fionne Introduction",
 *   "startNode": "greet",
 *   "nodes": [
 *     {
 *       "id": "greet",
 *       "type": "DIALOGUE",
 *       "speaker": "Fionne",
 *       "text": "Hello traveler!",
 *       "nextNode": "end",
 *       "choices": [
 *         {"text": "Hello", "targetNode": "friendly"},
 *         {"text": "Goodbye", "targetNode": "end"}
 *       ],
 *       "conditions": [
 *         {"type": "min_level", "value": 5}
 *       ],
 *       "actions": [
 *         {"type": "award_xp", "amount": 100}
 *       ]
 *     }
 *   ]
 * }
 */
public class EnhancedDialogueLoader {
    
    /**
     * Load dialogue with enhanced features
     */
    public static DialogueTree loadFromFile(String filePath) {
        try {
            InputStream is = EnhancedDialogueLoader.class.getResourceAsStream(filePath);

            // If not found, try common resource prefixes to account for different packaging
            if (is == null) {
                String[] prefixes = {"/dev/main/resources", "/dev/main", "/resources", ""};
                for (String p : prefixes) {
                    String candidate = p;
                    if (!candidate.endsWith("/") && !candidate.isEmpty()) candidate += "/";
                    candidate += filePath.replaceFirst("^/", "");
                    is = EnhancedDialogueLoader.class.getResourceAsStream(candidate);
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
    
    private static DialogueTree parseDialogueTree(String json) {
        // Remove outer braces
        json = json.substring(json.indexOf('{') + 1, json.lastIndexOf('}'));
        
        String id = extractStringValue(json, "id");
        String name = extractStringValue(json, "name");
        String startNode = extractStringValue(json, "startNode");
        
        DialogueTree tree = new DialogueTree(id, name);
        
        // Extract and parse nodes
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
        
        // Basic properties
        String speaker = extractStringValue(json, "speaker");
        if (speaker != null) node.setSpeakerName(speaker);
        
        String text = extractStringValue(json, "text");
        if (text != null) node.setText(text);
        
        String nextNode = extractStringValue(json, "nextNode");
        if (nextNode != null) node.setNextNodeId(nextNode);
        
        String questId = extractStringValue(json, "questId");
        if (questId != null) node.setQuestId(questId);
        
        // Parse choices
        parseChoices(json, node);
        
        // Parse conditions
        parseConditions(json, node);
        
        // Parse actions
        parseActions(json, node);
        
        return node;
    }
    
    private static void parseChoices(String json, DialogueNode node) {
        String choicesJson = extractArrayValue(json, "choices");
        if (choicesJson == null || choicesJson.isEmpty()) return;
        
        List<String> choiceObjects = splitJsonArray(choicesJson);
        for (String choiceJson : choiceObjects) {
            String choiceText = extractStringValue(choiceJson, "text");
            String targetNode = extractStringValue(choiceJson, "targetNode");
            if (choiceText != null && targetNode != null) {
                // Parse optional conditions for this choice
                String condsJson = extractArrayValue(choiceJson, "conditions");
                if (condsJson == null || condsJson.isEmpty()) {
                    node.addChoice(choiceText, targetNode);
                } else {
                    List<String> condObjects = splitJsonArray(condsJson);
                    List<DialogueCondition> conds = new ArrayList<>();
                    for (String cjson : condObjects) {
                        String type = extractStringValue(cjson, "type");
                        DialogueCondition dc = createCondition(type, cjson);
                        if (dc != null) conds.add(dc);
                    }

                    if (conds.isEmpty()) {
                        node.addChoice(choiceText, targetNode);
                    } else if (conds.size() == 1) {
                        node.addChoice(choiceText, targetNode, conds.get(0));
                    } else {
                        node.addChoice(choiceText, targetNode, new DialogueConditions.AndCondition(
                            conds.toArray(new DialogueCondition[0])
                        ));
                    }
                }
            }
        }
    }
    
    private static void parseConditions(String json, DialogueNode node) {
        String conditionsJson = extractArrayValue(json, "conditions");
        if (conditionsJson == null || conditionsJson.isEmpty()) return;
        
        List<String> conditionObjects = splitJsonArray(conditionsJson);
        List<DialogueCondition> conditions = new ArrayList<>();
        
        for (String condJson : conditionObjects) {
            String type = extractStringValue(condJson, "type");
            DialogueCondition condition = createCondition(type, condJson);
            if (condition != null) {
                conditions.add(condition);
            }
        }
        
        // If multiple conditions, combine with AND
        if (conditions.size() == 1) {
            node.setCondition(conditions.get(0));
        } else if (conditions.size() > 1) {
            node.setCondition(new DialogueConditions.AndCondition(
                conditions.toArray(new DialogueCondition[0])
            ));
        }
    }
    
    private static DialogueCondition createCondition(String type, String json) {
        if (type == null) return null;
        
        switch (type.toLowerCase()) {
            case "min_level":
                String levelStr = extractStringValue(json, "value");
                if (levelStr != null) {
                    return new DialogueConditions.MinLevelCondition(
                        Integer.parseInt(levelStr)
                    );
                }
                break;
                
            case "has_quest":
                String questId = extractStringValue(json, "questId");
                if (questId != null) {
                    return new DialogueConditions.HasQuestCondition(questId);
                }
                break;
                
            case "quest_completed":
                String completedQuestId = extractStringValue(json, "questId");
                if (completedQuestId != null) {
                    return new DialogueConditions.QuestCompletedCondition(completedQuestId);
                }
                break;
        }
        
        return null;
    }
    
    private static void parseActions(String json, DialogueNode node) {
        String actionsJson = extractArrayValue(json, "actions");
        if (actionsJson == null || actionsJson.isEmpty()) return;
        
        List<String> actionObjects = splitJsonArray(actionsJson);
        
        for (String actionJson : actionObjects) {
            String type = extractStringValue(actionJson, "type");
            DialogueAction action = createAction(type, actionJson);
            if (action != null) {
                node.addAction(action);
            }
        }
    }
    
    private static DialogueAction createAction(String type, String json) {
        if (type == null) return null;
        
        switch (type.toLowerCase()) {
            case "award_xp":
                String xpStr = extractStringValue(json, "amount");
                if (xpStr != null) {
                    return new DialogueActions.AwardXPAction(
                        Integer.parseInt(xpStr)
                    );
                }
                break;
                
            case "heal_player":
                String healStr = extractStringValue(json, "amount");
                if (healStr != null) {
                    return new DialogueActions.HealPlayerAction(
                        Integer.parseInt(healStr)
                    );
                }
                break;
                
            case "debug_message":
                String message = extractStringValue(json, "message");
                if (message != null) {
                    return new DialogueActions.DebugMessageAction(message);
                }
                break;
        }
        
        return null;
    }
    
    // Helper methods (same as original DialogueLoader)
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
    
    private static String extractArrayValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(':', keyIndex);
        if (colonIndex == -1) return null;
        
        int arrayStart = json.indexOf('[', colonIndex);
        if (arrayStart == -1) return null;
        
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
                if (braceCount == 0) start = i;
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