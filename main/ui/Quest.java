package dev.main.ui;

import java.util.ArrayList;
import java.util.List;

import dev.main.entity.Entity;
import dev.main.entity.Experience;
import dev.main.quest.QuestObjective;

/**
 * Quest system - tracks objectives, rewards, and completion status
 */
public class Quest {
    
    public enum QuestStatus {
        NOT_STARTED,
        ACTIVE,
        COMPLETED,
        FAILED
    }
    
    public enum QuestType {
        KILL,           // Kill X monsters
        COLLECT,        // Collect X items
        TALK,           // Talk to NPC
        ESCORT,         // Escort NPC
        EXPLORE,        // Reach location
        DELIVERY        // Deliver item to NPC
    }
    
    public static final int QUEST_MAIN = 0, QUEST_SIDE = 1;
    
    private String id;
    private String name;
    private String description;
    private QuestType type;
    private QuestStatus status;
    
    // Quest giver
    private String questGiverNPCId;
    
    // Requirements
    private int levelRequirement;
    
    // Objectives
    private List<QuestObjective> objectives;
    private int currentObjectiveIndex;
    
    // Rewards
    private int expReward;
    private int aurelReward;
    private List<String> itemRewards;  // Item IDs
    
    // Dialogue
    private String acceptDialogue;
    private String progressDialogue;
    private String completeDialogue;
    
    private int questType;
    
    public Quest(String id, String name, String description, QuestType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = QuestStatus.NOT_STARTED;
        
        this.questGiverNPCId = null;
        this.levelRequirement = 1;
        
        this.objectives = new ArrayList<>();
        this.currentObjectiveIndex = 0;
        
        this.expReward = 0;
        this.aurelReward = 0;
        this.itemRewards = new ArrayList<>();
        
        this.acceptDialogue = "Thank you for accepting this quest!";
        this.progressDialogue = "The quest is not yet complete.";
        this.completeDialogue = "Excellent work! Here is your reward.";
    }//TODO: must declare if it's main quest or side
    
    public void questType(int type) {
    	this.questType = type;
    }
    
    public int getQuestType() {
    	return questType;
    }
    /**
     * Add an objective to the quest
     */
    public void addObjective(QuestObjective objective) {
        objectives.add(objective);
    }
    
    /**
     * Start the quest
     */
    public void accept() {
        if (status == QuestStatus.NOT_STARTED) {
            status = QuestStatus.ACTIVE;
            System.out.println("Quest accepted: " + name);
        }
    }
    
    /**
     * Update quest progress (e.g., killed a monster, collected item)
     */
    public void updateProgress(String objectiveId, int amount) {
        if (status != QuestStatus.ACTIVE) return;
        
        for (QuestObjective obj : objectives) {
            if (obj.getId().equals(objectiveId)) {
                obj.addProgress(amount);
                
                if (obj.isComplete()) {
                    System.out.println("Objective complete: " + obj.getDescription());
                }
                break;
            }
        }
        
        // Check if all objectives complete
        checkCompletion();
    }

    /**
     * Reset quest to not-started and clear objective progress
     */
    public void reset() {
        this.status = QuestStatus.NOT_STARTED;
        this.currentObjectiveIndex = 0;
        for (QuestObjective obj : objectives) {
            obj.resetProgress();
        }
    }
    
    /**
     * Check if all objectives are complete
     */
    private void checkCompletion() {
        for (QuestObjective obj : objectives) {
            if (!obj.isComplete()) {
                return;  // Not all objectives done
            }
        }
        
        // All objectives complete
        status = QuestStatus.COMPLETED;
        System.out.println("Quest completed: " + name);
    }
    
    /**
     * Claim quest rewards
     */
    public void claimRewards(Entity player) {
        if (status != QuestStatus.COMPLETED) {
            System.out.println("Quest not complete yet!");
            return;
        }
        
        // Award XP
        Experience exp = player.getComponent(Experience.class);
        if (exp != null && expReward > 0) {
            System.out.println("Gained " + expReward + " XP from quest!");
            // TODO: Add XP to player
        }
        
        // Award aurel
        if (aurelReward > 0) {
            System.out.println("Gained " + aurelReward + " aurels from quest!");
            // TODO: Add aurel to player
        }
        
        // Award items
        for (String itemId : itemRewards) {
            System.out.println("Received item: " + itemId);
            // TODO: Add item to inventory
        }
    }
    
    /**
     * Get current objective description
     */
    public String getCurrentObjectiveDescription() {
        if (objectives.isEmpty()) return "No objectives";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < objectives.size(); i++) {
            QuestObjective obj = objectives.get(i);
            sb.append("• ").append(obj.getDescription());
            
            if (obj.isComplete()) {
                sb.append(" ✓");
            } else {
                sb.append(" (").append(obj.getCurrentProgress()).append("/")
                  .append(obj.getRequiredAmount()).append(")");
            }
            
            if (i < objectives.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
    /**
     * Get quest progress percentage
     */
    public float getProgressPercent() {
        if (objectives.isEmpty()) return 0f;

        // Calculate fractional progress across all objectives.
        // Sum individual objective progress / required amounts, then divide
        // by total required to get a value between 0.0 and 1.0.
        float totalRequired = 0f;
        float totalProgress = 0f;

        for (QuestObjective obj : objectives) {
            totalRequired += obj.getRequiredAmount();
            totalProgress += obj.getCurrentProgress();
        }

        if (totalRequired <= 0f) return 0f;
        float pct = totalProgress / totalRequired;
        return Math.max(0f, Math.min(1f, pct));
    }
    
    // Getters/Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public QuestType getType() { return type; }
    public QuestStatus getStatus() { return status; }
    
    public void setQuestGiver(String npcId) { this.questGiverNPCId = npcId; }
    public String getQuestGiver() { return questGiverNPCId; }
    
    public void setLevelRequirement(int level) { this.levelRequirement = level; }
    public int getLevelRequirement() { return levelRequirement; }
    
    public void setExpReward(int exp) { this.expReward = exp; }
    public int getExpReward() { return expReward; }
    
    public void setAurelReward(int aurel) { this.aurelReward = aurel; }
    public int getAurelReward() { return aurelReward; }
    
    public void addItemReward(String itemId) { itemRewards.add(itemId); }
    public List<String> getItemRewards() { return itemRewards; }
    
    public void setAcceptDialogue(String dialogue) { this.acceptDialogue = dialogue; }
    public String getAcceptDialogue() { return acceptDialogue; }
    
    public void setProgressDialogue(String dialogue) { this.progressDialogue = dialogue; }
    public String getProgressDialogue() { return progressDialogue; }
    
    public void setCompleteDialogue(String dialogue) { this.completeDialogue = dialogue; }
    public String getCompleteDialogue() { return completeDialogue; }
    
    public List<QuestObjective> getObjectives() { return objectives; }
    
    public boolean isActive() { return status == QuestStatus.ACTIVE; }
    public boolean isCompleted() { return status == QuestStatus.COMPLETED; }
    public boolean canAccept() { return status == QuestStatus.NOT_STARTED; }
    
    @Override
    public String toString() {
        return name + " (" + status + ") - " + getCurrentObjectiveDescription();
    }
}

