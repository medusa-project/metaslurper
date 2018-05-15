package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;
import org.w3c.dom.Node;

public class DefaultElementTransformer implements ElementTransformer {

    /**
     * @param pmhNode Element node from an OAI-PMH record.
     * @return        Element with a name of the node name and a value of the
     *                node's text content.
     */
    @Override
    public Element transform(Node pmhNode) {
        final String name = pmhNode.getNodeName();
        final String value = pmhNode.getTextContent();

        if (name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
            return new Element(name, value);
        }
        return null;
    }

}
