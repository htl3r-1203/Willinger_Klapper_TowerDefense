package com.example.willinger_klapper_towerdefense;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import java.util.List;

public class RegenEnemy extends Enemy {
    private static final int BASE_HEALTH     = 8;
    private static final double BASE_SPEED   = 1;
    private static final int REGEN_INTERVAL  = 60; // in Frames (≈1 Sekunde @60fps)
    private static final int REGEN_AMOUNT    = 1;  // pro Intervall

    private int regenCounter = REGEN_INTERVAL;

    public RegenEnemy(List<Point2D> path) {
        super(path, BASE_HEALTH, BASE_SPEED);
        this.color = Color.LIMEGREEN;
    }

    @Override
    public void update() {
        super.update();
        if (!isAlive()) return;
        // regenerations-Tick
        if (--regenCounter <= 0) {
            heal(REGEN_AMOUNT);
            regenCounter = REGEN_INTERVAL;
        }
    }
}