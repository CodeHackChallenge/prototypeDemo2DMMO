package dev.main.stats;

import dev.main.entity.MobTier;

public class MobStatFactory {

    public static MobStats create(int level, MobTier tier) {

        // Player reference stats
        int baseHp  = 100 + 10 * (level - 1);
        int baseAtk = 10  + 2  * (level - 1);
        int baseDef = 2   + 1  * (level - 1);
        int baseAcc = 0   + 1  * (level - 1);
        int baseEva = 0; // hero EVA does not grow

        // Mob stats = player stats × tier multipliers
        int hp  = (int)(baseHp  * tier.hpMult);
        int atk = (int)(baseAtk * tier.atkMult);
        int def = (int)(baseDef * tier.defMult);
        int acc = (int)(baseAcc * tier.accMult);

        // Monster EVA grows per level
        int eva = (int)(tier.evaGrowth * level);

        return new MobStats(hp, atk, def, acc, eva);
    }
}
