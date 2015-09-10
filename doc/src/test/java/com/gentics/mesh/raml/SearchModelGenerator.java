package com.gentics.mesh.raml;

import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.basic.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.basic.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.nesting.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.test.DummySearchProvider;
import com.gentics.mesh.util.UUIDUtil;

public class SearchModelGenerator {

	public static void main(String[] args) throws Exception {
		new SearchModelGenerator().run();
	}

	@Test
	public void testGenerator() throws Exception {
		run();
	}

	private void run() throws Exception {

		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringNoDBConfiguration.class)) {
			ctx.start();
			ctx.registerShutdownHook();

			Language language = mock(Language.class);
			when(language.getLanguageTag()).thenReturn("de");

			User user = getMockedUser();
			Tag tagA = getMockedTag("green", user);
			Tag tagB = getMockedTag("red", user);

			Node parentNode = getMockedNodeBasic("folder");
			Node node = getMockedNode(parentNode, user, language, tagA, tagB);

			NodeIndexHandler nodeIndexHandler = ctx.getBean(NodeIndexHandler.class);
			CountDownLatch latch = new CountDownLatch(1);
			nodeIndexHandler.store(node, "node", rh -> {
				latch.countDown();
			});
			failingLatch(latch);

			DummySearchProvider provider = ctx.getBean("dummySearchProvider", DummySearchProvider.class);
			for (Entry<String, Map<String, Object>> entry : provider.getStoreEvents().entrySet()) {
				Map<String, Object> outputMap = new TreeMap<>();
				flatten(entry.getValue(), outputMap, null);

				JSONObject json = new JSONObject(outputMap);
				System.out.println(json.toString(4));

			}
		}

	}

	private Node getMockedNodeBasic(String schemaType) {
		Node node = mock(NodeImpl.class);
		when(node.getUuid()).thenReturn(UUIDUtil.randomUUID());
		SchemaContainer schemaContainer = getMockedSchemaContainer(schemaType);
		when(node.getSchemaContainer()).thenReturn(schemaContainer);
		return node;
	}

	private void flatten(Map<String, Object> map, Map<String, Object> output, String key) throws JSONException {
		String prefix = "";
		if (key != null) {
			prefix = key + ".";
		}
		for (Entry<String, Object> entry : map.entrySet()) {
			String currentKey = prefix + entry.getKey();
			if (entry.getValue() instanceof Map) {
				flatten((Map<String, Object>) entry.getValue(), output, prefix + entry.getKey());
			} else if (entry.getValue() instanceof List) {
				output.put(currentKey, entry.getValue());
			} else if (entry.getValue() instanceof String) {
				output.put(currentKey, entry.getValue());
			}
		}
	}

	private User getMockedUser() {
		User user = mock(UserImpl.class);
		when(user.getUuid()).thenReturn(UUIDUtil.randomUUID());
		return user;
	}

	private Tag getMockedTag(String name, User user) {
		Tag tag = mock(TagImpl.class);
		when(tag.getCreator()).thenReturn(user);
		when(tag.getEditor()).thenReturn(user);
		when(tag.getName()).thenReturn(name);
		when(tag.getUuid()).thenReturn(UUIDUtil.randomUUID());
		return tag;
	}

	private SchemaContainer getMockedSchemaContainer(String name) {
		SchemaContainer container = mock(SchemaContainerImpl.class);
		when(container.getName()).thenReturn(name);
		when(container.getUuid()).thenReturn(UUIDUtil.randomUUID());
		when(container.getSchema()).thenReturn(getContentSchema());
		return container;
	}

	private Schema getContentSchema() {
		Schema schema = new SchemaImpl();
		schema.setName("content");
		schema.setDescription("Content schema");
		schema.addField(new StringFieldSchemaImpl().setName("name").setRequired(true));
		return schema;
	}

	private Node getMockedNode(Node parentNode, User user, Language language, Tag tagA, Tag tagB) {
		Node node = mock(NodeImpl.class);

		when(node.getParentNode()).thenReturn(parentNode);

		List<? extends Tag> tagList = Arrays.asList(tagA, tagB);
		Mockito.<List<? extends Tag>> when(node.getTags()).thenReturn(tagList);

		SchemaContainer schemaContainer = getMockedSchemaContainer("content");
		when(node.getSchemaContainer()).thenReturn(schemaContainer);

		when(node.getCreator()).thenReturn(user);
		when(node.getEditor()).thenReturn(user);
		when(node.getUuid()).thenReturn(UUIDUtil.randomUUID());
		Schema schema = schemaContainer.getSchema();
		when(node.getSchema()).thenReturn(schema);

		NodeGraphFieldContainer container = getMockedContainer(language);
		Mockito.<List<? extends NodeGraphFieldContainer>> when(node.getGraphFieldContainers()).thenReturn(Arrays.asList(container));

		return node;
	}

	private NodeGraphFieldContainer getMockedContainer(Language language) {
		NodeGraphFieldContainer container = mock(NodeGraphFieldContainerImpl.class);
		when(container.getLanguage()).thenReturn(language);

		// String field
		StringGraphField stringField = mock(StringGraphFieldImpl.class);
		when(stringField.getString()).thenReturn("The name value");
		when(container.getString("stringField")).thenReturn(stringField);

		// Number field
		NumberGraphField numberField = mock(NumberGraphFieldImpl.class);
		when(numberField.getNumber()).thenReturn("0.146");
		when(container.getNumber("numberField")).thenReturn(numberField);

		// Date field
		DateGraphField dateField = mock(DateGraphFieldImpl.class);
		when(dateField.getDate()).thenReturn("01.01.2015 23:12:01");
		when(container.getDate("dateField")).thenReturn(dateField);

		// Boolean field
		BooleanGraphField booleanField = mock(BooleanGraphFieldImpl.class);
		when(booleanField.getBoolean()).thenReturn(true);
		when(container.getBoolean("booleanField")).thenReturn(booleanField);

		// Node field
		NodeGraphField nodeField = mock(NodeGraphFieldImpl.class);
		Node nodeRef = getMockedNodeBasic("folder");
		when(nodeField.getNode()).thenReturn(nodeRef);
		when(container.getNode("nodeField")).thenReturn(nodeField);

		// Html field
		HtmlGraphField htmlField = mock(HtmlGraphFieldImpl.class);
		when(htmlField.getHTML()).thenReturn("some<b>html");
		when(container.getHtml("content-htmlField")).thenReturn(htmlField);

		//Node List Field
		NodeGraphFieldList nodeListField = mock(NodeGraphFieldListImpl.class);
		when(container.getNodeList("nodeListField")).thenReturn(nodeListField);

		// String List Field
		StringGraphFieldList stringListField = mock(StringGraphFieldListImpl.class);
		when(container.getStringList("stringListField")).thenReturn(stringListField);

		// Date List Field
		DateGraphFieldList dateListField = mock(DateGraphFieldListImpl.class);
		when(container.getDateList("dateListField")).thenReturn(dateListField);

		// Number List Field
		NumberGraphFieldList numberListField = mock(NumberGraphFieldListImpl.class);
		when(container.getNumberList("numberListField")).thenReturn(numberListField);

		// Html List Field
		HtmlGraphFieldList htmlListField = mock(HtmlGraphFieldListImpl.class);
		Mockito.<List<? extends HtmlGraphField>> when(htmlListField.getList()).thenReturn(Arrays.asList(htmlField, htmlField, htmlField));
		when(container.getHTMLList("htmlListField")).thenReturn(htmlListField);

		//TODO add microschema and select fields

		return container;
	}
}
