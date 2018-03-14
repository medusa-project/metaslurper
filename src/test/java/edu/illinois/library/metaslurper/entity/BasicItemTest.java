package edu.illinois.library.metaslurper.entity;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class BasicItemTest {

    private BasicItem instance;

    @Before
    public void setUp() {
        instance = new BasicItem();
        instance.setID("id");
        instance.setServiceKey("key");
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

    /* setSourceURI(String) */

    @Test(expected = IllegalArgumentException.class)
    public void testSetSourceURIWithStringWithEmptyArgument() {
        instance.setSourceURI("");
    }

    @Test
    public void testSetSourceURIWithStringWithValidArgument() {
        instance.setSourceURI("http://example.org/cats");
    }

    /* setSourceURI(URI) */

    @Test
    public void testSetSourceURIWithURIWithValidArgument() throws Exception {
        instance.setSourceURI(new URI("http://example.org/cats"));
    }

    @Test
    public void testToString() {
        assertEquals("key id", instance.toString());
    }

}
