package dev.main.state;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import dev.main.Engine;
import dev.main.ai.AI;
import dev.main.ai.AI.State;
import dev.main.buffs.BuffManager;
import dev.main.drops.DropSystem;
import dev.main.drops.DroppedItem;
import dev.main.entity.Combat;
import dev.main.entity.Entity;
import dev.main.entity.EntityType;
import dev.main.entity.Experience;
import dev.main.entity.LevelUpEffect;
import dev.main.entity.MonsterLevel;
import dev.main.entity.NPC;
import dev.main.entity.TargetIndicator;
import dev.main.input.CollisionBox;
import dev.main.input.Movement;
import dev.main.input.Position;
import dev.main.item.Item;
import dev.main.pathfinder.Path;
import dev.main.pathfinder.Pathfinder;
import dev.main.quest.IntroQuestHandler;
import dev.main.quest.QuestIndicator;
import dev.main.quest.QuestLog;
import dev.main.quest.QuestObjective;
import dev.main.skill.Skill;
import dev.main.skill.SkillLevel;
import dev.main.sprite.Sprite;
import dev.main.stats.Stats;
import dev.main.tile.TileMap;
import dev.main.ui.Quest; 
import dev.main.ui.UIManager;
import dev.main.ui.UIScrollableInventoryPanel;
import dev.main.util.Alert;
import dev.main.util.DamageText;
import dev.main.util.Dead; 
import dev.main.ui.UIGearSlot;

public class GameLogic {
    
	private DropSystem dropSystem;
	
	
    private GameState state;
    private float cameraLerpSpeed = 5f;
    
    public GameLogic(GameState state) {
        this.state = state;
        
        this.dropSystem = new DropSystem(); // Initialize drop system

        
    } 
    

	public void update(float delta) {
	    state.incrementGameTime(delta);
	    
	    // ★ NEW: Update intro quest handler (initializes indicator on first frame)
	    IntroQuestHandler introHandler = state.getIntroQuestHandler();
	    if (introHandler != null) {
	        introHandler.update(delta);
	    }
	    
	    Entity player = state.getPlayer();
	    Position playerPos = player.getComponent(Position.class);
	    
	    for (Entity entity : state.getEntities()) {
	        EntityType entityType = entity.getType();
	        
	        Combat combat = entity.getComponent(Combat.class);
	        if (combat != null) {
	            combat.update(delta);
	        }
	        
	        if (entityType == EntityType.PLAYER) {
	            updatePlayer(entity, delta);
	        } else if (entityType == EntityType.MONSTER) {
	            updateMonster(entity, playerPos, delta);
	        } 
	        // ⭐ NEW: Update NPCs
	        else if (entityType == EntityType.NPC) {
	           // updateNPC(entity, player, delta);
	        }
	        
	        Sprite sprite = entity.getComponent(Sprite.class);
	        if (sprite != null) {
	            sprite.update(delta);
	        } 
	        
	        
	        // ★ NEW: Update quest indicators
	        QuestIndicator questIndicator = entity.getComponent(QuestIndicator.class);
	        if (questIndicator != null) { 
	            questIndicator.update(delta); 
	            //updateNPCQuestIndicators();
	        }
	      
	        TargetIndicator indicator = entity.getComponent(TargetIndicator.class);
	        if (indicator != null) {
	            indicator.update(delta);
	        }
	        
	        // Update level-up effect
	        LevelUpEffect levelUpEffect = entity.getComponent(LevelUpEffect.class);
	        if (levelUpEffect != null) {
	            levelUpEffect.update(delta);
	        }
	    }
	    
	 // ★ UPDATE QUEST INDICATORS (add this near the end of update method)
	    updateQuestIndicators(delta);
	    state.updateDamageTexts(delta);
	    state.updateSpawnPoints(delta);
	    state.removeMarkedEntities();
	    updateCamera(delta);
	}
	/**
	 * ★ NEW: Update all quest indicators for animation
	 */
	private void updateQuestIndicators(float delta) {
	    for (Entity entity : state.getEntities()) {
	        QuestIndicator indicator = entity.getComponent(QuestIndicator.class);
	        if (indicator != null) {
	            indicator.update(delta);
	        }
	    }
	}
	/**
	 * Update NPC quest indicators based on player's quest status
	 
	private void updateNPC(Entity npc, Entity player, float delta) {
	    NPC npcComponent = npc.getComponent(NPC.class);
	    QuestIndicator questIndicator = npc.getComponent(QuestIndicator.class);
	    
	    if (npcComponent == null || questIndicator == null) return;
	    
	    // Update quest indicator animation
	    questIndicator.update(delta);
	    
	    // Check quest status
	    Quest completedQuest = npcComponent.getCompletedQuest();
	    if (completedQuest != null) {
	        // Show "?" for quest completion
	        questIndicator.show(QuestIndicator.IndicatorType.COMPLETE);
	        return;
	    }
	    
	    Quest activeQuest = npcComponent.getActiveQuest();
	    if (activeQuest != null) {
	        // Show "..." for quest in progress
	        questIndicator.show(QuestIndicator.IndicatorType.IN_PROGRESS);
	        return;
	    }
	    
	    Quest availableQuest = npcComponent.getNextAvailableQuest();
	    if (availableQuest != null) {
	        // Show "!" for available quest
	        questIndicator.show(QuestIndicator.IndicatorType.AVAILABLE);
	        return;
	    }
	    
	    // No quests - hide indicator
	    questIndicator.hide();
	}
    /**
     * Player attacks target entity (called from click or auto-attack)
     * ☆ FIXED: Clears previous movement path immediately when targeting monster
     */
    public void playerAttack(Entity target) {
        Entity player = state.getPlayer();
        Combat playerCombat = player.getComponent(Combat.class);
        Position playerPos = player.getComponent(Position.class);
        Position targetPos = target.getComponent(Position.class);
        Movement playerMovement = player.getComponent(Movement.class);
        Stats playerStats = player.getComponent(Stats.class);
        Path playerPath = player.getComponent(Path.class);  // ☆ NEW
        TargetIndicator indicator = player.getComponent(TargetIndicator.class);  // ☆ NEW
        
        if (playerCombat == null || playerStats == null) return;
        if (playerPos == null || targetPos == null) return;
        
        // ☆ NEW: Clear any existing movement path/target immediately
        if (playerPath != null) {
            playerPath.clear();
        }
        if (indicator != null) {
            indicator.clear();
        }
        
        // Set as auto-attack target
        state.setAutoAttackTarget(target);
        
        // Check distance
        float distance = distance(playerPos.x, playerPos.y, targetPos.x, targetPos.y);
        
        if (distance <= 80f && playerCombat.canAttackWithStamina(playerStats)) {
            // In range and can attack - do it now
            playerMovement.direction = calculateDirection(targetPos.x - playerPos.x, targetPos.y - playerPos.y);
            playerMovement.lastDirection = playerMovement.direction;
            
            // ☆ Stop moving to old location
            playerMovement.stopMoving();
            
            if (playerStats.consumeStaminaForAttack()) {
                playerCombat.startAttack(target);
            } else {
                System.out.println("Not enough stamina to attack!");
            }
        } else if (distance > 80f) {
            // Out of range - will path to target in update loop
            // ☆ Movement is already cleared, auto-attack will create new path to monster
        } else {
            System.out.println("Not enough stamina to attack!");
        }
    }
    /**
     * Stop auto-attacking (called when moving to new location)
     */
    public void stopAutoAttack() {
        state.clearAutoAttackTarget();
        
        // ☆ NEW: Clear target indicator when stopping auto-attack
        Entity player = state.getPlayer();
        TargetIndicator indicator = player.getComponent(TargetIndicator.class);
        if (indicator != null) {
            indicator.clear();
        }
    }
    /**
     * Perform attack calculation with crit/evasion
     */ 
    private void performAttack(Entity attacker, Entity target, Position attackerPos, Position targetPos) {
        Stats attackerStats = attacker.getComponent(Stats.class);
        Stats targetStats = target.getComponent(Stats.class);
        Combat attackerCombat = attacker.getComponent(Combat.class);
        Combat targetCombat = target.getComponent(Combat.class);
        
        if (attackerStats == null || targetStats == null) return;
        
        // Check evasion
        float evasionRoll = ThreadLocalRandom.current().nextFloat();
        float evasionChance = targetCombat != null ? targetCombat.evasionChance : 0f;
       
        if (evasionRoll < evasionChance) {
            DamageText missText = new DamageText("MISS", DamageText.Type.MISS, targetPos.x, targetPos.y - 30);
            state.addDamageText(missText); 
            
            reduceDurability();
            
            return;
        }
        
        // Check critical hit
        boolean isCrit = false;
        float critRoll = ThreadLocalRandom.current().nextFloat();
        float critChance = attackerCombat != null ? attackerCombat.critChance : 0f;
        
        if (critRoll < critChance) {
            isCrit = true;
        }
        
        // Calculate damage
        int baseDamage = attackerStats.attack - targetStats.defense;
        baseDamage = Math.max(1, baseDamage);
        
        if (isCrit && attackerCombat != null) {
            baseDamage = (int)(baseDamage * attackerCombat.critMultiplier);
        } 
        
        // Apply damage
        targetStats.hp -= baseDamage;
        if (targetStats.hp < 0) targetStats.hp = 0;
        
        // ☆ REFACTORED: Choose damage text type based on target
        DamageText.Type textType;
        
        if (target.getType() == EntityType.PLAYER) {
            // ☆ Player taking damage
            if (isCrit) {
                textType = DamageText.Type.PLAYER_CRITICAL_DAMAGE;  // Dark orange-red crit
            } else {
                textType = DamageText.Type.PLAYER_DAMAGE;  // Dark red normal
            }
        } else if (isCrit) {
            // Monster taking crit - use orange
            textType = DamageText.Type.CRITICAL;
        } else {
            // Monster taking normal damage - use white
            textType = DamageText.Type.NORMAL;
        }
        
        // Spawn damage text
        DamageText damageText = new DamageText(String.valueOf(baseDamage), textType, targetPos.x, targetPos.y - 30);
        state.addDamageText(damageText);
         
        // Check death
        if (targetStats.hp <= 0) {
            Dead alreadyDead = target.getComponent(Dead.class);
            if (alreadyDead == null) {
                if (target.getType() == EntityType.MONSTER) {
                    handleMonsterDeath(target, target.getComponent(Sprite.class));
                } else if (target.getType() == EntityType.PLAYER) {
                    // If a monster killed the player, put the monster into VICTORY_IDLE
                    if (attacker.getType() == EntityType.MONSTER) {
                        AI killerAI = attacker.getComponent(AI.class);
                        Sprite killerSprite = attacker.getComponent(Sprite.class);
                        Movement killerMovement = attacker.getComponent(Movement.class);

                        if (killerAI != null) {
                            killerAI.currentState = AI.State.VICTORY_IDLE;
                            killerAI.victoryIdleTimer = 0f;
                        }

                        if (killerSprite != null) {
                            String victoryAnim = Sprite.ANIM_VICTORY_IDLE_DOWN;
                            if (killerMovement != null) {
                                victoryAnim = getVictoryAnimationForDirection(killerMovement.lastDirection);
                            }
                            killerSprite.setAnimation(victoryAnim);
                        }
                    }

                    handlePlayerDeath(target, target.getComponent(Sprite.class));
                }
            }
        }
        
        // If monster was hit by player, aggro it
        if (attacker.getType() == EntityType.PLAYER && target.getType() == EntityType.MONSTER) {
            AI targetAI = target.getComponent(AI.class);
            Stats monsterStats = target.getComponent(Stats.class);
            
            if (targetAI != null && monsterStats != null && monsterStats.hp > 0 && 
                targetAI.currentState != AI.State.DEAD) {
                if (targetAI.currentState == AI.State.IDLE || 
                    targetAI.currentState == AI.State.ROAMING || 
                    targetAI.currentState == AI.State.RETURNING) {
                    transitionAIState(target, targetAI, AI.State.CHASING);
                    targetAI.target = attacker;
                }
            }
        }
    }
    
    private void reduceDurability() {
    	//reduce durability when attack is a miss! - execute only when weapon is equipped
        if(state.getUIManager().getGearSlot(UIGearSlot.SlotType.WEAPON) != null &&
           state.getUIManager().getGearSlot(UIGearSlot.SlotType.WEAPON).getItem() != null &&
           state.getUIManager().getGearSlot(UIGearSlot.SlotType.WEAPON).getItem().getCurrentDurability() > 0) {
        	
        	state.getUIManager().getGearSlot(UIGearSlot.SlotType.WEAPON).getItem().reduceDurability(1);
            System.out.println("****NOTIFICATION: \n GameLogic.performAttack()>Reduced durability!");
        }
		
	}


	/**
     * Handle player death
     */
    private void handlePlayerDeath(Entity player, Sprite sprite) {
        System.out.println("Player has died!");
        
        // Stop all actions
        Movement movement = player.getComponent(Movement.class);
        if (movement != null) {
            movement.stopMoving();
        }
        
        Path path = player.getComponent(Path.class);
        if (path != null) {
            path.clear();
        }
        
        // Clear auto-attack
        state.clearAutoAttackTarget();
        
        // Play death animation
        if (sprite != null) {
            sprite.setAnimation(Sprite.ANIM_DEAD);
        }
        
        // Add dead component
        player.addComponent(new Dead(10f));  // Player corpse lasts 10 seconds
        
        // TODO: Show respawn UI, game over screen, etc.
    }

    private void updatePlayer(Entity player, float delta) {
        Movement movement = player.getComponent(Movement.class);
        Position position = player.getComponent(Position.class);
        Sprite sprite = player.getComponent(Sprite.class);
        Stats stats = player.getComponent(Stats.class);
        Path path = player.getComponent(Path.class);
        Combat combat = player.getComponent(Combat.class);
        TargetIndicator indicator = player.getComponent(TargetIndicator.class);
        Dead dead = player.getComponent(Dead.class);
        
        if (movement == null || position == null || sprite == null || stats == null) return;
        
        if (dead != null) {
            if (sprite != null) {
                sprite.setAnimation(Sprite.ANIM_DEAD);
            }
            dead.update(delta);
            return;
        }
        
        if (stats.hp <= 0) {
            handlePlayerDeath(player, sprite);
            return;
        }
        
        // NEW: Update buff manager
        BuffManager buffManager = player.getComponent(BuffManager.class);
        if (buffManager != null) {
            buffManager.update(delta);
            
            // Health regen
            float healthRegen = buffManager.getTotalHealthRegen();
            if (healthRegen > 0 && stats.hp < stats.maxHp) {
                stats.hp = Math.min(stats.maxHp, stats.hp + (int)(healthRegen * delta));
            }
            
            // Mana regen boost
            float manaBoost = buffManager.getTotalManaRegenBoost();
            if (manaBoost > 0) {
                stats.manaRegenRate += manaBoost;
            }
            
            // Stamina regen boost
            float staminaBoost = buffManager.getTotalStaminaRegenBoost();
            if (staminaBoost > 0) {
                stats.stamina = Math.min(stats.maxStamina, stats.stamina + staminaBoost * delta);
            }
        }
        
        // Regenerate mana
        stats.regenerateMana(delta);
        
        // ☆ NEW: Handle stamina based on movement state
        String staminaState = "idle";
        if (movement.isMoving) {
            if (movement.isRunning) {
                staminaState = "running";
                
                // ☆ Check if out of stamina while running
                if (stats.stamina <= 0) {
                    movement.stopRunning();
                    System.out.println("Out of stamina! Can't run.");
                }
            } else {
                staminaState = "walking";
            }
        }
        stats.regenerateStaminaByState(staminaState, delta);
        
        // Check if attack animation hit frame reached - apply damage
        if (combat != null && combat.shouldDealDamage() && combat.attackTarget != null) {
            Position targetPos = combat.attackTarget.getComponent(Position.class);
            if (targetPos != null) {
                performAttack(player, combat.attackTarget, position, targetPos);
            }
        }
        
        // Auto-attack logic
        Entity autoAttackTarget = state.getAutoAttackTarget();
        if (autoAttackTarget != null) {
            Stats targetStats = autoAttackTarget.getComponent(Stats.class);
            if (targetStats == null || targetStats.hp <= 0) {
                state.clearAutoAttackTarget();
                autoAttackTarget = null;
            } else {
                Position targetPos = autoAttackTarget.getComponent(Position.class);
                if (targetPos != null) {
                    float distance = distance(position.x, position.y, targetPos.x, targetPos.y);
                    
                    if (distance <= 80f) {
                        // ☆ NEW: Check stamina before attacking
                        if (combat != null && combat.canAttackWithStamina(stats) && !combat.isAttacking) {
                            float dx = targetPos.x - position.x;
                            float dy = targetPos.y - position.y;
                            movement.direction = calculateDirection(dx, dy);
                            movement.lastDirection = movement.direction;
                            
                            movement.stopMoving();
                            if (path != null) path.clear();
                            if (indicator != null) indicator.clear();
                            
                            // ☆ NEW: Consume stamina for attack
                            if (stats.consumeStaminaForAttack()) {
                                combat.startAttack(autoAttackTarget);
                            } else {
                                System.out.println("Not enough stamina to attack!");
                            }
                        }
                    } else {
                        if (!movement.isMoving || (path != null && !path.isFollowing)) {
                            int startTileX = (int)(position.x / TileMap.TILE_SIZE);
                            int startTileY = (int)(position.y / TileMap.TILE_SIZE);
                            int goalTileX = (int)(targetPos.x / TileMap.TILE_SIZE);
                            int goalTileY = (int)(targetPos.y / TileMap.TILE_SIZE);
                            
                            List<int[]> foundPath = state.getPathfinder().findPath(startTileX, startTileY, goalTileX, goalTileY);
                            
                            if (foundPath != null && path != null) {
                                path.setPath(foundPath);
                                movement.isRunning = false;
                            }
                        }
                    }
                }
            }
        }
        
        // Handle attack animation
        if (combat != null && combat.isAttacking) {
            sprite.setAnimation(getAttackAnimationForDirection(movement.lastDirection));
            
            if (autoAttackTarget != null) {
                Position targetPos = autoAttackTarget.getComponent(Position.class);
                if (targetPos != null) {
                    float dx = targetPos.x - position.x;
                    float dy = targetPos.y - position.y;
                    int newDirection = calculateDirection(dx, dy);
                    if (newDirection != movement.lastDirection) {
                        movement.direction = newDirection;
                        movement.lastDirection = newDirection;
                    }
                }
            }
            return;
        }
        
        if (path != null && path.isFollowing) {
            followPath(player, path, movement, position, delta);
        }
        
        if (movement.isMoving) {
            moveTowardsTarget(player, movement, position, delta);
            
            String moveAnim = movement.isRunning 
                ? getRunAnimationForDirection(movement.direction)
                : getWalkAnimationForDirection(movement.direction);
            sprite.setAnimation(moveAnim);
            
        } else {
            if (indicator != null && autoAttackTarget == null) {
                indicator.clear();
            }
            
            sprite.setAnimation(getIdleAnimationForDirection(movement.lastDirection));
        }
    }
	
    private void updateMonster(Entity monster, Position playerPos, float delta) {
        AI ai = monster.getComponent(AI.class);
        Position position = monster.getComponent(Position.class);
        Movement movement = monster.getComponent(Movement.class);
        Sprite sprite = monster.getComponent(Sprite.class);
        Stats stats = monster.getComponent(Stats.class);
        Path path = monster.getComponent(Path.class);
        Dead dead = monster.getComponent(Dead.class);
        Combat combat = monster.getComponent(Combat.class);
        Alert alert = monster.getComponent(Alert.class);  // NEW
        
        if (position == null || ai == null) return;
        
        // Update dead state
        if (dead != null) {
            dead.update(delta);
            if (dead.shouldRemove()) {
                state.markForRemoval(monster);
            }
            return;
        }
        
        // Check if dead
        if (stats != null && stats.hp <= 0) {
            handleMonsterDeath(monster, sprite);
            return;
        }
        
        // ⭐ Check if monster attack hit frame reached
        if (combat != null && combat.shouldDealDamage() && combat.attackTarget != null) {
            Position targetPos = combat.attackTarget.getComponent(Position.class);
            if (targetPos != null) {
                performAttack(monster, combat.attackTarget, position, targetPos);
            }
        }
        
        // Update AI
        ai.update(delta);
        
        // ⭐ Update alert animation
        if (alert != null) {
            alert.update(delta);
            
            // Show alert when chasing or attacking, hide otherwise
            if (ai.currentState == AI.State.CHASING || ai.currentState == AI.State.ATTACKING) {
                alert.show();
            } else {
                alert.hide();
            }
        }
        
        // Update combat animations
        if (combat != null && combat.isAttacking) {
            if (sprite != null && movement != null) {
                sprite.setAnimation(getAttackAnimationForDirection(movement.lastDirection));
            }
        }
        
        // AI State Machine
        switch (ai.currentState) {
            case IDLE: 
                handleIdleState(monster, ai, movement, sprite, playerPos, delta);
                break;
                
            case ROAMING:
                handleRoamingState(monster, ai, movement, position, path, sprite, playerPos, delta);
                break;
                
            case CHASING:
                handleChasingState(monster, ai, movement, position, path, sprite, playerPos, delta);
                break;
                
            case RETURNING:
                handleReturningState(monster, ai, movement, position, path, sprite, delta);
                break;
                
            case ATTACKING:
                handleAttackingState(monster, ai, movement, sprite, playerPos, stats, delta);
                break;
                
            case VICTORY_IDLE:  
                handleVictoryIdleState(monster, ai, movement, sprite, delta);
                break;
        }
        
        // Move if has target
        if (movement != null && movement.isMoving) {
            moveTowardsTarget(monster, movement, position, delta);
        }
        
        // Follow path
        if (path != null && path.isFollowing) {
            followPath(monster, path, movement, position, delta);
        }
    }
	/**
	 * Victory idle state - monster idles after killing player
	 */
	private void handleVictoryIdleState(Entity monster, AI ai, Movement movement, Sprite sprite, float delta) {
	    // Play idle animation
	    if (sprite != null && movement != null) {
        sprite.setAnimation(getVictoryAnimationForDirection(movement.lastDirection));
	    }
	    
	    // Debug output
	    //System.out.println(monster.getName() + " victory idle: " + ai.victoryIdleTimer + "/" + ai.victoryIdleDuration);
	    
	    // After timer expires, return to patrolling
	    if (ai.victoryIdleTimer >= ai.victoryIdleDuration) {
	        //System.out.println(monster.getName() + " victory timer complete! Transitioning...");
	        
	        Position monsterPos = monster.getComponent(Position.class);
	        if (monsterPos != null) {
	            float distFromHome = distance(monsterPos.x, monsterPos.y, ai.homeX, ai.homeY);
	            
	            //System.out.println("  Distance from home: " + distFromHome + " (roam radius: " + ai.roamRadius + ")");
	            
	            if (distFromHome <= ai.roamRadius * 1.2f) {
	                //System.out.println("  -> Transitioning to IDLE");
	                transitionAIState(monster, ai, AI.State.IDLE);
	            } else {
	                //System.out.println("  -> Transitioning to RETURNING");
	                transitionAIState(monster, ai, AI.State.RETURNING);
	            }
	        } else {
	            //System.out.println("  -> No position, transitioning to IDLE");
	            transitionAIState(monster, ai, AI.State.IDLE);
	        }
	    }
	}
	/**
	 * Helper to cleanly transition AI state and clear movement
	 */
	private void transitionAIState(Entity entity, AI ai, AI.State newState) {
	    if (ai.currentState == newState) return;
	    
	    //System.out.println(entity.getName() + " transitioning: " + ai.currentState + " -> " + newState);
	    
	    AI.State oldState = ai.currentState;
	    ai.currentState = newState;
	    
	    // Clear movement/path when transitioning
	    Movement movement = entity.getComponent(Movement.class);
	    Path path = entity.getComponent(Path.class);
	    
	    if (movement != null) {
	        movement.stopMoving();
	        
	        // ⭐ Clear haste when leaving RETURNING state
	        if (oldState == AI.State.RETURNING && newState != AI.State.RETURNING) {
	            movement.setHaste(false);
	            //System.out.println("  Haste removed");
	        }
	    }
	    if (path != null) {
	        path.clear();
	    }
	    
	    // Reset timers based on new state
	    switch(newState) {
	        case IDLE:
	            ai.roamTimer = 0;
	            ai.roamInterval = ThreadLocalRandom.current().nextFloat(3f, 6f);
	            break;
	        case VICTORY_IDLE:
	            ai.victoryIdleTimer = 0;
	            ai.target = null;
	            //System.out.println("  Victory idle timer reset to 0");
	            break;
	        case RETURNING:
	            ai.target = null;
	            // Haste will be applied in handleReturningState
	            break;
	    }
	}
    // Add attack animation helper (8 directions)
    private String getAttackAnimationForDirection(int direction) {
        // TODO: Map to actual attack animation rows in sprite sheet
        // For now, return walk animation as placeholder
        switch(direction) {
            case Movement.DIR_EAST:
                return "attack_right";  // or Sprite.ANIM_ATTACK_RIGHT
            case Movement.DIR_SOUTH_EAST:
                return "attack_down_right";
            case Movement.DIR_SOUTH:
                return "attack_down";
            case Movement.DIR_SOUTH_WEST:
                return "attack_down_left";
            case Movement.DIR_WEST:
                return "attack_left";
            case Movement.DIR_NORTH_WEST:
                return "attack_up_left";
            case Movement.DIR_NORTH:
                return "attack_up";
            case Movement.DIR_NORTH_EAST:
                return "attack_up_right";
            default:
                return "attack_down";
        }
    }
    
    private void handleIdleState(Entity monster, AI ai, Movement movement, Sprite sprite, Position playerPos, float delta) {
        Position monsterPos = monster.getComponent(Position.class);
        
        // Detect player (only if alive and we're within our territory)
        if (ai.behaviorType.equals("aggressive") && playerPos != null && monsterPos != null) {
            Entity player = state.getPlayer();
            Stats playerStats = player.getComponent(Stats.class);
            
            // Check if we're within safe territory before engaging
            float distFromHome = distance(monsterPos.x, monsterPos.y, ai.homeX, ai.homeY);
            
            // Only detect if player is alive AND we're not too far from home
            if (playerStats != null && playerStats.hp > 0 && distFromHome <= ai.roamRadius * 1.5f) {
                if (canDetectPlayer(monsterPos, playerPos, ai.detectionRange)) {
                    transitionAIState(monster, ai, AI.State.CHASING);
                    ai.target = state.getPlayer();
                    return;
                }
            }
        }
        
        // Transition to roaming after timer
        if (ai.roamTimer >= ai.roamInterval) {
            transitionAIState(monster, ai, AI.State.ROAMING);
        }
        
        // Play idle animation
        if (sprite != null && movement != null) {
            sprite.setAnimation(getIdleAnimationForDirection(movement.lastDirection));
        }
    }

    private void handleRoamingState(Entity monster, AI ai, Movement movement, Position position, Path path, Sprite sprite, Position playerPos, float delta) {
        // Detect player (only if alive and within territory)
        if (ai.behaviorType.equals("aggressive") && playerPos != null && position != null) {
            Entity player = state.getPlayer();
            Stats playerStats = player.getComponent(Stats.class);
            
            // Check territory
            float distFromHome = distance(position.x, position.y, ai.homeX, ai.homeY);
            
            // Only detect if player is alive AND we're not too far from home
            if (playerStats != null && playerStats.hp > 0 && distFromHome <= ai.roamRadius * 1.5f) {
                if (canDetectPlayer(position, playerPos, ai.detectionRange)) {
                    transitionAIState(monster, ai, AI.State.CHASING);
                    ai.target = state.getPlayer();
                    return;
                }
            }
        }
        
        // Pick random point within roam radius
        if (movement != null && !movement.isMoving) {
            float angle = (float)(ThreadLocalRandom.current().nextDouble() * Math.PI * 2);
            float distance = ThreadLocalRandom.current().nextFloat(0.5f, 1f) * ai.roamRadius;
            
            float targetX = ai.homeX + (float)Math.cos(angle) * distance;
            float targetY = ai.homeY + (float)Math.sin(angle) * distance;
            
            int startTileX = (int)(position.x / TileMap.TILE_SIZE);
            int startTileY = (int)(position.y / TileMap.TILE_SIZE);
            int goalTileX = (int)(targetX / TileMap.TILE_SIZE);
            int goalTileY = (int)(targetY / TileMap.TILE_SIZE);
            
            List<int[]> foundPath = state.getPathfinder().findPath(startTileX, startTileY, goalTileX, goalTileY);
            
            if (foundPath != null && path != null) {
                path.setPath(foundPath);
                movement.isRunning = false;
            } else {
                transitionAIState(monster, ai, AI.State.IDLE);
            }
        }
        
        // Reached destination
        if (movement != null && !movement.isMoving && (path == null || !path.isFollowing)) {
            transitionAIState(monster, ai, AI.State.IDLE);
        }
        
        // Animation
        if (sprite != null && movement != null) {
            if (movement.isMoving) {
                sprite.setAnimation(getWalkAnimationForDirection(movement.direction));
            } else {
                sprite.setAnimation(getIdleAnimationForDirection(movement.lastDirection));
            }
        }
    } 
    
    private void handleChasingState(Entity monster, AI ai, Movement movement, 
            Position position, Path path, Sprite sprite, 
            Position playerPos, float delta) {
    	
        if (playerPos == null || movement == null || position == null) {
            transitionAIState(monster, ai, AI.State.RETURNING);
            return;
        }
        
        // Check if player is dead
        Entity player = state.getPlayer();
        if (player != null) {
            Stats playerStats = player.getComponent(Stats.class);
            if (playerStats != null && playerStats.hp <= 0) {
                transitionAIState(monster, ai, AI.State.VICTORY_IDLE);
                return;
            }
        }
        
        // ⭐ PRIORITY 1: Check if too far from home (strict leash)
        float distFromHome = distance(position.x, position.y, ai.homeX, ai.homeY);
        if (distFromHome > ai.returnThreshold) {
            transitionAIState(monster, ai, AI.State.RETURNING);
            //System.out.println(monster.getName() + " leash break - too far from home!");
            return;
        }
        
        // ⭐ PRIORITY 2: Check if player moved too far away (lose aggro)
        float distToPlayer = distance(position.x, position.y, playerPos.x, playerPos.y);
        if (distToPlayer > ai.detectionRange * TileMap.TILE_SIZE * 1.5f) {
            transitionAIState(monster, ai, AI.State.RETURNING);
            //System.out.println(monster.getName() + " player too far - returning home");
            return;
        }
        
        // ⭐ PRIORITY 3: Check if in attack range
        if (distToPlayer <= ai.attackRange) {
            transitionAIState(monster, ai, AI.State.ATTACKING);
            return;
        }
        
        // PRIORITY 4: Chase player - update path periodically
        // ★ OPTIMIZED: Only recalculate path periodically
        ai.pathUpdateTimer += delta;
        
        if (!movement.isMoving || ai.pathUpdateTimer >= ai.pathUpdateInterval) {
            ai.pathUpdateTimer = 0;
            
            int startTileX = (int)(position.x / TileMap.TILE_SIZE);
            int startTileY = (int)(position.y / TileMap.TILE_SIZE);
            int goalTileX = (int)(playerPos.x / TileMap.TILE_SIZE);
            int goalTileY = (int)(playerPos.y / TileMap.TILE_SIZE);
            
            List<int[]> foundPath = state.getPathfinder().findPath(
                startTileX, startTileY, goalTileX, goalTileY);
            
            if (foundPath != null && path != null) {
                ai.cachedPath = foundPath;  // ★ Cache it
                path.setPath(foundPath);
                movement.isRunning = true;
            } else {
                transitionAIState(monster, ai, AI.State.RETURNING);
            }
        }
        
        // Animation
        if (sprite != null && movement.isMoving) {
            sprite.setAnimation(getRunAnimationForDirection(movement.direction));
        } else if (sprite != null) {
            // Not moving but still in chase state (stuck?)
            sprite.setAnimation(getIdleAnimationForDirection(movement.lastDirection));
        }
    }

    private void handleReturningState(Entity monster, AI ai, Movement movement, Position position, Path path, Sprite sprite, float delta) {
        if (movement == null || position == null) return;
        
        // ⭐ Apply haste effect when returning
        if (!movement.isHasted) {
            movement.setHaste(true);
            //System.out.println(monster.getName() + " has haste! (3x speed)");
        }
        
        // Check if back home
        float distFromHome = distance(position.x, position.y, ai.homeX, ai.homeY);
        if (distFromHome < 32f) {
            // Remove haste
            movement.setHaste(false);
            
            // Heal to full when reaching home
            Stats stats = monster.getComponent(Stats.class);
            if (stats != null && stats.hp < stats.maxHp) {
                int healAmount = stats.maxHp - stats.hp;
                stats.hp = stats.maxHp;
               // System.out.println(monster.getName() + " healed to full! (+" + healAmount + " HP)");
                
                DamageText healText = new DamageText("+" + healAmount, DamageText.Type.HEAL, position.x, position.y - 30);
                state.addDamageText(healText);
            }
            
            transitionAIState(monster, ai, AI.State.IDLE);
            return;
        }
        
        // ⭐ REMOVED: Don't check for player while returning - just go home!
        // Once we commit to returning, we ignore everything until we're home
        
        // Path back home
        if (!movement.isMoving || (path != null && !path.isFollowing)) {
            int startTileX = (int)(position.x / TileMap.TILE_SIZE);
            int startTileY = (int)(position.y / TileMap.TILE_SIZE);
            int goalTileX = (int)(ai.homeX / TileMap.TILE_SIZE);
            int goalTileY = (int)(ai.homeY / TileMap.TILE_SIZE);
            
            if (startTileX == goalTileX && startTileY == goalTileY) {
                // Remove haste
                movement.setHaste(false);
                
                // Heal to full
                Stats stats = monster.getComponent(Stats.class);
                if (stats != null && stats.hp < stats.maxHp) {
                    int healAmount = stats.maxHp - stats.hp;
                    stats.hp = stats.maxHp;
                    //System.out.println(monster.getName() + " healed to full! (+" + healAmount + " HP)");
                    
                    DamageText healText = new DamageText("+" + healAmount, DamageText.Type.HEAL, position.x, position.y - 30);
                    state.addDamageText(healText);
                }
                
                transitionAIState(monster, ai, AI.State.IDLE);
                return;
            }
            
            List<int[]> foundPath = state.getPathfinder().findPath(startTileX, startTileY, goalTileX, goalTileY);
            
            if (foundPath != null && path != null) {
                path.setPath(foundPath);
                movement.isRunning = false;
            } else {
                //System.out.println(monster.getName() + " couldn't find path home, going idle");
                movement.setHaste(false);
                transitionAIState(monster, ai, AI.State.IDLE);
            }
        }
        
        // Animation - use run animation when hasted
        if (sprite != null && movement != null) {
            if (movement.isMoving) {
                if (movement.isHasted) {
                    sprite.setAnimation(getRunAnimationForDirection(movement.direction));
                } else {
                    sprite.setAnimation(getWalkAnimationForDirection(movement.direction));
                }
            } else {
                sprite.setAnimation(getIdleAnimationForDirection(movement.lastDirection));
            }
        }
    } 
    private void handleAttackingState(Entity monster, AI ai, Movement movement, Sprite sprite, Position playerPos, Stats stats, float delta) {
    	Position monsterPos = monster.getComponent(Position.class);
        Entity player = state.getPlayer();
        Combat combat = monster.getComponent(Combat.class);
        
        if (playerPos == null || monsterPos == null) {
            transitionAIState(monster, ai, AI.State.IDLE);
            return;
        }
        
        // Check if player is dead
        if (player != null) {
            Stats playerStats = player.getComponent(Stats.class);
            if (playerStats != null && playerStats.hp <= 0) {
                transitionAIState(monster, ai, AI.State.VICTORY_IDLE);
                return;
            }
        }
        
        float distToPlayer = distance(monsterPos.x, monsterPos.y, playerPos.x, playerPos.y);
        
        // Player moved away
        if (distToPlayer > ai.attackRange * 1.5f) {
            transitionAIState(monster, ai, AI.State.CHASING);
            return;
        }
        
        // ⭐ Face player only when actually attacking
        // ⭐ Start attack animation (damage will be applied later at hit frame)
        if (ai.canAttack() && combat != null && combat.canAttack()) {
            if (movement != null) {
                float dx = playerPos.x - monsterPos.x;
                float dy = playerPos.y - monsterPos.y;
                movement.direction = calculateDirection(dx, dy);
                movement.lastDirection = movement.direction;
            }
            
            combat.startAttack(player);  // ⭐ Pass target to combat
            ai.resetAttackCooldown();
            
            if (sprite != null && movement != null) {
                sprite.setAnimation(getAttackAnimationForDirection(movement.lastDirection));
            }
        } else {
            if (sprite != null && movement != null) {
                if (combat != null && combat.isAttacking) {
                    sprite.setAnimation(getAttackAnimationForDirection(movement.lastDirection));
                } else {
                    sprite.setAnimation(getIdleAnimationForDirection(movement.lastDirection));
                }
            }
        }
    } 
    /**
     * Detect player using distance check (can add dot product later for FOV)
     */
    private boolean canDetectPlayer(Position monsterPos, Position playerPos, float detectionTiles) {
        float detectionDistance = detectionTiles * TileMap.TILE_SIZE;
        float dist = distance(monsterPos.x, monsterPos.y, playerPos.x, playerPos.y);
        return dist <= detectionDistance;
    }
    
    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }
    /*
    public void tryAttack(Entity attacker, Entity target) {
        Position attackerPos = attacker.getComponent(Position.class);
        Position targetPos = target.getComponent(Position.class);
        
        if (attackerPos == null || targetPos == null) {
            System.out.println("ERROR: tryAttack called without valid positions!");
            return;
        }
        
        performAttack(attacker, target, attackerPos, targetPos);
    }
    */
    /**
     * Follow the current path waypoint by waypoint
     */
    private void followPath(Entity entity, Path path, Movement movement, Position position, float delta) {
        int[] waypoint = path.getCurrentWaypoint();
        
        if (waypoint == null) {
            path.clear();
            movement.stopMoving();
            return;
        }
        
        // Convert tile coordinates to world coordinates (center of tile)
        float waypointWorldX = waypoint[0] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
        float waypointWorldY = waypoint[1] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
        
        // Check if we're close to the waypoint
        float dx = waypointWorldX - position.x;
        float dy = waypointWorldY - position.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 5f) {
            // Reached waypoint, move to next
            path.advanceWaypoint();
            
            if (!path.isFollowing) {
                // Reached final destination
                movement.stopMoving();
                
                // Snap to exact position
                position.x = waypointWorldX;
                position.y = waypointWorldY;
            } else {
                // Set target to next waypoint
                int[] nextWaypoint = path.getCurrentWaypoint();
                if (nextWaypoint != null) {
                    float nextX = nextWaypoint[0] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
                    float nextY = nextWaypoint[1] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
                    movement.setTarget(nextX, nextY, movement.isRunning);
                } else {
                    // No more waypoints
                    path.clear();
                    movement.stopMoving();
                }
            }
        } else {
            // Still moving to current waypoint
            if (!movement.isMoving) {
                movement.setTarget(waypointWorldX, waypointWorldY, movement.isRunning);
            }
        }
    }
    
    private String getIdleAnimationForDirection(int direction) {
        switch(direction) {
            case Movement.DIR_EAST:
                return Sprite.ANIM_IDLE_RIGHT;
            case Movement.DIR_SOUTH_EAST:
                return Sprite.ANIM_IDLE_DOWN_RIGHT;
            case Movement.DIR_SOUTH:
                return Sprite.ANIM_IDLE_DOWN;
            case Movement.DIR_SOUTH_WEST:
                return Sprite.ANIM_IDLE_DOWN_LEFT;
            case Movement.DIR_WEST:
                return Sprite.ANIM_IDLE_LEFT;
            case Movement.DIR_NORTH_WEST:
                return Sprite.ANIM_IDLE_UP_LEFT;
            case Movement.DIR_NORTH:
                return Sprite.ANIM_IDLE_UP;
            case Movement.DIR_NORTH_EAST:
                return Sprite.ANIM_IDLE_UP_RIGHT;
            default:
                return Sprite.ANIM_IDLE_DOWN;
        }
    }

        private String getVictoryAnimationForDirection(int direction) {
            switch(direction) {
                case Movement.DIR_EAST:
                    return Sprite.ANIM_VICTORY_IDLE_RIGHT;
                case Movement.DIR_SOUTH_EAST:
                    return Sprite.ANIM_VICTORY_IDLE_DOWN_RIGHT;
                case Movement.DIR_SOUTH:
                    return Sprite.ANIM_VICTORY_IDLE_DOWN;
                case Movement.DIR_SOUTH_WEST:
                    return Sprite.ANIM_VICTORY_IDLE_DOWN_LEFT;
                case Movement.DIR_WEST:
                    return Sprite.ANIM_VICTORY_IDLE_LEFT;
                case Movement.DIR_NORTH_WEST:
                    return Sprite.ANIM_VICTORY_IDLE_UP_LEFT;
                case Movement.DIR_NORTH:
                    return Sprite.ANIM_VICTORY_IDLE_UP;
                case Movement.DIR_NORTH_EAST:
                    return Sprite.ANIM_VICTORY_IDLE_UP_RIGHT;
                default:
                    return Sprite.ANIM_VICTORY_IDLE_DOWN;
            }
        }
    
    private void moveTowardsTarget(Entity entity, Movement movement, Position position, float delta) {
        float dx = movement.targetX - position.x;
        float dy = movement.targetY - position.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 2f) {
            position.x = movement.targetX;
            position.y = movement.targetY;
            movement.stopMoving();
            return;
        }
        
        float moveAmount = movement.currentSpeed * delta;
        
        if (moveAmount >= distance) {
            // Would reach destination - check if destination is valid
            CollisionBox collisionBox = entity.getComponent(CollisionBox.class);
            TileMap map = state.getMap();
            
            if (collisionBox != null && map != null) {
                if (map.collidesWithTiles(collisionBox, movement.targetX, movement.targetY)) {
                    // Destination is blocked, stop here
                    movement.stopMoving();
                    return;
                }
            }
            
            position.x = movement.targetX;
            position.y = movement.targetY;
            movement.stopMoving();
        } else {
            // Moving partial distance
            float ratio = moveAmount / distance;
            float newX = position.x + dx * ratio;
            float newY = position.y + dy * ratio;
            
            // Check collision at new position
            CollisionBox collisionBox = entity.getComponent(CollisionBox.class);
            TileMap map = state.getMap();
            
            if (collisionBox != null && map != null) {
                // Try moving on both axes
                if (!map.collidesWithTiles(collisionBox, newX, newY)) {
                    // No collision, move normally
                    position.x = newX;
                    position.y = newY;
                } else {
                    // Collision detected - try sliding along walls
                    
                    // Try X axis only
                    if (!map.collidesWithTiles(collisionBox, newX, position.y)) {
                        position.x = newX;
                    }
                    // Try Y axis only
                    else if (!map.collidesWithTiles(collisionBox, position.x, newY)) {
                        position.y = newY;
                    }
                    // Completely blocked
                    else {
                        movement.stopMoving();
                        return;
                    }
                }
            } else {
                // No collision box, move freely
                position.x = newX;
                position.y = newY;
            }
            
            movement.direction = calculateDirection(dx, dy);
        }
    }
    
    private int calculateDirection(float dx, float dy) {
        // Returns 0-7 for 8 directions
        // 0=East, 1=SE, 2=South, 3=SW, 4=West, 5=NW, 6=North, 7=NE
        double angle = Math.atan2(dy, dx);
        int direction = (int) Math.round(angle / (Math.PI / 4));
        return (direction + 8) % 8;
    }
    
    private String getWalkAnimationForDirection(int direction) {
        switch(direction) {
            case Movement.DIR_EAST:
                return Sprite.ANIM_WALK_RIGHT;
            case Movement.DIR_SOUTH_EAST:
                return Sprite.ANIM_WALK_DOWN_RIGHT;
            case Movement.DIR_SOUTH:
                return Sprite.ANIM_WALK_DOWN;
            case Movement.DIR_SOUTH_WEST:
                return Sprite.ANIM_WALK_DOWN_LEFT;
            case Movement.DIR_WEST:
                return Sprite.ANIM_WALK_LEFT;
            case Movement.DIR_NORTH_WEST:
                return Sprite.ANIM_WALK_UP_LEFT;
            case Movement.DIR_NORTH:
                return Sprite.ANIM_WALK_UP;
            case Movement.DIR_NORTH_EAST:
                return Sprite.ANIM_WALK_UP_RIGHT;
            default:
                return Sprite.ANIM_WALK_DOWN;
        }
    }
    
    private String getRunAnimationForDirection(int direction) {
        switch(direction) {
            case Movement.DIR_EAST:
                return Sprite.ANIM_RUN_RIGHT;
            case Movement.DIR_SOUTH_EAST:
                return Sprite.ANIM_RUN_DOWN_RIGHT;
            case Movement.DIR_SOUTH:
                return Sprite.ANIM_RUN_DOWN;
            case Movement.DIR_SOUTH_WEST:
                return Sprite.ANIM_RUN_DOWN_LEFT;
            case Movement.DIR_WEST:
                return Sprite.ANIM_RUN_LEFT;
            case Movement.DIR_NORTH_WEST:
                return Sprite.ANIM_RUN_UP_LEFT;
            case Movement.DIR_NORTH:
                return Sprite.ANIM_RUN_UP;
            case Movement.DIR_NORTH_EAST:
                return Sprite.ANIM_RUN_UP_RIGHT;
            default:
                return Sprite.ANIM_RUN_DOWN;
        }
    }
    private void updateCamera(float delta) {
        Entity player = state.getPlayer();
        Position playerPos = player.getComponent(Position.class);
        TileMap map = state.getMap();
        
        if (playerPos != null && map != null) {
            float targetX = playerPos.x - Engine.WIDTH / 2;
            float targetY = playerPos.y - Engine.HEIGHT / 2;
            
            // Clamp to map bounds
            float maxCameraX = map.getWidthInPixels() - Engine.WIDTH;
            float maxCameraY = map.getHeightInPixels() - Engine.HEIGHT;
            
            targetX = Math.max(0, Math.min(targetX, maxCameraX));
            targetY = Math.max(0, Math.min(targetY, maxCameraY));
            
            // Smooth lerp
            float currentX = state.getCameraX();
            float currentY = state.getCameraY();
            
            float newX = currentX + (targetX - currentX) * cameraLerpSpeed * delta;
            float newY = currentY + (targetY - currentY) * cameraLerpSpeed * delta;
            
            state.setCameraPosition(newX, newY);
        }
    } 
    
    public void movePlayerTo(float worldX, float worldY, boolean run) {
        Entity player = state.getPlayer();
        Position position = player.getComponent(Position.class);
        Movement movement = player.getComponent(Movement.class);
        Path path = player.getComponent(Path.class);
        TargetIndicator indicator = player.getComponent(TargetIndicator.class);  // NEW
        
        if (position == null || movement == null || path == null) {
            return;
        }
        
        // Determine smart starting point for pathfinding
        int startTileX, startTileY;
        
        if (path.isFollowing && path.waypoints != null && path.currentWaypoint < path.waypoints.size()) {
            int[] currentWaypoint = path.waypoints.get(path.currentWaypoint);
            startTileX = currentWaypoint[0];
            startTileY = currentWaypoint[1];
        } else {
            startTileX = (int)(position.x / TileMap.TILE_SIZE);
            startTileY = (int)(position.y / TileMap.TILE_SIZE);
        }
        
        int goalTileX = (int)(worldX / TileMap.TILE_SIZE);
        int goalTileY = (int)(worldY / TileMap.TILE_SIZE);
        
        // Check if clicking the same tile
        if (path.isFollowing && path.waypoints != null && !path.waypoints.isEmpty()) {
            int[] lastWaypoint = path.waypoints.get(path.waypoints.size() - 1);
            if (lastWaypoint[0] == goalTileX && lastWaypoint[1] == goalTileY) {
                movement.isRunning = run;
                return;
            }
        }
        
        // Find new path
        Pathfinder pathfinder = state.getPathfinder();
        List<int[]> foundPath = pathfinder.findPath(startTileX, startTileY, goalTileX, goalTileY);
        
        if (foundPath != null) {
            path.setPath(foundPath);
            movement.isRunning = run;
            
            // Set target indicator at final destination
            if (indicator != null) {
                indicator.setTarget(worldX, worldY);
            }
            
            //System.out.println("Path found with " + foundPath.size() + " waypoints");
        } else {
           // System.out.println("No path to destination!");
            path.clear();
            movement.stopMoving();
            
            if (indicator != null) {
                indicator.clear();
            }
        }
    }
    public void setCameraLerpSpeed(float speed) {
        this.cameraLerpSpeed = speed;
    }
 // Replace these methods in GameLogic.java

    /**
     * Calculate XP reward for killing a monster
     * Uses MonsterLevel component if available, otherwise falls back to old formula
     */
    private int calculateMonsterXP(Entity monster) {
    	// NEW: Use MonsterLevel component for XP calculation
    	MonsterLevel monsterLevel = monster.getComponent(MonsterLevel.class);
        if (monsterLevel != null) {
            return monsterLevel.getXPReward();  // ★ Use getter, not calculate
        }
        
        // OLD: Fallback for monsters without MonsterLevel component
        Stats stats = monster.getComponent(Stats.class);
        if (stats == null) return 10;
        
        int baseXP = (int)(stats.maxHp * 0.5f + stats.attack * 2);
        
        // Boss monsters give bonus XP
        String monsterType = monster.getName();
        if (monsterType.contains("Boss")) {
            baseXP *= 3;
        }
        
        return Math.max(10, baseXP);
    }
    /**
     * Use a skill - handles different skill types
     */
    public void useSkill(Entity caster, Skill skill) {
        if (skill == null || !skill.isReady()) {
            return;
        }
        
        Stats stats = caster.getComponent(Stats.class);
        if (stats == null) return;
        
        // ☆ NEW: Check mana cost
        int manaCost = skill.calculateManaCost(stats.maxMana);
        
        if (stats.mana < manaCost) {
            System.out.println("Not enough mana! Need " + manaCost + ", have " + stats.mana);
            return;
        }
        
        // ☆ NEW: Consume mana
        if (!stats.consumeMana(manaCost)) {
            return;  // Failed to consume mana
        }
        
        // Use the skill (start cooldown)
        if (!skill.use()) {
            return;  // Skill not ready
        }
        
        System.out.println("Used " + skill.getName() + " - Cost: " + manaCost + " mana (" + 
                           stats.mana + "/" + stats.maxMana + " remaining)");
        
        // Apply skill effect based on type
        switch (skill.getType()) {
            case HEAL:
                castHeal(caster, skill, stats);
                break;
                
            case ATTACK:
                // TODO: Implement attack skills
                System.out.println("Attack skill used: " + skill.getName());
                break;
                
            case DEFENSE:
                // TODO: Implement defense skills
                System.out.println("Defense skill used: " + skill.getName());
                break;
                
            case BUFF:
                // TODO: Implement buff skills
                System.out.println("Buff skill used: " + skill.getName());
                break;
                
            case PASSIVE:
                // Passives don't have active effects
                break;
        }
    }

    /**
     * Cast heal skill
     * Formula: Heals (10% + 1.5% * skillLevel) of max HP
     */
    private void castHeal(Entity caster, Skill skill, Stats stats) {
        // Calculate heal amount
        int healAmount = skill.calculateHealAmount(stats.maxHp);
        
        // Apply healing
        int oldHp = stats.hp;
        stats.hp = Math.min(stats.maxHp, stats.hp + healAmount);
        int actualHealed = stats.hp - oldHp;
        
        // Visual feedback
        Position pos = caster.getComponent(Position.class);
        if (pos != null) {
            DamageText healText = new DamageText(
                "+" + actualHealed,
                DamageText.Type.HEAL,
                pos.x,
                pos.y - 30
            );
            state.addDamageText(healText);
        }
        
        // Console feedback
        double healPercent = skill.getHealPercent() * 100;
        System.out.println("═══════════════════════════════");
        System.out.println("HEAL CAST!");
        System.out.println("Skill Level: " + skill.getSkillLevel());
        System.out.println("Heal Power: " + String.format("%.1f", healPercent) + "%");
        System.out.println("Healed: " + actualHealed + " HP");
        System.out.println("HP: " + oldHp + " → " + stats.hp + "/" + stats.maxHp);
        System.out.println("═══════════════════════════════");
    }
    /**
     * Award XP to player and handle level-ups
     */
    private void awardExperience(Entity player, int xpAmount) {
        Experience exp = player.getComponent(Experience.class);
        Stats stats = player.getComponent(Stats.class);
        SkillLevel skillLevel = player.getComponent(SkillLevel.class);
        LevelUpEffect levelUpEffect = player.getComponent(LevelUpEffect.class);
        BuffManager buffManager = player.getComponent(BuffManager.class);
        
        if (exp == null || stats == null) return;
        
        // NEW: Apply EXP boost from buffs
        float expBoost = buffManager != null ? buffManager.getTotalExpBoost() : 0f;
        int finalXP = (int)(xpAmount * (1.0f + expBoost));
        
        if (expBoost > 0) {
            System.out.println("EXP with bonus: " + xpAmount + " → " + finalXP + 
                             " (+" + (int)(expBoost * 100) + "%)");
        }
        
        System.out.println("Gained " + xpAmount + " XP!");
        
        // Add XP and check for level-ups
        int levelsGained = exp.addExperience(xpAmount);
        
        if (levelsGained > 0) {
            // ★ Recalculate stats with new level AND fully heal
            stats.applyLevelStats(exp, true);  // true = full heal on level up
            
            // ★ Award skill points (1 per level)
            if (skillLevel != null) {
                exp.awardSkillPoints(levelsGained, skillLevel);
            }
            
            // Trigger level-up effect
            if (levelUpEffect != null) {
                levelUpEffect.trigger(exp.level);
            }
            
            // Spawn level-up text
            Position pos = player.getComponent(Position.class);
            if (pos != null) {
                DamageText levelText = new DamageText(
                    "LEVEL UP! " + exp.level,
                    DamageText.Type.HEAL,
                    pos.x,
                    pos.y - 40
                );
                state.addDamageText(levelText);
                
                // ★ Spawn "FULLY HEALED!" text below level up
                DamageText healText = new DamageText(
                    "FULLY HEALED!",
                    DamageText.Type.HEAL,
                    pos.x,
                    pos.y - 20
                );
                state.addDamageText(healText);
                
                // ★ NEW: Spawn skill point text
                if (skillLevel != null) {
                    DamageText spText = new DamageText(
                        "+" + levelsGained + " SKILL POINT!",
                        DamageText.Type.HEAL,
                        pos.x,
                        pos.y
                    );
                    state.addDamageText(spText);
                }
            }
            
            
            // Notify UI of level up
            state.getUIManager().notifyLevelUp();
            
            System.out.println("╔════════════════════════════════╗");
            System.out.println("║        LEVEL UP!               ║");
            System.out.println("╠════════════════════════════════╣");
            System.out.println("║ New Level: " + exp.level);
            System.out.println("║ HP:        " + stats.hp + "/" + stats.maxHp + " (FULL!)");
            System.out.println("║ Stamina:   " + (int)stats.stamina + "/" + (int)stats.maxStamina + " (FULL!)");
            System.out.println("║ Attack:    " + stats.attack);
            System.out.println("║ Defense:   " + stats.defense);
            System.out.println("║ Accuracy:  " + stats.accuracy);
            if (skillLevel != null) {
                System.out.println("║ Skill Points: " + skillLevel.availablePoints);
            }
            System.out.println("╚════════════════════════════════╝");
        }
        
        // Show XP progress
        System.out.println("XP: " + (int)exp.currentXP + "/" + (int)exp.xpToNextLevel + 
                           " (" + (int)(exp.getXPProgress() * 100) + "%)");
    } 
    /**
     * Handle monster death with drops
     * REPLACE your existing handleMonsterDeath() method with this:
     */
    private void handleMonsterDeath(Entity monster, Sprite sprite) {
        // Get monster info for logging
        MonsterLevel monsterLevel = monster.getComponent(MonsterLevel.class);
        String monsterInfo = monster.getName();
        if (monsterLevel != null) {
            monsterInfo += " Lv" + monsterLevel.level + " " + monsterLevel.tier;
        }
        
        System.out.println(monsterInfo + " has died!");
        
        // ★ NEW: Generate drops
        int dropCapacity = calculateDropCapacity(monster);
        List<DroppedItem> drops = dropSystem.generateDrops(dropCapacity);
        
        // ★ NEW: Display drops to player
        if (!drops.isEmpty()) {
            if (dropSystem.isLuckyDrop(drops)) {
                System.out.println("✨ ✨ ✨ LUCKY DROP! ✨ ✨ ✨");
            }
            
            System.out.println(monsterInfo + " dropped:");
            for (DroppedItem drop : drops) {
                System.out.println("  • " + drop.toString());
            }
            
            // ★ NEW: Add items to player inventory (with stacking)
            Entity player = state.getPlayer();
            addDropsToInventory(player, drops);
        } else {
            System.out.println("  No drops...");
        }
        
        // Award XP to player
        Entity player = state.getPlayer();
        int xpReward = calculateMonsterXP(monster);
        System.out.println("→ XP Reward: " + xpReward);
        awardExperience(player, xpReward);
        
        // Notify buffs of kill
        BuffManager buffManager = player.getComponent(BuffManager.class);
        if (buffManager != null) {
            buffManager.onMonsterKill();
        }
        
        // Update quest progress
        updateQuestProgress(player, monster);
        
        // Clear as auto-attack target
        if (state.getAutoAttackTarget() == monster) {
            state.clearAutoAttackTarget();
        }
        
        // Clear as targeted entity
        if (state.getTargetedEntity() == monster) {
            state.setTargetedEntity(null);
        }
        
        // Notify spawn point of death
        state.onMonsterDeath(monster);
        
        // Add dead component
        monster.addComponent(new Dead(1.5f));
        
        // Stop movement
        Movement movement = monster.getComponent(Movement.class);
        if (movement != null) {
            movement.stopMoving();
        }
        
        Path path = monster.getComponent(Path.class);
        if (path != null) {
            path.clear();
        }
        
        // Set AI to dead state
        AI ai = monster.getComponent(AI.class);
        if (ai != null) {
            ai.currentState = AI.State.DEAD;
        }
        
        // Play death animation
        if (sprite != null) {
            sprite.setAnimation(Sprite.ANIM_DEAD);
        }
    }

    /**
     * ★ NEW: Calculate drop capacity based on monster tier
     */
    private int calculateDropCapacity(Entity monster) {
        MonsterLevel monsterLevel = monster.getComponent(MonsterLevel.class);
        
        if (monsterLevel == null) {
            return 2; // Default for monsters without level
        }
        
        // More powerful monsters drop more items
        switch (monsterLevel.tier) {
            case TRASH:
                return 2;  // Max 2 different items
            case NORMAL:
                return 3;  // Max 3 different items
            case ELITE:
                return 4;  // Max 4 different items
            case MINIBOSS:
                return 5;  // Max 5 different items
            default:
                return 2;
        }
    }
    /**
     * ★ UPDATED: Add dropped items to player inventory with stacking support
     */
    private void addDropsToInventory(Entity player, List<DroppedItem> drops) {
        UIManager uiManager = state.getUIManager();
        UIScrollableInventoryPanel inventory = uiManager.getInventoryGrid();
        
        if (inventory == null) {
            System.out.println("⚠ Inventory not initialized!");
            return;
        }
        
        int itemsAdded = 0;
        int totalQuantity = 0;
        int itemsFailed = 0;
        
        for (DroppedItem drop : drops) {
            int quantity = drop.getQuantity();
            totalQuantity += quantity;
            
            // Create one item template
            Item itemTemplate = drop.getDropTemplate().createItem();
            
            // ★ NEW: Use stacking system
            if (itemTemplate.isStackable()) {
                // Add as stack
                boolean success = inventory.addItemStack(itemTemplate, quantity);
                if (success) {
                    itemsAdded += quantity;
                    //notify inventory when an item is added
                    uiManager.notifyInventoryUpdate();
                    uiManager.getInventoryGrid().addItemToCurrentTab(itemTemplate, true);
                } else {
                    itemsFailed += quantity;
                }
            } else {
                // Add individually (non-stackable like gear)
                for (int i = 0; i < quantity; i++) {
                    Item item = drop.getDropTemplate().createItem();
                    boolean added = uiManager.addItemToInventory(item);
                    if (added) {
                        itemsAdded++;
                      //notify inventory when an item is added
                        uiManager.notifyInventoryUpdate();
                        uiManager.getInventoryGrid().addItemToCurrentTab(itemTemplate, true);
                         
                    } else {
                        itemsFailed++;
                    }
                }
            }
        }
        
        if (itemsAdded > 0) {
            System.out.println("✓ Added " + itemsAdded + " items to inventory");
        }
        
        if (itemsFailed > 0) {
            System.out.println("⚠ " + itemsFailed + " items lost (inventory full!)");
        }
    }
    /**
     * Update quest progress when monster is killed
     */
    private void updateQuestProgress(Entity player, Entity monster) {
        QuestLog questLog = player.getComponent(QuestLog.class);
        if (questLog == null) return;
        
        String monsterName = monster.getName();
        
        // Update all active quests
        for (Quest quest : questLog.getActiveQuests()) {
            // Check each objective
            for (QuestObjective objective : quest.getObjectives()) {
                // Match objective ID with monster name
                // e.g., "kill_goblins" matches "Goblin"
                String objectiveId = objective.getId();
                
                if (objectiveId.contains("kill")) {
                    // Extract monster type from objective ID
                    // "kill_goblins" -> "goblin"
                    String targetMonster = objectiveId.replace("kill_", "").replace("s", "");
                    
                    if (monsterName.toLowerCase().contains(targetMonster)) {
                        questLog.updateQuestProgress(objectiveId, 1);
                        System.out.println("Quest progress: " + quest.getName() + " - " + objective.getDescription());
                    }
                }
            }
        }
    }

    /**
     * ⭐ NEW: Update all NPC quest indicators
     * Call this when quest status changes
    
    private void updateNPCQuestIndicators() {
        for (Entity entity : state.getEntities()) { 
            if (entity.getType() == EntityType.NPC) {
                NPC npcComponent = entity.getComponent(NPC.class);
                QuestIndicator questIndicator = entity.getComponent(QuestIndicator.class);
                	
                if (npcComponent != null && questIndicator != null) {
                    updateNPCIndicator(entity, npcComponent, questIndicator);  
                }
            }
        }
    }

    /**
     * ⭐ NEW: Update individual NPC quest indicator
      
    private void updateNPCIndicator(Entity npc, NPC npcComponent, QuestIndicator questIndicator) {
        // Check quest status
        Quest completedQuest = npcComponent.getCompletedQuest();
        if (completedQuest != null) {
            // Show "?" for quest completion
            questIndicator.show(QuestIndicator.IndicatorType.COMPLETE);
            return;
        }
        
        Quest activeQuest = npcComponent.getActiveQuest();
        if (activeQuest != null) {
            // Show "..." for quest in progress
            questIndicator.show(QuestIndicator.IndicatorType.IN_PROGRESS);
            return;
        }
        
        Quest availableQuest = npcComponent.getNextAvailableQuest();
        if (availableQuest != null) {
            // Show "!" for available quest
            questIndicator.show(QuestIndicator.IndicatorType.AVAILABLE);
            return;
        }
        
        // No quests - hide indicator
        questIndicator.hide();
    }
    */
    
} 