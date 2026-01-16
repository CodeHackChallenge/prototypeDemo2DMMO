package dev.main.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import dev.main.item.Item;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

/**
 * UI component representing an inventory slot
 * Similar visual style to menu buttons but for holding items
 */
public class UIInventorySlot extends UIComponent {
    private static final Font SLOT_FONT = new Font("Arial", Font.PLAIN, 10);
    private static final Font NEW_BADGE_FONT = new Font("Arial", Font.BOLD, 9);
   
    private int stackCount = 1;
    private Item item;
    private int slotIndex;
    
    // Reference to UIManager for equipping
    private UIManager uiManager;
    
    // Visual properties
    private Color emptyColor;
    private Color hoverColor;
    private Color fillColor;
    
    // ★ NEW: Item notification system
    private boolean isNewItem;
    private float newItemPulse;
    private float newItemTimer;
    private static final float NEW_ITEM_DURATION = 5.0f; // Show "NEW" for 5 seconds
    
    public UIInventorySlot(int x, int y, int size, int slotIndex, UIManager uiManager) {
        super(x, y, size, size);
        
        this.slotIndex = slotIndex;
        this.uiManager = uiManager;
        this.item = null;
        
        // Colors similar to locked menu buttons
        this.emptyColor = new Color(60, 60, 60, 180);
        this.hoverColor = new Color(100, 100, 100, 200);
        this.fillColor = new Color(80, 80, 120, 200);
        
        // ★ NEW: Initialize notification
        this.isNewItem = false;
        this.newItemPulse = 0f;
        this.newItemTimer = 0f;
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
        
        // If empty, draw empty slot indicator (grid pattern)
        if (item == null) {
            drawEmptySlotPattern(g);
        } else {
            // ★ UPDATED: Draw item icon
            drawItemIcon(g);
        }
        
        // Draw hover effect
        if (hovered) {
            g.setColor(new Color(255, 255, 255, 30));
            g.fillRect(x, y, width, height);
        }
        
        // ★ NEW: Draw "NEW" badge if item was recently added
        if (item != null && isNewItem && newItemTimer > 0) {
            drawNewItemBadge(g);
        }
        
        // Draw stack count if stackable
        if (item != null && item.isStackable() && stackCount > 1) {
            Font originalFont = g.getFont();
            g.setFont(new Font("Arial", Font.BOLD, 12));
            
            String countText = String.valueOf(stackCount);
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(countText);
            
            int textX = x + width - textWidth - 4;
            int textY = y + height - 4;
            
            // Shadow
            g.setColor(Color.BLACK);
            g.drawString(countText, textX + 1, textY + 1);
            
            // Text
            g.setColor(Color.WHITE);
            g.drawString(countText, textX, textY);
            
            g.setFont(originalFont);
        }
    }
    
    /**
     * ★ NEW: Draw "NEW" badge for recently added items
     */
    private void drawNewItemBadge(Graphics2D g) {
        Font originalFont = g.getFont();
        g.setFont(NEW_BADGE_FONT);
        
        String text = "NEW";
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        int badgeWidth = textWidth + 6;
        int badgeHeight = textHeight + 2;
        int badgeX = x + 2;
        int badgeY = y + 2;
        
        // Pulsing effect with fade-out
        float fadeAlpha = Math.min(1.0f, newItemTimer / 2.0f); // Fade in first 2 seconds
        float pulseAlpha = 0.8f + (float)Math.sin(newItemPulse) * 0.2f;
        float finalAlpha = fadeAlpha * pulseAlpha;
        
        // Background
        Color bgColor = new Color(0, 200, 0, (int)(finalAlpha * 220));
        g.setColor(bgColor);
        g.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 3, 3);
        
        // Border
        Color borderColor = new Color(0, 255, 0, (int)(finalAlpha * 255));
        g.setColor(borderColor);
        g.drawRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 3, 3);
        
        // Text
        g.setColor(new Color(255, 255, 255, (int)(finalAlpha * 255)));
        int textX = badgeX + 3;
        int textY = badgeY + textHeight - 1;
        g.drawString(text, textX, textY);
        
        g.setFont(originalFont);
    }
    
    /**
     * Draw empty slot pattern (subtle grid/cross)
     */
    private void drawEmptySlotPattern(Graphics2D g) {
        g.setColor(new Color(80, 80, 80, 100));
        
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        int size = width / 3;
        
        // Draw small cross pattern
        g.fillRect(centerX - 1, centerY - size / 2, 2, size);
        g.fillRect(centerX - size / 2, centerY - 1, size, 2);
    }
    
    /**
     * ★ UPDATED: Draw actual item icon (with extensive debugging)
     */
    private void drawItemIcon(Graphics2D g) {
        if (item == null) {
            System.out.println("⚠️ drawItemIcon called but item is null!");
            return;
        }
        
        // Calculate icon position and size
        int itemSize = (int)(width * 0.75f);
        int itemX = x + (width - itemSize) / 2;
        int itemY = y + (height - itemSize) / 2;
        
        // Try to load item icon
        String iconPath = item.getIconPath();
        BufferedImage icon = null;
        
        // ★ DEBUG: Print icon loading attempt
        //System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        //System.out.println("🔍 Loading icon for: " + item.getName());
        //System.out.println("📁 Icon path: " + iconPath);
        
        if (iconPath != null && !iconPath.isEmpty()) {
            try {
                icon = dev.main.sprite.TextureManager.load(iconPath);
                
                if (icon != null) {
                   // System.out.println("✅ Icon loaded successfully!");
                   // System.out.println("   Size: " + icon.getWidth() + "x" + icon.getHeight());
                } else {
                    System.out.println("❌ TextureManager.load() returned null");
                }
            } catch (Exception e) {
                System.out.println("❌ Exception loading icon: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            //System.out.println("❌ Icon path is null or empty!");
        }
        
        if (icon != null) {
            // ★ Draw the actual icon image
            //System.out.println("🎨 Drawing icon at (" + itemX + ", " + itemY + ") size: " + itemSize);
            g.drawImage(icon, itemX, itemY, itemSize, itemSize, null);
            
            // Draw rarity border
            drawRarityBorder(g, itemX, itemY, itemSize);
        } else {
            // ★ Fallback: Draw enhanced placeholder
            System.out.println("🔸 Using fallback placeholder");
            drawFallbackPlaceholder(g, itemX, itemY, itemSize);
        }
        
        //System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
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
        // ★ NEW: Update "NEW" badge timer
        if (isNewItem && newItemTimer > 0) {
            newItemTimer -= delta;
            newItemPulse += delta * 3.0f; // Pulse speed
            
            if (newItemTimer <= 0) {
                isNewItem = false;
                newItemTimer = 0;
                newItemPulse = 0;
            }
        }
    }
    
    @Override
    public boolean onClick() {
        if (item == null) {
            System.out.println("Clicked empty inventory slot " + slotIndex);
        } else {
            System.out.println("Clicked inventory slot " + slotIndex + " with item: " + item.getName());
            System.out.println("  Icon path: " + item.getIconPath());
        }
        return true;
    }
 // ═══════════════════════════════════════════════════════════════════
 // REPLACE onRightClick() METHOD IN UIInventorySlot.java
 // This version handles ALL gear types, not just weapons
 // ═══════════════════════════════════════════════════════════════════

    @Override
    public boolean onRightClick() {
	     if (item != null) {
	         String itemName = item.getName();
	         
	         // ★ Check if item can be equipped
	         if (item.isEquippable()) {
	             String equipSlot = item.getEquipSlot();
	             UIGearSlot.SlotType slotType = null;
	             
	             // Convert string to SlotType enum
	             try {
	                 slotType = UIGearSlot.SlotType.valueOf(equipSlot);
	             } catch (IllegalArgumentException e) {
	                 System.out.println("❌ Invalid slot type: " + equipSlot);
	                 return true;
	             }
	             
	             // Try to equip the item
	             boolean equipped = uiManager.equipItem(slotType, item);
	             
	             if (equipped) {
	                 // Remove from inventory
	                 UIScrollableInventoryPanel inventoryPanel = uiManager.getInventoryGrid();
	                 inventoryPanel.removeItemFromSlot(slotIndex);
	                 System.out.println("✅ Equipped " + itemName + " to " + slotType + " slot");
	                 return true;
	             } else {
	                 // If RING_1 failed, try RING_2 (for rings)
	                 if (equipSlot.equals("RING_1")) {
	                     equipped = uiManager.equipItem(UIGearSlot.SlotType.RING_2, item);
	                     if (equipped) {
	                         UIScrollableInventoryPanel inventoryPanel = uiManager.getInventoryGrid();
	                         inventoryPanel.removeItemFromSlot(slotIndex);
	                         System.out.println("✅ Equipped " + itemName + " to RING_2 slot");
	                         return true;
	                     }
	                 }
	                 
	                 System.out.println("❌ Failed to equip " + itemName + " - slot occupied?");
	             }
	         } else {
	             System.out.println("ℹ️  Cannot equip " + itemName + " - not an equippable item");
	         }
	     }
	     return true;
 	}
 
    public void setItem(Item item) {
        this.item = item;
        this.stackCount = (item != null) ? 1 : 0;
        
        // ★ DEBUG: Print when item is set
        if (item != null) {
            System.out.println("📦 Item set in slot " + slotIndex + ": " + item.getName());
            System.out.println("   Icon path: " + item.getIconPath());
        }
    }
    
    /**
     * ★ NEW: Set item and mark as new
     */
    public void setItemAsNew(Item item) {
        setItem(item);
        if (item != null) {
            markAsNew();
        }
    }
    
    /**
     * ★ NEW: Mark this item as newly added
     */
    public void markAsNew() {
        this.isNewItem = true;
        this.newItemTimer = NEW_ITEM_DURATION;
        this.newItemPulse = 0f;
        System.out.println("✨ Marked slot " + slotIndex + " as NEW");
    }
    
    /**
     * ★ NEW: Clear the "NEW" badge
     */
    public void clearNewBadge() {
        this.isNewItem = false;
        this.newItemTimer = 0f;
    }
    
    public Item removeItem() {
        Item removed = this.item;
        this.item = null;
        this.stackCount = 0;
        return removed;
    }
    
    public Item getItem() {
        return item;
    }
    
    public boolean isEmpty() {
        return item == null;
    }
    
    public int getSlotIndex() {
        return slotIndex;
    }
    @Override
    public String getTooltipText() {
        if (item == null) return null;
        return getItemDescription(item);
    }

    /**
     * ★ UPDATED: Unified item description format
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
        
        // ★ Stackable info
        if (item.isStackable()) {
            sb.append("\n");
            sb.append("Stack: ").append(stackCount)
               .append("/").append(item.getMaxStackSize()).append("\n");
        }
        
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
    
    public void setStackCount(int count) {
        if (item != null && item.isStackable()) {
            this.stackCount = Math.min(count, item.getMaxStackSize());
        } else {
            this.stackCount = 1;
        }
    }

    public boolean addToStack(int amount) {
        if (item == null || !item.isStackable()) {
            return false;
        }
        
        int maxStack = item.getMaxStackSize();
        int newCount = stackCount + amount;
        
        if (newCount <= maxStack) {
            stackCount = newCount;
            return true;
        } else {
            return false;
        }
    }

    public int removeFromStack(int amount) {
        int removed = Math.min(amount, stackCount);
        stackCount -= removed;
        
        if (stackCount <= 0) {
            removeItem();
        }
        
        return removed;
    }

    public boolean hasRoomInStack(int amount) {
        if (item == null || !item.isStackable()) {
            return false;
        }
        return (stackCount + amount) <= item.getMaxStackSize();
    }

    public int getRemainingStackSpace() {
        if (item == null || !item.isStackable()) {
            return 0;
        }
        return item.getMaxStackSize() - stackCount;
    }

    public int getStackCount() {
        return stackCount;
    }
}