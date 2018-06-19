package edu.illinois.library.metaslurper.config;

final class ConfigurationFactory {

    private static Configuration config;

    /**
     * @return Shared application configuration.
     */
    static synchronized Configuration getConfiguration() {
        if (config == null) {
            config = new EnvironmentConfiguration();
        }
        return config;
    }

    static synchronized void clearInstance() {
        config = null;
    }

    private ConfigurationFactory() {}

}
