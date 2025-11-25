/**
 *
 * Author: Caden Douglas
 * @due 12/01/2025
 *
 * Fixed and completed version of the original starter code.
 * Implements Roads, Places, CarQueue, TrafficLight, Simulator, and car movement.
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
import java.util.*;
import javafx.geometry.Point2D;

public class TrafficFX extends Application 
{
    private Simulator simulator;
    private Canvas canvas;
    public static final long target = 20; // frames per second target
    private static final long frameTime = 1_000_000_000 / target;
    private long lastUpdate = 0;

    // visual grid scale
    private static final int CELL_SIZE = 20;
    private static final int CANVAS_SIZE = 600;

    @Override
    public void start(Stage stage) 
    {
        // User inputs
        TextInputDialog dialogNS = new TextInputDialog("30");
        dialogNS.setHeaderText("Green time North/South (seconds):");
        int greenNSsec = (Integer.parseInt(dialogNS.showAndWait().orElse("30")));

        TextInputDialog dialogEW = new TextInputDialog("30");
        dialogEW.setHeaderText("Green time East/West (seconds):");
        int greenEWsec = (Integer.parseInt(dialogEW.showAndWait().orElse("30")));

        TextInputDialog dialogProb = new TextInputDialog("6");
        dialogProb.setHeaderText("Car arrival probability (1/n, e.g., 6):");
        int probN = Integer.parseInt(dialogProb.showAndWait().orElse("6"));

        TextInputDialog dialogDuration = new TextInputDialog("10000");
        dialogDuration.setHeaderText("Simulation duration (timer units):");
        int duration = Integer.parseInt(dialogDuration.showAndWait().orElse("10000"));

        // Convert seconds to simulation ticks (we use ticks ~ frames)
        int greenNS = Math.max(1, greenNSsec * (int)target);
        int greenEW = Math.max(1, greenEWsec * (int)target);

        // initialize simulator with proper timings, arrival probability per tick
        simulator = new Simulator(greenNS, greenEW, 1.0 / Math.max(1, probN), duration);

        // create a sample car to show initial state (optional)
        // Place p = new Place(5,5); Car c = new Car(Color.BLUE, p, "north"); simulator.addCar(c);

        canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, CANVAS_SIZE, CANVAS_SIZE);
        stage.setTitle("Traffic Intersection Simulation");
        stage.setScene(scene);
        stage.show();

        // AnimationTimer for simulation updates
        new AnimationTimer() 
        {
            @Override
            public void handle(long now) 
            {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                if (now - lastUpdate >= frameTime)
                {
                    if (simulator.isRunning()) 
                    {
                        simulator.update();
                        draw();
                    } 
                    else 
                    {
                        stop();
                        // Output stats to console
                        System.out.println("Simulation ended. Stats:");
                        System.out.println(simulator.getStats());
                    }
                    lastUpdate = now;
                }
            }
        }.start();
    }

    private void draw() 
    {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw roads (4 lanes crossing at center)
        gc.setFill(Color.GRAY);
        // Horizontal (East-West)
        gc.fillRect(0, 250, canvas.getWidth(), 100); // Bottom inbound
        gc.fillRect(0, 200, canvas.getWidth(), 50); // Top outbound
        // Vertical (North-South)
        gc.fillRect(250, 0, 100, canvas.getHeight()); // Right inbound
        gc.fillRect(200, 0, 50, canvas.getHeight()); // Left outbound

        // Draw intersection box
        gc.setFill(Color.BLACK);
        gc.fillRect(200, 200, 150, 150);

        // Draw traffic lights around intersection using simulator lights
        drawLight(gc, 350, 140, simulator.getTrafficLight("north").getState()); // North-facing
        drawLight(gc, 180, 350, simulator.getTrafficLight("south").getState()); // South-facing
        drawLight(gc, 350, 350, simulator.getTrafficLight("east").getState());  // East-facing
        drawLight(gc, 180, 140, simulator.getTrafficLight("west").getState());  // West-facing

        // Draw cars based on their Place positions
        for (Car car : simulator.getAllCars()) 
        {
            Place place = car.getPlace();
            if (place == null) continue;
            Point2D pos = getCarPosition(car);
            gc.setFill(car.getColor());
            String dir = car.getDirection();
            // draw oriented rectangles for clarity
            if (dir.equals("north") || dir.equals("south")) {
                gc.fillRect(pos.getX(), pos.getY(), 10, 20);
            } else {
                gc.fillRect(pos.getX(), pos.getY(), 20, 10);
            }
        }
    }

    private void drawLight(GraphicsContext gc, double x, double y, String state) 
    {
        gc.setFill(Color.BLACK);
        gc.fillRect(x, y, 20, 60);
        gc.setFill("red".equals(state) ? Color.RED : Color.DARKGRAY);
        gc.fillOval(x + 2, y + 2, 16, 16);
        gc.setFill("yellow".equals(state) ? Color.YELLOW : Color.DARKGRAY);
        gc.fillOval(x + 2, y + 22, 16, 16);
        gc.setFill("green".equals(state) ? Color.LIMEGREEN : Color.DARKGRAY);
        gc.fillOval(x + 2, y + 42, 16, 16);
    }

    private Point2D getCarPosition(Car car) 
{
   
        // Map Place row/col to pixels based on a simple offset and CELL_SIZE
        Place place = car.getPlace();
        double px = place.col * CELL_SIZE + 40; // offset to center visible area
        double py = place.row * CELL_SIZE + 40;
        // clamp
        px = Math.max(0, Math.min(px, CANVAS_SIZE - 20));
        py = Math.max(0, Math.min(py, CANVAS_SIZE - 20));
        return new Point2D(px, py);
    }

    public static void main(String[] args) 
    {
        launch(args);
    }
}



//Simulator: orchestrates roads, queues, traffic lights, and car movement 
class Simulator 
{
    private boolean running = true;
    private int time = 0;
    private final int duration;
    private final double arrivalProb; // probability per tick

    private final Map<String, TrafficLight> trafficLights = new HashMap<>();
    private final Map<String, Road> roads = new HashMap<>();
    private final Map<String, CarQueue> queues = new HashMap<>();

    private final List<Car> cars = new ArrayList<>();
    private final List<Car> finishedCars = new ArrayList<>();

    // statistics
    private int totalCreated = 0;
    private int totalExited = 0;

    // intersection placement constants (logical grid positions chosen to map to drawing)
    private static final int OFFSET_ROW = 5;
    private static final int OFFSET_COL = 5;

    public Simulator(int greenNS, int greenEW, double prob, int dur) 
    {
        this.arrivalProb = prob;
        this.duration = dur;

        // Set yellow to 6 * target ticks (project requests yellow = 6 units)
        int yellow = 6 * (int)TrafficFX.target;

        // Create traffic lights: NS green initially, EW red initially
        TrafficLight nsLight = new TrafficLight(greenNS, yellow, greenEW, "green");
        TrafficLight ewLight = new TrafficLight(greenEW, yellow, greenNS, "red");
        trafficLights.put("north", nsLight);
        trafficLights.put("south", nsLight); // north and south share
        trafficLights.put("east", ewLight);
        trafficLights.put("west", ewLight);

        // Create roads: each road is length 12 places (tweakable)
        Road north = new Road("north", 12, OFFSET_ROW - 12, OFFSET_COL + 6, false); // vertical downwards toward intersection
        Road south = new Road("south", 12, OFFSET_ROW + 18, OFFSET_COL + 14, false); // vertical upwards
        Road east  = new Road("east", 12, OFFSET_ROW + 6, OFFSET_COL + 18, true); // horizontal leftwards
        Road west  = new Road("west", 12, OFFSET_ROW + 14, OFFSET_COL - 12, true); // horizontal rightwards

        roads.put("north", north);
        roads.put("south", south);
        roads.put("east", east);
        roads.put("west", west);

        // Create queues and link them to roads
        queues.put("north", new CarQueue(north, "north"));
        queues.put("south", new CarQueue(south, "south"));
        queues.put("east", new CarQueue(east, "east"));
        queues.put("west", new CarQueue(west, "west"));
    }

    public void update() 
    {
        time++;
        if (time > duration) 
        {
            running = false;
            return;
        }

        // Update unique traffic light objects
        Set<TrafficLight> updated = new HashSet<>(trafficLights.values());
        for (TrafficLight tl : updated) tl.update();

        // Spawn cars in each queue
        for (CarQueue q : queues.values()) {
            if (Math.random() < arrivalProb) {
                Place start = q.getRoad().getFirstPlace();
                if (start != null && start.getOccupiedBy() == null) {
                    Color[] colors={Color.RED,Color.BLUE,Color.GREEN,Color.ORANGE,Color.PURPLE};
                    Color carColor= colors[new Random().nextInt(colors.length)];
                    Car newCar = new Car(carColor, start, q.getDirection());
                    q.addCar(newCar);
                    cars.add(newCar);
                    totalCreated++;
                }
            }
            // local queue movement (push into road if free) is handled by road movement loop
        }

        // Move cars along roads (process each road separately to avoid collisions)
        for (String dir : Arrays.asList("north", "south", "east", "west")) {
            Road r = roads.get(dir);
            TrafficLight light = trafficLights.get(dir);
            List<Place> placeList = r.getAllPlaces();
            // move from tail to head to avoid overwriting places
            for (int i = placeList.size() - 1; i >= 0; i--) {
                Place p = placeList.get(i);
                Car occupant = p.getOccupiedBy();
                if (occupant != null) {
                    // If car is at road's last place, then it should exit next move
                    if (occupant.canMove()) {
                        // If next place is intersection entry, check light
                        Place next = occupant.getPlace().next();
                        boolean enteringIntersection = (next != null && r.isIntersectionEntry(next));
                        if (enteringIntersection) {
                            // allow entry only if corresponding light is green
                            if ("green".equals(light.getState())) {
                                occupant.move();
                            } else {
                                // wait
                            }
                        } else {
                            occupant.move();
                        }
                    }
                    // if at end (no next), remove car
                    if (occupant.atEnd()) {
                        finishedCars.add(occupant);
                        totalExited++;
                    }
                }
            }
        }

        // remove finished cars from active list
        Iterator<Car> it = cars.iterator();
        while (it.hasNext()) {
            Car c = it.next();
            if (c.atEnd()) it.remove();
        }
    }

    public boolean isRunning() 
    { 
        return running; 
    }

    public void addCar(Car car)
    {
        cars.add(car);
    }

    public List<Car> getAllCars() 
    { 
        return new ArrayList<>(cars); 
    }

    public TrafficLight getTrafficLight(String dir) 
    { 
        return this.trafficLights.get(dir); 
    }

    public String getStats() 
    { 
        return "Time: " + time + "\nTotal created: " + totalCreated + "\nTotal exited: " + totalExited + "\nActive: " + cars.size();
    } 
}

//Place class 
class Place 
{ 
    int row, col; 
    private boolean blocked; 
    private Car occupiedBy;
    private Place nextPlace;

    public Place(int row, int col)
    {
        this.row = row;
        this.col = col;
        this.occupiedBy = null;
        this.blocked = false;
        this.nextPlace = null;
    }

    public boolean freeToMove()
    {
        return this.occupiedBy == null && !this.blocked;
    }

    public Place next()
    {
        return this.nextPlace;
    }

    public void setNext(Place next)
    {
        this.nextPlace = next;
    }

    public void block()
    {
        this.blocked = true;
    }

    public void unblock()
    {
        this.blocked = false;
    }

    public Car getOccupiedBy()
    {
        return this.occupiedBy;
    }

    public void setOccupiedBy(Car car)
    {
        this.occupiedBy = car;
    }
}

//Car class 
class Car 
{ 
    private Place place; 
    private Place lastPlace;
    private javafx.scene.paint.Color color; 
    private String direction; 
    private boolean left;
    public Car(Color color, Place place, String direction)
    {
        this.color = color;
        this.place = place;
        this.direction = direction;
        this.lastPlace = null;
        this.left = false;
        if (place != null) place.setOccupiedBy(this);
    }
    public Place getPlace() 
    { 
        return this.place; 
    } 
    public javafx.scene.paint.Color getColor() 
    { 
        return this.color; 
    } 
    public String getDirection()
    {
        return this.direction;
    }

    public boolean canMove(){
        Place next = (place == null) ? null : place.next();
        return next != null && next.freeToMove();
    }

    public void move()
    {
        if (place == null) return;
        Place next = place.next();
        if (next == null) {
            // reached end, mark left and clear occupancy of current
            if (lastPlace != null) lastPlace.setOccupiedBy(null);
            place.setOccupiedBy(null);
            place = null;
            left = true;
            return;
        }
        // free lastPlace (two-cell car behavior can be extended)
        if (lastPlace != null) {
            lastPlace.setOccupiedBy(null);
        }
        lastPlace = place;
        place = next;
        place.setOccupiedBy(this);
        // mark left if next has no next
        if (place.next() == null) {
            // on next iteration, atEnd will be true after move
        }
    }

    public boolean atEnd()
    {
        return this.left;
    }
}

// Road : a linked list of Places 
class Road 
{
    private final List<Place> places;
    private final String direction;

    // For simplicity we mark one particular place as intersection entry if needed
    private final int intersectionIndex; // index of place that is entry to intersection (approx)

    public Road(String direction, int length, int startRow, int startCol, boolean isHorizontal)
    {
        this.direction = direction;
        this.places = new ArrayList<>();
        Place previous = null;
        for(int i = 0; i < length; i++)
        {
            int row = isHorizontal ? startRow : startRow + i;
            int col = isHorizontal ? startCol + i: startCol;
            Place current = new Place(row, col);
            places.add(current);
            if(previous != null)
            {
                previous.setNext(current);
            }
            previous = current;
        }
        // choose intersection entry as last-but-two for safety
        this.intersectionIndex = Math.max(0, places.size() -3);
    }

    public Place getFirstPlace() 
    {
        return this.places.isEmpty() ? null : this.places.get(0);
    }

    public Place getPlaceAt(int index)
    {
        if (index >=0 && index < places.size())
        {
            return this.places.get(index);
        }
        return null;
    }

    public List<Place> getAllPlaces() {
        return places;
    }

    public String getDirection()
    {
        return this.direction;
    }

    public boolean isIntersectionEntry(Place p) {
        int idx = places.indexOf(p);
        return idx == intersectionIndex;
    }
}

// TrafficLight 
class TrafficLight 
{ 
    private String state; 
    private int greenDuration;
    private int yellowDuration;
    private int redDuration;
    private int timeElapsed = 0;
    public TrafficLight(int greenDuration, int yellowDuration, int redDuration, String state)
    {
        this.state = state;
        this.greenDuration = Math.max(1, greenDuration);
        this.yellowDuration = Math.max(1, yellowDuration);
        this.redDuration = Math.max(1, redDuration);
    }
    public String getState() 
    { 
        return this.state; 
    }
    public void update()
    {
        timeElapsed++;
        if(this.state.equals("green") && this.timeElapsed >= this.greenDuration)
        {
            this.state = "yellow";
            this.timeElapsed = 0;
        } else if(this.state.equals("yellow") && this.timeElapsed >= this.yellowDuration)
        {
            this.state = "red";
            this.timeElapsed = 0;
        } else if(this.state.equals("red") && this.timeElapsed >= this.redDuration)
        {
            this.state = "green";
            this.timeElapsed = 0;
        }

    }
    public void setTimings(int greenTime, int redTime)
    {
        this.greenDuration = greenTime;
        this.redDuration = redTime;
    }
}

// CarQueue : manages arrivals for each road 
class CarQueue 
{
    private Queue<Car> cars;
    private Road road;
    private String direction;

    public CarQueue(Road road, String direction)
    {
        this.cars = new LinkedList<>();
        this.road =  road;
        this.direction = direction;
    }

    public void update(double arrivalProb) 
    {
        // This method is unused in this refactor; Simulator handles spawn by checking road first place
        if (Math.random() < arrivalProb) {
            Place start = road.getFirstPlace();
            if (start != null && start.getOccupiedBy() == null) {
                Car c = new Car(Color.RED, start, direction);
                cars.offer(c);
            }
        }
        // movement handled by simulator
    }

    public void addCar(Car car)
    {
        this.cars.offer(car);
    }
    public Car removeCar()
    {
        return this.cars.poll();
    }
    public int getSize()
    {
        return this.cars.size();
    }
    public List<Car> getCars()
    {
        return new ArrayList<>(cars);
    }
    public Road getRoad() { return this.road; }
    public String getDirection() { return this.direction; }
}


