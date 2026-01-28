package dev.main.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import dev.main.Engine;
import dev.main.dialogue.DialogueChoice;
import dev.main.dialogue.DialogueManager;
import dev.main.dialogue.DialogueNode;
import dev.main.entity.Entity;
import dev.main.entity.NPC;
import dev.main.quest.QuestLog;

/**
 * Enhanced dialogue box with support for branching dialogue trees
 */
public class UIDialogueBoxEnhanced extends UIComponent {
    
    // Cached fonts
    private static final Font NPC_NAME_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font DIALOGUE_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font CHOICE_FONT = new Font("Arial", Font.PLAIN, 13);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 12);
    
    private DialogueManager dialogueManager;
    private Entity player;
    private Entity npcEntity;
    
    private String npcName;
    private String dialogueText;
    private DialogueNode currentNode;
    
    // Choice buttons
    private List<ChoiceButton> choiceButtons;
    
    // Navigation buttons
    private UIButton continueButton;
    private UIButton closeButton;
    private UIButton acceptButton;
    private UIButton declineButton;
    
    // Callbacks
    private Runnable onClose;
    
    // Visual
    private Color backgroundColor;
    private Color borderColor;
    private Color npcNameColor;
    private Color textColor;
    private Color choiceHoverColor;
    
    // Layout
    private int padding;
    
    public UIDialogueBoxEnhanced(int x, int y, int width, int height) {
        super(x, y, width, height);
        
        this.dialogueManager = DialogueManager.getInstance();
        this.player = null;
        
        this.npcName = "NPC";
        this.dialogueText = "";
        this.currentNode = null;
        
        this.choiceButtons = new ArrayList<>();
        
        this.backgroundColor = new Color(20, 20, 30, 240);
        this.borderColor = new Color(150, 150, 180);
        this.npcNameColor = new Color(255, 215, 0);
        this.textColor = new Color(220, 220, 220);
        this.choiceHoverColor = new Color(80, 100, 120, 200);
        
        this.padding = 16;
        
        createButtons();
    }
    
    public void setPlayer(Entity player) {
        this.player = player;
    }
    
    private void createButtons() {
        int buttonWidth = 100;
        int buttonHeight = 36;
        int buttonY = y + height - buttonHeight - padding;
        int centerX = x + width / 2;
        
        // Continue button
        continueButton = new UIButton(
            centerX - buttonWidth / 2,
            buttonY,
            buttonWidth,
            buttonHeight,
            "continue",
            "Continue"
        );
        continueButton.setOnClick(() -> handleContinue());
        continueButton.setVisible(false);
        
        // Close button
        closeButton = new UIButton(
            centerX - buttonWidth / 2,
            buttonY,
            buttonWidth,
            buttonHeight,
            "close",
            "Close"
        );
        closeButton.setOnClick(() -> handleClose());
        closeButton.setVisible(false);

        // Accept button (for quest offers)
        acceptButton = new UIButton(
            centerX - buttonWidth - 8,
            buttonY,
            buttonWidth,
            buttonHeight,
            "accept",
            "Accept"
        );
        acceptButton.setOnClick(() -> handleAccept());
        acceptButton.setVisible(false);

        // Decline button
        declineButton = new UIButton(
            centerX + 8,
            buttonY,
            buttonWidth,
            buttonHeight,
            "decline",
            "Decline"
        );
        declineButton.setOnClick(() -> handleDecline());
        declineButton.setVisible(false);
    }
    
    /**
     * Start a dialogue tree
     */
    public void startDialogue(String dialogueId, Entity npc, Entity player) {
        this.player = player;
        this.npcEntity = npc;
        
        NPC npcComponent = npc.getComponent(NPC.class);
        this.npcName = npcComponent != null ? npcComponent.getNpcName() : npc.getName();
        
        DialogueNode node = dialogueManager.startDialogue(dialogueId, npc, player);
        showNode(node);
        
        this.setVisible(true);
    }
    
    /**
     * Show a dialogue node
     */
    private void showNode(DialogueNode node) {
        if (node == null) {
            handleClose();
            return;
        }
        
        this.currentNode = node;
        this.dialogueText = node.getText();
        
        // Clear previous choices
        choiceButtons.clear();
        // If node contains explicit choices, show them regardless of declared type
        if (node.hasChoices()) {
            showChoiceNode(node);
            return;
        }

        // Handle different node types
        switch (node.getType()) {
            case DIALOGUE:
                showDialogueNode(node);
                break;

            case PLAYER_CHOICE:
                showChoiceNode(node);
                break;

            case QUEST_OFFER:
                showQuestOfferNode(node);
                break;

            case END:
                showEndNode(node);
                break;

            default:
                showDialogueNode(node);
                break;
        }
    }
    
    private void showDialogueNode(DialogueNode node) {
        // Show continue button if there's a next node
        if (node.getNextNodeId() != null) {
            continueButton.setVisible(true);
            closeButton.setVisible(false);
        } else {
            continueButton.setVisible(false);
            closeButton.setVisible(true);
        }
    }
    
    private void showChoiceNode(DialogueNode node) {
        // Hide default buttons
        continueButton.setVisible(false);
        closeButton.setVisible(false);
        
        // Create choice buttons
        List<DialogueChoice> choices = node.getAvailableChoices(player);
        int choiceY = y + height - 150;
        int choiceHeight = 40;
        int gap = 10;
        
        for (int i = 0; i < choices.size(); i++) {
            DialogueChoice choice = choices.get(i);
            int buttonY = choiceY + (i * (choiceHeight + gap));
            
            ChoiceButton button = new ChoiceButton(
                x + padding,
                buttonY,
                width - padding * 2,
                choiceHeight,
                i,
                choice.getText()
            );
            choiceButtons.add(button);
        }
    }
    
    private void showQuestOfferNode(DialogueNode node) {
        // Find quest by id on the NPC (if available)
        String questId = node.getQuestId();
        Quest offered = null;
        if (npcEntity != null && questId != null) {
            NPC npcComp = npcEntity.getComponent(NPC.class);
            if (npcComp != null) {
                for (Quest q : npcComp.getAvailableQuests()) {
                    if (q.getId().equals(questId)) {
                        offered = q;
                        npcComp.setCurrentOfferedQuest(q);
                        break;
                    }
                }
            }
        }

        // Show accept/decline buttons when a quest object is available
        if (offered != null) {
            acceptButton.setVisible(true);
            declineButton.setVisible(true);
            continueButton.setVisible(false);
            closeButton.setVisible(false);
            // Store offered quest on the dialogue box via npc component
        } else {
            // Fallback to regular dialogue flow
            showDialogueNode(node);
        }
    }

    private void handleAccept() {
        if (npcEntity == null || player == null) return;

        NPC npcComp = npcEntity.getComponent(NPC.class);
        Quest quest = npcComp != null ? npcComp.getCurrentOfferedQuest() : null;
        if (quest != null) {
            quest.accept();
            QuestLog ql = player.getComponent(QuestLog.class);
            if (ql != null) ql.addQuest(quest);
            System.out.println("Quest accepted via enhanced UI: " + quest.getName());

            // Update UI: unlock quest button and open quest panel
            try {
                Engine eng = Engine.getInstance();
                if (eng != null && eng.getGameState() != null && eng.getGameState().getUIManager() != null) {
                    UIManager ui = eng.getGameState().getUIManager();
                    ui.unlockMenuButton("quest");
                    UIButton qb = ui.getMenuButton("quest");
                    if (qb != null) qb.onClick();
                    ui.updateQuestIndicator();
                }
            } catch (Exception e) {
                System.err.println("Failed to update UI after accept: " + e.getMessage());
            }
        }

        handleClose();
    }

    private void handleDecline() {
        // Clear current offered quest on NPC if present
        if (npcEntity != null) {
            NPC npcComp = npcEntity.getComponent(NPC.class);
            if (npcComp != null) npcComp.setCurrentOfferedQuest(null);
        }
        handleClose();
    }
    
    private void showEndNode(DialogueNode node) {
        continueButton.setVisible(false);
        closeButton.setVisible(true);
    }
    
    private void handleContinue() {
        DialogueNode next = dialogueManager.next(player);
        showNode(next);
    }
    
    private void handleChoice(int choiceIndex) {
        // Check for automatic quest acceptance: if current node is a QUEST_OFFER
        // and the selected choice targets the "accept" path, accept immediately.
        DialogueNode current = this.currentNode;
        if (current != null && current.getType() == DialogueNode.NodeType.QUEST_OFFER) {
            List<DialogueChoice> choices = current.getAvailableChoices(player);
            if (choiceIndex >= 0 && choiceIndex < choices.size()) {
                DialogueChoice choice = choices.get(choiceIndex);
                String target = choice.getTargetNodeId();
                if (target != null && target.equalsIgnoreCase("accept")) {
                    acceptQuestNow();
                }
            }
        }

        DialogueNode next = dialogueManager.choose(choiceIndex, player);
        showNode(next);
    }

    private void acceptQuestNow() {
        if (npcEntity == null || player == null) return;

        NPC npcComp = npcEntity.getComponent(NPC.class);
        Quest quest = npcComp != null ? npcComp.getCurrentOfferedQuest() : null;
        if (quest == null) {
            // Try to find by questId on the current node
            if (currentNode != null && currentNode.getQuestId() != null && npcComp != null) {
                for (Quest q : npcComp.getAvailableQuests()) {
                    if (q.getId().equals(currentNode.getQuestId())) {
                        quest = q;
                        npcComp.setCurrentOfferedQuest(q);
                        break;
                    }
                }
            }
        }

        if (quest != null) {
            quest.accept();
            QuestLog ql = player.getComponent(QuestLog.class);
            if (ql != null) ql.addQuest(quest);
            System.out.println("Quest accepted via choice: " + quest.getName());

            try {
                Engine eng = Engine.getInstance();
                if (eng != null && eng.getGameState() != null && eng.getGameState().getUIManager() != null) {
                    UIManager ui = eng.getGameState().getUIManager();
                    ui.unlockMenuButton("quest");
                    UIButton qb = ui.getMenuButton("quest");
                    if (qb != null) qb.onClick();
                    ui.updateQuestIndicator();
                }
            } catch (Exception e) {
                System.err.println("Failed to update UI after accepting quest via choice: " + e.getMessage());
            }
        }
    }
    
    private void handleClose() {
        dialogueManager.endDialogue();
        this.setVisible(false);
        
        if (onClose != null) {
            onClose.run();
        }
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
        
        // Draw background
        g.setColor(backgroundColor);
        g.fillRect(x, y, width, height);
        
        // Draw border
        g.setColor(borderColor);
        g.setStroke(new java.awt.BasicStroke(2));
        g.drawRect(x, y, width, height);
        
        // Draw NPC name
        Font originalFont = g.getFont();
        g.setFont(NPC_NAME_FONT);
        g.setColor(npcNameColor);
        g.drawString(npcName, x + padding, y + padding + 20);
        
        // Draw separator
        g.setColor(new Color(100, 100, 120));
        g.drawLine(x + padding, y + padding + 30, x + width - padding, y + padding + 30);
        
        // Draw dialogue text
        g.setFont(DIALOGUE_FONT);
        g.setColor(textColor);
        drawWrappedText(g, dialogueText, x + padding, y + padding + 55, width - padding * 2);
        
        // Draw choice buttons
        for (ChoiceButton button : choiceButtons) {
            button.render(g);
        }
        
        // Draw navigation buttons
        continueButton.render(g);
        closeButton.render(g);
        
        g.setFont(originalFont);
    }
    
    private void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        
        StringBuilder line = new StringBuilder();
        int currentY = y;
        
        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            int lineWidth = fm.stringWidth(testLine);
            
            if (lineWidth > maxWidth && line.length() > 0) {
                g.drawString(line.toString(), x, currentY);
                currentY += fm.getHeight();
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        
        if (line.length() > 0) {
            g.drawString(line.toString(), x, currentY);
        }
    }
    
    @Override
    public void update(float delta) {
        if (!visible) return;
        
        continueButton.update(delta);
        closeButton.update(delta);
        
        for (ChoiceButton button : choiceButtons) {
            button.update(delta);
        }
    }
    
    public void handleMouseMove(int mouseX, int mouseY) {
        if (!visible) return;
        
        updateButtonHover(continueButton, mouseX, mouseY);
        updateButtonHover(closeButton, mouseX, mouseY);
        
        for (ChoiceButton button : choiceButtons) {
            button.updateHover(mouseX, mouseY);
        }
    }
    
    private void updateButtonHover(UIButton button, int mouseX, int mouseY) {
        if (!button.isVisible()) return;
        
        boolean contains = button.contains(mouseX, mouseY);
        
        if (contains && !button.isHovered()) {
            button.onMouseEnter();
        } else if (!contains && button.isHovered()) {
            button.onMouseExit();
        }
    }
    
    public boolean handleClick(int mouseX, int mouseY) {
        if (!visible) return false;
        
        if (continueButton.isVisible() && continueButton.contains(mouseX, mouseY)) {
            return continueButton.onClick();
        }
        
        if (closeButton.isVisible() && closeButton.contains(mouseX, mouseY)) {
            return closeButton.onClick();
        }
        
        for (ChoiceButton button : choiceButtons) {
            if (button.contains(mouseX, mouseY)) {
                handleChoice(button.choiceIndex);
                return true;
            }
        }
        
        return this.contains(mouseX, mouseY);
    }
    
    public void setOnClose(Runnable callback) {
        this.onClose = callback;
    }
    
    /**
     * Choice button helper class
     */
    private class ChoiceButton {
        int x, y, width, height;
        int choiceIndex;
        String text;
        boolean hovered;
        
        ChoiceButton(int x, int y, int width, int height, int choiceIndex, String text) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.choiceIndex = choiceIndex;
            this.text = text;
            this.hovered = false;
        }
        
        void render(Graphics2D g) {
            // Background
            if (hovered) {
                g.setColor(choiceHoverColor);
            } else {
                g.setColor(new Color(40, 40, 50, 200));
            }
            g.fillRect(x, y, width, height);
            
            // Border
            g.setColor(new Color(100, 100, 120));
            g.setStroke(new java.awt.BasicStroke(1));
            g.drawRect(x, y, width, height);
            
            // Text
            g.setFont(CHOICE_FONT);
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            int textY = y + (height + fm.getHeight()) / 2 - 2;
            g.drawString("• " + text, x + 10, textY);
        }
        
        void update(float delta) {
            // No updates needed
        }
        
        void updateHover(int mouseX, int mouseY) {
            hovered = mouseX >= x && mouseX <= x + width &&
                     mouseY >= y && mouseY <= y + height;
        }
        
        boolean contains(int mouseX, int mouseY) {
            return hovered;
        }
    }
}