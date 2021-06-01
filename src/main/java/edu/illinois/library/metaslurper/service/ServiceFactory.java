package edu.illinois.library.metaslurper.service;

import java.util.Set;

public final class ServiceFactory {

    private static final Set<SourceService> DEFAULT_SOURCE_SERVICES = Set.of(
            new BookTrackerService(),
            new IDEALSService(),
            new IDNCService(),
            new IllinoisDataBankService(),
            new MedusaDLSService(),
            new TestSourceService());
    private static final Set<SinkService> DEFAULT_SINK_SERVICES = Set.of(
            new MetaslurpService(),
            new TestSinkService());

    private static Set<SourceService> sourceServices;
    private static Set<SinkService> sinkServices;

    /**
     * @return Set of all known source services.
     */
    public static synchronized Set<SourceService> allSourceServices() {
        return (sourceServices != null) ?
                sourceServices : DEFAULT_SOURCE_SERVICES;
    }

    /**
     * @return Set of all known sink services.
     */
    public static synchronized Set<SinkService> allSinkServices() {
        return (sinkServices != null) ? sinkServices : DEFAULT_SINK_SERVICES;
    }

    /**
     * @param key {@link SourceService#getKey() Service key}.
     * @return    Service with the given key, or {@literal null}.
     */
    public static SourceService getSourceService(String key) {
        return allSourceServices()
                .stream()
                .filter(s -> s.getKey() != null)
                .filter(s -> s.getKey().equalsIgnoreCase(key))
                .findAny()
                .orElse(null);
    }

    /**
     * @param key {@link SinkService#getKey() Service key}.
     * @return    Service with the given key, or {@literal null}.
     */
    public static SinkService getSinkService(String key) {
        return allSinkServices()
                .stream()
                .filter(s -> s.getKey() != null)
                .filter(s -> s.getKey().equalsIgnoreCase(key))
                .findAny()
                .orElse(null);
    }

    /**
     * For testing only.
     *
     * @param services Set of source services. Supply {@literal null} to use
     *                 the default source services.
     */
    public static synchronized void setSourceServices(Set<SourceService> services) {
        sourceServices = services;
    }

    /**
     * For testing only.
     *
     * @param services Set of sink services. Supply {@literal null} to use the
     *                 default sink services.
     */
    public static synchronized void setSinkServices(Set<SinkService> services) {
        sinkServices = services;
    }

}
