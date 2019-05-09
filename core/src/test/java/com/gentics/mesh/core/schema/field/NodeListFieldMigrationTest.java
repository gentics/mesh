package com.gentics.mesh.core.schema.field;

import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.node.NodeListFieldTestHelper;
import com.gentics.mesh.test.context.MeshTestSetting;
import org.junit.Test;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
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
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class NodeListFieldMigrationTest extends AbstractFieldMigrationTest implements NodeListFieldTestHelper {
	private final DataProvider FILL = (container, name) -> {
		NodeGraphFieldList field = container.createNodeList(name);
		field.createNode("0", folder("2015"));
		field.createNode("1", folder("news"));
	};

	@Test
	@Override
	public void testRemove() throws Exception {
		removeField(CREATENODELIST, FILL, FETCH);
	}

	@Test
	@Override
	public void testChangeToBinary() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToBoolean() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToBooleanList() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getBooleanList(name).getValues()).as(NEWFIELDVALUE).isEmpty();
		});
	}

	@Test
	@Override
	public void testChangeToDate() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToDateList() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getDateList(name).getValues()).as(NEWFIELDVALUE).isEmpty();
		});
	}

	@Test
	@Override
	public void testChangeToHtml() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToHtmlList() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getHTMLList(name).getValues()).as(NEWFIELDVALUE).isEmpty();
		});
	}

	@Test
	@Override
	public void testChangeToMicronode() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToMicronodeList() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNode() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNode(name).getNode()).as(NEWFIELDVALUE).isEqualTo(folder("2015"));
		});
	}

	@Test
	@Override
	public void testChangeToNodeList() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNodeList(name).getValues()).as(NEWFIELDVALUE).containsExactly(folder("2015"), folder("news"));
		});
	}

	@Test
	@Override
	public void testChangeToNumber() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToNumberList() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getNumberList(name).getValues()).as(NEWFIELDVALUE).isEmpty();
		});
	}

	@Test
	@Override
	public void testChangeToString() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNull();
		});
	}

	@Test
	@Override
	public void testChangeToStringList() throws Exception {
		changeType(CREATENODELIST, FILL, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getStringList(name).getValues()).as(NEWFIELDVALUE).isEmpty();
		});
	}

}
