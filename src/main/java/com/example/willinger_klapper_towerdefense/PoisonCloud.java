// Schritt 1: Erweiterung in TowerType.java (nur beispielhaft)
// TowerType POISON = new TowerType("Giftturm", Color.DARKGREEN, 80, 0.5, 0, 25);

// Schritt 2: Neue Klasse für die Giftwolke
package com.example.willinger_klapper_towerdefense;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import java.util.Map;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PoisonCloud {
    private final Point2D center;
    private final double radius;
    private int durationTicks; // z. B. 100 Frames Lebensdauer
    private final int damagePerTick;
    private final Color color = Color.DARKGREEN;
    private final Map<Enemy, Integer> cooldowns = new HashMap<>();

    public PoisonCloud(double x, double y, double radius, int durationTicks, int damagePerTick) {
        this.center = new Point2D(x, y);
        this.radius = radius;
        this.durationTicks = durationTicks;
        this.damagePerTick = damagePerTick;
    }

    public boolean isAlive() {
        return durationTicks > 0;
    }

    public void update(List<Enemy> enemies) {
        if (durationTicks-- <= 0) return;

        // Cooldowns aktualisieren
        cooldowns.replaceAll((e, c) -> Math.max(0, c - 1));

        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;

            if (center.distance(e.getX(), e.getY()) <= radius) {
                int cd = cooldowns.getOrDefault(e, 0);
                if (cd == 0) {
                    e.takeDamage(damagePerTick);
                    cooldowns.put(e, 60); // 60 Frames Cooldown → alle 1 Sekunde
                }
            }
        }
    }

    public void render(GraphicsContext gc) {
        if (!isAlive()) return;
        gc.setFill(Color.rgb(0, 100, 0, 0.3)); // giftgrün, halbtransparent
        gc.fillOval(center.getX() - radius, center.getY() - radius, radius * 2, radius * 2);
    }
}
