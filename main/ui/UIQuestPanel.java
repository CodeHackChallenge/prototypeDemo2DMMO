package dev.main.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import dev.main.entity.Entity;
import dev.main.entity.NPC;
import dev.main.quest.QuestLog;
import dev.main.quest.QuestObjective;
import dev.main.state.GameState;

/**
 * Quest panel UI - Shows active and completed quests
 */
public class UIQuestPanel extends UIComponent {
    
    // Cached fonts
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font TAB_FONT = new Font("Arial", Font.BOLD, 12);
    private static final Font QUEST_NAME_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font QUEST_DESC_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final Font OBJECTIVE_FONT = new Font("Arial", Font.PLAIN, 11);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 12);
    
    private GameState gameState;
    private Entity player;
    
    // Tabs
    private boolean showingActive;  // true = active, false = completed
    private UIButton activeTabButton;
    private UIButton completedTabButton;
    private UIButton abandonButton;
    
    // Quest list
    private List<QuestEntry> questEntries;
    private QuestEntry selectedQuest;
    
    // Scroll
    private int scrollOffsetY;
    private int maxScrollY;
    
    // Layout
    private int padding;
    private int listX, listY, listWidth, listHeight;
    private int detailX, detailY, detailWidth, detailHeight;
    
    // Colors
    private Color backgroundColor;
    private Color borderColor;
    private Color selectedColor;
    private Color hoverColor;
    
    public UIQuestPanel(int x, int y, int width, int height, GameState gameState) {
        super(x, y, width, height);
        
        this.gameState = gameState;
        this.player = gameState.getPlayer();
        this.showingActive = true;
        this.questEntries = new ArrayList<>();
        this.selectedQuest = null;
        
        this.scrollOffsetY = 0;
        this.maxScrollY = 0;
        this.padding = 16;
        
        this.backgroundColor = new Color(20, 20, 30, 240);
        this.borderColor = new Color(150, 150, 180);
        this.selectedColor = new Color(80, 100, 120, 200);
        this.hoverColor = new Color(60, 80, 100, 150);
        
        calculateLayout();
        createButtons();
    }
    
    private void calculateLayout() {
        // Quest list on left (40% width)
        listX = x + padding;
        listY = y + padding + 60;  // Below title and tabs
        listWidth = (width * 40) / 100;
        listHeight = height - padding * 2 - 60;
        
        // Quest details on right (60% width)
        detailX = listX + listWidth + padding;
        detailY = listY;
        detailWidth = width - listWidth - padding * 3;
        detailHeight = listHeight;
    }
    
    private void createButtons() {
        int tabWidth = 120;
        int tabHeight = 32;
        int tabY = y + padding + 25;
        
        // Active quests tab
        activeTabButton = new UIButton(
            x + padding,
            tabY,
            tabWidth,
            tabHeight,
            "active_tab",
            "Active"
        );
        activeTabButton.setOnClick(() -> switchToActiveTab());
        
        // Completed quests tab
        completedTabButton = new UIButton(
            x + padding + tabWidth + 5,
            tabY,
            tabWidth,
            tabHeight,
            "completed_tab",
            "Completed"
        );
        completedTabButton.setOnClick(() -> switchToCompletedTab());
        
        // Abandon button (bottom right)
        int buttonWidth = 100;
        int buttonHeight = 36;
        abandonButton = new UIButton(
            detailX + detailWidth - buttonWidth,
            detailY + detailHeight - buttonHeight,
            buttonWidth,
            buttonHeight,
            "abandon",
            "Abandon"
        );
        abandonButton.setOnClick(() -> abandonSelectedQuest());
        abandonButton.setVisible(false);  // Only show when quest selected
    }
    
    private void switchToActiveTab() {
        showingActive = true;
        selectedQuest = null;
        // Hide abandon when switching tabs
        if (abandonButton != null) abandonButton.setVisible(false);
        refreshQuestList();
    }
    
    private void switchToCompletedTab() {
        showingActive = false;
        selectedQuest = null;
        // Hide abandon when switching to completed
        if (abandonButton != null) abandonButton.setVisible(false);
        refreshQuestList();
    }
    
    /**
     * Refresh the quest list from player's quest log
     */
    public void refreshQuestList() {
        questEntries.clear();
        
        QuestLog questLog = player.getComponent(QuestLog.class);
        if (questLog == null) return;
        
        List<Quest> quests = showingActive ? 
            questLog.getActiveQuests() : 
            questLog.getCompletedQuests();
        
        int entryY = 0;
        for (Quest quest : quests) {
            questEntries.add(new QuestEntry(quest, entryY));
            entryY += 60;  // Each entry is 60px tall
        }
        
        // Calculate max scroll
        int totalHeight = questEntries.size() * 60;
        maxScrollY = Math.max(0, totalHeight - listHeight);
        scrollOffsetY = Math.min(scrollOffsetY, maxScrollY);
    }
    
    private void abandonSelectedQuest() {
        if (selectedQuest == null) return;
 
        Quest questToAbandon = selectedQuest.quest;
        GameState gs = gameState;
        UIManager ui = gs.getUIManager();
        
        // Show confirmation dialog
        if (ui != null) {
        	if(questToAbandon.getQuestType() == Quest.QUEST_MAIN) {  
       		 	ui.showDialogue("NOTICE", "Main Quest can't be abandoned");   
       		 	return;
        	}
        	else {
        		ui.showConfirmation("Confirm Abandon",
                        "You will lose any progress you have",
                        "Continue",
                        "Cancel",
                        () -> {
                            // On confirm: remove from player's quest log, reset quest, update NPC indicator
                            QuestLog questLog = player.getComponent(QuestLog.class);
                            if (questLog != null) {   
                            	questLog.removeQuest(questToAbandon.getId()); 
                            } 
                            // Reset quest state so NPC can offer again
                            questToAbandon.reset();

                            // Find NPC and clear any current offered reference
                            String giverId = questToAbandon.getQuestGiver();
                            if (giverId != null) {
                                for (Entity e : gs.getEntities()) {
                                    NPC npc = e.getComponent(NPC.class);
                                    if (npc != null && giverId.equals(npc.getNpcId())) {
                                        // Ensure current offered quest cleared
                                        if (npc.getCurrentOfferedQuest() == questToAbandon) {
                                            npc.setCurrentOfferedQuest(null);
                                        }
                                        break;
                                    }
                                }
                            }

                            System.out.println("Quest abandoned: " + questToAbandon.getName());

                            selectedQuest = null;
                            if (abandonButton != null) abandonButton.setVisible(false);
                            refreshQuestList();

                            // Update quest indicators in UI
                            if (ui != null) ui.updateQuestIndicator();
                        },
                        () -> {
                            // Cancel - do nothing
                        }
                    );
        	}
            
        }
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
     
        // Refresh quest list each frame (in case quests updated)
        refreshQuestList();
        
        // Draw background
        g.setColor(backgroundColor);
        g.fillRect(x, y, width, height);
        
        // Draw border
        g.setColor(borderColor);
        g.setStroke(new java.awt.BasicStroke(2));
        g.drawRect(x, y, width, height);
        
        // Draw title
        Font originalFont = g.getFont();
        g.setFont(TITLE_FONT);
        g.setColor(new Color(255, 215, 0));
        g.drawString("Quest Log", x + padding, y + padding + 15);
        
        // Draw tabs
        renderTabs(g);
        
        // Draw separator
        g.setColor(new Color(100, 100, 120));
        g.drawLine(listX + listWidth + padding/2, listY, 
                   listX + listWidth + padding/2, listY + listHeight);
        
        // Draw quest list
        renderQuestList(g);
        
        // Draw quest details
        if (selectedQuest != null) {
            renderQuestDetails(g);
        } else {
            renderNoQuestSelected(g);
        }
        
        // Draw buttons
        activeTabButton.render(g);
        completedTabButton.render(g);
        if (selectedQuest != null && showingActive) {
            abandonButton.render(g);
        }
        
        g.setFont(originalFont);
    }
    
    private void renderTabs(Graphics2D g) {
        // Highlight active tab
        if (showingActive) {
            g.setColor(new Color(80, 100, 120, 220));
            g.fillRect(activeTabButton.getX(), activeTabButton.getY(), 
                      activeTabButton.getWidth(), activeTabButton.getHeight());
        } else {
            g.setColor(new Color(80, 100, 120, 220));
            g.fillRect(completedTabButton.getX(), completedTabButton.getY(), 
                      completedTabButton.getWidth(), completedTabButton.getHeight());
        }
    }
    
    private void renderQuestList(Graphics2D g) {
        // Create clipping region
        java.awt.Rectangle oldClip = g.getClipBounds();
        g.setClip(listX, listY, listWidth, listHeight);
        
        g.setFont(QUEST_NAME_FONT);
        
        for (QuestEntry entry : questEntries) {
            int entryY = listY + entry.yOffset - scrollOffsetY;
            
            // Skip if outside view
            if (entryY + 60 < listY || entryY > listY + listHeight) {
                continue;
            }
            
            // Draw entry background
            if (entry == selectedQuest) {
                g.setColor(selectedColor);
            } else if (entry.hovered) {
                g.setColor(hoverColor);
            } else {
                g.setColor(new Color(40, 40, 50, 150));
            }
            g.fillRect(listX, entryY, listWidth, 58);
            
            // Draw quest name
            g.setColor(new Color(255, 215, 0));
            g.drawString(entry.quest.getName(), listX + 8, entryY + 20);
            
            // Draw progress bar
            g.setFont(OBJECTIVE_FONT);
            float progress = entry.quest.getProgressPercent();
            int barWidth = listWidth - 16;
            int barHeight = 6;
            int barX = listX + 8;
            int barY = entryY + 35;
            
            // Bar background
            g.setColor(new Color(60, 60, 60));
            g.fillRect(barX, barY, barWidth, barHeight);
            
            // Bar fill
            int fillWidth = (int)(barWidth * progress);
            g.setColor(new Color(100, 200, 100));
            g.fillRect(barX, barY, fillWidth, barHeight);
            
            // Progress text
            g.setColor(new Color(200, 200, 200));
            String progressText = (int)(progress * 100) + "%";
            g.drawString(progressText, listX + 8, entryY + 52);
            
            // Draw border
            g.setColor(new Color(80, 80, 90));
            g.drawRect(listX, entryY, listWidth, 58);
        }
        
        g.setClip(oldClip);
    }
    
    private void renderQuestDetails(Graphics2D g) {
        Quest quest = selectedQuest.quest;
        int currentY = detailY + 10;
        
        // Quest name
        g.setFont(QUEST_NAME_FONT);
        g.setColor(new Color(255, 215, 0));
        g.drawString(quest.getName(), detailX + 8, currentY + 15);
        currentY += 30;
        
        // Description
        g.setFont(QUEST_DESC_FONT);
        g.setColor(new Color(200, 200, 200));
        String[] descLines = wrapText(g, quest.getDescription(), detailWidth - 16);
        for (String line : descLines) {
            g.drawString(line, detailX + 8, currentY);
            currentY += 16;
        }
        
        currentY += 10;
        
        // Objectives
        g.setFont(OBJECTIVE_FONT);
        g.setColor(new Color(180, 180, 180));
        g.drawString("Objectives:", detailX + 8, currentY);
        currentY += 18;
        
        for (QuestObjective obj : quest.getObjectives()) {
            String objText = "• " + obj.getDescription();
            if (obj.isComplete()) {
                objText += " ✓";
                g.setColor(new Color(100, 200, 100));
            } else {
                objText += " (" + obj.getCurrentProgress() + "/" + obj.getRequiredAmount() + ")";
                g.setColor(new Color(200, 200, 200));
            }
            g.drawString(objText, detailX + 16, currentY);
            currentY += 16;
        }
        
        currentY += 10;
        
        // Rewards
        if (quest.getExpReward() > 0 || quest.getAurelReward() > 0 || !quest.getItemRewards().isEmpty()) {
            g.setColor(new Color(180, 180, 180));
            g.drawString("Rewards:", detailX + 8, currentY);
            currentY += 18;
            
            g.setColor(new Color(100, 255, 100));
            
            if (quest.getExpReward() > 0) {
                g.drawString("• " + quest.getExpReward() + " XP", detailX + 16, currentY);
                currentY += 16;
            }
            
            if (quest.getAurelReward() > 0) {
                g.drawString("• " + quest.getAurelReward() + " Aurel", detailX + 16, currentY);
                currentY += 16;
            }
            
            for (String itemId : quest.getItemRewards()) {
                g.drawString("• " + itemId, detailX + 16, currentY);
                currentY += 16;
            }
        }
    }
    
    private void renderNoQuestSelected(Graphics2D g) {
        g.setFont(QUEST_DESC_FONT);
        g.setColor(new Color(150, 150, 150));
        
        String message = showingActive ? 
            "Select a quest to view details" : 
            "No completed quests yet";
        
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(message);
        int textX = detailX + (detailWidth - textWidth) / 2;
        int textY = detailY + detailHeight / 2;
        
        g.drawString(message, textX, textY);
    }
    
    private String[] wrapText(Graphics2D g, String text, int maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        
        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            int lineWidth = fm.stringWidth(testLine);
            
            if (lineWidth > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        
        return lines.toArray(new String[0]);
    }
    
    @Override
    public void update(float delta) {
        if (!visible) return;
        
        activeTabButton.update(delta);
        completedTabButton.update(delta);
        abandonButton.update(delta);
    }
    
    public void handleMouseMove(int mouseX, int mouseY) {
        if (!visible) return;
        
        // Update button hovers
        updateButtonHover(activeTabButton, mouseX, mouseY);
        updateButtonHover(completedTabButton, mouseX, mouseY);
        updateButtonHover(abandonButton, mouseX, mouseY);
        
        // Update quest entry hovers
        for (QuestEntry entry : questEntries) {
            int entryY = listY + entry.yOffset - scrollOffsetY;
            
            boolean contains = mouseX >= listX && mouseX <= listX + listWidth &&
                             mouseY >= entryY && mouseY <= entryY + 58;
            
            entry.hovered = contains;
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
        
        // Check buttons
        if (activeTabButton.contains(mouseX, mouseY)) {
            return activeTabButton.onClick();
        }
        
        if (completedTabButton.contains(mouseX, mouseY)) {
            return completedTabButton.onClick();
        }
        
        if (abandonButton.isVisible() && abandonButton.contains(mouseX, mouseY)) {
            return abandonButton.onClick();
        }
        
        // Check quest entries
        for (QuestEntry entry : questEntries) {
            int entryY = listY + entry.yOffset - scrollOffsetY;
            
            if (mouseX >= listX && mouseX <= listX + listWidth &&
                mouseY >= entryY && mouseY <= entryY + 58) {
                selectedQuest = entry;
                // Show abandon only for active tab
                if (abandonButton != null) abandonButton.setVisible(showingActive);
                return true;
            }
        }
        
        // Consume click if inside panel
        return this.contains(mouseX, mouseY);
    }
    
    public void handleScroll(int wheelRotation) {
        if (!visible) return;
        
        int scrollAmount = wheelRotation * 20;
        scrollOffsetY = Math.max(0, Math.min(maxScrollY, scrollOffsetY + scrollAmount));
    }
    
    // Quest entry helper class
    private static class QuestEntry {
        Quest quest;
        int yOffset;
        boolean hovered;
        
        QuestEntry(Quest quest, int yOffset) {
            this.quest = quest;
            this.yOffset = yOffset;
            this.hovered = false;
        }
    }
}