package dev.main.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.main.Engine;
import dev.main.ai.AI;
import dev.main.ai.AI.State;
import dev.main.bars.HealthBar;
import dev.main.bars.ManaBar;
import dev.main.bars.StaminaBar;
import dev.main.entity.Entity;
import dev.main.entity.EntityType;
import dev.main.entity.Experience;
import dev.main.entity.LevelUpEffect;
import dev.main.entity.MobTier;
import dev.main.entity.MonsterLevel;
import dev.main.entity.NameTag;
import dev.main.entity.Portal;
import dev.main.entity.SpawnPoint;
import dev.main.entity.TargetIndicator;
import dev.main.input.CollisionBox;
import dev.main.input.Movement;
import dev.main.input.Position;
import dev.main.pathfinder.Path;
import dev.main.quest.QuestIndicator;
import dev.main.sprite.Sprite;
import dev.main.state.GameState;
import dev.main.stats.Stats;
import dev.main.tile.TileMap;
import dev.main.ui.TransitionEffect;
import dev.main.util.Alert;
import dev.main.util.DamageText;
import dev.main.util.Dead;
import dev.main.util.DamageText.Type; 

/**
 * OPTIMIZED: Fixed double entity sorting and added batched state changes
 */
public class Renderer {
	
	private static final Font QUEST_INDICATOR_FONT = new Font("Arial", Font.BOLD, 24);

	
    private GameState gameState;
    private Engine engine;
    
    // ⭐ NEW: Cached fonts
    private static final Font NAME_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final Font LEVEL_BADGE_FONT = new Font("Arial", Font.BOLD, 9);
    private static final Font DAMAGE_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font DAMAGE_CRIT_FONT = new Font("Arial", Font.BOLD, 20);
    private static final Font LEVELUP_FONT = new Font("Arial", Font.BOLD, 20);
    private static final Font TIMER_FONT = new Font("Arial", Font.BOLD, 12);
    private static final Font ALERT_FONT = new Font("Arial", Font.BOLD, 24);
    
    // ⭐ NEW: Reusable sorted list
    private List<RenderObject> sortedRenderObjects;
    
    public Renderer(GameState gameState, Engine engine) {
        this.gameState = gameState;
        this.engine = engine;
        this.sortedRenderObjects = new ArrayList<>();
    }
    
    public void render(Graphics2D g) {
        float cameraX = gameState.getCameraX();
        float cameraY = gameState.getCameraY();
        
        // ⭐ NEW: Build sorted list ONCE
        buildSortedRenderList();
        
        // Render all layers
        renderGround(g, cameraX, cameraY);
        renderGroundDecor(g, cameraX, cameraY);
        renderEntities(g, cameraX, cameraY);
        renderEffects(g, cameraX, cameraY);
        renderWorldUI(g, cameraX, cameraY);
        renderScreenUI(g, cameraX, cameraY);
        
        gameState.getUIManager().render(g);
        
        if (engine.isDebugMode()) {
            renderDebug(g, cameraX, cameraY);
        }
        
        // ★ NEW: Render transition effect LAST (on top of everything)
        TransitionEffect transition = gameState.getTransitionEffect();
        if (transition != null && transition.isActive()) {
            transition.render(g);
        }
    }
    
    // ⭐ NEW: Build and sort render list once per frame
    private void buildSortedRenderList() {
        sortedRenderObjects.clear();
        
        for (Entity entity : gameState.getEntities()) {
            Position pos = entity.getComponent(Position.class);
            Renderable renderable = entity.getComponent(Renderable.class);
            Sprite sprite = entity.getComponent(Sprite.class);
            
            if (pos != null && renderable != null && sprite != null) {
                if (renderable.layer == RenderLayer.ENTITIES) {
                    sortedRenderObjects.add(new RenderObject(entity, pos, renderable));
                }
            }
        }
        
        Collections.sort(sortedRenderObjects);  // ⭐ Sort ONCE
    }
    
    private void renderGround(Graphics2D g, float cameraX, float cameraY) {
        TileMap map = gameState.getMap();
        if (map != null) {
            map.render(g, cameraX, cameraY);
        }
    }
    
    private void renderGroundDecor(Graphics2D g, float cameraX, float cameraY) {
        
    	// ★ Render portals first (under entities)
        for (Entity entity : gameState.getEntities()) {
            if (entity.getType() == EntityType.PORTAL) {
                Position pos = entity.getComponent(Position.class);
                Portal portal = entity.getComponent(Portal.class);
                
                if (pos != null && portal != null) {
                    int screenX = (int)Math.round(pos.x - cameraX);
                    int screenY = (int)Math.round(pos.y - cameraY);
                    
                    PortalRenderer.renderPortal(g, screenX, screenY, portal);
                }
            }
        }
        
    	for (Entity entity : gameState.getEntities()) {
            TargetIndicator indicator = entity.getComponent(TargetIndicator.class);
            if (indicator != null && indicator.active) {
                int screenX = (int)Math.round(indicator.worldX - cameraX);
                int screenY = (int)Math.round(indicator.worldY - cameraY);
                DiamondRenderer.renderDiamond(g, screenX, screenY, indicator.pulseScale, 1.0f);
            }
        }
    }
    
    // ⭐ OPTIMIZED: Use pre-sorted list
    private void renderEntities(Graphics2D g, float cameraX, float cameraY) {
        for (RenderObject ro : sortedRenderObjects) {
            Entity entity = ro.entity;
            Position pos = ro.position;
            
            int spriteScreenX = (int)Math.round(pos.x - cameraX);
            int spriteScreenY = (int)Math.round(pos.y - cameraY);
            
            Sprite sprite = entity.getComponent(Sprite.class);
            if (sprite != null) {
                sprite.renderAtPixel(g, spriteScreenX, spriteScreenY);
            }
        }
    }
    
    private void renderEffects(Graphics2D g, float cameraX, float cameraY) {
        drawDamageTexts(g, cameraX, cameraY);
    }
     
 // ★★★ OPTIMIZED: BATCHED RENDERING BY FONT TYPE ★★★
    private void renderWorldUI(Graphics2D g, float cameraX, float cameraY) {
        Font originalFont = g.getFont();
        
     // ========================================
        // BATCH 1: ALERTS (existing code - keep as is)
        // ========================================
        boolean hasAlerts = false;
        for (RenderObject ro : sortedRenderObjects) {
            Alert alert = ro.entity.getComponent(Alert.class);
            if (alert != null && alert.active) {
                hasAlerts = true;
                break;
            }
        }
        
        if (hasAlerts) {
            g.setFont(ALERT_FONT);
            for (RenderObject ro : sortedRenderObjects) {
                Dead dead = ro.entity.getComponent(Dead.class);
                if (dead != null) continue;
                
                Alert alert = ro.entity.getComponent(Alert.class);
                if (alert != null && alert.active) {
                    int screenX = (int)Math.round(ro.position.x - cameraX);
                    int screenY = (int)Math.round(ro.position.y - cameraY);
                    drawAlertOnly(g, screenX, screenY, alert);
                }
            }
        }
        
     // BATCH: QUEST INDICATORS (Clean version)
        g.setFont(QUEST_INDICATOR_FONT);

        for (Entity entity : gameState.getEntities()) {
            if (entity.getType() != EntityType.NPC) continue;
            
            Dead dead = entity.getComponent(Dead.class);
            if (dead != null) continue;
            
            Position pos = entity.getComponent(Position.class);
            if (pos == null) continue;
            
            QuestIndicator qi = entity.getComponent(QuestIndicator.class);
            if (qi == null || !qi.active) continue;
            
            int screenX = (int)Math.round(pos.x - cameraX);
            int screenY = (int)Math.round(pos.y - cameraY);
            
            // Get symbol and color
            String symbol;
            Color color;
            
            if (qi.type == QuestIndicator.IndicatorType.IN_PROGRESS) {
                symbol = qi.getSymbol();//"...";
                color = qi.getColor();//new Color(150, 150, 150);
            } else if (qi.type == QuestIndicator.IndicatorType.COMPLETE) {
                symbol = qi.getSymbol();//"?";
                color = qi.getColor();//new Color(255, 215, 0);
            } else {
                symbol = qi.getSymbol();//"!";
                color = qi.getColor();//new Color(255, 215, 0);
            }
            
            // Draw indicator
            int indicatorX = screenX;
            int indicatorY = (int)(screenY + qi.offsetY + qi.bounceOffset);
            
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(symbol);
            int textHeight = fm.getHeight();
            
            int textX = indicatorX - textWidth / 2;
            int textY = indicatorY + textHeight / 4;
            
            // Shadow
            g.setColor(new Color(0, 0, 0, 150));
            g.drawString(symbol, textX + 2, textY + 2);
            
            // Symbol
            g.setColor(color);
            g.drawString(symbol, textX, textY);
        }
        // ========================================
        // BATCH 2: LEVEL BADGES (existing code - keep as is)
        // ========================================
        g.setFont(LEVEL_BADGE_FONT);
        
        for (RenderObject ro : sortedRenderObjects) {
            Dead dead = ro.entity.getComponent(Dead.class);
            if (dead != null) continue;
            
            int screenX = (int)Math.round(ro.position.x - cameraX);
            int screenY = (int)Math.round(ro.position.y - cameraY);
            
            if (ro.entity.getType() == EntityType.MONSTER) {
                MonsterLevel monsterLevel = ro.entity.getComponent(MonsterLevel.class);
                NameTag nameTag = ro.entity.getComponent(NameTag.class);
                
                if (monsterLevel != null && nameTag != null && nameTag.visible) {
                    drawMonsterLevelBadgeOnly(g, screenX, screenY, monsterLevel);
                }
            }
            else if (ro.entity.getType() == EntityType.PLAYER) {
                Experience exp = ro.entity.getComponent(Experience.class);
                if (exp != null) {
                    drawLevelBadgeOnly(g, screenX, screenY, exp.level);
                }
            }
        }
        
        // ========================================
        // BATCH 3: NAME TAGS
        // ========================================
        g.setFont(NAME_FONT);
        
        for (RenderObject ro : sortedRenderObjects) {
            Dead dead = ro.entity.getComponent(Dead.class);
            if (dead != null) continue;
            
            NameTag nameTag = ro.entity.getComponent(NameTag.class);
            if (nameTag != null && nameTag.visible) {
                int screenX = (int)Math.round(ro.position.x - cameraX);
                int screenY = (int)Math.round(ro.position.y - cameraY);
                drawNameTagOnly(g, screenX, screenY, nameTag, ro.entity);
            }
        }
        
        // ========================================
        // BATCH 4: HEALTH/STAMINA/MANA BARS (NO FONT NEEDED)
        // ========================================
        for (RenderObject ro : sortedRenderObjects) {
            Dead dead = ro.entity.getComponent(Dead.class);
            if (dead != null) continue;
            
            int screenX = (int)Math.round(ro.position.x - cameraX);
            int screenY = (int)Math.round(ro.position.y - cameraY);
            
            Stats stats = ro.entity.getComponent(Stats.class);
            
            // Health bar
            HealthBar hpBar = ro.entity.getComponent(HealthBar.class);
            if (stats != null && hpBar != null) {
                drawHealthBar(g, screenX, screenY, stats, hpBar, ro);
            }
            
            // Player-specific bars
            if (ro.entity.getType() == EntityType.PLAYER) {
                StaminaBar staminaBar = ro.entity.getComponent(StaminaBar.class);
                if (stats != null && staminaBar != null) {
                    drawStaminaBar(g, screenX, screenY, stats, staminaBar);
                }
                
                ManaBar manaBar = ro.entity.getComponent(ManaBar.class);
                if (stats != null && manaBar != null) {
                    drawManaBar(g, screenX, screenY, stats, manaBar);
                }
                
                Experience exp = ro.entity.getComponent(Experience.class);
                if (exp != null) {
                    drawXPBar(g, screenX, screenY, exp);
                }
            }
        }
        
        // ========================================
        // BATCH 5: LEVEL-UP EFFECTS
        // ========================================
        boolean hasLevelUps = false;
        for (RenderObject ro : sortedRenderObjects) {
            LevelUpEffect levelUpEffect = ro.entity.getComponent(LevelUpEffect.class);
            if (levelUpEffect != null && levelUpEffect.active) {
                hasLevelUps = true;
                break;
            }
        }
        
        if (hasLevelUps) {
            g.setFont(LEVELUP_FONT);
            for (RenderObject ro : sortedRenderObjects) {
                if (ro.entity.getType() == EntityType.PLAYER) {
                    LevelUpEffect levelUpEffect = ro.entity.getComponent(LevelUpEffect.class);
                    if (levelUpEffect != null && levelUpEffect.active) {
                        int screenX = (int)Math.round(ro.position.x - cameraX);
                        int screenY = (int)Math.round(ro.position.y - cameraY);
                        drawLevelUpEffectOnly(g, screenX, screenY, levelUpEffect);
                    }
                }
            }
        }
        
        g.setFont(originalFont);
    }
    private void renderScreenUI(Graphics2D g, float cameraX, float cameraY) {
        // Screen-space UI
    }
    
    private void renderDebug(Graphics2D g, float cameraX, float cameraY) {
        TileMap map = gameState.getMap();
        
        if (map != null) {
            drawTileGrid(g, map, cameraX, cameraY);
        }
        
        drawDebugSpawnPoints(g, cameraX, cameraY);
        
        for (Entity entity : gameState.getEntities()) {
            Position pos = entity.getComponent(Position.class);
            
            if (pos != null) {
                int screenX = (int)Math.round(pos.x - cameraX);
                int screenY = (int)Math.round(pos.y - cameraY);
                
                CollisionBox box = entity.getComponent(CollisionBox.class);
                if (box != null) {
                    drawCollisionBox(g, pos, box, cameraX, cameraY);
                }
                
                drawDebugPath(g, entity, cameraX, cameraY);
                
                if (entity.getType() == EntityType.MONSTER) {
                    drawDebugAI(g, entity, cameraX, cameraY);
                }
                
                Renderable renderable = entity.getComponent(Renderable.class);
                if (renderable != null) {
                    g.setColor(Color.CYAN);
                    g.drawString("Y:" + (int)pos.y, screenX + 10, screenY);
                }
            }
        }
    }
 // ========================================
    // INDIVIDUAL DRAWING METHODS (NO FONT SETTING)
    // ========================================
    
    private void drawMonsterLevelBadgeOnly(Graphics2D g, int spriteX, int spriteY, MonsterLevel monsterLevel) {
        // Font already set to LEVEL_BADGE_FONT
        String levelText = "Lv" + monsterLevel.level;
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(levelText);
        
        int badgeX = spriteX + 20;
        int badgeY = spriteY - 30;
        
        Color tierColor = getTierColor(monsterLevel.tier);
        
        g.setColor(new Color(0, 0, 0, 180));
        g.fillOval(badgeX - 10, badgeY - 6, 20, 12);
        
        g.setColor(tierColor);
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(badgeX - 10, badgeY - 6, 20, 12);
        
        g.setColor(Color.WHITE);
        g.drawString(levelText, badgeX - textWidth/2, badgeY + 3);
    }
    
    private void drawNameTagOnly(Graphics2D g, int spriteX, int spriteY, NameTag tag, Entity entity) {
        // Font already set to NAME_FONT
        String displayName = tag.displayName;
        Color nameColor = Color.WHITE;
        
        if (entity.getType() == EntityType.MONSTER) {
            MonsterLevel monsterLevel = entity.getComponent(MonsterLevel.class);
            if (monsterLevel != null) {
                String tierPrefix = getTierPrefix(monsterLevel.tier);
                if (tierPrefix != null) {
                    displayName = tierPrefix + displayName;
                }
                nameColor = getTierColor(monsterLevel.tier);
            }
        }
        
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(displayName);
        
        int textX = spriteX - textWidth / 2;
        int textY = (int)(spriteY + tag.offsetY);
        
        g.setColor(Color.BLACK);
        g.drawString(displayName, textX + 1, textY + 1);
        
        g.setColor(nameColor);
        g.drawString(displayName, textX, textY);
    }
    
    private void drawLevelBadgeOnly(Graphics2D g, int spriteX, int spriteY, int level) {
        // Font already set to LEVEL_BADGE_FONT
        String levelText = "Lv" + level;
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(levelText);
        
        int badgeX = spriteX - 25;
        int badgeY = spriteY - 35;
        
        g.setColor(new Color(0, 0, 0, 180));
        g.fillOval(badgeX - 12, badgeY - 8, 24, 16);
        
        g.setColor(new Color(255, 215, 0));
        g.setStroke(new BasicStroke(2));
        g.drawOval(badgeX - 12, badgeY - 8, 24, 16);
        
        g.setColor(Color.WHITE);
        g.drawString(levelText, badgeX - textWidth/2, badgeY + 4);
    }
    
    private void drawLevelUpEffectOnly(Graphics2D g, int spriteX, int spriteY, LevelUpEffect effect) {
        // Font already set to LEVELUP_FONT
        if (!effect.active) return;
        
        float alpha = effect.getAlpha();
        int alphaVal = (int)(alpha * 200);
        
        int radius = (int)(30 + (1 - alpha) * 20);
        g.setColor(new Color(255, 255, 0, alphaVal / 2));
        g.fillOval(spriteX - radius, spriteY - radius, radius * 2, radius * 2);
        
        g.setColor(new Color(255, 215, 0, alphaVal));
        g.setStroke(new BasicStroke(3));
        g.drawOval(spriteX - radius, spriteY - radius, radius * 2, radius * 2);
        
        String text = "LEVEL " + effect.newLevel;
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        
        int textY = spriteY - 50 - (int)((1 - alpha) * 20);
        
        g.setColor(new Color(0, 0, 0, alphaVal));
        g.drawString(text, spriteX - textWidth/2 + 2, textY + 2);
        
        g.setColor(new Color(255, 215, 0, alphaVal));
        g.drawString(text, spriteX - textWidth/2, textY);
    }
    
    private void drawAlertOnly(Graphics2D g, int spriteX, int spriteY, Alert alert) {
        // Font already set to ALERT_FONT
        if (!alert.active) return;
        
        Stroke originalStroke = g.getStroke();
        
        int alertX = spriteX;
        int alertY = (int)(spriteY + alert.offsetY + alert.bounceOffset);
        
        String exclamation = "!";
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(exclamation);
        int textHeight = fm.getHeight();
        
        int textX = alertX - textWidth / 2;
        int textY = alertY + textHeight / 4;
        
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(exclamation, textX + 1, textY + 1);
        
        g.setColor(new Color(255, 0, 0, 255));
        g.drawString(exclamation, textX, textY);
        
        g.setStroke(originalStroke);
    }
     
    private String getTierPrefix(MobTier tier) {
        switch (tier) {
            case ELITE: return "[Elite]";
            case MINIBOSS: return "[Boss]";
            default: return null;
        }
    }
    
    private Color getTierColor(MobTier tier) {
        switch (tier) {
            case TRASH: return new Color(150, 150, 150);
            case NORMAL: return Color.WHITE;
            case ELITE: return new Color(100, 150, 255);
            case MINIBOSS: return new Color(200, 100, 200);
            default: return Color.WHITE;
        }
    }
    
    private void drawHealthBar(Graphics2D g, int spriteX, int spriteY, Stats hp, HealthBar bar, RenderObject ro) {
        Stroke originalStroke = g.getStroke();
        
        int barX = spriteX - bar.width / 2;
        int barY = spriteY + bar.offsetY;
        
        float pct = (float) hp.hp / hp.maxHp;
        pct = Math.max(0f, Math.min(1f, pct));
        
        if (hp.hp > 0 && pct < 0.10f) {
            pct = 0.10f;
        }
        
        int filledWidth = (int)(bar.width * pct);
        
        EntityType et = ro.entity.getType();
        Color hpColor = Color.GREEN; //default
        if(et != null)  {//is this needed?
            if(et == EntityType.PLAYER) {
                hpColor = pct > 0.50f ? HealthBar.HP_GREEN :
                           pct > 0.25f ? HealthBar.HP_ORANGE :
                           HealthBar.HP_RED;
            } else {
                hpColor = HealthBar.HP_RED;

            } 
        } 
        
        g.setColor(HealthBar.BG_COLOR);
        g.fillRect(barX, barY, bar.width, bar.height);
        
        g.setColor(hpColor);
        g.fillRect(barX, barY, filledWidth, bar.height);
        
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(barX, barY, bar.width, bar.height);
        
        g.setStroke(originalStroke);
    }
    
    private void drawStaminaBar(Graphics2D g, int spriteX, int spriteY, Stats stats, StaminaBar bar) {
        Stroke originalStroke = g.getStroke();
        
        int barX = spriteX - bar.width / 2;
        int barY = spriteY + bar.offsetY;
        
        float pct = stats.stamina / stats.maxStamina;
        pct = Math.max(0f, Math.min(1f, pct));
        
        int filledWidth = (int)(bar.width * pct);
        
        g.setColor(StaminaBar.BG_COLOR);
        g.fillRect(barX, barY, bar.width, bar.height);
        
        g.setColor(StaminaBar.STAMINA_COLOR);
        g.fillRect(barX, barY, filledWidth, bar.height);
        
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(barX, barY, bar.width, bar.height);
        
        g.setStroke(originalStroke);
    }
    
    private void drawManaBar(Graphics2D g, int spriteX, int spriteY, Stats stats, ManaBar bar) {
        Stroke originalStroke = g.getStroke();
        
        int barX = spriteX - bar.width / 2;
        int barY = spriteY + bar.offsetY;
        
        float pct = (float) stats.mana / stats.maxMana;
        pct = Math.max(0f, Math.min(1f, pct));
        
        int filledWidth = (int)(bar.width * pct);
        
        g.setColor(ManaBar.BG_COLOR);
        g.fillRect(barX, barY, bar.width, bar.height);
        
        g.setColor(ManaBar.MANA_COLOR);
        g.fillRect(barX, barY, filledWidth, bar.height);
        
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(barX, barY, bar.width, bar.height);
        
        g.setStroke(originalStroke);
    }
    
    private void drawXPBar(Graphics2D g, int spriteX, int spriteY, Experience exp) {
        Stroke originalStroke = g.getStroke();
        
        int barWidth = 40;
        int barHeight = 3;
        int offsetY = 52;
        
        int barX = spriteX - barWidth / 2;
        int barY = spriteY + offsetY;
        
        float pct = exp.getXPProgress();
        int filledWidth = (int)(barWidth * pct);
        
        g.setColor(new Color(40, 40, 40));
        g.fillRect(barX, barY, barWidth, barHeight);
        
        g.setColor(new Color(255, 215, 0));
        g.fillRect(barX, barY, filledWidth, barHeight);
        
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(barX, barY, barWidth, barHeight);
        
        g.setStroke(originalStroke);
    }
    
    // ★★★ OPTIMIZED: Batched damage text rendering
    private void drawDamageTexts(Graphics2D g, float cameraX, float cameraY) {
        Font originalFont = g.getFont();
        
        // Draw all normal damage texts
        g.setFont(DAMAGE_FONT);
        for (DamageText dt : gameState.getDamageTexts()) {
            if (dt.type != DamageText.Type.CRITICAL && 
                dt.type != DamageText.Type.PLAYER_CRITICAL_DAMAGE) {
                drawSingleDamageText(g, dt, cameraX, cameraY);
            }
        }
        
        // Draw all critical damage texts
        g.setFont(DAMAGE_CRIT_FONT);
        for (DamageText dt : gameState.getDamageTexts()) {
            if (dt.type == DamageText.Type.CRITICAL || 
                dt.type == DamageText.Type.PLAYER_CRITICAL_DAMAGE) {
                drawSingleDamageText(g, dt, cameraX, cameraY);
            }
        }
        
        g.setFont(originalFont);
    }
    
    private void drawSingleDamageText(Graphics2D g, DamageText dt, float cameraX, float cameraY) {
        int screenX = (int)(dt.worldX - cameraX);
        int screenY = (int)(dt.worldY - cameraY);
        
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(dt.text);
        
        int textX = screenX - textWidth / 2;
        int textY = screenY;
        
        int alpha = (int)(dt.getAlpha() * 255);
        
        g.setColor(new Color(0, 0, 0, alpha));
        g.drawString(dt.text, textX + 2, textY + 2);
        
        Color textColor = new Color(
            dt.color.getRed(),
            dt.color.getGreen(),
            dt.color.getBlue(),
            alpha
        );
        g.setColor(textColor);
        g.drawString(dt.text, textX, textY);
    }
     
    private void drawCollisionBox(Graphics2D g, Position pos, CollisionBox box, float cameraX, float cameraY) {
        int boxX = (int)Math.round(box.getLeft(pos.x) - cameraX);
        int boxY = (int)Math.round(box.getTop(pos.y) - cameraY);
        int boxW = (int)box.width;
        int boxH = (int)box.height;
        
        g.setColor(new Color(255, 0, 0, 150));
        g.setStroke(new BasicStroke(2));
        g.drawRect(boxX, boxY, boxW, boxH);
        
        int centerX = (int)Math.round(pos.x - cameraX);
        int centerY = (int)Math.round(pos.y - cameraY);
        g.setColor(Color.YELLOW);
        g.fillOval(centerX - 3, centerY - 3, 6, 6);
    }
    
    private void drawTileGrid(Graphics2D g, TileMap map, float cameraX, float cameraY) {
        if (map == null) return;
        
        int mapWidth = map.getWidth();
        int mapHeight = map.getHeight();
        
        if (mapWidth <= 0 || mapHeight <= 0) return;
        
        // Calculate visible tile range (inclusive)
        int startCol = Math.max(0, (int)(cameraX / TileMap.TILE_SIZE));
        int startRow = Math.max(0, (int)(cameraY / TileMap.TILE_SIZE));
        int endCol = Math.min(mapWidth - 1, (int)((cameraX + Engine.WIDTH - 1) / TileMap.TILE_SIZE));
        int endRow = Math.min(mapHeight - 1, (int)((cameraY + Engine.HEIGHT - 1) / TileMap.TILE_SIZE));
        
        // Sanity check
        if (startCol > endCol || startRow > endRow) return;
        
        // Draw grid lines
        g.setColor(new Color(255, 255, 255, 100));
        g.setStroke(new BasicStroke(1));
        
        // Vertical lines (from startCol to endCol+1, clamped to mapWidth)
        int maxVerticalLine = Math.min(endCol + 1, mapWidth);
        for (int col = startCol; col <= maxVerticalLine; col++) {
            int x = (int)(col * TileMap.TILE_SIZE - cameraX);
            if (x >= 0 && x <= Engine.WIDTH) {
                g.drawLine(x, 0, x, Engine.HEIGHT);
            }
        }
        
        // Horizontal lines (from startRow to endRow+1, clamped to mapHeight)
        int maxHorizontalLine = Math.min(endRow + 1, mapHeight);
        for (int row = startRow; row <= maxHorizontalLine; row++) {
            int y = (int)(row * TileMap.TILE_SIZE - cameraY);
            if (y >= 0 && y <= Engine.HEIGHT) {
                g.drawLine(0, y, Engine.WIDTH, y);
            }
        }
        
        // Draw solid tiles (red overlay)
        g.setColor(new Color(255, 0, 0, 80));
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                // Triple-check bounds (paranoid but safe)
                if (col >= 0 && col < mapWidth && row >= 0 && row < mapHeight) {
                    if (map.isSolid(col, row)) {
                        int x = (int)(col * TileMap.TILE_SIZE - cameraX);
                        int y = (int)(row * TileMap.TILE_SIZE - cameraY);
                        g.fillRect(x, y, TileMap.TILE_SIZE, TileMap.TILE_SIZE);
                    }
                }
            }
        }
    }
    
    private void drawDebugPath(Graphics2D g, Entity entity, float cameraX, float cameraY) {
        Path path = entity.getComponent(Path.class);
        
        if (path != null && path.waypoints != null) {
            g.setColor(new Color(0, 255, 255, 200));
            g.setStroke(new BasicStroke(3));
            
            for (int i = 0; i < path.waypoints.size() - 1; i++) {
                int[] current = path.waypoints.get(i);
                int[] next = path.waypoints.get(i + 1);
                
                int x1 = (int)((current[0] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f) - cameraX);
                int y1 = (int)((current[1] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f) - cameraY);
                int x2 = (int)((next[0] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f) - cameraX);
                int y2 = (int)((next[1] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f) - cameraY);
                
                g.drawLine(x1, y1, x2, y2);
            }
            
            g.setColor(Color.CYAN);
            for (int i = 0; i < path.waypoints.size(); i++) {
                int[] waypoint = path.waypoints.get(i);
                int x = (int)((waypoint[0] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f) - cameraX);
                int y = (int)((waypoint[1] * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f) - cameraY);
                
                if (i == path.currentWaypoint) {
                    g.setColor(Color.YELLOW);
                    g.fillOval(x - 5, y - 5, 10, 10);
                    g.setColor(Color.CYAN);
                } else {
                    g.fillOval(x - 3, y - 3, 6, 6);
                }
            }
        }
    }
    
    private void drawDebugAI(Graphics2D g, Entity entity, float cameraX, float cameraY) {
        AI ai = entity.getComponent(AI.class);
        Position pos = entity.getComponent(Position.class);
        Movement movement = entity.getComponent(Movement.class);
        Stats stats = entity.getComponent(Stats.class);
        MonsterLevel monsterLevel = entity.getComponent(MonsterLevel.class);
        
        if (ai == null || pos == null) return;
        
        Stroke originalStroke = g.getStroke();
        Font originalFont = g.getFont();
        
        int homeScreenX = (int)(ai.homeX - cameraX);
        int homeScreenY = (int)(ai.homeY - cameraY);
        g.setColor(new Color(0, 255, 0, 100));
        g.fillOval(homeScreenX - 5, homeScreenY - 5, 10, 10);
        
        int roamRadius = (int)ai.roamRadius;
        g.setColor(new Color(255, 255, 0, 80));
        g.setStroke(new BasicStroke(2));
        g.drawOval(homeScreenX - roamRadius, homeScreenY - roamRadius, roamRadius * 2, roamRadius * 2);
        
        int detectionRadius = (int)(ai.detectionRange * TileMap.TILE_SIZE);
        int screenX = (int)(pos.x - cameraX);
        int screenY = (int)(pos.y - cameraY);
        
        Color detectionColor = ai.currentState == AI.State.CHASING 
            ? new Color(255, 0, 0, 100)
            : new Color(100, 100, 255, 80);
        g.setColor(detectionColor);
        g.drawOval(screenX - detectionRadius, screenY - detectionRadius, detectionRadius * 2, detectionRadius * 2);
        
        if (movement != null && movement.isHasted) {
            g.setColor(new Color(255, 255, 0, 200));
            g.setStroke(new BasicStroke(3));
            g.drawOval(screenX - 20, screenY - 20, 40, 40);
            g.drawString("HASTE", screenX - 20, screenY - 50);
        }
        
        Font infoFont = new Font("Arial", Font.PLAIN, 10);
        g.setFont(infoFont);
        g.setColor(Color.WHITE);
        
        int textY = screenY - 60;
        
        String stateText = ai.currentState.toString();
        if (movement != null && movement.isHasted) {
            stateText += " (HASTE)";
        }
        g.drawString(stateText, screenX - 30, textY);
        textY += 12;
        
        if (monsterLevel != null) {
            String levelInfo = "Lv" + monsterLevel.level + " " + monsterLevel.tier;
            g.setColor(new Color(255, 215, 0));
            g.drawString(levelInfo, screenX - 30, textY);
            textY += 12;
            g.setColor(Color.WHITE);
        }
        
        if (stats != null) {
            g.drawString(String.format("HP:%d/%d", stats.hp, stats.maxHp), screenX - 30, textY);
            textY += 12;
            g.drawString(String.format("ATK:%d DEF:%d", stats.attack, stats.defense), screenX - 30, textY);
            textY += 12;
            g.drawString(String.format("ACC:%d EVA:%d", stats.accuracy, stats.evasion), screenX - 30, textY);
        }
        
        g.setFont(originalFont);
        g.setStroke(originalStroke);
    }
    
    // ⭐ OPTIMIZED: Use cached font
    private void drawDebugSpawnPoints(Graphics2D g, float cameraX, float cameraY) {
        Font originalFont = g.getFont();
        g.setFont(TIMER_FONT);  // ⭐ Cached
        
        for (SpawnPoint sp : gameState.getSpawnPoints()) {
            int screenX = (int)(sp.x - cameraX);
            int screenY = (int)(sp.y - cameraY);
            
            if (sp.isOccupied) {
                g.setColor(new Color(0, 255, 0, 150));
            } else {
                g.setColor(new Color(255, 0, 0, 150));
            }
            g.fillOval(screenX - 8, screenY - 8, 16, 16);
            
            g.setColor(Color.WHITE);
            g.drawOval(screenX - 8, screenY - 8, 16, 16);
            
            if (!sp.isOccupied) {
                float timeLeft = sp.respawnDelay - sp.respawnTimer;
                String timerText = String.format("%.1fs", timeLeft);
                
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(timerText);
                
                g.setColor(Color.BLACK);
                g.drawString(timerText, screenX - textWidth/2 + 1, screenY + 20 + 1);
                
                g.setColor(Color.YELLOW);
                g.drawString(timerText, screenX - textWidth/2, screenY + 20);
            }
            
            String typeText = sp.monsterType;
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(typeText);
            
            g.setColor(Color.BLACK);
            g.drawString(typeText, screenX - textWidth/2 + 1, screenY - 15 + 1);
            
            g.setColor(Color.WHITE);
            g.drawString(typeText, screenX - textWidth/2, screenY - 15);
        }
        
        g.setFont(originalFont);
    }
    
    
}