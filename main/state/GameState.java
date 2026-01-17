package dev.main.state;

import java.util.ArrayList;
import java.util.List;

import dev.main.Engine;
import dev.main.dialogue.DialogueDatabase;
import dev.main.dialogue.DialogueExamples;
import dev.main.entity.Entity;
import dev.main.entity.EntityFactory;
import dev.main.entity.MobTier;
import dev.main.entity.MonsterLevel;
import dev.main.entity.NameTag;
import dev.main.entity.Respawn;
import dev.main.entity.SpawnPoint;
import dev.main.pathfinder.Pathfinder;
import dev.main.quest.IntroQuestHandler;  // ★ NEW IMPORT
import dev.main.stats.Stats;
import dev.main.tile.TileMap;
import dev.main.ui.UIManager;
import dev.main.util.DamageText;

import java.util.Iterator;

public class GameState {
    
    private TileMap map;
    private List<Entity> entities;
    private List<Entity> entitiesToRemove;
    private List<DamageText> damageTexts;
    private List<SpawnPoint> spawnPoints;
    
    private Entity player;
    private Entity hoveredEntity;
    private Entity targetedEntity;
    private Entity autoAttackTarget;
    private Pathfinder pathfinder;
    
    // UI
    private UIManager uiManager;
    
    // ★ NEW: Intro quest handler
    private IntroQuestHandler introQuestHandler;
    
    private float gameTime;
    private float cameraX;
    private float cameraY;
    
    public GameState() {
        entities = new ArrayList<>();
        entitiesToRemove = new ArrayList<>();
        damageTexts = new ArrayList<>();
        spawnPoints = new ArrayList<>();
        
        gameTime = 0f;
        cameraX = 0f;
        cameraY = 0f;
        
        // Load map
        if(Engine.IDE == Engine.Eclipse)
        	map = new TileMap("/maps/env_fionne_map.png", "/maps/fionnes_introMap01.txt");
        else if(Engine.IDE == Engine.VSCode)
        	map = new TileMap("resources/maps/env_fionne_map.png", "resources/maps/fionnes_introMap01.txt");
        
        pathfinder = new Pathfinder(map);
        
        initializeWorld();
        
        // Create UI Manager (GameLogic will be set later)
        uiManager = new UIManager(this);
        
        // ★ NEW: Initialize intro quest handler AFTER UIManager
        introQuestHandler = new IntroQuestHandler(this);
   
        //test items
        //uiManager.addTestGearItems();
        
        // Initialize dialogue system
        initializeDialogueSystem();
    }
   
    private void initializeDialogueSystem() {
        DialogueDatabase db = DialogueDatabase.getInstance();
        
        // Load all dialogue files
        db.loadAllDialogues("/dialogues/");
        
        // Create programmatic dialogues
        DialogueExamples.createSimpleGreeting();
        
        // Map NPCs to their dialogues
        db.mapNPCToDialogue("fionne", "fionne_intro");
        
        System.out.println("Dialogue system initialized");
    }
    
    /**
     * Set game logic reference for UI Manager
     * Call this from Engine after creating GameLogic
     */
    public void setGameLogic(GameLogic gameLogic) {
        uiManager.setGameLogic(gameLogic);
    }
    
    private void initializeWorld() {
        // Create player
        player = EntityFactory.createPlayer(8 * 64, 5 * 64);
        entities.add(player);
        
        // Create Fionne NPC
        Entity fionne = EntityFactory.createFionne(14 * 64 - 32, 6 * 64 - 31);
        entities.add(fionne);
        System.out.println("Fionne NPC created at (13, 5)");
       
        
        float normalRespawn = 30f;
        float bossRespawn = 50f;
        
        //addSpawnPoint("Orc", 1 * 64, 2 * 64, normalRespawn, 1, MobTier.NORMAL);
        
      
        //GHOST
        //addSpawnPoint("Ghost", 2 * 64, 2 * 64, normalRespawn, 1, MobTier.TRASH);
        //addSpawnPoint("Ghost", 3* 64, 6 * 64, normalRespawn, 1, MobTier.TRASH);
        //addSpawnPoint("Ghost", 5 * 64, 7 * 64, normalRespawn, 1, MobTier.TRASH);
        //addSpawnPoint("Ghost", 7 * 64, 7 * 64, normalRespawn, 1, MobTier.TRASH);
         
        // TRASH tier goblins (weak, low XP)
        addSpawnPoint("Goblin", 10 * 64, 10 * 64, normalRespawn, 1, MobTier.TRASH);
        addSpawnPoint("Goblin", 11 * 64, 10 * 64, normalRespawn, 1, MobTier.TRASH);
        addSpawnPoint("Goblin", 5 * 64, 6 * 64, normalRespawn, 1, MobTier.TRASH);
        addSpawnPoint("Goblin", 7 * 64, 10 * 64, normalRespawn, 1, MobTier.TRASH);
        addSpawnPoint("Goblin", 8 * 64, 10 * 64, normalRespawn, 1, MobTier.TRASH);
        addSpawnPoint("Goblin", 8 * 64, 9 * 64, normalRespawn, 1, MobTier.TRASH);
        addSpawnPoint("Goblin", 9 * 64, 2 * 64, normalRespawn, 1, MobTier.TRASH);
        addSpawnPoint("Goblin", 9 * 64, 5 * 64, normalRespawn, 1, MobTier.TRASH); 
        
        // NORMAL tier goblins
        addSpawnPoint("Goblin", 12 * 64, 10 * 64, normalRespawn, 2, MobTier.NORMAL);
        addSpawnPoint("Goblin", 13 * 64, 10 * 64, normalRespawn, 2, MobTier.NORMAL);
        addSpawnPoint("Goblin", 12 * 64, 2 * 64, normalRespawn, 2, MobTier.NORMAL);
        addSpawnPoint("Goblin", 13 * 64, 2 * 64, normalRespawn, 2, MobTier.NORMAL);
        addSpawnPoint("Goblin", 12 * 64, 3 * 64, normalRespawn, 2, MobTier.NORMAL);
        addSpawnPoint("Goblin", 14 * 64, 10 * 64, normalRespawn, 2, MobTier.NORMAL);
        
        // ELITE goblin
        addSpawnPoint("Goblin", 14 * 64, 10 * 64, normalRespawn, 3, MobTier.ELITE);
        /*
        // NORMAL bunnies
        addSpawnPoint("Bunny", 20 * 64, 20 * 64, normalRespawn, 1, MobTier.NORMAL);
        addSpawnPoint("Bunny", 20 * 64, 21 * 64, normalRespawn, 2, MobTier.NORMAL);
        addSpawnPoint("Bunny", 21 * 64, 22 * 64, normalRespawn, 2, MobTier.NORMAL);
        addSpawnPoint("Bunny", 21 * 64, 23 * 64, normalRespawn, 3, MobTier.NORMAL);
        addSpawnPoint("Bunny", 20 * 64, 20 * 64, normalRespawn, 1, MobTier.NORMAL);
        addSpawnPoint("Bunny", 20 * 64, 22 * 64, normalRespawn, 2, MobTier.NORMAL);
        addSpawnPoint("Bunny", 22 * 64, 22 * 64, normalRespawn, 2, MobTier.NORMAL);
        addSpawnPoint("Bunny", 23 * 64, 23 * 64, normalRespawn, 3, MobTier.NORMAL);
        
        // MINIBOSS spawns
        addSpawnPoint("GoblinBoss", 13 * 64, 12 * 64, bossRespawn, 5, MobTier.MINIBOSS);
        addSpawnPoint("BunnyBoss", 22 * 64, 23 * 64, bossRespawn, 5, MobTier.MINIBOSS);
        addSpawnPoint("MinotaurBoss", 22 * 64, 21 * 64, bossRespawn, 7, MobTier.MINIBOSS);
         */
        // Initial spawn of all monsters
        for (SpawnPoint sp : spawnPoints) {
            spawnMonsterAtPoint(sp);
        }
          
        //add boulder
        addBoulder(4 * 64 - 13,  3 * 64 - 18);
        // User-requested tree at tile (5,5)
        addTree(2 * 64 - 30, 1 * 64 - 25, "right");
        addTree(1 * 64 - 32, 8 * 64 - 66, "right");
        addTree(3 * 64 - 6 , 14 * 64 -10  , "right");
        //
        addTree(19 * 64 - 6 , 0 * 64 - 10 , "right");
        addTree(20 * 64 - 6 , 6 * 64 - 10 , "right");
        addTree(18 * 64 - 6 , 12 * 64 - 10 , "right");
        addTree(13 * 64 - 6 , 13 * 64 - 10 , "right");
        //fountain
        addFountain(15 * 64 - 6 , 4 * 64 - 10 );
        
    } 
    
    private void addFountain(float x, float y) {
        Entity fountain = EntityFactory.createFountain(x, y);
        entities.add(fountain);
        System.out.println("Added fountain at (" + (int)x + ", " + (int)y + ")");
    }

    private void addBoulder(float x, float y) {
        Entity boulder = EntityFactory.createBoulder(x, y);
        entities.add(boulder);
        System.out.println("Added boulder at (" + (int)x + ", " + (int)y + ")");
    }

    private void addTree(float x, float y, String orientation) {
        Entity tree = EntityFactory.createTree(x, y, orientation);
        entities.add(tree);
        System.out.println("Added tree at (" + (int)x + ", " + (int)y + ")");
    }
    
    public void addSpawnPoint(String monsterType, float x, float y, float respawnDelay, int level, MobTier tier) {
        SpawnPoint sp = new SpawnPoint(monsterType, x, y, respawnDelay, level, tier);
        spawnPoints.add(sp);
        System.out.println("Added spawn point: " + sp);
    }
    
    public void updateSpawnPoints(float delta) {
        for (SpawnPoint sp : spawnPoints) {
            sp.update(delta);
            
            if (sp.canRespawn()) {
                spawnMonsterAtPoint(sp);
            }
        }
    }
    
    public void onMonsterDeath(Entity monster) {
        Respawn respawn = monster.getComponent(Respawn.class);
        if (respawn != null) {
            for (SpawnPoint sp : spawnPoints) {
                if (sp.currentMonster == monster) {
                    sp.onMonsterDeath();
                    break;
                }
            }
        }
    }
    
    public List<SpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }
    
    public void spawnMonsterAtPoint(SpawnPoint spawnPoint) {
        if (spawnPoint.isOccupied) {
            return;
        }
        
        Entity monster = EntityFactory.createMonster(
            spawnPoint.monsterType, 
            spawnPoint.x, 
            spawnPoint.y,
            spawnPoint.level,
            spawnPoint.tier
        );
        
        monster.addComponent(new Respawn(
            spawnPoint.monsterType, 
            spawnPoint.x, 
            spawnPoint.y, 
            spawnPoint.respawnDelay
        ));
        
        entities.add(monster);
        spawnPoint.spawn(monster);
        
        MonsterLevel monsterLevel = monster.getComponent(MonsterLevel.class);
        Stats stats = monster.getComponent(Stats.class);
        
        System.out.println("Spawned " + spawnPoint.monsterType + 
                         " Lv" + spawnPoint.level + " " + spawnPoint.tier +
                         " at (" + (int)spawnPoint.x + ", " + (int)spawnPoint.y + ")" +
                         " - HP:" + stats.maxHp + " ATK:" + stats.attack + 
                         " DEF:" + stats.defense + " ACC:" + stats.accuracy + 
                         " EVA:" + stats.evasion);
    }
    
    public void spawnMonster(String type, float x, float y) {
        spawnMonster(type, x, y, 1, MobTier.NORMAL);
    }
    
    public void spawnMonster(String type, float x, float y, int level, MobTier tier) {
        Entity monster = EntityFactory.createMonster(type, x, y, level, tier);
        entities.add(monster);
        System.out.println("Spawned " + type + " Lv" + level + " " + tier + " at (" + x + ", " + y + ")");
    }
    
    public Entity getAutoAttackTarget() {
        return autoAttackTarget;
    }
    
    public void setAutoAttackTarget(Entity target) {
        this.autoAttackTarget = target;
    }
    
    public void clearAutoAttackTarget() {
        this.autoAttackTarget = null;
    }
    
    public void addDamageText(DamageText text) {
        damageTexts.add(text);
    }
    
    public List<DamageText> getDamageTexts() {
        return damageTexts;
    }
    
    public void updateDamageTexts(float delta) {
        Iterator<DamageText> it = damageTexts.iterator();
        while (it.hasNext()) {
            DamageText dt = it.next();
            dt.update(delta);
            if (dt.shouldRemove()) {
                it.remove();
            }
        }
    }
    
    public Entity getHoveredEntity() {
        return hoveredEntity;
    }
    
    public void setHoveredEntity(Entity entity) {
        if (hoveredEntity != null && hoveredEntity != targetedEntity) {
            NameTag tag = hoveredEntity.getComponent(NameTag.class);
            if (tag != null) tag.hide();
        }
        
        this.hoveredEntity = entity;
        
        if (hoveredEntity != null) {
            NameTag tag = hoveredEntity.getComponent(NameTag.class);
            if (tag != null) tag.show();
        }
    }
    
    public Entity getTargetedEntity() {
        return targetedEntity;
    }
    
    public void setTargetedEntity(Entity entity) {
        if (targetedEntity != null && targetedEntity != hoveredEntity) {
            NameTag tag = targetedEntity.getComponent(NameTag.class);
            if (tag != null) tag.hide();
        }
        
        this.targetedEntity = entity;
        
        if (targetedEntity != null) {
            NameTag tag = targetedEntity.getComponent(NameTag.class);
            if (tag != null) tag.show();
        }
    }
    
    public void markForRemoval(Entity entity) {
        if (!entitiesToRemove.contains(entity)) {
            entitiesToRemove.add(entity);
        }
    }
    
    public void removeMarkedEntities() {
        for (Entity entity : entitiesToRemove) {
            entities.remove(entity);
            System.out.println("Removed " + entity.getName());
        }
        entitiesToRemove.clear();
    }
    
    public List<Entity> getEntities() {
        return entities;
    }
    
    public Entity getPlayer() {
        return player;
    }
    
    public TileMap getMap() {
        return map;
    }
    
    public Pathfinder getPathfinder() {
        return pathfinder;
    }
    
    public float getGameTime() {
        return gameTime;
    }
    
    public void incrementGameTime(float delta) {
        gameTime += delta;
    }
    
    public float getCameraX() {
        return cameraX;
    }
    
    public float getCameraY() {
        return cameraY;
    }
    
    public void setCameraPosition(float x, float y) {
        this.cameraX = x;
        this.cameraY = y;
    }
    
    public UIManager getUIManager() {
        return uiManager;
    }
    
    // ★ NEW: Getter for intro quest handler
    public IntroQuestHandler getIntroQuestHandler() {
        return introQuestHandler;
    }
}