package dev.main.entity;

public class SpawnPoint {
    public String monsterType;
    public float x;
    public float y;
    public float respawnDelay;
    public int level;           // NEW: Monster level
    public MobTier tier;        // NEW: Monster tier
    
    public Entity currentMonster;
    public boolean isOccupied;
    public float respawnTimer;
    
    /**
     * NEW: Constructor with level and tier
     */
    public SpawnPoint(String monsterType, float x, float y, float respawnDelay, int level, MobTier tier) {
        this.monsterType = monsterType;
        this.x = x;
        this.y = y;
        this.respawnDelay = respawnDelay;
        this.level = level;
        this.tier = tier;
        this.currentMonster = null;
        this.isOccupied = false;
        this.respawnTimer = 0;
    }
    
    /**
     * OLD: Backwards compatibility - defaults to level 1, NORMAL tier
     */
    public SpawnPoint(String monsterType, float x, float y, float respawnDelay) {
        this(monsterType, x, y, respawnDelay, 1, MobTier.NORMAL);
    }
    
    public void update(float delta) {
        if (!isOccupied) {
            respawnTimer += delta;
        }
    }
    
    public boolean canRespawn() {
        return !isOccupied && respawnTimer >= respawnDelay;
    }
    
    public void spawn(Entity monster) {
        this.currentMonster = monster;
        this.isOccupied = true;
        this.respawnTimer = 0;
    }
    
    public void onMonsterDeath() {
        this.currentMonster = null;
        this.isOccupied = false;
        this.respawnTimer = 0;
    }
    
    @Override
    public String toString() {
        return String.format("%s Lv%d (%s) at (%.0f, %.0f)", 
            monsterType, level, tier, x, y);
    }
}