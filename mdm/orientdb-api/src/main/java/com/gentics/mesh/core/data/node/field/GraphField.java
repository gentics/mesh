package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibField;

/**
 * Common interface for all graph fields. Every field has a key and can be removed from a container.
 */
public interface GraphField extends HibField {

	String FIELD_KEY_PROPERTY_KEY = "fieldkey";

}
