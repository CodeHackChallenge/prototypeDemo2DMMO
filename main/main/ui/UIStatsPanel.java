package dev.main.ui;

import java.awt.Graphics2D;

import dev.main.entity.Entity;
import dev.main.entity.Experience;
import dev.main.skill.SkillLevel;
import dev.main.state.GameState;
import dev.main.stats.Stats;

import java.awt.Color;
import java.awt.Font;

/**
 * UI Panel for displaying hero stats
 */
public class UIStatsPanel extends UIComponent {
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font TEXT_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final Font SMALL_FONT = new Font("Arial", Font.PLAIN, 10);

    private GameState gameState;
    private boolean visible = false;

    public UIStatsPanel(int x, int y, int width, int height, GameState gameState) {
        super(x, y, width, height);
        this.gameState = gameState;
    }

    @Override
    public void update(float delta) {
        // No animation or updates needed
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public void render(Graphics2D g) {
        // Background
        g.setColor(new Color(40, 40, 40, 220));
        g.fillRect(x, y, width, height);

        // Border
        g.setColor(Color.WHITE);
        g.drawRect(x, y, width, height);

        // Title
        g.setFont(TITLE_FONT);
        g.setColor(Color.YELLOW);
        g.drawString("Hero Stats", x + 20, y + 30);

        Entity player = gameState.getPlayer();
        if (player == null) return;

        Experience exp = player.getComponent(Experience.class);
        Stats stats = player.getComponent(Stats.class);
        SkillLevel skillLevel = player.getComponent(SkillLevel.class);

        if (exp == null || stats == null) return;

        g.setFont(TEXT_FONT);
        g.setColor(Color.WHITE);

        int lineY = y + 60;
        int lineSpacing = 20;

        // Level and XP
        g.drawString("Level: " + exp.level, x + 20, lineY);
        lineY += lineSpacing;
        g.drawString("XP: " + (int)exp.currentXP + " / " + (int)exp.xpToNextLevel, x + 20, lineY);
        lineY += lineSpacing;

        // Skill Points
        int skillPoints = (skillLevel != null) ? skillLevel.availablePoints : 0;
        g.drawString("Skill Points: " + skillPoints, x + 20, lineY);
        lineY += lineSpacing * 2;

        // Stats
        g.setFont(SMALL_FONT);
        g.setColor(Color.CYAN);
        g.drawString("Combat Stats:", x + 20, lineY);
        lineY += lineSpacing;

        g.setColor(Color.WHITE);
        g.drawString("HP: " + stats.hp + " / " + stats.maxHp, x + 30, lineY);
        lineY += lineSpacing;
        g.drawString("Mana: " + stats.mana + " / " + stats.maxMana, x + 30, lineY);
        lineY += lineSpacing;
        g.drawString("Stamina: " + (int)stats.stamina + " / " + (int)stats.getMaxStamina(), x + 30, lineY);
        lineY += lineSpacing;
        g.drawString("Attack: " + stats.attack, x + 30, lineY);
        lineY += lineSpacing;
        g.drawString("Defense: " + stats.defense, x + 30, lineY);
        lineY += lineSpacing;
        g.drawString("Magic Attack: " + stats.magicAttack, x + 30, lineY);
        lineY += lineSpacing;
        g.drawString("Magic Defense: " + stats.magicDefense, x + 30, lineY);
    }
}