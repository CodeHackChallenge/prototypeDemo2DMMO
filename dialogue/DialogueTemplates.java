package dev.main.dialogue;

import dev.main.ui.Quest;

/**
 * Pre-built dialogue templates for common scenarios
 */
public class DialogueTemplates {
    
    /**
     * Simple greeting with goodbye
     */
    public static DialogueTree createGreeting(String npcId, String npcName, String greetingText) {
        return new DialogueBuilder(npcId + "_greeting", npcName + " Greeting")
            .node("greet", DialogueNode.NodeType.DIALOGUE)
                .speaker(npcName)
                .text(greetingText)
                .next("end")
            .node("end", DialogueNode.NodeType.END)
                .text("Farewell, traveler.")
            .start("greet")
            .build();
    }
    
    /**
     * Quest offer dialogue
     */
    public static DialogueTree createQuestOffer(String npcId, String npcName, 
                                                String questId, Quest quest) {
        return new DialogueBuilder(npcId + "_quest_" + questId, npcName + " Quest")
            .node("offer", DialogueNode.NodeType.QUEST_OFFER)
                .speaker(npcName)
                .text(quest.getDescription())
                .quest(questId)
                .choice("I'll help you!", "accept")
                .choice("Not right now.", "decline")
            .node("accept", DialogueNode.NodeType.DIALOGUE)
                .speaker(npcName)
                .text(quest.getAcceptDialogue())
                .action(new DialogueActions.GiveQuestAction(quest))
                .next("end")
            .node("decline", DialogueNode.NodeType.DIALOGUE)
                .speaker(npcName)
                .text("I understand. Come back if you change your mind.")
                .next("end")
            .node("end", DialogueNode.NodeType.END)
            .start("offer")
            .build();
    }
    
    /**
     * Quest completion dialogue
     */
    public static DialogueTree createQuestComplete(String npcId, String npcName, 
                                                   String questId, Quest quest) {
        return new DialogueBuilder(npcId + "_complete_" + questId, "Quest Complete")
            .node("complete", DialogueNode.NodeType.DIALOGUE)
                .speaker(npcName)
                .text(quest.getCompleteDialogue())
                .action(new DialogueActions.AwardXPAction(quest.getExpReward()))
                .next("end")
            .node("end", DialogueNode.NodeType.END)
            .start("complete")
            .build();
    }
    
    /**
     * Merchant dialogue
     */
    public static DialogueTree createMerchant(String npcId, String npcName) {
        return new DialogueBuilder(npcId + "_merchant", npcName + " Shop")
            .node("greet", DialogueNode.NodeType.DIALOGUE)
                .speaker(npcName)
                .text("Welcome to my shop! What can I do for you?")
                .choice("Show me your wares", "shop")
                .choice("Just looking", "end")
            .node("shop", DialogueNode.NodeType.SHOP)
                .speaker(npcName)
                .text("Here's what I have in stock:")
                .next("end")
            .node("end", DialogueNode.NodeType.END)
                .text("Come back anytime!")
            .start("greet")
            .build();
    }
    
    /**
     * Branching conversation based on quest progress
     */
    public static DialogueTree createQuestProgressBranching(String npcId, String npcName,
                                                           String questId, Quest quest) {
        DialogueCondition hasQuest = new DialogueConditions.HasQuestCondition(questId);
        DialogueCondition questComplete = new DialogueConditions.QuestCompletedCondition(questId);
        
        return new DialogueBuilder(npcId + "_progress_" + questId, "Quest Progress")
            .node("check", DialogueNode.NodeType.DIALOGUE)
                .speaker(npcName)
                .text("Hello again!")
                .choiceIf("About that quest...", "has_quest", hasQuest)
                .choiceIf("I've completed the quest!", "completed", questComplete)
                .choice("Goodbye", "end")
            .node("has_quest", DialogueNode.NodeType.DIALOGUE)
                .speaker(npcName)
                .text(quest.getProgressDialogue())
                .next("end")
            .node("completed", DialogueNode.NodeType.DIALOGUE)
                .speaker(npcName)
                .text(quest.getCompleteDialogue())
                .action(new DialogueActions.AwardXPAction(quest.getExpReward()))
                .next("end")
            .node("end", DialogueNode.NodeType.END)
            .start("check")
            .build();
    }
}