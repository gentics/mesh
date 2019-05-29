package com.gentics.mesh.core.schema.field;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATEBINARY;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATEBOOLEAN;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATEBOOLEANLIST;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATEDATE;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATEDATELIST;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATEHTML;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATEHTMLLIST;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATEMICRONODE;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATEMICRONODELIST;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATENODE;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATENODELIST;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATENUMBER;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATENUMBERLIST;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATESTRING;
import static com.gentics.mesh.core.field.FieldSchemaCreator.CREATESTRINGLIST;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import javax.script.ScriptException;

import org.junit.Test;

import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.node.NodeFieldTestHelper;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class NodeFieldMigrationTest extends AbstractFieldMigrationTest implements NodeFieldTestHelper {

	final DataProvider FILL = (container, name) -> container.createNode(name, folder("2015"));

	@Test
	@Override
	public void testRemove() throws Exception {
		removeField(CREATENODE, FILL, FETCH);
	}

	@Test
	@Override
	public void testRename() throws Exception {
		renameField(CREATENODE, FILL, FETCH, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNode(name).getNode()).as(NEWFIELDVALUE).isEqualTo(folder("2015"));
		});
	}

	@Test
	@Override
	public void testChangeToBinary() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToBoolean() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToBooleanList() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToDate() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToDateList() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToHtml() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToHtmlList() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToMicronode() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToMicronodeList() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNode() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNode(name).getNode()).as(NEWFIELDVALUE).isEqualTo(folder("2015"));
		});
	}

	@Test
	@Override
	public void testChangeToNodeList() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNodeList(name).getValues()).as(NEWFIELDVALUE).containsExactly(folder("2015"));
		});
	}

	@Test
	@Override
	public void testChangeToNumber() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNumberList() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToString() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToStringList() throws Exception {
		changeType(CREATENODE, FILL, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testCustomMigrationScript() throws Exception {
		String uuid = db().tx(() -> folder("news").getUuid());
		customMigrationScript(CREATENODE, FILL, FETCH,
				"function migrate(node, fieldname) {node.fields[fieldname].uuid = '" + uuid + "'; return node;}", (container, name) -> {
					assertThat(container.getNode(name)).as(NEWFIELD).isNotNull();
					assertThat(container.getNode(name).getNode()).as(NEWFIELDVALUE).isEqualTo(folder("news"));
				});
	}

	@Override
	@Test(expected = ScriptException.class)
	public void testInvalidMigrationScript() throws Throwable {
		invalidMigrationScript(CREATENODE, FILL, INVALIDSCRIPT);
	}

	@Override
	@Test(expected = ClassNotFoundException.class)
	public void testSystemExit() throws Throwable {
		invalidMigrationScript(CREATENODE, FILL, KILLERSCRIPT);
	}
}
