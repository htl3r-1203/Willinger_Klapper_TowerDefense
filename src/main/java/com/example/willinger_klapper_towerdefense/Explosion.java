package com.example.willinger_klapper_towerdefense;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Explosion {
    private final double x, y;
    private int age = 0;
    private static final int MAX_AGE = 30;     // Frames
    private static final double MAX_RADIUS = 30;

    public Explosion(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** Gibt true zurück, solange die Explosion noch angezeigt werden soll */
    public boolean update() {
        age++;
        return age <= MAX_AGE;
    }

    public void draw(GraphicsContext gc) {
        double frac = (double) age / MAX_AGE;
        double r = frac * MAX_RADIUS;
        // Alpha fade-out
        gc.setGlobalAlpha(1.0 - frac);
        gc.setFill(Color.ORANGE);
        gc.fillOval(x - r, y - r, 2 * r, 2 * r);
        // Reset Alpha
        gc.setGlobalAlpha(1.0);
    }
}
