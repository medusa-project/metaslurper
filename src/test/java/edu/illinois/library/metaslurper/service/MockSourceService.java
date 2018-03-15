package edu.illinois.library.metaslurper.service;

import edu.illinois.library.metaslurper.entity.BasicItem;
import edu.illinois.library.metaslurper.entity.Item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    public ConcurrentIterator<Item> items() throws IOException {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < numItems(); i++) {
            BasicItem item = new BasicItem();
            item.setID("ID " + (i + 1));
            items.add(item);
        }

        return new ConcurrentIterator<Item>() {
            private final int numItems = numItems();
            private final AtomicInteger index = new AtomicInteger();
            private final Iterator<Item> it = items.iterator();

            @Override
            public Item next() throws EndOfIterationException {
                if (index.getAndIncrement() < numItems) {
                    return it.next();
                } else {
                    throw new EndOfIterationException();
                }
            }
        };
    }

}
