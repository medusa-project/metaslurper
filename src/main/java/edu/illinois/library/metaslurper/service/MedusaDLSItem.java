package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Image;
import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

class MedusaDLSItem extends MedusaDLSEntity implements ConcreteEntity {

    // Example: https://digital.library.illinois.edu/items/7f3c5580-9975-0134-2096-0050569601ca-8.json
    MedusaDLSItem(JSONObject rootObject) {
        super(rootObject);
    }

    @Override
    public Set<Image> getAccessImages() {
        final Set<Image> images = new HashSet<>();
        JSONObject allImages = rootObject.getJSONObject("representative_images");
        if (allImages.has("full")) {
            JSONObject fullImages = allImages.getJSONObject("full");
            fullImages.keySet().forEach(key -> {
                String uri = fullImages.getString(key);
                if ("full".equals(key)) {
                    // TODO: deal with this
                } else {
                    int size = Integer.parseInt(key);
                    images.add(new Image(uri, size, Image.Crop.FULL));
                }
            });
        }
        if (allImages.has("square")) {
            JSONObject squareImages = allImages.getJSONObject("square");
            squareImages.keySet().forEach(key -> {
                int size = Integer.parseInt(key);
                String uri = squareImages.getString(key);
                images.add(new Image(uri, size, Image.Crop.SQUARE));
            });
        }

        return images;
    }

    @Override
    public Variant getVariant() {
        return ("file".equalsIgnoreCase(rootObject.optString("variant"))) ?
                Variant.FILE : Variant.ITEM;
    }

}
