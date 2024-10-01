package com.gentics.mesh.hibernate.data.domain;

import java.util.Map;

import com.gentics.mesh.core.data.node.field.HibImageDataField;

/**
 * Hibernate image data field entity extension
 * 
 * @author plyhun
 *
 */
public interface HibImageDataFieldBase extends HibImageDataField {

	public void setMetadataProperties(Map<String, String> properties);
}
