package edu.illinois.library.metaslurper.config;

class EnvironmentConfiguration implements Configuration {

    @Override
    public String getString(String key) {
        return System.getenv(key);
    }

}
