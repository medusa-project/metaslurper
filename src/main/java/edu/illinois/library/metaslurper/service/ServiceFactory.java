package edu.illinois.library.metaslurper.service;

import java.util.HashSet;
import java.util.Set;

public final class ServiceFactory {

    /**
     * @return Set of all known sink services.
     */
    public static Set<SinkService> allSinkServices() {
        Set<SinkService> services = new HashSet<>();
        services.add(new MetaslurpService());
        return services;
    }

    /**
     * @return Set of all known source services.
     */
    public static Set<SourceService> allSourceServices() {
        Set<SourceService> services = new HashSet<>();
        services.add(new MedusaDLSService());
        return services;
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

}
