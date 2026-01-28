package dev.main.dialogue;

/**
 * Builder pattern for creating dialogue trees programmatically
 */
public class DialogueBuilder {
    
    private DialogueTree tree;
    private DialogueNode currentNode;
    
    public DialogueBuilder(String dialogueId, String dialogueName) {
        this.tree = new DialogueTree(dialogueId, dialogueName);
    }
    
    /**
     * Start building a new node
     */
    public DialogueBuilder node(String nodeId, DialogueNode.NodeType type) {
        currentNode = new DialogueNode(nodeId, type);
        tree.addNode(currentNode);
        return this;
    }
    
    /**
     * Set speaker name
     */
    public DialogueBuilder speaker(String speakerName) {
        if (currentNode != null) {
            currentNode.setSpeakerName(speakerName);
        }
        return this;
    }
    
    /**
     * Set dialogue text
     */
    public DialogueBuilder text(String text) {
        if (currentNode != null) {
            currentNode.setText(text);
        }
        return this;
    }
    
    /**
     * Set next node
     */
    public DialogueBuilder next(String nextNodeId) {
        if (currentNode != null) {
            currentNode.setNextNodeId(nextNodeId);
        }
        return this;
    }
    
    /**
     * Add a choice
     */
    public DialogueBuilder choice(String choiceText, String targetNodeId) {
        if (currentNode != null) {
            currentNode.addChoice(choiceText, targetNodeId);
        }
        return this;
    }
    
    /**
     * Add a conditional choice
     */
    public DialogueBuilder choiceIf(String choiceText, String targetNodeId, DialogueCondition condition) {
        if (currentNode != null) {
            currentNode.addChoice(choiceText, targetNodeId, condition);
        }
        return this;
    }
    
    /**
     * Set node condition
     */
    public DialogueBuilder condition(DialogueCondition condition) {
        if (currentNode != null) {
            currentNode.setCondition(condition);
        }
        return this;
    }
    
    /**
     * Add an action
     */
    public DialogueBuilder action(DialogueAction action) {
        if (currentNode != null) {
            currentNode.addAction(action);
        }
        return this;
    }
    
    /**
     * Set quest reference
     */
    public DialogueBuilder quest(String questId) {
        if (currentNode != null) {
            currentNode.setQuestId(questId);
        }
        return this;
    }
    
    /**
     * Set start node
     */
    public DialogueBuilder start(String nodeId) {
        tree.setStartNode(nodeId);
        return this;
    }
    
    /**
     * Build and return the dialogue tree
     */
    public DialogueTree build() {
        return tree;
    }
    
    /**
     * Build and register in database
     */
    public DialogueTree buildAndRegister() {
        DialogueDatabase.getInstance().registerDialogue(tree);
        return tree;
    }
}