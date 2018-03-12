package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Item;

import java.util.ArrayList;
import java.util.List;

public class MockSinkService implements SinkService {

    private final List<Item> ingestedItems = new ArrayList<>();

    @Override
    public String getName() {
        return MockSinkService.class.getSimpleName();
    }

    @Override
    public void close() {
    }

    @Override
    public void ingest(Item item) {
        ingestedItems.add(item);
    }

    public List<Item> getIngestedItems() {
        return ingestedItems;
    }

}
