package net.v0174.nexthighermountain;

import com.geodesk.feature.Feature;
import com.geodesk.feature.FeatureLibrary;
import com.geodesk.feature.Features;
import javafx.application.Application;
import javafx.scene.Scene;
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
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.stream.StreamSupport;

public class NextHigherMountain extends Application {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(NextHigherMountain.class);

    public static void main(String[] args) {
        Configurator.setRootLevel(Level.DEBUG);
        launch(args);
    }

    /**
     * Starts the whole thing.
     *
     * @param stage The GUI stage to create everything on.
     */
    @Override
    public void start(Stage stage) {
        List<String> params = getParameters().getRaw();
        if (params.size() < 2) throw new IllegalStateException("No parameters provided.");
        String path = params.get(0);
        String query = params.get(1);

        FeatureLibrary library = new FeatureLibrary(path);
        LOGGER.debug("Looking for start points.");
        Features<? extends Feature> cities = findStartLocations(library, query);
        LOGGER.debug("Looking for peaks.");
        Map<String, List<Mountain>> mountainMap = findPeaks(cities, library);
        LOGGER.debug("{} peaks found.", mountainMap.size());
        for (Map.Entry<String, List<Mountain>> entry : mountainMap.entrySet()) {
            Stage newStage = new Stage();
            String place = entry.getKey();
            List<Mountain> mountainList = entry.getValue();
            LOGGER.debug("Creating the dataset for place {}.", place);
            XYDataset dataset = createDataset(mountainList);
            LOGGER.debug("Creating the labels for place {}.", place);
            LegendItemCollection labels = createLabels(mountainList);
            LOGGER.debug("Creating the chart.");
            JFreeChart chart = createChart(dataset, labels, place);
            LOGGER.debug("Creating the window contents.");
            Scene scene = new Scene(new ChartViewer(chart));
            newStage.setTitle("The Next Higher Mountain from " + place);
            newStage.setWidth(800);
            newStage.setHeight(600);
            newStage.setScene(scene);
            newStage.show();
        }
        LOGGER.debug("Done.");
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
            data[0][i] = m.distance();
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
        JFreeChart chart = ChartFactory.createXYLineChart("Graf pro " + place, "Vzdálenost", "Výška", dataset);
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