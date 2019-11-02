package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

class MedusaDLSAgent extends MedusaDLSEntity implements ConcreteEntity {

    private String sourceURI;

    MedusaDLSAgent(JSONObject rootObject, String sourceURI) {
        super(rootObject);
        this.sourceURI = sourceURI;
    }

    @Override
    public Set<Element> getElements() {
        final Set<Element> elements = new HashSet<>();
        elements.add(new Element("service", MedusaDLSService.PUBLIC_NAME));
        if (rootObject.has("name")) {
            // name
            String value = rootObject.getString("name");
            if (value != null && !value.isEmpty()) {
                elements.add(new Element("name", value));
            }

            // description
            value = rootObject.getString("description");
            if (value != null && !value.isEmpty()) {
                elements.add(new Element("description", value));
            }
        }
        return elements;
    }

    @Override
    public String getSinkID() {
        final String key = "id";
        return rootObject.has(key) ?
                String.format("%sentity-%d",
                        MedusaDLSService.ENTITY_ID_PREFIX, rootObject.getInt(key)) : null;
    }

    public String getSourceID() {
        final String key = "id";
        return rootObject.has(key) ? "" + rootObject.getInt(key) : null;
    }

    @Override
    public String getSourceURI() {
        return sourceURI;
    }

    @Override
    public Variant getVariant() {
        return Variant.ENTITY;
    }

}
