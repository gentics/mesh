package com.gentics.mesh.core;

import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.contentoperation.DynamicContentColumn;
import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.AbstractHibFieldSchemaVersion;
import com.gentics.mesh.test.MeshTestActions;

/**
 * Hibernate implementations for {@link MeshTestActions}.
 * 
 * @author plyhun
 *
 */
class HibernateMeshTestActions implements MeshTestActions {

	HibernateMeshTestActions() {
	}

	@Override
	public <SCV extends FieldSchemaVersionElement<?, ?, ?, ?, ?>> SCV updateSchemaVersion(SCV version) {
		AbstractHibFieldSchemaVersion<?,?,?,?,?> scv = AbstractHibFieldSchemaVersion.class.cast(version);
		scv.getSchema().getFields().forEach(field -> {
			DynamicContentColumn dynamicContentColumn = new DynamicContentColumn(field);
			ContentStorage contentStorage = HibernateTx.get().data().getContentStorage();
			contentStorage.addColumnIfNotExists(scv, dynamicContentColumn);
		});
		return version;
	}
}
