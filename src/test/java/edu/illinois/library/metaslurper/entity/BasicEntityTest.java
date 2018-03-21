package edu.illinois.library.metaslurper.entity;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BasicEntityTest {

    private BasicEntity instance;

    @Before
    public void setUp() {
        instance = new BasicEntity();
        instance.setSinkID("id");
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

    /* setServiceKey() */

    @Test(expected = IllegalArgumentException.class)
    public void testSetServiceKeyWithNullArgument() {
        instance.setServiceKey(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetServiceKeyWithEmptyArgument() {
        instance.setSinkID("");
    }

    @Test
    public void testSetServiceKeyWithValidArgument() {
        instance.setSinkID("cats");
    }

    /* setSinkID() */

    @Test(expected = IllegalArgumentException.class)
    public void testSetSinkIDWithNullArgument() {
        instance.setSinkID(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSinkIDWithEmptyArgument() {
        instance.setSinkID("");
    }

    @Test
    public void testSetSinkIDWithValidArgument() {
        instance.setSinkID("cats");
    }

    /* setSourceID() */

    @Test(expected = IllegalArgumentException.class)
    public void testSetSourceIDWithNullArgument() {
        instance.setSourceID(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSourceIDWithEmptyArgument() {
        instance.setSourceID("");
    }

    @Test
    public void testSetSourceIDWithValidArgument() {
        instance.setSourceID("cats");
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
