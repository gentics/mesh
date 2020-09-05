package com.gentics.mesh.core.data.schema;

import java.util.stream.Stream;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.VersionUtil;

public interface HibFieldSchemaVersionElement<R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion, SC extends HibFieldSchemaElement<R, RM, SC, SCV>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>>
	extends HibCoreElement, Comparable<SCV> {

	String getName();

	void setName(String name);

	String getJson();

	void setJson(String json);

	String getVersion();

	RM getSchema();

	void setSchema(RM schema);

	void deleteElement();

	// Version chain

	void setPreviousVersion(SCV version);

	void setNextVersion(SCV version);

	// Changes

	HibSchemaChange<?> getNextChange();

	HibSchemaChange<?> getPreviousChange();

	void setPreviousChange(HibSchemaChange<?> change);

	void setNextChange(HibSchemaChange<?> change);

	SCV getPreviousVersion();

	SCV getNextVersion();

	/**
	 * Return the parent schema container of the version.
	 *
	 * @return
	 */
	SC getSchemaContainer();

	/**
	 * Set the parent schema container of this version.
	 *
	 * @param container
	 */
	void setSchemaContainer(SC container);

	default int compareTo(SCV version) {
		return VersionUtil.compareVersions(getVersion(), version.getVersion());
	}

	/**
	 * Returns a stream of all previous versions.
	 * 
	 * @return
	 */
	default Stream<SCV> getPreviousVersions() {
		return StreamUtil.untilNull(
			this::getPreviousVersion,
			HibFieldSchemaVersionElement::getPreviousVersion);
	}

}
