package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;

import jakarta.persistence.Entity;

import com.gentics.mesh.core.data.branch.HibBranchSchemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.database.HibernateTx;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Branch schema version edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "branch_schema_version_edge")
public class HibBranchSchemaVersionEdgeImpl extends AbstractHibBranchSchemaVersion<HibSchemaVersionImpl> implements HibBranchSchemaVersion, Serializable {

	private static final long serialVersionUID = 5299748263133922913L;

	public HibBranchSchemaVersionEdgeImpl() {
		super();
	}

	public HibBranchSchemaVersionEdgeImpl(HibernateTx tx, HibBranchImpl branch, HibSchemaVersionImpl version) {
		super(tx, branch, version);
	}

	@Override
	public HibSchemaVersion getSchemaContainerVersion() {
		return getVersion();
	}
}
