package com.gentics.mesh.core.schema.field;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import java.io.IOException;

import org.junit.Test;

public class NodeFieldMigrationTest extends AbstractFieldMigrationTest {
	private final DataProvider FILL = (container, name) -> container.createNode(name, folder("2015"));

	private static final FieldFetcher FETCH = (container, name) -> container.getNode(name);

	@Override
	@Test
	public void testRemove() throws IOException {
		removeField(CREATENODE, FILL, FETCH);
	}

	@Override
	@Test
	public void testRename() throws IOException {
		renameField(CREATENODE, FILL, FETCH, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNode(name).getNode()).as(NEWFIELDVALUE).isEqualTo(folder("2015"));
		});
	}

	@Override
	@Test
	public void testChangeToBinary() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBoolean() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBooleanList() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDate() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDateList() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtml() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtmlList() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToMicronode() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToMicronodeList() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNode() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNode(name).getNode()).as(NEWFIELDVALUE).isEqualTo(folder("2015"));
		});
	}

	@Override
	@Test
	public void testChangeToNodeList() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNodeList(name).getValues()).as(NEWFIELDVALUE).containsExactly(folder("2015"));
		});
	}

	@Override
	@Test
	public void testChangeToNumber() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumberList() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToString() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToStringList() throws IOException {
		changeType(CREATENODE, FILL, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNull();
		});
	}
}
