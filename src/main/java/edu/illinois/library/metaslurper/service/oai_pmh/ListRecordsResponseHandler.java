package edu.illinois.library.metaslurper.service.oai_pmh;

import edu.illinois.library.metaslurper.entity.Element;
import org.xml.sax.Attributes;

import java.util.HashSet;

final class ListRecordsResponseHandler extends OAIPMHResponseHandler {

    @Override
    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes attributes) {
        this.currentElementAttributes = attributes;
        if (OAI_PMH_NAMESPACE.equals(namespaceURI)) {
            // If there is a resumptionToken, the value will be found in the
            // completeListSize attribute. If not, it will be found by counting
            // the number of record elements.
            switch (localName) {
                case "record":
                    completeListSize++;
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

        if (isReadingResumptionToken) {
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

        if (OAI_PMH_NAMESPACE.equals(namespaceURI) && "metadata".equals(localName)) {
            if (listener != null) {
                listener.onRecord(new HashSet<>(currentRecordElements));
            }
            currentRecordElements = new HashSet<>();
        }
    }

}
