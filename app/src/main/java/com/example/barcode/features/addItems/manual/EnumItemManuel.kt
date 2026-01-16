package com.example.barcode.features.addItems.manual


enum class ManualType(val label: String) {
    MEAT("Viande"),
    VEGETABLES("Légumes"),
    EGGS("Œufs"),
    DAIRY("Produits laitiers"),
    FISH("Poisson"),
    LEFTOVERS("Restes / Tupperware"),
    OTHER("Autre")
}

enum class ManualSubType(
    val parentType: ManualType,
    val label: String
) {

    // --------------------
    // VEGETABLES
    // --------------------
    VEG_CARROT(ManualType.VEGETABLES, "Carottes"),
    VEG_GREEN_BEANS(ManualType.VEGETABLES, "Haricots verts"),
    VEG_SALAD(ManualType.VEGETABLES, "Salade"),
    VEG_TOMATO(ManualType.VEGETABLES, "Tomates"),
    VEG_CUCUMBER(ManualType.VEGETABLES, "Concombre"),
    VEG_ZUCCHINI(ManualType.VEGETABLES, "Courgette"),
    VEG_BROCCOLI(ManualType.VEGETABLES, "Brocoli"),
    VEG_CAULIFLOWER(ManualType.VEGETABLES, "Chou-fleur"),
    VEG_PEPPER(ManualType.VEGETABLES, "Poivron"),
    VEG_MUSHROOM(ManualType.VEGETABLES, "Champignons"),
    VEG_ONION(ManualType.VEGETABLES, "Oignons"),
    VEG_POTATO(ManualType.VEGETABLES, "Pommes de terre"),

    // --------------------
    // MEAT
    // --------------------
    MEAT_CHICKEN(ManualType.MEAT, "Poulet"),
    MEAT_BEEF(ManualType.MEAT, "Bœuf"),
    MEAT_PORK(ManualType.MEAT, "Porc"),
    MEAT_TURKEY(ManualType.MEAT, "Dinde"),
    MEAT_LAMB(ManualType.MEAT, "Agneau"),
    MEAT_GROUND_MEAT(ManualType.MEAT, "Viande hachée"),
    MEAT_DELI_MEAT(ManualType.MEAT, "Charcuterie"),

    // --------------------
    // DAIRY
    // --------------------
    DAIRY_MILK(ManualType.DAIRY, "Lait"),
    DAIRY_YOGURT(ManualType.DAIRY, "Yaourt"),
    DAIRY_CREAM(ManualType.DAIRY, "Crème"),
    DAIRY_BUTTER(ManualType.DAIRY, "Beurre"),
    DAIRY_SOFT_CHEESE(ManualType.DAIRY, "Fromage (pâte molle)"),
    DAIRY_HARD_CHEESE(ManualType.DAIRY, "Fromage (pâte dure)"),
    DAIRY_FRESH_CHEESE(ManualType.DAIRY, "Fromage frais / Faisselle"),

    // --------------------
    // Optionnel : FISH / EGGS si tu veux aussi du subtype
    // --------------------
    FISH_WHITE(ManualType.FISH, "Poisson blanc"),
    FISH_SALMON(ManualType.FISH, "Saumon"),
    FISH_SHELLFISH(ManualType.FISH, "Fruits de mer"),

    EGGS_CHICKEN(ManualType.EGGS, "Œufs de poule"),

    // --------------------
    // Fallback
    // --------------------
    GENERIC(ManualType.OTHER, "Générique");
}