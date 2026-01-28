package dev.main.entity;

import java.util.ArrayList;
import java.util.List;

import dev.main.input.Component;
import dev.main.input.Position;
import dev.main.ui.Quest;

/**
 * NPC Component - holds NPC-specific data
 */
public class NPC implements Component {
    
    public enum NPCType {
        QUEST_GIVER,
        MERCHANT,
        TRAINER,
        GUARD,
        VILLAGER
    }
    
    private String npcId;
    private String npcName;
    private NPCType npcType;
    
    // Dialogue
    private String greetingDialogue;
    private String farewellDialogue;
    
    // Quests
    private List<Quest> availableQuests;
    private Quest currentOfferedQuest;
    
    // Interaction
    private float interactionRange;  // Distance in pixels
    private boolean isInteractable;
    
    public NPC(String npcId, String npcName, NPCType npcType) {
        this.npcId = npcId;
        this.npcName = npcName;
        this.npcType = npcType;
        
        this.greetingDialogue = "Hello, traveler!";
        this.farewellDialogue = "Safe travels!";
        
        this.availableQuests = new ArrayList<>();
        this.currentOfferedQuest = null;
        
        this.interactionRange = 100f;  // 100 pixels
        this.isInteractable = true;
    }
    
    /**
     * Add a quest this NPC can offer
     */
    public void addQuest(Quest quest) {
        quest.setQuestGiver(npcId);
        availableQuests.add(quest);
    }
    
    /**
     * Get next available quest (not started)
     */
    public Quest getNextAvailableQuest() {
        for (Quest quest : availableQuests) {
            if (quest.canAccept()) {
                return quest;
            }
        }
        return null;
    }
    
    /**
     * Check if NPC has active quest for player
     */
    public Quest getActiveQuest() {
        for (Quest quest : availableQuests) {
            if (quest.isActive()) {
                return quest;
            }
        }
        return null;
    }
    
    /**
     * Check if NPC has completed quest for player
     */
    public Quest getCompletedQuest() {
        for (Quest quest : availableQuests) {
            if (quest.isCompleted()) {
                return quest;
            }
        }
        return null;
    }
    
    /**
     * Get dialogue based on quest status
     */
    public String getDialogue() {
        Quest completedQuest = getCompletedQuest();
        if (completedQuest != null) {
            return completedQuest.getCompleteDialogue();
        }
        
        Quest activeQuest = getActiveQuest();
        if (activeQuest != null) {
            return activeQuest.getProgressDialogue();
        }
        
        Quest availableQuest = getNextAvailableQuest();
        if (availableQuest != null) {
            return availableQuest.getDescription();
        }
        
        return greetingDialogue;
    }
    
    /**
     * Check if player is in interaction range
     */
    public boolean isPlayerInRange(Entity player, Entity npc) {
        Position playerPos = player.getComponent(Position.class);
        Position npcPos = npc.getComponent(Position.class);
        
        if (playerPos == null || npcPos == null) return false;
        
        float dx = playerPos.x - npcPos.x;
        float dy = playerPos.y - npcPos.y;
        float distance = (float)Math.sqrt(dx * dx + dy * dy);
        
        return distance <= interactionRange;
    }
    
    // Getters/Setters
    public String getNpcId() { return npcId; }
    public String getNpcName() { return npcName; }
    public NPCType getNpcType() { return npcType; }
    
    public void setGreetingDialogue(String dialogue) { this.greetingDialogue = dialogue; }
    public String getGreetingDialogue() { return greetingDialogue; }
    
    public void setFarewellDialogue(String dialogue) { this.farewellDialogue = dialogue; }
    public String getFarewellDialogue() { return farewellDialogue; }
    
    public List<Quest> getAvailableQuests() { return availableQuests; }
    
    public void setCurrentOfferedQuest(Quest quest) { this.currentOfferedQuest = quest; }
    public Quest getCurrentOfferedQuest() { return currentOfferedQuest; }
    
    public void setInteractionRange(float range) { this.interactionRange = range; }
    public float getInteractionRange() { return interactionRange; }
    
    public void setInteractable(boolean interactable) { this.isInteractable = interactable; }
    public boolean isInteractable() { return isInteractable; }
    
    public boolean hasQuestAvailable() {
        return getNextAvailableQuest() != null;
    }
    
    public boolean hasQuestComplete() {
        return getCompletedQuest() != null;
    }
}