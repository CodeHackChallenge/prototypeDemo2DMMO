package dev.main;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;
import javax.swing.JPanel;

import dev.main.dialogue.DialogueDatabase;
import dev.main.dialogue.DialogueTree;
import dev.main.entity.Entity;
import dev.main.entity.EntityType;
import dev.main.entity.Experience;
import dev.main.entity.LevelUpEffect;
import dev.main.entity.NPC;
import dev.main.input.CollisionBox;
import dev.main.input.MouseInput;
import dev.main.input.Position;
import dev.main.item.ItemManager;
import dev.main.quest.IntroQuestHandler;
import dev.main.quest.QuestIndicator;
import dev.main.render.Renderer;
import dev.main.state.GameLogic;
import dev.main.state.GameState;
import dev.main.stats.Stats;
import dev.main.ui.Quest;
import dev.main.ui.UIButton;
import dev.main.ui.UIDialogueBoxEnhanced;
import dev.main.ui.UIManager;
import dev.main.util.DamageText;

public class Engine extends Canvas implements Runnable, KeyListener {

	public boolean rendererDebugMode = false;  // Add this field to Engine class

	
    //constants
    public static final int Eclipse = 0;
    public static final int VSCode = 1;
    public static int IDE = -1;
    
    // Display constants
    public static final int SPRITE_SIZE = 64;
    private static final int SCALE = 2;
    
    public static final int WIDTH = 640 * SCALE;
    public static final int HEIGHT = 320 * SCALE;
    
    // Game loop constants
    public static final int UPS = 60;
    public static final int FPS = 120;

    // Engine state
    private boolean isRunning = false;
    private Thread thread;
    private BufferStrategy bufferStrategy;
    private boolean lastMousePressed = false;
    
    // Input
    private MouseInput mouse;
    
    // Game systems
    private GameState gameState;
    private GameLogic gameLogic;
    private Renderer renderer;
    
    private boolean shiftPressed = false;
    private boolean debugMode = false;
    
    private Cursor defaultCursor;
    private Cursor attackCursor;
    private static Engine instance;
    
    public Engine() {
        setupIDE(Engine.Eclipse);
        
        instance = this;
        
        // Window setup
        JFrame window = new JFrame("RO-Style 2D Game v.1");
        JPanel panel = (JPanel) window.getContentPane();
        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        panel.setLayout(null);
        panel.add(this);

        setBounds(0, 0, WIDTH, HEIGHT);
        setIgnoreRepaint(true);

        window.pack();
        window.setVisible(true);
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
        if(Engine.IDE == Engine.VSCode) {
            // VS Code cursor setup
            defaultCursor = Cursor.getDefaultCursor();
            try {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                try {
                    java.net.URL cursorUrl = getClass().getResource("/dev/main/resources/items/icons/sword.png");
                    if (cursorUrl == null) {
                        cursorUrl = getClass().getResource("/dev/main/resources/items/icons/sword2.cur");
                    }

                    if (cursorUrl != null) {
                        System.out.println("Loading cursor from: " + cursorUrl);
                        java.awt.image.BufferedImage cursorImg = javax.imageio.ImageIO.read(cursorUrl);
                        if (cursorImg != null) {
                            attackCursor = toolkit.createCustomCursor(cursorImg, new Point(16, 16), "attack");
                            System.out.println("Custom attack cursor created.");
                        } else {
                            System.out.println("Cursor resource found but failed to read image; using default cursor.");
                            attackCursor = defaultCursor;
                        }
                    } else {
                        System.out.println("Cursor resource not found on classpath; trying local file fallback.");
                        java.io.File f = new java.io.File("bin/dev/main/resources/items/icons/sword.png");
                        if (f.exists()) {
                            java.awt.image.BufferedImage cursorImg = javax.imageio.ImageIO.read(f);
                            attackCursor = toolkit.createCustomCursor(cursorImg, new Point(16, 16), "attack");
                        } else {
                            attackCursor = defaultCursor;
                        }
                    }
                } catch (Exception ex) {
                    attackCursor = defaultCursor;
                }
            } catch (Exception e) {
                attackCursor = defaultCursor;
            }
        } else if (Engine.IDE == Engine.Eclipse) {
            // Eclipse cursor setup
            defaultCursor = Cursor.getDefaultCursor();
            try {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image cursorImage = toolkit.getImage("resources/items/icons/sword.png");
                attackCursor = toolkit.createCustomCursor(cursorImage, new Point(16, 16), "attack");
            } catch (Exception e) {
                attackCursor = defaultCursor;
            }
        }
        
        // Input
        mouse = new MouseInput();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();

        // Buffer strategy
        createBufferStrategy(2);
        bufferStrategy = getBufferStrategy();

        // Initialize game
        gameSetup();

        // Attach mouse wheel listener
        if (gameState != null && gameState.getUIManager() != null) {
            addMouseWheelListener(gameState.getUIManager());
        }
    }

    private void setupIDE(int ide) {
        switch(ide) {
            case Eclipse: IDE = Eclipse; break;
            case VSCode: IDE = VSCode; break;
        }
    }

    private void gameSetup() { 
        // Initialize game systems
        gameState = new GameState();
        gameLogic = new GameLogic(gameState);
        renderer = new Renderer(gameState, this);
        
        // Connect UI Manager to GameLogic
        gameState.setGameLogic(gameLogic);
        
        System.out.println("Game initialized!");
    }
    
    public void update(float delta) {
        // Only check mouse hover when needed
        if (mouse.hasMoved()) {
            handleMouseHover();
        }
        
        // Handle input
        handleInput();
        
        // Update game logic
        gameLogic.update(delta);
        
        // Update UI
        gameState.getUIManager().update(delta);
    }
    
    private void handleMouseHover() {
        if (!mouse.hasMoved()) {
            return;
        }
        
        int screenX = mouse.getX();
        int screenY = mouse.getY();
        
        float worldX = screenX + gameState.getCameraX();
        float worldY = screenY + gameState.getCameraY();
        
        // Check if hovering over any entity (monster or NPC)
        Entity hoveredEntity = null;
        
        for (Entity entity : gameState.getEntities()) {
            if (entity.getType() != EntityType.MONSTER && entity.getType() != EntityType.NPC) {
                continue;
            }
            
            Position pos = entity.getComponent(Position.class);
            CollisionBox box = entity.getComponent(CollisionBox.class);
            
            if (pos != null && box != null) {
                float left = box.getLeft(pos.x);
                float right = box.getRight(pos.x);
                float top = box.getTop(pos.y);
                float bottom = box.getBottom(pos.y);
                
                if (worldX >= left && worldX <= right && worldY >= top && worldY <= bottom) {
                    hoveredEntity = entity;
                    break;
                }
            }
        }
        
        // Update hover state
        gameState.setHoveredEntity(hoveredEntity);

        // Change cursor
        if (hoveredEntity != null) {
            setCursor(attackCursor);
        } else {
            setCursor(defaultCursor);
        }
    }
    
    private void handleInput() {
        if (mouse.isLeftClick()) {
            int screenX = mouse.getX();
            int screenY = mouse.getY();
            
            // Check UI clicks first
            boolean uiConsumedClick = gameState.getUIManager().handleClick(screenX, screenY);
            
            if (!uiConsumedClick) {
                float worldX = screenX + gameState.getCameraX();
                float worldY = screenY + gameState.getCameraY();
                
                Entity hoveredEntity = gameState.getHoveredEntity();
                
                // Check if clicking an NPC
                if (hoveredEntity != null && hoveredEntity.getType() == EntityType.NPC) {
                    handleNPCClick(hoveredEntity);
                }
                // Check if clicking a monster
                else if (hoveredEntity != null && hoveredEntity.getType() == EntityType.MONSTER) {
                    Stats stats = hoveredEntity.getComponent(Stats.class);
                    if (stats != null && stats.hp > 0) {
                        gameState.setTargetedEntity(hoveredEntity);
                        gameLogic.playerAttack(hoveredEntity);
                        System.out.println("Attacking " + hoveredEntity.getName());
                    }
                } else {
                    // Clicking empty ground
                    gameLogic.stopAutoAttack();
                    gameLogic.movePlayerTo(worldX, worldY, shiftPressed);
                }
            }
            
            mouse.resetPressed();
        }
        
        if (mouse.isRightClick()) {
            int screenX = mouse.getX();
            int screenY = mouse.getY();
            
            boolean uiConsumedClick = gameState.getUIManager().handleRightClick(screenX, screenY);
            
            if (!uiConsumedClick) {
                float worldX = screenX + gameState.getCameraX();
                float worldY = screenY + gameState.getCameraY();
                
                gameLogic.stopAutoAttack();
                gameLogic.movePlayerTo(worldX, worldY, shiftPressed);
            }
            
            mouse.resetPressed();
        }
    }
    
    /**
     * ★ REFACTORED: Handle NPC interaction
     * Now uses IntroQuestHandler for Fionne, falls back to normal dialogue for others
     */
    private void handleNPCClick(Entity npc) {
        NPC npcComponent = npc.getComponent(NPC.class);
        if (npcComponent == null) return;
        
        Entity player = gameState.getPlayer();
        
        // Check range
        if (!npcComponent.isPlayerInRange(player, npc)) {
            System.out.println("Too far away to talk to " + npcComponent.getNpcName());
            return;
        }
        
        // ★ Check if intro quest handler wants to handle this interaction
        IntroQuestHandler introHandler = gameState.getIntroQuestHandler();
        if (introHandler != null && introHandler.handleFionneInteraction(npc)) {
            return; // Intro quest handled it
        }
        
        // ★ ORIGINAL CODE FOR OTHER NPCs
        // If NPC has a completed quest for the player, show quest completion dialog
        Quest completedQuest = npcComponent.getCompletedQuest();
        if (completedQuest != null) {
            gameState.getUIManager().showQuestComplete(npcComponent.getNpcName(), completedQuest);
            return;
        }

        // If NPC has an active quest in-progress, show progress dialogue
        Quest activeQuest = npcComponent.getActiveQuest();
        if (activeQuest != null) {
            gameState.getUIManager().showDialogue(npcComponent.getNpcName(), npcComponent.getDialogue());
            return;
        }

        // Otherwise fall back to dialogue database or greeting
        DialogueDatabase db = DialogueDatabase.getInstance();
        DialogueTree dialogue = db.getDialogueForNPC(npcComponent.getNpcId());

        if (dialogue != null) {
            // Start dialogue using enhanced dialogue box
            UIDialogueBoxEnhanced dialogueBox = gameState.getUIManager().getEnhancedDialogueBox();
            dialogueBox.startDialogue(dialogue.getId(), npc, player);
        } else {
            // Fallback to simple greeting
            gameState.getUIManager().showDialogue(
                npcComponent.getNpcName(),
                npcComponent.getGreetingDialogue()
            );
        }
    }
   
    public boolean isDebugMode() {
        return debugMode;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
    	// In keyPressed():
    	if (e.getKeyCode() == KeyEvent.VK_R) {
    	    rendererDebugMode = !rendererDebugMode;
    	    System.out.println("Renderer debug mode: " + (rendererDebugMode ? "ON" : "OFF"));
    	    
    	    if (rendererDebugMode) {
    	        System.out.println("\nRenderer will now print indicator state EVERY FRAME.");
    	        System.out.println("Watch the console to see what the renderer is reading.");
    	        System.out.println("Press 'R' again to turn off.\n");
    	    }
    	}
    	
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shiftPressed = true;
        }
        
        // Toggle debug mode with F3
        if (e.getKeyCode() == KeyEvent.VK_F3) {
            debugMode = !debugMode;
            System.out.println("Debug mode: " + (debugMode ? "ON" : "OFF"));
        }
        
        // Handle skill hotkeys AND inventory key
        gameState.getUIManager().handleKeyPress(e.getKeyCode());
        
        // Show controls help (press F1)
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            System.out.println("\n╔═══════════════════════════════════════╗");
            System.out.println("║         GAME CONTROLS                ║");
            System.out.println("╠═══════════════════════════════════════╣");
            System.out.println("║ Movement:                            ║");
            System.out.println("║   Left Click - Move / Attack         ║");
            System.out.println("║   Right Click - Force Move           ║");
            System.out.println("║   Shift + Click - Run                ║");
            System.out.println("║                                      ║");
            System.out.println("║ Skills:                              ║");
            System.out.println("║   1-8 - Use skill slots              ║");
            System.out.println("║   Q, E, R, F - Quick skills          ║");
            System.out.println("║   Right Click Slot - Upgrade skill   ║");
            System.out.println("║                                      ║");
            System.out.println("║ UI:                                  ║");
            System.out.println("║   I - Toggle Inventory               ║");
            System.out.println("║   J - Toggle Quest Log               ║");
            System.out.println("║                                      ║");
            System.out.println("║ Debug:                               ║");
            System.out.println("║   F1 - Show this help                ║");
            System.out.println("║   F3 - Toggle debug mode             ║");
            System.out.println("║   N - Next intro quest stage         ║");
            System.out.println("║   M - Reset intro quests             ║");
            System.out.println("║   D - Damage self                    ║");
            System.out.println("║   F - Full heal                      ║");
            System.out.println("║   X - Add XP                         ║");
            System.out.println("║   S - Show stats                     ║");
            System.out.println("╚═══════════════════════════════════════╝\n");
        }
        
        // ★ NEW: Debug intro quest controls
        // Advance intro quest (press N for "Next stage")
        if (e.getKeyCode() == KeyEvent.VK_N) {
            IntroQuestHandler ih = gameState.getIntroQuestHandler();
            if (ih != null) {
                IntroQuestHandler.IntroStage current = ih.getCurrentStage();
                IntroQuestHandler.IntroStage[] stages = IntroQuestHandler.IntroStage.values();
                
                int nextIndex = (current.ordinal() + 1) % stages.length;
                ih.forceSetStage(stages[nextIndex]);
                
                System.out.println("DEBUG: Advanced intro quest to " + stages[nextIndex]);
            }
        }
        
        // Reset intro quest (press M for "reset Main quest")
        if (e.getKeyCode() == KeyEvent.VK_M) {
            IntroQuestHandler ih = gameState.getIntroQuestHandler();
            if (ih != null) {
                ih.resetIntroQuests();
            }
        }
        
        // Test unlocking menu buttons (press U)
        if (e.getKeyCode() == KeyEvent.VK_U) {
            String[] buttonIds = {"settings", "crafting", "quest", "skilltree", "stats", "character", "trade", "message", "world"};
            boolean unlocked = false;  
            
            for (String id : buttonIds) {
                UIButton button = gameState.getUIManager().getMenuButton(id);
                if (button != null && button.isLocked()) {
                    gameState.getUIManager().unlockMenuButton(id);
                    unlocked = true;
                    break;
                }
            }
            
            if (!unlocked) {
                System.out.println("DEBUG: All menu buttons already unlocked!");
            }
        }
        
        // Lock all buttons again (press L)
        if (e.getKeyCode() == KeyEvent.VK_L) {
            String[] buttonIds = {"settings", "crafting", "quest", "skilltree", "stats", "character", "trade", "message", "world"};
            
            for (String id : buttonIds) {
                gameState.getUIManager().lockMenuButton(id);
            }
            
            System.out.println("DEBUG: Locked all menu buttons except Inventory");
        }
        
        // Add test item to inventory (press G for "Get item")
        if (e.getKeyCode() == KeyEvent.VK_G) {
            boolean added = gameState.getUIManager().addItemToInventory(ItemManager.createWoodenShortSword());
            if (added) {
                System.out.println("DEBUG: Added test item to inventory");
            } else {
                System.out.println("DEBUG: Inventory is full!");
            }
        }
        
        // Clear all items from inventory (press C for "Clear")
        if (e.getKeyCode() == KeyEvent.VK_C) {
            // Implementation omitted for brevity - same as before
        }
        
        // Debug: Damage player (press D)
        if (e.getKeyCode() == KeyEvent.VK_D) {
            Entity player = gameState.getPlayer();
            Stats stats = player.getComponent(Stats.class);
            if (stats != null) {
                stats.hp -= 10;
                if (stats.hp < 0) stats.hp = 0;
                System.out.println("HP: " + stats.hp + "/" + stats.maxHp);
            }
        }
        
        // Full heal command (press F)
        if (e.getKeyCode() == KeyEvent.VK_F) {
            Entity player = gameState.getPlayer();
            Stats stats = player.getComponent(Stats.class);
            Position pos = player.getComponent(Position.class);
            
            if (stats != null) {
                stats.fullHeal();
                System.out.println("DEBUG: Full heal!");
                
                if (pos != null) {
                    DamageText healText = new DamageText("HEALED!", DamageText.Type.HEAL, pos.x, pos.y - 30);
                    gameState.addDamageText(healText);
                }
            }
        }
        
        // Add XP for testing (press X)
        if (e.getKeyCode() == KeyEvent.VK_X) {
            Entity player = gameState.getPlayer();
            Experience exp = player.getComponent(Experience.class);
            Stats stats = player.getComponent(Stats.class);
            
            if (exp != null && stats != null) {
                int xpGain = 100;
                System.out.println("DEBUG: Adding " + xpGain + " XP");
                
                int levelsGained = exp.addExperience(xpGain);
                
                if (levelsGained > 0) {
                    stats.applyLevelStats(exp, true);
                    
                    LevelUpEffect levelUpEffect = player.getComponent(LevelUpEffect.class);
                    if (levelUpEffect != null) {
                        levelUpEffect.trigger(exp.level);
                    }
                    
                    Position pos = player.getComponent(Position.class);
                    if (pos != null) {
                        DamageText levelText = new DamageText("LEVEL UP! " + exp.level, DamageText.Type.HEAL, pos.x, pos.y - 40);
                        gameState.addDamageText(levelText);
                    }
                }
            }
        }
     // ════════════════════════════════════════════════════════════════════════
     // ADD TO Engine.java keyPressed() - Press 'H' to test IntroQuestHandler
     // ════════════════════════════════════════════════════════════════════════

     if (e.getKeyCode() == KeyEvent.VK_H) {
         System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
         System.out.println("║  INTRO QUEST HANDLER TEST                               ║");
         System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
         
         // Get the intro quest handler
         dev.main.quest.IntroQuestHandler handler = gameState.getIntroQuestHandler();
         
         if (handler == null) {
             System.out.println("✗ ERROR: IntroQuestHandler is null!");
             return;
         }
         
         System.out.println("✓ IntroQuestHandler found");
         System.out.println("  Current stage: " + handler.getCurrentStage());
         
         // Find Fionne
         System.out.println("\nSearching for Fionne...");
         dev.main.entity.Entity fionneEntity = null;
         for (dev.main.entity.Entity entity : gameState.getEntities()) {
             dev.main.entity.NPC npc = entity.getComponent(dev.main.entity.NPC.class);
             if (npc != null && "fionne".equals(npc.getNpcId())) {
                 fionneEntity = entity;
                 System.out.println("✓ Found Fionne: " + entity);
                 break;
             }
         }
         
         if (fionneEntity == null) {
             System.out.println("✗ ERROR: Fionne not found!");
             return;
         }
         
         // Get current indicator state
         dev.main.quest.QuestIndicator indicator = 
             fionneEntity.getComponent(dev.main.quest.QuestIndicator.class);
         
         if (indicator == null) {
             System.out.println("✗ ERROR: QuestIndicator not found!");
             return;
         }
         
         System.out.println("\nCurrent indicator state:");
         System.out.println("  active: " + indicator.active);
         System.out.println("  type: " + indicator.type);
         System.out.println("  symbol: '" + indicator.getSymbol() + "'");
         System.out.println("  color: " + indicator.getColor());
         
         // Test: Force stage to STAGE_1_COMPLETE
         System.out.println("\nTest: Forcing stage to STAGE_1_COMPLETE...");
         handler.forceSetStage(dev.main.quest.IntroQuestHandler.IntroStage.STAGE_1_COMPLETE);
         
         System.out.println("After forceSetStage:");
         System.out.println("  Handler stage: " + handler.getCurrentStage());
         System.out.println("  Indicator type: " + indicator.type);
         System.out.println("  Indicator symbol: '" + indicator.getSymbol() + "'");
         System.out.println("  Expected: IN_PROGRESS / '...'");
         
         boolean testPass = indicator.type == dev.main.quest.QuestIndicator.IndicatorType.IN_PROGRESS;
         
         if (testPass) {
             System.out.println("\n✓ IntroQuestHandler.forceSetStage() works!");
             System.out.println("  The handler CAN update the indicator correctly.");
             System.out.println("\n  Now look at Fionne on screen:");
             System.out.println("  - If you see '...' (gray) → Handler works, problem was in dialogue flow");
             System.out.println("  - If you see '!' (gold) → Renderer is reading wrong data");
         } else {
             System.out.println("\n✗ IntroQuestHandler.forceSetStage() FAILED!");
             System.out.println("  The handler is NOT updating the indicator.");
             System.out.println("  Check IntroQuestHandler.updateQuestIndicator() method.");
         }
         
         System.out.println("\nWaiting 3 seconds... watch Fionne's indicator...");
         System.out.println("(The indicator should be bouncing with '...' if working)\n");
     }
	     // ════════════════════════════════════════════════════════════════════════
	     // ADD THIS TEST METHOD TO Engine.java keyPressed()
	     // Press 'Q' to run a complete quest indicator test
	     // ════════════════════════════════════════════════════════════════════════
	
	     if (e.getKeyCode() == KeyEvent.VK_Q) {
	         System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
	         System.out.println("║     QUEST INDICATOR UNIT TEST                           ║");
	         System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
	         
	         // Step 1: Find Fionne
	         System.out.println("Step 1: Finding Fionne entity...");
	         Entity fionneEntity = null;
	         for (Entity entity : gameState.getEntities()) {
	             dev.main.entity.NPC npc = entity.getComponent(dev.main.entity.NPC.class);
	             if (npc != null && "fionne".equals(npc.getNpcId())) {
	                 fionneEntity = entity;
	                 System.out.println("✓ Found Fionne: " + entity);
	                 break;
	             }
	         }
	         
	         if (fionneEntity == null) {
	             System.out.println("✗ ERROR: Fionne not found!");
	             return;
	         }
	         
	         // Step 2: Get QuestIndicator component
	         System.out.println("\nStep 2: Getting QuestIndicator component...");
	         dev.main.quest.QuestIndicator indicator = 
	             fionneEntity.getComponent(dev.main.quest.QuestIndicator.class);
	         
	         if (indicator == null) {
	             System.out.println("✗ ERROR: QuestIndicator component not found!");
	             return;
	         }
	         System.out.println("✓ QuestIndicator found: " + indicator);
	         System.out.println("  Current state:");
	         System.out.println("    - active: " + indicator.active);
	         System.out.println("    - type: " + indicator.type);
	         System.out.println("    - isVisible(): " + indicator.isVisible());
	         System.out.println("    - getSymbol(): '" + indicator.getSymbol() + "'");
	         System.out.println("    - getColor(): " + indicator.getColor());
	         
	         // Step 3: Test AVAILABLE state
	         System.out.println("\nStep 3: Testing AVAILABLE state...");
	         indicator.show(dev.main.quest.QuestIndicator.IndicatorType.AVAILABLE);
	         System.out.println("  After show(AVAILABLE):");
	         System.out.println("    - type: " + indicator.type);
	         System.out.println("    - getSymbol(): '" + indicator.getSymbol() + "'");
	         System.out.println("    - getColor(): " + indicator.getColor());
	         
	         boolean test1Pass = indicator.type == dev.main.quest.QuestIndicator.IndicatorType.AVAILABLE
	                          && indicator.getSymbol().equals("!")
	                          && indicator.getColor().equals(new java.awt.Color(255, 215, 0));
	         System.out.println(test1Pass ? "✓ AVAILABLE test PASSED" : "✗ AVAILABLE test FAILED");
	         
	         // Step 4: Test IN_PROGRESS state
	         System.out.println("\nStep 4: Testing IN_PROGRESS state...");
	         indicator.show(dev.main.quest.QuestIndicator.IndicatorType.IN_PROGRESS);
	         System.out.println("  After show(IN_PROGRESS):");
	         System.out.println("    - type: " + indicator.type);
	         System.out.println("    - getSymbol(): '" + indicator.getSymbol() + "'");
	         System.out.println("    - getColor(): " + indicator.getColor());
	         
	         boolean test2Pass = indicator.type == dev.main.quest.QuestIndicator.IndicatorType.IN_PROGRESS
	                          && indicator.getSymbol().equals("...")
	                          && indicator.getColor().equals(new java.awt.Color(150, 150, 150));
	         System.out.println(test2Pass ? "✓ IN_PROGRESS test PASSED" : "✗ IN_PROGRESS test FAILED");
	         
	         // Step 5: Test COMPLETE state
	         System.out.println("\nStep 5: Testing COMPLETE state...");
	         indicator.show(dev.main.quest.QuestIndicator.IndicatorType.COMPLETE);
	         System.out.println("  After show(COMPLETE):");
	         System.out.println("    - type: " + indicator.type);
	         System.out.println("    - getSymbol(): '" + indicator.getSymbol() + "'");
	         System.out.println("    - getColor(): " + indicator.getColor());
	         
	         boolean test3Pass = indicator.type == dev.main.quest.QuestIndicator.IndicatorType.COMPLETE
	                          && indicator.getSymbol().equals("?")
	                          && indicator.getColor().equals(new java.awt.Color(255, 215, 0));
	         System.out.println(test3Pass ? "✓ COMPLETE test PASSED" : "✗ COMPLETE test FAILED");
	         
	         // Step 6: Test hide()
	         System.out.println("\nStep 6: Testing hide()...");
	         indicator.hide();
	         System.out.println("  After hide():");
	         System.out.println("    - active: " + indicator.active);
	         System.out.println("    - isVisible(): " + indicator.isVisible());
	         
	         boolean test4Pass = !indicator.active && !indicator.isVisible();
	         System.out.println(test4Pass ? "✓ hide() test PASSED" : "✗ hide() test FAILED");
	         
	         // Step 7: Test animation
	         System.out.println("\nStep 7: Testing animation...");
	         indicator.show(dev.main.quest.QuestIndicator.IndicatorType.AVAILABLE);
	         float oldOffset = indicator.bounceOffset;
	         indicator.update(0.016f); // ~1 frame at 60fps
	         float newOffset = indicator.bounceOffset;
	         System.out.println("  Bounce offset changed: " + oldOffset + " → " + newOffset);
	         
	         boolean test5Pass = oldOffset != newOffset;
	         System.out.println(test5Pass ? "✓ Animation test PASSED" : "✗ Animation test FAILED");
	         
	         // Final summary
	         System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
	         System.out.println("║     TEST RESULTS                                        ║");
	         System.out.println("╠═══════════════════════════════════════════════════════════╣");
	         System.out.println("║ AVAILABLE state:  " + (test1Pass ? "✓ PASS" : "✗ FAIL") + "                               ║");
	         System.out.println("║ IN_PROGRESS state:" + (test2Pass ? "✓ PASS" : "✗ FAIL") + "                               ║");
	         System.out.println("║ COMPLETE state:   " + (test3Pass ? "✓ PASS" : "✗ FAIL") + "                               ║");
	         System.out.println("║ hide() method:    " + (test4Pass ? "✓ PASS" : "✗ FAIL") + "                               ║");
	         System.out.println("║ Animation:        " + (test5Pass ? "✓ PASS" : "✗ FAIL") + "                               ║");
	         System.out.println("╚═══════════════════════════════════════════════════════════╝");
	         
	         boolean allPass = test1Pass && test2Pass && test3Pass && test4Pass && test5Pass;
	         if (allPass) {
	             System.out.println("\n🎉 ALL TESTS PASSED! Quest indicator is working correctly.");
	             System.out.println("   The issue is likely in IntroQuestHandler or rendering.");
	         } else {
	             System.out.println("\n⚠️  SOME TESTS FAILED! Quest indicator has bugs.");
	             System.out.println("   Fix QuestIndicator.java before debugging other components.");
	         }
	         
	         // Reset to AVAILABLE for visual testing
	         System.out.println("\nResetting indicator to AVAILABLE state for visual verification...");
	         indicator.show(dev.main.quest.QuestIndicator.IndicatorType.AVAILABLE);
	         System.out.println("Look at Fionne - you should see a gold '!' above her head.\n");
	     }
	  // Press 'T' to test indicator change
	     if (e.getKeyCode() == KeyEvent.VK_T) {
	         for (Entity entity : gameState.getEntities()) {
	             NPC npc = entity.getComponent(NPC.class);
	             if (npc != null && "fionne".equals(npc.getNpcId())) {
	                 QuestIndicator qi = entity.getComponent(QuestIndicator.class);
	                 if (qi != null) {
	                     System.out.println("BEFORE: type=" + qi.type);
	                     qi.show(QuestIndicator.IndicatorType.IN_PROGRESS);
	                     System.out.println("AFTER: type=" + qi.type);
	                     System.out.println("Symbol should be: " + qi.getSymbol());
	                 }
	             }
	         }
	     }
	  // ════════════════════════════════════════════════════════════════════════
	  // ENUM SWITCH BUG TEST
	  // Add this to Engine.java keyPressed() - Press 'E' to test enum behavior
	  // ════════════════════════════════════════════════════════════════════════
	
	  if (e.getKeyCode() == KeyEvent.VK_E) {
	      System.out.println("\n╔═══════════════════════════════════════════╗");
	      System.out.println("║  ENUM SWITCH BUG TEST                   ║");
	      System.out.println("╚═══════════════════════════════════════════╝\n");
	      
	      // Create a test indicator
	      dev.main.quest.QuestIndicator testIndicator = new dev.main.quest.QuestIndicator(-40);
	      
	      // Test 1: AVAILABLE
	      System.out.println("Test 1: Setting type to AVAILABLE");
	      testIndicator.type = dev.main.quest.QuestIndicator.IndicatorType.AVAILABLE;
	      System.out.println("  type field = " + testIndicator.type);
	      System.out.println("  type.name() = " + testIndicator.type.name());
	      System.out.println("  type.ordinal() = " + testIndicator.type.ordinal());
	      
	      String symbol1 = testIndicator.getSymbol();
	      System.out.println("  getSymbol() returned: '" + symbol1 + "'");
	      System.out.println("  Expected: '!'");
	      System.out.println("  Match: " + "!".equals(symbol1));
	      
	      // Test 2: IN_PROGRESS
	      System.out.println("\nTest 2: Setting type to IN_PROGRESS");
	      testIndicator.type = dev.main.quest.QuestIndicator.IndicatorType.IN_PROGRESS;
	      System.out.println("  type field = " + testIndicator.type);
	      System.out.println("  type.name() = " + testIndicator.type.name());
	      System.out.println("  type.ordinal() = " + testIndicator.type.ordinal());
	      
	      String symbol2 = testIndicator.getSymbol();
	      System.out.println("  getSymbol() returned: '" + symbol2 + "'");
	      System.out.println("  Expected: '...'");
	      System.out.println("  Match: " + "...".equals(symbol2));
	      
	      // Test 3: COMPLETE
	      System.out.println("\nTest 3: Setting type to COMPLETE");
	      testIndicator.type = dev.main.quest.QuestIndicator.IndicatorType.COMPLETE;
	      System.out.println("  type field = " + testIndicator.type);
	      System.out.println("  type.name() = " + testIndicator.type.name());
	      System.out.println("  type.ordinal() = " + testIndicator.type.ordinal());
	      
	      String symbol3 = testIndicator.getSymbol();
	      System.out.println("  getSymbol() returned: '" + symbol3 + "'");
	      System.out.println("  Expected: '?'");
	      System.out.println("  Match: " + "?".equals(symbol3));
	      
	      // Test 4: Manual if-else test
	      System.out.println("\nTest 4: Manual if-else logic test");
	      testIndicator.type = dev.main.quest.QuestIndicator.IndicatorType.IN_PROGRESS;
	      
	      String manualSymbol;
	      if (testIndicator.type == dev.main.quest.QuestIndicator.IndicatorType.AVAILABLE) {
	          manualSymbol = "!";
	      } else if (testIndicator.type == dev.main.quest.QuestIndicator.IndicatorType.COMPLETE) {
	          manualSymbol = "?";
	      } else if (testIndicator.type == dev.main.quest.QuestIndicator.IndicatorType.IN_PROGRESS) {
	          manualSymbol = "...";
	      } else {
	          manualSymbol = "!";
	      }
	      
	      System.out.println("  Manual if-else returned: '" + manualSymbol + "'");
	      System.out.println("  getSymbol() returned: '" + testIndicator.getSymbol() + "'");
	      System.out.println("  Match: " + manualSymbol.equals(testIndicator.getSymbol()));
	      
	      // Summary
	      System.out.println("\n╔═══════════════════════════════════════════╗");
	      System.out.println("║  RESULTS                                ║");
	      System.out.println("╠═══════════════════════════════════════════╣");
	      System.out.println("║  AVAILABLE:    " + ("!".equals(symbol1) ? "✓ PASS" : "✗ FAIL") + "                    ║");
	      System.out.println("║  IN_PROGRESS:  " + ("...".equals(symbol2) ? "✓ PASS" : "✗ FAIL") + "                    ║");
	      System.out.println("║  COMPLETE:     " + ("?".equals(symbol3) ? "✓ PASS" : "✗ FAIL") + "                    ║");
	      System.out.println("║  Manual match: " + (manualSymbol.equals(testIndicator.getSymbol()) ? "✓ PASS" : "✗ FAIL") + "                    ║");
	      System.out.println("╚═══════════════════════════════════════════╝\n");
	      
	      if ("...".equals(symbol2)) {
	          System.out.println("✓ getSymbol() works correctly!");
	          System.out.println("  The bug is NOT in the enum switch.");
	          System.out.println("  Check IntroQuestHandler or Renderer instead.");
	      } else {
	          System.out.println("✗ getSymbol() is broken!");
	          System.out.println("  The enum switch is not working.");
	          System.out.println("  This is the root cause of your problem.");
	          System.out.println("\nSOLUTION: Replace switch with if-else in getSymbol():");
	          System.out.println("  Use the refactored QuestIndicator.java from artifacts.");
	      }
	  }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shiftPressed = false;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}

    public void render() {
        Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
        
        // Clear screen
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);
         
        // Render game world
        renderer.render(g);
  
        g.dispose();
        bufferStrategy.show();
    }
         
    @Override
    public void run() {
        final double timePerUpdate = 1_000_000_000.0 / UPS;
        final double timePerFrame = 1_000_000_000.0 / FPS;

        long previousTime = System.nanoTime();
        double deltaU = 0;
        double deltaF = 0;

        long timer = System.currentTimeMillis();
        int frames = 0;
        int updates = 0;

        while (isRunning) {
            long currentTime = System.nanoTime();
            long elapsed = currentTime - previousTime;
            previousTime = currentTime;

            deltaU += elapsed / timePerUpdate;
            deltaF += elapsed / timePerFrame;

            while (deltaU >= 1) { 
                update(1f / UPS);
                updates++;
                deltaU--;
            }

            if (deltaF >= 1) {
                render();
                frames++;
                deltaF--;
            }

            boolean currentPressed = mouse.isPressed();
            if (mouse.hasMoved() || currentPressed != lastMousePressed) {
                gameState.getUIManager().handleMouseMove(mouse.getX(), mouse.getY(), currentPressed);
                mouse.resetMoved();
            }
            lastMousePressed = currentPressed;

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (System.currentTimeMillis() - timer >= 1000) {
                timer += 1000;
                //System.out.println("FPS: " + frames + " | UPS: " + updates);
                frames = 0;
                updates = 0;
            }
        }
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        if (isRunning) {
            isRunning = false;
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Getters
    public int getWidth() { return WIDTH; }
    public int getHeight() { return HEIGHT; }
    public MouseInput getMouse() { return mouse; }
    public GameState getGameState() { return gameState; }
    public static Engine getInstance() { return instance; }
	 // ════════════════════════════════════════════════════════════════════════
	 // Also add getter to Engine.java so Renderer can access the debug flag:
	 // ════════════════════════════════════════════════════════════════════════

	 public boolean isRendererDebugMode() {
	     return rendererDebugMode;
	 }
    public static void main(String[] args) {
        new Engine().start();
    }
}