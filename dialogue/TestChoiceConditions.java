package dev.main.dialogue;

import dev.main.entity.Entity;
import dev.main.entity.NPC;
import dev.main.entity.NPC.NPCType;
import dev.main.quest.QuestLog;
import dev.main.quest.QuestObjective;
import dev.main.ui.Quest; 

public class TestChoiceConditions {
    public static void main(String[] args) {
        String path = "/dialogues/fionne_intro.json";
        DialogueTree tree = EnhancedDialogueLoader.loadFromFile(path);
        if (tree == null) {
            System.err.println("Failed to load dialogue from: " + path);
            return;
        }

        // Create player and components
        Entity player = new Entity("player1");
        QuestLog ql = new QuestLog();
        player.addComponent(ql);

        // Create NPC with quest
        Entity npc = new Entity("npc_fionne");
        NPC npcComp = new NPC("npc_fionne", "Fionne", NPC.NPCType.QUEST_GIVER);
        npc.addComponent(npcComp);

        // Create quest matching JSON (goblin_slayer)
        Quest q = new Quest("goblin_slayer", "Goblin Slayer", "Defeat 5 goblins", Quest.QuestType.KILL);
        q.addObjective(new QuestObjective("goblin_kill", "Kill goblins", 5));
        npcComp.addQuest(q);

        System.out.println("-- Start before accepting quest --");
        DialogueNode start = tree.start(player, npc);
        System.out.println("Start node: " + (start != null ? start.getId() : "null"));

        // Simulate accepting quest
        System.out.println("-- Accepting quest programmatically --");
        q.accept();
        ql.addQuest(q);

        // Restart dialogue to hit progress node
        tree.reset();
        DialogueNode progress = tree.start(player, npc);
        System.out.println("Start after accepting: " + (progress != null ? progress.getId() : "null"));
        if (progress != null && progress.hasChoices()) {
            System.out.println("Available choices when NOT completed:");
            for (DialogueChoice c : progress.getAvailableChoices(player)) {
                System.out.println(" - " + c.getText());
            }
        }

        // Complete the quest objectives
        System.out.println("-- Completing quest objectives --");
        q.updateProgress("goblin_kill", 5);

        // Restart dialogue to hit complete node
        tree.reset();
        DialogueNode completeStart = tree.start(player, npc);
        System.out.println("Start after completion: " + (completeStart != null ? completeStart.getId() : "null"));
        if (completeStart != null && completeStart.hasChoices()) {
            System.out.println("Available choices when completed:");
            for (DialogueChoice c : completeStart.getAvailableChoices(player)) {
                System.out.println(" - " + c.getText());
            }
        }
    }
}
