package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.UpdateMicroschemaChange;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = FULL, startServer = false)
public class UpdateMicroschemaChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (Tx tx = tx()) {
			PersistingMicroschemaDao microschemaDao = tx.<CommonTx>unwrap().microschemaDao();
			Microschema microschema = microschemaDao.createPersisted(null, m -> {
				m.setName(m.getUuid());
			});
			MicroschemaVersion version = microschemaDao.createPersistedVersion(microschema, v -> {});
			UpdateMicroschemaChange change = (UpdateMicroschemaChange) microschemaDao.createPersistedChange(version, SchemaChangeOperation.UPDATEMICROSCHEMA);
			change.setDescription("test");
			assertEquals("test", change.getDescription());
		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = tx()) {
			PersistingMicroschemaDao microschemaDao = tx.<CommonTx>unwrap().microschemaDao();
			Microschema microschema = microschemaDao.createPersisted(null, m -> {
				m.setName(m.getUuid());
			});
			MicroschemaVersion version = microschemaDao.createPersistedVersion(microschema, v -> {});
			
			MicroschemaModelImpl schema = new MicroschemaModelImpl();

			UpdateMicroschemaChange change = (UpdateMicroschemaChange) microschemaDao.createPersistedChange(version, SchemaChangeOperation.UPDATEMICROSCHEMA);
			change.setName("updated");
			change.setIndexOptions(new JsonObject().put("key", "value"));
			version.setSchema(schema);
			version.setNextChange(change);

			MicroschemaModel updatedSchema = mutator.apply(version);
			assertEquals("updated", updatedSchema.getName());
			assertEquals("value", updatedSchema.getElasticsearch().getString("key"));

			change = (UpdateMicroschemaChange) microschemaDao.createPersistedChange(version, SchemaChangeOperation.UPDATEMICROSCHEMA);
			change.setDescription("text");
			version.setNextChange(change);
			updatedSchema = mutator.apply(version);
			assertEquals("text", updatedSchema.getDescription());
		}
	}

	@Test
	@Override
	public void testUpdateFromRest() {
		try (Tx tx = tx()) {
			PersistingMicroschemaDao microschemaDao = tx.<CommonTx>unwrap().microschemaDao();
			Microschema microschema = microschemaDao.createPersisted(null, m -> {
				m.setName(m.getUuid());
			});
			MicroschemaVersion version = microschemaDao.createPersistedVersion(microschema, v -> {});
			
			SchemaChangeModel model = SchemaChangeModel.createUpdateMicroschemaChange();
			model.setProperty(SchemaChangeModel.NAME_KEY, "someName");

			UpdateMicroschemaChange change = (UpdateMicroschemaChange) microschemaDao.createChange(version, model);
			change.updateFromRest(model);
			assertEquals("someName", change.getName());
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = tx()) {
			PersistingMicroschemaDao microschemaDao = tx.<CommonTx>unwrap().microschemaDao();
			Microschema microschema = microschemaDao.createPersisted(null, m -> {
				m.setName(m.getUuid());
			});
			MicroschemaVersion version = microschemaDao.createPersistedVersion(microschema, v -> {});
			UpdateMicroschemaChange change = (UpdateMicroschemaChange) microschemaDao.createPersistedChange(version, SchemaChangeOperation.UPDATEMICROSCHEMA);
			change.setName("vcard");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("vcard", model.getProperty(SchemaChangeModel.NAME_KEY));
		}
	}

}
