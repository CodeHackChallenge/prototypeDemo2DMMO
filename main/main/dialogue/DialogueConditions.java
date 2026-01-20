package dev.main.dialogue;

import java.util.Arrays;
import java.util.List;

import dev.main.entity.Entity;
import dev.main.entity.Experience;
import dev.main.quest.QuestLog;
import dev.main.quest.QuestObjective;
import dev.main.ui.Quest; 

/**
 * Common dialogue conditions
 */
public class DialogueConditions {
    
    /**
     * Check if player has a quest
     */
    public static class HasQuestCondition implements DialogueCondition {
        private String questId;
        
        public HasQuestCondition(String questId) {
            this.questId = questId;
        }
        
        @Override
        public boolean check(Entity player) {
            QuestLog questLog = player.getComponent(QuestLog.class);
            if (questLog == null) return false;
            return questLog.hasQuest(questId);
        }
    }
    
    /**
     * Check if player has completed a quest
     */
    public static class QuestCompletedCondition implements DialogueCondition {
        private String questId;
        
        public QuestCompletedCondition(String questId) {
            this.questId = questId;
        }
        
        @Override
        public boolean check(Entity player) {
            QuestLog questLog = player.getComponent(QuestLog.class);
            if (questLog == null) return false;
            
            Quest quest = questLog.getQuest(questId);
            return quest != null && quest.isCompleted();
        }
    }
    
    /**
     * Check if player is at least a certain level
     */
    public static class MinLevelCondition implements DialogueCondition {
        private int minLevel;
        
        public MinLevelCondition(int minLevel) {
            this.minLevel = minLevel;
        }
        
        @Override
        public boolean check(Entity player) {
            Experience exp = player.getComponent(Experience.class);
            if (exp == null) return false;
            return exp.level >= minLevel;
        }
    }
    
    /**
     * Check if player has killed enough of a monster type
     */
    public static class KillCountCondition implements DialogueCondition {
        private String objectiveId;
        private int requiredCount;
        
        public KillCountCondition(String objectiveId, int requiredCount) {
            this.objectiveId = objectiveId;
            this.requiredCount = requiredCount;
        }
        
        @Override
        public boolean check(Entity player) {
            QuestLog questLog = player.getComponent(QuestLog.class);
            if (questLog == null) return false;
            
            for (Quest quest : questLog.getActiveQuests()) {
                for (QuestObjective obj : quest.getObjectives()) {
                    if (obj.getId().equals(objectiveId)) {
                        return obj.getCurrentProgress() >= requiredCount;
                    }
                }
            }
            return false;
        }
    }
    
    /**
     * Inverse condition - true when inner condition is false
     */
    public static class NotCondition implements DialogueCondition {
        private DialogueCondition condition;
        
        public NotCondition(DialogueCondition condition) {
            this.condition = condition;
        }
        
        @Override
        public boolean check(Entity player) {
            return !condition.check(player);
        }
    }
    
    /**
     * AND condition - true when all conditions are true
     */
    public static class AndCondition implements DialogueCondition {
        private List<DialogueCondition> conditions;
        
        public AndCondition(DialogueCondition... conditions) {
            this.conditions = Arrays.asList(conditions);
        }
        
        @Override
        public boolean check(Entity player) {
            for (DialogueCondition condition : conditions) {
                if (!condition.check(player)) return false;
            }
            return true;
        }
    }
    
    /**
     * OR condition - true when any condition is true
     */
    public static class OrCondition implements DialogueCondition {
        private List<DialogueCondition> conditions;
        
        public OrCondition(DialogueCondition... conditions) {
            this.conditions = Arrays.asList(conditions);
        }
        
        @Override
        public boolean check(Entity player) {
            for (DialogueCondition condition : conditions) {
                if (condition.check(player)) return true;
            }
            return false;
        }
    }
}