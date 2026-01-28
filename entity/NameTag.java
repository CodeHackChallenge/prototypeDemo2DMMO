package dev.main.entity;

import dev.main.input.Component;

public class NameTag implements Component {
    public boolean visible;
    public String displayName;
    public float offsetY;  // How far above entity to show
    
    public NameTag(String displayName, float offsetY) {
        this.displayName = displayName;
        this.offsetY = offsetY;
        this.visible = false;
    }
    
    public void show() {
        this.visible = true;
    }
    
    public void hide() {
        this.visible = false;
    }
}