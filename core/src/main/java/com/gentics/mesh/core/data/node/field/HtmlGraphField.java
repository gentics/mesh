package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.parameter.impl.LinkType;

/**
 * The HtmlField Domain Model interface.
 * 
 * A HTML graph field is a basic node field which can be used to store a single HTML string value.
 */
public interface HtmlGraphField extends ListableGraphField, BasicGraphField<HtmlField> {

	/**
	 * Set the HTML field value for the field.
	 * 
	 * @param html
	 */
	void setHtml(String html);

	/**
	 * Return the HTML field value for the field.
	 * 
	 * @return
	 */
	String getHTML();

}
