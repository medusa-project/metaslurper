package edu.illinois.library.metaslurper.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ServiceFactory {

    private static Set<SourceService> DEFAULT_SOURCE_SERVICES =
            Collections.unmodifiableSet(
                    new HashSet<>(Arrays.asList(
                            new IDEALSService(),
                            new MedusaDLSService())));
    private static Set<SinkService> DEFAULT_SINK_SERVICES =
            Collections.unmodifiableSet(
                    new HashSet<>(Collections.singletonList(
                            new MetaslurpService())));

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
    public static synchronized  Set<SinkService> allSinkServices() {
        return (sinkServices != null) ? sinkServices : DEFAULT_SINK_SERVICES;
    }

    /**
     * @param name {@link SourceService#getName() Service name}.
     * @return     Service with the given name or {@literal null}.
     */
    public static SourceService getSourceService(String name) {
        name = name.toLowerCase();
        for (SourceService service : allSourceServices()) {
            if (service.getName().toLowerCase().equals(name)) {
                return service;
            }
        }
        return null;
    }

    /**
     * @param name {@link SinkService#getName() Service name}.
     * @return     Service with the given name or {@literal null}.
     */
    public static SinkService getSinkService(String name) {
        name = name.toLowerCase();
        for (SinkService service : allSinkServices()) {
            if (service.getName().toLowerCase().equals(name)) {
                return service;
            }
        }
        return null;
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
