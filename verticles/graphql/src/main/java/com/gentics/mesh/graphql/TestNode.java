package com.gentics.mesh.graphql;

import java.util.ArrayList;
import java.util.List;

public class TestNode {

	String uuid;

	List<TestField> fields = new ArrayList<>();

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public List<TestField> getFields() {
		return fields;
	}
}
