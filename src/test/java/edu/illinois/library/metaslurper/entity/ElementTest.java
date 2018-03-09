package edu.illinois.library.metaslurper.entity;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ElementTest {

    private Element instance;

    @Before
    public void setUp() {
        instance = new Element("name", "value");
    }

    @Test
    public void testEqualsWithEqualInstance() {
        Element e2 = new Element("name", "value");
        assertTrue(instance.equals(e2));
    }

    @Test
    public void testEqualsWithUnequalNamedInstance() {
        Element e2 = new Element("name2", "value");
        assertFalse(instance.equals(e2));
    }

    @Test
    public void testEqualsWithUnequalValuedInstance() {
        Element e2 = new Element("name", "value2");
        assertFalse(instance.equals(e2));
    }

    @Test
    public void testToString() {
        assertEquals("name: value", instance.toString());
    }

}
