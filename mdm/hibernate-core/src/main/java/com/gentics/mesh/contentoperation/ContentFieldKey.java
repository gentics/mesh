package com.gentics.mesh.contentoperation;

import com.gentics.mesh.core.rest.common.ReferenceType;

import java.util.UUID;

public class ContentFieldKey extends ContentKey {

	private final String fieldKey;

	public ContentFieldKey(UUID contentUuid, UUID schemaVersionUuid, ReferenceType type, String fieldKey) {
		super(contentUuid, schemaVersionUuid, type);

		this.fieldKey = fieldKey;
	}

	public String getFieldKey() {
		return fieldKey;
	}
}
