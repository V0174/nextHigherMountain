package net.v0174.nexthighermountain;

import com.geodesk.feature.Feature;
import com.geodesk.feature.FeatureLibrary;
import com.geodesk.feature.Features;
import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.geotools.referencing.GeodeticCalculator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.stream.StreamSupport;

import static javafx.scene.paint.Color.BLACK;
import static javafx.scene.paint.Color.RED;

public class NextHigherMountain extends Application {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(NextHigherMountain.class);
    private File golFile;
    private FeatureLibrary golLibrary;
    private boolean fileChanged = true;

    public static void main(String[] args) {
        Configurator.setRootLevel(Level.DEBUG);
        launch();
    }

    /**
     * Starts the whole thing.
     *
     * @param stage The GUI stage to create everything on.
     */
    @Override
    public void start(Stage stage) {
        // First print the form window
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        // Library .gol file
        Label libraryLabel = new Label("Knihovna:");
        grid.add(libraryLabel, 0, 0);
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Geodesk knihovny", "*.gol", "*.GOL");
        fileChooser.getExtensionFilters().add(filter);
        final Button fileButton = new Button("Otevři soubor");
        grid.add(fileButton, 1, 0);
        fileButton.setOnAction(e -> {
            golFile = fileChooser.showOpenDialog(stage);
            fileChanged = true;
        });

        // Query
        Label queryLabel = new Label("Dotaz:");
        grid.add(queryLabel, 0, 1);
        TextField queryField = new TextField("n[place=city,town,village,hamlet][name][population>=1000000]");
        queryField.setMinWidth(400);
        grid.add(queryField, 1, 1);

        // Status
        Label statusLabel = new Label("Status:");
        grid.add(statusLabel, 0, 2);
        Label statusText = new Label("Připraven.");
        grid.add(statusText, 1, 2);

        // Run button
        final Button runButton = new Button("Spusť");
        grid.add(runButton, 1, 3);
        runButton.setOnAction(e -> compute(queryField.getText(), statusText));

        stage.setTitle("Nejvyšší bližší hory");
        Scene scene = new Scene(grid, 500, 200);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Computes the nearest higher mountains
     *
     * @param startLocationQuery The query to determine the start locations
     * @param statusLabel        The label with status to be updated
     */
    private void compute(String startLocationQuery, Label statusLabel) {
        if (golFile == null) {
            statusLabel.setText("Musíš vybrat soubor s knihovnou.");
            return;
        }
        if (!golFile.canRead()) {
            statusLabel.setText("Nemohu přečíst soubor: " + golFile.getName());
            return;
        }
        statusLabel.setText("Spouštím dotaz.");

        if (fileChanged) {
            golLibrary = new FeatureLibrary(golFile.getPath());
            fileChanged = false;
        }
        LOGGER.debug("Looking for start points.");
        Features<? extends Feature> starts = findStartLocations(golLibrary, startLocationQuery);
        LOGGER.debug("Looking for peaks.");
        Map<City, List<Mountain>> mountainMap = findPeaks(starts, golLibrary);
        LOGGER.debug("{} peaks found.", mountainMap.size());
        for (Map.Entry<City, List<Mountain>> entry : mountainMap.entrySet()) {
            // Chart
            Stage chartStage = new Stage();
            City place = entry.getKey();
            List<Mountain> mountainList = entry.getValue();
            LOGGER.debug("Creating the dataset for place {}.", place);
            XYDataset dataset = createDataset(mountainList);
            LOGGER.debug("Creating the labels for place {}.", place);
            LegendItemCollection labels = createLabels(mountainList);
            LOGGER.debug("Creating the chart.");
            JFreeChart chart = createChart(dataset, labels, place.name());
            LOGGER.debug("Creating the window contents.");
            ChartViewer chartViewer = new ChartViewer(chart);

            // Map
            MapView mapView = new MapView();
            mapView.setCenter(new MapPoint(place.latitude(), place.longitude()));
            // Quick and dirty estimation
            long zoom = Math.round(19-Math.log(mountainList.get((mountainList.size() - 1) / 2).distance()));
            if (zoom < 0) zoom = 0;
            else if (zoom > 19) zoom = 19;
            LOGGER.debug("Setting zoom to {}.", zoom);
            mapView.setZoom(zoom);
            mapView.addLayer(createPoiLayer(place, mountainList));

            // Tabs
            Tab chartTab = new Tab("Graf", chartViewer);
            Tab mapTab = new Tab("Mapa", mapView);
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            ObservableList<Tab> tabList = tabPane.getTabs();
            tabList.add(chartTab);
            tabList.add(mapTab);

            // Stage (window)
            chartStage.setTitle("Nejbližší vyšší hory z " + place);
            chartStage.setWidth(1024);
            chartStage.setHeight(768);
            chartStage.setScene(new Scene(tabPane));
            chartStage.show();
            LOGGER.info("Next higher mountains from {}:", place);
            for (int i = 0; i < mountainList.size(); i++) {
                Mountain mountain = mountainList.get(i);
                LOGGER.info(
                        "{}. {} ({} m, {} km), {} {}",
                        i + 1,
                        mountain.name(),
                        mountain.elevation(),
                        Math.round(mountain.distance() / 1000),
                        mountain.latitude(),
                        mountain.longitude()
                );
            }
        }
        LOGGER.debug("Done.");
        statusLabel.setText("Hotovo.");
    }

    /**
     * @param start        The starting place.
     * @param mountainList The mountains to be displayed in the new layer.
     * @return a layer with the POIs representing the provided mountains.
     */
    private MapLayer createPoiLayer(City start, List<Mountain> mountainList) {
        PoiLayer layer = new PoiLayer();

        // Start position
        MapPoint startMapPoint = new MapPoint(start.latitude(), start.longitude());
        Rectangle rectangle = new Rectangle(6, 6);
        String nameString = start.name();
        Text startText = new Text(-nameString.length() * 3, -7, nameString);
        Shape startPoint = Shape.union(rectangle, startText);
        startPoint.setFill(BLACK);
        layer.addPoint(startMapPoint, startPoint);

        for (int i = 0; i < mountainList.size(); i++) {
            Mountain mountain = mountainList.get(i);
            MapPoint mapPoint = new MapPoint(mountain.latitude(), mountain.longitude());
            Circle circle = new Circle(3);
            String textString = i + ". " + mountain.name() + " (" + mountain.elevation() + " m, " + Math.round(mountain.distance() / 1000) + " km)";
            Text text = new Text(-textString.length() * 3, -7, textString);
            Shape point = Shape.union(circle, text);
            point.setFill(RED);
            layer.addPoint(mapPoint, point);
        }
        return layer;
    }

    /**
     * Returns a sample dataset.
     *
     * @param mountainList The list of mountains being charted
     * @return The dataset.
     */
    private XYDataset createDataset(List<Mountain> mountainList) {
        DefaultXYDataset dataset = new DefaultXYDataset();
        double[][] data = new double[2][mountainList.size()];
        for (int i = 0; i < mountainList.size(); i++) {
            Mountain m = mountainList.get(i);
            data[0][i] = m.distance() / 1_000;  // meters to kilometers
            data[1][i] = m.elevation();
        }
        dataset.addSeries("Mountains", data);
        return dataset;
    }

    /**
     * Creates a sample chart.
     *
     * @param dataset the dataset.
     * @param labels  The labels for the data points
     * @param place   The name of the place from which we're looking for mountains.
     * @return The chart.
     */
    private JFreeChart createChart(XYDataset dataset, LegendItemCollection labels, String place) {
        JFreeChart chart = ChartFactory.createXYLineChart("Graf pro " + place + " (" + dataset.getItemCount(0) + " vrchů)", "Vzdálenost [km]", "Výška [m]", dataset);
        XYPlot plot = (XYPlot) chart.getPlot();

        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2); // etc.
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);

        renderer.setDefaultItemLabelGenerator(new LegendXYItemLabelGenerator(labels));
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setSeriesShape(0, ShapeUtils.createUpTriangle(5f));
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setDefaultSeriesVisibleInLegend(false);
        plot.setRenderer(renderer);
        return chart;
    }

    /**
     * Creates labels for the chart.
     *
     * @return the collection of labels to be used.
     */
    private LegendItemCollection createLabels(List<Mountain> mountainList) {
        LegendItemCollection lic = new LegendItemCollection();
        for (Mountain mountain : mountainList)
            lic.add(new LegendItem(mountain.name() + " (" + mountain.elevation() + ')'));
        return lic;
    }

    /**
     * Finds the peaks for the given place.
     *
     * @return a list of mountains found per city.
     */
    private Map<City, List<Mountain>> findPeaks(Features<? extends Feature> cities, FeatureLibrary library) {
        GeodeticCalculator gc = new GeodeticCalculator();

        // Find the peaks
        LOGGER.debug("Looking for peaks according to the filter.");
        Features<? extends Feature> mountains = library.select("n[natural=peak,volcano,hill][ele]");
        LOGGER.debug("{} mountains found.", mountains.count());
        if (cities.isEmpty()) System.exit(1);

        Map<City, List<Mountain>> map = new HashMap<>();
        for (Feature city : cities) {
            String cityName = getBestName(city);

            LOGGER.debug("Now computing mountains for place {}.", cityName);
            gc.setStartingGeographicPoint(city.lon(), city.lat());

            // Find the distance of the highest peak since we don't need anything further than that.
            LOGGER.debug("Computing a distance to the highest peak.");
            Feature highestPeak = StreamSupport.stream(mountains.spliterator(), true)
                    .filter(m -> m.intValue("ele") < 9000)  // Remove invalid data over this limit
                    .max(Comparator.comparingInt(m -> m.intValue("ele")))
                    .orElseThrow();
            gc.setDestinationGeographicPoint(highestPeak.lon(), highestPeak.lat());
            double highestPeakDistance = gc.getOrthodromicDistance();

            // Sort the relevant peaks by distance
            LOGGER.debug("Computing distances to mountains for city {} with population {}.", city.stringValue("name"), city.intValue("population"));
            List<Mountain> mountainList = StreamSupport.stream(mountains.spliterator(), true)
                    // Convert to mountains.
                    .map(f -> {
                        gc.setDestinationGeographicPoint(f.lon(), f.lat());
                        String name = getBestName(f);
                        return new Mountain(name, f.intValue("ele"), gc.getOrthodromicDistance(), f.lat(), f.lon());
                    })
                    .filter(m -> (m.distance() - highestPeakDistance < 0.1))
                    .filter(m -> m.elevation() < 9000)  // Remove invalid data over this limit (again)
                    .sorted(Comparator.comparingDouble(Mountain::distance))
                    .sequential()
                    .collect(
                            ArrayList::new,
                            (l, m) -> {
                                if (l.isEmpty() || (l.get(l.size() - 1).elevation() < m.elevation())) l.add(m);
                            },
                            ArrayList::addAll   // Shouldn't happen in a sequential stream.
                    );
            map.put(new City(cityName, city.lat(), city.lon()), mountainList);
            LOGGER.debug("{} mountains stored for displaying.", mountainList.size());
        }
        return map;
    }

    /**
     * Tries to find the name in Czech, then in English and then the default name.
     *
     * @param feat The feature (tagged OSM object) to be searched.
     * @return The best name for this feature.
     */
    private String getBestName(Feature feat) {
        String name = feat.stringValue("name:cs");
        if (name.isBlank()) name = feat.stringValue("name:en");
        if (name.isBlank()) name = feat.stringValue("name");
        if (name.isBlank()) name = "Bezejmenný vrchol";
        return name;
    }

    /**
     * @param library The GeoDesk library to search.
     * @param query   The query to run for places.
     * @return an iterable list of features matching the query in the provided library.
     */
    private Features<? extends Feature> findStartLocations(FeatureLibrary library, String query) {
        // Find the location(s)
        LOGGER.debug("Looking for start locations according to the filter.");
        Features<? extends Feature> cities = library.select(query);
        LOGGER.debug("{} locations found.", cities.count());
        if (cities.isEmpty()) System.exit(2);
        return cities;
    }

}