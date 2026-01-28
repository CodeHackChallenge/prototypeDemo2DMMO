package dev.main.stats; 
/**
 * Data class holding calculated mob stats
 */
public class MobStats {
    public final int hp;
    public final int attack;
    public final int defense;
    public final int accuracy;
    public final int evasion;
    
    public MobStats(int hp, int attack, int defense, int accuracy, int evasion) {
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.accuracy = accuracy;
        this.evasion = evasion;
    }
    
    @Override
    public String toString() {
        return String.format("MobStats[HP=%d, ATK=%d, DEF=%d, ACC=%d, EVA=%d]", 
            hp, attack, defense, accuracy, evasion);
    }
}