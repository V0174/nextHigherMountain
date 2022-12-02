package net.v0174.nexthighermountain;

import com.geodesk.feature.Feature;
import com.geodesk.feature.FeatureLibrary;
import com.geodesk.feature.Features;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
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
        Features<? extends Feature> cities = findStartLocations(golLibrary, startLocationQuery);
        LOGGER.debug("Looking for peaks.");
        Map<String, List<Mountain>> mountainMap = findPeaks(cities, golLibrary);
        LOGGER.debug("{} peaks found.", mountainMap.size());
        for (Map.Entry<String, List<Mountain>> entry : mountainMap.entrySet()) {
            Stage chartStage = new Stage();
            String place = entry.getKey();
            List<Mountain> mountainList = entry.getValue();
            LOGGER.debug("Creating the dataset for place {}.", place);
            XYDataset dataset = createDataset(mountainList);
            LOGGER.debug("Creating the labels for place {}.", place);
            LegendItemCollection labels = createLabels(mountainList);
            LOGGER.debug("Creating the chart.");
            JFreeChart chart = createChart(dataset, labels, place);
            LOGGER.debug("Creating the window contents.");
            Scene chartScene = new Scene(new ChartViewer(chart));
            chartStage.setTitle("Nejbližší vyšší hory z " + place);
            chartStage.setWidth(850);
            chartStage.setHeight(500);
            chartStage.setScene(chartScene);
            chartStage.show();
        }
        LOGGER.debug("Done.");
        statusLabel.setText("Hotovo.");
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
     * @return a list of mountains found.
     */
    private Map<String, List<Mountain>> findPeaks(Features<? extends Feature> cities, FeatureLibrary library) {
        GeodeticCalculator gc = new GeodeticCalculator();

        // Find the peaks
        LOGGER.debug("Looking for peaks according to the filter.");
        Features<? extends Feature> mountains = library.select("n[natural=peak,volcano,hill][name][ele]");
        LOGGER.debug("{} mountains found.", mountains.count());
        if (cities.isEmpty()) System.exit(1);

        Map<String, List<Mountain>> map = new HashMap<>();
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
                        return new Mountain(name, f.intValue("ele"), gc.getOrthodromicDistance());
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
            map.put(cityName, mountainList);
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