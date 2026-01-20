package dev.main.render;

import dev.main.input.Component;

public class Renderable implements Component {
    public RenderLayer layer;
    public int depthOffset;  // For sorting within same layer (higher = drawn later/on top)
    
    public Renderable(RenderLayer layer) {
        this.layer = layer;
        this.depthOffset = 0;
    }
    
    public Renderable(RenderLayer layer, int depthOffset) {
        this.layer = layer;
        this.depthOffset = depthOffset;
    }
}
