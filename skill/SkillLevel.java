package dev.main.skill;

import dev.main.input.Component;

/**
 * Tracks skill points and manages skill leveling
 */
public class SkillLevel implements Component {
    
    // Skill points
    public int availablePoints;  // Points available to spend
    public int totalPoints;      // Total points earned
    public int spentPoints;      // Points already spent
    
    public SkillLevel() {
        this.availablePoints = 0;
        this.totalPoints = 0;
        this.spentPoints = 0;
    }
    
    /**
     * Award skill points (e.g., on level-up)
     */
    public void awardPoints(int points) {
        availablePoints += points;
        totalPoints += points;
        System.out.println("Gained " + points + " skill point(s)! Available: " + availablePoints);
    }
    
    /**
     * Spend skill points
     * @return true if had enough points to spend
     */
    public boolean spendPoints(int cost) {
        if (availablePoints >= cost) {
            availablePoints -= cost;
            spentPoints += cost;
            return true;
        }
        return false;
    }
    
    /**
     * Refund skill points (for respec)
     */
    public void refundPoints(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot refund negative points");
        }
        if (spentPoints < amount) {
            throw new IllegalStateException(
                "Cannot refund " + amount + " points (only " + spentPoints + " spent)");
        }
        
        availablePoints += amount;
        spentPoints -= amount;
    }
    /**
     * Check if can afford an upgrade
     */
    public boolean canAfford(int cost) {
        return availablePoints >= cost;
    }
}