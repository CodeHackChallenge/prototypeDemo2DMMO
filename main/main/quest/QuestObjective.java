package dev.main.quest;

/**
 * Individual quest objective
 */
public class QuestObjective {
    private String id;
    private String description;
    private int requiredAmount;
    private int currentProgress;
    
    public QuestObjective(String id, String description, int requiredAmount) {
        this.id = id;
        this.description = description;
        this.requiredAmount = requiredAmount;
        this.currentProgress = 0;
    }
    
    public void addProgress(int amount) {
        currentProgress = Math.min(requiredAmount, currentProgress + amount);
    }
    
    public boolean isComplete() {
        return currentProgress >= requiredAmount;
    }
    
    public String getId() { return id; }
    public String getDescription() { return description; }
    public int getRequiredAmount() { return requiredAmount; }
    public int getCurrentProgress() { return currentProgress; }
    
    public void resetProgress() { this.currentProgress = 0; }
}