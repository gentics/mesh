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
import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

import com.gentics.mesh.core.field.string.StringFieldTestHelper;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class StringFieldMigrationTest extends AbstractFieldMigrationTest implements StringFieldTestHelper {

	@Test
	@Override
	public void testRemove() throws Exception {
		removeField(CREATESTRING, FILLTEXT, FETCH);
	}

	@Test
	@Override
	public void testChangeToBinary() throws Exception {
		changeType(CREATESTRING, FILLTEXT, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToBinary() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToBoolean() throws Exception {
		changeType(CREATESTRING, FILLTRUE, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBoolean(name).getBoolean()).as(NEWFIELDVALUE).isEqualTo(true);
		});

		changeType(CREATESTRING, FILLFALSE, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBoolean(name).getBoolean()).as(NEWFIELDVALUE).isEqualTo(false);
		});

		changeType(CREATESTRING, FILL1, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBoolean(name).getBoolean()).as(NEWFIELDVALUE).isEqualTo(true);
		});

		changeType(CREATESTRING, FILL0, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBoolean(name).getBoolean()).as(NEWFIELDVALUE).isEqualTo(false);
		});

		changeType(CREATESTRING, FILLTEXT, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToBoolean() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToBooleanList() throws Exception {
		changeType(CREATESTRING, FILLTRUE, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBooleanList(name).getValues()).as(NEWFIELDVALUE).containsExactly(true);
		});

		changeType(CREATESTRING, FILLFALSE, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBooleanList(name).getValues()).as(NEWFIELDVALUE).containsExactly(false);
		});

		changeType(CREATESTRING, FILL1, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBooleanList(name).getValues()).as(NEWFIELDVALUE).containsExactly(true);
		});

		changeType(CREATESTRING, FILL0, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBooleanList(name).getValues()).as(NEWFIELDVALUE).containsExactly(false);
		});

		changeType(CREATESTRING, FILLTEXT, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToBooleanList() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToDate() throws Exception {
		changeType(CREATESTRING, FILL_DATE, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getDate(name).getDate()).as(NEWFIELDVALUE).isEqualTo(fromISO8601(EXAMPLE_DATE));
		});

		changeType(CREATESTRING, FILL1, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNotNull();
			// Internally timestamp with miliseconds is stored
			assertThat(container.getDate(name).getDate()).as(NEWFIELDVALUE).isEqualTo(1000L);
		});

		changeType(CREATESTRING, FILL0, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getDate(name).getDate()).as(NEWFIELDVALUE).isEqualTo(0L);
		});

		changeType(CREATESTRING, FILLTEXT, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToDate() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToDateList() throws Exception {
		changeType(CREATESTRING, FILL0, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getDateList(name).getValues()).as(NEWFIELDVALUE).containsExactly(0L);
		});

		changeType(CREATESTRING, FILL1, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNotNull();
			// Internally timestamps are stored with miliseconds
			assertThat(container.getDateList(name).getValues()).as(NEWFIELDVALUE).containsExactly(1000L);
		});

		changeType(CREATESTRING, FILLTEXT, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToDateList() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToHtml() throws Exception {
		changeType(CREATESTRING, FILLTEXT, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHtml(name).getHTML()).as(NEWFIELDVALUE).isEqualTo("<b>HTML</b> content");
		});
	}

	@Override
	@Test
	public void testEmptyChangeToHtml() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtmlList() throws Exception {
		changeType(CREATESTRING, FILLTEXT, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHTMLList(name).getValues()).as(NEWFIELDVALUE)
				.containsExactly("<b>HTML</b> content");
		});
	}

	@Override
	@Test
	public void testEmptyChangeToHtmlList() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToMicronode() throws Exception {
		changeType(CREATESTRING, FILLTEXT, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToMicronode() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToMicronodeList() throws Exception {
		changeType(CREATESTRING, FILLTEXT, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToMicronodeList() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNode() throws Exception {
		changeType(CREATESTRING, FILLTEXT, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNode() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNodeList() throws Exception {
		changeType(CREATESTRING, FILLTEXT, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNodeList() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNumber() throws Exception {
		changeType(CREATESTRING, FILL0, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumber(name).getNumber().longValue()).as(NEWFIELDVALUE).isEqualTo(0L);
		});

		changeType(CREATESTRING, FILL1, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumber(name).getNumber().longValue()).as(NEWFIELDVALUE).isEqualTo(1L);
		});

		changeType(CREATESTRING, FILLTEXT, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNumber() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNumberList() throws Exception {
		changeType(CREATESTRING, FILL0, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumberList(name).getValues()).hasSize(1);
			assertThat(new BigDecimal(container.getNumberList(name).getValues().get(0).intValue())).as(NEWFIELDVALUE).isEqualTo(BigDecimal.ZERO);
		});

		changeType(CREATESTRING, FILL1, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumberList(name).getValues()).hasSize(1);
			assertThat(new BigDecimal(container.getNumberList(name).getValues().get(0).intValue())).as(NEWFIELDVALUE).isEqualTo(BigDecimal.ONE);
		});

		changeType(CREATESTRING, FILLTEXT, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testEmptyChangeToNumberList() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToString() throws Exception {
		changeType(CREATESTRING, FILLTEXT, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getString(name).getString()).as(NEWFIELDVALUE).isEqualTo("<b>HTML</b> content");
		});
	}

	@Test
	@Override
	public void testEmptyChangeToString() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToStringList() throws Exception {
		changeType(CREATESTRING, FILLTEXT, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getStringList(name).getValues()).as(NEWFIELDVALUE)
				.containsExactly("<b>HTML</b> content");
		});
	}

	@Test
	@Override
	public void testEmptyChangeToStringList() throws Exception {
		changeType(CREATESTRING, NOOP, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNull();
		});
	}

}
