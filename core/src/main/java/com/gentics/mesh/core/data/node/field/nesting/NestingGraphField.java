package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;

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

	/**
	 * Create a new node graph field.
	 * 
	 * @param key
	 * @param node
	 * @return
	 */
	NodeGraphField createNode(String key, Node node);

	/**
	 * Create a new date graph field.
	 * 
	 * @param key
	 * @return
	 */
	DateGraphField createDate(String key);

	/**
	 * Create a new number graph field.
	 * 
	 * @param key
	 * @return
	 */
	NumberGraphField createNumber(String key);

	/**
	 * Create a new html graph field.
	 * 
	 * @param key
	 * @return
	 */
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
