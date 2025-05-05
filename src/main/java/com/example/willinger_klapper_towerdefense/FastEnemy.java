// FastEnemy.java
package com.example.willinger_klapper_towerdefense;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import java.util.List;

public class FastEnemy extends Enemy {
    public FastEnemy(List<Point2D> path) {
        super(path, 2, 1.5);
        this.color = Color.BLUE;
    }
}
