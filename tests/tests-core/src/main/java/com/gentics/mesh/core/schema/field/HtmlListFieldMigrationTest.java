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

import com.gentics.mesh.core.field.html.HtmlListFieldHelper;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class HtmlListFieldMigrationTest extends AbstractFieldMigrationTest implements HtmlListFieldHelper {

	@Test
	@Override
	public void testRemove() throws Exception {
		removeField(CREATEHTMLLIST, FILLTEXT, FETCH);
	}

	@Test
	@Override
	public void testChangeToBinary() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToBinary() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToBoolean() throws Exception {
		changeType(CREATEHTMLLIST, FILLTRUEFALSE, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBoolean(name).getBoolean()).as(NEWFIELDVALUE).isEqualTo(true);
		});

		changeType(CREATEHTMLLIST, FILLNUMBERS, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBoolean(name).getBoolean()).as(NEWFIELDVALUE).isEqualTo(true);
		});

		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToBoolean() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToBooleanList() throws Exception {
		changeType(CREATEHTMLLIST, FILLTRUEFALSE, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBooleanList(name).getValues()).as(NEWFIELDVALUE).containsExactly(true, false);
		});

		changeType(CREATEHTMLLIST, FILLNUMBERS, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBooleanList(name).getValues()).as(NEWFIELDVALUE).containsExactly(true, false);
		});

		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToBooleanList() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToDate() throws Exception {
		changeType(CREATEHTMLLIST, FILLNUMBERS, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNotNull();
			// Internally timestamps are stored in miliseconds
			assertThat(container.getDate(name).getDate()).as(NEWFIELDVALUE).isEqualTo(1000L);
		});

		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToDate() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToDateList() throws Exception {
		changeType(CREATEHTMLLIST, FILLNUMBERS, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNotNull();
			// Internally timestamps are stored in miliseconds
			assertThat(container.getDateList(name).getValues()).as(NEWFIELDVALUE).containsExactly(1000L, 0L);
		});

		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToDateList() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToHtml() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHtml(name).getHTML()).as(NEWFIELDVALUE).isEqualTo(TEXT1 + "," + TEXT2 + "," + TEXT3);
		});
	}

	@Test
	@Override
	public void testEmptyChangeToHtml() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNull();
		});

	}

	@Test
	@Override
	public void testChangeToHtmlList() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHTMLList(name).getValues()).as(NEWFIELDVALUE).containsExactly(TEXT1, TEXT2, TEXT3);
		});
	}

	@Test
	@Override
	public void testEmptyChangeToHtmlList() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToMicronode() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToMicronode() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToMicronodeList() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToMicronodeList() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNode() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNode() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNodeList() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNodeList() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNumber() throws Exception {
		changeType(CREATEHTMLLIST, FILLNUMBERS, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumber(name).getNumber()).as(NEWFIELDVALUE).isEqualTo(1L);
		});

		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNumber() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNumberList() throws Exception {
		changeType(CREATEHTMLLIST, FILLNUMBERS, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumberList(name).getValues()).as(NEWFIELDVALUE).containsExactly(1L, 0L);
		});

		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNumberList() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToString() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getString(name).getString()).as(NEWFIELDVALUE).isEqualTo(TEXT1 + "," + TEXT2 + "," + TEXT3);
		});
	}

	@Test
	@Override
	public void testEmptyChangeToString() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNull();
		});

	}

	@Test
	@Override
	public void testChangeToStringList() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getStringList(name).getValues()).as(NEWFIELDVALUE).containsExactly(TEXT1, TEXT2, TEXT3);
		});
	}

	@Test
	@Override
	public void testEmptyChangeToStringList() throws Exception {
		changeType(CREATEHTMLLIST, NOOP, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNull();
		});
	}

}
