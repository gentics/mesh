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
import static com.gentics.mesh.util.DateUtils.toISO8601;
import static org.assertj.core.api.Assertions.assertThat;

import javax.script.ScriptException;

import org.junit.Test;

import com.gentics.mesh.core.field.date.DateFieldTestHelper;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class DateFieldMigrationTest extends AbstractFieldMigrationTest implements DateFieldTestHelper {

	@Override
	@Test
	public void testRemove() throws Exception {
		removeField(CREATEDATE, FILL, FETCH);
	}

	@Override
	@Test
	public void testRename() throws Exception {
		renameField(CREATEDATE, FILL, FETCH, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getDate(name).getDate()).as(NEWFIELDVALUE).isEqualTo(DATEVALUE);
		});
	}

	@Override
	@Test
	public void testChangeToBinary() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBoolean() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBooleanList() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDate() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getDate(name).getDate()).as(NEWFIELDVALUE).isEqualTo(DATEVALUE);
		});
	}

	@Override
	@Test
	public void testChangeToDateList() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getDateList(name).getValues()).as(NEWFIELDVALUE).containsExactly(DATEVALUE);
		});
	}

	@Override
	@Test
	public void testChangeToHtml() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHtml(name).getHTML()).as(NEWFIELDVALUE).isEqualTo(toISO8601(DATEVALUE));
		});
	}

	@Override
	@Test
	public void testChangeToHtmlList() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHTMLList(name).getValues()).as(NEWFIELDVALUE).containsExactly(toISO8601(DATEVALUE));
		});
	}

	@Override
	@Test
	public void testChangeToMicronode() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToMicronodeList() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNode() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNodeList() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumber() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumber(name).getNumber()).as(NEWFIELDVALUE).isEqualTo(DATEVALUE);
		});
	}

	@Override
	@Test
	public void testChangeToNumberList() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumberList(name).getValues()).as(NEWFIELDVALUE).containsExactly(DATEVALUE);
		});
	}

	@Test
	@Override
	public void testChangeToString() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getString(name).getString()).as(NEWFIELDVALUE).isEqualTo(toISO8601(DATEVALUE));
		});
	}

	@Test
	@Override
	public void testChangeToStringList() throws Exception {
		changeType(CREATEDATE, FILL, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getStringList(name).getValues()).as(NEWFIELDVALUE).containsExactly(toISO8601(DATEVALUE));
		});
	}

	@Override
	@Test(expected = ClassNotFoundException.class)
	public void testSystemExit() throws Throwable {
		invalidMigrationScript(CREATEDATE, FILL, KILLERSCRIPT);
	}
}
