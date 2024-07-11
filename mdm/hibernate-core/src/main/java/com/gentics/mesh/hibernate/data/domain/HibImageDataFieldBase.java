package com.gentics.mesh.hibernate.data.domain;

import java.util.Map;

import com.gentics.mesh.core.data.node.field.ImageDataField;

/**
 * Hibernate image data field entity extension
 * 
 * @author plyhun
 *
 */
public interface HibImageDataFieldBase extends ImageDataField {

	public void setMetadataProperties(Map<String, String> properties);
}
