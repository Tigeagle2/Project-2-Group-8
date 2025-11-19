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

    @Override
    public void start(Stage stage) 
    {
        // User inputs
        TextInputDialog dialogNS = new TextInputDialog("30");
        dialogNS.setHeaderText("Green time North/South (seconds):");
        int greenNS = Integer.parseInt(dialogNS.showAndWait().orElse("30"));

        TextInputDialog dialogEW = new TextInputDialog("30");
        dialogEW.setHeaderText("Green time East/West (seconds):");
        int greenEW = Integer.parseInt(dialogEW.showAndWait().orElse("30"));

        TextInputDialog dialogProb = new TextInputDialog("6");
        dialogProb.setHeaderText("Car arrival probability (1/n, e.g., 6):");
        int probN = Integer.parseInt(dialogProb.showAndWait().orElse("6"));

        TextInputDialog dialogDuration = new TextInputDialog("1000");
        dialogDuration.setHeaderText("Simulation duration (timer units):");
        int duration = Integer.parseInt(dialogDuration.showAndWait().orElse("1000"));

        simulator = new Simulator(greenNS, greenEW, 1.0 / probN, duration);

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
        }.start();
    }

    private void draw() 
    {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw roads (4 lanes crossing at center)
        gc.setFill(Color.GRAY);
        // Horizontal (East-West)
        gc.fillRect(0, 250, canvas.getWidth(), 50); // Bottom inbound
        gc.fillRect(0, 200, canvas.getWidth(), 50); // Top outbound
        // Vertical (North-South)
        gc.fillRect(250, 0, 50, canvas.getHeight()); // Right inbound
        gc.fillRect(200, 0, 50, canvas.getHeight()); // Left outbound

        // Draw intersection
        gc.setFill(Color.BLACK);
        gc.fillRect(200, 200, 150, 150);

        // Draw traffic lights (one for each direction)
        drawLight(gc, 360, 180, simulator.getTrafficLight("north").getState()); // North
        drawLight(gc, 180, 360, simulator.getTrafficLight("south").getState()); // South
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
        arrivalProb = prob;
        duration = dur;
        // Initialize lights, roads, queues
    }

    public void update() 
    {
        time++;
        if (time > duration) running = false;
        // Update lights
        // Update queues and cars
        // Add new cars if Math.random() < arrivalProb
        // Move cars if free
    }

    public boolean isRunning() 
    { 
        return running; 
    }
    public List<Car> getAllCars() 
    { 
        return cars; 
    }
    public TrafficLight getTrafficLight(String dir) 
    { 
        return trafficLights.get(dir); 
    }
    public String getStats() 
    { 
        return "Cars traversed: " + cars.size(); 
    } // Example
}

// Core class stubs (as per project)
class Place 
{ 
    int row, col; 
    boolean occupied; /* ... */ 
}
class Car 
{ 
    Place place; 
    javafx.scene.paint.Color color; 
    String direction; 
    public Place getPlace() 
    { 
        return place; 
    } 
    public javafx.scene.paint.Color getColor() 
    { 
        return color; 
    } /* move, etc. */ 
}
class Road 
{ /* linked Places */ 
    public void getFirstPlace() 
    {
        
    }
    public void getPlaceAt(int index)
    {
        
    }
}
class TrafficLight 
{ 
    String state; 
    public String getState() 
    { 
        return state; 
    } /* update */ 
    public void update()
    {
        
    }
    public void setTimings(int greenNS, int greenEW)
    {
    }
}
class CarQueue 
{ /* queue of cars */ 
    public void update() // add options to add, move, and remove
    {
        
    }
}
