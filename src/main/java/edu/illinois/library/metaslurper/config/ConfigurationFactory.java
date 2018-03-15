package edu.illinois.library.metaslurper.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

public final class ConfigurationFactory {

    public static final String CONFIG_VM_ARGUMENT =
            "edu.illinois.library.metaslurper.config";

    private static Configuration config;

    /**
     * @return Shared application configuration.
     */
    public static synchronized Configuration getConfiguration() {
        if (config == null) {
            try {
                File file = getFile();
                if (!file.exists()) {
                    throw new NoSuchFileException(file.getAbsolutePath());
                }
                config = new Configurations().properties(getFile());
            } catch (ConfigurationException | NoSuchFileException e) {
                System.err.println(e.getMessage());
            }
        }
        return config;
    }

    public static synchronized void clearInstance() {
        config = null;
    }

    private static File getFile() {
        String path = System.getProperty(CONFIG_VM_ARGUMENT);
        if (path != null && !path.isEmpty()) {
            try {
                // expand paths that start with "~"
                path = path.replaceFirst("^~", System.getProperty("user.home"));
                return new File(path).getCanonicalFile();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            throw new IllegalArgumentException(
                    "Missing " + CONFIG_VM_ARGUMENT + " argument");
        }
    }

    private ConfigurationFactory() {}

}
