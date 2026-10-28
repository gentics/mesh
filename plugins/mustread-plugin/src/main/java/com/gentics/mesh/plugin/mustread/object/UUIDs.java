package com.gentics.mesh.plugin.mustread.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UUIDs {

    private List<String> uuids = new ArrayList<>();

    public UUIDs UUIDs() {
        return this;
    }

    public void addUuid(String id) {
        uuids.add(id);
    }

    public List<String> getUUIDs() {
        return uuids;
    }

    public void removeAll(UUIDs second) {
        Collection<String> secondArray = second.getUUIDs();

        uuids.removeAll(secondArray);
    }

    @Override
    public String toString() {
        return "UUIDs{" +
                "uuids=" + uuids +
                '}';
    }
}
