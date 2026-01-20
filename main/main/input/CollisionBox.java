package dev.main.input;

public class CollisionBox implements Component {
    public float offsetX;  // Offset from entity center
    public float offsetY;
    public float width;
    public float height;
    
    public CollisionBox(float offsetX, float offsetY, float width, float height) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;
    }
    
    // Get the actual collision box bounds in world space
    public float getLeft(float entityX) {
        return entityX + offsetX;
    }
    
    public float getRight(float entityX) {
        return entityX + offsetX + width;
    }
    
    public float getTop(float entityY) {
        return entityY + offsetY;
    }
    
    public float getBottom(float entityY) {
        return entityY + offsetY + height;
    }
    
    // Check if this box overlaps with another
    public boolean overlaps(float entityX, float entityY, CollisionBox other, float otherX, float otherY) {
        float left1 = getLeft(entityX);
        float right1 = getRight(entityX);
        float top1 = getTop(entityY);
        float bottom1 = getBottom(entityY);
        
        float left2 = other.getLeft(otherX);
        float right2 = other.getRight(otherX);
        float top2 = other.getTop(otherY);
        float bottom2 = other.getBottom(otherY);
        
        return !(right1 <= left2 || left1 >= right2 || bottom1 <= top2 || top1 >= bottom2);
    }
}