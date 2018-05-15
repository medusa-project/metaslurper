package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;
import org.w3c.dom.Node;

public interface ElementTransformer {

    /**
     * @param pmhNode Element node from an OAI-PMH record.
     * @return        Element, or {@literal null} if a valid element cannot be
     *                created from the given node.
     */
    Element transform(Node pmhNode);

}
