package edu.illinois.library.metaslurper.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class NumberUtilsTest {

    @Test
    public void testPercent() {
        assertEquals("50.00%", NumberUtils.percent(1, 2));
        assertEquals("33.33%", NumberUtils.percent(1, 3));
    }

}
