package dev.main.ui;

import java.awt.Graphics2D;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import dev.main.Engine;
import dev.main.buffs.Buff;
import dev.main.buffs.BuffFactory;
import dev.main.buffs.BuffManager;
import dev.main.entity.Entity;
import dev.main.entity.Experience;
import dev.main.item.Item;
import dev.main.item.ItemManager;
import dev.main.quest.IntroQuestHandler;
import dev.main.quest.QuestLog;
import dev.main.skill.Skill;
import dev.main.skill.SkillLevel;
import dev.main.state.GameLogic;
import dev.main.state.GameState;
import dev.main.stats.Stats;

/**
 * UI Manager with fixed layout:
 * Top: [Left Gear] [Hero Preview] [Right Gear]
 * Middle: [Inventory Tabs]
 * Bottom: [5x10 Scrollable Inventory Grid]
 */
public class UIManager implements MouseWheelListener {
     
	private UIDialogueBoxEnhanced enhancedDialogueBox;
	
	private static final String[] TAB_NAMES = {"Misc", "Weap", "Arm", "Acc", "Rune"};
	
	private UIDialogueBox dialogueBox;
	private UIQuestPanel questPanel;
	
    private List<UIPanel> panels;
    private UIComponent hoveredComponent;
    private GameState gameState;
    private GameLogic gameLogic;
    
    private UIPanel skillBar;
    private UIPanel verticalMenu;
    private UIPanel inventoryContainer;
    private UIScrollableInventoryPanel inventoryGrid;
    private UIPanel heroPreviewPanel;
    private UITooltipPanel tooltipPanel;
    
    private String currentInventoryTab = "Misc";
    private List<UIInventoryTab> inventoryTabs;
    
    private UIStatsPanel statsPanel;
    private boolean isStatsVisible = false;
    
    //buff
    private UIBuffBar buffBar;
    /*
    // Quest flags
    private boolean fionneSecondQuestAvailable = false;
    private String fionneNotification = null;
    private boolean secondQuestCompleted = false;
    
    private boolean introQuestCompleted = false;
    private boolean swordEquipped = false;
    
//    public boolean isFionneSecondQuestAvailable() {
//        return fionneSecondQuestAvailable && swordEquipped;
//    }

    public boolean isIntroQuestCompleted() {
        return introQuestCompleted;
    }
    
    public boolean hasSwordButNotEquipped() {
        return hasWoodenSwordInInventory() && !isWoodenSwordEquipped();
    }
*/
    
    public UIManager(GameState gameState) {
        this.gameState = gameState;
        this.gameLogic = null;
        this.panels = new ArrayList<>();
        this.inventoryTabs = new ArrayList<>();
        
        initializeUI();
    }
    
    public void setGameLogic(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
    }
    
    private void initializeUI() {
         createSkillBar();
         createBuffBar();  // NEW
         createVerticalMenu();
         createDialogueBox();
         createEnhancedDialogueBox();  // NEW
         createStatsPanel();
         
         
    }
    
    private void createBuffBar() {
        // Position above skill bar
        int buffBarY = Engine.HEIGHT - 64 - 20 - 40;  // Above skill bar
        int buffBarX = (Engine.WIDTH / 2);  // Centered
        
        buffBar = new UIBuffBar(buffBarX, buffBarY, gameState);
    }

	private void createEnhancedDialogueBox() {
         int width = 600;
         int height = 450;
         int x = (Engine.WIDTH - width) / 2;
         int y = (Engine.HEIGHT - height) / 2;
         
         enhancedDialogueBox = new UIDialogueBoxEnhanced(x, y, width, height);
         enhancedDialogueBox.setVisible(false);
         enhancedDialogueBox.setPlayer(gameState.getPlayer());
         
         enhancedDialogueBox.setOnClose(() -> {
             System.out.println("Dialogue closed");
         });
    }
   
    public UIDialogueBoxEnhanced getEnhancedDialogueBox() {
         return enhancedDialogueBox;
    }
    
    private void createStatsPanel() {
        int width = 300;
        int height = 400;
        int menuX = Engine.WIDTH - 58;  // From vertical menu
        int x = menuX - width - 10;
        int y = 50;
        statsPanel = new UIStatsPanel(x, y, width, height, gameState);
    }
    
    /**
     * ⭐ NEW: Create dialogue box (hidden by default)
     */
    private void createDialogueBox() {
        int width = 500;
        int height = 400;
        int x = (Engine.WIDTH - width) / 2;
        int y = (Engine.HEIGHT - height) / 2;
        
        dialogueBox = new UIDialogueBox(x, y, width, height);
        dialogueBox.setVisible(false);
        
        // Set callbacks
        dialogueBox.setOnAccept(() -> handleQuestAccept());
        dialogueBox.setOnDecline(() -> handleQuestDecline());
        dialogueBox.setOnClose(() -> handleDialogueClose());
        
        // Don't add to panels list yet - we'll handle it separately
    }
    /**
     * ⭐ NEW: Show dialogue with NPC
     */
    public void showDialogue(String npcName, String dialogue) {
        dialogueBox.showDialogue(npcName, dialogue);
    }
    /**
     * ⭐ NEW: Show quest offer from NPC
     */
    public void showQuestOffer(String npcName, Quest quest) {
        dialogueBox.showQuestOffer(npcName, quest);
    }

    /**
     * ⭐ NEW: Show quest completion
     */
    public void showQuestComplete(String npcName, Quest quest) {
        dialogueBox.showQuestComplete(npcName, quest);
    }

    /**
     * Show a confirmation dialog. Callback runs on confirm/cancel.
     */
    public void showConfirmation(String title, String message, String confirmLabel, String cancelLabel,
                                 Runnable onConfirm, Runnable onCancel) {
        dialogueBox.showConfirmation(title, message, confirmLabel, cancelLabel, onConfirm, onCancel);
    } 
    /**
     * ★ NEW: Notify level up - show alert on stats button
     */
    public void notifyLevelUp() {
        UIButton statsButton = getMenuButton("stats");
        if (statsButton != null) {
            statsButton.setAlertNotification();
            System.out.println("Level up notification set on stats button");
        }
    }
    /**
     * ★ NEW: Notify new item in inventory
     */
    public void notifyInventoryUpdate() {
        UIButton inventoryButton = getMenuButton("inventory");
        if (inventoryButton != null) {
            inventoryButton.setAlertNotification();
            System.out.println("Inventory update notification set");
        }
    }
    /**
     * ★ NEW: Notify quest update (count shows number of active quests)
     */
    public void notifyQuestUpdate(int activeQuestCount) {
        UIButton questButton = getMenuButton("quest");
        if (questButton != null) {
            if (activeQuestCount > 0) {
                questButton.setCountNotification(activeQuestCount);
            } else {
                questButton.clearNotification();
            }
        }
    }
    /**
     * ★ NEW: Notify quest completion (use info indicator)
     */
    public void notifyQuestComplete() {
        UIButton questButton = getMenuButton("quest");
        if (questButton != null) {
            questButton.setInfoNotification();
            System.out.println("Quest completion notification set");
        }
    }
    /**
     * ⭐ NEW: Handle quest acceptance
     */
    private void handleQuestAccept() {
        Quest quest = dialogueBox.getOfferedQuest();
        if (quest != null) {
            quest.accept();
            System.out.println("Quest accepted: " + quest.getName());
            
            // Add quest to player's quest log
            Entity player = gameState.getPlayer();
            QuestLog questLog = player.getComponent(QuestLog.class);
            if (questLog != null) {
                questLog.addQuest(quest);
            }
            
            // ⭐ NEW: Unlock quest button on first quest
            //unlockQuestButton();
            unlockQuestButton("quest");
            
            // ⭐ NEW: Update quest indicator
            updateQuestIndicator();
        }
        
        dialogueBox.close();
    }
    /**
     * ⭐ NEW: Handle quest decline
     */
    private void handleQuestDecline() {
        Quest quest = dialogueBox.getOfferedQuest();
        if (quest != null) {
            System.out.println("Quest declined: " + quest.getName());
        }
        
        dialogueBox.close();
    }

    /**
     * ⭐ NEW: Handle dialogue close
     */
    private void handleDialogueClose() {
        Quest quest = dialogueBox.getOfferedQuest();
        
        // If closing a completed quest, claim rewards
        if (quest != null && quest.isCompleted()) {
            Entity player = gameState.getPlayer();
            quest.claimRewards(player);
            
            // Award XP
            Experience exp = player.getComponent(Experience.class);
            if (exp != null && quest.getExpReward() > 0) {
                int levelsGained = exp.addExperience(quest.getExpReward());
                
                if (levelsGained > 0) {
                    Stats stats = player.getComponent(Stats.class);
                    if (stats != null) {
                        stats.applyLevelStats(exp, true);
                    }
                }
            }
            
            // ⭐ NEW: Remove from quest log and update indicator
            QuestLog questLog = player.getComponent(QuestLog.class);
            if (questLog != null) {
                questLog.completeQuest(quest.getId());
            }
            
            updateQuestIndicator();
            
            System.out.println("Quest rewards claimed!");
        }
        
        dialogueBox.close();
    }
    /**
     * ⭐ NEW: Get dialogue box
     */
    public UIDialogueBox getDialogueBox() {
        return dialogueBox;
    }

    
    private void createSkillBar() {
        int slotSize = 48;
        int numSlots = 8;
        int gap = 4;
        int padding = 8;
        
        int barWidth = (slotSize * numSlots) + (gap * (numSlots - 1)) + (padding * 2);
        int barHeight = slotSize + (padding * 2);
        
        int barX = (Engine.WIDTH - barWidth) / 2;
        int barY = Engine.HEIGHT - barHeight - 20;
        
        skillBar = new UIPanel(barX, barY, barWidth, barHeight);
        skillBar.setLayout(UIPanel.LayoutType.HORIZONTAL);
        skillBar.setGap(gap);
        skillBar.setPadding(padding);
        skillBar.setBackgroundColor(new java.awt.Color(30, 30, 30, 220));
        skillBar.setBorderColor(new java.awt.Color(100, 100, 100, 255));
        skillBar.setBorderWidth(2);
        
        String[] keys = {"1", "2", "3", "4", "Q", "E", "R", "F"};
        for (int i = 0; i < numSlots; i++) {
            UISkillSlot slot = new UISkillSlot(0, 0, slotSize, keys[i]);
            slot.setMargin(0);
            slot.setUIManager(this, i);
            skillBar.addChild(slot);
        }
        
        addExampleSkills();
        panels.add(skillBar);
    }
    
    private void createVerticalMenu() {
        int buttonSize = 48;
        int gap = 4;
        int padding = 0;
        
        int numButtons = 10;
        int menuHeight = (buttonSize * numButtons) + (gap * (numButtons - 1)) + (padding * 2);
        int menuWidth = buttonSize + (padding * 2);
        
        int skillBarHeight = 64;
        int skillBarMargin = 20;
        int menuMarginFromSkillBar = 10;
        
        int menuX = Engine.WIDTH - menuWidth - 10;
        int menuY = Engine.HEIGHT - skillBarHeight - skillBarMargin - menuHeight - menuMarginFromSkillBar;
        
        verticalMenu = new UIPanel(menuX, menuY, menuWidth, menuHeight);
        verticalMenu.setLayout(UIPanel.LayoutType.VERTICAL);
        verticalMenu.setGap(gap);
        verticalMenu.setPadding(padding);
        verticalMenu.setBackgroundColor(null);
        verticalMenu.setBorderColor(null);
        verticalMenu.setBorderWidth(0);
        
        String[] buttonLabels = {
            "Settings", "World", "Trade", "Message", "Quest",
            "Stats", "Character Info", "Skill Tree", "Crafting", "Inventory"
        };
        
        String[] buttonIds = {
            "settings", "world", "trade", "message", "quest",
            "stats", "character", "skilltree", "crafting", "inventory"
        };
        
        for (int i = 0; i < buttonLabels.length; i++) {
            UIButton button = new UIButton(0, 0, buttonSize, buttonSize, buttonIds[i], buttonLabels[i]);
            
            String iconPath = "/ui/icons/" + buttonIds[i] + ".png";
            String iconHoverPath = "/ui/icons/" + buttonIds[i] + "_hover.png";
            String iconLockedPath = "/ui/icons/" + buttonIds[i] + "_locked.png";
            
            button.setIcons(iconPath, iconHoverPath, iconLockedPath);
            
            // ⭐ Special button handlers
            if (buttonIds[i].equals("inventory")) {
                button.setLocked(true);  // Initially locked - unlocked by intro quest
                button.setVisible(true);
                button.setOnClick(() -> toggleInventory());
            } else if (buttonIds[i].equals("crafting")) {
                button.setLocked(true);
                button.setVisible(true);
                button.setOnClick(() -> toggleInventory());
            } 
            // ⭐ NEW: Quest button handler
            else if (buttonIds[i].equals("quest")) {
                button.setLocked(true);  // Initially locked
                button.setVisible(true);
                button.setOnClick(() -> toggleQuestPanel());
            } 
            // Stats button handler
            else if (buttonIds[i].equals("stats")) {
                button.setLocked(true);  // Initially locked
                button.setVisible(true);
                button.setOnClick(() -> toggleStatsPanel());
            } else if(buttonIds[i].equals("settings")) {
            	button.setLocked(false);  // Initially locked
                button.setVisible(true);
                //TODO: createa settings panel ui
               button.setOnClick(() -> toggleInventory());
            }
            else {
                button.setLocked(true);
                button.setVisible(true);
            }
            
            verticalMenu.addChild(button);
        }
        
        panels.add(verticalMenu);
        
        setupMenuButtonTooltips();
    }
    /**
     * ★ MODIFIED: Toggle quest panel (clears notification when opened)
     */
    private void toggleQuestPanel() {
        if (questPanel == null) {
            createQuestPanel();
        }
        
        boolean newVisibility = !questPanel.isVisible();
        questPanel.setVisible(newVisibility);
        
        if (newVisibility) {
            questPanel.refreshQuestList();
            
            // Update quest count indicator
            Entity player = gameState.getPlayer();
            QuestLog questLog = player.getComponent(QuestLog.class);
            if (questLog != null) {
                int activeCount = questLog.getActiveQuestCount();
                notifyQuestUpdate(activeCount);
            }
        }
        
        // ★ Notification is automatically cleared by onClick() in UIButton
        
        System.out.println("Quest Panel " + (newVisibility ? "opened" : "closed"));
    }
    
    /**
     * ★ MODIFIED: Toggle stats panel (clears notification when opened)
     */
    public void toggleStatsPanel() {
        isStatsVisible = !isStatsVisible;
        statsPanel.setVisible(isStatsVisible);
        
        // ★ Notification is automatically cleared by onClick() in UIButton
        // No manual clearing needed!
        
        System.out.println("Stats Panel " + (isStatsVisible ? "opened" : "closed"));
    }
    /**
     * Create quest panel
     */
    private void createQuestPanel() {
        int width = 700;
        int height = 500;
        
        // Position next to settings button
        UIButton settingsButton = getMenuButton("settings");
        int panelX;
        int panelY;
        
        if (settingsButton != null) {
            panelX = settingsButton.getX() - width - 10;
            panelY = settingsButton.getY();
        } else {
            panelX = Engine.WIDTH - width - 70;
            panelY = 100;
        }
        
        questPanel = new UIQuestPanel(panelX, panelY, width, height, gameState);
        questPanel.setVisible(false);
        
        // Don't add to panels list - handle separately like dialogueBox
        
        System.out.println("Quest panel created");
    }

    /**
     * ★ MODIFIED: Unlock quest button when first quest is accepted
     
    public void unlockQuestButton() {
        UIButton questButton = getMenuButton("quest");
        if (questButton != null && questButton.isLocked()) {
            questButton.unlock();  // Shows "NEW!" badge
            if (verticalMenu != null) {
                verticalMenu.relayout();
            }
            
            // Also show count notification for 1 active quest
            notifyQuestUpdate(1);
            
            System.out.println("Quest button unlocked with NEW badge!");
        }
    }
*/
    /**
     * ★ MODIFIED: Update quest indicator
     */
    public void updateQuestIndicator() {
        Entity player = gameState.getPlayer();
        QuestLog questLog = player.getComponent(QuestLog.class);
        
        if (questLog != null) {
            int activeCount = questLog.getActiveQuestCount();
            
            // Show count badge with number of active quests
            notifyQuestUpdate(activeCount);
            
            // Check if any quest is completed
            boolean hasCompleted = !questLog.getCompletedQuests().isEmpty();
            if (hasCompleted) {
                notifyQuestComplete();
            }
        }
        
        // Refresh quest panel if open
        if (questPanel != null && questPanel.isVisible()) {
            questPanel.refreshQuestList();
        }
    }
    
    /**
     * ★ MODIFIED: Toggle inventory (clears notification when opened)
     */
    private void toggleInventory() {
        if (inventoryContainer == null) {
            createInventorySystem();
        }
        
        boolean newVisibility = !inventoryContainer.isVisible();
        inventoryContainer.setVisible(newVisibility);
        
        // ★ Notification is automatically cleared by onClick() in UIButton
        // No manual clearing needed!
        
        System.out.println("Inventory " + (newVisibility ? "opened" : "closed"));
    }
    
    /**
     * Create inventory system with tabs ABOVE the inventory grid
     */
    private void createInventorySystem() {
        int slotSize = 48;
        int gap = 4;
        int padding = 8;
        int tabHeight = 28;
        
        int columnWidth = slotSize;
        
        // Inventory grid: 5 columns, 10 total rows, 4 visible rows
        int inventoryColumns = 5;
        int inventoryTotalRows = 10;
        int inventoryVisibleRows = 4;
        
        // Total width = 5 columns (inventory width)
        int totalWidth = (columnWidth * inventoryColumns) + (gap * (inventoryColumns - 1)) + (padding * 2) + 12;
        
        // Top section: gear slots + hero preview
        int gearColumnHeight = (slotSize * 6) + (gap * 4);
        int heroPreviewHeight = gearColumnHeight + 6;
        
        // Middle section: tabs
        int tabSectionHeight = tabHeight + gap;
        
        // Bottom section: inventory
        int inventoryGridHeight = (slotSize * inventoryVisibleRows) + (gap * (inventoryVisibleRows - 1)) + (padding * 2);
        
        // Total height = top section + tabs + inventory + padding
        int totalHeight = gearColumnHeight + tabSectionHeight + inventoryGridHeight + (padding * 3);
        
        // Position
        UIButton settingsButton = getMenuButton("settings");
        int containerX;
        int containerY;
        
        if (settingsButton != null) {
            containerX = settingsButton.getX() - totalWidth - 10;
            containerY = settingsButton.getY();
        } else {
            containerX = Engine.WIDTH - totalWidth - 70;
            containerY = 100;
        }
        
        // Main container
        inventoryContainer = new UIPanel(containerX, containerY, totalWidth, totalHeight);
        inventoryContainer.setLayout(UIPanel.LayoutType.NONE);
        inventoryContainer.setPadding(0);
        inventoryContainer.setBackgroundColor(new java.awt.Color(20, 20, 30, 230));
        inventoryContainer.setBorderColor(new java.awt.Color(100, 100, 120));
        inventoryContainer.setBorderWidth(2);
        
        int currentY = containerY + padding;
        
        // TOP SECTION - Calculate hero preview width and gear columns
        // Left and right gear columns use `columnWidth`; hero preview takes remaining center space
        int heroWidth = totalWidth - (padding * 2) - (columnWidth * 2) - (gap * 2);
        
        // ⭐ START BATCH - Add all children at once
        inventoryContainer.beginBatch();
        
        // LEFT GEAR COLUMN
        int leftGearX = containerX + padding;
        int leftGearY = currentY;
        
        addGearSlotToContainer(leftGearX, leftGearY + (slotSize + gap) * 0, columnWidth, slotSize, UIGearSlot.SlotType.HEAD);
        addGearSlotToContainer(leftGearX, leftGearY + (slotSize + gap) * 1, columnWidth, slotSize, UIGearSlot.SlotType.TOP_ARMOR);
        addGearSlotToContainer(leftGearX, leftGearY + (slotSize + gap) * 2, columnWidth, slotSize, UIGearSlot.SlotType.PANTS);
        addGearSlotToContainer(leftGearX, leftGearY + (slotSize + gap) * 3, columnWidth, slotSize, UIGearSlot.SlotType.GLOVES);
        addGearSlotToContainer(leftGearX, leftGearY + (slotSize + gap) * 4, columnWidth, slotSize, UIGearSlot.SlotType.SHOES);
        addGearSlotToContainer(leftGearX, leftGearY + (slotSize + gap) * 5, columnWidth, slotSize, UIGearSlot.SlotType.WEAPON);
        
        // CENTER HERO PREVIEW
        int heroX = leftGearX + columnWidth + gap;
        int heroY = currentY;
         
        heroPreviewPanel = new UIPanel(heroX, heroY, heroWidth, heroPreviewHeight);
        heroPreviewPanel.setBackgroundColor(new java.awt.Color(30, 30, 40, 180));
        heroPreviewPanel.setBorderColor(new java.awt.Color(80, 80, 100));
        heroPreviewPanel.setBorderWidth(1);
        inventoryContainer.addChild(heroPreviewPanel);
        
        // RIGHT GEAR COLUMN (flush to right edge)
        int rightGearX = containerX + totalWidth - padding - columnWidth;
        int rightGearY = currentY;
        
        addGearSlotToContainer(rightGearX, rightGearY + (slotSize + gap) * 0, columnWidth, slotSize, UIGearSlot.SlotType.EARRINGS);
        addGearSlotToContainer(rightGearX, rightGearY + (slotSize + gap) * 1, columnWidth, slotSize, UIGearSlot.SlotType.NECKLACE);
        addGearSlotToContainer(rightGearX, rightGearY + (slotSize + gap) * 2, columnWidth, slotSize, UIGearSlot.SlotType.BRACELET);
        addGearSlotToContainer(rightGearX, rightGearY + (slotSize + gap) * 3, columnWidth, slotSize, UIGearSlot.SlotType.RING_1);
        addGearSlotToContainer(rightGearX, rightGearY + (slotSize + gap) * 4, columnWidth, slotSize, UIGearSlot.SlotType.RING_2);
        addGearSlotToContainer(rightGearX, rightGearY + (slotSize + gap) * 5, columnWidth, slotSize, UIGearSlot.SlotType.SPECIAL);
       
        
        // Move to next section (tabs)
        currentY += gearColumnHeight + padding;
        
        // MIDDLE SECTION - TABS (above inventory)
        createInventoryTabs(containerX, currentY, totalWidth, padding, gap);
        
        // Move to next section (inventory)
        currentY += tabSectionHeight;
        
        // BOTTOM SECTION - SCROLLABLE INVENTORY GRID
        int inventoryGridX = containerX + padding;
        
        inventoryGrid = new UIScrollableInventoryPanel(
            inventoryGridX, currentY,
            totalWidth - (padding * 2), inventoryGridHeight,
            inventoryColumns, inventoryTotalRows, inventoryVisibleRows,
            this
        );
        inventoryContainer.addChild(inventoryGrid);
        // Ensure the inventory grid shows the currently selected tab
        inventoryGrid.switchToTab(currentInventoryTab);
        
        // ⭐ END BATCH - Single relayout for all components
        inventoryContainer.endBatch();

        panels.add(inventoryContainer);
        
        System.out.println("Inventory system created:");
        System.out.println("  Total size: " + totalWidth + "x" + totalHeight);
        System.out.println("  Layout: [Gear] -> [Tabs] -> [Inventory]");
        System.out.println("  Inventory: 5x10 grid (50 total slots, 4 visible rows)");
    }
    
    private void createInventoryTabs(int containerX, int containerY, int containerWidth, int padding, int gap) {
        //String[] tabNames = {"Misc", "Weap", "Arm", "Acc", "Rune"};
        int tabWidth = (containerWidth - (padding * 2) - (gap * 4)) / 5;
        int tabHeight = 24;
        
        inventoryTabs.clear();
        
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int tabX = containerX + padding + (i * (tabWidth + gap));
            int tabY = containerY;
            
            UIInventoryTab tab = new UIInventoryTab(tabX, tabY, tabWidth, tabHeight, TAB_NAMES[i]);
            final String tabName = TAB_NAMES[i];
            tab.setOnClick(() -> switchInventoryTab(tabName));
            
            if (TAB_NAMES[i].equals(currentInventoryTab)) {
                tab.setActive(true);
            }
            
            inventoryTabs.add(tab);
            inventoryContainer.addChild(tab);
        }
    }
    
    private void addGearSlotToContainer(int x, int y, int width, int height, UIGearSlot.SlotType slotType) {
        UIGearSlot slot = new UIGearSlot(x, y, width, height, slotType, this);
        inventoryContainer.addChild(slot);
    }
    
    private void switchInventoryTab(String tabName) {
        currentInventoryTab = tabName;
        
        for (UIInventoryTab tab : inventoryTabs) {
            tab.setActive(tab.getTabName().equals(tabName));
        }
        
        System.out.println("Switched to tab: " + tabName);
        // Update inventory grid to show the selected tab
        if (inventoryGrid != null) {
            inventoryGrid.switchToTab(tabName);
        }
    }
    
    private void addExampleSkills() {
        Skill fireball = new Skill("fireball", "Fireball", "Launch a blazing fireball", 
            Skill.SkillType.ATTACK, 3.0f, 12, 1);
        Skill heal = new Skill("heal", "Heal", "Restore health", 
            Skill.SkillType.HEAL, 8.0f, 12, 1); heal.setIconPath("/items/icons/heal.png");
        
        Skill shield = new Skill("shield", "Shield", "Create a barrier", 
            Skill.SkillType.DEFENSE, 12.0f, 12, 3);
        Skill haste = new Skill("haste", "Haste", "Increase speed", 
            Skill.SkillType.BUFF, 20.0f, 12, 2);
        
        List<UIComponent> slots = skillBar.getChildren();
        if (slots.size() >= 4) {
            ((UISkillSlot)slots.get(0)).setSkill(fireball);
            ((UISkillSlot)slots.get(1)).setSkill(heal);
            ((UISkillSlot)slots.get(2)).setSkill(shield);
            ((UISkillSlot)slots.get(4)).setSkill(haste);
        }
    }
    
    public void update(float delta) {
        for (UIPanel panel : panels) {
            if (panel.isVisible()) {
                panel.update(delta);
            }
        }
        
        // Update tabs
        for (UIInventoryTab tab : inventoryTabs) {
            if (inventoryContainer != null && inventoryContainer.isVisible()) {
                tab.update(delta);
            }
        }
        
        // Update scrollable inventory
        if (inventoryGrid != null && inventoryContainer != null && inventoryContainer.isVisible()) {
            inventoryGrid.update(delta);
        }
        
        // ⭐ NEW: Update dialogue box
        if (dialogueBox != null && dialogueBox.isVisible()) {
            dialogueBox.update(delta);
        }
        // ⭐ NEW: Update quest panel
        if (questPanel != null && questPanel.isVisible()) {
            questPanel.update(delta);
        }
    }
    
    public void render(Graphics2D g) {
        for (UIPanel panel : panels) {
            if (panel.isVisible()) {
                panel.render(g);
            }
        }
        
        // NEW: Render buff bar
        if (buffBar != null) {
            buffBar.render(g);
        }
        
        // ⭐ NEW: Render quest panel (after regular panels, before dialogue)
        if (questPanel != null && questPanel.isVisible()) {
            questPanel.render(g);
        }
        
        // Render stats panel
        if (statsPanel != null && statsPanel.isVisible()) {
            statsPanel.render(g);
        }
        
        // ⭐ Render dialogue box on top of everything
        if (dialogueBox != null && dialogueBox.isVisible()) {
            dialogueBox.render(g);
        }
        // NEW: Render enhanced dialogue box
        if (enhancedDialogueBox != null && enhancedDialogueBox.isVisible()) {
              enhancedDialogueBox.render(g);
        }
        
        // Render tooltip on top of everything
        if (tooltipPanel != null && tooltipPanel.isVisible()) {
            tooltipPanel.render(g);
        }
         
    }
     
    // Updated to accept pressed state for drag handling
    public void handleMouseMove(int mouseX, int mouseY, boolean pressed) {
        for (UIPanel panel : panels) {
            if (panel.isVisible()) {
                panel.handleMouseMove(mouseX, mouseY, pressed);
            }
        }
        
        // NEW: Update buff bar hover
        if (buffBar != null) {
            buffBar.handleMouseMove(mouseX, mouseY);
        }
        
        // Handle inventory grid hover/drag
        if (inventoryGrid != null && inventoryContainer != null && inventoryContainer.isVisible()) {
            inventoryGrid.handleMouseMove(mouseX, mouseY, pressed);
        }
        
        // Handle enhanced dialogue box hover first (on-top)
        if (enhancedDialogueBox != null && enhancedDialogueBox.isVisible()) {
            enhancedDialogueBox.handleMouseMove(mouseX, mouseY);
            return; // enhanced box is on top; consumed for hover
        }

        // Handle legacy dialogue box hover
        if (dialogueBox != null && dialogueBox.isVisible()) {
            dialogueBox.handleMouseMove(mouseX, mouseY);
        }
        
        // ⭐ NEW: Handle quest panel hover
        if (questPanel != null && questPanel.isVisible()) {
            questPanel.handleMouseMove(mouseX, mouseY);
        }
        
        // Handle tooltips
        updateTooltips(mouseX, mouseY);
    }
 // ═══════════════════════════════════════════════════════════════════
 // UPDATE THE updateTooltips() METHOD IN UIManager.java
 // This makes tooltips work for menu buttons
 // ═══════════════════════════════════════════════════════════════════

 private void updateTooltips(int mouseX, int mouseY) {
     String tooltipText = null;
     
     // NEW: Check buff bar hover first
     if (buffBar != null) {
         tooltipText = buffBar.getTooltipText(mouseX, mouseY);
     }
     
     // ★ NEW: Check menu buttons (vertical menu)
     if (tooltipText == null && verticalMenu != null && verticalMenu.isVisible()) {
         for (UIComponent child : verticalMenu.getChildren()) {
             if (child instanceof UIButton && child.contains(mouseX, mouseY)) {
                 tooltipText = child.getTooltipText();
                 break;
             }
         }
     }
     
     // Check inventory slots
     if (tooltipText == null && inventoryGrid != null && inventoryContainer != null && inventoryContainer.isVisible()) {
         if (inventoryContainer.contains(mouseX, mouseY)) {
             UIInventorySlot slot = inventoryGrid.getHoveredSlot(mouseX, mouseY);
             if (slot != null) {
                 tooltipText = slot.getTooltipText();
             }
         }
     }
     
     // Check gear slots
     if (tooltipText == null) {
         for (UIPanel panel : panels) {
             if (panel.isVisible() && panel.contains(mouseX, mouseY)) {
                 // Check if it's a gear slot container
                 for (UIComponent child : panel.getChildren()) {
                     if (child instanceof UIGearSlot && child.contains(mouseX, mouseY)) {
                         tooltipText = child.getTooltipText();
                         break;
                     }
                 }
                 if (tooltipText != null) break;
             }
         }
     }
     
     // Show or hide tooltip
     if (tooltipText != null) {
         showTooltip(tooltipText, mouseX, mouseY);
     } else {
         hideTooltip();
     }
 }
    
    public boolean handleClick(int mouseX, int mouseY) {
    	 // NEW: Check enhanced dialogue box first
    	 if (enhancedDialogueBox != null && enhancedDialogueBox.isVisible()) {
    	     if (enhancedDialogueBox.handleClick(mouseX, mouseY)) {
    	             return true;
    	     }
    	 }
        // Check dialogue box first (highest priority)
        if (dialogueBox != null && dialogueBox.isVisible()) {
            if (dialogueBox.handleClick(mouseX, mouseY)) {
                return true;
            }
        }
        
        // ⭐ NEW: Check quest panel
        if (questPanel != null && questPanel.isVisible()) {
            if (questPanel.handleClick(mouseX, mouseY)) {
                return true;
            }
        }
        
        for (int i = panels.size() - 1; i >= 0; i--) {
            UIPanel panel = panels.get(i);
            if (!panel.isVisible()) continue;
            
            if (panel.handleClick(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean handleRightClick(int mouseX, int mouseY) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            UIPanel panel = panels.get(i);
            if (!panel.isVisible()) continue;
            
            if (panel.handleRightClick(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (inventoryGrid != null && inventoryContainer != null && inventoryContainer.isVisible()) {
            int mouseX = e.getX();
            int mouseY = e.getY();
            
            if (inventoryGrid.contains(mouseX, mouseY)) {
                inventoryGrid.handleScroll(e.getWheelRotation());
            }
        }
        
        // ⭐ NEW: Quest panel scroll
        if (questPanel != null && questPanel.isVisible()) {
            int mouseX = e.getX();
            int mouseY = e.getY();
            
            if (questPanel.contains(mouseX, mouseY)) {
                questPanel.handleScroll(e.getWheelRotation());
            }
        }
    }
    
    public void handleKeyPress(int keyCode) {
        if (skillBar != null) {
            List<UIComponent> slots = skillBar.getChildren();
            
            if (keyCode >= java.awt.event.KeyEvent.VK_1 && 
                keyCode <= java.awt.event.KeyEvent.VK_8) {
                int index = keyCode - java.awt.event.KeyEvent.VK_1;
                if (index < slots.size()) {
                    useSkillInSlot(index);
                }
            }
            
            switch (keyCode) {
                case java.awt.event.KeyEvent.VK_Q:
                    if (slots.size() > 4) useSkillInSlot(4);
                    break;
                case java.awt.event.KeyEvent.VK_E:
                    if (slots.size() > 5) useSkillInSlot(5);
                    break;
                case java.awt.event.KeyEvent.VK_R:
                    if (slots.size() > 6) useSkillInSlot(6);
                    break;
                case java.awt.event.KeyEvent.VK_F:
                    if (slots.size() > 7) useSkillInSlot(7);
                    break;
            }
        }
        
        // Inventory toggle
        if (keyCode == java.awt.event.KeyEvent.VK_I) {
            toggleInventory();
        }
        
        // ⭐ NEW: Quest panel toggle (J key)
        if (keyCode == java.awt.event.KeyEvent.VK_J) {
            // Check if quest button is unlocked
            UIButton questButton = getMenuButton("quest");
            if (questButton != null && !questButton.isLocked()) {
                toggleQuestPanel();
            }
        }
        
        // Stats panel toggle (S key)
        if (keyCode == java.awt.event.KeyEvent.VK_S) {
            UIButton statsButton = getMenuButton("stats");
            if (statsButton != null && !statsButton.isLocked()) {
                toggleStatsPanel();
            }
        }
        
        // Close UI panels one at a time (ESC key)
        if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
            // Close dialogue boxes first (highest priority)
            if (enhancedDialogueBox != null && enhancedDialogueBox.isVisible()) {
                enhancedDialogueBox.setVisible(false);
            } else if (dialogueBox != null && dialogueBox.isVisible()) {
                dialogueBox.setVisible(false);
            }
            // Then stats panel
            else if (isStatsVisible) {
                toggleStatsPanel();
            }
            // Then quest panel
            else if (questPanel != null && questPanel.isVisible()) {
                toggleQuestPanel();
            }
            // Then inventory
            else if (inventoryContainer != null && inventoryContainer.isVisible()) {
                toggleInventory();
            }
            // Finally hide tooltip
            else {
                hideTooltip();
            }
        }
    }
    
    public void useSkillInSlot(int slotIndex) {
        UISkillSlot slot = getSkillSlot(slotIndex);
        if (slot != null && slot.getSkill() != null) {
            Skill skill = slot.getSkill();
            
            if (skill.isReady()) {
                if (gameLogic != null) {
                    Entity player = gameState.getPlayer();
                    gameLogic.useSkill(player, skill);
                } else {
                    skill.use();
                    System.out.println("Used skill: " + skill.getName());
                }
            }
        }
    }
    
    public boolean upgradeSkill(int slotIndex) {
        Entity player = gameState.getPlayer();
        SkillLevel skillLevel = player.getComponent(SkillLevel.class);
        UISkillSlot slot = getSkillSlot(slotIndex);
        
        if (slot == null || skillLevel == null) return false;
        
        Skill skill = slot.getSkill();
        if (skill == null) return false;
        
        if (!skill.canUpgrade()) {
            System.out.println("Skill is already max level!");
            return false;
        }
        
        int cost = skill.getUpgradeCost();
        
        if (!skillLevel.canAfford(cost)) {
            System.out.println("Not enough skill points!");
            return false;
        }
        
        skillLevel.spendPoints(cost);
        skill.upgrade();
        
        System.out.println("Upgraded " + skill.getName() + " to level " + skill.getSkillLevel());
        return true;
    }
    
    public void addPanel(UIPanel panel) {
        panels.add(panel);
    }
    
    public void removePanel(UIPanel panel) {
        panels.remove(panel);
    }
    
    public UIPanel getSkillBar() {
        return skillBar;
    }
    
    public UISkillSlot getSkillSlot(int index) {
        if (skillBar == null) return null;
        
        List<UIComponent> slots = skillBar.getChildren();
        if (index >= 0 && index < slots.size()) {
            return (UISkillSlot)slots.get(index);
        }
        return null;
    }
    
    public void equipSkill(Skill skill, int slotIndex) {
        UISkillSlot slot = getSkillSlot(slotIndex);
        if (slot != null) {
            slot.setSkill(skill);
        }
    }
    
    public void clearSkillSlot(int slotIndex) {
        UISkillSlot slot = getSkillSlot(slotIndex);
        if (slot != null) {
            slot.setSkill(null);
        }
    }
    
    public UIButton getMenuButton(String id) {
        if (verticalMenu == null) return null;
        
        for (UIComponent child : verticalMenu.getChildren()) {
            if (child instanceof UIButton) {
                UIButton button = (UIButton) child;
                if (button.getId().equals(id)) {
                    return button;
                }
            }
        }
        return null;
    }
    /**
     * ★ MODIFIED: Unlock menu button (automatically shows NEW badge)
     */
    public void unlockMenuButton(String id) {
        UIButton button = getMenuButton(id);
        if (button != null && button.isLocked()) {
            button.unlock();  // Now shows "NEW!" badge automatically
            if (verticalMenu != null) {
                verticalMenu.relayout();
            }
            System.out.println("Unlocked: " + button.getLabel() + " (NEW badge shown)");
        }
    }
    
    public void unlockQuestButton(String id) {
    	UIButton button = getMenuButton(id);
    	if (button != null && button.isLocked()) {
            button.unlock();  // Now shows "NEW!" badge automatically
            if (verticalMenu != null) {
                verticalMenu.relayout(); //Does this need to be called?
            }
            System.out.println("Unlocked: " + button.getLabel() + " (NEW badge shown)");
        }
    }
    
    public void lockMenuButton(String id) {
        UIButton button = getMenuButton(id);
        if (button != null) {
            button.lock();
            if (verticalMenu != null) {
                verticalMenu.relayout();
            }
            System.out.println("Locked: " + button.getLabel());
        }
    }
    
    public UIPanel getInventoryPanel() {
        return inventoryContainer;
    }
    
    public UIScrollableInventoryPanel getInventoryGrid() {
        return inventoryGrid;
    }
    
    public UIInventorySlot getInventorySlot(int index) {
        if (inventoryGrid == null) return null;
        return inventoryGrid.getSlot(index);
    }
	 // ═══════════════════════════════════════════════════════════════════
	 // UPDATE UIManager.java - Replace addItemToInventory method
	 // This version marks new items with a "NEW" badge
	 // ═══════════════════════════════════════════════════════════════════
	
	 /**
	  * ★ UPDATED: Add item to inventory (default: mark as new)
	  */
	 public boolean addItemToInventory(Item item) {
	     return addItemToInventory(item, true);
	 }
	 /**
	  * ★ NEW: Add item to inventory with optional "NEW" badge
	  */
	 public boolean addItemToInventory(Item item, boolean markAsNew) {
	     if (inventoryContainer == null) {
	         createInventorySystem();
	     }
	     
	     if (inventoryGrid != null) {
	         boolean added;
	         
	         if (markAsNew) {
	             // Use the new marking system
	             added = inventoryGrid.addItemToCurrentTab(item, true);
	         } else {
	             // Add without marking
	             added = inventoryGrid.addItemToCurrentTab(item, false);
	         }
	         
	         if (added) {
	             System.out.println("Added item to inventory (tab=" + currentInventoryTab + 
	                              ", marked as new=" + markAsNew + ")");
	             
	             // Show notification on inventory button
	             notifyInventoryUpdate();
	             
	             return true;
	         }
	     }

	     System.out.println("Inventory full!");
	     return false;
	 }
    public UIGearSlot getGearSlot(UIGearSlot.SlotType slotType) {
        if (inventoryContainer == null) return null;
        
        for (UIComponent component : inventoryContainer.getChildren()) {
            if (component instanceof UIGearSlot) {
                UIGearSlot slot = (UIGearSlot) component;
                if (slot.getSlotType() == slotType) {
                    return slot;
                }
            }
        }
        return null;
    }
    /**
     * ★ UPDATED: Equip item and notify quest handler
     */
    public boolean equipItem(UIGearSlot.SlotType slotType, Item item) {
        UIGearSlot slot = getGearSlot(slotType);
        if (slot == null) return false;
         
        Item oldItem = slot.equipItem(item);
        if (oldItem != null) {
        	 
            addItemToInventory(oldItem);
            applyItemStats(oldItem, false);
        }
        
        if (item != null) {
            applyItemStats(item, true);
            
            // ★ TRACK WHEN WOODEN SHORT SWORD IS EQUIPPED
            if (slotType == UIGearSlot.SlotType.WEAPON && "Wooden Short Sword".equals(item.getName())) {
                //swordEquipped = true;
                unlockMenuButton("stats");  // Shows NEW badge automatically
                //fionneSecondQuestAvailable = true;
               // fionneNotification = "!";
                
                // ★ NEW: Notify intro quest handler
                IntroQuestHandler introHandler = gameState.getIntroQuestHandler();
                if (introHandler != null) {
                    introHandler.checkSwordEquipStatus();
                }
                
                System.out.println("Stats button unlocked with NEW badge! Second quest available from Fionne.");
            }
        }
        return true;
    }
    
    public boolean isWoodenSwordEquipped() {
        UIGearSlot weaponSlot = getGearSlot(UIGearSlot.SlotType.WEAPON);
        if (weaponSlot == null) return false;
        
        Item equippedWeapon = weaponSlot.getItem();
        return equippedWeapon != null && "Wooden Short Sword".equals(equippedWeapon.getName());
    }
    
    public boolean hasWoodenSwordInInventory() {
        if (inventoryGrid == null) return false;
        
        for (UIInventorySlot slot : inventoryGrid.getSlots()) {
            Item item = slot.getItem();
            if (item != null && "Wooden Short Sword".equals(item.getName())) {
                return true;
            }
        }
        return false;
    }
    
    public void showEquipSwordReminder() {
        dialogueBox.showMessageWithAccept(
            "You need to equip the sword I gave you! Right-click it in your inventory to equip it.",
            () -> {
                dialogueBox.setVisible(false);
            },
            () -> {
                dialogueBox.setVisible(false);
            }
        );
    }
    
    /**
     * ★ NEW: Clear all notifications (useful for debugging or reset)
     */
    public void clearAllNotifications() {
        for (UIComponent child : verticalMenu.getChildren()) {
            if (child instanceof UIButton) {
                ((UIButton) child).clearNotification();
            }
        }
        System.out.println("All button notifications cleared");
    }

	/**
	 * ★ NEW: Get notification summary (for debugging)
	 */
	public void printNotificationStatus() {
	    System.out.println("=== Button Notification Status ===");
	    for (UIComponent child : verticalMenu.getChildren()) {
	        if (child instanceof UIButton) {
	            UIButton btn = (UIButton) child;
	            String status = btn.hasNotification() ? 
	                btn.getNotificationType().toString() : "NONE";
	            System.out.println(btn.getLabel() + ": " + status);
	        }
	    }
	    System.out.println("==================================");
	}
    public Item unequipItem(UIGearSlot.SlotType slotType) {
        UIGearSlot slot = getGearSlot(slotType);
        if (slot == null) return null;
        Item item = slot.unequipItem();
        if (item != null) {
            addItemToInventory(item);
            applyItemStats(item, false);
        }
        return item;
    }
    
    private void applyItemStats(Item item, boolean add) {
        if (item == null) return;
        Entity player = gameState.getPlayer();
        Stats stats = player.getComponent(Stats.class);
        if (stats == null) return;
        int multiplier = add ? 1 : -1;
        stats.attack += multiplier * item.getAttackBonus();
        stats.defense += multiplier * item.getDefenseBonus();
        stats.magicAttack += multiplier * item.getMagicAttackBonus();
        stats.magicDefense += multiplier * item.getMagicDefenseBonus();
    }
    
    public UIPanel getHeroPreviewPanel() {
        return heroPreviewPanel;
    }
    
    // Tooltip methods
    public void showTooltip(String text, int mouseX, int mouseY) {
        if (tooltipPanel != null) {
            panels.remove(tooltipPanel);
        }
        
        tooltipPanel = UIExamples.createTooltip(text, mouseX, mouseY);
        panels.add(tooltipPanel);
    }
    
    public void hideTooltip() {
        if (tooltipPanel != null) {
            panels.remove(tooltipPanel);
            tooltipPanel = null;
        }
    }
    /**
     * ★ NEW: Setup tooltips for menu buttons
     * Call this at the end of createVerticalMenu()
     */
    private void setupMenuButtonTooltips() {
        // Settings
        UIButton settingsButton = getMenuButton("settings");
        if (settingsButton != null) {
            settingsButton.setTooltipText(
                "Settings\n\n" +
                "Configure game options and preferences."
            );
        }
        
        // World
        UIButton worldButton = getMenuButton("world");
        if (worldButton != null) {
            worldButton.setTooltipText(
                "World Map\n\n" +
                "View the world map and travel to different locations.\n\n" +
                "(Currently locked)"
            );
        }
        
        // Trade
        UIButton tradeButton = getMenuButton("trade");
        if (tradeButton != null) {
            tradeButton.setTooltipText(
                "Trade\n\n" +
                "Trade items with NPCs and other players.\n\n" +
                "(Currently locked)"
            );
        }
        
        // Message
        UIButton messageButton = getMenuButton("message");
        if (messageButton != null) {
            messageButton.setTooltipText(
                "Messages\n\n" +
                "View messages, mail, and communications.\n\n" +
                "(Currently locked)"
            );
        }
        
        // Quest
        UIButton questButton = getMenuButton("quest");
        if (questButton != null) {
            questButton.setTooltipText(
                "Quest Log\n\n" +
                "View active quests and completed objectives.\n\n" +
                "Hotkey: J"
            );
        }
        
        // Stats
        UIButton statsButton = getMenuButton("stats");
        if (statsButton != null) {
            statsButton.setTooltipText(
                "Character Stats\n\n" +
                "View your character's attributes and statistics.\n\n" +
                "Hotkey: S"
            );
        }
        
        // Character Info
        UIButton characterButton = getMenuButton("character");
        if (characterButton != null) {
            characterButton.setTooltipText(
                "Character Info\n\n" +
                "View detailed character information and biography.\n\n" +
                "(Currently locked)"
            );
        }
        
        // Skill Tree
        UIButton skilltreeButton = getMenuButton("skilltree");
        if (skilltreeButton != null) {
            skilltreeButton.setTooltipText(
                "Skill Tree\n\n" +
                "Upgrade and unlock new skills and abilities.\n\n" +
                "(Currently locked)"
            );
        }
        
        // Rune
        UIButton runeButton = getMenuButton("rune");
        if (runeButton != null) {
            runeButton.setTooltipText(
                "Rune Crafting\n\n" +
                "Craft magical runes and inscriptions.\n\n" +
                "(Currently locked)"
            );
        }
        
        // Inventory
        UIButton inventoryButton = getMenuButton("inventory");
        if (inventoryButton != null) {
            inventoryButton.setTooltipText(
                "Inventory\n\n" +
                "View and manage your items and equipment.\n\n" +
                "Hotkey: I"
            );
        }
    }

 // ═══════════════════════════════════════════════════════════════════
 // ADD THIS METHOD TO YOUR UIManager OR WHEREVER YOU TEST ITEMS
 // Call it once to populate your inventory with test gear
 // ═══════════════════════════════════════════════════════════════════

 public void addTestGearItems() {
     System.out.println("\n🎮 ADDING TEST GEAR ITEMS...\n");
     
     // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     // WEAPONS (Should go in Weapon slot)
     // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     addItemToInventory(ItemManager.createWoodenShortSword());
     //System.out.println("✅ Added: Wooden Short Sword (COMMON)");
     
     addItemToInventory(ItemManager.createIronSword());
     //System.out.println("✅ Added: Iron Sword (COMMON)");
     
     addItemToInventory(ItemManager.createSteelLongsword());
     //System.out.println("✅ Added: Steel Longsword (UNCOMMON)");
     
     addItemToInventory(ItemManager.createMysticStaff());
     //System.out.println("✅ Added: Mystic Staff (RARE)");
     
     // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     // ARMOR (Should go in Armor slots)
     // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     addItemToInventory(ItemManager.createLeatherArmor());
     //System.out.println("✅ Added: Leather Armor (COMMON)");
     
     addItemToInventory(ItemManager.createChainmail());
     //System.out.println("✅ Added: Chainmail (UNCOMMON)");
     
     addItemToInventory(ItemManager.createPlateArmor());
     //System.out.println("✅ Added: Plate Armor (RARE)");
     
     // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     // ACCESSORIES (Should go in Accessory slots)
     // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     addItemToInventory(ItemManager.createPowerRing());
     //System.out.println("✅ Added: Ring of Power (UNCOMMON)");
     
     addItemToInventory(ItemManager.createAmuletOfProtection());
     //System.out.println("✅ Added: Amulet of Protection (RARE)");
     
     addItemToInventory(ItemManager.createSpeedBoots());
     //System.out.println("✅ Added: Boots of Speed (UNCOMMON)");
     
     // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     // CONSUMABLES (Stackable items)
     // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     //addItemToInventory(ItemManager.createHealthPotion());
     //addItemToInventory(ItemManager.createHealthPotion());
     //addItemToInventory(ItemManager.createHealthPotion());
     //System.out.println("✅ Added: 3x Health Potion (Should stack)");
     
     //addItemToInventory(ItemManager.createManaPotion());
     //addItemToInventory(ItemManager.createManaPotion());
    // System.out.println("✅ Added: 2x Mana Potion (Should stack)");
     
     // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     // MATERIALS (Stackable items)
     // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     for (int i = 0; i < 5; i++) {
         addItemToInventory(ItemManager.createCarvedWood());
     }
     //System.out.println("✅ Added: 5x Carved Wood (Should stack)");
     
     for (int i = 0; i < 3; i++) {
         addItemToInventory(ItemManager.createFireRune());
     }
     //System.out.println("✅ Added: 3x Fire Rune (Should stack)");
     /*
     System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
     System.out.println("✅ TEST ITEMS ADDED SUCCESSFULLY!");
     System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
     System.out.println("\nTEST INSTRUCTIONS:");
     System.out.println("1. Press [I] to open inventory");
     System.out.println("2. RIGHT-CLICK weapons to equip (Weapon slot)");
     System.out.println("3. RIGHT-CLICK armor to equip (Armor slots)");
     System.out.println("4. RIGHT-CLICK accessories to equip (Ring/Neck slots)");
     System.out.println("5. Check different tabs to see items");
     System.out.println("6. Hover items to see tooltips");
     System.out.println("\nExpected Results:");
     System.out.println("- Icons should appear in inventory slots");
     System.out.println("- Rarity borders: Gray/Green/Blue/Purple/Gold");
     System.out.println("- Stackable items show count (e.g., '5' for wood)");
     System.out.println("- Equipped items show in gear slots with icons");
     System.out.println("\n");
     */
 }

 // ═══════════════════════════════════════════════════════════════════
 // CALL THIS AFTER YOUR UI IS INITIALIZED
 // Example: In your Engine or Game initialization
 // ═══════════════════════════════════════════════════════════════════

 // After creating UIManager:
 // uiManager.addTestGearItems();

 // OR add a keybind to test anytime:
 // if (keyCode == KeyEvent.VK_T) {  // Press T to add test items
//      uiManager.addTestGearItems();
 // }
    
}