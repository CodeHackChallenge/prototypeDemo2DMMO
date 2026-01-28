package dev.main.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

import dev.main.buffs.Buff;
import dev.main.buffs.BuffManager;
import dev.main.entity.Entity;
import dev.main.state.GameState;

/**
 * UIBuffBar - Displays active buffs as small icons above the skill bar
 */
public class UIBuffBar extends UIComponent {
    
    private static final Font DURATION_FONT = new Font("Arial", Font.BOLD, 9);
    
    private GameState gameState;
    private Entity player;
    
    // Layout
    private int iconSize;
    private int gap;
    private int maxIcons;
    
    // Visual
    private Color borderColor;
    private Color durationBgColor;
    
    // Hover tracking
    private Buff hoveredBuff;
    
    public UIBuffBar(int x, int y, GameState gameState) {
        super(x, y, 0, 0);  // Width/height calculated dynamically
        
        this.gameState = gameState;
        this.player = gameState.getPlayer();
        
        this.iconSize = 32;
        this.gap = 4;
        this.maxIcons = 10;
        
        this.borderColor = new Color(100, 100, 120);
        this.durationBgColor = new Color(0, 0, 0, 180);
        
        this.hoveredBuff = null;
        
        // Update dimensions
        updateDimensions();
    }
    
    private void updateDimensions() {
        BuffManager buffManager = player.getComponent(BuffManager.class);
        if (buffManager == null) {
            width = 0;
            height = 0;
            return;
        }
        
        int buffCount = Math.min(buffManager.getActiveBuffCount(), maxIcons);
        width = (iconSize * buffCount) + (gap * Math.max(0, buffCount - 1));
        height = iconSize;
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
        
        BuffManager buffManager = player.getComponent(BuffManager.class);
        if (buffManager == null || !buffManager.hasActiveBuffs()) {
            return;
        }
        
        updateDimensions();
        
        List<Buff> buffs = buffManager.getActiveBuffs();
        int displayCount = Math.min(buffs.size(), maxIcons);
        
        for (int i = 0; i < displayCount; i++) {
            Buff buff = buffs.get(i);
            int iconX = x + (i * (iconSize + gap));
            int iconY = y;
            
            renderBuffIcon(g, buff, iconX, iconY);
        }
        
        // If more buffs than can be displayed, show count
        if (buffs.size() > maxIcons) {
            renderOverflowIndicator(g, buffs.size() - maxIcons);
        }
    }
    
    private void renderBuffIcon(Graphics2D g, Buff buff, int iconX, int iconY) {
        // Draw background with buff color
        g.setColor(buff.getIconColor());
        g.fillRect(iconX, iconY, iconSize, iconSize);
        
        // Draw border (highlight if hovered)
        if (buff == hoveredBuff) {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
        } else {
            g.setColor(borderColor);
            g.setStroke(new BasicStroke(1f));
        }
        g.drawRect(iconX, iconY, iconSize, iconSize);
        
        // Draw items letter (first letter of name)
        String letter = buff.getName().substring(0, 1).toUpperCase();
        Font letterFont = new Font("Arial", Font.BOLD, 16);
        g.setFont(letterFont);
        
        FontMetrics fm = g.getFontMetrics();
        int letterWidth = fm.stringWidth(letter);
        int letterHeight = fm.getHeight();
        
        int letterX = iconX + (iconSize - letterWidth) / 2;
        int letterY = iconY + (iconSize + letterHeight / 2) / 2 - 2;
        
        // Letter shadow
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(letter, letterX + 1, letterY + 1);
        
        // Letter
        g.setColor(Color.WHITE);
        g.drawString(letter, letterX, letterY);
        
        // Draw duration bar at bottom
        renderDurationBar(g, buff, iconX, iconY);
        
        // Draw hover effect
        if (buff == hoveredBuff) {
            g.setColor(new Color(255, 255, 255, 30));
            g.fillRect(iconX, iconY, iconSize, iconSize);
        }
    }
    
    private void renderDurationBar(Graphics2D g, Buff buff, int iconX, int iconY) {
        int barHeight = 4;
        int barY = iconY + iconSize - barHeight;
        
        float durationPercent = buff.getDurationPercent();
        int fillWidth = (int)(iconSize * durationPercent);
        
        // Background
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(iconX, barY, iconSize, barHeight);
        
        // Fill - color changes based on remaining time
        Color fillColor;
        if (durationPercent > 0.5f) {
            fillColor = new Color(100, 200, 100);  // Green
        } else if (durationPercent > 0.25f) {
            fillColor = new Color(255, 200, 0);    // Yellow
        } else {
            fillColor = new Color(255, 100, 100);  // Red
        }
        
        g.setColor(fillColor);
        g.fillRect(iconX, barY, fillWidth, barHeight);
        
        // Duration text (for kill-based or short time-based)
        if (buff.getDurationType() == Buff.DurationType.KILL_BASED || 
            buff.getCurrentDuration() < 60) {
            renderDurationText(g, buff, iconX, iconY);
        }
    }
    
    private void renderDurationText(Graphics2D g, Buff buff, int iconX, int iconY) {
        Font originalFont = g.getFont();
        g.setFont(DURATION_FONT);
        
        String durationText;
        if (buff.getDurationType() == Buff.DurationType.KILL_BASED) {
            durationText = String.format("%.0f", buff.getCurrentDuration());
        } else {
            durationText = String.format("%.0f", buff.getCurrentDuration());
        }
        
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(durationText);
        
        int textX = iconX + (iconSize - textWidth) / 2;
        int textY = iconY + iconSize - 8;
        
        // Background
        g.setColor(durationBgColor);
        g.fillRect(textX - 2, textY - fm.getHeight() + 2, textWidth + 4, fm.getHeight());
        
        // Text
        g.setColor(Color.WHITE);
        g.drawString(durationText, textX, textY);
        
        g.setFont(originalFont);
    }
    
    private void renderOverflowIndicator(Graphics2D g, int extraCount) {
        int indicatorX = x + width - iconSize - 4;
        int indicatorY = y + 2;
        
        String text = "+" + extraCount;
        Font font = new Font("Arial", Font.BOLD, 10);
        g.setFont(font);
        
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        // Background
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(indicatorX, indicatorY, textWidth + 6, textHeight + 2, 4, 4);
        
        // Text
        g.setColor(new Color(255, 215, 0));
        g.drawString(text, indicatorX + 3, indicatorY + textHeight - 2);
    }
    
    @Override
    public void update(float delta) {
        // Update handled by BuffManager component
    }
    
    /**
     * Handle mouse movement for hover
     */
    public void handleMouseMove(int mouseX, int mouseY) {
        if (!visible) return;
        
        BuffManager buffManager = player.getComponent(BuffManager.class);
        if (buffManager == null || !buffManager.hasActiveBuffs()) {
            hoveredBuff = null;
            return;
        }
        
        updateDimensions();
        
        List<Buff> buffs = buffManager.getActiveBuffs();
        int displayCount = Math.min(buffs.size(), maxIcons);
        
        hoveredBuff = null;
        
        for (int i = 0; i < displayCount; i++) {
            int iconX = x + (i * (iconSize + gap));
            int iconY = y;
            
            if (mouseX >= iconX && mouseX <= iconX + iconSize &&
                mouseY >= iconY && mouseY <= iconY + iconSize) {
                hoveredBuff = buffs.get(i);
                break;
            }
        }
    }
    
    /**
     * Get tooltip text for hovered buff
     */
    public String getTooltipText(int mouseX, int mouseY) {
        if (hoveredBuff != null) {
            return hoveredBuff.getTooltipText();
        }
        return null;
    }
    
    /**
     * Get hovered buff
     */
    public Buff getHoveredBuff() {
        return hoveredBuff;
    }
}