package dev.main.entity;

public enum MobTier {
    TRASH   (0.40, 0.50, 0.40, 0.80, 0.40),
    NORMAL  (0.70, 0.80, 0.70, 1.00, 0.60),
    ELITE   (1.20, 1.10, 1.00, 1.10, 0.80),
    MINIBOSS(3.00, 1.50, 1.30, 1.20, 1.00);

    public final double hpMult;
    public final double atkMult;
    public final double defMult;
    public final double accMult;
    public final double evaGrowth; // EVA grows per level

    MobTier(double hpMult, double atkMult, double defMult, double accMult, double evaGrowth) {
        this.hpMult = hpMult;
        this.atkMult = atkMult;
        this.defMult = defMult;
        this.accMult = accMult;
        this.evaGrowth = evaGrowth;
    }
}