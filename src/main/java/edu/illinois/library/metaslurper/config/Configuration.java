package edu.illinois.library.metaslurper.config;

public interface Configuration {

    /**
     * @return Shared application configuration instance.
     */
    static Configuration getInstance() {
        return ConfigurationFactory.getConfiguration();
    }

    /**
     * @return String corresponding to the given key, or {@literal null}.
     */
    String getString(String key);

}
