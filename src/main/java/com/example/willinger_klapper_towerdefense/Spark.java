// Spark.java
package com.example.willinger_klapper_towerdefense;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Spark {
    private final double x, y;
    private int age = 0;
    private static final int MAX_AGE = 15;
    private static final double MAX_RADIUS = 8;

    public Spark(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** Gibt false zurück, wenn der Spark abgelaufen ist */
    public boolean update() {
        age++;
        return age <= MAX_AGE;
    }

    public void draw(GraphicsContext gc) {
        double frac = age / (double) MAX_AGE;
        double r = frac * MAX_RADIUS;
        gc.setGlobalAlpha(1.0 - frac);
        gc.setFill(Color.AQUA);
        gc.fillOval(x - r, y - r, 2 * r, 2 * r);
        gc.setGlobalAlpha(1.0);
    }
}
