package com.example.willinger_klapper_towerdefense;

import javafx.scene.canvas.GraphicsContext;

public class Tower {
    private final double   x, y;
    private final TowerType type;
    private int            cooldownFrames = 0;

    public Tower(double x, double y, TowerType type) {
        this.x    = x;
        this.y    = y;
        this.type = type;
    }

    public double getX()       { return x; }
    public double getY()       { return y; }
    public TowerType getType() { return type; }

    /** Zieht die Abklingzeit um 1 Frame herunter. */
    public void tickCooldown() {
        if (cooldownFrames > 0) cooldownFrames--;
    }

    /** Gibt true, sobald er schießen darf. */
    public boolean readyToFire() {
        return type.getDamage() > 0 && cooldownFrames <= 0;
    }

    /** Setzt die Abklingzeit gemäß Fire-Rate zurück. */
    public void resetCooldown() {
        cooldownFrames = type.getFireRateFrames();
    }

    public void draw(GraphicsContext gc) {
        gc.setFill(type.getColor());
        gc.fillRect(x - 15, y - 15, 30, 30);
    }
}
