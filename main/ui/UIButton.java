package dev.main.ui;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import dev.main.sprite.TextureManager;

import java.awt.Color;

/**
 * UI Button component with notification system
 * - Shows "NEW!" when first unlocked
 * - Shows notification indicators for updates (!, ?, numbers)
 * - Auto-clears on click
 */
public class UIButton extends UIComponent {
    
    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final Font NOTIFICATION_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font NEW_BADGE_FONT = new Font("Arial", Font.BOLD, 10);
    
    // Notification types
    public enum NotificationType {
        NONE,           // No notification
        NEW,            // "NEW!" badge (for first unlock)
        ALERT,          // Red "!" (urgent)
        INFO,           // Yellow "?"
        COUNT           // Number badge
    }
    
    private String id;
    private String label;
    private BufferedImage iconNormal;
    private BufferedImage iconHover;
    private BufferedImage iconLocked;
    private boolean locked;
    
    private String iconPathNormal;
    private String iconPathHover;
    private String iconPathLocked;
    
    private String tooltipText;
    
    // Callback for when button is clicked
    private Runnable onClickCallback;
    
    // ★ NEW: Enhanced notification system
    private NotificationType notificationType;
    private String notificationText;  // For COUNT type
    private boolean wasJustUnlocked;  // Track if button was just unlocked
    
    // Animation for notifications
    private float notificationPulse;
    private float pulseSpeed = 3f;
    
    public UIButton(int x, int y, int width, int height, String id, String label) {
        super(x, y, width, height);
        this.id = id;
        this.label = label;
        this.locked = false;
        this.iconNormal = null;
        this.iconHover = null;
        this.iconLocked = null;
        this.onClickCallback = null;
        
        // ★ NEW: Initialize notification system
        this.notificationType = NotificationType.NONE;
        this.notificationText = null;
        this.wasJustUnlocked = false;
        this.notificationPulse = 0f;
    }
    
    /**
     * Set items paths (will be loaded when needed)
     */
    public void setIcons(String normalPath, String hoverPath, String lockedPath) {
        this.iconPathNormal = normalPath;
        this.iconPathHover = hoverPath;
        this.iconPathLocked = lockedPath;
        
        // Load icons
        if (normalPath != null) {
            iconNormal = TextureManager.load(normalPath);
        }
        if (hoverPath != null) {
            iconHover = TextureManager.load(hoverPath);
        }
        if (lockedPath != null) {
            iconLocked = TextureManager.load(lockedPath);
        }
    }
    
    /**
     * Set single items (same for all states)
     */
    public void setIcon(String iconPath) {
        setIcons(iconPath, iconPath, iconPath);
    }
    
    /**
     * Set click callback
     */
    public void setOnClick(Runnable callback) {
        this.onClickCallback = callback;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ★ NEW: NOTIFICATION SYSTEM
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Set notification type and optional text
     */
    public void setNotification(NotificationType type, String text) {
        this.notificationType = type;
        this.notificationText = text;
        this.notificationPulse = 0f;
    }
    
    /**
     * Set notification with default text
     */
    public void setNotification(NotificationType type) {
        setNotification(type, null);
    }
    
    /**
     * Clear all notifications
     */
    public void clearNotification() {
        this.notificationType = NotificationType.NONE;
        this.notificationText = null;
        this.wasJustUnlocked = false;
    }
    
    /**
     * Check if button has any notification
     */
    public boolean hasNotification() {
        return notificationType != NotificationType.NONE;
    }
    
    /**
     * Set alert notification (red !)
     */
    public void setAlertNotification() {
        setNotification(NotificationType.ALERT);
    }
    
    /**
     * Set info notification (yellow ?)
     */
    public void setInfoNotification() {
        setNotification(NotificationType.INFO);
    }
    
    /**
     * Set count notification (number badge)
     */
    public void setCountNotification(int count) {
        setNotification(NotificationType.COUNT, String.valueOf(count));
    }
    
    /**
     * Mark as "NEW" (shown when first unlocked)
     */
    private void markAsNew() {
        this.wasJustUnlocked = true;
        setNotification(NotificationType.NEW);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // RENDERING
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
        
        // Choose which items to display
        BufferedImage currentIcon = null;
        
        if (locked && iconLocked != null) {
            currentIcon = iconLocked;
        } else if (hovered && !locked && iconHover != null) {
            currentIcon = iconHover;
        } else if (iconNormal != null) {
            currentIcon = iconNormal;
        }
        
        // Draw items
        if (currentIcon != null) {
            if (locked) {
                // Apply transparency to locked icons
                java.awt.AlphaComposite alphaComposite = java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, 0.4f);
                java.awt.Composite oldComposite = g.getComposite();
                g.setComposite(alphaComposite);
                g.drawImage(currentIcon, x, y, width, height, null);
                g.setComposite(oldComposite);
                
                // Draw small lock items overlay
                drawLockOverlay(g);
            } else {
                g.drawImage(currentIcon, x, y, width, height, null);
            }
        } else {
            // Fallback: draw colored rectangle if no items
            if (locked) {
                g.setColor(new java.awt.Color(80, 80, 80, 120));
            } else if (hovered) {
                g.setColor(new java.awt.Color(150, 150, 150, 220));
            } else {
                g.setColor(new java.awt.Color(100, 100, 100, 200));
            }
            g.fillRect(x, y, width, height);
            
            // Draw label
            Font originalFont = g.getFont();
            g.setFont(LABEL_FONT);
            g.setColor(locked ? new java.awt.Color(150, 150, 150) : java.awt.Color.WHITE);
            g.drawString(label, x + 5, y + height / 2 + 5);
            g.setFont(originalFont);
        }
        
        // ★ NEW: Draw notification indicator
        if (notificationType != NotificationType.NONE && !locked) {
            drawNotification(g);
        }
    }
    
    /**
     * ★ NEW: Draw notification indicator based on type
     */
    private void drawNotification(Graphics2D g) {
        Font originalFont = g.getFont();
        
        switch (notificationType) {
            case NEW:
                drawNewBadge(g);
                break;
                
            case ALERT:
                drawAlertIndicator(g);
                break;
                
            case INFO:
                drawInfoIndicator(g);
                break;
                
            case COUNT:
                drawCountBadge(g);
                break;
                
            default:
                break;
        }
        
        g.setFont(originalFont);
    }
    
    /**
     * Draw "NEW!" badge (green, bottom-right)
     */
    private void drawNewBadge(Graphics2D g) {
        g.setFont(NEW_BADGE_FONT);
        
        String text = "NEW!";
        java.awt.FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        int badgeWidth = textWidth + 8;
        int badgeHeight = textHeight + 4;
        int badgeX = x + width - badgeWidth - 2;
        int badgeY = y + height - badgeHeight - 2;
        
        // Pulsing background
        float alpha = 0.8f + (float)Math.sin(notificationPulse) * 0.2f;
        Color bgColor = new Color(0, 200, 0, (int)(alpha * 255));
        
        g.setColor(bgColor);
        g.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 4, 4);
        
        // Border
        g.setColor(new Color(0, 255, 0));
        g.drawRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 4, 4);
        
        // Text
        g.setColor(Color.WHITE);
        int textX = badgeX + 4;
        int textY = badgeY + textHeight - 2;
        g.drawString(text, textX, textY);
    }
    
    /**
     * Draw alert indicator (red !, top-right)
     */
    private void drawAlertIndicator(Graphics2D g) {
        g.setFont(NOTIFICATION_FONT);
        
        // Pulsing effect
        float scale = 1.0f + (float)Math.sin(notificationPulse) * 0.15f;
        int size = (int)(20 * scale);
        
        int indicatorX = x + width - size - 4;
        int indicatorY = y + 4;
        
        // Background circle
        g.setColor(new Color(255, 0, 0, 220));
        g.fillOval(indicatorX, indicatorY, size, size);
        
        // Border
        g.setColor(new Color(255, 100, 100));
        g.drawOval(indicatorX, indicatorY, size, size);
        
        // "!" text
        g.setColor(Color.WHITE);
        java.awt.FontMetrics fm = g.getFontMetrics();
        String text = "!";
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int textX = indicatorX + (size - textWidth) / 2;
        int textY = indicatorY + (size + textHeight / 2) / 2 - 1;
        g.drawString(text, textX, textY);
    }
    
    /**
     * Draw info indicator (yellow ?, top-right)
     */
    private void drawInfoIndicator(Graphics2D g) {
        g.setFont(NOTIFICATION_FONT);
        
        // Pulsing effect
        float scale = 1.0f + (float)Math.sin(notificationPulse) * 0.1f;
        int size = (int)(20 * scale);
        
        int indicatorX = x + width - size - 4;
        int indicatorY = y + 4;
        
        // Background circle
        g.setColor(new Color(255, 200, 0, 220));
        g.fillOval(indicatorX, indicatorY, size, size);
        
        // Border
        g.setColor(new Color(255, 220, 100));
        g.drawOval(indicatorX, indicatorY, size, size);
        
        // "?" text
        g.setColor(Color.WHITE);
        java.awt.FontMetrics fm = g.getFontMetrics();
        String text = "?";
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int textX = indicatorX + (size - textWidth) / 2;
        int textY = indicatorY + (size + textHeight / 2) / 2 - 1;
        g.drawString(text, textX, textY);
    }
    
    /**
     * Draw count badge (number, top-right)
     */
    private void drawCountBadge(Graphics2D g) {
        if (notificationText == null || notificationText.isEmpty()) return;
        
        g.setFont(NEW_BADGE_FONT);
        
        java.awt.FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(notificationText);
        int textHeight = fm.getHeight();
        
        int badgeWidth = Math.max(18, textWidth + 8);
        int badgeHeight = 16;
        int badgeX = x + width - badgeWidth - 2;
        int badgeY = y + 2;
        
        // Background
        g.setColor(new Color(255, 100, 100, 220));
        g.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 8, 8);
        
        // Border
        g.setColor(new Color(255, 150, 150));
        g.drawRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 8, 8);
        
        // Text
        g.setColor(Color.WHITE);
        int textX = badgeX + (badgeWidth - textWidth) / 2;
        int textY = badgeY + textHeight - 3;
        g.drawString(notificationText, textX, textY);
    }
    
    /**
     * Draw lock items overlay for locked buttons
     */
    private void drawLockOverlay(Graphics2D g) {
        int lockSize = width / 3;
        int lockX = x + width - lockSize - 2;
        int lockY = y + 2;
        
        // Draw lock body
        g.setColor(new java.awt.Color(200, 200, 200, 200));
        g.fillRect(lockX + 2, lockY + 6, lockSize - 4, lockSize - 8);
        
        // Draw lock shackle
        g.drawArc(lockX + 3, lockY, lockSize - 6, lockSize - 4, 0, 180);
        
        // Draw keyhole
        g.setColor(new java.awt.Color(80, 80, 80, 200));
        int keyholeX = lockX + lockSize / 2 - 1;
        int keyholeY = lockY + lockSize / 2;
        g.fillOval(keyholeX, keyholeY, 2, 2);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // UPDATE & INTERACTION
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public void update(float delta) {
        // Animate notification pulse
        if (notificationType != NotificationType.NONE) {
            notificationPulse += delta * pulseSpeed;
        }
    }
    
    @Override
    public boolean onClick() {
        if (locked || !enabled) {
            System.out.println(label + " is locked!");
            return true;  // Still consume the click
        }
        
        // ★ NEW: Clear notification on click
        clearNotification();
        
        // Execute callback
        if (onClickCallback != null) {
            onClickCallback.run();
        } else {
            System.out.println("Clicked: " + label);
        }
        
        return true;  // Consume click
    }
    
    // ═══════════════════════════════════════════════════════════════
    // LOCK/UNLOCK SYSTEM
    // ═══════════════════════════════════════════════════════════════
    
    public boolean isLocked() {
        return locked;
    }
    
    public void setLocked(boolean locked) {
        this.locked = locked;
        this.enabled = !locked;
    }
    
    /**
     * ★ MODIFIED: Unlock button and show "NEW!" badge
     */
    public void unlock() {
        boolean wasLocked = this.locked;
        setLocked(false);
        setVisible(true);
        
        // ★ Show "NEW!" badge on first unlock
        if (wasLocked) {
            markAsNew();
            System.out.println("Unlocked: " + label + " (showing NEW badge)");
        }
    }

    public void lock() {
        setLocked(true);
        setVisible(true);
        clearNotification();  // Clear notifications when locking
    }
    /**
     * ★ NEW: Get tooltip text (implements UIComponent tooltip system)
     */
    @Override
    public String getTooltipText() {
        // Don't show tooltip if button is locked
        if (locked) {
            return label + "\n(Locked)";
        }
        
        // Return custom tooltip if set, otherwise return label
        return tooltipText != null ? tooltipText : label;
    }
    /**
     * ★ NEW: Set tooltip text for this button
     */
    public void setTooltipText(String tooltipText) {
        this.tooltipText = tooltipText;
    }
    // ═══════════════════════════════════════════════════════════════
    // GETTERS/SETTERS
    // ═══════════════════════════════════════════════════════════════
    
    public String getId() {
        return id;
    }
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String newLabel) {
        this.label = newLabel;
    }
    
    public NotificationType getNotificationType() {
        return notificationType;
    }
    
    public boolean wasJustUnlocked() {
        return wasJustUnlocked;
    }
}