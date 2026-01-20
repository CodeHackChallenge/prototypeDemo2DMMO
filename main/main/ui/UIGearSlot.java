package dev.main.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import dev.main.item.Item;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

/**
 * UI component representing a gear/equipment slot
 * UPDATED: Now displays item icons
 */
public class UIGearSlot extends UIComponent {
    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 9);
   
    public enum SlotType {
        HEAD,
        TOP_ARMOR,
        PANTS,
        GLOVES, 
        SHOES,
        WEAPON,
        EARRINGS,
        NECKLACE,
        BRACELET,
        RING_1,
        RING_2,
        SPECIAL
    }
    
    private SlotType slotType;
    private Item item;
    
    // Reference to UIManager for unequipping
    private UIManager uiManager;
    
    // Visual properties
    private Color emptyColor;
    private Color hoverColor;
    private Color fillColor;
    
    public UIGearSlot(int x, int y, int width, int height, SlotType slotType, UIManager uiManager) {
        super(x, y, width, height);
        
        this.slotType = slotType;
        this.uiManager = uiManager;
        this.item = null;
        
        // Colors
        this.emptyColor = new Color(60, 60, 60, 180);
        this.hoverColor = new Color(100, 100, 100, 200);
        this.fillColor = new Color(80, 100, 120, 200);
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
        
        // Choose background color
        Color bgColor;
        if (item != null) {
            bgColor = hovered ? fillColor.brighter() : fillColor;
        } else {
            bgColor = hovered ? hoverColor : emptyColor;
        }
        
        // Draw slot background
        g.setColor(bgColor);
        g.fillRect(x, y, width, height);
        
        // Draw border
        g.setColor(hovered ? Color.WHITE : new Color(100, 100, 100));
        g.setStroke(new java.awt.BasicStroke(1f));
        g.drawRect(x, y, width, height);
        
        // Draw slot label (only if empty or on hover)
        if (item == null || hovered) {
            drawSlotLabel(g);
        }
        
        // ★ UPDATED: Draw item icon if equipped
        if (item != null) {
            drawItemIcon(g);
        }
        
        // Draw hover effect
        if (hovered) {
            g.setColor(new Color(255, 255, 255, 30));
            g.fillRect(x, y, width, height);
        }
    }
    
    /**
     * Draw slot type label
     */
    private void drawSlotLabel(Graphics2D g) {
        Font originalFont = g.getFont();
        g.setFont(LABEL_FONT);
        
        String label = getSlotLabel();
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textHeight = fm.getHeight();
        
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height + textHeight / 2) / 2 - 2;
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(label, textX + 1, textY + 1);
        
        // Text
        g.setColor(new Color(200, 200, 200));
        g.drawString(label, textX, textY);
        
        g.setFont(originalFont);
    }
    
    /**
     * Get display label for slot type
     */
    private String getSlotLabel() {
        switch (slotType) {
            case HEAD: return "Head";
            case TOP_ARMOR: return "Armor";
            case PANTS: return "Pants";
            case GLOVES: return "Gloves";
            case SHOES: return "Shoes";
            case WEAPON: return "Weapon"; 
            case EARRINGS: return "Earring";
            case NECKLACE: return "Neck";
            case BRACELET: return "Bracelet";
            case RING_1: return "Ring";
            case RING_2: return "Ring";
            case SPECIAL: return "Special";
            default: return "???";
        }
    }
    
    /**
     * ★ NEW: Draw item icon (same system as inventory slots)
     */
    private void drawItemIcon(Graphics2D g) {
        if (item == null) return;
        
        // Calculate icon position and size
        int itemSize = (int)(Math.min(width, height) * 0.75f);
        int itemX = x + (width - itemSize) / 2;
        int itemY = y + (height - itemSize) / 2;
        
        // Try to load item icon
        String iconPath = item.getIconPath();
        BufferedImage icon = null;
        
        if (iconPath != null && !iconPath.isEmpty()) {
            try {
                icon = dev.main.sprite.TextureManager.load(iconPath);
            } catch (Exception e) {
                // Icon not found, will use fallback
                icon = null;
            }
        }
        
        if (icon != null) {
            // ★ Draw the actual icon image
            g.drawImage(icon, itemX, itemY, itemSize, itemSize, null);
            
            // Draw rarity border
            drawRarityBorder(g, itemX, itemY, itemSize);
        } else {
            // ★ Fallback: Draw enhanced placeholder
            drawFallbackPlaceholder(g, itemX, itemY, itemSize);
        }
    }
    
    /**
     * ★ NEW: Draw fallback placeholder
     */
    private void drawFallbackPlaceholder(Graphics2D g, int x, int y, int size) {
        // Draw colored square based on rarity
        Color rarityColor = getRarityColor(item.getRarity());
        g.setColor(rarityColor);
        g.fillRect(x, y, size, size);
        
        // Draw border
        g.setColor(rarityColor.brighter());
        g.setStroke(new java.awt.BasicStroke(2f));
        g.drawRect(x, y, size, size);
        
        // Draw first letter of item name
        drawItemInitial(g, x, y, size);
        
        // Draw rarity border
        drawRarityBorder(g, x, y, size);
    }
    
    /**
     * ★ NEW: Draw rarity border around icon
     */
    private void drawRarityBorder(Graphics2D g, int x, int y, int size) {
        Color rarityColor = getRarityColor(item.getRarity());
        g.setColor(new Color(rarityColor.getRed(), rarityColor.getGreen(), 
                            rarityColor.getBlue(), 180));
        g.setStroke(new java.awt.BasicStroke(2f));
        g.drawRect(x - 1, y - 1, size + 2, size + 2);
    }
    
    /**
     * ★ NEW: Get color based on rarity
     */
    private Color getRarityColor(Item.Rarity rarity) {
        switch (rarity) {
            case COMMON:
                return new Color(180, 180, 180);  // Gray
            case UNCOMMON:
                return new Color(100, 200, 100);  // Green
            case RARE:
                return new Color(80, 120, 220);   // Blue
            case EPIC:
                return new Color(160, 80, 220);   // Purple
            case LEGENDARY:
                return new Color(255, 165, 0);    // Orange/Gold
            default:
                return new Color(150, 150, 150);
        }
    }
    
    /**
     * ★ NEW: Draw item initial letter (fallback)
     */
    private void drawItemInitial(Graphics2D g, int x, int y, int size) {
        if (item == null || item.getName() == null || item.getName().isEmpty()) {
            return;
        }
        
        String initial = item.getName().substring(0, 1).toUpperCase();
        
        Font originalFont = g.getFont();
        g.setFont(new Font("Arial", Font.BOLD, size / 2));
        
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(initial);
        int textHeight = fm.getAscent();
        
        int textX = x + (size - textWidth) / 2;
        int textY = y + (size + textHeight) / 2 - 2;
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(initial, textX + 2, textY + 2);
        
        // Text
        g.setColor(Color.WHITE);
        g.drawString(initial, textX, textY);
        
        g.setFont(originalFont);
    }
    
    @Override
    public void update(float delta) {
        // Gear slots don't need per-frame updates
    }
    
    @Override
    public boolean onClick() {
        if (item == null) {
            System.out.println("Clicked empty gear slot: " + slotType);
        } else {
            System.out.println("Clicked gear slot: " + slotType + " with item: " + item.getName());
        }
        return true;
    }
    
    @Override
    public boolean onRightClick() {
        if (item != null) {
            System.out.println("Right-clicked gear slot: " + slotType + " - unequipping " + item.getName());
            uiManager.unequipItem(slotType);
        }
        return true;
    }
    
    // Item management
    public Item equipItem(Item item) {
        Item oldItem = this.item;
        this.item = item;
        
        if (item != null) {
            System.out.println("✅ Equipped " + item.getName() + " in " + slotType + " slot");
        }
        
        return oldItem;
    }
    
    public Item unequipItem() {
        Item oldItem = this.item;
        this.item = null;
        
        if (oldItem != null) {
            System.out.println("⬇️ Unequipped " + oldItem.getName() + " from " + slotType + " slot");
        }
        
        return oldItem;
    }
    
    public Item getItem() {
        return item;
    }
    
    public boolean isEmpty() {
        return item == null;
    }
    
    public SlotType getSlotType() {
        return slotType;
    }
    /**
     * Get tooltip text for this item
     * UNIFIED FORMAT: Shows all item properties
     */
    @Override
    public String getTooltipText() {
        if (item != null) {
            return getItemDescription(item);
        }
        return null;
    }

    /**
     * ★ UPDATED: Unified item description format
     * Same format as UIInventorySlot for consistency
     */
    private String getItemDescription(Item item) {
        StringBuilder sb = new StringBuilder();
        
        // Header: Name
        sb.append(item.getName()).append("\n");
        
        // Type and Rarity
        sb.append("Type: ").append(item.getType()).append("\n");
        sb.append("Rarity: ").append(item.getRarity()).append("\n");
        
        // Blank line before stats
        sb.append("\n");
        
        // Stats (only show if > 0)
        if (item.getAttackBonus() > 0) {
            sb.append("Attack: +").append(item.getAttackBonus()).append("\n");
        }
        if (item.getDefenseBonus() > 0) {
            sb.append("Defense: +").append(item.getDefenseBonus()).append("\n");
        }
        if (item.getMagicAttackBonus() > 0) {
            sb.append("Magic Attack: +").append(item.getMagicAttackBonus()).append("\n");
        }
        if (item.getMagicDefenseBonus() > 0) {
            sb.append("Magic Defense: +").append(item.getMagicDefenseBonus()).append("\n");
        }
        
        // Durability (show if item has durability)
        if (item.getMaxDurability() > 0) {
            sb.append("\n");
            sb.append("Durability: ").append(item.getCurrentDurability())
               .append("/").append(item.getMaxDurability()).append("\n");
        }
        
        // ★ Note: Gear slots don't stack, so no stack info needed
        
        // Blank line before flags
        sb.append("\n");
        
        // Flags/Properties (only show restrictions)
        if (!item.isUpgradable()) {
            sb.append("Cannot be upgraded\n");
        }
        if (!item.canInfuseElemental()) {
            sb.append("Cannot infuse elemental stones\n");
        }
        if (!item.isTradable()) {
            sb.append("Cannot be traded\n");
        }
        if (!item.isSellable()) {
            sb.append("Cannot be sold\n");
        }
        
        return sb.toString().trim();
    }
}