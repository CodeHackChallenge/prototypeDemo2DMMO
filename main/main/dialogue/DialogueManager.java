package dev.main.dialogue;

import java.util.HashMap;
import java.util.Map;

import dev.main.entity.Entity;

/**
 * Central dialogue management system
 * Handles all dialogue trees and NPC conversations
 */
public class DialogueManager {
    
    private static DialogueManager instance;
    
    private Map<String, DialogueTree> dialogueTrees;
    private DialogueTree currentDialogue;
    private Entity currentNPC;
    
    private DialogueManager() {
        this.dialogueTrees = new HashMap<>();
        this.currentDialogue = null;
        this.currentNPC = null;
    }
    
    /**
     * Get singleton instance
     */
    public static DialogueManager getInstance() {
        if (instance == null) {
            instance = new DialogueManager();
        }
        return instance;
    }
    
    /**
     * Register a dialogue tree
     */
    public void registerDialogue(DialogueTree tree) {
        dialogueTrees.put(tree.getId(), tree);
        System.out.println("Registered dialogue: " + tree.getName());
    }
    
    /**
     * Load dialogue from JSON file
     */
    public void loadDialogue(String filePath) {
        DialogueTree tree = EnhancedDialogueLoader.loadFromFile(filePath);
        if (tree != null) {
            registerDialogue(tree);
        }
    }
    
    /**
     * Start a dialogue with an NPC
     */
    public DialogueNode startDialogue(String dialogueId, Entity npc, Entity player) {
        DialogueTree tree = dialogueTrees.get(dialogueId);
        if (tree == null) {
            System.err.println("Dialogue not found: " + dialogueId);
            return null;
        }
        
        currentDialogue = tree;
        currentNPC = npc;
        
        // Reset and start
        tree.reset();
        return tree.start(player, npc);
    }
    
    /**
     * Get current dialogue node
     */
    public DialogueNode getCurrentNode() {
        if (currentDialogue == null) return null;
        return currentDialogue.getCurrentNode();
    }
    
    /**
     * Advance to next node
     */
    public DialogueNode next(Entity player) {
        if (currentDialogue == null) return null;
        return currentDialogue.next(player);
    }
    
    /**
     * Choose a dialogue option
     */
    public DialogueNode choose(int choiceIndex, Entity player) {
        if (currentDialogue == null) return null;
        return currentDialogue.choose(choiceIndex, player);
    }
    
    /**
     * End current dialogue
     */
    public void endDialogue() {
        if (currentDialogue != null) {
            currentDialogue.reset();
        }
        currentDialogue = null;
        currentNPC = null;
    }
    
    /**
     * Check if dialogue is active
     */
    public boolean isDialogueActive() {
        return currentDialogue != null && !currentDialogue.isFinished();
    }
    
    /**
     * Get current NPC
     */
    public Entity getCurrentNPC() {
        return currentNPC;
    }
    
    /**
     * Get dialogue tree by ID
     */
    public DialogueTree getDialogue(String dialogueId) {
        return dialogueTrees.get(dialogueId);
    }
    
    /**
     * Clear all dialogues (for testing/debugging)
     */
    public void clearAll() {
        dialogueTrees.clear();
        currentDialogue = null;
        currentNPC = null;
    }
}