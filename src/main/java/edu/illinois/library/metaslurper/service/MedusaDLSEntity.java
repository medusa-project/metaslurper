package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Element;
import edu.illinois.library.metaslurper.entity.Image;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static edu.illinois.library.metaslurper.service.MedusaDLSService.getClient;

abstract class MedusaDLSEntity {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MedusaDLSEntity.class);

    JSONObject rootObject;

    static String getSinkID(String id) {
        return MedusaDLSService.ENTITY_ID_PREFIX + id;
    }

    MedusaDLSEntity(JSONObject rootObject) {
        this.rootObject = rootObject;
    }

    public Set<Image> getAccessImages() {
        final Set<Image> images = new HashSet<>();
        JSONObject allCrops = rootObject.optJSONObject("representative_images");
        if (allCrops != null) {
            // For each full crop
            JSONObject fullCrops = allCrops.optJSONObject("full");
            if (fullCrops != null) {
                fullCrops.keySet().forEach(key -> {
                    String value = fullCrops.getString(key);
                    if ("full".equals(key)) {
                        // The value is a master image binary URI. Fetch its
                        // representation and create a corresponding Image.
                        try {
                            JSONObject binaryObj = fetchBinary(value);
                            String s3uri = binaryObj.optString("object_uri");
                            images.add(new Image(s3uri, Image.Crop.FULL, 0, true));
                        } catch (IOException e) {
                            LOGGER.error("getAccessImages(): {}", e.getMessage());
                        }
                    } else {
                        // The value is the URI of an access image of a
                        // particular size.
                        int size = Integer.parseInt(key);
                        images.add(new Image(value, Image.Crop.FULL, size, false));
                    }
                });
            }
            // For each square crop
            JSONObject squareImages = allCrops.optJSONObject("square");
            if (squareImages != null) {
                squareImages.keySet().forEach(key -> {
                    String uri = squareImages.getString(key);
                    int size = Integer.parseInt(key);
                    images.add(new Image(uri, Image.Crop.SQUARE, size, false));
                });
            }
        }
        return images;
    }

    private JSONObject fetchBinary(String uri) throws IOException {
        try {
            ContentResponse response = getClient().newRequest(uri)
                    .header("Accept", "application/json")
                    .timeout(MedusaDLSService.REQUEST_TIMEOUT, TimeUnit.SECONDS)
                    .send();
            String body = response.getContentAsString();

            switch (response.getStatus()) {
                case HttpStatus.OK_200:
                    return new JSONObject(body);
                default:
                    JSONObject jobj = new JSONObject(body);
                    String message = jobj.getString("error");
                    throw new IOException("Got HTTP " + response.getStatus() +
                            " for " + uri + ": " + message);
            }
        } catch (ExecutionException | InterruptedException |
                TimeoutException e) {
            throw new IOException(e);
        }
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
        return MedusaDLSService.getServiceKey();
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
        return getSinkID(getSourceID());
    }

    public String getParentSinkID() {
        return null;
    }

    public String getContainerSinkID() {
        return null;
    }

    public String toString() {
        return getSourceID() + " / " + getSinkID();
    }

}
