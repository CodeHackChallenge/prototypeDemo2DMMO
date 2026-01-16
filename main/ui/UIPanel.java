package dev.main.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * ⭐ OPTIMIZED: Added batch mode to prevent cascading relayout calls
 */
public class UIPanel extends UIComponent {
    
    public enum LayoutType {
        NONE,
        HORIZONTAL,
        VERTICAL,
        GRID
    }
    
    private List<UIComponent> children;
    private LayoutType layoutType;
    private int gap;
    
    // Grid layout properties
    private int columns;
    private int rows;
    
    // Visual properties
    private Color backgroundColor;
    private Color borderColor;
    private int borderWidth;
    
    // Dirty flag system
    private boolean needsRelayout = false;
    
    // ⭐ NEW: Batch mode
    private boolean batchMode = false;
    
    public UIPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.children = new ArrayList<>();
        this.layoutType = LayoutType.NONE;
        this.gap = 0;
        this.columns = 1;
        this.rows = 1;
        
        this.backgroundColor = new Color(40, 40, 40, 200);
        this.borderColor = new Color(100, 100, 100, 255);
        this.borderWidth = 2;
        
        this.needsRelayout = false;
        this.batchMode = false;
    }
    
    // ════════════════════════════════════════════════════════════════
    // BATCH MODE API
    // ════════════════════════════════════════════════════════════════
    
    /**
     * ⭐ Start batch mode - prevents automatic relayout
     * Use when adding multiple children at once
     */
    public void beginBatch() {
        batchMode = true;
    }
    
    /**
     * ⭐ End batch mode - triggers single relayout
     * Must be called after beginBatch()
     */
    public void endBatch() {
        batchMode = false;
        markDirty();  // Now trigger the relayout
    }
    
    /**
     * ⭐ Mark panel as needing relayout
     * Respects batch mode - won't mark if batching
     */
    private void markDirty() {
        if (!batchMode) {
            needsRelayout = true;
        }
    }
    
    // ════════════════════════════════════════════════════════════════
    // CHILD MANAGEMENT
    // ════════════════════════════════════════════════════════════════
    
    /**
     * Add single child (triggers relayout unless in batch mode)
     */
    public void addChild(UIComponent child) {
        child.setParent(this);
        children.add(child);
        markDirty();  // Respects batchMode
    }
    
    /**
     * ⭐ NEW: Convenience method - adds multiple children efficiently
     */
    public void addChildren(UIComponent... components) {
        beginBatch();
        for (UIComponent child : components) {
            addChild(child);  // Won't trigger relayout (batchMode=true)
        }
        endBatch();  // Single relayout at end
    }
    
    /**
     * ⭐ NEW: Add list of children
     */
    public void addChildren(List<UIComponent> components) {
        beginBatch();
        for (UIComponent child : components) {
            addChild(child);
        }
        endBatch();
    }
    
    public void removeChild(UIComponent child) {
        child.setParent(null);
        children.remove(child);
        markDirty();
    }
    
    public void clearChildren() {
        for (UIComponent child : children) {
            child.setParent(null);
        }
        children.clear();
        markDirty();
    }
    
    // ════════════════════════════════════════════════════════════════
    // LAYOUT CONFIGURATION
    // ════════════════════════════════════════════════════════════════
    
    public void setLayout(LayoutType layoutType) {
        this.layoutType = layoutType;
        markDirty();
    }
    
    public void setGap(int gap) {
        this.gap = gap;
        markDirty();
    }
    
    public void setGridDimensions(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        if (layoutType == LayoutType.GRID) {
            markDirty();
        }
    }
    
    /**
     * Force immediate relayout (public API)
     */
    public void relayout() {
        if (children.isEmpty()) {
            needsRelayout = false;
            return;
        }
        
        Rectangle innerBounds = getInnerBounds();
        int startX = innerBounds.x;
        int startY = innerBounds.y;
        int availableWidth = innerBounds.width;
        int availableHeight = innerBounds.height;
        
        switch (layoutType) {
            case HORIZONTAL:
                layoutHorizontal(startX, startY, availableWidth);
                break;
                
            case VERTICAL:
                layoutVertical(startX, startY, availableHeight);
                break;
                
            case GRID:
                layoutGrid(startX, startY, availableWidth, availableHeight);
                break;
                
            case NONE:
            default:
                break;
        }
        
        needsRelayout = false;
    }
    
    private void layoutHorizontal(int startX, int startY, int availableWidth) {
        int currentX = startX;
        
        for (UIComponent child : children) {
            if (!child.isVisible()) continue;
            
            currentX += child.marginLeft;
            child.setPosition(currentX, startY + child.marginTop);
            currentX += child.getWidth() + child.marginRight + gap;
        }
    }
    
    private void layoutVertical(int startX, int startY, int availableHeight) {
        int currentY = startY;
        
        for (UIComponent child : children) {
            if (!child.isVisible()) continue;
            
            currentY += child.marginTop;
            child.setPosition(startX + child.marginLeft, currentY);
            currentY += child.getHeight() + child.marginBottom + gap;
        }
    }
    
    private void layoutGrid(int startX, int startY, int availableWidth, int availableHeight) {
        if (columns <= 0 || rows <= 0) return;
        
        int cellWidth = (availableWidth - (gap * (columns - 1))) / columns;
        int cellHeight = (availableHeight - (gap * (rows - 1))) / rows;
        
        int index = 0;
        for (UIComponent child : children) {
            if (!child.isVisible()) continue;
            if (index >= columns * rows) break;
            
            int col = index % columns;
            int row = index / columns;
            
            int cellX = startX + (col * (cellWidth + gap)) + child.marginLeft;
            int cellY = startY + (row * (cellHeight + gap)) + child.marginTop;
            
            child.setPosition(cellX, cellY);
            index++;
        }
    }
    
    // ════════════════════════════════════════════════════════════════
    // RENDERING
    // ════════════════════════════════════════════════════════════════
    
    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
        
        // Relayout only when dirty and visible
        if (needsRelayout) {
            relayout();
        }
        
        // Draw background
        if (backgroundColor != null) {
            g.setColor(backgroundColor);
            g.fillRect(x, y, width, height);
        }
        
        // Draw border
        if (borderColor != null && borderWidth > 0) {
            g.setColor(borderColor);
            g.setStroke(new java.awt.BasicStroke(borderWidth));
            g.drawRect(x, y, width, height);
        }
        
        // Render children
        for (UIComponent child : children) {
            if (child.isVisible()) {
                child.render(g);
            }
        }
    }
    
    @Override
    public void update(float delta) {
        if (!visible) return;
        
        for (UIComponent child : children) {
            if (child.isVisible()) {
                child.update(delta);
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════
    // INPUT HANDLING
    // ════════════════════════════════════════════════════════════════
    
    public void handleMouseMove(int mouseX, int mouseY, boolean pressed) {
        for (UIComponent child : children) {
            if (!child.isVisible() || !child.isEnabled()) continue;
            
            boolean contains = child.contains(mouseX, mouseY);
            
            if (contains && !child.isHovered()) {
                child.onMouseEnter();
            } else if (!contains && child.isHovered()) {
                child.onMouseExit();
            }
        }
    }
    
    public boolean handleClick(int mouseX, int mouseY) {
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (!child.isVisible() || !child.isEnabled()) continue;
            
            if (child.contains(mouseX, mouseY)) {
                if (child instanceof UIScrollableInventoryPanel) {
                    UIScrollableInventoryPanel sp = (UIScrollableInventoryPanel) child;
                    if (sp.handleClick(mouseX, mouseY)) return true;
                } else {
                    boolean consumed = child.onClick();
                    if (consumed) {
                        return true;
                    }
                }
            }
        }
        
        if (this.contains(mouseX, mouseY)) {
            return true;
        }
        
        return false;
    }
    
    public boolean handleRightClick(int mouseX, int mouseY) {
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (!child.isVisible() || !child.isEnabled()) continue;
            
            if (child.contains(mouseX, mouseY)) {
                if (child instanceof UIScrollableInventoryPanel) {
                    UIScrollableInventoryPanel sp = (UIScrollableInventoryPanel) child;
                    if (sp.handleRightClick(mouseX, mouseY)) return true;
                } else {
                    boolean consumed = child.onRightClick();
                    if (consumed) {
                        return true;
                    }
                }
            }
        }
        
        if (this.contains(mouseX, mouseY)) {
            return true;
        }
        
        return false;
    }
    
    // ════════════════════════════════════════════════════════════════
    // GETTERS / SETTERS
    // ════════════════════════════════════════════════════════════════
    
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }
    
    public void setBorderColor(Color color) {
        this.borderColor = color;
    }
    
    public void setBorderWidth(int width) {
        this.borderWidth = width;
    }
    
    public List<UIComponent> getChildren() {
        return children;
    }
    
    public LayoutType getLayoutType() {
        return layoutType;
    }
}