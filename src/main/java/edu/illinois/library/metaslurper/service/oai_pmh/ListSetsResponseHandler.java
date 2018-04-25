package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;
import org.xml.sax.Attributes;

import java.util.HashSet;

final class ListSetsResponseHandler extends OAIPMHResponseHandler {

    private boolean isReadingName, isReadingSpec;

    @Override
    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes attributes) {
        this.currentElementAttributes = attributes;
        if (OAI_PMH_NAMESPACE.equals(namespaceURI)) {
            // If there is a resumptionToken, the value will be found in the
            // completeListSize attribute. If not, it will be found by counting
            // the number of set elements.
            switch (localName) {
                case "set":
                    completeListSize++;
                    break;
                case "setName":
                    isReadingName = true;
                    break;
                case "setSpec":
                    isReadingSpec = true;
                    break;
                case "resumptionToken":
                    isReadingResumptionToken = true;
                    completeListSize =
                            Integer.parseInt(attributes.getValue("completeListSize"));
                    break;
                case "error":
                    isReadingError = true;
                    break;
            }
        } else {
            for (String namespace : elementNamespaces) {
                if (namespace.equals(namespaceURI)) {
                    currentElementName = qName;
                }
            }
        }

        currentElementChars.setLength(0);
    }

    @Override
    public void endElement(String namespaceURI,
                           String localName,
                           String qName) {
        if (currentElementName != null) {
            Element e = new Element(currentElementName,
                    currentElementChars.toString());
            if (!e.getValue().isEmpty()) {
                currentRecordElements.add(e);
            }
        }
        currentElementName = null;

        if (isReadingName) {
            Element e = new Element("setName", currentElementChars.toString());
            currentRecordElements.add(e);
            isReadingName = false;
        } else if (isReadingSpec) {
            Element e = new Element("setSpec", currentElementChars.toString());
            currentRecordElements.add(e);
            isReadingSpec = false;
        } else if (isReadingResumptionToken) {
            resumptionToken = currentElementChars.toString();
            if (resumptionToken.isEmpty()) {
                resumptionToken = null;
            }
            isReadingResumptionToken = false;
        } else if (isReadingError) {
            errorException = new OAIPMHException(
                    currentElementAttributes.getValue("code"),
                    currentElementChars.toString());
            if (listener != null) {
                listener.onError(errorException);
            }
            isReadingError = false;
        }

        if (OAI_PMH_NAMESPACE.equals(namespaceURI) && "set".equals(localName)) {
            if (listener != null) {
                listener.onSet(new HashSet<>(currentRecordElements));
            }
            currentRecordElements = new HashSet<>();
        }
    }

}
