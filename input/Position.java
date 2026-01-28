package dev.main.input;
 

public class Position implements Component {
    public float x, y;
    public float prevX, prevY;  // For interpolation later
    
    public Position(float x, float y) {
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
    }
}