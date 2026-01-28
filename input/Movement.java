package dev.main.input;

public class Movement implements Component {
    public float walkSpeed;
    public float runSpeed;
    public float currentSpeed;
    
    public float targetX;
    public float targetY;
    public boolean isMoving;
    public boolean isRunning;
    public boolean isHasted;  // NEW: Haste effect (3x speed)
    
    public int direction;
    public int lastDirection;
    
    public float staminaCostPerSecond = 15f;
    
    // Direction constants
    public static final int DIR_EAST = 0;
    public static final int DIR_SOUTH_EAST = 1;
    public static final int DIR_SOUTH = 2;
    public static final int DIR_SOUTH_WEST = 3;
    public static final int DIR_WEST = 4;
    public static final int DIR_NORTH_WEST = 5;
    public static final int DIR_NORTH = 6;
    public static final int DIR_NORTH_EAST = 7;
    
    public Movement(float walkSpeed, float runSpeed) {
        this.walkSpeed = walkSpeed;
        this.runSpeed = runSpeed;
        this.currentSpeed = walkSpeed;
        this.isMoving = false;
        this.isRunning = false;
        this.isHasted = false;  // NEW
        this.direction = DIR_SOUTH;
        this.lastDirection = DIR_SOUTH;
    }
    
    public void setTarget(float x, float y, boolean run) {
        this.targetX = x;
        this.targetY = y;
        this.isMoving = true;
        this.isRunning = run;
        updateSpeed();
    }
    
    public void stopMoving() {
        this.isMoving = false;
        this.isRunning = false;
        this.isHasted = false;  // Clear haste when stopping
        this.currentSpeed = walkSpeed;
        this.lastDirection = this.direction;
    }
    
    public void stopRunning() {
        this.isRunning = false;
        updateSpeed();
    }
    
    public void setHaste(boolean hasted) {
        this.isHasted = hasted;
        updateSpeed();
    }
    
    private void updateSpeed() {
        if (isHasted) {
            // Haste gives 3x speed
            currentSpeed = walkSpeed * 3f;
        } else if (isRunning) {
            currentSpeed = runSpeed;
        } else {
            currentSpeed = walkSpeed;
        }
    }
}
