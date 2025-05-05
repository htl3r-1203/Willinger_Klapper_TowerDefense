package com.example.willinger_klapper_towerdefense;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

public class Main extends Application {

    private static final double PATH_WIDTH       = 40;
    private static final double SPOT_MARGIN      = 10;
    private static final double SPOT_SPACING     = 80;
    private static final double PROJECTILE_SPEED = 2;
    private static final long   SPAWN_INTERVAL   = 1_000_000_000L;

    private final List<Point2D> pathNorm     = List.of(
            new Point2D(0.00, 0.30), new Point2D(0.35, 0.30),
            new Point2D(0.35, 0.60), new Point2D(0.65, 0.60),
            new Point2D(0.65, 0.40), new Point2D(0.30, 0.40),
            new Point2D(0.30, 0.80), new Point2D(0.80, 0.80),
            new Point2D(0.80, 0.20), new Point2D(1.02, 0.20)
    );
    private final List<Point2D> pathPoints   = new ArrayList<>();
    private final List<Point2D> buildSpots   = new ArrayList<>();
    private final List<Enemy>    enemies      = new ArrayList<>();
    private final List<Tower>    towers       = new ArrayList<>();
    private final List<Projectile> projectiles= new ArrayList<>();
    private final List<Explosion>  explosions = new ArrayList<>();
    private final List<IceBlast>   iceBlasts  = new ArrayList<>();
    private final List<Spark>      sparks     = new ArrayList<>();
    private final Map<Tower, TowerType> towerTypeMap  = new HashMap<>();
    private final Map<TowerType, Button> typeButtonMap = new HashMap<>();

    private TowerType currentTowerType = TowerType.RED;
    private long      lastSpawnTime    = 0;

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas();
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Label selectedLabel = new Label("Ausgewählt: " + currentTowerType.getDisplayName());
        selectedLabel.setAlignment(Pos.CENTER_RIGHT);
        selectedLabel.setMaxWidth(Double.MAX_VALUE);

        HBox buttonBar = new HBox(8);
        for (TowerType type : TowerType.values()) {
            Button btn = new Button(type.getDisplayName());
            btn.setStyle("-fx-background-color:" + toHex(type.getColor()) + ";");
            btn.setOnAction(e -> {
                currentTowerType = type;
                selectedLabel.setText("Ausgewählt: " + type.getDisplayName());
                updateSelectionUI();
            });
            btn.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getClickCount() == 2) showTypeInfo(type);
            });
            buttonBar.getChildren().add(btn);
            typeButtonMap.put(type, btn);
        }
        updateSelectionUI();

        HBox topBar = new HBox(8, buttonBar, selectedLabel);
        HBox.setHgrow(selectedLabel, Priority.ALWAYS);
        topBar.setAlignment(Pos.CENTER_LEFT);

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleCanvasClick);
        canvas.widthProperty().addListener((o, oldW, newW) ->
                recalcPathAndSpots(canvas, newW.doubleValue(), canvas.getHeight()));
        canvas.heightProperty().addListener((o, oldH, newH) ->
                recalcPathAndSpots(canvas, canvas.getWidth(), newH.doubleValue()));

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastSpawnTime == 0 || now - lastSpawnTime >= SPAWN_INTERVAL) {
                    enemies.add(Enemy.spawn(pathPoints));
                    lastSpawnTime = now;
                }
                updateGame();
                drawGame(gc);
            }
        }.start();

        BorderPane root = new BorderPane(canvas);
        root.setTop(topBar);
        Scene scene = new Scene(root, 900, 650);
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty().subtract(topBar.heightProperty()));
        stage.setScene(scene);
        stage.setTitle("Tower Defense – Vorausschauende Kanone");
        stage.show();

        recalcPathAndSpots(canvas, scene.getWidth(), scene.getHeight() - topBar.getHeight());
    }

    private void updateSelectionUI() {
        typeButtonMap.forEach((type, btn) -> {
            String base = "-fx-background-color:" + toHex(type.getColor()) + ";";
            btn.setStyle(type == currentTowerType
                    ? base + " -fx-border-color: yellow; -fx-border-width: 3;"
                    : base);
        });
    }

    private void handleCanvasClick(MouseEvent e) {
        double x = e.getX(), y = e.getY();
        if (e.getButton() == MouseButton.SECONDARY) {
            Iterator<Tower> it = towers.iterator();
            while (it.hasNext()) {
                Tower t = it.next();
                if (Math.hypot(x - t.getX(), y - t.getY()) < SPOT_MARGIN) {
                    it.remove();
                    towerTypeMap.remove(t);
                    return;
                }
            }
        } else {
            for (Tower t : towers) {
                if (Math.hypot(x - t.getX(), y - t.getY()) < SPOT_MARGIN) {
                    showPlacementInfo(towerTypeMap.get(t));
                    return;
                }
            }
            tryPlaceTower(e);
        }
    }

    private void showPlacementInfo(TowerType type) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Turm-Info");
        a.setHeaderText(type.getDisplayName());
        a.setContentText("Kosten: " + type.getCost());
        a.showAndWait();
    }

    private void showTypeInfo(TowerType type) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Turm-Auswahl");
        a.setHeaderText(type.getDisplayName());
        a.setContentText("Kosten: " + type.getCost());
        a.showAndWait();
    }

    private void recalcPathAndSpots(Canvas canvas, double w, double h) {
        pathPoints.clear();
        for (Point2D p : pathNorm) {
            pathPoints.add(new Point2D(p.getX() * w, p.getY() * h));
        }
        buildSpots.clear();
        double off = PATH_WIDTH / 2 + SPOT_MARGIN;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D a = pathPoints.get(i), b = pathPoints.get(i + 1);
            Point2D dir = b.subtract(a).normalize();
            Point2D norm = new Point2D(-dir.getY(), dir.getX());
            double len = a.distance(b);
            int steps = (int) Math.floor(len / SPOT_SPACING);
            for (int s = 0; s <= steps; s++) {
                Point2D base = a.add(dir.multiply(s * SPOT_SPACING));
                for (Point2D cand : List.of(
                        base.add(norm.multiply(off)),
                        base.subtract(norm.multiply(off))
                )) {
                    if (insideCanvas(cand, w, h) && isValidSpot(cand)) {
                        buildSpots.add(cand);
                    }
                }
            }
        }
    }

    private boolean isValidSpot(Point2D spot) {
        double min = PATH_WIDTH / 2 + SPOT_MARGIN;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            if (distancePointToSegment(spot, pathPoints.get(i), pathPoints.get(i + 1)) < min)
                return false;
        }
        return true;
    }

    private double distancePointToSegment(Point2D p, Point2D a, Point2D b) {
        double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
        if (dx == 0 && dy == 0) return p.distance(a);
        double t = ((p.getX() - a.getX()) * dx + (p.getY() - a.getY()) * dy)
                / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        Point2D proj = new Point2D(a.getX() + t * dx, a.getY() + t * dy);
        return p.distance(proj);
    }

    private boolean insideCanvas(Point2D p, double w, double h) {
        return p.getX() >= 0 && p.getX() <= w && p.getY() >= 0 && p.getY() <= h;
    }

    private void tryPlaceTower(MouseEvent e) {
        Point2D click = new Point2D(e.getX(), e.getY());
        for (Point2D spot : buildSpots) {
            if (spot.distance(click) < SPOT_MARGIN &&
                    towers.stream().noneMatch(t ->
                            Math.hypot(t.getX() - spot.getX(), t.getY() - spot.getY()) < 1)) {
                Tower tw = new Tower(spot.getX(), spot.getY(), currentTowerType);
                towers.add(tw);
                towerTypeMap.put(tw, currentTowerType);
                return;
            }
        }
    }

    private void updateGame() {
        // 1) Türme feuern
        for (Tower t : towers) {
            t.tickCooldown();
            TowerType type = towerTypeMap.get(t);
            if (!t.readyToFire()) continue;

            Enemy target = null;
            double bestDist = Double.MAX_VALUE;
            for (Enemy en : enemies) {
                double d = Math.hypot(t.getX() - en.getX(), t.getY() - en.getY());
                if (d <= type.getRange() && d < bestDist) {
                    bestDist = d; target = en;
                }
            }
            if (target == null) continue;

            Point2D origin = new Point2D(t.getX(), t.getY());

            if (type == TowerType.GREEN) {
                // Eisturm
                iceBlasts.add(new IceBlast(origin.getX(), origin.getY()));
                for (Enemy en : enemies) {
                    if (origin.distance(en.getX(), en.getY()) <= type.getRange()) {
                        en.takeDamage(type.getDamage());
                        en.applySlow(100, 0.3);
                    }
                }
            } else if (type == TowerType.BLUE) {
                // Kanonenturm: vorausschauend schießen
                Point2D aim = calculateIntercept(
                        origin,
                        new Point2D(target.getX(), target.getY()),
                        target.getVelocity(),
                        PROJECTILE_SPEED
                );
                if (aim == null) aim = new Point2D(target.getX(), target.getY());
                projectiles.add(new CannonProjectile(
                        origin, aim, PROJECTILE_SPEED, type.getDamage(), type.getColor()
                ));
            } else if (type == TowerType.YELLOW) {
                // Magieturm
                Point2D aim = calculateIntercept(
                        origin,
                        new Point2D(target.getX(), target.getY()),
                        target.getVelocity(),
                        PROJECTILE_SPEED
                );
                if (aim == null) aim = new Point2D(target.getX(), target.getY());
                projectiles.add(new MagicProjectile(
                        origin, aim, PROJECTILE_SPEED, type.getDamage(), type.getColor(), type.getRange()
                ));
            } else {
                // Bogenschütze
                Point2D aim = calculateIntercept(
                        origin,
                        new Point2D(target.getX(), target.getY()),
                        target.getVelocity(),
                        PROJECTILE_SPEED
                );
                if (aim == null) aim = new Point2D(target.getX(), target.getY());
                projectiles.add(new Projectile(
                        origin, aim, PROJECTILE_SPEED, type.getDamage(), type.getColor()
                ));
            }

            t.resetCooldown();
        }

        // 2) Gegner bewegen
        enemies.forEach(Enemy::update);

        // 3) Projektil-Kollisionen
        Iterator<Projectile> pit = projectiles.iterator();
        while (pit.hasNext()) {
            Projectile p = pit.next();
            p.update();

            boolean remove = false;
            for (Enemy en : enemies) {
                if (Math.hypot(p.getX() - en.getX(), p.getY() - en.getY()) < 12) {
                    if (p instanceof MagicProjectile) {
                        MagicProjectile mp = (MagicProjectile) p;
                        if (!mp.hasHit(en)) {
                            en.takeDamage(mp.getDamage());
                            sparks.add(new Spark(en.getX(), en.getY()));
                            mp.registerHit(en);
                        }
                        if (mp.isExpired()) {
                            remove = true;
                        }
                    } else {
                        en.takeDamage(p.getDamage());
                        sparks.add(new Spark(en.getX(), en.getY()));
                        if (p instanceof CannonProjectile) {
                            explosions.add(new Explosion(en.getX(), en.getY()));
                        }
                        remove = true;
                    }
                    break;
                }
            }
            if (remove) {
                pit.remove();
            }
        }


        // 4) Animationen updaten
        explosions.removeIf(ex -> !ex.update());
        iceBlasts.removeIf(ib -> !ib.update());
        sparks.removeIf(sp -> !sp.update());

        // 5) Gegner-Cleanup
        enemies.removeIf(en -> !en.isAlive() || en.hasFinished());
    }

    /**
     * Berechnet den prognostizierten Trefferpunkt auf einen sich bewegenden Gegner.
     */
    private Point2D calculateIntercept(Point2D shooter,
                                       Point2D target,
                                       Point2D vel,
                                       double projSpeed) {
        Point2D toT = target.subtract(shooter);
        double a = vel.dotProduct(vel) - projSpeed * projSpeed;
        double b = 2 * toT.dotProduct(vel);
        double c = toT.dotProduct(toT);
        double disc = b * b - 4 * a * c;
        if (disc < 0) return null;
        double sq = Math.sqrt(disc);
        double t1 = (-b + sq) / (2 * a), t2 = (-b - sq) / (2 * a);
        double t = Math.min(t1, t2);
        if (t < 0) t = Math.max(t1, t2);
        if (t < 0) return null;
        return target.add(vel.multiply(t));
    }

    private void drawGame(GraphicsContext gc) {
        double w = gc.getCanvas().getWidth(),
                h = gc.getCanvas().getHeight();

        // Hintergrund
        gc.setFill(Color.LIGHTGREEN);
        gc.fillRect(0, 0, w, h);

        // Pfad
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(PATH_WIDTH);
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D p1 = pathPoints.get(i), p2 = pathPoints.get(i + 1);
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }

        // Build-Spots
        gc.setFill(Color.DARKGRAY);
        for (Point2D spot : buildSpots) {
            gc.fillOval(spot.getX() - 6, spot.getY() - 6, 12, 12);
        }

        // ... Zeichnen aller Objekte ...
        towers.forEach(t -> t.draw(gc));
        projectiles.forEach(p -> p.draw(gc));
        explosions.forEach(ex -> ex.draw(gc));
        iceBlasts.forEach(ib -> ib.draw(gc));
        sparks.forEach(sp -> sp.draw(gc));
        enemies.forEach(e -> e.draw(gc));
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int)(c.getRed() * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue() * 255));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
