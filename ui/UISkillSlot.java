package dev.main.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import dev.main.skill.Skill;
import dev.main.sprite.TextureManager;

/**
 * OPTIMIZED: Cached Font objects to eliminate per-frame Font creation
 */
public class UISkillSlot extends UIComponent {
    
    // ⭐ NEW: Cached fonts (created once, reused forever)
    private static final Font COOLDOWN_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font KEYBIND_FONT = new Font("Arial", Font.BOLD, 12);
    private static final Font LEVEL_FONT = new Font("Arial", Font.BOLD, 10);
    
    private Skill skill;
    private String keyBinding;
    private boolean showCooldown;
    private boolean showKeybind;
    
    private UIManager uiManager;
    private int slotIndex;
    
    // Visual properties
    private Color emptyColor;
    private Color hoverColor;
    private Color cooldownColor;
    private Color keybindBgColor;
    
    public UISkillSlot(int x, int y, int size) {
        super(x, y, size, size);
        
        this.skill = null;
        this.keyBinding = "";
        this.showCooldown = true;
        this.showKeybind = true;
        this.uiManager = null;
        this.slotIndex = -1;
        
        this.emptyColor = new Color(60, 60, 60, 200);
        this.hoverColor = new Color(100, 100, 100, 255);
        this.cooldownColor = new Color(0, 0, 0, 150);
        this.keybindBgColor = new Color(0, 0, 0, 180);
    }
    
    public UISkillSlot(int x, int y, int size, String keyBinding) {
        this(x, y, size);
        this.keyBinding = keyBinding;
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
        
        // Draw slot background
        if (skill == null) {
            g.setColor(hovered ? hoverColor : emptyColor);
        } else {
            g.setColor(skill.getIconColor());
        }
        g.fillRect(x, y, width, height);
        
        // Draw border
        g.setColor(hovered ? Color.WHITE : new Color(100, 100, 100));
        g.setStroke(new BasicStroke(2));
        g.drawRect(x, y, width, height);
        
        if (skill != null) {
            // Draw skill items
            if (skill.getIconPath() != null) {
                BufferedImage icon = TextureManager.load(skill.getIconPath());
                if (icon != null) {
                    g.drawImage(icon, x, y, width, height, null);
                }
            }
            
            // Draw cooldown overlay
            if (showCooldown && !skill.isReady()) {
                drawCooldownOverlay(g);
            }
            
            // Draw cooldown text
            if (!skill.isReady()) {
                drawCooldownText(g);
            }
            
            // Draw skill level
            drawSkillLevel(g);
        } else {
            // Draw empty slot indicator
            g.setColor(new Color(150, 150, 150, 100));
            int size = width / 3;
            int centerX = x + width / 2 - size / 2;
            int centerY = y + height / 2 - size / 2;
            g.fillRect(centerX + size / 3, centerY, size / 3, size);
            g.fillRect(centerX, centerY + size / 3, size, size / 3);
        }
        
        // Draw keybind
        if (showKeybind && !keyBinding.isEmpty()) {
            drawKeybind(g);
        }
        
        // Draw hover effect
        if (hovered && skill != null) {
            g.setColor(new Color(255, 255, 255, 50));
            g.fillRect(x, y, width, height);
        }
    }
    
    private void drawCooldownOverlay(Graphics2D g) {
        float progress = skill.getCooldownProgress();
        int overlayHeight = (int)(height * progress);
        
        g.setColor(cooldownColor);
        g.fillRect(x, y, width, overlayHeight);
    }
    
    // ⭐ OPTIMIZED: Use cached font
    private void drawCooldownText(Graphics2D g) {
        Font originalFont = g.getFont();
        g.setFont(COOLDOWN_FONT);  // ⭐ Use cached font
        
        String cooldownText = String.format("%.1f", skill.getRemainingCooldown());
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(cooldownText);
        int textHeight = fm.getHeight();
        
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height + textHeight) / 2 - 4;
        
        // Shadow
        g.setColor(Color.BLACK);
        g.drawString(cooldownText, textX + 1, textY + 1);
        
        // Text
        g.setColor(Color.WHITE);
        g.drawString(cooldownText, textX, textY);
        
        g.setFont(originalFont);
    }
    
    // ⭐ OPTIMIZED: Use cached font
    private void drawKeybind(Graphics2D g) {
        Font originalFont = g.getFont();
        g.setFont(KEYBIND_FONT);  // ⭐ Use cached font
        
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(keyBinding);
        int textHeight = fm.getHeight();
        
        int bgSize = Math.max(textWidth, textHeight) + 4;
        int bgX = x + 2;
        int bgY = y + height - bgSize - 2;
        
        // Background
        g.setColor(keybindBgColor);
        g.fillRect(bgX, bgY, bgSize, bgSize);
        
        // Border
        g.setColor(new Color(200, 200, 200));
        g.drawRect(bgX, bgY, bgSize, bgSize);
        
        // Text
        int textX = bgX + (bgSize - textWidth) / 2;
        int textY = bgY + (bgSize + textHeight / 2) / 2;
        
        g.setColor(Color.WHITE);
        g.drawString(keyBinding, textX, textY);
        
        g.setFont(originalFont);
    }
    
    // ⭐ OPTIMIZED: Use cached font
    private void drawSkillLevel(Graphics2D g) {
        Font originalFont = g.getFont();
        g.setFont(LEVEL_FONT);  // ⭐ Use cached font
        
        String levelText = "Lv" + skill.getSkillLevel();
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(levelText);
        int textHeight = fm.getHeight();
        
        int bgWidth = textWidth + 6;
        int bgHeight = textHeight;
        int bgX = x + width - bgWidth - 2;
        int bgY = y + 2;
        
        // Background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(bgX, bgY, bgWidth, bgHeight);
        
        // Text
        int textX = bgX + 3;
        int textY = bgY + textHeight - 3;
        
        Color levelColor;
        if (skill.isMaxLevel()) {
            levelColor = new Color(255, 215, 0);
        } else {
            levelColor = Color.WHITE;
        }
        
        g.setColor(levelColor);
        g.drawString(levelText, textX, textY);
        
        g.setFont(originalFont);
    }
    
    @Override
    public void update(float delta) {
        if (skill != null) {
            skill.update(delta);
        }
    }
    
    @Override
    public boolean onClick() {
        if (skill != null && uiManager != null && slotIndex >= 0) {
            if (skill.isReady()) {
                uiManager.useSkillInSlot(slotIndex);
            }
        }
        return true;
    }
    
    @Override
    public boolean onRightClick() {
        if (skill != null && uiManager != null && slotIndex >= 0) {
            uiManager.upgradeSkill(slotIndex);
        }
        return true;
    }
    
    public String getTooltipText() {
        if (skill == null) {
            return "Empty Slot" + (keyBinding.isEmpty() ? "" : " [" + keyBinding + "]");
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(skill.getName()).append(" - Level ").append(skill.getSkillLevel());
        
        if (skill.isMaxLevel()) {
            sb.append(" (MAX)");
        }
        
        sb.append("\n");
        sb.append(skill.getDescription()).append("\n");
        sb.append("────────────\n");
        sb.append("Type: ").append(skill.getType()).append("\n");
        sb.append("Cooldown: ").append(skill.getCooldown()).append("s\n");
        sb.append("Base Mana: ").append(skill.getBaseManaPercent()).append("% of Max MP\n");
        
        float reduction = skill.getManaCostReduction() * 100;
        if (reduction > 0) {
            sb.append("Cost Reduction: -").append(String.format("%.0f", reduction)).append("%\n");
        }
        
        sb.append("Level Req: ").append(skill.getLevelRequired()).append("\n");
        
        if (skill.getType() == Skill.SkillType.HEAL) {
            double healPercent = skill.getHealPercent() * 100;
            sb.append("Heal Power: ").append(String.format("%.1f", healPercent)).append("%\n");
        }
        
        if (!skill.isMaxLevel()) {
            sb.append("\n");
            sb.append("Upgrade Cost: ").append(skill.getUpgradeCost()).append(" point(s)");
        }
        
        if (!keyBinding.isEmpty()) {
            sb.append("\n\nHotkey: [").append(keyBinding).append("]");
        }
        
        return sb.toString();
    }
    
    public void setUIManager(UIManager uiManager, int slotIndex) {
        this.uiManager = uiManager;
        this.slotIndex = slotIndex;
    }
    
    // Getters/Setters
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public String getKeyBinding() { return keyBinding; }
    public void setKeyBinding(String keyBinding) { this.keyBinding = keyBinding; }
    public void setShowCooldown(boolean show) { this.showCooldown = show; }
    public void setShowKeybind(boolean show) { this.showKeybind = show; }
    public int getSlotIndex() { return slotIndex; }
}