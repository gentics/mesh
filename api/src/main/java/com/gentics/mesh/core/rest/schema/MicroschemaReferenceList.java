package com.gentics.mesh.core.rest.schema;

import java.util.ArrayList;
import java.util.Collection;

import com.gentics.mesh.core.rest.common.RestModel;

public class MicroschemaReferenceList extends ArrayList<MicroschemaReference> implements RestModel {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4193979905408006174L;

	/**
	 * Create an empty list
	 */
	public MicroschemaReferenceList() {
		super();
	}

	/**
	 * Create a list containing the items of the given collection
	 * @param c
	 */
	public MicroschemaReferenceList(Collection<? extends MicroschemaReference> c) {
		super(c);
	}
}
