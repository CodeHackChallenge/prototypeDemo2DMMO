package dev.main.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Tab component for inventory categories
 */
public class UIInventoryTab extends UIComponent {
    
    private static final Font TAB_FONT = new Font("Arial", Font.BOLD, 11);
    
    private String tabName;
    private boolean active;
    private Runnable onClickCallback;
    
    // Colors
    private Color activeColor;
    private Color inactiveColor;
    private Color hoverColor;
    private Color textColor;
    
    public UIInventoryTab(int x, int y, int width, int height, String tabName) {
        super(x, y, width, height);
        
        this.tabName = tabName;
        this.active = false;
        this.onClickCallback = null;
        
        // Colors
        this.activeColor = new Color(80, 100, 120, 220);
        this.inactiveColor = new Color(50, 50, 60, 180);
        this.hoverColor = new Color(70, 80, 100, 200);
        this.textColor = Color.WHITE;
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
        
        // Choose background color
        Color bgColor;
        if (active) {
            bgColor = activeColor;
        } else if (hovered) {
            bgColor = hoverColor;
        } else {
            bgColor = inactiveColor;
        }
        
        // Draw background
        g.setColor(bgColor);
        g.fillRect(x, y, width, height);
        
        // Draw border
        if (active) {
            g.setColor(new Color(150, 170, 200));
            g.setStroke(new java.awt.BasicStroke(2f));
        } else {
            g.setColor(new Color(80, 80, 90));
            g.setStroke(new java.awt.BasicStroke(1f));
        }
        g.drawRect(x, y, width, height);
        
        // Draw tab name
        Font originalFont = g.getFont();
        g.setFont(TAB_FONT);
        
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(tabName);
        int textHeight = fm.getHeight();
        
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height + textHeight / 2) / 2 - 2;
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(tabName, textX + 1, textY + 1);
        
        // Text
        Color finalTextColor = active ? Color.WHITE : new Color(180, 180, 180);
        g.setColor(finalTextColor);
        g.drawString(tabName, textX, textY);
        
        g.setFont(originalFont);
    }
    
    @Override
    public void update(float delta) {
        // Tabs don't need per-frame updates
    }
    
    @Override
    public boolean onClick() {
        if (onClickCallback != null) {
            onClickCallback.run();
        }
        return true;  // Consume click
    }
    
    // Getters/Setters
    public String getTabName() {
        return tabName;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public void setOnClick(Runnable callback) {
        this.onClickCallback = callback;
    }
}