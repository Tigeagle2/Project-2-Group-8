/**
*
* Author: Caden Douglas
* @due 12/01/2025
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

// Main GUI class
public class TrafficFX extends Application 
{
    private Simulator simulator;
    private Canvas canvas;
    private static final long target = 60;
    private static final long frameTime = 1_000_000_000 / target;
    private long lastUpdate = 0;

    @Override
    public void start(Stage stage) 
    {
        // User inputs
        TextInputDialog dialogNS = new TextInputDialog("30");
        dialogNS.setHeaderText("Green time North/South (seconds):");
        int greenNS = (Integer.parseInt(dialogNS.showAndWait().orElse("30"))) * 60;

        TextInputDialog dialogEW = new TextInputDialog("30");
        dialogEW.setHeaderText("Green time East/West (seconds):");
        int greenEW = (Integer.parseInt(dialogEW.showAndWait().orElse("30"))) * 60;

        TextInputDialog dialogProb = new TextInputDialog("6");
        dialogProb.setHeaderText("Car arrival probability (1/n, e.g., 6):");
        int probN = Integer.parseInt(dialogProb.showAndWait().orElse("6"));

        TextInputDialog dialogDuration = new TextInputDialog("10000");
        dialogDuration.setHeaderText("Simulation duration (timer units):");
        int duration = Integer.parseInt(dialogDuration.showAndWait().orElse("1000"));
        Place place1 = new Place(5, 5);
        Car car1 = new Car(Color.BLUE, place1, "north");
        simulator = new Simulator(greenNS, greenEW, 1.0 / probN, duration);
        simulator.addCar(car1);

        canvas = new Canvas(600, 600);
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, 600, 600);
        stage.setTitle("Traffic Intersection Simulation");
        stage.setScene(scene);
        stage.show();

        // AnimationTimer for simulation updates
        new AnimationTimer() 
        {
            
            @Override
            public void handle(long now) 
            {
                if (now - lastUpdate>= frameTime)
                {
                    
                
                    if (simulator.isRunning()) 
                    {
                        simulator.update();
                        draw();
                    } else 
                    {
                        stop();
                        // Output stats to console
                        System.out.println("Simulation ended. Stats:");
                        System.out.println(simulator.getStats());
                    }
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

        // Draw intersection
        gc.setFill(Color.BLACK);
        gc.fillRect(200, 200, 150, 150);

        // Draw traffic lights (one for each direction)
        drawLight(gc, 350, 135, simulator.getTrafficLight("east").getState()); // North
        drawLight(gc, 350, 355, simulator.getTrafficLight("north").getState());
        drawLight(gc, 180, 135, simulator.getTrafficLight("south").getState()); // North
        drawLight(gc, 180, 355, simulator.getTrafficLight("west").getState());// South
        // Add for east, west similarly, adjusting positions and orientations

        // Draw cars
        for (Car car : simulator.getAllCars()) 
        {
            javafx.geometry.Point2D pos = getCarPosition(car);
            gc.setFill(car.getColor());
            gc.fillRect(pos.getX(), pos.getY(), 20, 10); // Simple car rectangle
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
        gc.setFill("green".equals(state) ? Color.GREEN : Color.DARKGRAY);
        gc.fillOval(x + 2, y + 42, 16, 16);
    }

    private javafx.geometry.Point2D getCarPosition(Car car) 
    {
        // Map Place row/col to pixels based on direction
        Place place = car.getPlace();
        double px = place.col * 20; // PLACE_SIZE = 20
        double py = place.row * 20;
        // Adjust based on direction (e.g., for northbound, invert y)
        return new javafx.geometry.Point2D(px, py);
    }

    public static void main(String[] args) 
    {
        launch(args);
    }
}

// Stub for Simulator (integrate with core)
class Simulator 
{
    // Assume core classes defined here or imported
    private boolean running = true;
    private int time = 0;
    private int duration;
    private double arrivalProb;
    // TrafficLight instances for each direction
    private Map<String, TrafficLight> trafficLights = new HashMap<>();
    private List<Car> cars = new ArrayList<>();
    // Roads, Queues, etc.

    public Simulator(int greenNS, int greenEW, double prob, int dur) 
    {
        this.arrivalProb = prob;
        this.duration = dur;
        TrafficLight nsLight = new TrafficLight(greenNS, 180, greenEW, "green");
        TrafficLight ewLight = new TrafficLight(greenEW, 180, greenNS, "red");
        this.trafficLights.put("north", nsLight);
        this.trafficLights.put("south", nsLight);
        this.trafficLights.put("east", ewLight);
        this.trafficLights.put("west", ewLight);
        
        // Initialize lights, roads, queues
    }

    public void update() 
    {
        this.time++;
        if (this.time > this.duration) 
        {
            this.running = false;
            return;
        }
        // Update lights
        this.trafficLights.get("north").update();
        //this.trafficLights.get("south").update();
        this.trafficLights.get("east").update();
        //this.trafficLights.get("west").update();
        // Update queues and cars
        // Add new cars if Math.random() < arrivalProb
        // Move cars if free
    }

    public boolean isRunning() 
    { 
        return this.running; 
    }
    public void addCar(Car car)
    {
        cars.add(car);
    }
    public List<Car> getAllCars() 
    { 
        return cars; 
    }
    public TrafficLight getTrafficLight(String dir) 
    { 
        return this.trafficLights.get(dir); 
    }
    public String getStats() 
    { 
        return "Cars traversed: " + this.cars.size(); 
    } // Example
}

// Core class stubs (as per project)
class Place 
{ 
    int row, col; 
    private boolean occupied; /* ... */ 
    private Car occupiedBy;
    private Place nextPlace;
    public Place(int row, int col)
    {
        this.row = row;
        this.col = col;
        this.occupiedBy = null;
        this.occupied = false;
        this.nextPlace = null;
    }
    public boolean freeToMove()
    {
        return this.occupiedBy == null && !this.occupied;
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
        this.occupied = true;
    }
    public void unblock()
    {
        this.occupied = false;
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
        this.place.setOccupiedBy(this);
        this.left = false;
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
    }/* move, etc. */ 
    public boolean canMove(){
        Place next = place.next();
        return next!= null && next.freeToMove();
    }
    public void move()
    {
        if (!canMove()) return;
        if(this.lastPlace != null)
        {
            this.lastPlace.setOccupiedBy(null);
            this.lastPlace = this.place;
            this.place = this.place.next();
            this.place.setOccupiedBy(this);
        }
        if(this.place.next() == null)
        {
            this.left = true;
        }    
    }
    public boolean atEnd()
        {
            return this.left;
        }
    
}
class Road 
{ 
    private List<Place> places;
    private String direction;
    public Road(String direction, int length,int startRow, int startCol,boolean isHorizontal)
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
    public String getDirection()
    {
        return this.direction;
    }
}
class TrafficLight 
{ 
    private String state; 
    private int greenDuration;
    private int yellowDuration;
    private int redDuration;
    private int timeElasped = 0;
    public TrafficLight(int greenDuration, int yellowDuration, int redDuration, String state)
    {
        this.state = state;
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        this.redDuration = redDuration;
    }
    public String getState() 
    { 
        return this.state; 
    } /* update */ 
    public void update()
    {
        this.timeElasped++;
        if(this.state.equals("green") && this.timeElasped >= this.greenDuration)
        {
            this.state = "yellow";
            this.timeElasped = 0;
        } else if(this.state.equals("yellow") && this.timeElasped >= this.yellowDuration)
        {
            this.state = "red";
            this.timeElasped = 0;
        } else if(this.state.equals("red") && this.timeElasped >= this.redDuration)
        {
            this.state = "green";
            this.timeElasped = 0;
        }
        
    }
    public void setTimings(int greenTime, int redTime)
    {
        this.greenDuration = greenTime;
        this.redDuration = redTime;
    }
}
class CarQueue 
{ /* queue of cars */ 
    private Queue<Car> cars;
    private Road road;
    private String direction;
    public CarQueue(Road road, String direction)
    {
        this.cars = new LinkedList<>();
        this.road =  road;
        this.direction = direction;
    }
    public void update(double arrivalProb) // add options to add, move, and remove
    {
        if(Math.random() < arrivalProb)
        {
            Place startPlace = road.getFirstPlace();
            if(startPlace != null && startPlace.getOccupiedBy() == null)
            {
                Car newCar = new Car(Color.RED, startPlace, direction);
                cars.offer(newCar);
            }
        }
        if(!cars.isEmpty())
        {
            Car frontCar = cars.peek();
            if(frontCar.getPlace().getOccupiedBy() == frontCar && frontCar.canMove())
            {
                frontCar.move();
            }
        }
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
    
        
}
