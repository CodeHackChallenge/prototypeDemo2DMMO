package dev.main.dialogue;

public class TestDialogueFlow {
    public static void main(String[] args) {
        String path = "/dialogues/fionne_intro.json";
        DialogueTree tree = EnhancedDialogueLoader.loadFromFile(path);
        if (tree == null) {
            System.err.println("Failed to load dialogue from: " + path);
            return;
        }

        System.out.println("Loaded dialogue: " + tree.getName() + " (" + tree.getId() + ")");

        DialogueNode node = tree.start(null);
        if (node == null) {
            System.out.println("No start node");
            return;
        }

        int step = 0;
        while (node != null && !node.isEnd() && step < 50) {
            System.out.println("Node: " + node.getId());
            System.out.println((node.getSpeakerName() == null ? "" : node.getSpeakerName() + ": ") + node.getText());

            if (node.hasChoices()) {
                System.out.println("Choices:");
                var choices = node.getAvailableChoices(null);
                for (int i = 0; i < choices.size(); i++) {
                    System.out.println("  " + i + ") " + choices.get(i).getText() + " -> " + choices.get(i).getTargetNodeId());
                }

                // Auto-select the first choice
                System.out.println("Auto-selecting choice 0\n");
                node = tree.choose(0, null);
            } else {
                // Follow next
                node = tree.next(null);
            }

            step++;
        }

        if (node == null) {
            System.out.println("Dialogue ended (null node)");
        } else if (node.isEnd()) {
            System.out.println("Reached end node: " + node.getId());
        } else {
            System.out.println("Stopped after steps: " + step);
        }
    }
}
