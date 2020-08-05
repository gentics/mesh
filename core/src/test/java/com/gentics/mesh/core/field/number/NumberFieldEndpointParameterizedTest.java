package com.gentics.mesh.core.field.number;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.test.Assert;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class NumberFieldEndpointParameterizedTest extends AbstractNumberFieldEndpointTest {

	@Parameterized.Parameters(name = "{index}: {1}")
	public static Collection<Object> paramData() {
		return Arrays.asList(new Object[][] {
			{ 1.0, "Float with 0 decimal places" },
			{ Integer.MIN_VALUE, "Int min" },
			{ Integer.MAX_VALUE, "Int max" },
			{ Long.MIN_VALUE, "Long min" },
			{ Long.MAX_VALUE, "Long max" },
			{ (long) Integer.MIN_VALUE - 1, "Int min -1" },
			{ (long) Integer.MAX_VALUE + 1, "Int max +1" },
			{ 42L, "Small Long" },
			{ 42, "Small Int" },
			{ Float.MIN_VALUE, "Float min" },
			{ Float.MAX_VALUE, "Float max" },
			{ (double) Float.MIN_VALUE - 0.1, "Float min +0.1" },
			{ (double) Float.MAX_VALUE + 0.1, "Float max +0.1" },
			{ 1.34256f, "Float >3 decimal places" },
			{ 100.9f, "Small Float" },
			{ Double.MIN_VALUE, "Double min" },
			{ Double.MAX_VALUE, "Double max" },
			{ 234.4353453456536d, "Double small" },
			{ 241212.34235243543534534535345d, "Double >3 decimal places" }
		});
	}

	@Parameterized.Parameter
	public Number num;

	@Parameterized.Parameter(1)
	public String testName;

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			NodeResponse firstResponse = updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(this.num));
			String oldNumber = firstResponse.getVersion();

			NodeResponse secondResponse = updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(this.num));
			assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldNumber);
		}
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNodeWithField();
			NumberFieldImpl numberField = response.getFields().getNumberField(FIELD_NAME);
			Assert.assertNumberValueEquals(this.num, numberField.getNumber());
		}
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		Node node = folder("2015");
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			NumberGraphField numberField = container.createNumber(FIELD_NAME);
			numberField.setNumber(this.num);
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse response = readNode(node);
			NumberFieldImpl deserializedNumberField = response.getFields().getNumberField(FIELD_NAME);
			assertNotNull(deserializedNumberField);
			Assert.assertNumberValueEquals(this.num, deserializedNumberField.getNumber());
		}
	}

	@Ignore
	@Override
	public void testUpdateNodeFieldWithField() throws IOException {

	}

	@Override
	@Ignore
	public void testUpdateSetNull() {

	}

	@Override
	@Ignore
	public void testUpdateSetEmpty() {

	}

	@Override
	@Ignore
	public void testCreateNodeWithNoField() {

	}

	@Override
	public NodeResponse createNodeWithField() {
		return createNode(FIELD_NAME, new NumberFieldImpl().setNumber(this.num));
	}

}
