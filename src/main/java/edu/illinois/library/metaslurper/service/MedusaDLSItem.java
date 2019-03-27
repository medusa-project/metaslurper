package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONObject;

class MedusaDLSItem extends MedusaDLSEntity implements ConcreteEntity {

    // Example: https://digital.library.illinois.edu/items/7f3c5580-9975-0134-2096-0050569601ca-8.json
    MedusaDLSItem(JSONObject rootObject) {
        super(rootObject);
    }

    @Override
    public Variant getVariant() {
        return ("file".equalsIgnoreCase(rootObject.optString("variant"))) ?
                Variant.FILE : Variant.ITEM;
    }

}
