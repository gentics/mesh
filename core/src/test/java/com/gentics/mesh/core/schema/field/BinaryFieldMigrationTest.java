package com.gentics.mesh.core.schema.field;

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
import static org.assertj.core.api.Assertions.assertThat;

import javax.script.ScriptException;

import org.junit.Test;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.binary.BinaryFieldTestHelper;

import io.vertx.rxjava.core.buffer.Buffer;

public class BinaryFieldMigrationTest extends AbstractFieldMigrationTest implements BinaryFieldTestHelper {

	String sha512Sum;

	final DataProvider FILL = (container, name) -> {
		BinaryGraphField field = container.createBinary(name);
		field.setFileName(FILENAME);
		field.setMimeType(MIMETYPE);
		sha512Sum = meshDagger.nodeFieldAPIHandler().hashAndStoreBinaryFile(Buffer.buffer(FILECONTENTS), field.getUuid(), field.getSegmentedPath()).toBlocking()
				.value();
		field.setSHA512Sum(sha512Sum);
	};

	@Test
	@Override
	public void testRemove() throws Exception {
		removeField(CREATEBINARY, FILL, FETCH);
	}

	@Test
	@Override
	public void testRename() throws Exception {
		renameField(CREATEBINARY, FILL, FETCH, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBinary(name).getFileName()).as(NEWFIELDVALUE).isEqualTo(FILENAME);
			assertThat(container.getBinary(name).getMimeType()).as(NEWFIELDVALUE).isEqualTo(MIMETYPE);
			assertThat(container.getBinary(name).getSHA512Sum()).as(NEWFIELDVALUE).isEqualTo(sha512Sum);
			container.getBinary(name).getFileBuffer().setHandler((h) -> {
				assertThat(h.succeeded()).isTrue();
				assertThat(h.result().toString()).as(NEWFIELDVALUE).isEqualTo(FILECONTENTS);
			});
		});
	}

	@Test
	@Override
	public void testChangeToBinary() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBinary(name).getFileName()).as(NEWFIELDVALUE).isEqualTo(FILENAME);
			assertThat(container.getBinary(name).getMimeType()).as(NEWFIELDVALUE).isEqualTo(MIMETYPE);
			assertThat(container.getBinary(name).getSHA512Sum()).as(NEWFIELDVALUE).isEqualTo(sha512Sum);
			container.getBinary(name).getFileBuffer().setHandler((h) -> {
				assertThat(h.succeeded()).isTrue();
				assertThat(h.result().toString()).as(NEWFIELDVALUE).isEqualTo(FILECONTENTS);
			});
		});
	}

	@Test
	@Override
	public void testChangeToBoolean() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToBooleanList() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToDate() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToDateList() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToHtml() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToHtmlList() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToMicronode() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToMicronodeList() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNode() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNodeList() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNumber() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNumberList() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToString() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToStringList() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testCustomMigrationScript() throws Exception {
		customMigrationScript(CREATEBINARY, FILL, FETCH,
				"function migrate(node, fieldname, convert) {node.fields[fieldname].fileName = 'bla' + node.fields[fieldname].fileName; return node;}",
				(container, name) -> {
					BinaryGraphField newField = container.getBinary(name);
					assertThat(newField).as(NEWFIELD).isNotNull();
					((BinaryGraphFieldImpl) newField).reload();
					assertThat(newField.getFileName()).as(NEWFIELDVALUE).isEqualTo("bla" + FILENAME);
					assertThat(newField.getMimeType()).as(NEWFIELDVALUE).isEqualTo(MIMETYPE);
					assertThat(newField.getSHA512Sum()).as(NEWFIELDVALUE).isEqualTo(sha512Sum);
					newField.getFileBuffer().setHandler((h) -> {
						assertThat(h.succeeded()).isTrue();
						assertThat(h.result().toString()).as(NEWFIELDVALUE).isEqualTo(FILECONTENTS);
					});
				});
	}

	@Override
	@Test(expected = ScriptException.class)
	public void testInvalidMigrationScript() throws Throwable {
		invalidMigrationScript(CREATEBINARY, FILL, INVALIDSCRIPT);
	}

	@Override
	@Test(expected = ClassNotFoundException.class)
	public void testSystemExit() throws Throwable {
		invalidMigrationScript(CREATEBINARY, FILL, KILLERSCRIPT);
	}
}
