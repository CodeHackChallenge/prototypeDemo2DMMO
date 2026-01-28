package dev.main.item;

/**
 * Manages item creation and templates
 * UPDATED: Runes now use RuneManager instead
 */
public class ItemManager {

    // ═══════════════════════════════════════════════════════════════
    // WEAPONS (Weap Tab)
    // ═══════════════════════════════════════════════════════════════
    
    public static Item createWoodenShortSword() {
        return new Item(
            "Wooden Short Sword",
            Item.ItemType.WEAPON,
            Item.Rarity.COMMON,
            6, 1, 0, 0, 999,
            false, false, false, false, 100
        );
    }
    
    public static Item createIronSword() {
        return new Item(
            "Iron Sword",
            Item.ItemType.WEAPON,
            Item.Rarity.COMMON,
            12, 2, 0, 0, 800,
            true, false, true, true, 100
        );
    }
    
    public static Item createSteelLongsword() {
        return new Item(
            "Steel Longsword",
            Item.ItemType.WEAPON,
            Item.Rarity.UNCOMMON,
            20, 3, 0, 0, 1000,
            true, true, true, true, 150
        );
    }
    
    public static Item createMysticStaff() {
        return new Item(
            "Mystic Staff",
            Item.ItemType.WEAPON,
            Item.Rarity.RARE,
            5, 2, 25, 5, 600,
            true, true, true, true, 200
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // ARMOR (Arm Tab)
    // ═══════════════════════════════════════════════════════════════
    
    public static Item createLeatherArmor() {
        return new Item(
            "Leather Armor",
            Item.ItemType.ARMOR,
            Item.Rarity.COMMON,
            0, 8, 0, 2, 500,
            true, false, true, true, 100
        );
    }
    
    public static Item createChainmail() {
        return new Item(
            "Chainmail",
            Item.ItemType.ARMOR,
            Item.Rarity.UNCOMMON,
            0, 15, 0, 3, 800,
            true, false, true, true, 150
        );
    }
    
    public static Item createPlateArmor() {
        return new Item(
            "Plate Armor",
            Item.ItemType.ARMOR,
            Item.Rarity.RARE,
            0, 25, 0, 5, 1200,
            true, true, true, true, 200
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // ACCESSORIES (Acc Tab)
    // ═══════════════════════════════════════════════════════════════
    
    public static Item createPowerRing() {
        return new Item(
            "Ring of Power",
            Item.ItemType.ACCESSORY,
            Item.Rarity.UNCOMMON,
            5, 0, 3, 0, 999,
            false, false, true, true, 100
        );
    }
    
    public static Item createAmuletOfProtection() {
        return new Item(
            "Amulet of Protection",
            Item.ItemType.ACCESSORY,
            Item.Rarity.RARE,
            0, 10, 0, 8, 999,
            false, false, true, true, 150
        );
    }
    
    public static Item createSpeedBoots() {
        return new Item(
            "Boots of Speed",
            Item.ItemType.ACCESSORY,
            Item.Rarity.UNCOMMON,
            0, 3, 0, 0, 600,
            false, false, true, true, 100
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // MATERIALS (Rune Tab)
    // ═══════════════════════════════════════════════════════════════
    
    public static Item createCarvedWood() {
        return new Item(
            "Carved Wood",
            Item.ItemType.MATERIAL,
            Item.Rarity.COMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
     
    public static Item createClay() {
        return new Item(
            "Clay",
            Item.ItemType.MATERIAL,
            Item.Rarity.COMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createBrokenTooth() {
        return new Item(
            "Broken Tooth",
            Item.ItemType.MATERIAL,
            Item.Rarity.COMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createAnimalClaws() {
        return new Item(
            "Animal Claws",
            Item.ItemType.MATERIAL,
            Item.Rarity.COMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createFruitBanana() {
        return new Item(
            "Fruit Banana",
            Item.ItemType.MATERIAL,
            Item.Rarity.COMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createAnimalSkull() {
        return new Item(
            "Animal Skull",
            Item.ItemType.MATERIAL,
            Item.Rarity.COMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createRawFish() {
        return new Item(
            "Raw Fish",
            Item.ItemType.MATERIAL,
            Item.Rarity.COMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createAnimalBone() {
        return new Item(
            "Animal Bone",
            Item.ItemType.MATERIAL,
            Item.Rarity.COMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createCarvingStone() {
        return new Item(
            "Carving Stone",
            Item.ItemType.MATERIAL,
            Item.Rarity.UNCOMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    //lucky drops
    public static Item createLuckyPouch() {
        return new Item(
            "Lucky Pouch",
            Item.ItemType.MATERIAL,
            Item.Rarity.UNCOMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    // ★ NEW: Crafting materials for runes
    public static Item createWoodenTablet() {
        return new Item(
            "Wooden Tablet",
            Item.ItemType.MATERIAL,
            Item.Rarity.UNCOMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createEssence() {
        return new Item(
            "Essence",
            Item.ItemType.MATERIAL,
            Item.Rarity.UNCOMMON,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createVerdantShard() {
        return new Item(
            "Verdant Shard",
            Item.ItemType.MATERIAL,
            Item.Rarity.RARE,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createScrollOfPurity() {
        return new Item(
            "Scroll of Purity",
            Item.ItemType.MATERIAL,
            Item.Rarity.EPIC,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    // Elemental runes (materials for crafting)
    public static Item createFireRune() {
        return new Item(
            "Fire Rune",
            Item.ItemType.MATERIAL,
            Item.Rarity.EPIC,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createIceRune() {
        return new Item(
            "Ice Rune",
            Item.ItemType.MATERIAL,
            Item.Rarity.EPIC,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }
    
    public static Item createLightningRune() {
        return new Item(
            "Lightning Rune",
            Item.ItemType.MATERIAL,
            Item.Rarity.EPIC,
            0, 0, 0, 0, 999,
            false, false, true, true, 0
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // CONSUMABLES (Misc Tab)
    // ═══════════════════════════════════════════════════════════════
    
    public static Item createHealthPotion() {
        return new Item(
            "Health Potion",
            Item.ItemType.CONSUMABLE,
            Item.Rarity.COMMON,
            0, 0, 0, 0, 1,
            false, false, true, true, 0
        );
    }
    
    public static Item createManaPotion() {
        return new Item(
            "Mana Potion",
            Item.ItemType.CONSUMABLE,
            Item.Rarity.COMMON,
            0, 0, 0, 0, 1,
            false, false, true, true, 0
        );
    }
    
    public static Item createStaminaPotion() {
        return new Item(
            "Stamina Potion",
            Item.ItemType.CONSUMABLE,
            Item.Rarity.COMMON,
            0, 0, 0, 0, 1,
            false, false, true, true, 0
        );
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ★ NEW: RUNES (Magical Consumables)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * ★ REFACTORED: Create Rune of Return using RuneManager
     * @deprecated Use RuneManager.createRuneItem(RuneType.RETURN) instead
     */
    @Deprecated
    public static Item createRuneOfReturn() {
        return RuneManager.createRuneItem(Rune.RuneType.RETURN);
    }
    
    /**
     * ★ NEW: Create Rune of Spawn using RuneManager
     */
    public static Item createRuneOfSpawn() {
        return RuneManager.createRuneItem(Rune.RuneType.SPAWN);
    }
    
    /**
     * ★ NEW: Create any rune by type
     */
    public static Item createRuneItem(Rune.RuneType type) {
        return RuneManager.createRuneItem(type);
    }
}