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
import static com.gentics.mesh.core.field.FieldTestHelper.NOOP;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.binary.BinaryFieldTestHelper;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.RxUtil;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

@MeshTestSetting(testSize = FULL, startServer = false)
public class BinaryFieldMigrationTest extends AbstractFieldMigrationTest implements BinaryFieldTestHelper {

	String hash;

	/**
	 * Creates a new binary field in the given container
	 */
	final DataProvider FILL = (container, name) -> {
		Buffer buffer = Buffer.buffer(FILECONTENTS);
		hash = FileUtils.hash(buffer).blockingGet();
		Binaries binaries = mesh().binaries();

		// Check whether the binary could already be found
		Binary binary = binaries.findByHash(hash).runInExistingTx(Tx.get());
		boolean store = false;
		if (binary == null) {
			binary = binaries.create(hash, 1L).runInExistingTx(Tx.get());
			store = true;
		}
		BinaryGraphField field = container.createBinary(name, binary);
		field.setFileName(FILENAME);
		field.setMimeType(MIMETYPE);

		// Only store the file data if we need to
		if (store == true) {
			BinaryStorage storage = meshDagger().binaryStorage();
			String tmpId = UUIDUtil.randomUUID();
			storage.storeInTemp(Flowable.just(buffer), buffer.length(), tmpId).blockingAwait();
			storage.moveInPlace(binary.getUuid(), tmpId).blockingAwait();
		}
	};

	@Test
	@Override
	public void testRemove() throws Exception {
		removeField(CREATEBINARY, FILL, FETCH);
	}

	@Test
	@Override
	public void testChangeToBinary() throws Exception {
		changeType(CREATEBINARY, FILL, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBinary(name).getFileName()).as(NEWFIELDVALUE).isEqualTo(FILENAME);
			assertThat(container.getBinary(name).getMimeType()).as(NEWFIELDVALUE).isEqualTo(MIMETYPE);
			assertThat(container.getBinary(name).getBinary().getSHA512Sum()).as(NEWFIELDVALUE).isEqualTo(hash);
			Buffer contents = RxUtil.readEntireData(container.getBinary(name).getBinary().getStream()).blockingGet();
			assertThat(contents.toString()).as(NEWFIELDVALUE).isEqualTo(FILECONTENTS);
		});
	}

	@Test
	@Override
	public void testEmptyChangeToBinary() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
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
	public void testEmptyChangeToBoolean() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATEBOOLEAN, (container, name) -> {
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
	public void testEmptyChangeToBooleanList() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATEBOOLEANLIST, (container, name) -> {
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
	public void testEmptyChangeToDate() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATEDATE, (container, name) -> {
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
	public void testEmptyChangeToDateList() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATEDATELIST, (container, name) -> {
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
	public void testEmptyChangeToHtml() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATEHTML, (container, name) -> {
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

	@Override
	public void testEmptyChangeToHtmlList() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATEHTMLLIST, (container, name) -> {
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
	public void testEmptyChangeToMicronode() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATEMICRONODE, (container, name) -> {
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
	public void testEmptyChangeToMicronodeList() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATEMICRONODELIST, (container, name) -> {
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
	public void testEmptyChangeToNode() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATENODE, (container, name) -> {
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
	public void testEmptyChangeToNodeList() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATENODELIST, (container, name) -> {
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
	public void testEmptyChangeToNumber() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATENUMBER, (container, name) -> {
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
	public void testEmptyChangeToNumberList() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATENUMBERLIST, (container, name) -> {
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
	public void testEmptyChangeToString() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATESTRING, (container, name) -> {
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
	public void testEmptyChangeToStringList() throws Exception {
		changeType(CREATEBINARY, NOOP, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNull();
		});
	}

}
