package net.v0174.nexthighermountain;

/**
 * @param name      The name of the mountain, hill or peak.
 * @param elevation The elevation of the mountain, hill or peak.
 * @param distance  The distance of the peak from the starting coordinates.
 * @param latitude       The latitude of the peak
 * @param longitude       The longitude of the peak
 */
public record Mountain(String name, int elevation, double distance, double latitude, double longitude) {
}
