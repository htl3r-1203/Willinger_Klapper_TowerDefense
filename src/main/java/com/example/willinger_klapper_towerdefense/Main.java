package com.example.willinger_klapper_towerdefense;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;
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
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.util.function.Supplier;
import java.util.Collections;

import java.util.*;

public class Main extends Application {
    private boolean gameStarted = false;
    private int gold = 500; // Startgeld
    private Label goldLabel; // Label für Anzeige
    private Label messageLabel = new Label(); // Für Hinweis-Meldungen
    private boolean paused = false;
    private int baseHealth = 100;
    private Label healthLabel; // Anzeige
    private Rectangle damageOverlay = new Rectangle();
    private final List<PoisonCloud> poisonClouds = new ArrayList<>();
    private static final double NO_SHOOT_RADIUS = 30;
    private int currentWave = 0;
    private final int maxWaves = 10;

    // Basis-Anzahl in Welle 1:
    // Wellen‐ und Spawn‐Daten

    // Basis‐Größen Welle 1
    private final int baseNormal  = 20;
    private final int baseFast    = 5;
    private final int baseTank    = 2;

    // Dynamische Spawn‐Queue und Intervall
    private List<Supplier<Enemy>> spawnQueue = new ArrayList<>();
    private long waveSpawnInterval;      // in Nanosekunden

    // Zähler für die gerade laufende Welle:
    private int remainingNormal;
    private int remainingFast;
    private int remainingTank;

    private static final double PATH_WIDTH       = 40;
    private static final double SPOT_MARGIN      = 10;
    private static final double SPOT_SPACING     = 80;
    private static final double PROJECTILE_SPEED = 4;
    private static final long   SPAWN_INTERVAL   = 1_000_000_000L;
    private Label waveLabel = new Label("Runde: 0");

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
        // Canvas & GraphicsContext
        Canvas canvas = new Canvas(900, 650 - 40); // leave room for topBar
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Damage-Overlay
        damageOverlay = new Rectangle();
        damageOverlay.setFill(Color.rgb(255, 0, 0, 0.3));
        damageOverlay.setVisible(false);
        damageOverlay.setMouseTransparent(true);

        // Labels
        goldLabel    = new Label("Gold: "   + gold);
        healthLabel  = new Label("Leben: " + baseHealth);
        Label selectedLabel = new Label("Ausgewählt: " + currentTowerType.getDisplayName());
        messageLabel = new Label();
        messageLabel.setTextFill(Color.RED);
        messageLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        messageLabel.setVisible(false);
        messageLabel.setMouseTransparent(true);
        StackPane.setAlignment(messageLabel, Pos.TOP_CENTER);
        StackPane.setMargin(messageLabel, new Insets(60,0,0,0));

        // Turm-Auswahl-Buttons
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

        // Start- & Pause-Buttons
        Button startButton = new Button("Spiel starten");
        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> {
            paused = !paused;
            pauseButton.setText(paused ? "Fortsetzen" : "Pause");
        });

        // Top-Bar (muss VOR recalcPathAndSpots referenziert sein)
        HBox topBar = new HBox(8,
                buttonBar,
                selectedLabel,
                goldLabel,
                healthLabel,
                waveLabel,      // <-- neu
                startButton,
                pauseButton,
                messageLabel
        );
        topBar.setPadding(new Insets(5));
        topBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(selectedLabel, Priority.ALWAYS);

        // Start-Button-Handler
        startButton.setOnAction(e -> {
            gameStarted = true;
            startButton.setDisable(true);
            recalcPathAndSpots(canvas, canvas.getWidth(), canvas.getHeight());
            currentWave = 1;
            initWave(currentWave);
        });

        // Layout
        StackPane gamePane = new StackPane(canvas, damageOverlay, messageLabel);
        BorderPane root     = new BorderPane(gamePane, topBar, null, null, null);

        // Scene & Resize bindings
        Scene scene = new Scene(root, 900, 650);
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty().subtract(topBar.heightProperty()));
        damageOverlay.widthProperty().bind(canvas.widthProperty());
        damageOverlay.heightProperty().bind(canvas.heightProperty());

        // Show stage
        stage.setScene(scene);
        stage.setTitle("Tower Defense");
        stage.show();

        // Initial path & spots
        recalcPathAndSpots(canvas, canvas.getWidth(), canvas.getHeight() - topBar.getHeight());

        // Mouse handler
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleCanvasClick);

        // Main loop
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (gameStarted && !paused) {
                    if (!spawnQueue.isEmpty()) {
                        if (lastSpawnTime == 0 || now - lastSpawnTime >= waveSpawnInterval) {
                            enemies.add(spawnQueue.remove(0).get());
                            lastSpawnTime = now;
                        }
                    } else if (enemies.isEmpty()) {
                        if (currentWave >= maxWaves) {
                            gameStarted = false;
                            messageLabel.setText("Du hast alle Wellen geschafft!");
                            messageLabel.setVisible(true);
                        } else {
                            currentWave++;
                            initWave(currentWave);
                        }
                    }
                    updateGame();
                }
                drawGame(gc);
            }
        }.start();
    }


    private void initWave(int wave) {
        this.currentWave = wave;
        waveLabel.setText("Runde: " + wave);
        spawnQueue.clear();

        // Basismenge pro Typ
        int nNormal = baseNormal + (wave - 1) * 5;
        int nFast   = baseFast   + (wave - 1) * 2;
        int nTank   = baseTank   + (wave - 1) * 1;

        // 1) normale, schnelle und Panzer-Gegner
        for (int i = 0; i < nNormal; i++) spawnQueue.add(() -> new Enemy(pathPoints));
        for (int i = 0; i < nFast;   i++) spawnQueue.add(() -> new FastEnemy(pathPoints));
        for (int i = 0; i < nTank;   i++) spawnQueue.add(() -> new TankEnemy(pathPoints));

        // 2) ab Welle 3: FlowEnemy, MetalBloon & RegenEnemy
        if (wave >= 3) {
            int extras = wave - 2; // ab Welle 3 → 1, Welle 4 → 2, …, Welle 10 → 8

            // FlowEnemy
            for (int i = 0; i < extras; i++) {
                spawnQueue.add(() -> new FlowEnemy(pathPoints));
            }
            // MetalBloon
            for (int i = 0; i < extras; i++) {
                spawnQueue.add(() -> new MetalBloon(pathPoints));
            }
            // RegenEnemy
            for (int i = 0; i < extras; i++) {
                spawnQueue.add(() -> new RegenEnemy(pathPoints));
            }
        }

        // 3) zufällige Reihenfolge
        Collections.shuffle(spawnQueue);

        // 4) Spawn-Intervall anpassen (min. 0.2 s)
        long baseIntervalNs = 1_000_000_000L;
        waveSpawnInterval = Math.max(
                200_000_000L,
                baseIntervalNs - (wave - 1) * 100_000_000L
        );
        lastSpawnTime = 0;

        // 5) Wellen-Message
        messageLabel.setText("Welle " + wave);
        messageLabel.setVisible(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> messageLabel.setVisible(false));
        pause.play();
    }




    private void flashDamageOverlay() {
        damageOverlay.setVisible(true);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> damageOverlay.setVisible(false));
            }
        }, 200); // 200 ms sichtbar
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
                    TowerType type = towerTypeMap.get(t);
                    int refund = type.getCost() / 2;
                    gold += refund;
                    goldLabel.setText("Gold: " + gold);
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
        if (paused) {
            showMessage("Spiel ist pausiert – keine Platzierung möglich.");
            return;
        }
        for (Point2D spot : buildSpots) {
            if (spot.distance(click) < SPOT_MARGIN &&
                    towers.stream().noneMatch(t ->
                            Math.hypot(t.getX() - spot.getX(), t.getY() - spot.getY()) < 1)) {

                // Preis prüfen
                int cost = currentTowerType.getCost();
                if (gold < cost) {
                    showMessage("Nicht genug Gold für " + currentTowerType.getDisplayName());
                    return;
                }

                // Turm setzen
                Tower tw = new Tower(spot.getX(), spot.getY(), currentTowerType);
                towers.add(tw);
                towerTypeMap.put(tw, currentTowerType);

                // Gold abziehen und anzeigen
                gold -= cost;
                goldLabel.setText("Gold: " + gold);

                return;
            }
        }
    }

    private void showMessage(String text) {
        messageLabel.setText(text);
        messageLabel.setVisible(true);

        // Automatisch nach 2 Sekunden ausblenden
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> messageLabel.setVisible(false));
            }
        }, 2000);
    }

    private void updateGame() {
        // 1) Türme feuern
        for (Tower t : towers) {
            t.tickCooldown();
            TowerType type = towerTypeMap.get(t);
            if (!t.readyToFire()) continue;

            //  --- FlowEnemy nur von bestimmten Türmen anvisierbar ---
            Enemy target = null;
            double bestDist = Double.MAX_VALUE;
            for (Enemy en : enemies) {
                // 1) Abstand prüfen
                double d = Math.hypot(t.getX() - en.getX(), t.getY() - en.getY());
                if (d > type.getRange()) continue;
                // 2) Stealth-FlowEnemy überspringen, wenn Turm nicht geeignet
                if (en instanceof FlowEnemy) {
                    boolean canSee = (type == TowerType.YELLOW   // Magieturm
                            || type == TowerType.SNIPER     // Sniperturm
                            || type == TowerType.RED);      // Bogenturm == RED
                    if (!canSee) continue;
                }
                // 3) Bestes Ziel nach Distanz
                if (d < bestDist) {
                    bestDist = d;
                    target = en;
                }
            }
            if (target == null) continue;

            // … hier kommt dein bestehender Abschuss-Code …
            Point2D origin = new Point2D(t.getX(), t.getY());

            if (type == TowerType.GREEN) {
                iceBlasts.add(new IceBlast(origin.getX(), origin.getY()));
                for (Enemy en : enemies) {
                    if (origin.distance(en.getX(), en.getY()) <= type.getRange()) {
                        en.takeDamage(type.getDamage());
                        en.applySlow(100, 0.5);
                    }
                }
            } else if (type == TowerType.FIRE) {
                Point2D aim = calculateIntercept(
                        origin,
                        new Point2D(target.getX(), target.getY()),
                        target.getVelocity(),
                        PROJECTILE_SPEED
                );
                if (aim == null) aim = new Point2D(target.getX(), target.getY());
                projectiles.add(new FireProjectile(origin, aim, PROJECTILE_SPEED, type.getDamage(), type.getColor()));
            } else if (type == TowerType.BLUE) {
                Point2D aim = calculateIntercept(origin, new Point2D(target.getX(), target.getY()), target.getVelocity(), PROJECTILE_SPEED);
                if (aim == null) aim = new Point2D(target.getX(), target.getY());
                projectiles.add(new CannonProjectile(origin, aim, PROJECTILE_SPEED, type.getDamage(), type.getColor()));
            } else if (type == TowerType.YELLOW) {
                Point2D aim = calculateIntercept(origin, new Point2D(target.getX(), target.getY()), target.getVelocity(), PROJECTILE_SPEED);
                if (aim == null) aim = new Point2D(target.getX(), target.getY());
                projectiles.add(new MagicProjectile(origin, aim, PROJECTILE_SPEED, type.getDamage(), type.getColor(), type.getRange()));
            } else if (type == TowerType.POISON) {
                Point2D targetPos = new Point2D(target.getX(), target.getY());
                poisonClouds.add(new PoisonCloud(targetPos.getX(), targetPos.getY(), type.getRange() / 2, 100, type.getDamage()));
            } else if (type == TowerType.SNIPER) {
                Point2D aim = calculateCurvedIntercept(origin, target, PROJECTILE_SPEED);
                if (aim == null) aim = new Point2D(target.getX(), target.getY());
                projectiles.add(new Projectile(origin, aim, PROJECTILE_SPEED, type.getDamage(), type.getColor()));
            } else {
                Point2D aim = calculateIntercept(origin, new Point2D(target.getX(), target.getY()), target.getVelocity(), PROJECTILE_SPEED);
                if (aim == null) aim = new Point2D(target.getX(), target.getY());
                projectiles.add(new Projectile(origin, aim, PROJECTILE_SPEED, type.getDamage(), type.getColor()));
            }

            t.resetCooldown();
        }

        enemies.forEach(Enemy::update);

        // 3) Projektil-Kollisionen
        // 3) Projektil-Kollisionen
        Iterator<Projectile> pit = projectiles.iterator();
        while (pit.hasNext()) {
            Projectile p = pit.next();
            p.update();

            boolean remove = false;

            // wir durchlaufen alle Gegner
            for (Enemy en : enemies) {
                // Trefferbereich prüfen
                if (Math.hypot(p.getX() - en.getX(), p.getY() - en.getY()) < 12) {

                    // ---- MetalBloon-Spezialfall ----
                    if (en instanceof MetalBloon mb) {
                        // 1) Hülle noch dran?
                        if (mb.isShellIntact()) {
                            // nur Feuer- oder Kanonen-Geschosse dürfen die Hülle beschädigen
                            if (p instanceof FireProjectile || p instanceof CannonProjectile) {
                                mb.hitShell(p.getDamage());
                                remove = true;  // diese Projektile gehen bei Einschlag auf die Hülle kaputt
                            }
                            // andere Projektil-Typen prallen ab und verschwinden NICHT:
                            //   wir setzen remove=false und springen direkt zum nächsten Gegner
                            break;
                        }
                        // 2) Hülle schon weg → ganz normal Schaden aufs Leben:
                        mb.takeDamage(p.getDamage());
                        sparks.add(new Spark(mb.getX(), mb.getY()));
                        if (p instanceof CannonProjectile) explosions.add(new Explosion(mb.getX(), mb.getY()));
                        remove = true;
                        break;
                    }

                    // ---- Normale Magic-/Standard-/Eis-Logik ----
                    if (p instanceof MagicProjectile mp) {
                        if (!mp.hasHit(en)) {
                            en.takeDamage(mp.getDamage());
                            sparks.add(new Spark(en.getX(), en.getY()));
                            mp.registerHit(en);
                        }
                        if (mp.isExpired()) remove = true;

                    } else if (p instanceof FireProjectile) {
                        // z.B. Brenn-Logik…
                        en.applyBurn(90, 1);
                        sparks.add(new Spark(en.getX(), en.getY()));
                        remove = true;

                    } else {
                        // Standardprojektil & CannonProjectile
                        en.takeDamage(p.getDamage());
                        sparks.add(new Spark(en.getX(), en.getY()));
                        if (p instanceof CannonProjectile) {
                            explosions.add(new Explosion(en.getX(), en.getY()));
                        }
                        remove = true;
                    }

                    break; // sobald ein Treffer verarbeitet ist, nicht weiter nach anderen Gegnern schauen
                }
            }

            if (remove) {
                pit.remove();
            }
        }

        Iterator<PoisonCloud> pcIt = poisonClouds.iterator();
        while (pcIt.hasNext()) {
            PoisonCloud cloud = pcIt.next();
            cloud.update(enemies);
            if (!cloud.isAlive()) pcIt.remove();
        }

        explosions.removeIf(ex -> !ex.update());
        iceBlasts.removeIf(ib -> !ib.update());
        sparks.removeIf(sp -> !sp.update());

        Iterator<Enemy> eit = enemies.iterator();
        while (eit.hasNext()) {
            Enemy en = eit.next();
            if (!en.isAlive()) {
                int reward;
                if (en instanceof FastEnemy) {
                    reward = 3;
                } else if (en instanceof TankEnemy) {
                    reward = 4;
                } else {
                    reward = 2;
                }
                gold += reward;
                goldLabel.setText("Gold: " + gold);
                eit.remove();
        } else if (en.hasFinished()) {
                if (en instanceof FastEnemy)      baseHealth -= 1;
                else if (en instanceof TankEnemy) baseHealth -= 5;
                else                               baseHealth -= 3;
                healthLabel.setText("Leben: " + baseHealth);
                flashDamageOverlay();
                if (baseHealth <= 0) {
                    gameStarted = false;
                    showMessage("Spiel verloren!");
                }
                eit.remove();
            }
        }
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

    /**
     * Liefert einen Abfangpunkt, der den Pfadwechsel berücksichtigt.
     * @param origin       Abschussposition des Projektils
     * @param enemy        der Gegner, den wir treffen wollen
     * @param projSpeed    Projektilgeschwindigkeit (px/frame)
     * @return             Zielpunkt für das Projektil
     */
    private Point2D calculateCurvedIntercept(Point2D origin, Enemy enemy, double projSpeed) {
        // 1) Vorbereitung: Pfaddaten und Geschwindigkeit
        List<Point2D> path = enemy.getPath();               // muss als getter existieren
        double baseSpeed = enemy.getBaseSpeed() * enemy.getSpeedMultiplier();
        // 2) Aktuellen Fortschritt (in Segmentindex + Anteil) ermitteln
        double prog = enemy.getProgress();                  // z. B. 3.5 heißt halb im 4. Segment
        // 3) Cumulierte Längen der Segmente vorab berechnen
        List<Double> segLens = new ArrayList<>();
        for (int i = 1; i < path.size(); i++)
            segLens.add(path.get(i-1).distance(path.get(i)));
        List<Double> cumLens = new ArrayList<>();
        double sum = 0;
        for (double l : segLens) {
            sum += l;
            cumLens.add(sum);
        }
        // 4) Simuliere für t=0…maxT (Frames), finde bestes t
        Point2D bestAim = null;
        double bestDiff = Double.MAX_VALUE;
        int maxT = 200; // 200 Frames ≈ 3 Sekunden Vorhersage
        for (int t = 0; t <= maxT; t++) {
            // a) Berechne zurückgelegte Distanz = prog*L0 + t*baseSpeed
            double distSoFar = prog * segLens.get((int)prog) % cumLens.get(cumLens.size()-1)
                    + t * baseSpeed;
            // b) Finde Position auf Pfad bei dieser Distanz
            Point2D future = pointAtDistance(path, cumLens, distSoFar);
            // c) Flugzeit des Projektils
            double fly = origin.distance(future) / projSpeed;
            double diff = Math.abs(fly - t);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestAim = future;
            }
        }
        return bestAim == null ? new Point2D(enemy.getX(), enemy.getY()) : bestAim;
    }

    /**
     * Ermittelt eine Punktkoordinate auf einem Stücklisten‐Pfad bei gegebener Gesamtstrecke.
     */
    private Point2D pointAtDistance(List<Point2D> path, List<Double> cumLen, double dist) {
        if (dist <= 0) return path.get(0);
        double total = cumLen.get(cumLen.size()-1);
        if (dist >= total) return path.get(path.size()-1);
        // Finde Segment
        int seg = 0;
        while (cumLen.get(seg) < dist) seg++;
        double prevCum = seg == 0 ? 0 : cumLen.get(seg-1);
        double segDist = dist - prevCum;
        Point2D A = path.get(seg), B = path.get(seg+1);
        double frac = segDist / A.distance(B);
        return new Point2D(
                A.getX() + (B.getX()-A.getX())*frac,
                A.getY() + (B.getY()-A.getY())*frac
        );
    }

    private void drawGame(GraphicsContext gc) {
        // Hintergrund grünlich einfärben
        gc.setFill(Color.web("#d0f0c0"));
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        // Pfad zeichnen (dunkelgrau)
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(PATH_WIDTH);
        for (int i = 1; i < pathPoints.size(); i++) {
            Point2D p1 = pathPoints.get(i - 1);
            Point2D p2 = pathPoints.get(i);
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }

        // Build Spots zeichnen (blau)
        gc.setFill(Color.LIGHTBLUE);
        for (Point2D spot : buildSpots) {
            gc.fillOval(spot.getX() - SPOT_MARGIN, spot.getY() - SPOT_MARGIN, SPOT_MARGIN * 2, SPOT_MARGIN * 2);
        }

        // Türme zeichnen
        // Türme zeichnen (quadratisch)
        for (Tower t : towers) {
            TowerType type = towerTypeMap.get(t);
            gc.setFill(type.getColor());
            gc.fillRect(t.getX() - 10, t.getY() - 10, 20, 20); // ✅ quadratisch
        }


        // Projektile zeichnen
        for (Projectile p : projectiles) {
            p.draw(gc);
        }


        // Poison-Wolken zeichnen
        for (PoisonCloud cloud : poisonClouds) {
            cloud.render(gc);
        }

        // Explosionen, Eis, Funken etc.
        for (Explosion ex : explosions) {
            ex.draw(gc);
        }
        for (IceBlast ib : iceBlasts) {
            ib.draw(gc);
        }
        for (Spark sp : sparks) {
            sp.draw(gc);
        }

        // Gegner zeichnen
        for (Enemy en : enemies) {
            en.draw(gc);
        }


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