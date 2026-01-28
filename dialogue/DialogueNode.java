package dev.main.dialogue;

import java.util.ArrayList;
import java.util.List;

import dev.main.entity.Entity;

/**
 * Dialogue node - represents one piece of dialogue in a conversation tree
 */
public class DialogueNode {
    
    public enum NodeType {
        DIALOGUE,           // Normal NPC speech
        PLAYER_CHOICE,      // Player chooses response
        QUEST_OFFER,        // Offers a quest
        QUEST_CHECK,        // Checks quest status
        SHOP,               // Opens shop
        END                 // Ends conversation
    }
    
    private String id;
    private NodeType type;
    private String speakerName;
    private String text;
    
    // Choices (for PLAYER_CHOICE nodes)
    private List<DialogueChoice> choices;
    
    // Next node (for linear dialogue)
    private String nextNodeId;
    
    // Conditions
    private DialogueCondition condition;
    
    // Actions (triggers when node is shown)
    private List<DialogueAction> actions;
    
    // Quest reference (for QUEST_OFFER/QUEST_CHECK)
    private String questId;
    
    public DialogueNode(String id, NodeType type) {
        this.id = id;
        this.type = type;
        this.speakerName = "";
        this.text = "";
        this.choices = new ArrayList<>();
        this.nextNodeId = null;
        this.condition = null;
        this.actions = new ArrayList<>();
        this.questId = null;
    }
    
    /**
     * Add a player choice option
     */
    public void addChoice(String choiceText, String targetNodeId) {
        choices.add(new DialogueChoice(choiceText, targetNodeId));
    }
    
    /**
     * Add a conditional choice
     */
    public void addChoice(String choiceText, String targetNodeId, DialogueCondition condition) {
        DialogueChoice choice = new DialogueChoice(choiceText, targetNodeId);
        choice.setCondition(condition);
        choices.add(choice);
    }
    
    /**
     * Add an action to trigger when this node is shown
     */
    public void addAction(DialogueAction action) {
        actions.add(action);
    }
    
    /**
     * Check if this node's condition is met
     */
    public boolean checkCondition(Entity player) {
        if (condition == null) return true;
        return condition.check(player);
    }
    
    /**
     * Get available choices (filtering by conditions)
     */
    public List<DialogueChoice> getAvailableChoices(Entity player) {
        List<DialogueChoice> available = new ArrayList<>();
        
        for (DialogueChoice choice : choices) {
            if (choice.checkCondition(player)) {
                available.add(choice);
            }
        }
        
        return available;
    }
    
    /**
     * Execute all actions for this node
     */
    public void executeActions(Entity player) {
        for (DialogueAction action : actions) {
            action.execute(player);
        }
    }
    
    // Getters/Setters
    public String getId() { return id; }
    public NodeType getType() { return type; }
    
    public String getSpeakerName() { return speakerName; }
    public void setSpeakerName(String name) { this.speakerName = name; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public List<DialogueChoice> getChoices() { return choices; }
    
    public String getNextNodeId() { return nextNodeId; }
    public void setNextNodeId(String nodeId) { this.nextNodeId = nodeId; }
    
    public DialogueCondition getCondition() { return condition; }
    public void setCondition(DialogueCondition condition) { this.condition = condition; }
    
    public String getQuestId() { return questId; }
    public void setQuestId(String questId) { this.questId = questId; }
    
    public List<DialogueAction> getActions() { return actions; }
    
    public boolean hasChoices() { return !choices.isEmpty(); }
    public boolean isEnd() { return type == NodeType.END || (nextNodeId == null && !hasChoices()); }
}



/**
 * Dialogue condition - checks if something is true
 */
interface DialogueCondition {
    boolean check(Entity player);
}

/**
 * Dialogue action - executes when node is shown
 */
interface DialogueAction {
    void execute(Entity player);
}