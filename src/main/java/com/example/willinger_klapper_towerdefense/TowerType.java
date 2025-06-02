// TowerType.java
package com.example.willinger_klapper_towerdefense;

import javafx.scene.paint.Color;

public enum TowerType {
    RED   ("Bogenschützenturm", Color.RED,    100, 200, 1,   60),
    BLUE  ("Kanonenturm",      Color.BLUE,   150, 180, 3,  180),
    GREEN ("Eisturm",          Color.CYAN,   250,  80, 1,  180),
    YELLOW("Magischer Turm",   Color.GOLD,   300, 190, 3,  180),
    POISON("Giftturm", Color.DARKGREEN, 350, 180, 1, 200),
    SNIPER("Sniperturm", Color.DARKSLATEBLUE, 300, 9999, 10, 300),
    FIRE("Feuerturm", Color.ORANGERED, 250, 200, 1, 90);

    private final String displayName;
    private final Color  color;
    private final int    cost;
    private final double range;
    private final int    damage;
    private final int    fireRateFrames;

    TowerType(String displayName, Color color, int cost,
              double range, int damage, int fireRateFrames) {
        this.displayName    = displayName;
        this.color          = color;
        this.cost           = cost;
        this.range          = range;
        this.damage         = damage;
        this.fireRateFrames = fireRateFrames;
    }

    public String getDisplayName()    { return displayName; }
    public Color  getColor()          { return color; }
    public int    getCost()           { return cost; }
    public double getRange()          { return range; }
    public int    getDamage()         { return damage; }
    public int    getFireRateFrames() { return fireRateFrames; }
}
