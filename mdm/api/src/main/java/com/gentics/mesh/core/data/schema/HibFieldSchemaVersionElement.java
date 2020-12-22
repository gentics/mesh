package com.gentics.mesh.core.data.schema;

import java.util.stream.Stream;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.VersionUtil;

public interface HibFieldSchemaVersionElement<R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion, SC extends HibFieldSchemaElement<R, RM, SC, SCV>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>>
	extends HibCoreElement, Comparable<SCV> {

	/**
	 * Return the schema name.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Set the schema name.
	 * 
	 * @param name
	 */
	void setName(String name);

	/**
	 * Return the schema JSON.
	 * 
	 * @return
	 */
	String getJson();

	/**
	 * Set the schema JSON.
	 * 
	 * @param json
	 */
	void setJson(String json);

	/**
	 * Return the schema version.
	 * 
	 * @return
	 */
	String getVersion();

	/**
	 * Return the stored schema rest model.
	 * 
	 * @return
	 */
	RM getSchema();

	/**
	 * Set the schema REST model.
	 * 
	 * @param schema
	 */
	void setSchema(RM schema);

	/**
	 * Delete the version.
	 */
	void deleteElement();

	// Version chain

	void setPreviousVersion(SCV version);

	void setNextVersion(SCV version);

	// Changes

	/**
	 * Return the next version of the change. This only applies when this version has a next version.
	 * 
	 * @return Next change or null when this is the current schema version which no further versions
	 */
	HibSchemaChange<?> getNextChange();

	/**
	 * Return the previous change of the version.
	 * 
	 * @return
	 */
	HibSchemaChange<?> getPreviousChange();

	/**
	 * Set the previous change that is linked to the version.
	 * 
	 * @param change
	 */
	void setPreviousChange(HibSchemaChange<?> change);

	/**
	 * Set the next change that is linked to the version.
	 * 
	 * @param change
	 */
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
