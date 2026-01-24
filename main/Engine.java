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
    	/*if (e.getKeyCode() == KeyEvent.VK_R) {
    	    rendererDebugMode = !rendererDebugMode;
    	    System.out.println("Renderer debug mode: " + (rendererDebugMode ? "ON" : "OFF"));
    	    
    	    if (rendererDebugMode) {
    	        System.out.println("\nRenderer will now print indicator state EVERY FRAME.");
    	        System.out.println("Watch the console to see what the renderer is reading.");
    	        System.out.println("Press 'R' again to turn off.\n");
    	    }
    	}
    	*/
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