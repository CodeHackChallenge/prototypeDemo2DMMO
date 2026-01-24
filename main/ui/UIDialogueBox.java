package dev.main.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialogue box for NPC conversations and quest acceptance
 */
public class UIDialogueBox extends UIComponent {
    
    // Cached fonts
    private static final Font NPC_NAME_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font DIALOGUE_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font QUEST_TITLE_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font QUEST_DESC_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 12);
    
    private String npcName;
    private String dialogueText;
    private Quest offeredQuest;
    
    // Buttons
    private UIButton acceptButton;
    private UIButton declineButton;
    private UIButton closeButton;
     
    
    // Callbacks
    private Runnable onAccept;
    private Runnable onDecline;
    private Runnable onClose;
     
    
    // Visual
    private Color backgroundColor;
    private Color borderColor;
    private Color npcNameColor;
    private Color textColor;
    
    // Layout
    private int padding;
    private int textAreaHeight;
    
    public UIDialogueBox(int x, int y, int width, int height) {
        super(x, y, width, height);
        
        this.npcName = "NPC";
        this.dialogueText = "";
        this.offeredQuest = null;
        
        this.backgroundColor = new Color(20, 20, 30, 240);
        this.borderColor = new Color(150, 150, 180);
        this.npcNameColor = new Color(255, 215, 0);
        this.textColor = new Color(220, 220, 220);
        
        this.padding = 16;
        this.textAreaHeight = height - 100;  // Leave room for buttons
        
        createButtons();
    }
    
    private void createButtons() {
        int buttonWidth = 100;
        int buttonHeight = 36;
        int buttonY = y + height - buttonHeight - padding;
        int centerX = x + width / 2; 
        
        // Accept button (left of center)
        acceptButton = new UIButton(
            centerX - buttonWidth - 10,
            buttonY,
            buttonWidth,
            buttonHeight,
            "accept",
            "oAccept"
        );
        acceptButton.setOnClick(() -> {
            if (onAccept != null) onAccept.run();
        });
        acceptButton.setVisible(false);  // Hidden by default
        
        // Decline button (right of center)
        declineButton = new UIButton(
            centerX + 10,
            buttonY,
            buttonWidth,
            buttonHeight,
            "decline",
            "Decline"
        );
        declineButton.setOnClick(() -> {
            if (onDecline != null) onDecline.run();
        });
        declineButton.setVisible(false);  // Hidden by default
        
        // Close button (centered, for non-quest dialogue)
        closeButton = new UIButton(
            centerX - buttonWidth / 2,
            buttonY,
            buttonWidth,
            buttonHeight,
            "close",
            "Close"
        );
        closeButton.setOnClick(() -> {
            if (onClose != null) onClose.run();
        });
        closeButton.setVisible(true);
    }
    
    /**
     * Show dialogue without quest
     */
    public void showDialogue(String npcName, String dialogue) {
        this.npcName = npcName;
        this.dialogueText = dialogue;
        this.offeredQuest = null;
        
        // Show only close button
        acceptButton.setVisible(false);
        declineButton.setVisible(false);
        closeButton.setVisible(true); 
        this.setVisible(true);
    } 
    /**
     * Show dialogue with quest offer
     */
    public void showQuestOffer(String npcName, Quest quest) {
        this.npcName = npcName;
        this.offeredQuest = quest;
        this.dialogueText = quest.getDescription();
        
        // Show accept/decline buttons
        // Ensure hover state cleared when dialog appears
        if (acceptButton != null) acceptButton.onMouseExit();
        if (declineButton != null) declineButton.onMouseExit();
        acceptButton.setVisible(true);
        declineButton.setVisible(true);
        closeButton.setVisible(false);
        
        this.setVisible(true);
    }
    
    /**
     * Show message with accept/decline buttons (for intro, confirmations, etc.)
     */
    public void showMessageWithAccept(String message, Runnable onAccept, Runnable onDecline) {
        this.npcName = "Fionne";
        this.dialogueText = message;
        this.offeredQuest = null;
        this.onAccept = onAccept;
        this.onDecline = onDecline;
        
        // Show accept/decline buttons
        if (acceptButton != null) acceptButton.onMouseExit();
        if (declineButton != null) declineButton.onMouseExit();
        acceptButton.setVisible(true);
        declineButton.setVisible(true);
        closeButton.setVisible(false);
        
        this.setVisible(true);
    }
    
    /**
     * Show quest completion dialogue
     */
    public void showQuestComplete(String npcName, Quest quest) {
        this.npcName = npcName;
        this.offeredQuest = quest;
        this.dialogueText = quest.getCompleteDialogue();
        
        // Show Confirm/Cancel for claiming rewards
        acceptButton.setLabel("Confirm");
        declineButton.setLabel("Cancel");

        // Wire accept to claim rewards via onClose callback (set by UIManager)
        this.onAccept = () -> {
            if (onClose != null) onClose.run();
            close();
        };

        // Cancel should just close without claiming
        this.onDecline = () -> {
            close();
        };

        // Ensure hover cleared
        if (acceptButton != null) acceptButton.onMouseExit();
        if (declineButton != null) declineButton.onMouseExit();

        acceptButton.setVisible(true);
        declineButton.setVisible(true);
        closeButton.setVisible(false);

        this.setVisible(true);
    }

    /**
     * Show a generic confirmation dialog with Continue/Cancel semantics
     */
    public void showConfirmation(String title, String message, String confirmLabel, String cancelLabel,
                                 Runnable onConfirm, Runnable onCancel) {
        this.npcName = title;
        this.dialogueText = message;
        this.offeredQuest = null;

        // Configure buttons
        acceptButton.setLabel(confirmLabel != null ? confirmLabel : "Continue");
        declineButton.setLabel(cancelLabel != null ? cancelLabel : "Cancel");

        // Wire callbacks: wrap user callbacks to also close the dialog
        this.onAccept = () -> {
            if (onConfirm != null) onConfirm.run();
            close();
        };

        this.onDecline = () -> {
            if (onCancel != null) onCancel.run();
            close();
        };

        acceptButton.setVisible(true);
        declineButton.setVisible(true);
        closeButton.setVisible(false);

        this.setVisible(true);
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
        
        // Draw separator line
        g.setColor(new Color(100, 100, 120));
        g.drawLine(x + padding, y + padding + 30, x + width - padding, y + padding + 30);
        
        // Draw dialogue text
        g.setFont(DIALOGUE_FONT);
        g.setColor(textColor);
        drawWrappedText(g, dialogueText, x + padding, y + padding + 50, width - padding * 2);
        
        // Draw quest info if present
        if (offeredQuest != null) {
            drawQuestInfo(g);
        }
        
        // Draw buttons
        acceptButton.render(g);
        declineButton.render(g);
        closeButton.render(g);
        
        g.setFont(originalFont);
    }
    
    /**
     * Draw quest information (name, objectives, rewards)
     */
    private void drawQuestInfo(Graphics2D g) {
        int questInfoY = y + padding + 150;
        
        // Quest name
        g.setFont(QUEST_TITLE_FONT);
        g.setColor(new Color(255, 215, 0));
        g.drawString("Quest: " + offeredQuest.getName(), x + padding, questInfoY);
        
        questInfoY += 25;
        
        // Objectives
        g.setFont(QUEST_DESC_FONT);
        g.setColor(new Color(200, 200, 200));
        g.drawString("Objectives:", x + padding, questInfoY);
        questInfoY += 18;
        
        g.setColor(textColor);
        String objectiveText = offeredQuest.getCurrentObjectiveDescription();
        String[] objectiveLines = objectiveText.split("\n");
        for (String line : objectiveLines) {
            g.drawString(line, x + padding + 10, questInfoY);
            questInfoY += 16;
        }
        
        questInfoY += 10;
        
        // Rewards
        g.setColor(new Color(200, 200, 200));
        g.drawString("Rewards:", x + padding, questInfoY);
        questInfoY += 18;
        
        g.setColor(new Color(100, 255, 100));
        if (offeredQuest.getExpReward() > 0) {
            g.drawString("• " + offeredQuest.getExpReward() + " XP", x + padding + 10, questInfoY);
            questInfoY += 16;
        }
        
        if (offeredQuest.getAurelReward() > 0) {
            g.drawString("• " + offeredQuest.getAurelReward() + " Gold", x + padding + 10, questInfoY);
            questInfoY += 16;
        }
        
        for (String itemId : offeredQuest.getItemRewards()) {
            g.drawString("• " + itemId, x + padding + 10, questInfoY);
            questInfoY += 16;
        }
    }
    
    /**
     * Draw text with word wrapping
     */
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
        
        acceptButton.update(delta);
        declineButton.update(delta);
        closeButton.update(delta);
    }
    
    /**
     * Handle mouse movement for button hover
     */
    public void handleMouseMove(int mouseX, int mouseY) {
        if (!visible) return;
        
        updateButtonHover(acceptButton, mouseX, mouseY);
        updateButtonHover(declineButton, mouseX, mouseY);
        updateButtonHover(closeButton, mouseX, mouseY);
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
    
    /**
     * Handle click
     */
    public boolean handleClick(int mouseX, int mouseY) {
        if (!visible) return false;
        
        if (acceptButton.isVisible() && acceptButton.contains(mouseX, mouseY)) {
            return acceptButton.onClick();
        }
        
        if (declineButton.isVisible() && declineButton.contains(mouseX, mouseY)) {
            return declineButton.onClick();
        }
        
        if (closeButton.isVisible() && closeButton.contains(mouseX, mouseY)) {
            return closeButton.onClick();
        }
        
        // Consume click if inside dialogue box
        return this.contains(mouseX, mouseY);
    }
    
    /**
     * Close the dialogue box
     */
    public void close() {
        this.setVisible(false);
        this.offeredQuest = null;

        // Reset buttons to default state and labels
        if (acceptButton != null) {
            acceptButton.setVisible(false);
            acceptButton.setLabel("Accept");
            acceptButton.onMouseExit();
        }
        if (declineButton != null) {
            declineButton.setVisible(false);
            declineButton.setLabel("Decline");
            declineButton.onMouseExit();
        }
        if (closeButton != null) {
            closeButton.setVisible(true);
        }

        // Clear callbacks
        this.onAccept = null;
        this.onDecline = null;
    }
    
    // Setters for callbacks
    public void setOnAccept(Runnable callback) {
        this.onAccept = callback;
    }
    
    public void setOnDecline(Runnable callback) {
        this.onDecline = callback;
    }
    
    public void setOnClose(Runnable callback) {
        this.onClose = callback;
    } 
    
    // Getters
    public Quest getOfferedQuest() {
        return offeredQuest;
    }
}