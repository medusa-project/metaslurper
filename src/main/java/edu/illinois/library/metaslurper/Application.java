package edu.illinois.library.metaslurper;

import edu.illinois.library.metaslurper.service.Service;
import edu.illinois.library.metaslurper.service.ServiceFactory;
import edu.illinois.library.metaslurper.slurp.SlurpResult;
import edu.illinois.library.metaslurper.slurp.Slurper;

import java.util.stream.Collectors;

public final class Application {

    public static final String CONFIG_VM_ARGUMENT =
            "edu.illinois.library.metaslurper.config";

    public static void main(String[] args) {
        String serviceStr = null;

        for (int i = 0; i < args.length; i++) {
            if ("-service".equals(args[i])) {
                serviceStr = args[i + 1];
            }
        }

        final Slurper slurper = new Slurper();
        SlurpResult result = null;

        if (serviceStr != null) {
            Service service = ServiceFactory.getService(serviceStr);
            if (service != null) {
                result = slurper.slurp(service);
            } else {
                String allServices = ServiceFactory.allServices()
                        .stream()
                        .map(Service::getName)
                        .collect(Collectors.joining(", "));
                System.err.println("Unrecognized service: " + serviceStr);
                System.err.println("Available services: " + allServices);
            }
        } else {
            result = slurper.slurpAll();
        }

        if (result != null) {
            System.out.println(result);
        }
    }

}
