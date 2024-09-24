package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;

import jakarta.persistence.Entity;

import com.gentics.mesh.core.data.branch.HibBranchMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.database.HibernateTx;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Branch Microschema version edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "branch_microschema_version_edge")
public class HibBranchMicroschemaVersionEdgeImpl extends AbstractHibBranchSchemaVersion<HibMicroschemaVersionImpl> implements HibBranchMicroschemaVersion, Serializable {

	private static final long serialVersionUID = -6414277073271044788L;

	public HibBranchMicroschemaVersionEdgeImpl() {
		super();
	}

	public HibBranchMicroschemaVersionEdgeImpl(HibernateTx tx, HibBranchImpl branch, HibMicroschemaVersionImpl version) {
		super(tx, branch, version);
	}

	@Override
	public HibMicroschemaVersion getMicroschemaContainerVersion() {
		return getVersion();
	}
}
