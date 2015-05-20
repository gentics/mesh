package com.gentics.mesh.core.data.model.schema.propertytypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListPropertyTypeSchema extends BasicPropertyTypeSchema {

	private static final long serialVersionUID = 1347967922556984653L;

	private Set<PropertyType> propertyTypeWhiteList = new HashSet<>();

	private List<AbstractPropertyTypeSchema> list = new ArrayList<>();

	private boolean isDynamicList;

	public ListPropertyTypeSchema(String name) {
		super(name, PropertyType.LIST);
	}

	/**
	 * Returns the whitelist for the allowed property types inside this list. All types are allowed when the list is empty.
	 * 
	 * @return
	 */
	public Set<PropertyType> getWhiteList() {
		return propertyTypeWhiteList;
	}

	public boolean isDynamicList() {
		return isDynamicList;
	}

	public void setDynamicList(boolean isDynamicList) {
		this.isDynamicList = isDynamicList;
	}

}
