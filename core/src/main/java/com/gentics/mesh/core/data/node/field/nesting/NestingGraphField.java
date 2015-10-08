package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;

public interface NestingGraphField extends GraphField {

	/**
	 * Return the string graph field with the given key.
	 * 
	 * @param key
	 * @return
	 */
	StringGraphField getString(String key);

	/**
	 * Create a new string graph field with the given string.
	 * 
	 * @param string
	 * @return
	 */
	StringGraphField createString(String string);

	NodeGraphField createNode(String key, Node node);

	DateGraphField createDate(String key);

	NumberGraphField createNumber(String key);

	HtmlGraphField createHTML(String key);

	/**
	 * Return the boolean graph field with the given key.
	 * 
	 * @param key
	 * @return
	 */
	BooleanGraphField getBoolean(String key);

	/**
	 * Create a new boolean graph field within the nesting field.
	 * 
	 * @param key
	 * @return
	 */
	BooleanGraphField createBoolean(String key);
}
