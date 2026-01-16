package dev.main.entity;

import dev.main.Engine;
import dev.main.ai.AI;
import dev.main.bars.HealthBar;
import dev.main.bars.ManaBar;
import dev.main.bars.StaminaBar;
import dev.main.buffs.BuffManager;
import dev.main.input.CollisionBox;
import dev.main.input.Movement;
import dev.main.input.Position;
import dev.main.pathfinder.Path;
import dev.main.quest.QuestIndicator;
import dev.main.quest.QuestLog;
import dev.main.quest.QuestObjective;
import dev.main.render.RenderLayer;
import dev.main.render.Renderable;
import dev.main.skill.SkillLevel;
import dev.main.sprite.Sprite;
import dev.main.stats.MobStatFactory;
import dev.main.stats.MobStats;
import dev.main.stats.Stats;
import dev.main.ui.Quest;
import dev.main.util.Alert;
import dev.main.util.XPBar; 

public class EntityFactory { 
    /**
     * Create a player entity with level system
     */
	public static Entity createPlayer(float x, float y) {
	    Entity player = new Entity("Player", EntityType.PLAYER);
	    
	    player.addComponent(new Position(x, y));
	    
	    if(Engine.IDE == Engine.Eclipse)
	    	player.addComponent(new Sprite("/sprites/hero2.png", 64, 64, 0.2f)); //eclipse
        else if(Engine.IDE == Engine.VSCode)
        	player.addComponent(new Sprite("resources/sprites/hero2.png", 64, 64, 0.2f));
         
	    player.addComponent(new Movement(100f, 200f));
	    
	    // Base stats
	    Stats stats = new Stats(1000, 100, 2, 0, 50);
	    player.addComponent(stats);
	    
	    // Experience and leveling
	    Experience exp = new Experience();
	    exp.hpGrowth = 10;
	    exp.attackGrowth = 2;
	    exp.defenseGrowth = 1;
	    exp.accGrowth = 1;
	    exp.manaGrowth = 5;
	    player.addComponent(exp);
	    
	    // Skill system
	    SkillLevel skillLevel = new SkillLevel();
	    player.addComponent(skillLevel);
	    
	    // ⭐ NEW: Quest log
	    QuestLog questLog = new QuestLog();
	    player.addComponent(questLog);
	    
	    // Apply level 1 stats
	    stats.applyLevelStats(exp);
	    
	    // Combat system
	    player.addComponent(new Combat(1.1f, 0.15f, 0.05f));
	    
	    // UI Components
	    player.addComponent(new HealthBar(40, 4, 40));
	    player.addComponent(new StaminaBar(40, 4, 46));
	    player.addComponent(new ManaBar(40, 4, 50));
	    player.addComponent(new XPBar(40, 3, 54));
	    
	    // Collision and movement
	    player.addComponent(new CollisionBox(-10, -14, 22, 44));
	    player.addComponent(new Path());
	    player.addComponent(new TargetIndicator());
	    
	    // Rendering
	    player.addComponent(new Renderable(RenderLayer.ENTITIES));
	    
	    // Level-up effect
	    player.addComponent(new LevelUpEffect());
	    
	    //buff
	    player.addComponent(new BuffManager());
	    
	    return player;
	}
    /**
     * NEW: Create monster with level and tier scaling
     */
    public static Entity createMonster(String monsterType, float x, float y, int level, MobTier tier) {
        Entity monster = new Entity(monsterType, EntityType.MONSTER);
        
        monster.addComponent(new Position(x, y));
        
        // Calculate stats based on level and tier
        MobStats mobStats = MobStatFactory.create(level, tier);
        
        // Add MonsterLevel component for XP calculation
        monster.addComponent(new MonsterLevel(level, tier));
        
        // Create Stats component with calculated values
        Stats stats = new Stats(mobStats.hp, 50f, mobStats.attack, mobStats.defense);
        stats.accuracy = mobStats.accuracy;
        stats.evasion = mobStats.evasion;
        monster.addComponent(stats);
        
        // Configure monster type-specific properties
        switch(monsterType) {
            case "Goblin":
            	if(Engine.IDE == Engine.Eclipse)
            		monster.addComponent(new Sprite("/sprites/goblin.png", 64, 64, 0.15f)); // eclipse
                else if(Engine.IDE == Engine.VSCode)
                	monster.addComponent(new Sprite("resources/sprites/goblin.png", 64, 64, 0.15f)); //VS Code 
                
                monster.addComponent(new Combat(1.2f, 0.10f, 0.08f));
                monster.addComponent(new Movement(80f, 160f));
                monster.addComponent(new CollisionBox(-10, -14, 22, 44)); 
                monster.addComponent(new AI("passive", x, y, 500f, 4f));
                monster.addComponent(new NameTag("Goblin", -20));
                monster.addComponent(new Alert(-40));
                monster.addComponent(new Renderable(RenderLayer.ENTITIES));
                break;
                
            case "GoblinBoss":
            	if(Engine.IDE == Engine.Eclipse)
            		monster.addComponent(new Sprite("/sprites/goblin_dark.png", 64, 64, 0.12f)); // eclipse
                else if(Engine.IDE == Engine.VSCode)
                    monster.addComponent(new Sprite("resources/sprites/goblin_dark.png", 64, 64, 0.12f)); //VS code
                
                monster.addComponent(new Combat(1.0f, 0.20f, 0.05f));
                monster.addComponent(new Movement(100f, 200f));
                monster.addComponent(new CollisionBox(-10, -14, 22, 44));
                monster.addComponent(new AI("aggressive", x, y, 250f, 6f));
                monster.addComponent(new NameTag("Goblin Boss", -20));
                monster.addComponent(new Alert(-40));
                monster.addComponent(new Renderable(RenderLayer.ENTITIES, 10));
                break;
                
            case "BunnyBoss":
            	if(Engine.IDE == Engine.Eclipse)
            		monster.addComponent(new Sprite("/sprites/bunny_boss.png", 64, 64, 0.12f)); // eclipse
                else if(Engine.IDE == Engine.VSCode)
                	monster.addComponent(new Sprite("resources/sprites/bunny_boss.png", 64, 64, 0.12f)); //vs code
                
            	
           
                monster.addComponent(new Combat(1.0f, 0.20f, 0.05f));
                monster.addComponent(new Movement(100f, 200f));
                monster.addComponent(new CollisionBox(-10, -14, 22, 44));
                monster.addComponent(new AI("aggressive", x, y, 500f, 6f));
                monster.addComponent(new NameTag("Bunny Boss", -20));
                monster.addComponent(new Alert(-40));
                monster.addComponent(new Renderable(RenderLayer.ENTITIES, 10));
                break;
                
            case "MinotaurBoss":
            	if(Engine.IDE == Engine.Eclipse)
            		monster.addComponent(new Sprite("/sprites/minotaur_boss.png", 64, 64, 0.12f)); // eclipse
                else if(Engine.IDE == Engine.VSCode)
                	monster.addComponent(new Sprite("resources/sprites/minotaur_boss.png", 64, 64, 0.12f)); //vs code
                
                 
                monster.addComponent(new Combat(1.0f, 0.20f, 0.05f));
                monster.addComponent(new Movement(100f, 200f));
                monster.addComponent(new CollisionBox(-10, -14, 22, 44));
                monster.addComponent(new AI("aggressive", x, y, 250f, 6f));
                monster.addComponent(new NameTag("Minotaur Boss", -20));
                monster.addComponent(new Alert(-40));
                monster.addComponent(new Renderable(RenderLayer.ENTITIES, 10));
                break;
                
            case "Bunny":
            	if(Engine.IDE == Engine.Eclipse)
            		monster.addComponent(new Sprite("/sprites/bunny.png", 64, 64, 0.15f)); // eclipse
                else if(Engine.IDE == Engine.VSCode)
                	monster.addComponent(new Sprite("resources/sprites/bunny.png", 64, 64, 0.15f)); //vs code
               
                monster.addComponent(new Combat(1.2f, 0.10f, 0.08f));
                monster.addComponent(new Movement(80f, 160f));
                monster.addComponent(new CollisionBox(-10, -14, 22, 44));
                monster.addComponent(new AI("passive", x, y, 500f, 4f));
                monster.addComponent(new NameTag("Bunny", -20));
                monster.addComponent(new Alert(-40));
                monster.addComponent(new Renderable(RenderLayer.ENTITIES));
                break;
                
                //me
            case "Ghost":
            	if(Engine.IDE == Engine.Eclipse)
            		monster.addComponent(new Sprite("/sprites/ghost.png", 64, 64, 0.15f)); // eclipse
                else if(Engine.IDE == Engine.VSCode)
                	monster.addComponent(new Sprite("resources/sprites/ghost.png", 64, 64, 0.15f)); //vs code
              
                monster.addComponent(new Combat(1.2f, 0.10f, 0.08f));
                monster.addComponent(new Movement(80f, 160f));
                monster.addComponent(new CollisionBox(-17, -14, 36, 42));
                monster.addComponent(new AI("passive", x, y, 500f, 4f));
                monster.addComponent(new NameTag("Ghost", -20));
                monster.addComponent(new Alert(-40));
                monster.addComponent(new Renderable(RenderLayer.ENTITIES));
                break;
                //me
            case "Orc":
            	if(Engine.IDE == Engine.Eclipse)
            		monster.addComponent(new Sprite("/sprites/ro_orc.png", 95, 129, 0.15f)); // eclipse
                else if(Engine.IDE == Engine.VSCode)
                	monster.addComponent(new Sprite("resources/sprites/ro_orc.png", 95, 129, 0.15f)); //vs code
                  
                monster.addComponent(new Combat(1.2f, 0.10f, 0.08f));
                monster.addComponent(new Movement(80f, 160f));
                monster.addComponent(new CollisionBox(-17, -14, 36, 42));
                monster.addComponent(new AI("passive", x, y, 500f, 4f));
                monster.addComponent(new NameTag("Orc", -20));
                monster.addComponent(new Alert(-40));
                monster.addComponent(new Renderable(RenderLayer.ENTITIES));
                break;
            default:
                // Default monster setup
            	if(Engine.IDE == Engine.Eclipse)
            		monster.addComponent(new Sprite("/sprites/goblin.png", 64, 64, 0.15f)); // eclipse 
                else if(Engine.IDE == Engine.VSCode)
                	monster.addComponent(new Sprite("resources/sprites/goblin.png", 64, 64, 0.15f)); //VS Code
                
                 
                monster.addComponent(new Combat(1.5f, 0.05f, 0.08f));
                monster.addComponent(new Movement(50f, 100f));
                monster.addComponent(new CollisionBox(-12, -12, 24, 24));
                monster.addComponent(new AI("neutral", x, y, 128f, 2f));
                monster.addComponent(new NameTag("Monster", -45));
                monster.addComponent(new Renderable(RenderLayer.ENTITIES));
                break;
        }
        
        monster.addComponent(new HealthBar(40, 4, 40));
        monster.addComponent(new Path());
        
        return monster;
    }
    
    /**
     * OLD: Backwards compatibility - creates level 1 NORMAL tier monster
     */
    public static Entity createMonster(String monsterType, float x, float y) {
        return createMonster(monsterType, x, y, 1, MobTier.NORMAL);
    }
    
 // Add this method to EntityFactory.java

    /**
     * Create NPC entity with dialogue and quests
     */
    public static Entity createNPC(String npcId, String npcName, float x, float y) {
        Entity npc = new Entity(npcName, EntityType.NPC);
        
        npc.addComponent(new Position(x, y));
        
        // Use a simple sprite for NPCs (can be customized later)
        if(Engine.IDE == Engine.Eclipse) {
        	npc.addComponent(new Sprite("/sprites/npc_left_" + npcId + ".png", 64, 64, 0f));
        }
        	
        else if(Engine.IDE == Engine.VSCode) {
        	npc.addComponent(new Sprite("resources/sprites/npc_left_" + npcId + ".png", 64, 64, 0f)); 
        }
        	
   
        
          
        // NPCs don't move
        // No Movement component needed
        
        // Add NPC component
        NPC npcComponent = new NPC(npcId, npcName, NPC.NPCType.QUEST_GIVER);
        npc.addComponent(npcComponent);
        
        // Add name tag (always visible)
        NameTag nameTag = new NameTag(npcName, -22);
        nameTag.show();  // Always show NPC names
        //nameTag.hide(); 
        npc.addComponent(nameTag);
        
        // Add collision box (so player can't walk through NPC)
        npc.addComponent(new CollisionBox(-10, -14, 22, 44));
        
        // Add rendering component
        npc.addComponent(new Renderable(RenderLayer.ENTITIES));
        
        // Add quest indicator (exclamation mark when quest available)
        npc.addComponent(new QuestIndicator(-40));
        
        return npc;
    }

    /**
     * Create Fionne NPC with starter quest
     */
    public static Entity createFionne(float x, float y) {
        Entity fionne = createNPC("fionne", "Fionne", x, y);
        
        NPC npcComponent = fionne.getComponent(NPC.class);
        
        // Set custom dialogue
        //npcComponent.setGreetingDialogue("Greetings, brave adventurer! I have a task that needs doing.");
        //npcComponent.setFarewellDialogue("May fortune favor you on your journey!");
        
        // Create a starter quest
        Quest goblinSlayerQuest = new Quest(
            "goblin_slayer",
            "Goblin Slayer",
            "The goblins have to be dealt with! Help us by defeating 5 of them.",
            Quest.QuestType.KILL
        );
        
        // Add objective: Kill 5 goblins
        goblinSlayerQuest.addObjective(new QuestObjective(
            "kill_goblins",
            "Defeat 5 Goblins",
            5
        ));
         
        // Set rewards
        goblinSlayerQuest.setExpReward(150);
        goblinSlayerQuest.setGoldReward(50);
        goblinSlayerQuest.addItemReward("Potion of Minor Healing");
        
        // Set quest dialogue
        //goblinSlayerQuest.setAcceptDialogue("Excellent! The goblins won't know what hit them. Return when you've slain 5 of them.");
        //goblinSlayerQuest.setProgressDialogue("Have you dealt with those goblins yet? We're counting on you!");
        //goblinSlayerQuest.setCompleteDialogue("Amazing work! The village is safer thanks to you. Here's your reward.");
        
        // Add quest to NPC
        npcComponent.addQuest(goblinSlayerQuest);
        
        return fionne;
    }
    /**
     * Create a simple tree environment entity
     */
    public static Entity createTree(float x, float y, String orientation) {
        Entity tree = new Entity("Tree", EntityType.ENVIRONMENT);

        tree.addComponent(new Position(x, y));
        
        switch(orientation) {
	        case "right":
	        	if(Engine.IDE == Engine.Eclipse)
	            	tree.addComponent(new Sprite("/sprites/tree01.png", 591, 545, 0f)); //eclipse
	            else if(Engine.IDE == Engine.VSCode)
	            	tree.addComponent(new Sprite("resources/sprites/tree01.png", 591, 545, 0f)); //VS Code
	             
	        	break;
	        case "left":
	        	if(Engine.IDE == Engine.Eclipse)
	            	tree.addComponent(new Sprite("/sprites/tree02.png", 591, 545, 0f)); //eclipse
	            else if(Engine.IDE == Engine.VSCode)
	            	tree.addComponent(new Sprite("resources/sprites/tree02.png", 591, 545, 0f)); //VS Code
	             
	        	break;
        }
        
        tree.addComponent(new Renderable(RenderLayer.ENTITIES, 120));
        NameTag nt = new NameTag("Tree", -40);
        nt.show();
        tree.addComponent(nt);

        return tree;
    }
    
    public static Entity createBoulder(float x, float y) {
    	Entity boulder = new Entity("Boulder", EntityType.ENVIRONMENT);
    	
    	boulder.addComponent(new Position(x, y));
    	
    	if(Engine.IDE == Engine.Eclipse)
    		boulder.addComponent(new Sprite("/sprites/boulder01.png", 239, 226, 0f)); //eclipse
        else if(Engine.IDE == Engine.VSCode)
        	boulder.addComponent(new Sprite("resources/sprites/boulder01.png", 239, 226, 0f)); //VS Code
      
    	boulder.addComponent(new Renderable(RenderLayer.ENTITIES, 20));
        NameTag nt = new NameTag("Boulder", -40);
        nt.show();
        boulder.addComponent(nt);
    	
    	
    	return boulder;
    }
    
    public static Entity createFountain(float x, float y) {
    	Entity fountain = new Entity("Fountain", EntityType.ENVIRONMENT);
    	
    	fountain.addComponent(new Position(x, y));
    	
    	if(Engine.IDE == Engine.Eclipse)
    		fountain.addComponent(new Sprite("/sprites/elvesfountain01.png", 239, 268, 0f)); //eclipse
        else if(Engine.IDE == Engine.VSCode)
        	fountain.addComponent(new Sprite("resources/sprites/elvesfountain01.png", 239, 268, 0f)); //VS Code
      
    	fountain.addComponent(new Renderable(RenderLayer.ENTITIES, 20));
        NameTag nt = new NameTag("Fountain", -40);
        nt.show();
        fountain.addComponent(nt);
    	
    	
    	return fountain;
    }
    
}