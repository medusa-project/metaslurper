package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.service.oai_pmh.DefaultElementTransformer;
import edu.illinois.library.metaslurper.service.oai_pmh.ElementTransformer;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class DIMElementTransformer implements ElementTransformer {

    /**
     * Transforms the given node into an element whose value corresponds to
     * the node's text content and name corresponds to the string
     * {@literal "[@mdschema]:[@element]:[@qualifier]"}.
     *
     * @param pmhNode Element node from an OAI-PMH record.
     */
    @Override
    public Element transform(Node pmhNode) {
        final NamedNodeMap attrs = pmhNode.getAttributes();
        if (attrs.getLength() > 0) {
            final List<String> nameParts = new ArrayList<>();
            for (String s : new String[] { "mdschema", "element", "qualifier"}) {
                Node attr = attrs.getNamedItem(s);
                if (attr != null) {
                    String value = attr.getNodeValue();
                    if (value != null && !value.isEmpty()) {
                        nameParts.add(value);
                    }
                }
            }
            String name = nameParts.stream().collect(Collectors.joining(":"));
            String value = pmhNode.getTextContent();
            if (name != null && !name.isEmpty() &&
                    value != null && !value.isEmpty()) {
                return new Element(name, value);
            }
        }
        return new DefaultElementTransformer().transform(pmhNode);
    }

}
