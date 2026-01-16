package dev.main.dialogue;

import java.util.Arrays;
import java.util.List;

import dev.main.Engine;
import dev.main.entity.Entity;
import dev.main.entity.Experience;
import dev.main.quest.QuestLog;
import dev.main.stats.Stats;
import dev.main.ui.Quest;
import dev.main.ui.UIButton;
import dev.main.ui.UIManager;

public class DialogueActions {
   
   /**
    * Award XP to player
    */
   public static class AwardXPAction implements DialogueAction {
       private int xpAmount;
       
       public AwardXPAction(int xpAmount) {
           this.xpAmount = xpAmount;
       }
       
       @Override
       public void execute(Entity player) {
           Experience exp = player.getComponent(Experience.class);
           if (exp != null) {
               exp.addExperience(xpAmount);
               System.out.println("Awarded " + xpAmount + " XP from dialogue");
           }
       }
   }
   
   /**
    * Give quest to player
    */
   public static class GiveQuestAction implements DialogueAction {
       private Quest quest;
       
       public GiveQuestAction(Quest quest) {
           this.quest = quest;
       }
       
       @Override
       public void execute(Entity player) {
           QuestLog questLog = player.getComponent(QuestLog.class);
           if (questLog != null) {
               quest.accept();
               questLog.addQuest(quest);
               System.out.println("Quest accepted via dialogue: " + quest.getName());
               try {
                   // Try to unlock and open quest UI
                   Engine eng = Engine.getInstance();
                   System.out.println("[GiveQuestAction] Engine instance: " + eng);
                   if (eng != null && eng.getGameState() != null && eng.getGameState().getUIManager() != null) {
                       UIManager ui = eng.getGameState().getUIManager();
                       System.out.println("[GiveQuestAction] UIManager: " + ui);
                       UIButton before = ui.getMenuButton("quest");
                       System.out.println("[GiveQuestAction] Quest button before unlock: " + (before == null ? "null" : ("locked=" + before.isLocked())));
                       ui.unlockMenuButton("quest");
                       UIButton after = ui.getMenuButton("quest");
                       System.out.println("[GiveQuestAction] Quest button after unlock: " + (after == null ? "null" : ("locked=" + after.isLocked())));

                       // Open the quest panel so player sees the quest and progress
                       if (after != null) {
                           after.onClick();
                       }

                       ui.updateQuestIndicator();
                   } else {
                       System.out.println("[GiveQuestAction] UIManager not available to update quest UI");
                   }
               } catch (Exception e) {
                   System.err.println("Failed to update UI after accepting quest: " + e.getMessage());
               }
           }
       }
   }
   
   /**
    * Complete a quest objective
    */
   public static class CompleteObjectiveAction implements DialogueAction {
       private String objectiveId;
       private int amount;
       
       public CompleteObjectiveAction(String objectiveId, int amount) {
           this.objectiveId = objectiveId;
           this.amount = amount;
       }
       
       @Override
       public void execute(Entity player) {
           QuestLog questLog = player.getComponent(QuestLog.class);
           if (questLog != null) {
               questLog.updateQuestProgress(objectiveId, amount);
               System.out.println("Quest progress updated: " + objectiveId + " +" + amount);
           }
       }
   }
   
   /**
    * Heal player
    */
   public static class HealPlayerAction implements DialogueAction {
       private int healAmount;
       
       public HealPlayerAction(int healAmount) {
           this.healAmount = healAmount;
       }
       
       @Override
       public void execute(Entity player) {
           Stats stats = player.getComponent(Stats.class);
           if (stats != null) {
               stats.hp = Math.min(stats.maxHp, stats.hp + healAmount);
               System.out.println("Healed " + healAmount + " HP from dialogue");
           }
       }
   }
   
   /**
    * Print debug message
    */
   public static class DebugMessageAction implements DialogueAction {
       private String message;
       
       public DebugMessageAction(String message) {
           this.message = message;
       }
       
       @Override
       public void execute(Entity player) {
           System.out.println("[Dialogue Action] " + message);
       }
   }
   
   /**
    * Chain multiple actions
    */
   public static class MultiAction implements DialogueAction {
       private List<DialogueAction> actions;
       
       public MultiAction(DialogueAction... actions) {
           this.actions = Arrays.asList(actions);
       }
       
       @Override
       public void execute(Entity player) {
           for (DialogueAction action : actions) {
               action.execute(player);
           }
       }
   }
}