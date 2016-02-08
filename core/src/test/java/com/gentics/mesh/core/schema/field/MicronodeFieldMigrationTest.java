package com.gentics.mesh.core.schema.field;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;

public class MicronodeFieldMigrationTest extends AbstractFieldMigrationTest {

	private final DataProvider FILL = (container, name) -> {
		MicronodeGraphField field = container.createMicronode(name, microschemaContainers().get("vcard"));

		Micronode micronode = field.getMicronode();
		micronode.createString("firstName").setString("Donald");
		micronode.createString("lastName").setString("Duck");
	};

	private static final FieldFetcher FETCH = (container, name) -> container.getMicronode(name);

	@Override
	@Test
	public void testRemove() throws IOException {
		removeField(CREATEMICRONODE, FILL, FETCH);
	}

	@Override
	@Test
	public void testRename() throws IOException {
		renameField(CREATEMICRONODE, FILL, FETCH, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getMicronode(name).getMicronode()).as(NEWFIELDVALUE)
					.containsStringField("firstName", "Donald").containsStringField("lastName", "Duck");
		});
	}

	@Override
	@Test
	public void testChangeToBinary() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBoolean() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBooleanList() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDate() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDateList() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtml() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtmlList() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToMicronode() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getMicronode(name).getMicronode()).as(NEWFIELDVALUE)
					.containsStringField("firstName", "Donald").containsStringField("lastName", "Duck");
		});
	}

	@Override
	@Test
	public void testChangeToMicronodeList() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getMicronodeList(name).getValues()).as(NEWFIELDVALUE).hasSize(1);
			assertThat(container.getMicronodeList(name).getValues().get(0)).as(NEWFIELDVALUE)
					.containsStringField("firstName", "Donald").containsStringField("lastName", "Duck");
		});
	}

	@Override
	@Test
	public void testChangeToNode() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNodeList() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumber() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumberList() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToString() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToStringList() throws IOException {
		changeType(CREATEMICRONODE, FILL, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNull();
		});
	}
}
