package com.gentics.mesh.plugin.forms.model;

/**
 * Combi Element
 */
@Reportable
@FormElementType("catalog")
public class CatalogElement extends ValueListElement {

	public CatalogElement() {
	}

	protected CatalogElement(CatalogElement other, boolean deep) {
		super(other, deep);

	}

	@Override
	public FormElement clone(boolean deep) {
		return new CatalogElement(this, deep);
	}

}
