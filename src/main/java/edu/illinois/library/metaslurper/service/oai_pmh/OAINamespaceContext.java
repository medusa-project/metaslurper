package edu.illinois.library.metaslurper.service.oai_pmh;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

public final class OAINamespaceContext implements NamespaceContext {

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix != null) {
            switch (prefix) {
                case "dc":
                    return "http://purl.org/dc/elements/1.1/";
                case "oai_dc":
                    return "http://www.openarchives.org/OAI/2.0/oai_dc/";
                case "oai":
                    return "http://www.openarchives.org/OAI/2.0/";
                default:
                    return XMLConstants.NULL_NS_URI;
            }
        }
        throw new NullPointerException("Null prefix");
    }

    @Override
    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }

}
