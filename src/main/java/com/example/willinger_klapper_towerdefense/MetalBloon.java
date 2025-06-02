package com.example.willinger_klapper_towerdefense;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;

/**
 * Ein Gegner mit metallischer Hülle (10 HP), der erst durch Feuer- oder Kanonenschüsse
 * aufgebrochen werden kann. Sobald die Hülle zerstört ist, verhält er sich wie ein normaler Enemy.
 */
public class MetalBloon extends Enemy {
    private int shellHP = 5;
    private boolean shellIntact = true;

    public MetalBloon(List<Point2D> path) {
        super(path);
    }

    /**
     * Entfernt HP von der Metallhülle.
     */
    public void hitShell(int dmg) {
        if (!shellIntact) return;
        shellHP -= dmg;
        if (shellHP <= 0) {
            shellIntact = false;
            shellHP = 0;
        }
    }

    public boolean isShellIntact() {
        return shellIntact;
    }

    @Override
    public void takeDamage(int dmg) {
        // Wird nur aufgerufen, wenn die Hülle schon weg ist
        super.takeDamage(dmg);
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Erstmal den normalen Enemy zeichnen (inkl. Lebensbalken etc.)
        super.draw(gc);

        // Und, falls die Hülle noch intakt ist, eine silberne Umrandung
        if (shellIntact) {
            gc.setStroke(Color.SILVER);
            gc.setLineWidth(3);
            gc.strokeOval(getX() - 12, getY() - 12, 24, 24);
        }
    }
}
