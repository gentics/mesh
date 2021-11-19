package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibUpdateMicroschemaChange;
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
			HibMicroschema microschema = microschemaDao.createPersisted(null);
			HibMicroschemaVersion version = microschemaDao.createPersistedVersion(microschema);
			HibUpdateMicroschemaChange change = (HibUpdateMicroschemaChange) microschemaDao.createPersistedChange(version, SchemaChangeOperation.UPDATEMICROSCHEMA);
			change.setDescription("test");
			assertEquals("test", change.getDescription());
		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = tx()) {
			PersistingMicroschemaDao microschemaDao = tx.<CommonTx>unwrap().microschemaDao();
			HibMicroschema microschema = microschemaDao.createPersisted(null);
			HibMicroschemaVersion version = microschemaDao.createPersistedVersion(microschema);
			
			MicroschemaModelImpl schema = new MicroschemaModelImpl();

			HibUpdateMicroschemaChange change = (HibUpdateMicroschemaChange) microschemaDao.createPersistedChange(version, SchemaChangeOperation.UPDATEMICROSCHEMA);
			change.setName("updated");
			change.setIndexOptions(new JsonObject().put("key", "value"));
			version.setSchema(schema);
			version.setNextChange(change);

			MicroschemaModel updatedSchema = mutator.apply(version);
			assertEquals("updated", updatedSchema.getName());
			assertEquals("value", updatedSchema.getElasticsearch().getString("key"));

			change = (HibUpdateMicroschemaChange) microschemaDao.createPersistedChange(version, SchemaChangeOperation.UPDATEMICROSCHEMA);
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
			HibMicroschema microschema = microschemaDao.createPersisted(null);
			HibMicroschemaVersion version = microschemaDao.createPersistedVersion(microschema);
			
			SchemaChangeModel model = SchemaChangeModel.createUpdateMicroschemaChange();
			model.setProperty(SchemaChangeModel.NAME_KEY, "someName");

			HibUpdateMicroschemaChange change = (HibUpdateMicroschemaChange) microschemaDao.createChange(version, model);
			change.updateFromRest(model);
			assertEquals("someName", change.getName());
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = tx()) {
			PersistingMicroschemaDao microschemaDao = tx.<CommonTx>unwrap().microschemaDao();
			HibMicroschema microschema = microschemaDao.createPersisted(null);
			HibMicroschemaVersion version = microschemaDao.createPersistedVersion(microschema);
			HibUpdateMicroschemaChange change = (HibUpdateMicroschemaChange) microschemaDao.createPersistedChange(version, SchemaChangeOperation.UPDATEMICROSCHEMA);
			change.setName("vcard");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("vcard", model.getProperty(SchemaChangeModel.NAME_KEY));
		}
	}

}
