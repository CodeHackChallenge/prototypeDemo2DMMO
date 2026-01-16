package dev.main.input;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * OPTIMIZED: Added movement tracking to prevent unnecessary hover checks
 */
public class MouseInput implements MouseListener, MouseMotionListener {
    
    private int mouseX;
    private int mouseY;
    private int lastMouseX;
    private int lastMouseY;
    private boolean mousePressed;
    
    private boolean leftClick;
    private boolean rightClick;
    private boolean mouseMoved;  // ⭐ NEW: Track if mouse actually moved
    
    public MouseInput() {
        this.mousePressed = false;
        this.leftClick = false;
        this.rightClick = false;
        this.mouseMoved = false;  // ⭐ NEW
        this.lastMouseX = -1;
        this.lastMouseY = -1;
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        mousePressed = true;
        
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftClick = true;
        }
        if (e.getButton() == MouseEvent.BUTTON3) {
            rightClick = true;
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        // Clear current pressed state on release
        mousePressed = false;
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        int newX = e.getX();
        int newY = e.getY();
        
        // ⭐ NEW: Only mark as moved if position actually changed
        if (newX != mouseX || newY != mouseY) {
            mouseMoved = true;
            mouseX = newX;
            mouseY = newY;
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        int newX = e.getX();
        int newY = e.getY();
        
        // ⭐ NEW: Track movement during drag
        if (newX != mouseX || newY != mouseY) {
            mouseMoved = true;
            mouseX = newX;
            mouseY = newY;
        }
    }
    
    // ⭐ NEW: Check if mouse moved since last reset
    public boolean hasMoved() {
        return mouseMoved;
    }
    
    // ⭐ NEW: Reset movement flag after processing
    public void resetMoved() {
        mouseMoved = false;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }
    
    public int getX() { return mouseX; }
    public int getY() { return mouseY; }
    public boolean isPressed() { return mousePressed; }
    public boolean isLeftClick() { return leftClick; }
    public boolean isRightClick() { return rightClick; }
    
    public void resetPressed() {
        // Clear one-shot click flags but keep current pressed state
        leftClick = false;
        rightClick = false;
    }
    
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}