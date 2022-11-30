package net.v0174.nexthighermountain;

import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.data.xy.XYDataset;

import java.io.Serializable;

/**
 * A label generator showing mountain names and elevations.
 */
public class LegendXYItemLabelGenerator extends StandardXYItemLabelGenerator
        implements XYItemLabelGenerator, Cloneable, PublicCloneable,
        Serializable {
    private final LegendItemCollection legendItems;

    public LegendXYItemLabelGenerator(LegendItemCollection legendItems) {
        super();
        this.legendItems = legendItems;
    }

    @Override
    public String generateLabel(XYDataset dataset, int series, int item) {
        LegendItem legendItem = legendItems.get(item);
        return legendItem.getLabel();
    }
}