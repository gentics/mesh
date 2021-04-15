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

import com.gentics.mesh.core.field.number.NumberFieldTestHelper;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class NumberFieldMigrationTest extends AbstractFieldMigrationTest implements NumberFieldTestHelper {

	@Test
	@Override
	public void testRemove() throws Exception {
		removeField(CREATENUMBER, FILL, FETCH);
	}

	@Test
	@Override
	public void testChangeToBinary() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToBinary() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToBoolean() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});

		changeType(CREATENUMBER, FILL1, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBoolean(name).getBoolean()).as(NEWFIELDVALUE).isEqualTo(true);
		});

		changeType(CREATENUMBER, FILL0, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBoolean(name).getBoolean()).as(NEWFIELDVALUE).isEqualTo(false);
		});
	}

	@Test
	@Override
	public void testEmptyChangeToBoolean() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToBooleanList() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});

		changeType(CREATENUMBER, FILL1, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBooleanList(name).getValues()).as(NEWFIELDVALUE).containsExactly(true);
		});

		changeType(CREATENUMBER, FILL0, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBooleanList(name).getValues()).as(NEWFIELDVALUE).containsExactly(false);
		});
	}

	@Test
	@Override
	public void testEmptyChangeToBooleanList() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToDate() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNotNull();
			// Internally values are stored as miliseconds
			assertThat(container.getDate(name).getDate()).as(NEWFIELDVALUE).isEqualTo(NUMBERVALUE * 1000);
		});
	}

	@Test
	@Override
	public void testEmptyChangeToDate() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToDateList() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getDateList(name).getValues()).as(NEWFIELDVALUE).containsExactly((long) NUMBERVALUE * 1000);
		});
	}

	@Test
	@Override
	public void testEmptyChangeToDateList() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToHtml() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHtml(name).getHTML()).as(NEWFIELDVALUE).isEqualTo(Long.toString(NUMBERVALUE));
		});
	}

	@Test
	@Override
	public void testEmptyChangeToHtml() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToHtmlList() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHTMLList(name).getValues()).as(NEWFIELDVALUE).containsExactly(Long.toString(NUMBERVALUE));
		});
	}

	@Test
	@Override
	public void testEmptyChangeToHtmlList() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToMicronode() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToMicronode() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToMicronodeList() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToMicronodeList() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNode() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNode() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNodeList() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNodeList() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNumber() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumber(name).getNumber()).as(NEWFIELDVALUE).isEqualTo(NUMBERVALUE);
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNumber() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNumberList() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumberList(name).getValues()).as(NEWFIELDVALUE).containsExactly(NUMBERVALUE);
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNumberList() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToString() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getString(name).getString()).as(NEWFIELDVALUE).isEqualTo(Long.toString(NUMBERVALUE));
		});
	}

	@Test
	@Override
	public void testEmptyChangeToString() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToStringList() throws Exception {
		changeType(CREATENUMBER, FILL, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getStringList(name).getValues()).as(NEWFIELDVALUE).containsExactly(Long.toString(NUMBERVALUE));
		});
	}

	@Test
	@Override
	public void testEmptyChangeToStringList() throws Exception {
		changeType(CREATENUMBER, NOOP, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNull();
		});
	}
}
