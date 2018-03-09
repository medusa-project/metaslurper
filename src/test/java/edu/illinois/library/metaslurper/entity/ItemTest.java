package edu.illinois.library.metaslurper.entity;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ItemTest {

    private Item instance;

    @Before
    public void setUp() {
        instance = new Item("cats");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullArgument() {
        new Item(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithEmptyArgument() {
        new Item("");
    }

    @Test
    public void testToString() {
        assertEquals("cats", instance.toString());
    }

}
