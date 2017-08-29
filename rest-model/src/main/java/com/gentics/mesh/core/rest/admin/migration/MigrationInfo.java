package com.gentics.mesh.core.rest.admin.migration;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Model for the migration status information.
 */
public class MigrationInfo implements RestModel, Comparable<MigrationInfo> {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the migration. The id can be used to identify a certain migration.")
	private String uuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The date when the migration was started.")
	private String startDate;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The date when the was completed or failed.")
	private String stopDate;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The name of the source element (e.g.: schema, microschema or release name) which was used to start the migration.")
	private String sourceName;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The uuid of the source element.")
	private String sourceUuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The type of migration (e.g.: node, micronode or release migration)")
	private MigrationType type;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The name of the Gentics Mesh node which ran the migration.")
	private String nodeName;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The amount of elements which have already been processed.")
	private int done;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The amount of elements which need to be processed.")
	private int total;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The target version of the element which was used to start the migration.")
	private String targetVersion;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The source version of the element which was used to start the migration.")
	private String sourceVersion;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The current migration status.")
	private MigrationStatus status;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The error output.")
	private String error;

	public MigrationInfo() {
	}

	/**
	 * Create a new migration info.
	 * 
	 * @param type
	 *            Type of the migration
	 * @param startDate
	 *            Start date of the migration
	 * @param nodeName
	 *            Name of the node on which the migration is running
	 */
	public MigrationInfo(MigrationType type, String startDate, String nodeName) {
		this.uuid = UUIDUtil.randomUUID();
		this.type = type;
		this.startDate = startDate;
		this.status = MigrationStatus.STARTING;
		this.nodeName = nodeName;
	}

	/**
	 * Return the uuid of the migration.
	 * 
	 * @return
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the uuid of the migration.
	 * 
	 * @param uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Return the date when the migration was started.
	 * 
	 * @return
	 */
	public String getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date of the migration.
	 * 
	 * @param startDate
	 * @return
	 */
	public MigrationInfo setStartDate(String startDate) {
		this.startDate = startDate;
		return this;
	}

	/**
	 * Return the stop date of the miration.
	 * 
	 * @return
	 */
	public String getStopDate() {
		return stopDate;
	}

	/**
	 * Set the date when the migration stopped due to completion or due to error.
	 * 
	 * @param stopDate
	 */
	public void setStopDate(String stopDate) {
		this.stopDate = stopDate;
	}

	/**
	 * Return the type of the migration. (e.g.: node, release, micronode migration)
	 * 
	 * @return
	 */
	public MigrationType getType() {
		return type;
	}

	/**
	 * Set the type of migration.
	 * 
	 * @param type
	 * @return
	 */
	public MigrationInfo setType(MigrationType type) {
		this.type = type;
		return this;
	}

	/**
	 * Return the current migration status.
	 * 
	 * @return
	 */
	public MigrationStatus getStatus() {
		return status;
	}

	/**
	 * Set the current migration status.
	 * 
	 * @param status
	 * @return
	 */
	public MigrationInfo setStatus(MigrationStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * Return the source element uuid (e.g: schemaUuid, releaseUuid).
	 * 
	 * @return
	 */
	public String getSourceUuid() {
		return sourceUuid;
	}

	/**
	 * Set the source element uuid.
	 * 
	 * @param sourceUuid
	 * @return
	 */
	public MigrationInfo setSourceUuid(String sourceUuid) {
		this.sourceUuid = sourceUuid;
		return this;
	}

	/**
	 * Return the name of the schema, release which will be used to run the migration.
	 * 
	 * @return
	 */
	public String getSourceName() {
		return sourceName;
	}

	/**
	 * Set the name of the schema or release which will be used to run the migration.
	 * 
	 * @param sourceName
	 * @return
	 */
	public MigrationInfo setSourceName(String sourceName) {
		this.sourceName = sourceName;
		return this;
	}

	/**
	 * Return the source version of the schema or microschema which was used in the migration.
	 * 
	 * @return
	 */
	public String getSourceVersion() {
		return sourceVersion;
	}

	/**
	 * Set the source version of the schema or microschema.
	 * 
	 * @param sourceVersion
	 * @return
	 */
	public MigrationInfo setSourceVersion(String sourceVersion) {
		this.sourceVersion = sourceVersion;
		return this;
	}

	/**
	 * Return the target version of the schema or microschema which was used in the migration.
	 * 
	 * @return
	 */
	public String getTargetVersion() {
		return targetVersion;
	}

	/**
	 * Set the target version of the schema or microschema.
	 * 
	 * @param targetVersion
	 * @return
	 */
	public MigrationInfo setTargetVersion(String targetVersion) {
		this.targetVersion = targetVersion;
		return this;
	}

	/**
	 * Return the total amount of elements which will be processed.
	 * 
	 * @return
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * Set the total amount of elements which will be processed.
	 * 
	 * @param total
	 * @return
	 */
	public MigrationInfo setTotal(int total) {
		this.total = total;
		return this;
	}

	/**
	 * Return the count of elements which have been processed.
	 * 
	 * @return
	 */
	public int getDone() {
		return done;
	}

	/**
	 * Set the count of elements which have been processed.
	 * 
	 * @param done
	 * @return
	 */
	public MigrationInfo setDone(int done) {
		this.done = done;
		return this;
	}

	/**
	 * Return the node name on which the migration is running.
	 * 
	 * @return
	 */
	public String getNodeName() {
		return nodeName;
	}

	/**
	 * Set the id of the mesh node on which the migration is running.
	 * 
	 * @param nodeName
	 * @return
	 */
	public MigrationInfo setNodeName(String nodeName) {
		this.nodeName = nodeName;
		return this;
	}

	/**
	 * Increment the counter for completed elements.
	 * 
	 * @return
	 */
	public MigrationInfo incDone() {
		this.done++;
		return this;
	}

	/**
	 * Return the error output.
	 * 
	 * @return
	 */
	public String getError() {
		return error;
	}

	/**
	 * Set the error output.
	 * 
	 * @param error
	 */
	public void setError(String error) {
		this.error = error;
	}

	@Override
	public String toString() {
		return getType() + " on {" + getStartDate() + "} @ {" + getNodeName() + "} with status {" + getStatus() + "} - " + getDone() + "/"
				+ getTotal();
	}

	@Override
	public int compareTo(MigrationInfo o) {
		long t1 = OffsetDateTime.parse(getStartDate()).toEpochSecond();
		long t2 = OffsetDateTime.parse(o.getStartDate()).toEpochSecond();
		return Long.compare(t1, t2);
	}

}
