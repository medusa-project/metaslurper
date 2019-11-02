package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONObject;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MedusaDLSItem extends MedusaDLSEntity implements ConcreteEntity {

    // Example: https://digital.library.illinois.edu/items/7f3c5580-9975-0134-2096-0050569601ca-8.json
    MedusaDLSItem(JSONObject rootObject) {
        super(rootObject);
    }

    @Override
    public String getContainerSinkID() {
        String containerURI = rootObject.optString("collection_uri");
        if (containerURI != null) {
            Pattern idPattern = Pattern.compile("/collections/([a-f0-9-]+)");
            Matcher matcher = idPattern.matcher(containerURI);
            if (matcher.find()) {
                return getSinkID(matcher.group(1));
            }
        }
        return null;
    }

    @Override
    public Set<Element> getElements() {
        Set<Element> elements = super.getElements();
        elements.add(new Element("service", MedusaDLSService.PUBLIC_NAME));
        return elements;
    }

    @Override
    public Variant getVariant() {
        return ("file".equalsIgnoreCase(rootObject.optString("variant"))) ?
                Variant.FILE : Variant.ITEM;
    }

}
