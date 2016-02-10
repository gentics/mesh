package com.gentics.mesh.core.schema.field;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.verticle.node.NodeFieldAPIHandler;

import io.vertx.rxjava.core.buffer.Buffer;

public class BinaryFieldMigrationTest extends AbstractFieldMigrationTest {
	@Autowired
	private NodeFieldAPIHandler nodeFieldAPIHandler;

	private final static String FILECONTENTS = "This is the file contents";

	private final static String FILENAME = "test.txt";

	private final static String MIMETYPE = "text/plain";

	private String sha512Sum;

	private final DataProvider FILL = (container, name) -> {
		BinaryGraphField field = container.createBinary(name);
		field.setFileName(FILENAME);
		field.setMimeType(MIMETYPE);
		sha512Sum = nodeFieldAPIHandler.hashAndStoreBinaryFile(Buffer.buffer(FILECONTENTS), field.getUuid(), field.getSegmentedPath()).toBlocking().last();
		field.setSHA512Sum(sha512Sum);
	};

	private static FieldFetcher FETCH = (container, name) -> container.getBinary(name);

	@Override
	@Test
	public void testRemove() {
		removeField(CREATEBINARY, FILL, FETCH);
	}

	@Override
	@Test
	public void testRename() {
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

	@Override
	@Test
	public void testChangeToBinary() {
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

	@Override
	@Test
	public void testChangeToBoolean() {
		changeType(CREATEBINARY, FILL, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBooleanList() {
		changeType(CREATEBINARY, FILL, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDate() {
		changeType(CREATEBINARY, FILL, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDateList() {
		changeType(CREATEBINARY, FILL, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtml() {
		changeType(CREATEBINARY, FILL, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtmlList() {
		changeType(CREATEBINARY, FILL, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToMicronode() {
		changeType(CREATEBINARY, FILL, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToMicronodeList() {
		changeType(CREATEBINARY, FILL, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNode() {
		changeType(CREATEBINARY, FILL, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNodeList() {
		changeType(CREATEBINARY, FILL, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumber() {
		changeType(CREATEBINARY, FILL, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumberList() {
		changeType(CREATEBINARY, FILL, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToString() {
		changeType(CREATEBINARY, FILL, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToStringList() {
		changeType(CREATEBINARY, FILL, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testCustomMigrationScript() {
		customMigrationScript(CREATEBINARY, FILL, FETCH, "function migrate(node, fieldname, convert) {node.fields[fieldname].fileName = 'bla' + node.fields[fieldname].fileName; return node;}", (container, name) -> {
			BinaryGraphField newField = container.getBinary(name);
			assertThat(newField).as(NEWFIELD).isNotNull();
			((BinaryGraphFieldImpl)newField).reload();
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
	@Test
	public void testInvalidMigrationScript() {
		invalidMigrationScript(CREATEBINARY, FILL);
	}
}
