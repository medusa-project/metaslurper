package edu.illinois.library.metaslurper.entity;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BasicEntityTest {

    private BasicEntity instance;

    @Before
    public void setUp() {
        instance = new BasicEntity();
        instance.setID("id");
        instance.setServiceKey("key");
    }

    /* setAccessImageURI() */

    @Test(expected = IllegalArgumentException.class)
    public void testSetAccessImageURIWithEmptyArgument() {
        instance.setAccessImageURI("");
    }

    @Test
    public void testSetAccessImageURIWithValidArgument() {
        instance.setAccessImageURI("http://example.org/cats");
    }

    /* setID() */

    @Test(expected = IllegalArgumentException.class)
    public void testSetIDWithNullArgument() {
        instance.setID(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetIDWithEmptyArgument() {
        instance.setID("");
    }

    @Test
    public void testSetIDWithValidArgument() {
        instance.setID("cats");
    }

    /* setServiceKey() */

    @Test(expected = IllegalArgumentException.class)
    public void testSetServiceKeyWithNullArgument() {
        instance.setServiceKey(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetServiceKeyWithEmptyArgument() {
        instance.setID("");
    }

    @Test
    public void testSetServiceKeyWithValidArgument() {
        instance.setID("cats");
    }

    /* setSourceURI() */

    @Test(expected = IllegalArgumentException.class)
    public void testSetSourceURIWithEmptyArgument() {
        instance.setSourceURI("");
    }

    @Test
    public void testSetSourceURIWithValidArgument() {
        instance.setSourceURI("http://example.org/cats");
    }

    @Test
    public void testToString() {
        assertEquals("key id", instance.toString());
    }

}
