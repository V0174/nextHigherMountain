module net.v0174.nexthighermountain {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.jfree.chart.fx;
    requires org.jfree.jfreechart;
    requires java.desktop;
    requires geodesk;
    requires org.geotools.referencing;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;


    opens net.v0174.nexthighermountain to javafx.fxml;
    exports net.v0174.nexthighermountain;
}