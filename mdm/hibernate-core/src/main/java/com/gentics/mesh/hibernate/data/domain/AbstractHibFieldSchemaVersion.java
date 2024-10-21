package com.gentics.mesh.hibernate.data.domain;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.database.connector.QueryUtils;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Common part for the container version entity.
 * 
 * @author plyhun
 *
 * @param <R> container entity
 * @param <RM> container version entity
 * @param <RE> container reference
 * @param <SC> container item entity
 * @param <SCV> container item version entity
 */
@MappedSuperclass
public abstract class AbstractHibFieldSchemaVersion<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>
		> extends AbstractHibBaseElement implements HibFieldSchemaVersionElement<R, RM, RE, SC, SCV> {
	
	protected static final String EMPTY_JSON = "{}";

	private String version;
	private String name;
	private Boolean noIndex;

	@Column(length = QueryUtils.DEFAULT_STRING_LENGTH)
	private String schemaJson = EMPTY_JSON;

	@Override
	public Boolean getNoIndex() {
		return noIndex;
	}

	@Override
	public void setNoIndex(Boolean noIndex) {
		this.noIndex = noIndex;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getJson() {
		return schemaJson;
	}

	@Override
	public void setJson(String json) {
		this.schemaJson = StringUtils.isBlank(json) ? EMPTY_JSON : json;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString() + ", json: " + schemaJson;
	}
}
