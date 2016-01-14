package com.gentics.mesh.core.field.microschema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaImpl;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.MeshJsonException;

/**
 * Test cases for fields of type "micronode"
 */
public class MicronodeGraphFieldTest extends AbstractBasicDBTest {
	/**
	 * Dummy microschema
	 */
	protected MicroschemaContainer dummyMicroschema;

	@Override
	public void setup() throws Exception {
		super.setup();
		dummyMicroschema = createDummyMicroschema();
	}

	//	@Autowired
	//	private ServerSchemaStorage schemaStorage;

	@Test
	@Ignore("Not yet implemented")
	public void testMicroschemaFieldTransformation() {

	}

	/**
	 * Test creation of field
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateMicronodeField() throws Exception {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

		MicronodeGraphField field = container.createMicronode("testMicronodeField", dummyMicroschema);
		assertNotNull(field);
		Micronode micronode = field.getMicronode();
		assertNotNull(micronode);
		assertTrue("Micronode must have a uuid", StringUtils.isNotEmpty(micronode.getUuid()));

		StringGraphField micronodeStringField = micronode.createString("stringfield");
		assertNotNull(micronodeStringField);
		micronodeStringField.setString("dummyString");

		MicronodeGraphField reloadedField = container.getMicronode("testMicronodeField");
		assertNotNull(reloadedField);
		Micronode reloadedMicronode = reloadedField.getMicronode();
		assertNotNull(reloadedMicronode);
		assertEquals(micronode.getUuid(), reloadedMicronode.getUuid());

		StringGraphField reloadedMicronodeStringField = reloadedMicronode.getString("stringfield");
		assertNotNull(reloadedMicronodeStringField);

		assertEquals(micronodeStringField.getString(), reloadedMicronodeStringField.getString());
	}

	/**
	 * Test updating the field
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateMicronodeField() throws Exception {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

		MicronodeGraphField field = container.createMicronode("testMicronodeField", dummyMicroschema);
		Micronode micronode = field.getMicronode();
		String originalUuid = micronode.getUuid();

		Set<? extends MicronodeImpl> existingMicronodes = tx.getGraph().v().has(MicronodeImpl.class).toSetExplicit(MicronodeImpl.class);
		for (Micronode foundMicronode : existingMicronodes) {
			assertEquals(micronode.getUuid(), foundMicronode.getUuid());
		}

		// update by recreation
		MicronodeGraphField updatedField = container.createMicronode("testMicronodeField", dummyMicroschema);
		Micronode updatedMicronode = updatedField.getMicronode();

		assertFalse("Uuid of micronode must be different after update", StringUtils.equalsIgnoreCase(originalUuid, updatedMicronode.getUuid()));

		existingMicronodes = tx.getGraph().v().has(MicronodeImpl.class).toSetExplicit(MicronodeImpl.class);
		for (MicronodeImpl foundMicronode : existingMicronodes) {
			assertEquals(updatedMicronode.getUuid(), foundMicronode.getUuid());
		}
	}

	/**
	 * Create a dummy microschema
	 * 
	 * @return
	 * @throws MeshJsonException
	 */
	protected MicroschemaContainer createDummyMicroschema() throws MeshJsonException {
		Microschema dummyMicroschema = new MicroschemaImpl();
		dummyMicroschema.setName("dummymicroschema");

		// firstname field
		StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
		stringFieldSchema.setName("stringfield");
		stringFieldSchema.setLabel("String Field");
		dummyMicroschema.addField(stringFieldSchema);

		return boot.microschemaContainerRoot().create(dummyMicroschema, getRequestUser());
	}
}
