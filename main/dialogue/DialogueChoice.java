package dev.main.dialogue;

import dev.main.entity.Entity;

/**
 * Player choice in dialogue
 */
public class DialogueChoice {
    private String text;
    private String targetNodeId;
    private DialogueCondition condition;
    
    public DialogueChoice(String text, String targetNodeId) {
        this.text = text;
        this.targetNodeId = targetNodeId;
        this.condition = null;
    }
    
    public boolean checkCondition(Entity player) {
        if (condition == null) return true;
        return condition.check(player);
    }
    
    public String getText() { return text; }
    public String getTargetNodeId() { return targetNodeId; }
    
    public void setCondition(DialogueCondition condition) {
        this.condition = condition;
    }
}