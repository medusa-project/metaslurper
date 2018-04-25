package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;

import java.util.Set;

/**
 * Supply an instance to {@link Harvester} to receive notifications when a
 * record or set is encountered.
 *
 * @author Alex Dolski UIUC
 */
public interface Listener {

    void onRecord(Set<Element> elements);

    void onSet(Set<Element> elements);

    void onError(OAIPMHException error);

}
