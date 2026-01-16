package dev.main.render;

import dev.main.entity.Entity;
import dev.main.input.Position;

public class RenderObject implements Comparable<RenderObject> {
    public Entity entity;
    public Position position;
    public RenderLayer layer;
    public float depth;  // Y position for depth sorting within layer
    
    public RenderObject(Entity entity, Position position, Renderable renderable) {
        this.entity = entity;
        this.position = position;
        this.layer = renderable.layer;
        this.depth = position.y + renderable.depthOffset;
    }
    
    @Override
    public int compareTo(RenderObject other) {
        // First sort by layer priority
        if (this.layer.priority != other.layer.priority) {
            return Integer.compare(this.layer.priority, other.layer.priority);
        }
        
        // Within same layer, sort by depth (Y position)
        // Lower Y = further back = drawn first
        return Float.compare(this.depth, other.depth);
    }
}
