package dev.main.entity;

import java.util.HashMap;
import java.util.Map;

import dev.main.input.Component;

public class Entity {

    private static int nextID = 0;
    
    public final int ID;
    private String name;
    private EntityType type;  // NEW
    
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    public Entity() {
        this.ID = nextID++;
        this.name = "Entity_" + ID;
        this.type = EntityType.PLAYER;
    }
    
    public Entity(String name) {
        this.ID = nextID++;
        this.name = name;
        this.type = EntityType.PLAYER;
    }
    
    public Entity(String name, EntityType type) {
        this.ID = nextID++;
        this.name = name;
        this.type = type;
    }

    public Entity(int id, String name) {
        this.ID = id;
        this.name = name;
        this.type = EntityType.PLAYER;
        
        if (id >= nextID) {
            nextID = id + 1;
        }
    }

    public <T extends Component> void addComponent(T component) {
        components.put(component.getClass(), component);
    }

    public <T extends Component> T getComponent(Class<T> type) {
        return type.cast(components.get(type));
    }

    public <T extends Component> void removeComponent(Class<T> type) {
        components.remove(type);
    }

    public <T extends Component> boolean hasComponent(Class<T> type) {
        return components.containsKey(type);
    }

    public int getID() {
        return ID;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public EntityType getType() {
        return type;
    }
    
    public void setType(EntityType type) {
        this.type = type;
    }
    
    @Override
    public String toString() {
        return name + " (ID: " + ID + ", Type: " + type + ")";
    }
}