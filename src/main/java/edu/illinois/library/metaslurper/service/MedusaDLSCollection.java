package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.ConcreteEntity;
import edu.illinois.library.metaslurper.entity.Variant;
import org.json.JSONObject;

class MedusaDLSCollection extends MedusaDLSEntity implements ConcreteEntity {

    MedusaDLSCollection(JSONObject rootObject) {
        super(rootObject);
    }

    @Override
    public Variant getVariant() {
        return Variant.COLLECTION;
    }

}
