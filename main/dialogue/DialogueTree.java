package dev.main.dialogue;

import java.util.HashMap;
import java.util.Map;

import dev.main.entity.Entity;
import dev.main.entity.NPC;
import dev.main.quest.QuestLog;
import dev.main.ui.Quest;

/**
 * Dialogue tree - manages branching conversations
 */
public class DialogueTree {
    
    private String id;
    private String name;
    private Map<String, DialogueNode> nodes;
    private String startNodeId;
    private String currentNodeId;
    
    public DialogueTree(String id, String name) {
        this.id = id;
        this.name = name;
        this.nodes = new HashMap<>();
        this.startNodeId = null;
        this.currentNodeId = null;
    }
    
    /**
     * Add a node to the tree
     */
    public void addNode(DialogueNode node) {
        nodes.put(node.getId(), node);
        
        // First node becomes start node
        if (startNodeId == null) {
            startNodeId = node.getId();
        }
    }
    
    /**
     * Set the starting node
     */
    public void setStartNode(String nodeId) {
        if (nodes.containsKey(nodeId)) {
            this.startNodeId = nodeId;
        }
    }
    
    /**
     * Start the dialogue
     */
    public DialogueNode start(Entity player) {
        return start(player, null);
    }

    /**
     * Start the dialogue, allowing NPC-based start node selection (e.g., progress/completed)
     */
    public DialogueNode start(Entity player, Entity npc) {
        String chosenStart = startNodeId;

        if (npc != null && player != null) {
            QuestLog ql = player.getComponent(QuestLog.class);
            NPC npcComp = npc.getComponent(NPC.class);

            if (npcComp != null && ql != null) {
                // Search for any node that references a questId
                for (DialogueNode node : nodes.values()) {
                    String qid = node.getQuestId();
                    if (qid == null) continue;

                    boolean hasQuest = ql.hasQuest(qid);

                    if (hasQuest) {
                        // Find matching quest on NPC to check completion
                        Quest found = null;
                        for (Quest q : npcComp.getAvailableQuests()) {
                            if (q.getId().equals(qid)) {
                                found = q;
                                break;
                            }
                        }

                        if (found != null && found.isCompleted()) {
                            if (nodes.containsKey("complete")) chosenStart = "complete";
                            else if (nodes.containsKey("completed")) chosenStart = "completed";
                        } else {
                            if (nodes.containsKey("progress")) chosenStart = "progress";
                            else if (nodes.containsKey("has_quest")) chosenStart = "has_quest";
                        }

                        break;
                    }
                }
            }
        }

        currentNodeId = chosenStart;
        DialogueNode node = getCurrentNode();

        if (node != null) {
            node.executeActions(player);
        }

        return node;
    }
    
    /**
     * Get the current dialogue node
     */
    public DialogueNode getCurrentNode() {
        if (currentNodeId == null) return null;
        return nodes.get(currentNodeId);
    }
    
    /**
     * Advance to next node (for linear dialogue)
     */
    public DialogueNode next(Entity player) {
        DialogueNode current = getCurrentNode();
        if (current == null) return null;
        
        String nextId = current.getNextNodeId();
        if (nextId == null) {
            currentNodeId = null;
            return null;
        }
        
        return goToNode(nextId, player);
    }
    
    /**
     * Go to specific node (for choices)
     */
    public DialogueNode goToNode(String nodeId, Entity player) {
        if (!nodes.containsKey(nodeId)) {
            currentNodeId = null;
            return null;
        }
        
        currentNodeId = nodeId;
        DialogueNode node = getCurrentNode();
        
        if (node != null && node.checkCondition(player)) {
            // Execute node actions
            node.executeActions(player);
            return node;
        }
        
        // If condition not met, try to find alternative
        return null;
    }
    
    /**
     * Choose a dialogue option
     */
    public DialogueNode choose(int choiceIndex, Entity player) {
        DialogueNode current = getCurrentNode();
        if (current == null) return null;
        
        // Get available choices (filtered by conditions)
        var choices = current.getAvailableChoices(player);
        
        if (choiceIndex < 0 || choiceIndex >= choices.size()) {
            return null;
        }
        
        DialogueChoice choice = choices.get(choiceIndex);
        return goToNode(choice.getTargetNodeId(), player);
    }
    
    /**
     * Reset dialogue to start
     */
    public void reset() {
        currentNodeId = null;
    }
    
    /**
     * Check if dialogue is finished
     */
    public boolean isFinished() {
        DialogueNode current = getCurrentNode();
        return current == null || current.isEnd();
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public Map<String, DialogueNode> getNodes() { return nodes; }
    public String getStartNodeId() { return startNodeId; }
}