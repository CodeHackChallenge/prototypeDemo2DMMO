package dev.main.state;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import dev.main.Engine;
import dev.main.ai.AI; 
import dev.main.buffs.BuffManager;
import dev.main.drops.DropSystem;
import dev.main.drops.DroppedItem;
import dev.main.drops.ZoneLootConfig.GuaranteedDrop;
import dev.main.entity.Combat;
import dev.main.entity.Entity;
import dev.main.entity.EntityType;
import dev.main.entity.Experience;
import dev.main.entity.LevelUpEffect;
import dev.main.entity.MonsterLevel; 
import dev.main.entity.Portal;
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
    
    // ★ NEW: Camera lerp control
    private boolean cameraLerpEnabled = true;
    
    private GameState state;
    private float cameraLerpSpeed = 5f;
    
    // ★ NEW: Stuck detection
    private static final float STUCK_TIMEOUT = 0.5f; // Half second without movement = stuck
    private static final float MIN_MOVEMENT = 2f; // Minimum pixels to consider "moved"
    
    //for intro quest collect recipies
    private boolean isIntroQuestCollect = false;
    
    private UIManager ui;
    
    public GameLogic(GameState state) {
        this.state = state;
        this.dropSystem = new DropSystem();
        this.ui = state.getUIManager();
        
        // ★ NEW: Set zone loot config if available
        if (state.getZoneLootConfig() != null) {
            dropSystem.setZoneLootConfig(state.getZoneLootConfig());
        }
    } 

    public void update(float delta) {
    	
        state.incrementGameTime(delta);
        state.updatePortalCooldown(delta);  // ★ ADD THIS
        state.updateTransition(delta);  // ★ NEW: Update transition
        
        // ★ NEW: Skip game updates during transition
        if (state.isInputBlocked()) {
            return;
        }
        
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
            
            // ★ ADD: Update portals
            if (entity.getType() == EntityType.PORTAL) {
                Portal portal = entity.getComponent(Portal.class);
                if (portal != null) {
                    portal.update(delta);
                }
            }else if (entityType == EntityType.PLAYER) {
                updatePlayer(entity, delta);
            } else if (entityType == EntityType.MONSTER) {
                updateMonster(entity, playerPos, delta);
            } 
            else if (entityType == EntityType.NPC) {
               // updateNPC(entity, player, delta);
            }
            
            Sprite sprite = entity.getComponent(Sprite.class);
            if (sprite != null) {
                sprite.update(delta);
            } 
            
            QuestIndicator questIndicator = entity.getComponent(QuestIndicator.class);
            if (questIndicator != null) { 
                questIndicator.update(delta); 
            }
          
            TargetIndicator indicator = entity.getComponent(TargetIndicator.class);
            if (indicator != null) {
                indicator.update(delta);
            }
            
            LevelUpEffect levelUpEffect = entity.getComponent(LevelUpEffect.class);
            if (levelUpEffect != null) {
                levelUpEffect.update(delta);
            }
        }
        
        // ★ ADD: Check portal collisions after player update
        checkPortalCollisions();
        
        updateQuestIndicators(delta);
        state.updateDamageTexts(delta);
        state.updateSpawnPoints(delta);
        state.removeMarkedEntities();
        updateCamera(delta);
    }
 // ★ UPDATED: Check portal collisions with camera control
    private void checkPortalCollisions() {
        Entity player = state.getPlayer();
        Position playerPos = player.getComponent(Position.class);
        CollisionBox playerBox = player.getComponent(CollisionBox.class);
        
        if (playerPos == null || playerBox == null) return;
        if (!state.isPortalReady()) return;
        
        // ★ NEW: Don't check portals during transition
        if (state.isInputBlocked()) return;
        
        for (Entity entity : state.getEntities()) {
            if (entity.getType() != EntityType.PORTAL) continue;
            
            Portal portal = entity.getComponent(Portal.class);
            Position portalPos = entity.getComponent(Position.class);
            CollisionBox portalBox = entity.getComponent(CollisionBox.class);
            
            if (portal == null || !portal.isActive) continue;
            if (portalPos == null || portalBox == null) continue;
            
            if (playerBox.overlaps(playerPos.x, playerPos.y, portalBox, portalPos.x, portalPos.y)) {
                System.out.println("🌀 Portal activated: " + portal.id);
                
                // ★ NEW: Disable camera lerp before teleport
                disableCameraLerp();
                
                // ★ NEW: Start transition instead of instant teleport
                state.startPortalTransition(portal.targetMap, portal.targetX, portal.targetY);
                state.setPortalCooldown();
                
                // Stop player movement
                Movement movement = player.getComponent(Movement.class);
                if (movement != null) {
                    movement.stopMoving();
                }
                
                Path path = player.getComponent(Path.class);
                if (path != null) {
                    path.clear();
                }
                
                break;
            }
        }
    }
    /**
     * ★ NEW: Find path with collision box awareness
     * Use this instead of calling pathfinder.findPath() directly
     */
    private List<int[]> findPathForEntity(Entity entity, int startX, int startY, int goalX, int goalY) {
        Pathfinder pathfinder = state.getPathfinder();
        CollisionBox collisionBox = entity.getComponent(CollisionBox.class);
        
        // Set collision box for pathfinding
        if (collisionBox != null) {
            pathfinder.setCollisionBox(collisionBox);
        }
        
        // Find path
        List<int[]> path = pathfinder.findPath(startX, startY, goalX, goalY);
        
        // Clear collision box
        pathfinder.clearCollisionBox();
        
        return path;
    }
    
    private void updateQuestIndicators(float delta) {
        for (Entity entity : state.getEntities()) {
            QuestIndicator indicator = entity.getComponent(QuestIndicator.class);
            if (indicator != null) {
                indicator.update(delta);
            }
        }
    }

    public void playerAttack(Entity target) {
        Entity player = state.getPlayer();
        Combat playerCombat = player.getComponent(Combat.class);
        Position playerPos = player.getComponent(Position.class);
        Position targetPos = target.getComponent(Position.class);
        Movement playerMovement = player.getComponent(Movement.class);
        Stats playerStats = player.getComponent(Stats.class);
        Path playerPath = player.getComponent(Path.class);
        TargetIndicator indicator = player.getComponent(TargetIndicator.class);
        
        if (playerCombat == null || playerStats == null) return;
        if (playerPos == null || targetPos == null) return;
        
        if (playerPath != null) {
            playerPath.clear();
        }
        if (indicator != null) {
            indicator.clear();
        }
        //TODO: must check if player has equipped weapon to attack
//        if(!ui.isWeaponEquipped()) {
//        	System.out.println("You must equipped a weapon to attack");
//        	return;
//        }
         
        state.setAutoAttackTarget(target);
        
        float distance = distance(playerPos.x, playerPos.y, targetPos.x, targetPos.y);
        
        if (distance <= 80f && playerCombat.canAttackWithStamina(playerStats)) {
            playerMovement.direction = calculateDirection(targetPos.x - playerPos.x, targetPos.y - playerPos.y);
            playerMovement.lastDirection = playerMovement.direction;
            
            playerMovement.stopMoving();
            
            if (playerStats.consumeStaminaForAttack()) {
                playerCombat.startAttack(target);
            } else {
                System.out.println("Not enough stamina to attack!");
            }
        } else if (distance > 80f) {
            // Path will be created in update loop
        } else {
            System.out.println("Not enough stamina to attack!");
        }
    }

    public void stopAutoAttack() {
        state.clearAutoAttackTarget();
        
        Entity player = state.getPlayer();
        TargetIndicator indicator = player.getComponent(TargetIndicator.class);
        if (indicator != null) {
            indicator.clear();
        }
    }

    private void performAttack(Entity attacker, Entity target, Position attackerPos, Position targetPos) {
        Stats attackerStats = attacker.getComponent(Stats.class);
        Stats targetStats = target.getComponent(Stats.class);
        Combat attackerCombat = attacker.getComponent(Combat.class);
        Combat targetCombat = target.getComponent(Combat.class);
        
        if (attackerStats == null || targetStats == null) return;
        
        float evasionRoll = ThreadLocalRandom.current().nextFloat();
        float evasionChance = targetCombat != null ? targetCombat.evasionChance : 0f;
       
        if (evasionRoll < evasionChance) {
            DamageText missText = new DamageText("MISS", DamageText.Type.MISS, targetPos.x, targetPos.y - 30);
            state.addDamageText(missText); 
            reduceDurability();
            return;
        }
        
        boolean isCrit = false;
        float critRoll = ThreadLocalRandom.current().nextFloat();
        float critChance = attackerCombat != null ? attackerCombat.critChance : 0f;
        
        if (critRoll < critChance) {
            isCrit = true;
        }
        
        int baseDamage = attackerStats.attack - targetStats.defense;
        baseDamage = Math.max(1, baseDamage);
        
        if (isCrit && attackerCombat != null) {
            baseDamage = (int)(baseDamage * attackerCombat.critMultiplier);
        } 
        
        targetStats.hp -= baseDamage;
        if (targetStats.hp < 0) targetStats.hp = 0;
        
        DamageText.Type textType;
        
        if (target.getType() == EntityType.PLAYER) {
            if (isCrit) {
                textType = DamageText.Type.PLAYER_CRITICAL_DAMAGE;
            } else {
                textType = DamageText.Type.PLAYER_DAMAGE;
            }
        } else if (isCrit) {
            textType = DamageText.Type.CRITICAL;
        } else {
            textType = DamageText.Type.NORMAL;
        }
        
        DamageText damageText = new DamageText(String.valueOf(baseDamage), textType, targetPos.x, targetPos.y - 30);
        state.addDamageText(damageText);
         
        if (targetStats.hp <= 0) {
            Dead alreadyDead = target.getComponent(Dead.class);
            if (alreadyDead == null) {
                if (target.getType() == EntityType.MONSTER) {
                    handleMonsterDeath(target, target.getComponent(Sprite.class));
                } else if (target.getType() == EntityType.PLAYER) {
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
        if(ui.getGearSlot(UIGearSlot.SlotType.WEAPON) != null &&
           ui.getGearSlot(UIGearSlot.SlotType.WEAPON).getItem() != null &&
           ui.getGearSlot(UIGearSlot.SlotType.WEAPON).getItem().getCurrentDurability() > 0) {
            
        	ui.getGearSlot(UIGearSlot.SlotType.WEAPON).getItem().reduceDurability(1);
            System.out.println("****NOTIFICATION: \n GameLogic.performAttack()>Reduced durability!");
        }
    }

    private void handlePlayerDeath(Entity player, Sprite sprite) {
        System.out.println("Player has died!");
        
        Movement movement = player.getComponent(Movement.class);
        if (movement != null) {
            movement.stopMoving();
        }
        
        Path path = player.getComponent(Path.class);
        if (path != null) {
            path.clear();
        }
        
        state.clearAutoAttackTarget();
        
        if (sprite != null) {
            sprite.setAnimation(Sprite.ANIM_DEAD);
        }
        
        player.addComponent(new Dead(10f));
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
        
        BuffManager buffManager = player.getComponent(BuffManager.class);
        if (buffManager != null) {
            buffManager.update(delta);
            
            float healthRegen = buffManager.getTotalHealthRegen();
            if (healthRegen > 0 && stats.hp < stats.maxHp) {
                stats.hp = Math.min(stats.maxHp, stats.hp + (int)(healthRegen * delta));
            }
            
            float manaBoost = buffManager.getTotalManaRegenBoost();
            if (manaBoost > 0) {
                stats.manaRegenRate += manaBoost;
            }
            
            float staminaBoost = buffManager.getTotalStaminaRegenBoost();
            if (staminaBoost > 0) {
                stats.stamina = Math.min(stats.maxStamina, stats.stamina + staminaBoost * delta);
            }
        }
        
        stats.regenerateMana(delta);
        
        String staminaState = "idle";
        if (movement.isMoving) {
        	// ★ NEW: Re-enable camera lerp when player is moving
            if (!cameraLerpEnabled) {
                enableCameraLerp();
            }
            if (movement.isRunning) {
                staminaState = "running";
                
                if (stats.stamina <= 0) {
                    movement.stopRunning();
                    System.out.println("Out of stamina! Can't run.");
                }
            } else {
                staminaState = "walking";
            }
        }
        stats.regenerateStaminaByState(staminaState, delta);
        
        if (combat != null && combat.shouldDealDamage() && combat.attackTarget != null) {
            Position targetPos = combat.attackTarget.getComponent(Position.class);
            if (targetPos != null) {
                performAttack(player, combat.attackTarget, position, targetPos);
            }
        }
        
        // ★ NEW: Stuck detection and recovery
        if (path != null && path.isFollowing) {
            // Update stuck timer
            path.stuckTimer += delta;
            
            // Check if we've moved recently
            float distMoved = distance(position.x, position.y, path.lastPositionX, path.lastPositionY);
            
            if (distMoved > MIN_MOVEMENT) {
                // We moved - reset stuck timer and update position
                path.stuckTimer = 0f;
                path.lastPositionX = position.x;
                path.lastPositionY = position.y;
            } else if (path.stuckTimer >= STUCK_TIMEOUT) {
                // We're stuck - try to recover
                System.out.println("⚠ Player stuck! Attempting recovery...");
                
                // Try to recalculate path from current position
                if (path.waypoints != null && !path.waypoints.isEmpty()) {
                    int[] finalGoal = path.waypoints.get(path.waypoints.size() - 1);
                    
                    int startTileX = (int)(position.x / TileMap.TILE_SIZE);
                    int startTileY = (int)(position.y / TileMap.TILE_SIZE);
                    
                    // ★ REPLACE THIS LINE:
                    // List<int[]> newPath = state.getPathfinder().findPath(startTileX, startTileY, finalGoal[0], finalGoal[1]);
                    
                    // ★ WITH THIS LINE:
                    List<int[]> newPath = findPathForEntity(player, startTileX, startTileY, finalGoal[0], finalGoal[1]);
                    
                    
                    if (newPath != null && newPath.size() > 1) {
                        // Found new path - use it
                        path.setPath(newPath);
                        path.stuckTimer = 0f;
                        path.lastPositionX = position.x;
                        path.lastPositionY = position.y;
                        System.out.println("✓ Found alternative path with " + newPath.size() + " waypoints");
                    } else {
                        // Can't find path - give up
                        System.out.println("✗ No path available - stopping movement");
                        path.clear();
                        movement.stopMoving();
                        if (indicator != null) {
                            indicator.clear();
                        }
                    }
                }
                
                path.stuckTimer = 0f;
            }
        }
        
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
                        if (combat != null && combat.canAttackWithStamina(stats) && !combat.isAttacking) {
                            float dx = targetPos.x - position.x;
                            float dy = targetPos.y - position.y;
                            movement.direction = calculateDirection(dx, dy);
                            movement.lastDirection = movement.direction;
                            
                            movement.stopMoving();
                            if (path != null) path.clear();
                            if (indicator != null) indicator.clear();
                            
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
                            
                            List<int[]> foundPath = findPathForEntity(player, startTileX, startTileY, goalTileX, goalTileY);
                            
                            if (foundPath != null && path != null) {
                                path.setPath(foundPath);
                                movement.isRunning = false;
                                
                                // ★ NEW: Re-enable camera lerp
                                enableCameraLerp();
                            }
                        }
                    }
                }
            }
        }
        
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
    // ★ NEW: Camera lerp control methods
    public void enableCameraLerp() {
        if (!cameraLerpEnabled) {
            cameraLerpEnabled = true;
            System.out.println("📹 Camera lerp enabled");
        }
    }
    
    public void disableCameraLerp() {
        if (cameraLerpEnabled) {
            cameraLerpEnabled = false;
            System.out.println("📹 Camera lerp disabled (instant snap)");
        }
    }
    
    public boolean isCameraLerpEnabled() {
        return cameraLerpEnabled;
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
        Alert alert = monster.getComponent(Alert.class);
        
        if (position == null || ai == null) return;
        
        if (dead != null) {
            dead.update(delta);
            if (dead.shouldRemove()) {
                state.markForRemoval(monster);
            }
            return;
        }
        
        if (stats != null && stats.hp <= 0) {
            handleMonsterDeath(monster, sprite);
            return;
        }
        
        if (combat != null && combat.shouldDealDamage() && combat.attackTarget != null) {
            Position targetPos = combat.attackTarget.getComponent(Position.class);
            if (targetPos != null) {
                performAttack(monster, combat.attackTarget, position, targetPos);
            }
        }
        
        ai.update(delta);
        
        if (alert != null) {
            alert.update(delta);
            
            if (ai.currentState == AI.State.CHASING || ai.currentState == AI.State.ATTACKING) {
                alert.show();
            } else {
                alert.hide();
            }
        }
        
        if (combat != null && combat.isAttacking) {
            if (sprite != null && movement != null) {
                sprite.setAnimation(getAttackAnimationForDirection(movement.lastDirection));
            }
        }
        
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
        
        if (movement != null && movement.isMoving) {
            moveTowardsTarget(monster, movement, position, delta);
        }
        
        if (path != null && path.isFollowing) {
            followPath(monster, path, movement, position, delta);
        }
    }

    private void handleVictoryIdleState(Entity monster, AI ai, Movement movement, Sprite sprite, float delta) {
        if (sprite != null && movement != null) {
            sprite.setAnimation(getVictoryAnimationForDirection(movement.lastDirection));
        }
        
        if (ai.victoryIdleTimer >= ai.victoryIdleDuration) {
            Position monsterPos = monster.getComponent(Position.class);
            if (monsterPos != null) {
                float distFromHome = distance(monsterPos.x, monsterPos.y, ai.homeX, ai.homeY);
                
                if (distFromHome <= ai.roamRadius * 1.2f) {
                    transitionAIState(monster, ai, AI.State.IDLE);
                } else {
                    transitionAIState(monster, ai, AI.State.RETURNING);
                }
            } else {
                transitionAIState(monster, ai, AI.State.IDLE);
            }
        }
    }

    private void transitionAIState(Entity entity, AI ai, AI.State newState) {
        if (ai.currentState == newState) return;
        
        AI.State oldState = ai.currentState;
        ai.currentState = newState;
        
        Movement movement = entity.getComponent(Movement.class);
        Path path = entity.getComponent(Path.class);
        
        if (movement != null) {
            movement.stopMoving();
            
            if (oldState == AI.State.RETURNING && newState != AI.State.RETURNING) {
                movement.setHaste(false);
            }
        }
        if (path != null) {
            path.clear();
        }
        
        switch(newState) {
            case IDLE:
                ai.roamTimer = 0;
                ai.roamInterval = ThreadLocalRandom.current().nextFloat(3f, 6f);
                break;
            case VICTORY_IDLE:
                ai.victoryIdleTimer = 0;
                ai.target = null;
                break;
            case RETURNING:
                ai.target = null;
                break;
        }
    }

    private String getAttackAnimationForDirection(int direction) {
        switch(direction) {
            case Movement.DIR_EAST:
                return "attack_right";
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
        
        if (ai.behaviorType.equals("aggressive") && playerPos != null && monsterPos != null) {
            Entity player = state.getPlayer();
            Stats playerStats = player.getComponent(Stats.class);
            
            float distFromHome = distance(monsterPos.x, monsterPos.y, ai.homeX, ai.homeY);
            
            if (playerStats != null && playerStats.hp > 0 && distFromHome <= ai.roamRadius * 1.5f) {
                if (canDetectPlayer(monsterPos, playerPos, ai.detectionRange)) {
                    transitionAIState(monster, ai, AI.State.CHASING);
                    ai.target = state.getPlayer();
                    return;
                }
            }
        }
        
        if (ai.roamTimer >= ai.roamInterval) {
            transitionAIState(monster, ai, AI.State.ROAMING);
        }
        
        if (sprite != null && movement != null) {
            sprite.setAnimation(getIdleAnimationForDirection(movement.lastDirection));
        }
    }

    private void handleRoamingState(Entity monster, AI ai, Movement movement, Position position, Path path, Sprite sprite, Position playerPos, float delta) {
        if (ai.behaviorType.equals("aggressive") && playerPos != null && position != null) {
            Entity player = state.getPlayer();
            Stats playerStats = player.getComponent(Stats.class);
            
            float distFromHome = distance(position.x, position.y, ai.homeX, ai.homeY);
            
            if (playerStats != null && playerStats.hp > 0 && distFromHome <= ai.roamRadius * 1.5f) {
                if (canDetectPlayer(position, playerPos, ai.detectionRange)) {
                    transitionAIState(monster, ai, AI.State.CHASING);
                    ai.target = state.getPlayer();
                    return;
                }
            }
        }
        
        if (movement != null && !movement.isMoving) {
            float angle = (float)(ThreadLocalRandom.current().nextDouble() * Math.PI * 2);
            float distance = ThreadLocalRandom.current().nextFloat(0.5f, 1f) * ai.roamRadius;
            
            float targetX = ai.homeX + (float)Math.cos(angle) * distance;
            float targetY = ai.homeY + (float)Math.sin(angle) * distance;
            
            int startTileX = (int)(position.x / TileMap.TILE_SIZE);
            int startTileY = (int)(position.y / TileMap.TILE_SIZE);
            int goalTileX = (int)(targetX / TileMap.TILE_SIZE);
            int goalTileY = (int)(targetY / TileMap.TILE_SIZE);
            
        	// ★ REPLACE THIS LINE:
            // List<int[]> foundPath = state.getPathfinder().findPath(startTileX, startTileY, goalTileX, goalTileY);
            
            // ★ WITH THIS LINE:
            List<int[]> foundPath = findPathForEntity(monster, startTileX, startTileY, goalTileX, goalTileY);
            
            if (foundPath != null && path != null) {
                path.setPath(foundPath);
                movement.isRunning = false;
            } else {
                transitionAIState(monster, ai, AI.State.IDLE);
            }
        }
        
        if (movement != null && !movement.isMoving && (path == null || !path.isFollowing)) {
            transitionAIState(monster, ai, AI.State.IDLE);
        }
        
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
        
        Entity player = state.getPlayer();
        if (player != null) {
            Stats playerStats = player.getComponent(Stats.class);
            if (playerStats != null && playerStats.hp <= 0) {
                transitionAIState(monster, ai, AI.State.VICTORY_IDLE);
                return;
            }
        }
        
        float distFromHome = distance(position.x, position.y, ai.homeX, ai.homeY);
        if (distFromHome > ai.returnThreshold) {
            transitionAIState(monster, ai, AI.State.RETURNING);
            return;
        }
        
        float distToPlayer = distance(position.x, position.y, playerPos.x, playerPos.y);
        if (distToPlayer > ai.detectionRange * TileMap.TILE_SIZE * 1.5f) {
            transitionAIState(monster, ai, AI.State.RETURNING);
            return;
        }
        
        if (distToPlayer <= ai.attackRange) {
            transitionAIState(monster, ai, AI.State.ATTACKING);
            return;
        }
        
        ai.pathUpdateTimer += delta;
        
        if (!movement.isMoving || ai.pathUpdateTimer >= ai.pathUpdateInterval) {
            ai.pathUpdateTimer = 0;
            
            int startTileX = (int)(position.x / TileMap.TILE_SIZE);
            int startTileY = (int)(position.y / TileMap.TILE_SIZE);
            int goalTileX = (int)(playerPos.x / TileMap.TILE_SIZE);
            int goalTileY = (int)(playerPos.y / TileMap.TILE_SIZE);
            
            // ★ REPLACE THIS LINE:
            // List<int[]> foundPath = state.getPathfinder().findPath(startTileX, startTileY, goalTileX, goalTileY);
            
            // ★ WITH THIS LINE:
            List<int[]> foundPath = findPathForEntity(monster, startTileX, startTileY, goalTileX, goalTileY);
           
            if (foundPath != null && path != null) {
                ai.cachedPath = foundPath;
                path.setPath(foundPath);
                movement.isRunning = true;
            } else {
                transitionAIState(monster, ai, AI.State.RETURNING);
            }
        }
        
        if (sprite != null && movement.isMoving) {
            sprite.setAnimation(getRunAnimationForDirection(movement.direction));
        } else if (sprite != null) {
            sprite.setAnimation(getIdleAnimationForDirection(movement.lastDirection));
        }
    }

    private void handleReturningState(Entity monster, AI ai, Movement movement, Position position, Path path, Sprite sprite, float delta) {
        if (movement == null || position == null) return;
        
        if (!movement.isHasted) {
            movement.setHaste(true);
        }
        
        float distFromHome = distance(position.x, position.y, ai.homeX, ai.homeY);
        if (distFromHome < 32f) {
            movement.setHaste(false);
            
            Stats stats = monster.getComponent(Stats.class);
            if (stats != null && stats.hp < stats.maxHp) {
                int healAmount = stats.maxHp - stats.hp;
                stats.hp = stats.maxHp;
                
                DamageText healText = new DamageText("+" + healAmount, DamageText.Type.HEAL, position.x, position.y - 30);
                state.addDamageText(healText);
            }
            
            transitionAIState(monster, ai, AI.State.IDLE);
            return;
        }
        
        if (!movement.isMoving || (path != null && !path.isFollowing)) {
            int startTileX = (int)(position.x / TileMap.TILE_SIZE);
            int startTileY = (int)(position.y / TileMap.TILE_SIZE);
            int goalTileX = (int)(ai.homeX / TileMap.TILE_SIZE);
            int goalTileY = (int)(ai.homeY / TileMap.TILE_SIZE);
            
            if (startTileX == goalTileX && startTileY == goalTileY) {
                movement.setHaste(false);
                
                Stats stats = monster.getComponent(Stats.class);
                if (stats != null && stats.hp < stats.maxHp) {
                    int healAmount = stats.maxHp - stats.hp;
                    stats.hp = stats.maxHp;
                    
                    DamageText healText = new DamageText("+" + healAmount, DamageText.Type.HEAL, position.x, position.y - 30);
                    state.addDamageText(healText);
                }
                
                transitionAIState(monster, ai, AI.State.IDLE);
                return;
            }
            
            // ★ REPLACE THIS LINE:
            // List<int[]> foundPath = state.getPathfinder().findPath(startTileX, startTileY, goalTileX, goalTileY);
            
            // ★ WITH THIS LINE:
            List<int[]> foundPath = findPathForEntity(monster, startTileX, startTileY, goalTileX, goalTileY);
           
            if (foundPath != null && path != null) {
                path.setPath(foundPath);
                movement.isRunning = false;
            } else {
                movement.setHaste(false);
                transitionAIState(monster, ai, AI.State.IDLE);
            }
        }
        
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
        
        if (player != null) {
            Stats playerStats = player.getComponent(Stats.class);
            if (playerStats != null && playerStats.hp <= 0) {
                transitionAIState(monster, ai, AI.State.VICTORY_IDLE);
                return;
            }
        }
        
        float distToPlayer = distance(monsterPos.x, monsterPos.y, playerPos.x, playerPos.y);
        
        if (distToPlayer > ai.attackRange * 1.5f) {
            transitionAIState(monster, ai, AI.State.CHASING);
            return;
        }
        
        if (ai.canAttack() && combat != null && combat.canAttack()) {
            if (movement != null) {
                float dx = playerPos.x - monsterPos.x;
                float dy = playerPos.y - monsterPos.y;
                movement.direction = calculateDirection(dx, dy);
                movement.lastDirection = movement.direction;
            }
            
            combat.startAttack(player);
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

    /**
     * ★ IMPROVED: Follow path with validation
     */
    private void followPath(Entity entity, Path path, Movement movement, Position position, float delta) {
        int[] waypoint = path.getCurrentWaypoint();
        
        if (waypoint == null) {
            path.clear();
            movement.stopMoving();
            return;
        }
        
        float waypointWorldX = waypoint[0] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
        float waypointWorldY = waypoint[1] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
        
        float dx = waypointWorldX - position.x;
        float dy = waypointWorldY - position.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 5f) {
            path.advanceWaypoint();
            
            if (!path.isFollowing) {
                movement.stopMoving();
                position.x = waypointWorldX;
                position.y = waypointWorldY;
            } else {
                int[] nextWaypoint = path.getCurrentWaypoint();
                if (nextWaypoint != null) {
                    float nextX = nextWaypoint[0] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
                    float nextY = nextWaypoint[1] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
                    movement.setTarget(nextX, nextY, movement.isRunning);
                } else {
                    path.clear();
                    movement.stopMoving();
                }
            }
        } else {
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
    
    /**
     * ★ IMPROVED: Enhanced movement with sliding and better collision handling
     */
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
            CollisionBox collisionBox = entity.getComponent(CollisionBox.class);
            TileMap map = state.getMap();
            
            if (collisionBox != null && map != null) {
                if (map.collidesWithTiles(collisionBox, movement.targetX, movement.targetY)) {
                    movement.stopMoving();
                    return;
                }
            }
            
            position.x = movement.targetX;
            position.y = movement.targetY;
            movement.stopMoving();
        } else {
            float ratio = moveAmount / distance;
            float newX = position.x + dx * ratio;
            float newY = position.y + dy * ratio;
            
            CollisionBox collisionBox = entity.getComponent(CollisionBox.class);
            TileMap map = state.getMap();
            
            if (collisionBox != null && map != null) {
                // ★ NEW: Try full movement first
                if (!map.collidesWithTiles(collisionBox, newX, newY)) {
                    position.x = newX;
                    position.y = newY;
                } else {
                    // ★ IMPROVED: Enhanced sliding with multiple attempts
                    boolean moved = false;
                    
                    // Try X-only slide
                    if (!map.collidesWithTiles(collisionBox, newX, position.y)) {
                        position.x = newX;
                        moved = true;
                    }
                    
                    // Try Y-only slide
                    if (!map.collidesWithTiles(collisionBox, position.x, newY)) {
                        position.y = newY;
                        moved = true;
                    }
                    
                    // ★ NEW: If still blocked, try smaller diagonal movements
                    if (!moved) {
                        // Try 50% movement on both axes
                        float halfX = position.x + dx * ratio * 0.5f;
                        float halfY = position.y + dy * ratio * 0.5f;
                        
                        if (!map.collidesWithTiles(collisionBox, halfX, halfY)) {
                            position.x = halfX;
                            position.y = halfY;
                            moved = true;
                        } else {
                            // Try 25% movement
                            float quarterX = position.x + dx * ratio * 0.25f;
                            float quarterY = position.y + dy * ratio * 0.25f;
                            
                            if (!map.collidesWithTiles(collisionBox, quarterX, quarterY)) {
                                position.x = quarterX;
                                position.y = quarterY;
                                moved = true;
                            }
                        }
                    }
                    
                    // ★ If completely stuck, stop movement
                    // The stuck detection in updatePlayer will handle rerouting
                    if (!moved) {
                        // Don't stop movement - let stuck detection handle it
                        // This prevents instant stopping on first collision
                    }
                }
            } else {
                position.x = newX;
                position.y = newY;
            }
            
            movement.direction = calculateDirection(dx, dy);
        }
    }
    
    private int calculateDirection(float dx, float dy) {
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
    
    // ★ UPDATED: Update camera with lerp control
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
            
            float currentX = state.getCameraX();
            float currentY = state.getCameraY();
            
            // ★ NEW: Use lerp or instant snap based on state
            float newX, newY;
            
            if (cameraLerpEnabled) {
                // Smooth lerp
                newX = currentX + (targetX - currentX) * cameraLerpSpeed * delta;
                newY = currentY + (targetY - currentY) * cameraLerpSpeed * delta;
            } else {
                // Instant snap (no lerp)
                newX = targetX;
                newY = targetY;
            }
            
            state.setCameraPosition(newX, newY);
        }
    }
 // ★ UPDATED: Move player - re-enable camera lerp when player moves
    public void movePlayerTo(float worldX, float worldY, boolean run) {
        Entity player = state.getPlayer();
        Position position = player.getComponent(Position.class);
        Movement movement = player.getComponent(Movement.class);
        Path path = player.getComponent(Path.class);
        TargetIndicator indicator = player.getComponent(TargetIndicator.class);
        
        if (position == null || movement == null || path == null) {
            return;
        }
        
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
        
        if (path.isFollowing && path.waypoints != null && !path.waypoints.isEmpty()) {
            int[] lastWaypoint = path.waypoints.get(path.waypoints.size() - 1);
            if (lastWaypoint[0] == goalTileX && lastWaypoint[1] == goalTileY) {
                movement.isRunning = run;
                return;
            }
        }
        
        List<int[]> foundPath = findPathForEntity(player, startTileX, startTileY, goalTileX, goalTileY);
        
        if (foundPath != null) {
            path.setPath(foundPath);
            movement.isRunning = run;
            
            if (indicator != null) {
                indicator.setTarget(worldX, worldY);
            }
            
            // ★ NEW: Re-enable camera lerp when player starts moving
            enableCameraLerp();
        } else {
            System.out.println("⚠ Cannot path to that location - collision box doesn't fit!");
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

    private int calculateMonsterXP(Entity monster) {
        MonsterLevel monsterLevel = monster.getComponent(MonsterLevel.class);
        if (monsterLevel != null) {
            return monsterLevel.getXPReward();
        }
        
        Stats stats = monster.getComponent(Stats.class);
        if (stats == null) return 10;
        
        int baseXP = (int)(stats.maxHp * 0.5f + stats.attack * 2);
        
        String monsterType = monster.getName();
        if (monsterType.contains("Boss")) {
            baseXP *= 3;
        }
        
        return Math.max(10, baseXP);
    }

    public void useSkill(Entity caster, Skill skill) {
        if (skill == null || !skill.isReady()) {
            return;
        }
        
        Stats stats = caster.getComponent(Stats.class);
        if (stats == null) return;
        
        int manaCost = skill.calculateManaCost(stats.maxMana);
        
        if (stats.mana < manaCost) {
            System.out.println("Not enough mana! Need " + manaCost + ", have " + stats.mana);
            return;
        }
        
        if (!stats.consumeMana(manaCost)) {
            return;
        }
        
        if (!skill.use()) {
            return;
        }
        
        System.out.println("Used " + skill.getName() + " - Cost: " + manaCost + " mana (" + 
                           stats.mana + "/" + stats.maxMana + " remaining)");
        
        switch (skill.getType()) {
            case HEAL:
                castHeal(caster, skill, stats);
                break;
                
            case ATTACK:
                System.out.println("Attack skill used: " + skill.getName());
                break;
                
            case DEFENSE:
                System.out.println("Defense skill used: " + skill.getName());
                break;
                
            case BUFF:
                System.out.println("Buff skill used: " + skill.getName());
                break;
                
            case PASSIVE:
                break;
        }
    }

    private void castHeal(Entity caster, Skill skill, Stats stats) {
        int healAmount = skill.calculateHealAmount(stats.maxHp);
        
        int oldHp = stats.hp;
        stats.hp = Math.min(stats.maxHp, stats.hp + healAmount);
        int actualHealed = stats.hp - oldHp;
        
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
        
        double healPercent = skill.getHealPercent() * 100;
        System.out.println("╔═══════════════════════════════");
        System.out.println("HEAL CAST!");
        System.out.println("Skill Level: " + skill.getSkillLevel());
        System.out.println("Heal Power: " + String.format("%.1f", healPercent) + "%");
        System.out.println("Healed: " + actualHealed + " HP");
        System.out.println("HP: " + oldHp + " → " + stats.hp + "/" + stats.maxHp);
        System.out.println("╚═══════════════════════════════");
    }

    private void awardExperience(Entity player, int xpAmount) {
        Experience exp = player.getComponent(Experience.class);
        Stats stats = player.getComponent(Stats.class);
        SkillLevel skillLevel = player.getComponent(SkillLevel.class);
        LevelUpEffect levelUpEffect = player.getComponent(LevelUpEffect.class);
        BuffManager buffManager = player.getComponent(BuffManager.class);
        
        if (exp == null || stats == null) return;
        
        float expBoost = buffManager != null ? buffManager.getTotalExpBoost() : 0f;
        int finalXP = (int)(xpAmount * (1.0f + expBoost));
        
        if (expBoost > 0) {
            System.out.println("EXP with bonus: " + xpAmount + " → " + finalXP + 
                             " (+" + (int)(expBoost * 100) + "%)");
        }
        
        System.out.println("Gained " + xpAmount + " XP!");
        //handle exp
        //int levelsGained = exp.addExperience(xpAmount);
        int levelsGained = exp.addExperience(finalXP);
        
        
        if (levelsGained > 0) {
            stats.applyLevelStats(exp, true);
            
            if (skillLevel != null) {
                exp.awardSkillPoints(levelsGained, skillLevel);
            }
            
            if (levelUpEffect != null) {
                levelUpEffect.trigger(exp.level);
            }
            
            Position pos = player.getComponent(Position.class);
            if (pos != null) {
                DamageText levelText = new DamageText(
                    "LEVEL UP! " + exp.level,
                    DamageText.Type.HEAL,
                    pos.x,
                    pos.y - 40
                );
                state.addDamageText(levelText);
                
                DamageText healText = new DamageText(
                    "FULLY HEALED!",
                    DamageText.Type.HEAL,
                    pos.x,
                    pos.y - 20
                );
                state.addDamageText(healText);
                
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
            
            state.getUIManager().notifyLevelUp();
            
            System.out.println("╔═══════════════════════════════════");
            System.out.println("║        LEVEL UP!               ║");
            System.out.println("╠═══════════════════════════════════╣");
            System.out.println("║ New Level: " + exp.level);
            System.out.println("║ HP:        " + stats.hp + "/" + stats.maxHp + " (FULL!)");
            System.out.println("║ Stamina:   " + (int)stats.stamina + "/" + (int)stats.maxStamina + " (FULL!)");
            System.out.println("║ Attack:    " + stats.attack);
            System.out.println("║ Defense:   " + stats.defense);
            System.out.println("║ Accuracy:  " + stats.accuracy);
            if (skillLevel != null) {
                System.out.println("║ Skill Points: " + skillLevel.availablePoints);
            }
            System.out.println("╚═══════════════════════════════════");
        }
        
        System.out.println("XP: " + (int)exp.currentXP + "/" + (int)exp.xpToNextLevel + 
                           " (" + (int)(exp.getXPProgress() * 100) + "%)");
    } 

    private void handleMonsterDeath(Entity monster, Sprite sprite) {
        MonsterLevel monsterLevel = monster.getComponent(MonsterLevel.class);
        String monsterInfo = monster.getName();
        
        Entity player = state.getPlayer();
       
        if (monsterLevel != null) {
            monsterInfo += " Lv" + monsterLevel.level + " " + monsterLevel.tier;
        }
        
        System.out.println(monsterInfo + " has died!");
        
        // Get active quest ID if player has intro quest
        String activeQuestId = null;
        QuestLog questLog = player.getComponent(QuestLog.class);
        if (questLog != null) {
        	//System.out.println("::::questLog>>>>>");
            // Check if player has intro_quest_01 active
            for (Quest quest : questLog.getActiveQuests()) {
                if ("collect_recipes".equals(quest.getId())) {
                	//System.out.println("::::quest.getId: "+quest.getId());
                    activeQuestId = "collect_recipes";
                    break;
                }
            }
        }
        
        //int dropCapacity = calculateDropCapacity(monster);
       // List<DroppedItem> drops = dropSystem.generateDrops(dropCapacity);
        int dropCapacity = calculateDropCapacity(monster);
        //MonsterLevel monsterLevel = monster.getComponent(MonsterLevel.class); 
        int level = monsterLevel != null ? monsterLevel.level : 1;
        
        //★ UPDATED: Pass monster level, type, and quest ID
        List<DroppedItem> drops = dropSystem.generateDrops(
        	    dropCapacity, 
        	    level, 
        	    monsterInfo,
        	    activeQuestId
        );
        
        if (!drops.isEmpty()) {
            if (dropSystem.isLuckyDrop(drops)) {
                System.out.println("✨ ✨ ✨ LUCKY DROP! ✨ ✨ ✨");
            }
            
            //can delete this for debugging
            System.out.println(monsterInfo + " dropped:");
            for (DroppedItem drop : drops) {
                System.out.println("  • " + drop.toString());
            }
            
            //Entity player = state.getPlayer();
            addDropsToInventory(player, drops);
        } else {
            System.out.println("  No drops...");
        }
        
        //Entity player = state.getPlayer();
        int xpReward = calculateMonsterXP(monster);
        System.out.println("→ XP Reward: " + xpReward);
        awardExperience(player, xpReward);
        
        BuffManager buffManager = player.getComponent(BuffManager.class);
        if (buffManager != null) {
            buffManager.onMonsterKill();
        }
       
        updateQuestProgress(player, monster, drops, activeQuestId);
        
        if (state.getAutoAttackTarget() == monster) {
            state.clearAutoAttackTarget();
        }
        
        if (state.getTargetedEntity() == monster) {
            state.setTargetedEntity(null);
        }
        
        state.onMonsterDeath(monster);
        
        monster.addComponent(new Dead(1.5f));
        
        Movement movement = monster.getComponent(Movement.class);
        if (movement != null) {
            movement.stopMoving();
        }
        
        Path path = monster.getComponent(Path.class);
        if (path != null) {
            path.clear();
        }
        
        AI ai = monster.getComponent(AI.class);
        if (ai != null) {
            ai.currentState = AI.State.DEAD;
        }
        
        if (sprite != null) {
            sprite.setAnimation(Sprite.ANIM_DEAD);
        }
    }

    private int calculateDropCapacity(Entity monster) {
        MonsterLevel monsterLevel = monster.getComponent(MonsterLevel.class);
        
        if (monsterLevel == null) {
            return 2;
        }
        
        switch (monsterLevel.tier) {
            case TRASH:
                return 2;
            case NORMAL:
                return 3;
            case ELITE:
                return 4;
            case MINIBOSS:
                return 8;
            default:
                return 2;
        }
    }

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
            
            Item itemTemplate = drop.getDropTemplate().createItem();
            System.out.println("::::::::::.createItem()="+itemTemplate.getName());
            if (itemTemplate.isStackable()) {
                boolean success = inventory.addItemStack(itemTemplate, quantity);
                if (success) {
                    itemsAdded += quantity;
                    uiManager.notifyInventoryUpdate();
                    uiManager.getInventoryGrid().addItemToCurrentTab(itemTemplate, true);
                } else {
                    itemsFailed += quantity;
                }
            } else {
                for (int i = 0; i < quantity; i++) {
                    Item item = drop.getDropTemplate().createItem();
                    boolean added = uiManager.addItemToInventory(item);
                    if (added) {
                        itemsAdded++;
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
    
    private void updateQuestProgress(Entity player, Entity monster, List<DroppedItem> drops, String activeQuestId) {
        QuestLog questLog = player.getComponent(QuestLog.class);
        if (questLog == null) return;
        System.out.println("::::updateQuestProgress()>>>>>");
        String monsterName = monster.getName();
        
        for (Quest quest : questLog.getActiveQuests()) {
            for (QuestObjective objective : quest.getObjectives()) {
                String objectiveId = objective.getId();
               // System.out.println("::::objectiveId="+objectiveId);
                //kill
                if (objectiveId.contains("kill")) {
                    String targetMonster = objectiveId.replace("kill_", "").replace("s", "");
                    
                    if (monsterName.toLowerCase().contains(targetMonster)) {
                        questLog.updateQuestProgress(objectiveId, 1);
                        System.out.println("Quest progress: " + quest.getName() + " - " + objective.getDescription());
                    }
                  
                }
                if(!isIntroQuestCollect) {
            		//collect
                    if(objectiveId.contains("collect")) {
                    	System.out.println("::::COLLECT QUEST>>>");
                    	for (DroppedItem drop : drops) { 
                    		GuaranteedDrop guaranteedDrop = 
                    				dropSystem.getZoneLootConfig().checkGuaranteedDrop(activeQuestId, monsterName);
                    		
                    		if (guaranteedDrop != null) { 
                    			if(drop.getItemName().equals(guaranteedDrop.dropItem.getItemName())) {
                        			System.out.println("  • " + drop.getItemName());
                        			System.out.println("Quest Requirement Complete!");
                        			questLog.updateQuestProgress(objectiveId, 1);
                        			
                        			isIntroQuestCollect = true;
                        			break;
                        		}
                    		} 
                        }
                    }
        		 }//is 
            }//for
        }//for 
        
    }
}