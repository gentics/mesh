package com.gentics.mesh.core.schema.field;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;

public class BooleanListFieldMigrationTest extends AbstractFieldMigrationTest {
	private static final DataProvider FILL = (container, name) -> {
		BooleanGraphFieldList field = container.createBooleanList(name);
		field.createBoolean(true);
		field.createBoolean(false);
	};

	private static final FieldFetcher FETCH = (container, name) -> container.getBooleanList(name);

	@Override
	@Test
	public void testRemove() {
		removeField(CREATEBOOLEANLIST, FILL, FETCH);
	}

	@Override
	@Test
	public void testRename() {
		renameField(CREATEBOOLEANLIST, FILL, FETCH, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBooleanList(name).getValues()).as(NEWFIELDVALUE).containsExactly(true, false);
		});
	}

	@Override
	@Test
	public void testChangeToBinary() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBoolean() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBoolean(name).getBoolean()).as(NEWFIELDVALUE).isEqualTo(true);
		});
	}

	@Override
	@Test
	public void testChangeToBooleanList() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBooleanList(name).getValues()).as(NEWFIELDVALUE).containsExactly(true, false);
		});
	}

	@Override
	@Test
	public void testChangeToDate() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDateList() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtml() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHtml(name).getHTML()).as(NEWFIELDVALUE).isEqualTo("true,false");
		});
	}

	@Override
	@Test
	public void testChangeToHtmlList() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHTMLList(name).getValues()).as(NEWFIELDVALUE).containsExactly("true", "false");
		});
	}

	@Override
	@Test
	public void testChangeToMicronode() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToMicronodeList() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNode() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNodeList() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumber() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumber(name).getNumber()).as(NEWFIELDVALUE).isEqualTo(1L);
		});
	}

	@Override
	@Test
	public void testChangeToNumberList() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumberList(name).getValues()).as(NEWFIELDVALUE).containsExactly(1L, 0L);
		});
	}

	@Override
	@Test
	public void testChangeToString() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getString(name).getString()).as(NEWFIELDVALUE).isEqualTo("true,false");
		});
	}

	@Override
	@Test
	public void testChangeToStringList() {
		changeType(CREATEBOOLEANLIST, FILL, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getStringList(name).getValues()).as(NEWFIELDVALUE).containsExactly("true", "false");
		});
	}

	@Override
	@Test
	public void testCustomMigrationScript() {
		customMigrationScript(CREATEBOOLEANLIST, FILL, FETCH, "function migrate(node, fieldname, convert) {node.fields[fieldname].reverse(); return node;}", (container, name) -> {
			BooleanGraphFieldList field = container.getBooleanList(name);
			assertThat(field).as(NEWFIELD).isNotNull();
			field.reload();
			assertThat(field.getValues()).as(NEWFIELDVALUE).containsExactly(false, true);
		});
	}

	@Override
	@Test
	public void testInvalidMigrationScript() {
		invalidMigrationScript(CREATEBOOLEANLIST, FILL);
	}
}
