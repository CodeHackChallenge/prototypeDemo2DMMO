package dev.main.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.main.Engine;
import dev.main.dialogue.DialogueDatabase;
import dev.main.dialogue.DialogueExamples;
import dev.main.drops.DropItem;
import dev.main.drops.DropRarity;
import dev.main.drops.ZoneLootConfig;
import dev.main.entity.Entity;
import dev.main.entity.EntityFactory;
import dev.main.entity.EntityType;
import dev.main.entity.MobTier;
import dev.main.entity.MonsterLevel;
import dev.main.entity.NameTag;
import dev.main.entity.Respawn;
import dev.main.entity.SpawnPoint;
import dev.main.input.Position;
import dev.main.item.ItemManager;
import dev.main.pathfinder.Pathfinder;
import dev.main.quest.IntroQuestHandler;  // ★ NEW IMPORT
import dev.main.stats.Stats;
import dev.main.tile.TileMap;
import dev.main.ui.TransitionEffect;
import dev.main.ui.UIManager;
import dev.main.util.DamageText;
import dev.main.util.MapData;

import java.util.Iterator;

public class GameState {
	
	// ★ ADD: Current map tracking
    private String currentMapId;  
    private TileMap map;
    // ★ ADD: Portal cooldown (prevent spam teleporting)
    private float portalCooldown = 0f;
    private static final float PORTAL_COOLDOWN_TIME = 2.0f;
    
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
    
    private ZoneLootConfig zoneLootConfig;
    // ★ NEW: Transition effect
    private TransitionEffect transitionEffect;
    
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
        
        // ★ NEW: Initialize transition effect
        transitionEffect = new TransitionEffect();
        
        // ★ Load initial map
        loadMap("intro_map");
        /*
        // ★ OPTION 1: Load from JSON
        if(Engine.IDE == Engine.Eclipse) {
            map = new TileMap("/maps/intro_map.json");
        } else if(Engine.IDE == Engine.VSCode) {
            map = new TileMap("resources/maps/intro_map.json");
        }
        
        // ★ OPTION 2: Load map from .txt
        if(Engine.IDE == Engine.Eclipse)
        	map = new TileMap("/maps/intro_map.png", "/maps/fionnes_introMap01.txt");
        else if(Engine.IDE == Engine.VSCode)
        	map = new TileMap("resources/maps/intro_map.png", "resources/maps/fionnes_introMap01.txt");
        */
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
    // ★ NEW: Start portal transition
    public void startPortalTransition(String targetMap, int targetX, int targetY) {
        System.out.println("🌀 Starting portal transition to " + targetMap);
        
        transitionEffect.startPortalTransition(
            // OnLoadPoint - called when screen is black
            () -> {
                changeMapImmediate(targetMap, targetX, targetY);
            },
            // OnComplete - called when fade-in finishes
            () -> {
                System.out.println("✓ Portal transition complete!");
            }
        );
    }
    // ★ NEW: Load map by ID
    public void loadMap(String mapId) {
        this.currentMapId = mapId;
        
        String jsonPath;
        if(Engine.IDE == Engine.Eclipse) {
            jsonPath = "/maps/" + mapId + ".json";
        } else {
            jsonPath = "resources/maps/" + mapId + ".json";
        }
        
        map = new TileMap(jsonPath);
        System.out.println("Loaded map: " + mapId);
        // ★ NEW: Load zone loot config
        loadZoneLootConfig();
        // Create pathfinder for new map
        if (pathfinder != null) {
            pathfinder = new Pathfinder(map);
        } 
        
    }
    // ★ RENAMED: Old changeMap() is now changeMapImmediate()
    private void changeMapImmediate(String newMapId, int targetTileX, int targetTileY) {
        System.out.println("Changing map: " + currentMapId + " → " + newMapId);
        
        clearMapEntities();
        loadMap(newMapId);
        
        Entity player = getPlayer();
        Position playerPos = player.getComponent(Position.class);
        if (playerPos != null) {
            playerPos.x = targetTileX * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
            playerPos.y = targetTileY * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
        }
        
        snapCameraToPlayer();
        initializeWorld();
        
        System.out.println("Map change complete! Player at (" + targetTileX + ", " + targetTileY + ")");
    }
    // ★ NEW: Update transition effect
    public void updateTransition(float delta) {
        if (transitionEffect != null) {
            transitionEffect.update(delta);
        }
    }
    // ★ NEW: Check if inputs should be blocked
    public boolean isInputBlocked() {
        return transitionEffect != null && transitionEffect.isActive();
    }
    // ★ NEW: Get transition effect for rendering
    public TransitionEffect getTransitionEffect() {
        return transitionEffect;
    }
    
    // ★ NEW: Instantly snap camera to player (no lerp)
    private void snapCameraToPlayer() {
        Entity player = getPlayer();
        Position playerPos = player.getComponent(Position.class);
        TileMap map = getMap();
        
        if (playerPos != null && map != null) {
            float targetX = playerPos.x - Engine.WIDTH / 2;
            float targetY = playerPos.y - Engine.HEIGHT / 2;
            
            // Clamp to map bounds
            float maxCameraX = map.getWidthInPixels() - Engine.WIDTH;
            float maxCameraY = map.getHeightInPixels() - Engine.HEIGHT;
            
            targetX = Math.max(0, Math.min(targetX, maxCameraX));
            targetY = Math.max(0, Math.min(targetY, maxCameraY));
            
            // Instant snap
            setCameraPosition(targetX, targetY);
            
            System.out.println("📹 Camera snapped to (" + (int)targetX + ", " + (int)targetY + ")");
        }
    }

    // ★ NEW: Clear all entities except player
    private void clearMapEntities() {
        List<Entity> toRemove = new ArrayList<>();
        
        for (Entity entity : entities) {
            if (entity.getType() != EntityType.PLAYER) {
                toRemove.add(entity);
            }
        }
        
        entities.removeAll(toRemove);
        spawnPoints.clear();
        damageTexts.clear();
        
        System.out.println("Cleared " + toRemove.size() + " entities from old map");
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
    	 // Only create player if it doesn't exist
        if (player == null) {
            player = EntityFactory.createPlayer(8 * 64, 5 * 64);
            entities.add(player);
        }
        
        // ★ Load portals from map data
        loadPortalsFromMapData();
        // ★ Load spawns from map data
        loadSpawnsFromMapData();
          
     // Add NPCs (map-specific)
        if ("intro_map".equals(currentMapId)) {
            Entity fionne = EntityFactory.createFionne(14 * 64 - 32, 6 * 64 - 31);
            entities.add(fionne);
        } 
        
        // ★ OR keep manual spawns (your choice)
        /*
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
        
        // Initial spawn of all monsters
        for (SpawnPoint sp : spawnPoints) {
            spawnMonsterAtPoint(sp);
        }
          */ 
        
        // Add environment decorations (map-specific)
        if ("intro_map".equals(currentMapId)) {
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
        
        
    } 
    // ★ NEW: Load portals from map JSON
    private void loadPortalsFromMapData() {
        MapData data = map.getMapData();
        if (data == null || data.portals == null) {
            System.out.println("No portal data in map JSON");
            return;
        }
        
        System.out.println("Loading " + data.portals.size() + " portals from JSON...");
        
        for (MapData.Portal portalData : data.portals) {
            float worldX = portalData.x * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
            float worldY = portalData.y * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
            
            Entity portal = EntityFactory.createPortal(
                portalData.id,
                worldX,
                worldY,
                portalData.targetMap,
                portalData.targetX,
                portalData.targetY
            );
            
            entities.add(portal);
            
            System.out.println("  - " + portalData.id + " → " + portalData.targetMap + 
                             " (" + portalData.targetX + ", " + portalData.targetY + ")");
        }
    }
    // ★ NEW: Update portal cooldown
    public void updatePortalCooldown(float delta) {
        if (portalCooldown > 0) {
            portalCooldown -= delta;
        }
    }
    // ★ NEW: Check if portal is ready
    public boolean isPortalReady() {
        return portalCooldown <= 0;
    }
    
    // ★ NEW: Set portal cooldown
    public void setPortalCooldown() {
        this.portalCooldown = PORTAL_COOLDOWN_TIME;
    }
    
    // ★ NEW: Getters
    public String getCurrentMapId() {
        return currentMapId;
    }
    // ★ ADD THIS METHOD to load spawns from JSON
    private void loadSpawnsFromMapData() {
        MapData data = map.getMapData();
        if (data == null || data.monsterSpawns == null) {
            System.out.println("No spawn data in map JSON");
            return;
        }
        
        System.out.println("Loading " + data.monsterSpawns.size() + " spawn points from JSON...");
        
        for (MapData.MonsterSpawn spawn : data.monsterSpawns) {
            // Convert tier string to enum
            MobTier tier = MobTier.valueOf(spawn.tier.toUpperCase());
            
            addSpawnPoint(
                spawn.monsterType,
                spawn.x,
                spawn.y,
                spawn.respawnDelay,
                spawn.level,
                tier
            );
            
            // Initial spawn of all monsters
            for (SpawnPoint sp : spawnPoints) {
                spawnMonsterAtPoint(sp);
            }
            
            System.out.println("  - " + spawn.id + ": " + spawn.monsterType + 
                             " Lv" + spawn.level + " " + tier);
        }
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
    
 // Add to GameState.java

    /**
     * ★ NEW: Load zone loot configuration from map data
     */
    private void loadZoneLootConfig() {  
    	//System.out.println("::::: inside loadZoneLootConfig()");
        MapData data = map.getMapData();
       // System.out.println("::::: data="+data+" data.zoneLoot="+data.zoneLoot);
       
     // ★ DEBUG: Check what we got
        System.out.println("════════════════════════════════════════");
        System.out.println("DEBUG: Loading zone loot config");
        System.out.println("MapData: " + (data != null ? "EXISTS" : "NULL"));
        
        if (data != null) {
            System.out.println("MapId: " + data.mapId);
            System.out.println("ZoneLoot field: " + (data.zoneLoot != null ? "EXISTS" : "NULL"));
            
            if (data.zoneLoot != null) {
                System.out.println("Loot Tier: " + data.zoneLoot.lootTier);
                System.out.println("Rarity Multipliers: " + (data.zoneLoot.rarityMultipliers != null ? data.zoneLoot.rarityMultipliers.size() : "NULL"));
                System.out.println("Extra Drops: " + (data.zoneLoot.extraDrops != null ? data.zoneLoot.extraDrops.size() : "NULL"));
                System.out.println("Guaranteed Drops: " + (data.zoneLoot.guaranteedDrops != null ? data.zoneLoot.guaranteedDrops.size() : "NULL"));
            }
        }
        System.out.println("════════════════════════════════════════");
        
        if (data == null || data.zoneLoot == null) {
            System.out.println("No zone loot data in map");
            return;
        }
        
        if (data == null || data.zoneLoot == null) {
            System.out.println("No zone loot data in map");
            // Use default drop system
            return;
        }
        
        MapData.ZoneLootData zoneData = data.zoneLoot;
        
        // Create zone loot config
        ZoneLootConfig.LootTier tier = ZoneLootConfig.LootTier.valueOf(zoneData.lootTier);
        ZoneLootConfig config = new ZoneLootConfig(tier);
        
        // Set rarity multipliers
        if (zoneData.rarityMultipliers != null) {
            for (Map.Entry<String, Double> entry : zoneData.rarityMultipliers.entrySet()) {
                DropRarity rarity = DropRarity.valueOf(entry.getKey());
                config.setRarityMultiplier(rarity, entry.getValue());
            }
        }
        
        // Load extra drops
        if (zoneData.extraDrops != null) {
            for (MapData.ExtraDropData dropData : zoneData.extraDrops) {
                DropRarity rarity = DropRarity.valueOf(dropData.rarity);
                
                // Create item creator from string reference
                DropItem.ItemCreator creator = getItemCreatorFromString(dropData.itemCreator);
                
                DropItem dropItem = new DropItem(
                    dropData.itemName,
                    rarity,
                    dropData.minQuantity,
                    dropData.maxQuantity,
                    creator
                );
                
                config.addExtraDrop(new ZoneLootConfig.ZoneDropItem(dropItem, dropData.dropChance));
            }
        }
        
        // Load guaranteed drops
        if (zoneData.guaranteedDrops != null) {
            for (MapData.GuaranteedDropData dropData : zoneData.guaranteedDrops) {
                DropRarity rarity = DropRarity.valueOf(dropData.rarity);
                
                DropItem.ItemCreator creator = getItemCreatorFromString(dropData.itemCreator);
                
                DropItem dropItem = new DropItem(
                    dropData.itemName,
                    rarity,
                    dropData.quantity,
                    dropData.quantity,
                    creator
                );
                
                config.addGuaranteedDrop(new ZoneLootConfig.GuaranteedDrop(
                    dropData.questId,
                    dropItem,
                    dropData.quantity,
                    dropData.dropOnFirstKill
                ));
            }
        }
        
        // Set config in drop system (GameLogic has the drop system)
        // You'll need to pass this to GameLogic
        this.zoneLootConfig = config;
        
        System.out.println("Zone loot loaded: " + tier + " tier with " + 
                          config.getExtraDrops().size() + " extra drops, " +
                          config.getGuaranteedDrops().size() + " guaranteed drops");
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
    
    // Add getter
    public ZoneLootConfig getZoneLootConfig() {
        return zoneLootConfig;
    }
    
    /**
     * Helper to convert string item creator reference to ItemCreator
     */
    private DropItem.ItemCreator getItemCreatorFromString(String creatorRef) {
        // Parse "ItemManager::createWoodenTablet" format
        switch (creatorRef) {
            case "ItemManager::createWoodenTablet":
                return ItemManager::createWoodenTablet;
            case "ItemManager::createClay":
                return ItemManager::createClay;
            case "ItemManager::createCarvedWood":
                return ItemManager::createCarvedWood;
            case "ItemManager::createEssence":
                return ItemManager::createEssence;
            case "ItemManager::createAnimalClaws":
                return ItemManager::createAnimalClaws;
            case "ItemManager::createBrokenTooth":
                return ItemManager::createBrokenTooth;
            case "ItemManager::createRawFish":
                return ItemManager::createRawFish;
            case "ItemManager::createAnimalBone":
                return ItemManager::createAnimalBone;    
            case "ItemManager::createAnimalSkull":
                return ItemManager::createAnimalSkull; 
            case "ItemManager::createFruitBanana":
                return ItemManager::createFruitBanana; 
            case "ItemManager::createLuckyPouch":  
                return ItemManager::createLuckyPouch;    
            case "ItemManager::createScrollOfPurity":  
                return ItemManager::createScrollOfPurity; 
            case "ItemManager::createFireRune":  
                return ItemManager::createFireRune; 
                
            // Add more cases as needed
            default:
                System.out.println("⚠ Unknown item creator: " + creatorRef);
                return ItemManager::createWoodenTablet; // Fallback
        }
    }
}