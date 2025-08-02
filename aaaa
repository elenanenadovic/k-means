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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import com.google.gson.Gson;

import static java.util.concurrent.ForkJoinTask.invokeAll;

public class Main extends Application {

    public static boolean noGuiRun = false;
    static PoiLayer poiLayer = new PoiLayer();

    public static void Test(Location2[] locations) {
        int runsPerTest = 3;
        long runtimeMax = 120_000;


        int numClusters = 30; //20 recommended in description but I put 30
        int numSitesStart = 500;
        int increaseSites = 500;
        int maxSites = 100000;

        System.out.println("Testing by limiting the number of clusters to " + numClusters);
        System.out.println("Number of accumulation sites | Average runtime (ms)");

        for (int sites = numSitesStart; sites <= maxSites; sites += increaseSites) {
            long totalTime = 0;

            for (int run = 0; run < runsPerTest; run++) {
                Location2[] dataset = buildDataset(locations, sites, 11093);
                Location[] points = convertToLocation(dataset);

                long startTime = System.nanoTime();
                kMeansParallel(points, numClusters, 1000, null);
                long endTime = System.nanoTime();

                totalTime += (endTime - startTime) / 1_000_000;
            }

            long avgTime = totalTime / runsPerTest;
            System.out.println(sites +  " | " + avgTime);

            if (avgTime > runtimeMax) {
                System.out.println("Runtime too long");
                break;
            }
        }


        int accumulationSites = 30000;
        int clustersStart = 5;
        int increaseClusters = 5;
        int maxClusters = accumulationSites / 3;  // e.g., 10000

        System.out.println("\n \n Testing by limiting the number of accumulation sites ");
        System.out.println("Number of accumulation sites |  AverageRuntime(ms)");

        Location2[] fixedDataset = buildDataset(locations, accumulationSites, 11093);
        Location[] fixedPoints = convertToLocation(fixedDataset);

        for (int clusters = clustersStart; clusters <= maxClusters; clusters += increaseClusters) {
            long totalTime = 0;
            for (int run = 0; run < runsPerTest; run++) {
                long startTime = System.nanoTime();
                kMeansParallel(fixedPoints, clusters, 1000, null);  // Run for 1000 cycles or convergence
                long endTime = System.nanoTime();

                totalTime += (endTime - startTime) / 1_000_000;  // ms
            }
            long avgTime = totalTime / runsPerTest;
            System.out.println(accumulationSites + "," + clusters + "," + avgTime);

            if (avgTime > runtimeMax) {
                System.out.println("Exceeded runtime threshold. Stopping.");
                break;
            }
        }
    }


    static class PartialResult {
        double[] sumLat, sumLon;
        int[] count;

        PartialResult(int k) {
            sumLat = new double[k];
            sumLon = new double[k];
            count = new int[k];
        }

        void merge(PartialResult other) {
            for (int i = 0; i < count.length; i++) {
                sumLat[i] += other.sumLat[i];
                sumLon[i] += other.sumLon[i];
                count[i] += other.count[i];
            }
        }
    }

    static final class SumTask extends RecursiveTask<PartialResult> {
        private static final int LIMIT = 2000;
        private final Location[] locations;
        private final int[] labels;
        private final int start;
        private final int end;
        private final int k;

        SumTask(Location[] locations, int[] labels, int start, int end, int k) {
            this.locations = locations;
            this.labels = labels;
            this.start = start;
            this.end = end;
            this.k = k;
        }

        @Override
        protected PartialResult compute() {
            if (end - start <= LIMIT) {
                PartialResult res = new PartialResult(k);
                for (int i = start; i < end; i++) {
                    int c = labels[i];
                    res.sumLat[c] += locations[i].la;
                    res.sumLon[c] += locations[i].lo;
                    res.count[c]++;
                }
                return res;
            } else {
                int mid = (start + end) / 2;
                SumTask left = new SumTask(locations, labels, start, mid, k);
                SumTask right = new SumTask(locations, labels, mid, end, k);
                invokeAll(left, right);
                PartialResult res = left.join();
                res.merge(right.join());
                return res;
            }
        }
    }



    static final class kmeans extends RecursiveAction {
        private static final int limit = 2_000;
        private final Location[] locations;
        private final Location[] centroids;
        private final int[] labels;
        private final int start;
        private final int end;

        kmeans(Location[] locations, Location[] centroids, int[] labels, int start, int end) {
            this.locations = locations;
            this.centroids = centroids;
            this.labels = labels;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            int size = end - start;
            if (size <= limit) {
                for (int i = start; i < end; i++) {
                    Location l = locations[i];
                    int best = -1;
                    double distance = Double.MAX_VALUE;

                    for (int c = 0; c < centroids.length; c++) {
                        double d = Main.distance(l, centroids[c]);
                        if (d < distance)
                            {
                                distance = d;
                                best = c;
                            }
                    }
                    labels[i] = best;
                }
            } else {
                int mid = start + size / 2;
                invokeAll(
                        new kmeans(locations, centroids, labels, start, mid),
                        new kmeans(locations, centroids, labels, mid, end)
                );
            }
        }
    }


    public static Location[] kMeansParallel(Location[] locations, int k, int maxCycles, MapView mapView) {
        if (locations.length == 0) throw new IllegalArgumentException("ne radi gson");
        final int n = locations.length;
        final Random rnd = new Random();

        //random k centroids at the start
        Location[] centroids = new Location[k];
        for (int i = 0; i < k; i++) {
            double la = 47.0 + (55.0 - 47.0) * rnd.nextDouble();
            double lo = 5.0 + (15.0 - 5.0) * rnd.nextDouble();
            centroids[i] = new Location("Centroid " + (i + 1), 0, la, lo, generateRandomColor());
        }

        //which cluster is a centroid in
        int[] labels = new int[n];
        boolean changed;
        int cycles = 0;

        ForkJoinPool pool = ForkJoinPool.commonPool();

        //loops until no one moves or i set max cycles as a limit
        do {
            changed = false;

            pool.invoke(new kmeans(locations, centroids, labels, 0, n));

            double[] sumLat = new double[k];
            double[] sumLon = new double[k];
            int[] count = new int[k];

            /*
            for (int i = 0; i < n; i++) {
                int c = labels[i];
                sumLat[c] += locations[i].la;
                sumLon[c] += locations[i].lo;
                count[c]++;
            }
            */
            PartialResult result = pool.invoke(new SumTask(locations, labels, 0, n, k));

             sumLat = result.sumLat;
             sumLon = result.sumLon;
             count = result.count;

            for (int c = 0; c < k; c++) {
                if (count[c] == 0) {
                    // Empty cluster handling (random respawn)
                } else {
                    double la = sumLat[c] / count[c];
                    double lo = sumLon[c] / count[c];
                    // Update centroid c to (la, lo)
                }
            }

            Location[] newCentroids = new Location[k];
            for (int c = 0; c < k; c++) {
                if (count[c] == 0) {

                    double la = 47.0 + (55.0 - 47.0) * rnd.nextDouble();
                    double lo = 5.0 + (15.0 - 5.0) * rnd.nextDouble();
                    newCentroids[c] = new Location("Centroid " + (c + 1), 0, la, lo, generateRandomColor());
                    changed = true;
                } else {
                    double la = sumLat[c] / count[c];
                    double lo = sumLon[c] / count[c];

                    Color color = centroids[c].color;
                    newCentroids[c] = new Location("Centroid " + (c + 1), 0, la, lo, color);

                    if (Math.abs(la - centroids[c].la) > 0.0001 || Math.abs(lo - centroids[c].lo) > 0.0001) {
                        changed = true;
                    }
                }
            }

            centroids = newCentroids;
            cycles++;

        } while (changed && cycles < maxCycles);

        System.out.println("Number of cycles (parallel): " + cycles);
        if (cycles == maxCycles) {
            System.out.println("Reached maximum cycles (" + maxCycles + ")");
        }

        pool.invoke(new kmeans(locations, centroids, labels, 0, n));

        if (!noGuiRun) {
            for (int i = 0; i < n; i++) {
                locations[i].color = centroids[labels[i]].color;
            }
            Location[] finalCentroids = centroids;
            javafx.application.Platform.runLater(() -> drawing(locations, finalCentroids, mapView));
        }

        return centroids;
    }

    // HELPER METHOD: when over ~ 11000 we need more locations, connecting first 11000 and additional array
    static Location2[] buildDataset(Location2[] locations, int accumulationNum, double maxCapacity) {
        if (accumulationNum > 11093) {
            Location2[] veciniz = new Location2[accumulationNum];

            //copy first 11000 and just add new
            for (int i = 0; i < 11093; i++) {
                veciniz[i] = locations[i];
            }

            Location2[] additionalLocations = generateSites(accumulationNum - 11093, maxCapacity);
            for (int i = 0; i < additionalLocations.length; i++) {
                veciniz[i + 11093] = additionalLocations[i];
            }

            return veciniz;
        } if (accumulationNum == locations.length) {
            //System.out.println("NJAAAAAAAAAA");
            return locations;
        } else {
            //System.out.println("ovde" + locations.length + "e" + accumulationNum);
            return Arrays.copyOf(locations, accumulationNum);
        }
    }

    //calls the main function
    public static void getLocations(MapView view, int numAccumulation, int numClusters, Location2[] locations2){

        Location[] locations = new Location[numAccumulation];
        locations = convertToLocation(locations2);

        Location[] newLocations = new Location[numClusters];
        long startTime = System.nanoTime();
        newLocations = kMeansParallel(locations, numClusters, 1000, view);
        long endTime = System.nanoTime();

        long duration = (endTime - startTime) / 1_000_000;
        System.out.println("Execution time: " + duration + " ms");
    }

    //HELPER METHOD: creates an array of additional locations in europe
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

            additionalLocations[i] = new Location2("Random point" + (i + 1), randomCapacity, randomLatitude, randomLongitude);
        }
        return additionalLocations;
    }

    // HELPER METHOD: coverting locations2 to locations with color
    public static Location[] convertToLocation(Location2[] locations2) {
        Location[] locations = new Location[locations2.length];
        for (int i = 0; i < locations2.length; i++) {
            locations[i] = new Location(
                    locations2[i].name,
                    locations2[i].capacity,
                    locations2[i].la,
                    locations2[i].lo,
                    generateRandomColor()
            );
        }
        return locations;
    }

    //HELPER METHOD: reading locations
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

    //HELPER METHOD: finding max capacity to generate additional points
    public static double maxCapacity(Location2[] shorterLocations){
        double maxCapacity = Double.MIN_VALUE;
        for(int i = 0; i < shorterLocations.length; i++){
            if(shorterLocations[i].capacity > maxCapacity){
                maxCapacity = shorterLocations[i].capacity;
            }
        }
        return maxCapacity;
    }

    //HELPER METHOD: drawing locations and centroids
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

    //HELPER METHOD: getting distance between two locations
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

    //HELPER METHOD: getting a random color for nodes
    public static Color generateRandomColor() {
        Random rand = new Random();
        double r = rand.nextDouble();
        double g = rand.nextDouble();
        double b = rand.nextDouble();
        return new Color(r, g, b, 1.0);
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
            veciniz = buildDataset(locations, numAccumulation, 11093);
            getLocations(view, numAccumulation, numClusters, veciniz);
        });
    }


    public static void main(String[] args) throws IOException {
        noGuiRun = Boolean.parseBoolean(System.getProperty("nogui", "false"));
        boolean runBench = Boolean.parseBoolean(System.getProperty("bench", "false"));
        Location2[] locations = gson();

        if (runBench) {
            noGuiRun = true;
            System.out.println("TEST MODE:");
            Test(locations);
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
