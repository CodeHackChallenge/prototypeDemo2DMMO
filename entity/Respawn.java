package dev.main.entity;

import dev.main.input.Component;

public class Respawn implements Component {
    public float respawnTimer;
    public float respawnDelay;  // How long until respawn
    
    // Spawn info
    public String monsterType;
    public float spawnX;
    public float spawnY;
    
    public Respawn(String monsterType, float spawnX, float spawnY, float respawnDelay) {
        this.monsterType = monsterType;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.respawnDelay = respawnDelay;
        this.respawnTimer = 0;
    }
    
    public void update(float delta) {
        respawnTimer += delta;
    }
    
    public boolean shouldRespawn() {
        return respawnTimer >= respawnDelay;
    }
    
    public void reset() {
        respawnTimer = 0;
    }
}