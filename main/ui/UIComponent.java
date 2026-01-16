package dev.main.ui;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * OPTIMIZED: Rectangle-free contains() and cached bounds
 */
public abstract class UIComponent {
    protected int x, y;
    protected int width, height;
    
    protected int marginTop, marginRight, marginBottom, marginLeft;
    protected int paddingTop, paddingRight, paddingBottom, paddingLeft;
    
    protected boolean visible = true;
    protected boolean enabled = true;
    protected boolean hovered = false;
    
    protected UIPanel parent;
    
    // ⭐ NEW: Cached bounds
    private Rectangle cachedBounds = null;
    private int lastBoundsX = -1;
    private int lastBoundsY = -1;
    private int lastBoundsWidth = -1;
    private int lastBoundsHeight = -1;
    
    public UIComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        
        this.marginTop = this.marginRight = this.marginBottom = this.marginLeft = 0;
        this.paddingTop = this.paddingRight = this.paddingBottom = this.paddingLeft = 0;
    }
    
    public abstract void render(Graphics2D g);
    public abstract void update(float delta);
    
    public void setMargin(int top, int right, int bottom, int left) {
        this.marginTop = top;
        this.marginRight = right;
        this.marginBottom = bottom;
        this.marginLeft = left;
    }
    
    public void setMargin(int all) {
        setMargin(all, all, all, all);
    }
    
    public void setPadding(int top, int right, int bottom, int left) {
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        this.paddingLeft = left;
    }
    
    public void setPadding(int all) {
        setPadding(all, all, all, all);
    }
    
    // ⭐ OPTIMIZED: Cache bounds
    public Rectangle getBounds() {
        if (cachedBounds == null || x != lastBoundsX || y != lastBoundsY || 
            width != lastBoundsWidth || height != lastBoundsHeight) {
            cachedBounds = new Rectangle(x, y, width, height);
            lastBoundsX = x;
            lastBoundsY = y;
            lastBoundsWidth = width;
            lastBoundsHeight = height;
        }
        return cachedBounds;
    }
    
    public Rectangle getOuterBounds() {
        return new Rectangle(
            x - marginLeft,
            y - marginTop,
            width + marginLeft + marginRight,
            height + marginTop + marginBottom
        );
    }
    
    public Rectangle getInnerBounds() {
        return new Rectangle(
            x + paddingLeft,
            y + paddingTop,
            width - paddingLeft - paddingRight,
            height - paddingTop - paddingBottom
        );
    }
    
    // ⭐ OPTIMIZED: Rectangle-free contains check
    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + height;
    }
    
    public void onMouseEnter() {
        hovered = true;
    }
    
    public void onMouseExit() {
        hovered = false;
    }
    
    public boolean onClick() {
        return false;
    }
    
    public boolean onRightClick() {
        return false;
    }
    
    public String getTooltipText() {
        return null;
    }
    
    // Getters/Setters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        cachedBounds = null;  // ⭐ Invalidate cache
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        cachedBounds = null;  // ⭐ Invalidate cache
    }
    
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isHovered() { return hovered; }
    
    public void setParent(UIPanel parent) {
        this.parent = parent;
    }
    
    public UIPanel getParent() {
        return parent;
    }
}