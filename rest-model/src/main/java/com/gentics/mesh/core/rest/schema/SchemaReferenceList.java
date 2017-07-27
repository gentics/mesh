package com.gentics.mesh.core.rest.schema;

import java.util.ArrayList;
import java.util.Collection;

import com.gentics.mesh.core.rest.common.RestModel;

public class SchemaReferenceList extends ArrayList<SchemaReference> implements RestModel {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1100439020402195658L;

	/**
	 * Create an empty list
	 */
	public SchemaReferenceList() {
		super();
	}

	/**
	 * Create a list containing the items of the given collection
	 * @param c
	 */
	public SchemaReferenceList(Collection<? extends SchemaReference> c) {
		super(c);
	}
}
