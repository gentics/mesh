package com.gentics.mesh.core.field.html;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.field.AbstractGraphListFieldVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;

public class HtmlFieldListVerticleTest extends AbstractGraphListFieldVerticleTest {

	@Override
	public String getListFieldType() {
		return "html";
	}

	@Test
	public void testHtmlList() throws IOException {
		HtmlFieldListImpl listField = new HtmlFieldListImpl();
		listField.add("A");
		listField.add("B");
		listField.add("C");

		NodeResponse response = createNode(FIELD_NAME, listField);
		HtmlFieldListImpl listFromResponse = response.getFields().getHtmlFieldList(FIELD_NAME);
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	public void testUpdateNodeWithHtmlField() throws IOException {
		Node node = folder("2015");

		List<List<String>> valueCombinations = Arrays.asList(Arrays.asList("A", "B", "C"), Arrays.asList("C", "B", "A"), Collections.emptyList(),
				Arrays.asList("X", "Y"), Arrays.asList("C"));

		NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
		for (int i = 0; i < 20; i++) {
			List<String> oldValue = getListValues(container, HtmlGraphFieldListImpl.class, FIELD_NAME);
			List<String> newValue = valueCombinations.get(i % valueCombinations.size());

			// Prepare update request and update the field
			HtmlFieldListImpl list = new HtmlFieldListImpl();
			for (String value : newValue) {
				list.add(value);
			}
			System.out.println("Update to " + list);
			NodeResponse response = updateNode(FIELD_NAME, list);

			// Assert the update response
			HtmlFieldListImpl field = response.getFields().getHtmlFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());

			// Assert the update within the graph
			node.reload();
			container.reload();
			NodeGraphFieldContainer updatedContainer = container.getNextVersion();
			assertEquals("The container version number did not match up with the response version number.", updatedContainer.getVersion().toString(),
					response.getVersion().getNumber());
			assertEquals("We expected container {" + container.getVersion().toString() + "} to contain the old value.", newValue,
					getListValues(updatedContainer, HtmlGraphFieldListImpl.class, FIELD_NAME));
			//assertEquals("We expected container {" + updatedContainer.getVersion().toString() +"} to contain the old value.", oldValue, getListValues(updatedContainer, HtmlGraphFieldListImpl.class, FIELD_NAME));
			container = updatedContainer;
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		HtmlFieldListImpl list = new HtmlFieldListImpl();
		list.add("A");
		list.add("B");
		updateNode(FIELD_NAME, list);

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getHtmlFieldList(FIELD_NAME)).as("Updated Field").isNull();
	}

	@Test
	public void testUpdateSetEmpty() {
		HtmlFieldListImpl list = new HtmlFieldListImpl();
		list.add("A");
		list.add("B");
		updateNode(FIELD_NAME, list);

		NodeResponse secondResponse = updateNode(FIELD_NAME, new DateFieldListImpl());
		assertThat(secondResponse.getFields().getHtmlFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getHtmlFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
	}

}
