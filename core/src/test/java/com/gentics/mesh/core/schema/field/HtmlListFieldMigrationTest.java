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

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.field.html.HtmlListFieldHelper;

public class HtmlListFieldMigrationTest extends AbstractFieldMigrationTest implements HtmlListFieldHelper {

	@Override
	@Test
	public void testRemove() throws Exception {
		removeField(CREATEHTMLLIST, FILLTEXT, FETCH);
	}

	@Override
	@Test
	public void testRename() throws Exception {
		renameField(CREATEHTMLLIST, FILLTEXT, FETCH, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHTMLList(name).getValues()).as(NEWFIELDVALUE).containsExactly(TEXT1, TEXT2, TEXT3);
		});
	}

	@Override
	@Test
	public void testChangeToBinary() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
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

	@Override
	@Test
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

	@Override
	@Test
	public void testChangeToDate() throws Exception {
		changeType(CREATEHTMLLIST, FILLNUMBERS, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getDate(name).getDate()).as(NEWFIELDVALUE).isEqualTo(1L);
		});

		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDateList() throws Exception {
		changeType(CREATEHTMLLIST, FILLNUMBERS, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getDateList(name).getValues()).as(NEWFIELDVALUE).containsExactly(1L, 0L);
		});

		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtml() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHtml(name).getHTML()).as(NEWFIELDVALUE).isEqualTo(TEXT1 + "," + TEXT2 + "," + TEXT3);
		});
	}

	@Override
	@Test
	public void testChangeToHtmlList() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHTMLList(name).getValues()).as(NEWFIELDVALUE).containsExactly(TEXT1, TEXT2, TEXT3);
		});
	}

	@Override
	@Test
	public void testChangeToMicronode() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToMicronodeList() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNode() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNodeList() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumber() throws Exception {
		changeType(CREATEHTMLLIST, FILLNUMBERS, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumber(name).getNumber()).as(NEWFIELDVALUE).isEqualTo(1L);
		});

		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});

	}

	@Override
	@Test
	public void testChangeToNumberList() throws Exception {
		changeType(CREATEHTMLLIST, FILLNUMBERS, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumberList(name).getValues()).as(NEWFIELDVALUE).containsExactly(1L, 0L);
		});

		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToString() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getString(name).getString()).as(NEWFIELDVALUE).isEqualTo(TEXT1 + "," + TEXT2 + "," + TEXT3);
		});
	}

	@Override
	@Test
	public void testChangeToStringList() throws Exception {
		changeType(CREATEHTMLLIST, FILLTEXT, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getStringList(name).getValues()).as(NEWFIELDVALUE).containsExactly(TEXT1, TEXT2, TEXT3);
		});
	}

	@Override
	@Test
	public void testCustomMigrationScript() throws Exception {
		customMigrationScript(CREATEHTMLLIST, FILLTEXT, FETCH,
				"function migrate(node, fieldname, convert) {node.fields[fieldname].reverse(); return node;}", (container, name) -> {
					HtmlGraphFieldList field = container.getHTMLList(name);
					assertThat(field).as(NEWFIELD).isNotNull();
					field.reload();
					assertThat(field.getValues()).as(NEWFIELDVALUE).containsExactly(TEXT3, TEXT2, TEXT1);
				});
	}

	@Override
	@Test(expected = ExecutionException.class)
	public void testInvalidMigrationScript() throws Exception {
		invalidMigrationScript(CREATEHTMLLIST, FILLTEXT, INVALIDSCRIPT);
	}

	@Override
	@Test(expected = ExecutionException.class)
	public void testSystemExit() throws Exception {
		invalidMigrationScript(CREATEHTMLLIST, FILLTEXT, KILLERSCRIPT);
	}
}
