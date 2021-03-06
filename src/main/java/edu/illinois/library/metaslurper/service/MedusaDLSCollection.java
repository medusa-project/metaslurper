package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

class MedusaDLSCollection extends MedusaDLSEntity implements ConcreteEntity {

    MedusaDLSCollection(JSONObject rootObject) {
        super(rootObject);
    }

    @Override
    public Set<Element> getElements() {
        // Get elements common to all entities.
        final Set<Element> elements = super.getElements();

        // Add these collection-specific strings & booleans.
        addStringIfExists(elements, "access_uri", "accessURI");
        addStringIfExists(elements, "external_id", "externalID");
        addStringIfExists(elements, "physical_collection_uri", "physicalCollectionURI");
        addStringIfExists(elements, "repository_title", "repositoryTitle");

        // Add resource types.
        JSONArray arr = rootObject.getJSONArray("resource_types");
        for (int i = 0; i < arr.length(); i++) {
            String value = arr.getString(i);
            // Capitalize the first letter.
            value = value.substring(0, 1).toUpperCase() + value.substring(1);
            elements.add(new Element("resourceType", value));
        }

        // Add access systems.
        arr = rootObject.getJSONArray("access_systems");
        boolean isDLSNative = false;
        for (int i = 0; i < arr.length(); i++) {
            String value = arr.getString(i);
            elements.add(new Element("accessSystem", value));
            if ("Medusa Digital Library".equals(value)) {
                isDLSNative = true;
            }
        }
        // Add a service element for DLS-native collections only.
        if (isDLSNative) {
            elements.add(new Element("service", MedusaDLSService.PUBLIC_NAME));
        }

        // Add collection type (DLDS-118) later renamed to collection structure
        // (DLDS-138)
        final String value = rootObject.optString("package_profile");
        if (value != null) {
            String collectionStructure;
            switch (value) {
                case "Free-Form":
                    collectionStructure = "Digital Archives";
                    break;
                default:
                    collectionStructure = "Digitized Materials";
                    break;
            }
            elements.add(new Element("collectionStructure",
                    collectionStructure));
        }

        return elements;
    }

    /**
     * @param elements    Set to add the element to.
     * @param sourceKey   Source key within {@link #rootObject}.
     * @param elementName Name of the element.
     */
    private void addStringIfExists(Set<Element> elements,
                                   String sourceKey,
                                   String elementName) {
        String value = rootObject.optString(sourceKey);
        if (value != null && !value.isBlank()) {
            elements.add(new Element(elementName, value));
        }
    }

    @Override
    public String getParentSinkID() {
        JSONObject parent = rootObject.optJSONObject("parent");
        if (parent != null) {
            return getSinkID(parent.getString("id"));
        }
        return null;
    }

    @Override
    public String getContainerName() {
        return rootObject.optString("repository_title");
    }

    @Override
    public Variant getVariant() {
        return Variant.COLLECTION;
    }

    public String toString() {
        return getSourceID() + " / " + getSinkID();
    }

}
