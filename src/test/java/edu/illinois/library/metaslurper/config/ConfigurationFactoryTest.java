package edu.illinois.library.metaslurper.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigurationFactoryTest {

    private String vmArgValue;

    @Before
    public void setUp() {
        vmArgValue = System.getProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        ConfigurationFactory.clearInstance();
    }

    @After
    public void tearDown() {
        if (vmArgValue != null) {
            System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, vmArgValue);
        }
    }

    @Test
    public void testGetInstanceWithNonExistentConfigFile() {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "bogus");

        Configuration config = ConfigurationFactory.getConfiguration();
        assertNull(config);
    }

    @Test
    public void testGetInstanceWithValidConfigFile() {

        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "./metaslurper.conf");

        Configuration config = ConfigurationFactory.getConfiguration();
        assertTrue(config instanceof PropertiesConfiguration);
    }

}
