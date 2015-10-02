package com.gentics.mesh.core.rest.node;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.node.field.Field;

/**
 * Convenience class which is used to classify a field map so that it can be referenced within the custom json deserializers.
 *
 */
public class FieldMapImpl extends HashMap<String, Field>implements FieldMap {

	public FieldMapImpl(Map<String, Field> map) {
		super(map);
	}

	public FieldMapImpl() {
	}

	private static final long serialVersionUID = 5375505652759811047L;

}
