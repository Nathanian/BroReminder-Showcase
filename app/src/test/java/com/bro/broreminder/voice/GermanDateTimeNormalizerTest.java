package com.bro.broreminder.voice;

import org.junit.Test;

import static org.junit.Assert.*;

public class GermanDateTimeNormalizerTest {
    @Test
    public void testUmlautAndSharpSVariants() {
        assertEquals(5, GermanDateTimeNormalizer.parseGermanNumber("fünf", 10));
        assertEquals(30, GermanDateTimeNormalizer.parseGermanNumber("dreißig", 40));
    }

    @Test
    public void testDialectVariantZwo() {
        assertEquals(2, GermanDateTimeNormalizer.parseGermanNumber("zwo", 10));
    }

    @Test
    public void testDigitProperty() {
        for (int i = 0; i <= 99; i++) {
            assertEquals(i, GermanDateTimeNormalizer.parseGermanNumber(Integer.toString(i), 99));
        }
    }
}
