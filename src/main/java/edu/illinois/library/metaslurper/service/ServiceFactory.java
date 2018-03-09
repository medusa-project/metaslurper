package edu.illinois.library.metaslurper.service;

import java.util.HashSet;
import java.util.Set;

public final class ServiceFactory {

    /**
     * @return Set of all known services.
     */
    public static Set<Service> allServices() {
        Set<Service> services = new HashSet<>();
        services.add(new MedusaDLSService());
        return services;
    }

    /**
     * @param name {@link Service#getName() Service name}.
     * @return     Service with the given name or {@literal null}.
     */
    public static Service getService(String name) {
        for (Service service : allServices()) {
            if (service.getName().equals(name)) {
                return service;
            }
        }
        return null;
    }

}
