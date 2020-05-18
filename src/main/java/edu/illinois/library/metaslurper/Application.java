package edu.illinois.library.metaslurper;

import edu.illinois.library.metaslurper.harvest.Harvest;
import edu.illinois.library.metaslurper.service.SinkService;
import edu.illinois.library.metaslurper.service.SourceService;
import edu.illinois.library.metaslurper.service.ServiceFactory;
import edu.illinois.library.metaslurper.harvest.Harvester;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class Application {

    private enum Argument {
        INCREMENTAL("i", "incremental", false, "Last-modified epoch second"),
        LOG_LEVEL("v", "log_level", false, "Log level: error, warn, info, debug (default), trace"),
        MAX_NUM_ENTITIES("m", "max_entities", false, "Maximum number of entities to harvest"),
        SOURCE_SERVICE("s", "source", true, "Source service key"),
        SINK_SERVICE("k", "sink", true, "Sink service key"),
        THREADS("t", "threads", false, "Number of harvesting threads (default = 1)"),
        THROTTLE("h", "throttle", false, "Milliseconds to wait between items (default = 0)");

        private String shortArg, longArg, description;
        private boolean isRequired;

        Argument(String shortArg,
                 String longArg,
                 boolean isRequired,
                 String description) {
            this.shortArg = shortArg;
            this.longArg = longArg;
            this.description = description;
            this.isRequired = isRequired;
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Application.class);

    private static int numThreads   = 1;
    private static int throttleMsec = 0;

    /**
     * @param args See {@link Argument}.
     */
    public static void main(String[] args) {
        LOGGER.info("Command arguments: " + String.join(" ", args));
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(getOptions(), args);

            // Set log level
            if (cmd.hasOption(Argument.LOG_LEVEL.longArg)) {
                String level = cmd.getOptionValue(Argument.LOG_LEVEL.longArg).toLowerCase();
                switch (level) {
                    case "error":
                        Configurator.setRootLevel(Level.ERROR);
                        break;
                    case "warn":
                        Configurator.setRootLevel(Level.WARN);
                        break;
                    case "info":
                        Configurator.setRootLevel(Level.INFO);
                        break;
                    case "trace":
                        Configurator.setRootLevel(Level.TRACE);
                        break;
                    default: // debug, already set in log4j2.xml
                        break;
                }
            }

            // Set num threads
            numThreads = cmd.hasOption(Argument.THREADS.longArg) ?
                    Integer.parseInt(cmd.getOptionValue(Argument.THREADS.longArg)) :
                    numThreads;
            numThreads = Math.max(numThreads, 1);

            // Set throttle time
            throttleMsec = cmd.hasOption(Argument.THROTTLE.longArg) ?
                    Integer.parseInt(cmd.getOptionValue(Argument.THROTTLE.longArg)) :
                    throttleMsec;

            String sourceStr = cmd.getOptionValue(Argument.SOURCE_SERVICE.longArg);
            String sinkStr = cmd.getOptionValue(Argument.SINK_SERVICE.longArg);

            try (SinkService sink = ServiceFactory.getSinkService(sinkStr)) {
                if (sink != null) {
                    try (SourceService source = ServiceFactory.getSourceService(sourceStr)) {
                        if (source != null) {
                            if (cmd.hasOption(Argument.INCREMENTAL.longArg)) {
                                long second = Long.parseLong(
                                        cmd.getOptionValue(Argument.INCREMENTAL.longArg));
                                Instant lastModified = Instant.ofEpochSecond(second);
                                source.setLastModified(lastModified);
                            }
                            new Harvester().harvest(source, sink, newHarvest(cmd));
                        } else {
                            System.err.println("Unrecognized source service key: " + sourceStr);
                            printSourceServices();
                            System.exit(-1);
                        }
                    }
                } else {
                    System.err.println("Unrecognized sink service key: " + sinkStr);
                    printSinkServices();
                    System.exit(-1);
                }
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            try {
                // Sometimes the above output gets interleaved into the help
                // output...
                Thread.sleep(1);
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar <jarfile>", getOptions());
                System.exit(-1);
            } catch (InterruptedException ignore) {}
        }
    }

    public static int getNumThreads() {
        return numThreads;
    }

    public static int getThrottleMsec() {
        return throttleMsec;
    }

    private static Options getOptions() {
        final Options options = new Options();
        Arrays.stream(Argument.values()).forEach(arg ->
                options.addOption(Option.builder()
                        .argName(arg.shortArg)
                        .longOpt(arg.longArg)
                        .hasArg()
                        .desc(arg.description +
                                (arg.isRequired ? "" : " (optional)"))
                        .required(arg.isRequired)
                        .build()));
        return options;
    }

    private static Harvest newHarvest(CommandLine cmd) {
        Harvest harvest = new Harvest();
        if (cmd.hasOption(Argument.MAX_NUM_ENTITIES.longArg)) {
            harvest.setMaxNumEntities(Integer.parseInt(
                    cmd.getOptionValue(Argument.MAX_NUM_ENTITIES.longArg)));
        }
        return harvest;
    }

    private static void printSourceServices() {
        String allSources = ServiceFactory.allSourceServices()
                .stream()
                .map(s -> s.getName() + " (" + s.getKey() + ")")
                .collect(Collectors.joining("\n"));
        System.err.println("Available source services: " + allSources + "\n");
    }

    private static void printSinkServices() {
        String allSources = ServiceFactory.allSinkServices()
                .stream()
                .map(s -> s.getName() + " (" + s.getKey() + ")")
                .collect(Collectors.joining("\n"));
        System.err.println("Available sink services: " + allSources + "\n");
    }

}
