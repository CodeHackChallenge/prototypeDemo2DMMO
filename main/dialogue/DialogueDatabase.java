package dev.main.dialogue;

import java.io.*;
import java.util.*;

/**
 * Central database for all dialogue trees
 * Loads dialogues from JSON files and manages them
 */
public class DialogueDatabase {
    
    private static DialogueDatabase instance;
    
    private Map<String, DialogueTree> dialogues;
    private Map<String, String> npcDialogueMapping;  // NPC ID -> Dialogue ID
    
    private DialogueDatabase() {
        this.dialogues = new HashMap<>();
        this.npcDialogueMapping = new HashMap<>();
    }
    
    public static DialogueDatabase getInstance() {
        if (instance == null) {
            instance = new DialogueDatabase();
        }
        return instance;
    }
    
    /**
     * Load all dialogues from a directory
     */
    public void loadAllDialogues(String directoryPath) {
        // In a real implementation, you'd scan the directory
        // For now, manually load known dialogue files
         
        /* //eclipse
        loadDialogue("resources/dialogues/fionne_intro.json"); 
        loadDialogue("resources/dialogues/merchant_generic.json");
        loadDialogue("resources/dialogues/healer_npc.json");
        */
        //vs code
        loadDialogue("/dialogues/fionne_intro.json"); 
        loadDialogue("/dialogues/merchant_generic.json");
        loadDialogue("/dialogues/healer_npc.json");
        System.out.println("Loaded " + dialogues.size() + " dialogue trees");
    }
    
    /**
     * Load a single dialogue file
     */
    public void loadDialogue(String filePath) {
        DialogueTree tree = EnhancedDialogueLoader.loadFromFile(filePath);
        if (tree != null) {
            registerDialogue(tree);
        }
    }
    
    /**
     * Register a dialogue tree
     */
    public void registerDialogue(DialogueTree tree) {
        if (tree == null || tree.getId() == null) {
            System.err.println("Attempted to register null or unnamed dialogue");
            return;
        }

        dialogues.put(tree.getId(), tree);
        System.out.println("Registered dialogue: " + tree.getName() + " (ID: " + tree.getId() + ")");

        // Keep DialogueManager in sync so UI components can start dialogues by ID
        try {
            DialogueManager.getInstance().registerDialogue(tree);
        } catch (Exception e) {
            // Non-fatal - just log
            System.err.println("Failed to register dialogue with DialogueManager: " + tree.getId());
        }
    }
    
    /**
     * Map an NPC to a dialogue tree
     */
    public void mapNPCToDialogue(String npcId, String dialogueId) {
        npcDialogueMapping.put(npcId, dialogueId);
    }
    
    /**
     * Get dialogue tree by ID
     */
    public DialogueTree getDialogue(String dialogueId) {
        return dialogues.get(dialogueId);
    }
    
    /**
     * Get dialogue for an NPC
     */
    public DialogueTree getDialogueForNPC(String npcId) {
        String dialogueId = npcDialogueMapping.get(npcId);
        return dialogueId != null ? dialogues.get(dialogueId) : null;
    }
    
    /**
     * Create a simple greeting dialogue programmatically
     */
    public DialogueTree createSimpleGreeting(String npcId, String npcName, String greeting) {
        DialogueTree tree = new DialogueTree(npcId + "_greeting", npcName + " Greeting");
        
        DialogueNode greetNode = new DialogueNode("greet", DialogueNode.NodeType.DIALOGUE);
        greetNode.setSpeakerName(npcName);
        greetNode.setText(greeting);
        
        DialogueNode endNode = new DialogueNode("end", DialogueNode.NodeType.END);
        endNode.setText("Goodbye!");
        
        greetNode.setNextNodeId("end");
        
        tree.addNode(greetNode);
        tree.addNode(endNode);
        tree.setStartNode("greet");
        
        registerDialogue(tree);
        mapNPCToDialogue(npcId, tree.getId());
        
        return tree;
    }
    
    /**
     * Get all dialogue IDs
     */
    public Set<String> getAllDialogueIds() {
        return new HashSet<>(dialogues.keySet());
    }
    
    /**
     * Clear all dialogues (for reload)
     */
    public void clearAll() {
        dialogues.clear();
        npcDialogueMapping.clear();
    }
}
