package dev.main.quest;

import dev.main.Engine;
import dev.main.entity.Entity;
import dev.main.entity.NPC;
import dev.main.buffs.Buff;
import dev.main.buffs.BuffFactory;
import dev.main.buffs.BuffManager;
import dev.main.item.Item;
import dev.main.item.ItemManager;
import dev.main.state.GameState;
import dev.main.ui.Quest;
import dev.main.ui.UIDialogueBox;
import dev.main.ui.UIManager;
import dev.main.quest.QuestIndicator.IndicatorType;

/**
 * Handles the sequential introduction quests for new players.
 * Manages progression gates and ensures quests complete in order.
 */
public class IntroQuestHandler {
    
    // Quest stage tracking
    public enum IntroStage {
        NOT_STARTED,           // Player hasn't talked to Fionne yet
        STAGE_1_DIALOGUE,      // Fionne's first dialogue shown
        STAGE_1_COMPLETE,      // Inventory unlocked, sword received
        STAGE_2_EQUIP_SWORD,   // Waiting for player to equip sword
        STAGE_2_COMPLETE,      // Sword equipped, stats unlocked
        STAGE_3_DIALOGUE,      // Second quest dialogue shown
        STAGE_3_COMPLETE,      // Rune received, blessing granted
        STAGE_4_DIALOGUE,	   // unlock quest menu	
        STAGE_4_COMPLETE,
        ALL_COMPLETE           // All intro quests finished
    }
    
    private boolean initializedIndicator = false;
    
    private IntroStage currentStage;
    private GameState gameState;
    private Entity player;
    private Entity fionneEntity;  // ★ NEW: Track Fionne entity for indicator updates
    
    public IntroQuestHandler(GameState gameState) {
        this.gameState = gameState;
        this.player = gameState.getPlayer();
        this.currentStage = IntroStage.NOT_STARTED;
        this.fionneEntity = null;
        
        // ★ NEW: Initialize Fionne's indicator on startup
        initializeFionneIndicator();
    }
    
    /**
     * ★ NEW: Call this from GameLogic.update() to ensure indicator is set
     * This handles the case where entities aren't loaded during construction
     */
    public void update(float delta) {
        if (!initializedIndicator) {
            initializeFionneIndicator();
            initializedIndicator = (fionneEntity != null);
        }
    }
    /**
     * ★ NEW: Initialize Fionne's quest indicator on game startup
     * This ensures the "!" appears immediately when the game loads
     */
    private void initializeFionneIndicator() {
        // Small delay to ensure entities are fully loaded
        // We'll do this in the first update cycle instead
        // For now, try to find and update Fionne immediately
        
        for (Entity entity : gameState.getEntities()) {
            NPC npc = entity.getComponent(NPC.class);
            if (npc != null && "fionne".equals(npc.getNpcId())) {
                fionneEntity = entity;
                QuestIndicator indicator = entity.getComponent(QuestIndicator.class);
                
                if (indicator != null) {
                    // Show "!" for available quest
                    indicator.show(QuestIndicator.IndicatorType.AVAILABLE);
                    System.out.println("[INTRO QUEST] Initialized Fionne's indicator: ! (AVAILABLE)");
                }
                break;
            }
        }
        
        if (fionneEntity == null) {
           // System.out.println("[INTRO QUEST] Warning: Fionne not found during initialization");
           // System.out.println("[INTRO QUEST] Indicator will be set on first interaction");
        }
    }
    /**
     * ★ Find and cache Fionne entity
     */
    private void cacheFionneEntity() {
        if (fionneEntity != null) return;
        
        for (Entity entity : gameState.getEntities()) {
            NPC npc = entity.getComponent(NPC.class);
            if (npc != null && "fionne".equals(npc.getNpcId())) {
                fionneEntity = entity;
                updateQuestIndicator();  // Set initial indicator
                break;
            }
        }
    }
    
    /**
     * Handle NPC interaction based on current quest stage
     * Returns true if interaction was handled by intro quest system
     */
    public boolean handleFionneInteraction(Entity npcEntity) {
        NPC npc = npcEntity.getComponent(NPC.class);
        if (npc == null || !npc.getNpcId().equals("fionne")) {
            return false; // Not Fionne, let other systems handle it
        }
        
        // ★ Cache Fionne entity for indicator updates
        if (fionneEntity == null) {
            fionneEntity = npcEntity;
            updateQuestIndicator();
        }
        
        UIManager ui = gameState.getUIManager();
        
        switch (currentStage) {
            case NOT_STARTED:
            case STAGE_1_DIALOGUE:  // ★ HANDLE DECLINE - Stay on Stage 1
                showStage1Dialogue(ui);
                return true;
                
            case STAGE_1_COMPLETE:
                // Check if sword equipped
                if (isSwordEquipped()) {
                    advanceToStage2Complete(ui);
                    /*
                    // ★ FIX: Return TRUE to prevent JSON dialogue from loading
                    // Stage is now STAGE_2_COMPLETE, next click will show Stage 3
                    ui.showDialogue(
                        npc.getNpcName(),
                        "I see you've equipped the sword. It will help you in your journey. Try to use it them speak with me again."
                    );
                    */
                    return true;
                } else {
                    showEquipSwordReminder(ui);
                    return true;
                } 
                
            case STAGE_2_COMPLETE:
            case STAGE_3_DIALOGUE:  // ★ HANDLE DECLINE - Stay on Stage 3
                showStage3Dialogue(ui);
                return true;
                
            case STAGE_3_COMPLETE:
            	 
            case ALL_COMPLETE:/*
                // Intro quests done, show generic dialogue
                ui.showDialogue(
                    npc.getNpcName(),
                    "I have given you all the aid I can. May your journey be swift and safe."
                );*/
                //return true;
                return false; //go straight to collect recipe for run crafting
                
            default:
                return false; // Let dialogue system handle
        }
    }
    

	/**
     * STAGE 1: First meeting with Fionne
     * Grants inventory and wooden sword
     */
    private void showStage1Dialogue(UIManager ui) {
        UIDialogueBox dialogueBox = ui.getDialogueBox();
        
        // ★ Set stage IMMEDIATELY so declining keeps player in this stage
        currentStage = IntroStage.STAGE_1_DIALOGUE;
        
        
        dialogueBox.showMessageWithAccept(
            "To return to the continent, you must craft a magical rune. For that, you'll need the proper recipes and materials. Take this. A gift from the Divine Elves.",
            () -> {
                dialogueBox.showMessageWithAccept(
                    "She hands you a shimmering pouch.",
                    () -> {
                        dialogueBox.showMessageWithAccept(
                            "Inventory Unlocked",
                            () -> completeStage1(ui),
                            () -> dialogueBox.setVisible(false)
                        );
                    },
                    () -> dialogueBox.setVisible(false)
                );
            },
            () -> {
                // ★ On decline, stay in STAGE_1_DIALOGUE (already set above)
                dialogueBox.setVisible(false);
                System.out.println("[INTRO QUEST] Player declined Stage 1 - will retry on next interaction");
            }
        ); 
    }
    
    /**
     * Complete Stage 1: Unlock inventory and give sword
     */
    private void completeStage1(UIManager ui) {
        // Unlock inventory button (shows NEW badge)
        ui.unlockMenuButton("inventory");
        
        // Add wooden sword to inventory
        Item sword = ItemManager.createWoodenShortSword();
        ui.addItemToInventory(sword);
        
        // Add test gear items for demonstration
       // ui.addTestGearItems();
        
        // Advance stage
        currentStage = IntroStage.STAGE_1_COMPLETE;
        updateQuestIndicator();  // ★ Update to IN_PROGRESS (equip sword)
        
        System.out.println("[INTRO QUEST] Stage 1 Complete: Inventory unlocked, sword received");
        
        // Close dialogue
        ui.getDialogueBox().setVisible(false);
    }
    
    /**
     * Remind player to equip the sword
     */
    private void showEquipSwordReminder(UIManager ui) {
        ui.getDialogueBox().showMessageWithAccept(
            "You need to equip the sword I gave you! Right-click it in your inventory to equip it.",
            () -> ui.getDialogueBox().setVisible(false),
            () -> ui.getDialogueBox().setVisible(false)
        );
    }
    
    /**
     * Advance to Stage 2 Complete when sword is equipped
     */
    private void advanceToStage2Complete(UIManager ui) {
        // Unlock stats button
        ui.unlockMenuButton("stats");
        
        currentStage = IntroStage.STAGE_2_COMPLETE;
        updateQuestIndicator();  // ★ Update to AVAILABLE (Stage 3 ready)
        
        System.out.println("[INTRO QUEST] Stage 2 Complete: Sword equipped, stats unlocked");
    }
    
    /**
     * STAGE 3: Quest explanation and blessing
     * Grants Rune of Return and Fionne's Blessing buff
     */
    private void showStage3Dialogue(UIManager ui) {
        UIDialogueBox dialogueBox = ui.getDialogueBox();
        
        // ★ Set stage IMMEDIATELY so declining keeps player in this stage
        currentStage = IntroStage.STAGE_3_DIALOGUE;
        
        dialogueBox.showMessageWithAccept(
            //"To craft the rune, gather: Carved Wood, Clay, Carving Stone\nThese can be found by hunting the creatures roaming this island.\nBut be cautious. Some creatures will attack without hesitation.\nIf you fall, your journey ends here.",
            "The wooden sword you equipped will grant you strength to protect yourself from evil elements \nIt is not the best weapon but its durability will be enough for your journey.",
            
            () -> {
                dialogueBox.showMessageWithAccept(
                    "Fionne raises her staff. A soft aura surrounds you.",
                    () -> {
                        dialogueBox.showMessageWithAccept(
                            "This enchantment will help you survive the first trials. And take this as well.",
                            () -> {
                                dialogueBox.showMessageWithAccept(
                                    "Quest Unlocked!", 
                                    () -> {
                                    	dialogueBox.showMessageWithAccept(
                                    	"Item obtained: Rune of Return",
                                    	() -> completeStage3(ui),
                                    	() -> dialogueBox.setVisible(false)
                                    	);
                                    },
                                    () -> dialogueBox.setVisible(false)
                                    
                                );
                            },
                            () -> dialogueBox.setVisible(false)
                        );
                    },
                    () -> dialogueBox.setVisible(false)
                );
            },
            () -> {
                // ★ On decline, stay in STAGE_3_DIALOGUE (already set above)
                dialogueBox.setVisible(false);
                System.out.println("[INTRO QUEST] Player declined Stage 3 - will retry on next interaction");
            }
        );
    }   
    /**
     * Complete Stage 3: Grant rune and blessing
     */
    private void completeStage3(UIManager ui) {
        // Add Rune of Return to inventory
        ui.addItemToInventory(ItemManager.createRuneOfReturn());
        ui.unlockQuestButton("quest");
        
        //Quest quest = new Quest("");
        // Grant Fionne's Blessing buff
        BuffManager buffManager = player.getComponent(BuffManager.class);
        if (buffManager != null) {
            Buff blessing = BuffFactory.createFionnesBlessing();
            buffManager.addBuff(blessing);
            System.out.println("Fionne's Blessing activated! +20% EXP for 20,000 kills");
        }
        
        // Notify inventory update
        ui.notifyInventoryUpdate();
        
        currentStage = IntroStage.STAGE_3_COMPLETE;
        
        updateQuestIndicator();  // ★ Update indicator (hide or show completion)
        
        System.out.println("[INTRO QUEST] Stage 3 Complete: Rune received, blessing granted");
        
        // Close dialogue
        ui.getDialogueBox().setVisible(false);
        
        // Mark all intro quests complete
        //markAllComplete(); //TODO: watch this!
    }
    /*
     * Stage 4 unlock quest menu
     * 
     */ 
    private void showStage4Dialogue(UIManager ui) {
    	
	UIDialogueBox dialogueBox = ui.getDialogueBox();
	        
	        // ★ Set stage IMMEDIATELY so declining keeps player in this stage
	        currentStage = IntroStage.STAGE_4_DIALOGUE;
	        /*
	        dialogueBox.showMessageWithAccept(
	            "To craft the rune, gather: Carved Wood, Clay, Carving Stone\nThese can be found by hunting the creatures roaming this island.\nBut be cautious. Some creatures will attack without hesitation.\nIf you fall, your journey ends here.",
	            () -> {
	                dialogueBox.showMessageWithAccept(
	                    "Fionne raises her staff. A soft aura surrounds you.",
	                    () -> {
	                        dialogueBox.showMessageWithAccept(
	                            "This enchantment will help you survive the first trials. And take this as well.",
	                            () -> {
	                                dialogueBox.showMessageWithAccept(
	                                    "Item obtained: Rune of Return",
	                                    () -> completeStage3(ui),
	                                    () -> dialogueBox.setVisible(false)
	                                );
	                            },
	                            () -> dialogueBox.setVisible(false)
	                        );
	                    },
	                    () -> dialogueBox.setVisible(false)
	                );
	            },
	            () -> {
	                // ★ On decline, stay in STAGE_3_DIALOGUE (already set above)
	                dialogueBox.setVisible(false);
	                System.out.println("[INTRO QUEST] Player declined Stage 3 - will retry on next interaction");
	            }
	        );
			*/
	}
    /**
     * Mark all intro quests as complete
     */
    private void markAllComplete() {
        currentStage = IntroStage.ALL_COMPLETE;
        updateQuestIndicator();  // ★ Hide indicator
      //  System.out.println("[INTRO QUEST] All introduction quests complete!");
    }
    /**
     * ★ REFACTORED: Update quest indicator based on current stage
     */
    private void updateQuestIndicator() {
        cacheFionneEntity();  // Ensure we have Fionne
        
        if (fionneEntity == null) {
           // System.err.println("[INTRO QUEST] Warning: Cannot update indicator - Fionne entity not found");
            return;
        }
        
        QuestIndicator indicator = fionneEntity.getComponent(QuestIndicator.class);
        if (indicator == null) {
           // System.err.println("[INTRO QUEST] Warning: Fionne has no QuestIndicator component");
            return;
        }
        
        // ★ DEBUG: Log state change
        //System.out.println("[INTRO QUEST] Updating indicator for stage: " + currentStage);
        //System.out.println("[INTRO QUEST] Before update - active=" + indicator.active + 
         //                  ", type=" + indicator.type);
        
        switch (currentStage) {
            case NOT_STARTED:
            case STAGE_1_DIALOGUE:
                // Quest available
                indicator.show(IndicatorType.AVAILABLE);
               // System.out.println("[INTRO QUEST] Set indicator: ! (Quest Available)");
                break;
                
            case STAGE_1_COMPLETE:
                // In progress (waiting for sword equip)
                indicator.show(IndicatorType.IN_PROGRESS);
               // System.out.println("[INTRO QUEST] Set indicator: ... (Equip Sword)");
                break;
                
            case STAGE_2_COMPLETE:
            case STAGE_3_DIALOGUE:
                // Next quest available
                indicator.show(IndicatorType.AVAILABLE);
               //System.out.println("[INTRO QUEST] Set indicator: ! (Next Quest Available)");
                break;
                
            case STAGE_3_COMPLETE:
            case ALL_COMPLETE:
                // All done, hide indicator
                //indicator.hide();
                indicator.show(IndicatorType.IN_PROGRESS);
                //System.out.println("[INTRO QUEST] Set indicator: Hidden (All Complete)");
                break;
                
            default:
                //indicator.hide();
            	indicator.show(IndicatorType.COMPLETE);
                //System.out.println("[INTRO QUEST] Set indicator: Hidden (Default)");
                break;
        }
        
        // ★ DEBUG: Verify change
        //System.out.println("[INTRO QUEST] After update - active=" + indicator.active + 
         //                  ", type=" + indicator.type);
    }
    /**
     * ★ UPDATED: Check and update sword equip status
     * Call this method whenever equipment changes
     */
    public void checkSwordEquipStatus() {
        if (currentStage == IntroStage.STAGE_1_COMPLETE) {
            if (isSwordEquipped()) {
                // ★ Sword equipped! Show message immediately
                UIManager ui = gameState.getUIManager();
                advanceToStage2Complete(ui);
                
                //System.out.println("[INTRO QUEST] Sword equipped detected - advancing to Stage 2 Complete");
            }
        }
    } 
    
    /**
     * Check if wooden sword is equipped
     */
    private boolean isSwordEquipped() {
        UIManager ui = gameState.getUIManager();
        return ui != null && ui.isWoodenSwordEquipped();
    }
    
    /**
     * Check if player has sword in inventory but not equipped
     */
    public boolean hasSwordButNotEquipped() {
        UIManager ui = gameState.getUIManager();
        return ui != null && ui.hasWoodenSwordInInventory() && !ui.isWoodenSwordEquipped();
    }
    
    /**
     * Notify when sword is equipped (call from UIManager.equipItem)
     */
    public void onSwordEquipped() {
        if (currentStage == IntroStage.STAGE_1_COMPLETE) {
            UIManager ui = gameState.getUIManager();
            advanceToStage2Complete(ui);
        }
    }
    // HELPER METHODS FOR ADDING MORE QUESTS
    // ════════════════════════════════════════════════════════════
    
    /**
     * Add a new stage to the intro quest sequence.
     * 
     * EXAMPLE: Add Stage 4 - Kill 5 Slimes
     * 
     * 1. Add enum value:
     *    STAGE_4_DIALOGUE,
     *    STAGE_4_COMPLETE,
     * 
     * 2. Add case to handleFionneInteraction:
     *    case STAGE_3_COMPLETE:
     *        showStage4Dialogue(ui);
     *        return true;
     * 
     * 3. Implement stage methods:
     *    private void showStage4Dialogue(UIManager ui) {
     *        dialogueBox.showMessageWithAccept(
     *            "Now, prove your strength. Hunt 5 slimes.",
     *            () -> completeStage4(ui),
     *            () -> dialogueBox.setVisible(false)
     *        );
     *    }
     * 
     * 4. Complete stage:
     *    private void completeStage4(UIManager ui) {
     *        // Grant rewards
     *        ui.addItemToInventory(ItemManager.createHealthPotion());
     *        currentStage = IntroStage.STAGE_4_COMPLETE;
     *    }
     */
    
    /**
     * Template for adding a new sequential dialogue stage
     */
    @SuppressWarnings("unused")
    private void showStageTemplate(UIManager ui, String dialogueText, Runnable onComplete) {
        UIDialogueBox dialogueBox = ui.getDialogueBox();
        
        dialogueBox.showMessageWithAccept(
            dialogueText,
            () -> {
                // Chain more dialogues if needed
                dialogueBox.showMessageWithAccept(
                    "Second message...",
                    () -> {
                        onComplete.run();
                        dialogueBox.setVisible(false);
                    },
                    () -> dialogueBox.setVisible(false)
                );
            },
            () -> dialogueBox.setVisible(false)
        );
    }
    
    /**
     * Template for completing a stage with rewards
     */
    @SuppressWarnings("unused")
    private void completeStageTemplate(UIManager ui, IntroStage nextStage) {
        // Grant items
        // ui.addItemToInventory(ItemManager.createSomeItem());
        
        // Unlock features
        // ui.unlockMenuButton("feature_id");
        
        // Grant buffs
        // BuffManager buffManager = player.getComponent(BuffManager.class);
        // if (buffManager != null) {
        //     buffManager.addBuff(BuffFactory.createSomeBuff());
        // }
        
        // Advance stage
        currentStage = nextStage;
        
        System.out.println("[INTRO QUEST] Stage Complete: " + nextStage);
    }
    
    // ════════════════════════════════════════════════════════════
    // GETTERS AND STATE CHECKS
    // ════════════════════════════════════════════════════════════
    
    public IntroStage getCurrentStage() {
        return currentStage;
    }
    
    public boolean isStage1Complete() {
        return currentStage.ordinal() >= IntroStage.STAGE_1_COMPLETE.ordinal();
    }
    
    public boolean isStage2Complete() {
        return currentStage.ordinal() >= IntroStage.STAGE_2_COMPLETE.ordinal();
    }
    
    public boolean isStage3Complete() {
        return currentStage.ordinal() >= IntroStage.STAGE_3_COMPLETE.ordinal();
    }
    
    public boolean isAllComplete() {
        return currentStage == IntroStage.ALL_COMPLETE;
    }
    
    /**
     * Check if a specific stage is active (for progression gates)
     */
    public boolean isStageActive(IntroStage stage) {
        return currentStage == stage;
    }
    
    /**
     * Check if player has progressed past a certain stage
     */
    public boolean hasCompletedStage(IntroStage stage) {
        return currentStage.ordinal() >= stage.ordinal();
    }
    
    /**
     * Debug: Force advance to specific stage
     */
    public void forceSetStage(IntroStage stage) {
        currentStage = stage;
        updateQuestIndicator();  // ★ Update indicator when forcing stage
        //System.out.println("[INTRO QUEST DEBUG] Force set stage to: " + stage);
    }
    
    /**
     * Debug: Reset all intro quests
     */
    public void resetIntroQuests() {
        currentStage = IntroStage.NOT_STARTED;
        updateQuestIndicator();  // ★ Reset indicator
        //System.out.println("[INTRO QUEST DEBUG] Reset to beginning");
    }
}