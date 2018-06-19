package edu.illinois.library.metaslurper.config;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigurationFactoryTest {

    @Before
    public void setUp() {
        ConfigurationFactory.clearInstance();
    }

    @Test
    public void testGetInstance() {
        Configuration config = ConfigurationFactory.getConfiguration();
        assertTrue(config instanceof EnvironmentConfiguration);
    }

}
