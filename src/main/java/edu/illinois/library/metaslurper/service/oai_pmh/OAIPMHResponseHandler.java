package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

abstract class OAIPMHResponseHandler extends DefaultHandler {

    static final String DC_NAMESPACE =
            "http://purl.org/dc/elements/1.1/";
    static final String OAI_PMH_NAMESPACE =
            "http://www.openarchives.org/OAI/2.0/";

    final Set<String> elementNamespaces = new HashSet<>();

    int completeListSize;

    Attributes currentElementAttributes;
    Set<Element> currentRecordElements = new HashSet<>();
    String currentElementName, resumptionToken;
    final StringBuilder currentElementChars = new StringBuilder();
    boolean isReadingResumptionToken, isReadingError;
    OAIPMHException errorException;

    Listener listener;

    OAIPMHResponseHandler() {
        elementNamespaces.add(DC_NAMESPACE);
    }

    @Override
    public void characters(char[] chars, int start, int length) {
        currentElementChars.append(Arrays.copyOfRange(chars, start, start + length));
    }

    /**
     * @param namespace Namespace URI of elements to listen for.
     */
    public void addElementNamespace(String namespace) {
        elementNamespaces.add(namespace);
    }

    public int getCompleteListSize() throws OAIPMHException {
        if (errorException != null) {
            throw errorException;
        }
        return completeListSize;
    }

    public String getResumptionToken() throws OAIPMHException {
        if (errorException != null) {
            throw errorException;
        }
        return resumptionToken;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

}
