package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.Item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MockSourceService implements SourceService {

    @Override
    public String getName() {
        return MockSourceService.class.getSimpleName();
    }

    @Override
    public void close() {
    }

    @Override
    public int numItems() throws IOException {
        return 2;
    }

    @Override
    public Stream<Item> items() throws IOException {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < numItems(); i++) {
            items.add(new Item("ID " + (i + 1)));
        }
        return items.stream();
    }

}
