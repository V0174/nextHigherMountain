package net.v0174.nexthighermountain;

import org.junit.jupiter.api.Test;

public class NextHigherMountainTest {

    @Test
    public void testOnePlace() {
        NextHigherMountain.main(new String[]{
                "T:\\Dokumenty\\Mapy\\Geodesk\\Czechia.gol",
                "n[ref:cobe=023515]"
        });
    }

    @Test
    public void testMultiplePlaces() {
        NextHigherMountain.main(new String[]{
                "T:\\Dokumenty\\Mapy\\Geodesk\\Czechia.gol",
                "n[place=city,town,village,hamlet][name][population>=100000]"
        });
    }
}