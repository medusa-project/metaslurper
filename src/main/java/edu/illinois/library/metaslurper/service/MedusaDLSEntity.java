package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Element;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

abstract class MedusaDLSEntity {

    JSONObject rootObject;

    MedusaDLSEntity(JSONObject rootObject) {
        this.rootObject = rootObject;
    }

    public Set<Element> getElements() {
        final String key = "elements";
        final Set<Element> elements = new HashSet<>();
        if (rootObject.has(key)) {
            final JSONArray jelements = rootObject.getJSONArray(key);

            for (int i = 0; i < jelements.length(); i++) {
                JSONObject jelement = jelements.getJSONObject(i);
                String name = jelement.getString("name");
                String value = jelement.getString("value");
                Element element = new Element(name, value);
                elements.add(element);
            }
        }
        return elements;
    }

    public String getMediaType() {
        return null;
    }

    public String getServiceKey() {
        return MedusaDLSService.getKeyFromConfiguration();
    }

    public String getSourceID() {
        final String key = "id";
        return rootObject.has(key) ? rootObject.getString(key) : null;
    }

    public String getSourceURI() {
        final String key = "public_uri";
        return rootObject.has(key) ? rootObject.getString(key) : null;
    }

    public String getSinkID() {
        final String key = "id";
        return rootObject.has(key) ?
                MedusaDLSService.ENTITY_ID_PREFIX + rootObject.getString(key) : null;
    }

    public String getParentSinkID() {
        return null;
    }

    public String toString() {
        return getSourceID() + " / " + getSinkID();
    }

}
