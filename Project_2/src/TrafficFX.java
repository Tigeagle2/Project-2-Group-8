/**
 * Author: Caden Douglas
 * @due 12/01/2025
 *
 * Traffic intersection simulation using JavaFX.
 * Implements Roads, Places, Cars, CarQueues, TrafficLights, Simulator, and rendering.
 *
 * NOTE: Run with JavaFX on your module path.
 */

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import javafx.scene.paint.Color;
import javafx.scene.control.TextInputDialog;
import javafx.geometry.Point2D;

import java.util.*;

// ============================================================================
// PUBLIC MAIN CLASS — RUNS THE SIMULATION
// ============================================================================
public class TrafficFX extends Application 
{

    private Simulator simulator;     // core simulation logic (cars, roads, lights)
    private Canvas canvas;           // where we draw everything

    // physics / render timing
    public static final long TARGET_FPS = 20;
    private static final long FRAME_TIME = 1_000_000_000L / TARGET_FPS;

    private long lastUpdate = 0;

    // Canvas metrics
    private static final int CELL_SIZE = 20;       // world-unit → screen pixel scale
    private static final int CANVAS_SIZE = 600;

    @Override
    public void start(Stage stage) 
    {

        // ---------------------------------------------------------------
        // USER CONFIGURATION INPUT
        // ---------------------------------------------------------------
        // The user controls light timing, arrival rate, and duration.
        int greenNSsec = askInt("Green time North/South (seconds):", "30");
        int greenEWsec = askInt("Green time East/West (seconds):", "30");
        int probN      = askInt("Car arrival probability (1/n):", "6");
        int duration   = askInt("Simulation duration (ticks):", "10000");

        // Convert seconds → "ticks" for the simulation update loop
        int greenNS = Math.max(1, greenNSsec * (int) TARGET_FPS);
        int greenEW = Math.max(1, greenEWsec * (int) TARGET_FPS);

        // create simulation engine
        simulator = new Simulator(greenNS, greenEW, 1.0 / Math.max(1, probN), duration);

        // ---------------------------------------------------------------
        // CANVAS SETUP
        // ---------------------------------------------------------------
        canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        Scene scene = new Scene(new StackPane(canvas), CANVAS_SIZE, CANVAS_SIZE);

        stage.setTitle("Traffic Intersection Simulation");
        stage.setScene(scene);
        stage.show();

        // ---------------------------------------------------------------
        // MAIN GAME LOOP (runs ~20 FPS)
        // ---------------------------------------------------------------
        new AnimationTimer() 
        {
            @Override
            public void handle(long now) 
            {

                // first-frame init
                if (lastUpdate == 0) 
                {
                    lastUpdate = now;
                    return;
                }

                // enforce constant FPS timing
                if (now - lastUpdate >= FRAME_TIME) 
                {

                    if (simulator.isRunning()) 
                    {
                        simulator.update();  // update cars + lights
                        draw();              // redraw the frame
                    } else 
                    {
                        stop();
                        System.out.println("Simulation ended.");
                        System.out.println(simulator.getStats());
                    }
                    lastUpdate = now;
                }
            }
        }.start();
    }

    /** Pops up a number-entry dialog and returns an integer. */
    private int askInt(String header, String defValue) 
    {
        TextInputDialog d = new TextInputDialog(defValue);
        d.setHeaderText(header);
        return Integer.parseInt(d.showAndWait().orElse(defValue));
    }

    // ============================================================================
    // RENDERING METHODS
    // ============================================================================

    /** Redraws the entire scene. */
    private void draw() 
    {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        drawRoads(gc);
        drawIntersection(gc);
        drawTrafficLights(gc);
        drawCars(gc);
    }

    /** Draws the four approach roads. */
private void drawRoads(GraphicsContext gc) 
{

    // ----------------------------
    // ROAD FILL
    // ----------------------------
    gc.setFill(Color.GRAY);

    // Horizontal
    gc.fillRect(0, 250, CANVAS_SIZE, 100);  // inbound
    gc.fillRect(0, 200, CANVAS_SIZE, 50);   // outbound

    // Vertical
    gc.fillRect(250, 0, 100, CANVAS_SIZE);  // inbound
    gc.fillRect(200, 0, 50, CANVAS_SIZE);   // outbound


    // ----------------------------
    // CENTER LINES (dotted)
    // ----------------------------
    gc.setStroke(Color.WHITE);
    gc.setLineWidth(3);
    gc.setLineDashes(12, 12); // dash length, gap length


    // ---- Horizontal center line ----
    gc.strokeLine(
        0,         // start X
        275,       // center of horizontal road
        CANVAS_SIZE,
        275
    );

    // ---- Vertical center line ----
    gc.strokeLine(
        275,       // center of vertical road
        0,
        275,
        CANVAS_SIZE
    );

    // reset dashes for other objects
    gc.setLineDashes(null);
}


    /** Draws central intersection block. */
    private void drawIntersection(GraphicsContext gc) 
    {
        gc.setFill(Color.BLACK);
        gc.fillRect(200, 200, 150, 150);
    }

    /** Draws four traffic lights. */
    private void drawTrafficLights(GraphicsContext gc) 
    {
        // Each one uses simulator.getTrafficLight(dir)
        drawLight(gc, 350, 140, simulator.getTrafficLight("north").getState());
        drawLight(gc, 180, 350, simulator.getTrafficLight("south").getState());
        drawLight(gc, 350, 350, simulator.getTrafficLight("east").getState());
        drawLight(gc, 180, 140, simulator.getTrafficLight("west").getState());
    }

    /** Draws a single 3-bulb vertical traffic light. */
    private void drawLight(GraphicsContext gc, double x, double y, String state) 
    {
        gc.setFill(Color.BLACK);
        gc.fillRect(x, y, 20, 60);

        gc.setFill(state.equals("red") ? Color.RED : Color.DARKGRAY);
        gc.fillOval(x + 2, y + 2, 16, 16);

        gc.setFill(state.equals("yellow") ? Color.YELLOW : Color.DARKGRAY);
        gc.fillOval(x + 2, y + 22, 16, 16);

        gc.setFill(state.equals("green") ? Color.LIMEGREEN : Color.DARKGRAY);
        gc.fillOval(x + 2, y + 42, 16, 16);
    }

    /** Draws all cars using their direction & grid position. */
    private void drawCars(GraphicsContext gc) 
    {
        for (Car car : simulator.getAllCars()) 
        {
            if (car.getPlace() == null) continue;

            Point2D pos = getCarPosition(car);
            gc.setFill(car.getColor());

            // Draw long axis according to orientation
            boolean vertical = car.getDirection().equals("north") ||
                               car.getDirection().equals("south");

            if (vertical) gc.fillRect(pos.getX(), pos.getY(), 10, 20);
            else          gc.fillRect(pos.getX(), pos.getY(), 20, 10);
        }
    }

    /** Converts Place(row,col) to screen coordinates. */
    private Point2D getCarPosition(Car car) 
    {
        Place p = car.getPlace();
        // simple visual conversion
        double px = p.col * CELL_SIZE + 40;
        double py = p.row * CELL_SIZE + 40;

        // keep cars inside drawing bounds
        px = Math.min(Math.max(px, 0), CANVAS_SIZE - 20);
        py = Math.min(Math.max(py, 0), CANVAS_SIZE - 20);

        return new Point2D(px, py);
    }
    public static void main(String[] args) 
    {
        launch(args);
    }
}
// ============================================================================
// SIMULATOR — UPDATES CARS, LIGHTS, AND CORE LOGIC
// ============================================================================
class Simulator 
{

    private boolean running = true;
    private int time = 0;
    private final int duration;
    private final double arrivalProb;

    // store all world components
    private final Map<String, TrafficLight> trafficLights = new HashMap<>();
    private final Map<String, Road> roads = new HashMap<>();
    private final Map<String, CarQueue> queues = new HashMap<>();

    private final List<Car> activeCars = new ArrayList<>();
    private final List<Car> finishedCars = new ArrayList<>();

    private int totalCreated = 0;
    private int totalExited = 0;

    // offsets used to position the roads in the grid
    private static final int OFFSET_ROW = 5;
    private static final int OFFSET_COL = 5;

    public Simulator(int greenNS, int greenEW, double prob, int dur) 
    {
        this.arrivalProb = prob;
        this.duration = dur;

        // yellow fixed at 6 frames of animation FPS
        int yellow = 1 * (int) TrafficFX.TARGET_FPS;

        // Create the two shared traffic light cycles:
        // north & south share same timing
        // east & west share same timing
        TrafficLight ns = new TrafficLight(greenNS, yellow, greenEW, "green");
        TrafficLight ew = new TrafficLight(greenEW, yellow, greenNS, "red");

        trafficLights.put("north", ns);
        trafficLights.put("south", ns);
        trafficLights.put("east",  ew);
        trafficLights.put("west",  ew);

        // Create straight roads for all directions
        roads.put("north", new Road("north", 12, OFFSET_ROW - 10, OFFSET_COL + 7, false));
        roads.put("south", new Road("south", 12, OFFSET_ROW + 10, OFFSET_COL + 7, false));
        roads.put("east",  new Road("east",  12, OFFSET_ROW + 6,  OFFSET_COL + 18, true));
        roads.put("west",  new Road("west",  12, OFFSET_ROW + 14, OFFSET_COL - 12, true));

        // Make a queue for each road
        for (String d : roads.keySet())
            queues.put(d, new CarQueue(roads.get(d), d));
    }

    /** Runs once per frame (20 times per second). */
    public void update() 
    {
        time++;

        // auto-stop when time expires
        if (time > duration) 
        {
            running = false;
            return;
        }

        // update each traffic-light ONCE (north/south share object)
        new HashSet<>(trafficLights.values()).forEach(TrafficLight::update);

        // probabilistic car spawning
        for (CarQueue q : queues.values()) 
        {
            if (Math.random() < arrivalProb) trySpawnCar(q);
        }

        // move cars forward
        for (String dir : roads.keySet()) 
        {
            moveCarsOnRoad(roads.get(dir), trafficLights.get(dir));
        }

        // remove cars that exited
        activeCars.removeIf(Car::atEnd);
    }

    /** Attempts to put a new car at the head of a road if empty. */
    private void trySpawnCar(CarQueue q) 
    {
        Place start = q.getRoad().getFirstPlace();

        if (start != null && start.getOccupiedBy() == null) 
        {

            Color[] colors = { Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE };
            Color c = colors[new Random().nextInt(colors.length)];

            Car car = new Car(c, start, q.getDirection());
            activeCars.add(car);

            totalCreated++;
        }
    }

    /** Moves cars along a road if light & next place allows it. */
    private void moveCarsOnRoad(Road road, TrafficLight tl) 
    {
        List<Place> places = road.getAllPlaces();

        // iterate backwards so movement doesn't interfere with earlier cars
        for (int i = places.size() - 1; i >= 0; i--) 
        {
            Place p = places.get(i);
            Car car = p.getOccupiedBy();
            if (car == null) continue;

            if (!car.canMove()) continue;

            Place next = p.next();
            boolean enteringIntersection = next != null && road.isIntersectionEntry(next);

            // if entering intersection, must have green
            if (enteringIntersection && !tl.getState().equals("green"))
                continue;

            car.move();

            // if car left the road, record stats
            if (car.atEnd()) 
            {
                finishedCars.add(car);
                totalExited++;
            }
        }
    }

    public boolean isRunning() 
    { 
        return running; 
    }
    public List<Car> getAllCars() 
    { 
        return new ArrayList<>(activeCars); 
    }
    public TrafficLight getTrafficLight(String dir) 
    { 
        return trafficLights.get(dir); 
    }

    /** Summary stats for printing after simulation. */
    public String getStats() 
    {
        return "Time: " + time +
               "\nTotal created: " + totalCreated +
               "\nTotal exited: " + totalExited +
               "\nActive: " + activeCars.size();
    }
}
// ============================================================================
// PLACE — A SINGLE CELL ON A ROAD
// A Place may contain: a car, a block state, and a link to the next Place.
// ============================================================================
class Place 
{

    int row, col;              // logical grid position
    private boolean blocked;   // not used heavily, but supports obstacles
    private Car occupiedBy;    // the car currently in this cell
    private Place nextPlace;   // the Place ahead of this one

    public Place(int row, int col) 
    {
        this.row = row;
        this.col = col;
    }

    /** True if nothing is blocking and no car is present. */
    public boolean freeToMove() 
    {
        return occupiedBy == null && !blocked;
    }

    public Place next() 
    { 
        return nextPlace; 
    }
    public void setNext(Place p) 
    { 
        this.nextPlace = p; 
    }

    public void block()   
    { 
        blocked = true; 
    }
    public void unblock() 
    { 
        blocked = false; 
    }

    public Car getOccupiedBy() 
    { 
        return occupiedBy; 
    }
    public void setOccupiedBy(Car c) 
    { 
        this.occupiedBy = c; 
    }
}
// ============================================================================
// CAR — REPRESENTS A VEHICLE MOVING ALONG A ROAD
// ============================================================================
class Car 
{

    private Place place;         // current position
    private Place lastPlace;     // used to clear the cell when moving
    private final Color color;
    private final String direction;
    private boolean exited = false;

    public Car(Color color, Place place, String direction) 
    {
        this.color = color;
        this.place = place;
        this.direction = direction;

        if (place != null) place.setOccupiedBy(this);
    }

    public Place getPlace() 
    { 
        return place; 
    }
    public Color getColor() 
    { 
        return color; 
    }
    public String getDirection() 
    { 
        return direction; 
    }

    /** Car can move if the next Place exists and is free. */
    public boolean canMove() 
    {
        Place next = (place == null) ? null : place.next();
        return next != null && next.freeToMove();
    }

    /** Move into next Place or exit the road entirely. */
    public void move() 
    {
        if (place == null) return;

        Place next = place.next();

        // leaving the road
        if (next == null) 
        {
            place.setOccupiedBy(null);
            place = null;
            exited = true;
            return;
        }

        // free previous
        if (lastPlace != null) lastPlace.setOccupiedBy(null);

        lastPlace = place;
        place = next;
        place.setOccupiedBy(this);
    }

    public boolean atEnd() 
    {
        return exited;
    }
}
// ============================================================================
// ROAD — LINEAR LIST OF PLACES (CELLS)
// Contains logic for intersection entry detection.
// ============================================================================
class Road 
{

    private final List<Place> places = new ArrayList<>();
    private final String direction;

    // Index of the Place that leads into the intersection
    private final int intersectionIndex;

    public Road(String direction, int length, int startRow, int startCol, boolean horizontal) 
    {
        this.direction = direction;

        Place prev = null;
        for (int i = 0; i < length; i++) 
        {

            int row = horizontal ? startRow : startRow + i;
            int col = horizontal ? startCol + i : startCol;

            Place p = new Place(row, col);
            places.add(p);

            if (prev != null) prev.setNext(p);
            prev = p;
        }

        // mark last few cells before intersection
        intersectionIndex = Math.max(0, places.size() - 3);
    }

    public Place getFirstPlace() 
    { 
        return places.isEmpty() ? null : places.get(0); 
    }
    public List<Place> getAllPlaces() 
    { 
        return places; 
    }

    /** True if p is the last cell before entering the intersection. */
    public boolean isIntersectionEntry(Place p) 
    {
        return places.indexOf(p) == intersectionIndex;
    }
}
// ============================================================================
// TRAFFIC LIGHT — CYCLES THROUGH GREEN > YELLOW > RED
// ============================================================================
class TrafficLight 
{

    private String state;
    private final int yellowDuration;
    private int greenDuration;
    private int redDuration;
    private int elapsed = 0;

    public TrafficLight(int green, int yellow, int red, String initialState) 
    {
        this.greenDuration = Math.max(1, green);
        this.yellowDuration = Math.max(1, yellow);
        this.redDuration = Math.max(1, red);
        this.state = initialState;
    }

    public String getState() 
    { 
        return state; 
    }

    /** Called once per simulation tick. */
    public void update() 
    {
        elapsed++;

        switch (state) 
        {
            case "green":
                if (elapsed >= greenDuration) cycleTo("yellow");
                break;

            case "yellow":
                if (elapsed >= yellowDuration) cycleTo("red");
                break;

            case "red":
                if (elapsed >= redDuration) cycleTo("green");
                break;
        }
    }

    private void cycleTo(String next) 
    {
        state = next;
        elapsed = 0;
    }
}
// ============================================================================
// CARQUEUE — HOLDS WAITING CARS
// ============================================================================
class CarQueue 
{

    private final Queue<Car> cars = new LinkedList<>();
    private final Road road;
    private final String direction;

    public CarQueue(Road road, String direction) 
    {
        this.road = road;
        this.direction = direction;
    }

    public void addCar(Car c) 
    { 
        cars.offer(c); 
    }
    public Car removeCar() 
    { 
        return cars.poll(); 
    }

    public int getSize() 
    { 
        return cars.size(); 
    }
    public List<Car> getCars() 
    { 
        return new ArrayList<>(cars); 
    }

    public Road getRoad() 
    { 
        return road; 
    }
    public String getDirection() 
    { 
        return direction; 
    }
}
