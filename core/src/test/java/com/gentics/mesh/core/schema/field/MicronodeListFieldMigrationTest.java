package com.gentics.mesh.core.schema.field;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;

public class MicronodeListFieldMigrationTest extends AbstractFieldMigrationTest {
	private final DataProvider FILL = (container, name) -> {
		MicronodeGraphFieldList field = container.createMicronodeFieldList(name);

		Micronode micronode = field.createMicronode(new MicronodeResponse());
		micronode.setMicroschemaContainer(microschemaContainers().get("vcard"));
		micronode.createString("firstname").setString("Donald");
		micronode.createString("lastname").setString("Duck");

		micronode = field.createMicronode(new MicronodeResponse());
		micronode.setMicroschemaContainer(microschemaContainers().get("vcard"));
		micronode.createString("firstname").setString("Mickey");
		micronode.createString("lastname").setString("Mouse");
	};

	private static final FieldFetcher FETCH = (container, name) -> container.getMicronodeList(name);

	@Override
	@Test
	public void testRemove() throws IOException {
		removeField(CREATEMICRONODELIST, FILL, FETCH);
	}

	@Override
	@Test
	public void testChangeToBinary() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBoolean() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBooleanList() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDate() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDateList() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtml() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtmlList() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToMicronode() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getMicronode(name).getMicronode()).as(NEWFIELDVALUE)
					.containsStringField("firstname", "Donald").containsStringField("lastname", "Duck");
		});
	}

	@Override
	public void testChangeToMicronodeList() throws IOException {
	}

	@Override
	@Test
	public void testChangeToNode() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNodeList() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumber() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumberList() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToString() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToStringList() throws IOException {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNull();
		});
	}
}
