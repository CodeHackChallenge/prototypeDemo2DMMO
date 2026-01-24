package dev.main.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import dev.main.item.Item;

/**
 * REFACTORED: All tabs share same inventory slots
 * Tabs filter items by category (Misc shows all)
 */
public class UIScrollableInventoryPanel extends UIComponent {
			  
    private List<UIInventorySlot> slots;
    private int columns;
    private int totalRows;
    private int visibleRows;
    private int slotSize;
    private int gap;
    private int padding;
    
    // Scrolling
    private int scrollOffsetY;
    private int maxScrollY;
    private float scrollbarAlpha;
    private float scrollbarFadeTimer;
    private boolean showScrollbar;
    
    // Visual
    private Color backgroundColor;
    private Color borderColor;
    private Color scrollbarColor;
    private Color scrollbarThumbColor;
    
    // Scrollbar dimensions
    private int scrollbarWidth;
    private int scrollbarX;
    private int scrollbarY;
    private int scrollbarHeight;
    private int thumbY;
    private int thumbHeight;
    
    // Scrollbar interaction
    private boolean mouseOverScrollbar;
    private boolean draggingThumb = false;
    private int dragOffset = 0;
    
    // ★ NEW: Single shared inventory storage
    //private Item[] sharedInventory;  // All items stored here
    // NEW:
    private ItemStack[] sharedInventory;  // All items WITH stack counts

    private String currentTab;
    
    // Reference to UIManager
    private UIManager uiManager;
    
    // Cached visible range
    private int cachedFirstVisibleRow = -1;
    private int cachedLastVisibleRow = -1;
    private int lastScrollOffsetY = -1;
    
    public UIScrollableInventoryPanel(int x, int y, int width, int height, 
                                      int columns, int totalRows, int visibleRows, UIManager uiManager) {
        super(x, y, width, height);
        
        this.uiManager = uiManager;
        
        this.columns = columns;
        this.totalRows = totalRows;
        this.visibleRows = visibleRows;
        this.gap = 4;
        this.padding = 8;
        
        this.slots = new ArrayList<>();
        this.scrollOffsetY = 0;
        this.maxScrollY = 0;
        this.scrollbarAlpha = 0f;
        this.scrollbarFadeTimer = 0f;
        this.showScrollbar = totalRows > visibleRows;
        this.mouseOverScrollbar = false;
        
        // Colors
        this.backgroundColor = new Color(20, 20, 30, 230);
        this.borderColor = new Color(100, 100, 120);
        this.scrollbarColor = new Color(40, 40, 50, 200);
        this.scrollbarThumbColor = new Color(120, 120, 140, 255);
        
        // Calculate slot size
        int availableWidth = width - (padding * 2) - (gap * (columns - 1));
        if (showScrollbar) {
            availableWidth -= 10;
        }
        this.slotSize = availableWidth / columns;
        
        // Scrollbar setup
        this.scrollbarWidth = 8;
        this.scrollbarX = x + width - scrollbarWidth - 2;
        this.scrollbarY = y + padding;
        this.scrollbarHeight = height - (padding * 2);
        
        // ★ NEW: Single shared inventory (5x10 = 50 slots)
        //this.sharedInventory = new Item[getTotalSlots()];
        this.sharedInventory = new ItemStack[getTotalSlots()];  // ★ Changed to ItemStack[]

        this.currentTab = "Misc";
        
        // Create slots
        createSlots();
        calculateScrollLimits();
        refreshSlotDisplay();
    }
    
    private void createSlots() {
        slots.clear();
        
        int slotIndex = 0;
        for (int row = 0; row < totalRows; row++) {
            for (int col = 0; col < columns; col++) {
                int slotX = x + padding + (col * (slotSize + gap));
                int slotY = y + padding + (row * (slotSize + gap));
                
                UIInventorySlot slot = new UIInventorySlot(slotX, slotY, slotSize, slotIndex, uiManager);
                slots.add(slot);
                slotIndex++;
            }
        }
    }
    
    private void calculateScrollLimits() {
        int contentHeight = (slotSize * totalRows) + (gap * (totalRows - 1));
        int viewportHeight = (slotSize * visibleRows) + (gap * (visibleRows - 1));
        maxScrollY = Math.max(0, contentHeight - viewportHeight);
    }
    
    private void updateVisibleRange() {
        if (scrollOffsetY == lastScrollOffsetY) {
            return;
        }
        
        int rowHeight = slotSize + gap;
        cachedFirstVisibleRow = Math.max(0, scrollOffsetY / rowHeight);
        cachedLastVisibleRow = Math.min(totalRows - 1, 
            (scrollOffsetY + height - padding * 2) / rowHeight + 1);
        
        lastScrollOffsetY = scrollOffsetY;
    }
    
    public void handleScroll(int wheelRotation) {
        if (!showScrollbar) return;
        
        int scrollAmount = wheelRotation * (slotSize + gap);
        scrollOffsetY = Math.max(0, Math.min(maxScrollY, scrollOffsetY + scrollAmount));
        
        scrollbarAlpha = 1.0f;
        scrollbarFadeTimer = 1.5f;
        
        updateSlotPositions();
    }
    /**
     * ★ NEW: Add item and mark slot as new
     */
    public boolean addItemToCurrentTabAsNew(Item item) {
        boolean added = addItemToCurrentTab(item);
        
        if (added) {
            // Find the slot where item was added and mark it as new
            List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
            
            for (int i = 0; i < filteredStacks.size() && i < slots.size(); i++) {
                ItemStack stack = filteredStacks.get(i);
                UIInventorySlot slot = slots.get(i);
                
                if (stack != null && stack.getItem() == item) {
                    slot.markAsNew();
                    System.out.println("✨ New item added: " + item.getName());
                    break;
                }
            }
        }
        
        return added;
    }
    // ═══════════════════════════════════════════════════════════════
    // ★ NEW: TAB FILTERING SYSTEM
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Switch to a tab (filters display but shares same inventory)
     */
    public void switchToTab(String tabName) {
        this.currentTab = tabName;
        scrollOffsetY = 0;  // Reset scroll to top
        refreshSlotDisplay();
    }
    
    /**
     * ★ FIXED: Refresh slot display preserving stack counts
     */
    private void refreshSlotDisplay() {
        // Get filtered item stacks for current tab
        List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
        
        // Clear all slots first
        for (UIInventorySlot slot : slots) {
            slot.removeItem();
            slot.setVisible(false);
        }
        
        // Populate visible slots with filtered items AND their stack counts
        for (int i = 0; i < filteredStacks.size() && i < slots.size(); i++) {
            UIInventorySlot slot = slots.get(i);
            ItemStack stack = filteredStacks.get(i);
            
            if (stack != null && stack.getItem() != null) {
                slot.setItem(stack.getItem());
                slot.setStackCount(stack.getStackCount());  // ★ Preserve stack count!
                slot.setVisible(true);
            }
        }
        
        // Update scroll limits based on filtered item count
        updateScrollLimitsForFilteredItems(filteredStacks.size());
    }
    /**
     * ★ NEW: Get stack count for an item from shared inventory
     */
    private int getStackCountForItem(Item item) {
        // For now, return 1 as default
        // You'll need to track stack counts separately or store them with items
        // This is a simplified version
        return 1;
    } 
    /**
     * Get items filtered by tab category (WITH stack counts)
     */
    private List<ItemStack> getFilteredItemStacks(String tabName) {
        List<ItemStack> filtered = new ArrayList<>();
        
        for (ItemStack stack : sharedInventory) {
            if (stack == null || stack.getItem() == null) continue;
            
            Item item = stack.getItem();
            
            // "Misc" shows everything
            if (tabName.equals("Misc")) {
                filtered.add(stack);
                continue;
            }
            
            // Filter by category
            if (matchesTabFilter(item, tabName)) {
                filtered.add(stack);
            }
        } 
        
        return filtered;
    }
    /**
     * Check if item matches tab filter
     */
    private boolean matchesTabFilter(Item item, String tabName) {
        Item.ItemType type = item.getType();
        
        switch (tabName) {
            case "Weap":
                return type == Item.ItemType.WEAPON;
                
            case "Arm":
                return type == Item.ItemType.ARMOR;
                
            case "Acc":
                return type == Item.ItemType.ACCESSORY;
                
            case "Rune":
                return type == Item.ItemType.MATERIAL && 
                       item.getName().toLowerCase().contains("rune");
                
            default:
                return false;
        }
    }
    
    /**
     * Update scroll limits based on filtered item count
     */
    private void updateScrollLimitsForFilteredItems(int itemCount) {
        int requiredRows = (int) Math.ceil((double) itemCount / columns);
        int contentHeight = (slotSize * requiredRows) + (gap * (requiredRows - 1));
        int viewportHeight = (slotSize * visibleRows) + (gap * (visibleRows - 1));
        maxScrollY = Math.max(0, contentHeight - viewportHeight);
        
        // Clamp scroll offset
        scrollOffsetY = Math.min(scrollOffsetY, maxScrollY);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ★ NEW: INVENTORY MANAGEMENT (Shared Storage)
    // ═══════════════════════════════════════════════════════════════
    /**
     * ★ MODIFIED: Update the existing addItemToCurrentTab to use the new version
     * This maintains backwards compatibility
     */
    public boolean addItemToCurrentTab(Item item) { 
        return addItemToCurrentTab(item, true);  // Mark as new by default
    }
    /**
     * ★ NEW: Add item with optional "NEW" badge marking
     * This is the main method that supports the badge system
     */
    public boolean addItemToCurrentTab(Item item, boolean markAsNew) {
        if (item == null) return false;
        
        int addedToSlotIndex = -1;
        
        // ★ If item is stackable, try to stack with existing items first
        if (item.isStackable()) {
            for (int i = 0; i < sharedInventory.length; i++) {
                ItemStack stack = sharedInventory[i];
                
                if (stack != null && stack.canStackWith(item)) {
                    if (stack.hasRoom(1)) {
                        stack.addToStack(1);
                        addedToSlotIndex = i;
                        refreshSlotDisplay();
//                        System.out.println("Stacked item: " + item.getName() + 
//                                         " (now " + stack.getStackCount() + "/" + 
//                                         item.getMaxStackSize() + ")");
                        break;
                    }
                }
            }
        }
        
        // ★ If can't stack (or not stackable), find empty slot
        if (addedToSlotIndex == -1) {
            for (int i = 0; i < sharedInventory.length; i++) {
                if (sharedInventory[i] == null) {
                    sharedInventory[i] = new ItemStack(item, 1);
                    addedToSlotIndex = i;
                    refreshSlotDisplay();
                    //System.out.println("Added " + item.getName() + " to inventory slot " + i);
                    break;
                }
            }
        }
        
        // ★ NEW: Mark slot as new if requested
        if (markAsNew && addedToSlotIndex != -1) {
            // Find the display slot for this inventory index
            List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
            for (int i = 0; i < filteredStacks.size() && i < slots.size(); i++) {
                ItemStack stack = filteredStacks.get(i);
                if (stack != null && sharedInventory[addedToSlotIndex] == stack) {
                    slots.get(i).markAsNew();
                   // System.out.println("✨ Marked slot " + i + " as NEW");
                    break;
                }
            }
        }
        
        if (addedToSlotIndex == -1) {
            System.out.println("Inventory full!");
            return false;
        }
        
        return true;
    } 
    /**
     * ★ COMPLETELY FIXED: Add multiple items with stacking
     */
    public boolean addItemStack(Item item, int quantity) {
        if (item == null || quantity <= 0) return false;
        
        if (!item.isStackable()) {
            // If not stackable, add each individually
            int added = 0;
            for (int i = 0; i < quantity; i++) {
                if (addItemToCurrentTab(item)) {
                    added++;
                } else {
                    break;
                }
            }
            return added == quantity;
        }
        
        // ★ Stackable items: distribute across stacks
        int remaining = quantity;
        
        // First pass: add to existing stacks
        for (int i = 0; i < sharedInventory.length && remaining > 0; i++) {
            ItemStack stack = sharedInventory[i];
            
            if (stack != null && stack.canStackWith(item)) {
                int roomInStack = stack.getRemainingSpace();
                if (roomInStack > 0) {
                    int toAdd = Math.min(remaining, roomInStack);
                    stack.addToStack(toAdd);
                    remaining -= toAdd;
                    System.out.println("Added " + toAdd + " to existing stack of " + item.getName());
                }
            }
        }
        
        // Second pass: create new stacks in empty slots
        while (remaining > 0) {
            int emptyIndex = -1;
            
            for (int i = 0; i < sharedInventory.length; i++) {
                if (sharedInventory[i] == null) {
                    emptyIndex = i;
                    break;
                }
            }
            
            if (emptyIndex == -1) {
                System.out.println("⚠ Inventory full! Lost " + remaining + " items");
                return false;
            }
            
            int toAdd = Math.min(remaining, item.getMaxStackSize());
            sharedInventory[emptyIndex] = new ItemStack(item, toAdd);
            remaining -= toAdd;
            System.out.println("Created new stack: " + toAdd + "x " + item.getName());
        }
        
        // Refresh display once at the end
        refreshSlotDisplay();
        return true;
    }
    /**
     * Remove item from specific slot index
     */
    public boolean removeItemFromSlot(int slotIndex) {
        List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
        
        if (slotIndex < 0 || slotIndex >= filteredStacks.size()) {
            return false;
        }
        
        ItemStack stackToRemove = filteredStacks.get(slotIndex);
        
        // Find and remove from shared inventory
        for (int i = 0; i < sharedInventory.length; i++) {
            if (sharedInventory[i] == stackToRemove) {
                sharedInventory[i] = null;
                refreshSlotDisplay();
                return true;
            }
        }
        
        return false;
    }
    /**
     * Get item at filtered slot index
     */
    public Item getItemAtSlot(int slotIndex) {
        List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
        if (slotIndex >= 0 && slotIndex < filteredStacks.size()) {
            ItemStack stack = filteredStacks.get(slotIndex);
            return stack != null ? stack.getItem() : null;
        }
        return null;
    }
    
    /**
     * Get total item count (all items, not filtered)
     */
    public int getTotalItemCount() {
        int count = 0;
        for (ItemStack stack : sharedInventory) {
            if (stack != null && stack.getItem() != null) count++;
        }
        return count;
    }
    
    /**
     * Get filtered item count (current tab)
     */
    public int getFilteredItemCount() {
        return getFilteredItemStacks(currentTab).size();
    }
    /**
     * Clear all items
     */
    public void clearInventory() {
        for (int i = 0; i < sharedInventory.length; i++) {
            sharedInventory[i] = null;
        }
        refreshSlotDisplay();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // RENDERING & UPDATES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Update slot positions (FIXED to use ItemStack)
     */
    private void updateSlotPositions() {
        List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
        int visibleSlotCount = Math.min(filteredStacks.size(), slots.size());
        
        for (int i = 0; i < visibleSlotCount; i++) {
            UIInventorySlot slot = slots.get(i);
            
            int row = i / columns;
            int col = i % columns;
            
            int slotX = x + padding + (col * (slotSize + gap));
            int slotY = y + padding + (row * (slotSize + gap)) - scrollOffsetY;
            
            slot.setPosition(slotX, slotY);
        }
    }
    /**
     * Render (FIXED to use ItemStack)
     */
    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
        
        // Draw background
        g.setColor(backgroundColor);
        g.fillRect(x, y, width, height);
        
        // Draw border
        g.setColor(borderColor);
        g.setStroke(new java.awt.BasicStroke(2));
        g.drawRect(x, y, width, height);
        
        // Create clipping region
        Rectangle oldClip = g.getClipBounds();
        g.setClip(x + padding, y + padding, 
                  width - padding * 2 - (showScrollbar ? 12 : 0), 
                  height - padding * 2);
        
        // Render visible slots
        updateVisibleRange();
        
        List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
        int visibleSlotCount = Math.min(filteredStacks.size(), slots.size());
        
        for (int i = 0; i < visibleSlotCount; i++) {
            int row = i / columns;
            
            if (row >= cachedFirstVisibleRow && row <= cachedLastVisibleRow) {
                UIInventorySlot slot = slots.get(i);
                if (slot.isVisible()) {
                    slot.render(g);
                }
            }
        }
        
        // Restore clip
        g.setClip(oldClip);
        
        // Draw scrollbar
        if (showScrollbar && scrollbarAlpha > 0) {
            drawScrollbar(g);
        }
        
        // Draw item count indicator
        drawItemCount(g, filteredStacks.size());
    }
    
    /**
     * Draw item count for current tab
     */
    private void drawItemCount(Graphics2D g, int count) {
        String countText = count + " / " + sharedInventory.length;
        
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(countText);
        
        int textX = x + width - textWidth - padding - (showScrollbar ? 12 : 0);
        int textY = y + height - 4;
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(countText, textX + 1, textY + 1);
        
        // Text
        g.setColor(new Color(180, 180, 180));
        g.drawString(countText, textX, textY);
    }
    
    /**
     * Draw scrollbar (FIXED to use ItemStack)
     */
    private void drawScrollbar(Graphics2D g) {
        List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
        int requiredRows = (int) Math.ceil((double) filteredStacks.size() / columns);
        
        if (requiredRows <= visibleRows) return;
        
        float scrollRatio = maxScrollY > 0 ? (float)scrollOffsetY / maxScrollY : 0;
        float thumbRatio = (float)visibleRows / requiredRows;
        
        thumbHeight = Math.max(20, (int)(scrollbarHeight * thumbRatio));
        thumbY = scrollbarY + (int)((scrollbarHeight - thumbHeight) * scrollRatio);
        
        // Scrollbar background
        int bgAlpha = (int)(scrollbarAlpha * 150);
        g.setColor(new Color(scrollbarColor.getRed(), scrollbarColor.getGreen(), 
                            scrollbarColor.getBlue(), bgAlpha));
        g.fillRoundRect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, 4, 4);
        
        // Thumb
        int thumbAlpha = (int)(scrollbarAlpha * 255);
        g.setColor(new Color(scrollbarThumbColor.getRed(), scrollbarThumbColor.getGreen(), 
                            scrollbarThumbColor.getBlue(), thumbAlpha));
        g.fillRoundRect(scrollbarX, thumbY, scrollbarWidth, thumbHeight, 4, 4);
    }
    
    /**
     * Update (FIXED to use ItemStack)
     */
    @Override
    public void update(float delta) {
        if (!visible) return;
        
        // Scrollbar fade
        if (scrollbarFadeTimer > 0) {
            scrollbarFadeTimer -= delta;
        } else if (!mouseOverScrollbar && scrollbarAlpha > 0) {
            scrollbarAlpha -= delta * 1.5f;
            if (scrollbarAlpha < 0) scrollbarAlpha = 0;
        }
        
        // Update visible slots
        updateVisibleRange();
        
        List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
        int visibleSlotCount = Math.min(filteredStacks.size(), slots.size());
        
        for (int i = 0; i < visibleSlotCount; i++) {
            int row = i / columns;
            
            if (row >= cachedFirstVisibleRow && row <= cachedLastVisibleRow) {
                slots.get(i).update(delta);
            }
        }
    }

    /**
     * Handle mouse move (FIXED to use ItemStack)
     */
  
    // ═══════════════════════════════════════════════════════════════
    // INPUT HANDLING
    // ═══════════════════════════════════════════════════════════════
    /**
     * Handle mouse move (FIXED to use ItemStack)
     */
    public void handleMouseMove(int mouseX, int mouseY, boolean pressed) {
        // Check scrollbar hover
        boolean wasOverScrollbar = mouseOverScrollbar;
        mouseOverScrollbar = showScrollbar && 
                            mouseX >= scrollbarX - 5 && 
                            mouseX <= scrollbarX + scrollbarWidth + 5 &&
                            mouseY >= scrollbarY && 
                            mouseY <= scrollbarY + scrollbarHeight;
        
        if (mouseOverScrollbar && !wasOverScrollbar) {
            scrollbarAlpha = 1.0f;
            scrollbarFadeTimer = 0;
        }
        
        // Update slot hovers
        updateVisibleRange();
        
        List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
        int visibleSlotCount = Math.min(filteredStacks.size(), slots.size());
        
        for (int i = 0; i < visibleSlotCount; i++) {
            int row = i / columns;
            
            if (row >= cachedFirstVisibleRow && row <= cachedLastVisibleRow) {
                UIInventorySlot slot = slots.get(i);
                if (!slot.isVisible() || !slot.isEnabled()) continue;
                
                boolean contains = slot.contains(mouseX, mouseY);
                
                if (contains && !slot.isHovered()) {
                    slot.onMouseEnter();
                } else if (!contains && slot.isHovered()) {
                    slot.onMouseExit();
                }
            }
        }
        
        // Handle thumb dragging
        if (draggingThumb) {
            if (!pressed) {
                draggingThumb = false;
            } else {
                int newThumbY = mouseY - dragOffset;
                int minThumbY = scrollbarY;
                int maxThumbY = scrollbarY + scrollbarHeight - thumbHeight;
                newThumbY = Math.max(minThumbY, Math.min(maxThumbY, newThumbY));
                
                float scrollRatio = (float)(newThumbY - scrollbarY) / (scrollbarHeight - thumbHeight);
                scrollOffsetY = (int)(scrollRatio * maxScrollY);
                updateSlotPositions();
                
                scrollbarAlpha = 1.0f;
                scrollbarFadeTimer = 0.5f;
            }
        }
    }
    /**
     * Handle click (FIXED to use ItemStack)
     */
    public boolean handleClick(int mouseX, int mouseY) {
        updateVisibleRange();
        
        List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
        int visibleSlotCount = Math.min(filteredStacks.size(), slots.size());
        
        for (int i = visibleSlotCount - 1; i >= 0; i--) {
            int row = i / columns;
            
            if (row >= cachedFirstVisibleRow && row <= cachedLastVisibleRow) {
                UIInventorySlot slot = slots.get(i);
                if (!slot.isVisible() || !slot.isEnabled()) continue;
                
                if (slot.contains(mouseX, mouseY)) {
                    return slot.onClick();
                }
            }
        }
        
        // Handle scrollbar thumb
        if (showScrollbar) {
            List<ItemStack> stacks = getFilteredItemStacks(currentTab);
            int requiredRows = (int) Math.ceil((double) stacks.size() / columns);
            
            if (requiredRows > visibleRows) {
                float scrollRatio = maxScrollY > 0 ? (float)scrollOffsetY / maxScrollY : 0;
                float thumbRatio = (float)visibleRows / requiredRows;
                thumbHeight = Math.max(20, (int)(scrollbarHeight * thumbRatio));
                thumbY = scrollbarY + (int)((scrollbarHeight - thumbHeight) * scrollRatio);
                
                if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                    mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    draggingThumb = true;
                    dragOffset = mouseY - thumbY;
                    scrollbarAlpha = 1.0f;
                    return true;
                }
            }
        }
        
        return this.contains(mouseX, mouseY);
    }
    /**
     * Handle right click (FIXED to use ItemStack)
     */
    public boolean handleRightClick(int mouseX, int mouseY) {
        updateVisibleRange();
        
        List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
        int visibleSlotCount = Math.min(filteredStacks.size(), slots.size());
        
        for (int i = visibleSlotCount - 1; i >= 0; i--) {
            int row = i / columns;
            
            if (row >= cachedFirstVisibleRow && row <= cachedLastVisibleRow) {
                UIInventorySlot slot = slots.get(i);
                if (!slot.isVisible() || !slot.isEnabled()) continue;
                
                if (slot.contains(mouseX, mouseY)) {
                    return slot.onRightClick();
                }
            }
        }
        
        return this.contains(mouseX, mouseY);
    }
   
    // ═══════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════
    
    public UIInventorySlot getSlot(int index) {
        if (index >= 0 && index < slots.size()) {
            return slots.get(index);
        }
        return null;
    }
    
    public List<UIInventorySlot> getSlots() {
        return slots;
    }
    
    public int getTotalSlots() {
        return columns * totalRows;
    }
    
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }
    
    public void setBorderColor(Color color) {
        this.borderColor = color;
    }
    
    /**
     * Get hovered slot (FIXED to use ItemStack)
     */
    public UIInventorySlot getHoveredSlot(int mouseX, int mouseY) {
        updateVisibleRange();
        
        List<ItemStack> filteredStacks = getFilteredItemStacks(currentTab);
        int visibleSlotCount = Math.min(filteredStacks.size(), slots.size());
        
        for (int i = visibleSlotCount - 1; i >= 0; i--) {
            int row = i / columns;
            
            if (row >= cachedFirstVisibleRow && row <= cachedLastVisibleRow) {
                UIInventorySlot slot = slots.get(i);
                if (!slot.isVisible() || !slot.isEnabled()) continue;
                
                if (slot.contains(mouseX, mouseY)) {
                    return slot;
                }
            }
        }
        return null;
    }
    
    public String getCurrentTab() {
        return currentTab;
    }
}