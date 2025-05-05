// MagicProjectile.java
package com.example.willinger_klapper_towerdefense;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Set;

public class MagicProjectile extends Projectile {
    private final double startX, startY, maxRange;
    private boolean       expired     = false;
    private final Set<Enemy> hitEnemies = new HashSet<>();

    public MagicProjectile(Point2D start, Point2D aim,
                           double speed, int damage,
                           Color color, double range) {
        super(start, aim, speed, damage, color);
        this.startX   = start.getX();
        this.startY   = start.getY();
        this.maxRange = range;
    }

    @Override
    public void update() {
        super.update();
        double dx = getX() - startX, dy = getY() - startY;
        if (Math.hypot(dx, dy) >= maxRange) {
            expired = true;
        }
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean hasHit(Enemy e) {
        return hitEnemies.contains(e);
    }

    public void registerHit(Enemy e) {
        hitEnemies.add(e);
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setStroke(Color.AQUA);
        gc.setLineWidth(3);
        gc.strokeLine(
                getX() - getDx() * 4,
                getY() - getDy() * 4,
                getX(),
                getY()
        );
        gc.setFill(Color.AQUA);
        gc.fillOval(getX() - 4, getY() - 4, 8, 8);
    }
}
