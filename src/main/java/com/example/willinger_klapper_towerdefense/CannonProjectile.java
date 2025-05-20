package com.example.willinger_klapper_towerdefense;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;


public class CannonProjectile extends Projectile {
    public CannonProjectile(Point2D start, Point2D aim, double speed, int damage, Color color) {
        super(start, aim, speed, damage, color);
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Bombe: dunkler Kreis + kurzer "Zündschnur"
        gc.setFill(Color.DARKGRAY);
        gc.fillOval(getX() - 6, getY() - 6, 12, 12);
        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(2);
        gc.strokeLine(getX(), getY() - 6, getX(), getY() - 12);
    }
}
