package dev.main.quest;

import java.util.ArrayList;
import java.util.List;

import dev.main.input.Component;
import dev.main.ui.Quest;

/**
 * Quest log component - tracks player's active and completed quests
 */
public class QuestLog implements Component {
    
    private List<Quest> activeQuests;
    private List<Quest> completedQuests;
    private int maxActiveQuests;
    
    public QuestLog() {
        this.activeQuests = new ArrayList<>();
        this.completedQuests = new ArrayList<>();
        this.maxActiveQuests = 10;  // Max 10 active quests at once
    }
    
    /**
     * Add quest to active quests
     */
    public boolean addQuest(Quest quest) {
        if (activeQuests.size() >= maxActiveQuests) {
            System.out.println("Quest log is full!");
            return false;
        }
        
        if (hasQuest(quest.getId())) {
            System.out.println("Quest already in log!");
            return false;
        }
        
        activeQuests.add(quest);
        System.out.println("Quest added to log: " + quest.getName());
        return true;
    }
    
    /**
     * Remove quest from active quests
     */
    public void removeQuest(String questId) {
        activeQuests.removeIf(q -> q.getId().equals(questId));
    }
    
    /**
     * Move quest to completed list
     */
    public void completeQuest(String questId) {
        Quest quest = getQuest(questId);
        if (quest != null && quest.isCompleted()) {
            activeQuests.remove(quest);
            completedQuests.add(quest);
            System.out.println("Quest completed: " + quest.getName());
        }
    }
    
    /**
     * Get quest by ID
     */
    public Quest getQuest(String questId) {
        for (Quest quest : activeQuests) {
            if (quest.getId().equals(questId)) {
                return quest;
            }
        }
        return null;
    }
    
    /**
     * Check if player has quest (active or completed)
     */
    public boolean hasQuest(String questId) {
        for (Quest quest : activeQuests) {
            if (quest.getId().equals(questId)) return true;
        }
        for (Quest quest : completedQuests) {
            if (quest.getId().equals(questId)) return true;
        }
        return false;
    }
    
    /**
     * Update quest progress
     */
    public void updateQuestProgress(String objectiveId, int amount) {
        // Iterate over a snapshot to avoid ConcurrentModificationException
        List<Quest> snapshot = new ArrayList<>(activeQuests);
        for (Quest quest : snapshot) {
            quest.updateProgress(objectiveId, amount);

            // Auto-move to completed if all objectives done
            if (quest.isCompleted()) {
                completeQuest(quest.getId());
            }
        }
    }
    
    /**
     * Get all active quests
     */
    public List<Quest> getActiveQuests() {
        return new ArrayList<>(activeQuests);
    }
    
    /**
     * Get all completed quests
     */
    public List<Quest> getCompletedQuests() {
        return new ArrayList<>(completedQuests);
    }
    
    /**
     * Get number of active quests
     */
    public int getActiveQuestCount() {
        return activeQuests.size();
    }
    
    /**
     * Check if quest log is full
     */
    public boolean isFull() {
        return activeQuests.size() >= maxActiveQuests;
    }
}