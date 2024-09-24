package com.gentics.mesh.contentoperation;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.hibernate.data.domain.AbstractFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * The composite key of content, containing the id of the schema version (which identifies the table) and the
 * id of the content (which identifies the record).
 */
public class ContentKey implements Serializable {

	private static final long serialVersionUID = 4565947217990493423L;

	private final UUID contentUuid;
	private final UUID schemaVersionUuid;
	private final ReferenceType type;

	public ContentKey(UUID contentUuid, UUID schemaVersionUuid, ReferenceType type) {
		this.contentUuid = contentUuid;
		this.schemaVersionUuid = schemaVersionUuid;
		this.type = type;
	}

	public UUID getContentUuid() {
		return contentUuid;
	}

	public UUID getSchemaVersionUuid() {
		return schemaVersionUuid;
	}

	public ReferenceType getType() {
		return type;
	}

	public static ContentKey fromEdge(HibNodeFieldContainerEdgeImpl edge) {
		return fromContentUUIDAndVersion(edge.getContentUuid(), edge.getVersion());
	}

	public static ContentKey fromContent(HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container) {
		return fromContentUUIDAndVersion(container.getDbUuid(), container.getSchemaContainerVersion());
	}

	public static ContentKey fromEdge(AbstractFieldEdgeImpl<?> edge) {
		return new ContentKey(edge.getContainerUuid(), edge.getContainerVersionUuid(), edge.getContainerType());
	}

	public static ContentKey fromContentUUIDAndVersion(UUID contentUuid, HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		ReferenceType type = version instanceof HibSchemaVersion ? ReferenceType.FIELD : ReferenceType.MICRONODE;
		return new ContentKey(contentUuid, (UUID) version.getId(), type);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ContentKey)) return false;
		ContentKey that = (ContentKey) o;
		return Objects.equals(contentUuid, that.contentUuid) && Objects.equals(schemaVersionUuid, that.schemaVersionUuid) && type == that.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentUuid, schemaVersionUuid, type);
	}

	@Override
	public String toString() {
		return String.format("ContentKey(schemaVersion: %s, content: %s, type: %s)", schemaVersionUuid, contentUuid, type);
	}
}
