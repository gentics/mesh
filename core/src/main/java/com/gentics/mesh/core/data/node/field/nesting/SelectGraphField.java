package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

public interface SelectGraphField<T extends ListableGraphField> extends NestingGraphField, MicroschemaListableGraphField {

	/**
	 * Add a option to the select graph field.
	 * 
	 * @param t
	 */
	void addOption(T t);

	/**
	 * Return a list of options.
	 * 
	 * @return
	 */
	List<T> getOptions();

	/**
	 * Remove the given options from the selection.
	 * 
	 * @param t
	 */
	void removeOption(T t);

	/**
	 * Remove all options from the selection.
	 */
	void removeAllOptions();

	/**
	 * Return the current selected graph field.
	 * 
	 * @return
	 */
	T getSelection();

	/**
	 * Return a list of selections.
	 * 
	 * @return
	 */
	List<T> getSelections();

	/**
	 * Return the flag which indicates whether the field is a multiselect field.
	 * 
	 * @return
	 */
	boolean isMultiselect();

	/**
	 * Set the multiselect flag.
	 * 
	 * @param flag
	 */
	void setMultiselect(boolean flag);

}
