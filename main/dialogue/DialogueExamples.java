package dev.main.dialogue;

import dev.main.ui.Quest;

/**
 * Examples showing how to create dialogues using the system
 */
public class DialogueExamples {
    
    /**
     * Example 1: Create a simple greeting programmatically
     */
    public static void createSimpleGreeting() {
        DialogueTree greeting = new DialogueBuilder("guard_greeting", "Guard Greeting")
            .node("greet", DialogueNode.NodeType.DIALOGUE)
                .speaker("Guard")
                .text("Halt! State your business.")
                .choice("I'm just passing through", "pass")
                .choice("I'm here to help", "help")
            .node("pass", DialogueNode.NodeType.DIALOGUE)
                .speaker("Guard")
                .text("Very well. Be on your way.")
                .next("end")
            .node("help", DialogueNode.NodeType.DIALOGUE)
                .speaker("Guard")
                .text("We could use someone brave. Speak to the captain.")
                .next("end")
            .node("end", DialogueNode.NodeType.END)
            .start("greet")
            .buildAndRegister();
    }
    
    /**
     * Example 2: Quest offer with conditions
     */
    public static void createConditionalQuestOffer(Quest quest) {
        // Only offer quest if player is level 5+
        DialogueCondition levelCheck = new DialogueConditions.MinLevelCondition(5);
        
        DialogueTree questDialogue = new DialogueBuilder("elder_quest", "Elder Quest Offer")
            .node("greet", DialogueNode.NodeType.DIALOGUE)
                .speaker("Village Elder")
                .text("Greetings, adventurer.")
                .choiceIf("Do you need help?", "offer", levelCheck)
                .choiceIf("You seem too inexperienced...", "too_weak", 
                    new DialogueConditions.NotCondition(levelCheck))
                .choice("Farewell", "end")
            .node("offer", DialogueNode.NodeType.QUEST_OFFER)
                .speaker("Village Elder")
                .text("Yes! " + quest.getDescription())
                .quest(quest.getId())
                .choice("I accept", "accept")
                .choice("Not now", "end")
            .node("accept", DialogueNode.NodeType.DIALOGUE)
                .speaker("Village Elder")
                .text(quest.getAcceptDialogue())
                .action(new DialogueActions.GiveQuestAction(quest))
                .next("end")
            .node("too_weak", DialogueNode.NodeType.DIALOGUE)
                .speaker("Village Elder")
                .text("Come back when you're stronger, young one.")
                .next("end")
            .node("end", DialogueNode.NodeType.END)
            .start("greet")
            .buildAndRegister();
    }
    
    /**
     * Example 3: Multi-path branching dialogue
     */
    public static void createBranchingDialogue() {
        DialogueTree branching = new DialogueBuilder("mysterious_stranger", "Mysterious Stranger")
            .node("greet", DialogueNode.NodeType.DIALOGUE)
                .speaker("Mysterious Stranger")
                .text("I sense great potential in you...")
                .choice("Who are you?", "ask_identity")
                .choice("What do you want?", "ask_purpose")
                .choice("I must go", "end")
            .node("ask_identity", DialogueNode.NodeType.DIALOGUE)
                .speaker("Mysterious Stranger")
                .text("My name is not important. What matters is your destiny.")
                .choice("Tell me more", "ask_destiny")
                .choice("This is nonsense", "dismiss")
            .node("ask_purpose", DialogueNode.NodeType.DIALOGUE)
                .speaker("Mysterious Stranger")
                .text("I want to help you unlock your true power.")
                .choice("How?", "ask_how")
                .choice("I don't trust you", "distrust")
            .node("ask_destiny", DialogueNode.NodeType.DIALOGUE)
                .speaker("Mysterious Stranger")
                .text("You are destined for greatness. But first, you must prove yourself.")
                .action(new DialogueActions.AwardXPAction(50))
                .next("end")
            .node("ask_how", DialogueNode.NodeType.DIALOGUE)
                .speaker("Mysterious Stranger")
                .text("Seek the ancient ruins to the north. There you will find what you seek.")
                .next("end")
            .node("dismiss", DialogueNode.NodeType.DIALOGUE)
                .speaker("Mysterious Stranger")
                .text("You will understand in time...")
                .next("end")
            .node("distrust", DialogueNode.NodeType.DIALOGUE)
                .speaker("Mysterious Stranger")
                .text("Wise, but unnecessary. I mean you no harm.")
                .next("end")
            .node("end", DialogueNode.NodeType.END)
            .start("greet")
            .buildAndRegister();
    }
    
    /**
     * Example 4: Quest progress tracking dialogue
     */
    public static void createProgressTrackingDialogue(Quest quest) {
        DialogueCondition hasQuest = new DialogueConditions.HasQuestCondition(quest.getId());
        DialogueCondition questComplete = new DialogueConditions.QuestCompletedCondition(quest.getId());
        DialogueCondition notHasQuest = new DialogueConditions.NotCondition(hasQuest);
        
        DialogueTree tracking = new DialogueBuilder("quest_tracker", "Quest Progress Tracker")
            .node("check", DialogueNode.NodeType.DIALOGUE)
                .speaker("Quest Giver")
                .text("Hello again!")
                .choiceIf("I have the quest active", "progress", hasQuest)
                .choiceIf("I completed it!", "complete", questComplete)
                .choiceIf("About that quest...", "offer", notHasQuest)
                .choice("Goodbye", "end")
            .node("progress", DialogueNode.NodeType.DIALOGUE)
                .speaker("Quest Giver")
                .text(quest.getProgressDialogue())
                .next("end")
            .node("complete", DialogueNode.NodeType.DIALOGUE)
                .speaker("Quest Giver")
                .text(quest.getCompleteDialogue())
                .action(new DialogueActions.AwardXPAction(quest.getExpReward()))
                .next("end")
            .node("offer", DialogueNode.NodeType.QUEST_OFFER)
                .speaker("Quest Giver")
                .text(quest.getDescription())
                .quest(quest.getId())
                .choice("Accept", "accept")
                .choice("Decline", "end")
            .node("accept", DialogueNode.NodeType.DIALOGUE)
                .speaker("Quest Giver")
                .text(quest.getAcceptDialogue())
                .action(new DialogueActions.GiveQuestAction(quest))
                .next("end")
            .node("end", DialogueNode.NodeType.END)
            .start("check")
            .buildAndRegister();
    }
    
    /**
     * Example 5: Create all NPC dialogues for a village
     */
    public static void initializeVillageDialogues() {
        DialogueDatabase db = DialogueDatabase.getInstance();
        
        // Blacksmith
        DialogueTree blacksmith = DialogueTemplates.createMerchant("blacksmith", "Blacksmith");
        db.mapNPCToDialogue("village_blacksmith", blacksmith.getId());
        
        // Innkeeper
        DialogueTree innkeeper = new DialogueBuilder("innkeeper_chat", "Innkeeper")
            .node("greet", DialogueNode.NodeType.DIALOGUE)
                .speaker("Innkeeper")
                .text("Welcome to my inn! Can I get you something?")
                .choice("Tell me about this town", "info")
                .choice("I'd like to rest", "rest")
                .choice("Nothing, thanks", "end")
            .node("info", DialogueNode.NodeType.DIALOGUE)
                .speaker("Innkeeper")
                .text("This is a peaceful village, though lately goblins have been troubling us.")
                .next("end")
            .node("rest", DialogueNode.NodeType.DIALOGUE)
                .speaker("Innkeeper")
                .text("Rest well, adventurer!")
                .action(new DialogueActions.HealPlayerAction(9999))
                .next("end")
            .node("end", DialogueNode.NodeType.END)
            .start("greet")
            .buildAndRegister();
        db.mapNPCToDialogue("village_innkeeper", innkeeper.getId());
        
        System.out.println("Village dialogues initialized");
    }
}