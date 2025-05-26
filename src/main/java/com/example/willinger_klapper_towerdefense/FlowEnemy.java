package com.example.willinger_klapper_towerdefense;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

/**
 * Ein "fließender" (stealth) Gegner: nur von Bogenschütze, Magie- und Sniperturm sichtbar.
 * Spawnt ab Welle 3.
 */
public class FlowEnemy extends Enemy {
    private static final int    DEFAULT_HEALTH = 5;
    private static final double DEFAULT_SPEED  = 0.6;

    public FlowEnemy(java.util.List<Point2D> path) {
        super(path, DEFAULT_HEALTH, DEFAULT_SPEED);
        this.color = Color.PURPLE;
    }

    @Override
    public void draw(javafx.scene.canvas.GraphicsContext gc) {
        // Transparenter, fließender Look
        gc.setGlobalAlpha(0.6);
        super.draw(gc);
        gc.setGlobalAlpha(1.0);
    }
}
