package com.example.willinger_klapper_towerdefense;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Projectile {
    private double x, y;
    private final double dx, dy;
    private final double speed;
    private final int    damage;
    private final Color  color;

    /**
     * @param start   Ausgangspunkt des Projektils
     * @param target  Zielpunkt (wird zur Berechnung der Richtung genutzt)
     * @param speed   Geschwindigkeit in Pixeln pro Frame
     * @param damage  Schaden, den dieses Projektil anrichtet
     * @param color   Farbe des Projektils
     */
    public Projectile(Point2D start, Point2D target, double speed, int damage, Color color) {
        this.x      = start.getX();
        this.y      = start.getY();
        double dist = start.distance(target);
        this.dx     = (target.getX() - x) / dist;
        this.dy     = (target.getY() - y) / dist;
        this.speed  = speed;
        this.damage = damage;
        this.color  = color;
    }

    /** Bewege das Projektil entlang seiner Richtung um speed Einheiten */
    public void update() {
        x += dx * speed;
        y += dy * speed;
    }

    /** Zeichnet das Projektil als Linie mit zwei „Federkielen“ */
    public void draw(GraphicsContext gc) {
        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.strokeLine(x - dx * 5, y - dy * 5, x, y);
        double angle = Math.atan2(dy, dx);
        double len   = 8;
        double a1    = angle + Math.toRadians(150);
        double a2    = angle - Math.toRadians(150);
        gc.strokeLine(x, y, x + Math.cos(a1) * len, y + Math.sin(a1) * len);
        gc.strokeLine(x, y, x + Math.cos(a2) * len, y + Math.sin(a2) * len);
    }

    /** Richtungskomponente X (normalisiert) */
    public double getDx() {
        return dx;
    }

    /** Richtungskomponente Y (normalisiert) */
    public double getDy() {
        return dy;
    }

    /** Aktuelle X-Position */
    public double getX() {
        return x;
    }

    /** Aktuelle Y-Position */
    public double getY() {
        return y;
    }

    /** Schaden, den dieses Projektil verursacht */
    public int getDamage() {
        return damage;
    }
}
