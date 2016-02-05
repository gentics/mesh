package com.gentics.mesh.core.schema.field;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;

public class BinaryFieldMigrationTest extends AbstractFieldMigrationTest {
	private static final DataProvider FILL = (container, name) -> {
		BinaryGraphField field = container.createBinary(name);
		field.setFileName("blume.jpg");
		field.setMimeType("image/jpg");
		field.setSHA512Sum(
				"6a793cf1c7f6ef022ba9fff65ed43ddac9fb9c2131ffc4eaa3f49212244c0d4191ae5877b03bd50fd137bd9e5a16799da4a1f2846f0b26e3d956c4d8423004cc");
	};

	private static FieldFetcher FETCH = (container, name) -> container.getBinary(name);

	private static DataAsserter ASSERT = (container, name) -> {
		assertThat(container.getBoolean(name)).as("New field").isNull();
		assertThat(container.getBinary(name)).as("Old field").isNull();
	};

	@Override
	@Test
	public void testRemove() throws IOException {
		removeField(CREATEBINARY, FILL, FETCH);
	}

	@Override
	public void testRemoveList() throws IOException {
	}

	@Override
	public void testChangeToBinary() throws IOException {
	}

	@Override
	@Test
	public void testChangeToBoolean() throws IOException {
		changeType(CREATEBINARY, FILL, CREATEBOOLEAN, ASSERT);
	}

	@Override
	public void testChangeToBooleanList() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToDate() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToDateList() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToHtml() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToHtmlList() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToMicronode() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToMicronodeList() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToNode() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToNodeList() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToNumber() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToNumberList() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToString() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testChangeToStringList() throws IOException {
		// TODO Auto-generated method stub

	}

}
