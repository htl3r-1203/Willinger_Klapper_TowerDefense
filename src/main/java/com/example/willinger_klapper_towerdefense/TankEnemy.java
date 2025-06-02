// TankEnemy.java
package com.example.willinger_klapper_towerdefense;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import java.util.List;

public class TankEnemy extends Enemy {
    public TankEnemy(List<Point2D> path) {
        super(path, 25, 0.2);
        this.color = Color.DARKGREEN;
    }
}
