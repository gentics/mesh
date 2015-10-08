package com.gentics.mesh.core.rest.node.field;

import java.util.List;

public interface SelectField extends ListableField, MicroschemaListableField {

	/**
	 * Set the select field string selections.
	 * 
	 * @param selections
	 * @return
	 */
	SelectField setSelections(List<String> selections);

	/**
	 * Return the select field string selections.
	 * 
	 * @return
	 */
	List<String> getSelections();

}
