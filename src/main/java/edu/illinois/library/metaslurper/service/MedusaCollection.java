package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Image;
import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Medusa Collection Registry collection, whose representation is available at
 * {@literal /collections/:id.json}.
 */
final class MedusaCollection implements ConcreteEntity {

    private JSONObject jobj;

    MedusaCollection(JSONObject jsonRepresentation) {
        this.jobj = jsonRepresentation;
    }

    @Override
    public Set<Image> getAccessImages() {
        final Set<Image> images = new HashSet<>();
        final String uuid = jobj.getString("representative_image");
        if (uuid != null && !uuid.isBlank()) {
            for (int i = (int) Math.pow(2, ConcreteEntity.MIN_ACCESS_IMAGE_POWER);
                 i <= Math.pow(2, ConcreteEntity.MAX_ACCESS_IMAGE_POWER);
                 i *= 2) {
                final int size = i;
                Arrays.stream(Image.Crop.values()).forEach((crop) -> {
                    String imageURI = String.format("%s/%s/%s/!%d,%d/0/default.jpg",
                            MedusaCollectionRegistryService.getIIIFEndpointURI(),
                            uuid,
                            crop.toIIIFRegion(),
                            size, size);
                    images.add(new Image(imageURI, size, crop));
                });
            }
        }
        return images;
    }

    @Override
    public Set<Element> getElements() {
        final Set<Element> elements = new HashSet<>();

        // simple strings & booleans
        addStringIfExists(elements, "title");
        addStringIfExists(elements, "description");
        addStringIfExists(elements, "description_html");
        addStringIfExists(elements, "access_url");
        addStringIfExists(elements, "physical_collection_url");
        addStringIfExists(elements, "representative_image");
        addStringIfExists(elements, "representative_item");
        addStringIfExists(elements, "repository_title");
        addStringIfExists(elements, "external_id");

        // resource types
        JSONArray arr = jobj.getJSONArray("resource_types");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject jobj2 = arr.getJSONObject(i);
            String value = jobj2.getString("name");
            // Capitalize the first letter.
            value = value.substring(0, 1).toUpperCase() + value.substring(1);
            elements.add(new Element("resource_type", value));
        }

        // access systems
        arr = jobj.getJSONArray("access_systems");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject jobj2 = arr.getJSONObject(i);
            elements.add(new Element("access_system", jobj2.getString("name")));
        }

        // rights basis
        JSONObject rights = jobj.getJSONObject("rights");
        String value = rights.optString("rights_basis");
        if (!value.isBlank()) {
            elements.add(new Element("rights_basis", value));
        }

        // copyright jurisdiction
        value = rights.optString("copyright_jurisdiction");
        if (!value.isBlank()) {
            elements.add(new Element("copyright_jurisdiction", value));
        }

        // copyright statement
        value = rights.optString("copyright_statement");
        if (!value.isBlank()) {
            elements.add(new Element("copyright_statement", value));
        }


        // custom copyright statement
        value = rights.optString("custom_copyright_statement");
        if (!value.isBlank()) {
            elements.add(new Element("custom_copyright_statement", value));
        }

        // access restrictions
        value = rights.optString("access_restrictions");
        if (!value.isBlank()) {
            elements.add(new Element("access_restrictions", value));
        }

        // child collections
        JSONArray children = jobj.getJSONArray("child_collections");
        for (int i = 0; i < children.length(); i++) {
            JSONObject jchild = children.getJSONObject(i);
            value = MedusaCollectionRegistryService.getEndpointURI() +
                    jchild.getString("path");
            elements.add(new Element("child_collection", value));
        }

        // parent collections
        JSONArray parents = jobj.getJSONArray("parent_collections");
        for (int i = 0; i < parents.length(); i++) {
            JSONObject jparent = parents.getJSONObject(i);
            value = MedusaCollectionRegistryService.getEndpointURI() +
                    jparent.getString("path");
            elements.add(new Element("parent_collection", value));
        }
        return elements;
    }

    private void addStringIfExists(Set<Element> elements, String key) {
        String value = jobj.optString(key);
        if (value != null && !value.isBlank()) {
            elements.add(new Element(key, value));
        }
    }

    @Override
    public String getServiceKey() {
        return MedusaCollectionRegistryService.getKeyFromConfiguration();
    }

    @Override
    public String getSinkID() {
        return getServiceKey() + "-" + getSourceID();
    }

    @Override
    public String getSourceID() {
        return jobj.getString("uuid");
    }

    @Override
    public String getSourceURI() {
        // Technically the source URI is
        // https://medusa.library.illinois.edu/collections/:id, but we want to
        // direct users to DLS instead.
        return MedusaDLSService.getEndpointURI() + "/collections/" +
                getSourceID();
    }

    @Override
    public Variant getVariant() {
        return Variant.COLLECTION;
    }

    boolean isPublished() {
        return jobj.getBoolean("publish");
    }

    public String toString() {
        return getSourceID() + " / " + getSinkID();
    }

}
