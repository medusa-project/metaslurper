package edu.illinois.library.metaslurper.service;

import java.util.HashSet;
import java.util.Set;

public final class ServiceFactory {

    /**
     * @return Set of all known services.
     */
    public static Set<SourceService> allServices() {
        Set<SourceService> services = new HashSet<>();
        services.add(new MedusaDLSService());
        return services;
    }

    /**
     * @param name {@link SourceService#getName() Service name}.
     * @return     Service with the given name or {@literal null}.
     */
    public static SourceService getService(String name) {
        for (SourceService service : allServices()) {
            if (service.getName().equals(name)) {
                return service;
            }
        }
        return null;
    }

}
