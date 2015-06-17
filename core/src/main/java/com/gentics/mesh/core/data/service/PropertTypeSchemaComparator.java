package com.gentics.mesh.core.data.service;

import java.util.Comparator;

import com.gentics.mesh.core.rest.schema.response.PropertyTypeSchemaResponse;

public class PropertTypeSchemaComparator implements Comparator<PropertyTypeSchemaResponse> {
	@Override
	public int compare(PropertyTypeSchemaResponse o1, PropertyTypeSchemaResponse o2) {
		return o1.getKey().compareTo(o2.getKey());
	}
}