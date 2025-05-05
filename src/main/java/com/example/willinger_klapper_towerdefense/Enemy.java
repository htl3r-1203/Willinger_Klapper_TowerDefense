// Enemy.java
package com.example.willinger_klapper_towerdefense;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;

public class Enemy {
    private final List<Point2D> path;
    private int currentSegment = 0;
    private double x, y;
    private final int maxHealth;
    private int health;
    private final double baseSpeed;
    private double speedMultiplier = 1.0;
    private int slowFrames = 0;
    protected Color color = Color.RED;

    public Enemy(List<Point2D> path, int maxHealth, double baseSpeed) {
        this.path = path;
        Point2D start = path.get(0);
        this.x = start.getX();
        this.y = start.getY();
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.baseSpeed = baseSpeed;
    }

    public Enemy(List<Point2D> path) {
        this(path, 5, 0.5);
    }

    public static Enemy spawn(List<Point2D> path) {
        double r = Math.random();
        if (r < 0.7) {
            return new Enemy(path);
        } else if (r < 0.9) {
            return new FastEnemy(path);
        } else {
            return new TankEnemy(path);
        }
    }

    public void update() {
        if (slowFrames > 0) {
            slowFrames--;
            if (slowFrames == 0) speedMultiplier = 1.0;
        }
        if (currentSegment >= path.size() - 1) return;
        Point2D next = path.get(currentSegment + 1);
        double dx = next.getX() - x, dy = next.getY() - y;
        double dist = Math.hypot(dx, dy);
        double v = baseSpeed * speedMultiplier;
        if (dist < v) {
            x = next.getX();
            y = next.getY();
            currentSegment++;
        } else {
            x += dx / dist * v;
            y += dy / dist * v;
        }
    }

    public void draw(GraphicsContext gc) {
        gc.setFill(color);
        gc.fillOval(x - 10, y - 10, 20, 20);
        double barW = 20, barH = 4;
        double ratio = (double) health / maxHealth;
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x - barW/2, y - 16, barW * ratio, barH);
        if (slowFrames > 0) {
            gc.setStroke(Color.CYAN);
            gc.setLineWidth(2);
            gc.strokeOval(x - 12, y - 12, 24, 24);
        }
    }

    public Point2D getVelocity() {
        if (currentSegment >= path.size() - 1) return new Point2D(0, 0);
        Point2D next = path.get(currentSegment + 1);
        Point2D dir = next.subtract(x, y).normalize();
        return dir.multiply(baseSpeed * speedMultiplier);
    }

    public double getProgress() {
        if (currentSegment >= path.size() - 1) return path.size();
        Point2D a = path.get(currentSegment), b = path.get(currentSegment + 1);
        double segLen = a.distance(b);
        double traveled = new Point2D(x, y).distance(a);
        return currentSegment + (segLen > 0 ? traveled / segLen : 0);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isAlive() { return health > 0; }
    public boolean hasFinished() { return currentSegment >= path.size() - 1; }

    public void takeDamage(int dmg) {
        health = Math.max(0, health - dmg);
    }

    public void applySlow(int frames, double multiplier) {
        slowFrames = Math.max(slowFrames, frames);
        speedMultiplier = multiplier;
    }
}
