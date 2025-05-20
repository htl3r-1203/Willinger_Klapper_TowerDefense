package com.example.willinger_klapper_towerdefense;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class FireProjectile extends Projectile {
    public FireProjectile(Point2D start, Point2D target, double speed, int damage, Color color) {
        super(start, target, speed, damage, color);
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.ORANGERED);
        gc.fillOval(getX() - 6, getY() - 6, 12, 12);
    }
}
