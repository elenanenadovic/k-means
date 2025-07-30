package org.example;
import com.gluonhq.maps.MapView;
import com.gluonhq.maps.MapPoint;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import java.io.*;
import java.util.*;
import com.google.gson.Gson;

public class Main extends Application {

    public static boolean noGuiRun = false;
    static PoiLayer poiLayer = new PoiLayer();

    public static Color generateRandomColor() {
        Random rand = new Random();
        double r = rand.nextDouble();
        double g = rand.nextDouble();
        double b = rand.nextDouble();
        return new Color(r, g, b, 1.0);
    }

    public static Location[] kMeans(Location[] locations, int numClusters, MapView mapView) {

        int numberOfCycles = 0;
        boolean changed;
        Location[][] clusters;

        if (locations.length == 0) {
            throw new IllegalArgumentException("No locations given");
        }


        //znaci samo pravimo random tacke centroide
        Random random = new Random();
        Location[] centroids = new Location[numClusters];
        for (int i = 0; i < numClusters; i++) {
            double latitude = 47.0 + (55.0 - 47.0) * random.nextDouble();
            double longitude = 5.0 + (15.0 - 5.0) * random.nextDouble();
            Color color = generateRandomColor();
            centroids[i] = new Location("Centroid " + (i + 1), 0, latitude, longitude, color);
        }

        do {
            changed = false;

            //each cluster can have all
            clusters = new Location[numClusters][locations.length];
            int[] clusterSizes = new int[numClusters];

            //assignment
            for (int i = 0; i < locations.length; i++) {
                int closesCentroid = -1;
                double minDistance = Double.MAX_VALUE;

                for (int j = 0; j < numClusters; j++) {
                    double distance = distance(locations[i], centroids[j]);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closesCentroid = j;
                    }
                }

                int newLocationIndex = clusterSizes[closesCentroid]++;
                clusters[closesCentroid][newLocationIndex] = locations[i];

                //just recolloring
                if (locations[i].color != centroids[closesCentroid].color) {
                    locations[i].color = centroids[closesCentroid].color;
                    changed = true;
                }
            }

            // calculations for new centroid locations
            Location[] newCentroids = new Location[numClusters];

            for (int i = 0; i < numClusters; i++) {
                if (clusterSizes[i] == 0) {
                    // u slucaju da je empty spawn random
                    double latitude = 47 + (55 - 47) * random.nextDouble();
                    double longitude = 5 + (15 - 5) * random.nextDouble();
                    Color color = generateRandomColor();
                    newCentroids[i] = new Location("Centroid " + (i + 1), 0, latitude, longitude, color);
                    continue;
                }

                double avgLat = 0;
                double avgLon = 0;

                for (int j = 0; j < clusterSizes[i]; j++) {
                    avgLat += clusters[i][j].la;
                    avgLon += clusters[i][j].lo;
                }
                avgLat /= clusterSizes[i];
                avgLon /= clusterSizes[i];
                Color color = centroids[i].color;

                newCentroids[i] = new Location("Centroid", 0, avgLat, avgLon, color);
            }


            if (centroidsMatch(centroids, newCentroids) == false) {
                centroids = newCentroids;
                changed = true;
            }

            numberOfCycles++;

        } while (changed);


        System.out.println("Number of cycles passed: " + numberOfCycles);

        /*
        for(int i = 0; i < numClusters; i++) {
            System.out.println("Cluster " + i + ": " + "la " + centroids[i].la + ", lo " + centroids[i].lo);
        }
    */
        if(noGuiRun == false){
            drawing(locations, centroids, mapView);
        }
        /*
        else{
            for(int i = 0; i < numClusters; i++){
                System.out.println("Centroid: " + i +1  + ": " + centroids[i].la + ", lo " + centroids[i].lo);
            }
        }
`*/
        return centroids;
    }


    public static boolean centroidsMatch(Location[] oldCentroids, Location[] newCentroids) {
        for (int i = 0; i < oldCentroids.length; i++) {
            if (Math.abs(oldCentroids[i].la - newCentroids[i].la) > 0.0001 || Math.abs(oldCentroids[i].lo - newCentroids[i].lo) > 0.0001) {
                return false;
            }
        }
        return true;
    }


    public static Location2[] generateSites(int numSites, double maxCapacity){

        Location2[] additionalLocations = new Location2[numSites];

        //it is not precise but they spwan in europe and not in the sea
        double la1 = 45;
        double lo1 = 5.5;
        double la2 = 53;
        double lo2 = 25;

        Random random = new Random();

        for (int i = 0; i < numSites; i++) {
            double randomLatitude = la1 + (la2 - la1) * random.nextDouble();
            double randomLongitude = lo1 + (lo2 - lo1) * random.nextDouble();
            double randomCapacity = random.nextDouble() * maxCapacity;

            additionalLocations[i] = new Location2("Random acc. point" + (i + 1), randomCapacity, randomLatitude, randomLongitude);
            //System.out.println(additionalLocations[i]);
        }
        return additionalLocations;
    }

    public static double maxCapacity(Location2[] shorterLocations){
        double maxCapacity = Double.MIN_VALUE;
        for(int i = 0; i < shorterLocations.length; i++){
            if(shorterLocations[i].capacity > maxCapacity){
                maxCapacity = shorterLocations[i].capacity;
            }
        }
        return maxCapacity;
    }

    public static void drawing(Location[] locations, Location[] centroids, MapView mapView) {

        mapView.removeLayer(poiLayer);
        poiLayer = new PoiLayer();
        for(int i = 0; i < centroids.length; i++){
            MapPoint cityLocation = new MapPoint(centroids[i].la, centroids[i].lo);
            Node cityIcon = new Circle(5, centroids[i].color);
            poiLayer.addPoint(cityLocation, cityIcon);
        }

        for(int i = 0; i < locations.length; i++){
            MapPoint cityLocation = new MapPoint(locations[i].la, locations[i].lo);
            Node cityIcon = new Circle(1, locations[i].color);
            poiLayer.addPoint(cityLocation, cityIcon);
        }

        mapView.addLayer(poiLayer);
    }

    static double distance(Location location1, Location location2) {
        int earthRadius = 6371;

        double lat1 = Math.toRadians(location1.la);
        double lat2 = Math.toRadians(location2.la);
        double lon1 = Math.toRadians(location1.lo);
        double lon2 = Math.toRadians(location2.lo);

        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    public static void getLocations(MapView view, int numAccumulation, int numClusters, Location2[] locations2){

        Location[] locations = new Location[numAccumulation];

        //adding color because locations2 do not have it
        for (int i = 0; i < numAccumulation; i++) {

            String name = locations2[i].name;
            double la = locations2[i].la;
            double lo = locations2[i].lo;
            double capacity = locations2[i].capacity;
            Color color = generateRandomColor();

            locations[i] = new Location(name, capacity, la, lo, color);
        }

        Location[] newLocations = new Location[numClusters];
        long startTime = System.nanoTime();
        newLocations = kMeans(locations, numClusters, view);
        long endTime = System.nanoTime();

        long duration = (endTime - startTime) / 1_000_000;
        System.out.println("Execution time: " + duration + " ms");
    }


    public static Location2[] gson() throws IOException {
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("cities.json");
        if (inputStream == null) {
            throw new FileNotFoundException("cities.json ne postoji");
        }
        InputStreamReader reader = new InputStreamReader(inputStream);
        Gson gson = new Gson();
        Location2[] data = gson.fromJson(reader, Location2[].class);
        reader.close();

        return data;
    }

    static Location2[] buildDataset(Location2[] locations, int accumulationNum, double maxCapacity) {
        if (accumulationNum > 11093) {
            Location2[] veciniz = new Location2[accumulationNum];

            for (int i = 0; i < 11093; i++) {
                veciniz[i] = locations[i];
            }

            Location2[] additionalLocations = generateSites(accumulationNum - 11093, maxCapacity);
            for (int i = 0; i < additionalLocations.length; i++) {
                veciniz[i + 11093] = additionalLocations[i];
            }

            return veciniz;
        } else {
            return Arrays.copyOf(locations, accumulationNum);
        }
    }

    // --- paste these inside class Main, after buildDataset(...) and before start(...) ---

    // Runs one k-means job and returns runtime in milliseconds (no GUI work).
    static long runOnce(Location2[] base, int numAccumulation, int numClusters) {
        double maxCap = maxCapacity(base);                    // max capacity from dataset
        Location2[] veciniz = buildDataset(base, numAccumulation, maxCap); // build set

        // Convert to Location[] (like in getLocations)
        Location[] pts = new Location[numAccumulation];
        for (int i = 0; i < numAccumulation; i++) {
            Location2 s = veciniz[i];
            pts[i] = new Location(s.name, s.capacity, s.la, s.lo, generateRandomColor());
        }

        MapView dummy = new MapView(); // drawing will be skipped when noGuiRun == true

        long t0 = System.nanoTime();
        kMeans(pts, numClusters, dummy);
        long t1 = System.nanoTime();

        return (t1 - t0) / 1_000_000L; // ms
    }

    // Averages runtime over `repeats` runs.
    static long averageMillis(Location2[] base, int numAccumulation, int numClusters, int repeats) {
        long sum = 0;
        for (int r = 0; r < repeats; r++) {
            sum += runOnce(base, numAccumulation, numClusters);
        }
        return Math.round(sum / (double) repeats);
    }

    // TEST A: limit clusters (fix clusters; grow accumulation by 500)
    static void benchmark_LimitClusters(Location2[] base) {
        final int fixedClusters = 20;
        final int repeats = 3;
        final int startAccum = 500;
        final int step = 500;
        final long stopThresholdMs = 2L * 60L * 1000L; // ~2 minutes

        System.out.println("config,accumulation,clusters,avg_ms");
        for (int accum = startAccum, cfg = 1; ; accum += step, cfg++) {
            long avg = averageMillis(base, accum, fixedClusters, repeats);
            System.out.println(cfg + "," + accum + "," + fixedClusters + "," + avg);
            if (avg >= stopThresholdMs) break;
        }
    }

    // TEST B: limit accumulation (fix accumulation; grow clusters by 5)
    static void benchmark_LimitAccum(Location2[] base) {
        final int accumulation = 30_000;
        final int startClusters = 5;
        final int step = 5;
        final int repeats = 3;
        final int clusterCap = accumulation / 3;
        final long stopThresholdMs = 2L * 60L * 1000L; // ~2 minutes

        System.out.println("config,accumulation,clusters,avg_ms");
        int cfg = 1;
        for (int k = startClusters; k <= clusterCap; k += step, cfg++) {
            long avg = averageMillis(base, accumulation, k, repeats);
            System.out.println(cfg + "," + accumulation + "," + k + "," + avg);
            if (avg >= stopThresholdMs) break;
        }
    }


    @Override
    public void start(Stage stage) throws IOException {

        Location2[] locations = gson();

        MapView view = new MapView();
        MapPoint center = new MapPoint(51, 11);
        view.setZoom(6);
        view.setCenter(center);
        StackPane sp = new StackPane() {
            @Override
            protected void layoutChildren() {
                super.layoutChildren();
            }
        };
        TextField textField = new TextField();
        textField.setPromptText("No. accumulation sites");
        textField.setPrefWidth(100);
        TextField textField2 = new TextField();
        textField2.setPromptText("No. clusters");
        textField.setPrefWidth(100);
        Button button = new Button("START");
        button.setPrefWidth(100);
        button.setPrefHeight(40);
        HBox hbox = new HBox(20);
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(20, 0, 20, 0));
        hbox.setPrefWidth(Double.MAX_VALUE);
        hbox.getChildren().addAll(textField, textField2, button);
        sp.getChildren().addAll(view);
        StackPane.setMargin(view, new Insets(20, 20, 20, 20));
        Rectangle clip = new Rectangle();
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        clip.widthProperty().bind(view.widthProperty());
        clip.heightProperty().bind(view.heightProperty());
        view.setClip(clip);
        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(hbox, sp);

        Scene scene;
        scene = new Scene(vbox, 800, 600);
        stage.setTitle("K-means clustering");
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        stage.setScene(scene);
        stage.show();

        button.setOnAction(event -> {
            button.setText("REDO");
            int numAccumulation = Integer.parseInt(textField.getText());
            int numClusters = Integer.parseInt(textField2.getText());

            if (numClusters > numAccumulation){
                numClusters = 1;
            }


            Location2[] veciniz = new Location2[numAccumulation];

            double maxCapacity = maxCapacity(locations);
            System.out.println("Max capacity: " + maxCapacity);

            /*
            if (numAccumulation > 11093) {
                Location2[] additionalLocations = generateSites(numAccumulation-11093,maxCapacity );
                for(int i = 0; i < 11093; i++){
                    veciniz[i] = locations[i];
                }
                for(int i = 0; i < additionalLocations.length; i++){
                    veciniz[i+11093] = additionalLocations[i];
                }
            }
            else{
                veciniz = Arrays.copyOf(locations, numAccumulation);
            }
            */
            veciniz = buildDataset(locations, numAccumulation, 11093);


            //System.out.println("LENGTH:" + locations.length);  ima u JSONU 11093 lokacija
            getLocations(view, numAccumulation, numClusters, veciniz);
        });
    }





    public static void main(String[] args) throws IOException {
         noGuiRun = Boolean.parseBoolean(System.getProperty("nogui", "false"));

        if (Boolean.parseBoolean(System.getProperty("bench", "false"))) {
            noGuiRun = true;
            Location2[] base = gson();
            benchmark_LimitClusters(base);
            benchmark_LimitAccum(base);
            return;
        }

        if (noGuiRun) {
            noGuiRun();
        } else {
            Application.launch(args);
        }
    }

    private static void noGuiRun() throws IOException {
        Location2[] locations = gson();

        System.out.println("WITHOUT GUI CALCULATION");

        Scanner scanner = new Scanner(System.in);

        System.out.print("Please enter the number of accumulation sites: ");
        int accumulationNum = scanner.nextInt();
        System.out.print("Please enter the number of clusters: ");
        int clustersNum = scanner.nextInt();

        if (clustersNum > accumulationNum){
            clustersNum = 1;
        }

        Location2[] veciniz = new Location2[accumulationNum];

        double maxCapacity = maxCapacity(locations);

        //ima u JSONU 11093 lokacija
        //there are 11093 locations in JSOn germany file

        //
        if (accumulationNum > 11093) {
            Location2[] additionalLocations = generateSites(accumulationNum - 11093, maxCapacity );
            for(int i = 0; i < 11093; i++){
                veciniz[i] = locations[i];
            }
            for(int i = 0; i < additionalLocations.length; i++){
                veciniz[i+11093] = additionalLocations[i];
            }
        }
        else{
            veciniz = Arrays.copyOf(locations, accumulationNum);
        }
        //
        veciniz = buildDataset(locations, accumulationNum, 11093);


        //just so i don't pass the null but nothing will be drawn in NoGui mode
        MapView view = new MapView();
        getLocations(view, accumulationNum, clustersNum, veciniz);

        scanner.close();

    }

}
