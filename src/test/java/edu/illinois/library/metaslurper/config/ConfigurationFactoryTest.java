package edu.illinois.library.metaslurper.config;

import edu.illinois.library.metaslurper.Application;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigurationFactoryTest {

    @Before
    public void setUp() {
        ConfigurationFactory.clearInstance();
    }

    @Test
    public void testGetInstanceWithNonExistentConfigFile() {
        System.setProperty(Application.CONFIG_VM_ARGUMENT, "bogus");

        Configuration config = ConfigurationFactory.getConfiguration();
        assertNull(config);
    }

    @Test
    public void testGetInstanceWithValidConfigFile() {
        System.setProperty(Application.CONFIG_VM_ARGUMENT, "./metaslurper.conf.sample");

        Configuration config = ConfigurationFactory.getConfiguration();
        assertTrue(config instanceof PropertiesConfiguration);
    }

}
