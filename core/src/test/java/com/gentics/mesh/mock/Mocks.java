package com.gentics.mesh.mock;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mockito.Mockito;

import com.gentics.ferma.Tx;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HandleContext;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.util.HttpQueryUtils;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public final class Mocks {

	private Mocks() {

	}

	public static Project mockProject(User user) {
		Project project = mock(ProjectImpl.class);
		when(project.getUuid()).thenReturn(randomUUID());
		when(project.getName()).thenReturn("dummyProject");
		when(project.getCreator()).thenReturn(user);
		when(project.getCreationTimestamp()).thenReturn(System.currentTimeMillis());
		when(project.getEditor()).thenReturn(user);
		when(project.getLastEditedTimestamp()).thenReturn(System.currentTimeMillis());
		return project;
	}

	public static Language mockLanguage(String code) {
		Language language = mock(LanguageImpl.class);
		when(language.getUuid()).thenReturn(randomUUID());
		when(language.getLanguageTag()).thenReturn("de");
		return language;
	}

	public static Node mockNodeBasic(String schemaType, User user) {
		Node node = mock(NodeImpl.class);
		when(node.getUuid()).thenReturn(randomUUID());
		SchemaContainer schemaContainer = mockSchemaContainer(schemaType, user);
		when(node.getSchemaContainer()).thenReturn(schemaContainer);
		return node;
	}

	public static Micronode mockMicronode(String microschemaName, User user) {
		Micronode micronode = mock(MicronodeImpl.class);
		when(micronode.getUuid()).thenReturn(randomUUID());
		MicroschemaContainer microschemaContainer = mockMicroschemaContainer(microschemaName, user);
		MicroschemaContainerVersion latestVersion = microschemaContainer.getLatestVersion();
		when(micronode.getSchemaContainerVersion()).thenReturn(latestVersion);
		MicroschemaModel microschema = microschemaContainer.getLatestVersion().getSchema();
		when(micronode.getSchemaContainerVersion().getSchema()).thenReturn(microschema);

		// longitude field
		NumberGraphField longitudeField = mock(NumberGraphFieldImpl.class);
		when(longitudeField.getNumber()).thenReturn(16.373063840833);
		when(micronode.getNumber("longitude")).thenReturn(longitudeField);

		// latitude field
		NumberGraphField latitudeField = mock(NumberGraphFieldImpl.class);
		when(latitudeField.getNumber()).thenReturn(16.373063840833);
		when(micronode.getNumber("latitude")).thenReturn(latitudeField);

		return micronode;
	}

	public static Role mockRole(String roleName, User creator) {
		Role role = mock(RoleImpl.class);
		when(role.getCreator()).thenReturn(creator);
		when(role.getCreationTimestamp()).thenReturn(System.currentTimeMillis());
		when(role.getEditor()).thenReturn(creator);
		when(role.getLastEditedTimestamp()).thenReturn(System.currentTimeMillis());
		when(role.getName()).thenReturn(roleName);
		when(role.getUuid()).thenReturn(randomUUID());
		return role;
	}

	public static Group mockGroup(String groupName, User creator) {
		Group group = mock(GroupImpl.class);
		when(group.getCreator()).thenReturn(creator);
		when(group.getCreationTimestamp()).thenReturn(System.currentTimeMillis());
		when(group.getEditor()).thenReturn(creator);
		when(group.getLastEditedTimestamp()).thenReturn(System.currentTimeMillis());
		when(group.getName()).thenReturn(groupName);
		when(group.getUuid()).thenReturn(randomUUID());
		return group;
	}

	public static User mockUser(String username, String firstname, String lastname) {
		return mockUser(username, firstname, lastname, null);
	}

	public static User mockUser(String username, String firstname, String lastname, User creator) {
		User user = mock(UserImpl.class);
		when(user.getUsername()).thenReturn(username);
		when(user.getFirstname()).thenReturn(firstname);
		when(user.getLastname()).thenReturn(lastname);
		when(user.getEmailAddress()).thenReturn(username + "@nowhere.tld");
		when(user.getUuid()).thenReturn(randomUUID());
		when(user.getCreationTimestamp()).thenReturn(System.currentTimeMillis());
		when(user.getLastEditedTimestamp()).thenReturn(System.currentTimeMillis());
		if (creator != null) {
			when(user.getCreator()).thenReturn(creator);
			when(user.getEditor()).thenReturn(creator);
		}
		return user;
	}

	public static UpdateDocumentEntry mockUpdateDocumentEntry() {
		UpdateDocumentEntry entry = mock(UpdateDocumentEntry.class);
		HandleContext context = new HandleContext();
		context.setProjectUuid(UUIDUtil.randomUUID());
		when(entry.getContext()).thenReturn(context);
		when(entry.getElementUuid()).thenReturn(randomUUID());
		return entry;
	}

	public static TagFamily mockTagFamily(String name, User user, Project project) {
		TagFamily tagFamily = mock(TagFamilyImpl.class);
		when(tagFamily.getCreator()).thenReturn(user);
		when(tagFamily.getCreationTimestamp()).thenReturn(System.currentTimeMillis());
		when(tagFamily.getEditor()).thenReturn(user);
		when(tagFamily.getLastEditedTimestamp()).thenReturn(System.currentTimeMillis());
		when(tagFamily.getName()).thenReturn(name);
		when(tagFamily.getUuid()).thenReturn(randomUUID());
		when(tagFamily.getProject()).thenReturn(project);
		return tagFamily;
	}

	public static Tag mockTag(String name, User user, TagFamily tagFamily, Project project) {
		Tag tag = mock(TagImpl.class);
		when(tag.getCreator()).thenReturn(user);
		when(tag.getCreationTimestamp()).thenReturn(System.currentTimeMillis());
		when(tag.getEditor()).thenReturn(user);
		when(tag.getLastEditedTimestamp()).thenReturn(System.currentTimeMillis());
		when(tag.getName()).thenReturn(name);
		when(tag.getUuid()).thenReturn(randomUUID());
		when(tag.getTagFamily()).thenReturn(tagFamily);
		when(tag.getProject()).thenReturn(project);
		return tag;
	}

	public static SchemaContainer mockSchemaContainer(String name, User user) {
		SchemaContainer container = mock(SchemaContainerImpl.class);
		when(container.getName()).thenReturn(name);
		when(container.getUuid()).thenReturn(randomUUID());
		SchemaContainerVersion latestVersion = mock(SchemaContainerVersionImpl.class);
		when(latestVersion.getSchemaContainer()).thenReturn(container);
		when(latestVersion.getSchema()).thenReturn(mockContentSchema());
		when(latestVersion.getName()).thenReturn(name);
		when(container.getLatestVersion()).thenReturn(latestVersion);
		when(container.getCreator()).thenReturn(user);
		when(container.getCreationTimestamp()).thenReturn(System.currentTimeMillis());
		when(container.getEditor()).thenReturn(user);
		when(container.getLastEditedTimestamp()).thenReturn(System.currentTimeMillis());
		return container;
	}

	public static MicroschemaContainer mockMicroschemaContainer(String name, User user) {
		MicroschemaContainer container = mock(MicroschemaContainerImpl.class);
		when(container.getName()).thenReturn(name);
		when(container.getUuid()).thenReturn(randomUUID());
		MicroschemaContainerVersionImpl latestVersion = mock(MicroschemaContainerVersionImpl.class);
		when(latestVersion.getSchema()).thenReturn(mockGeolocationMicroschema());

		when(container.getLatestVersion()).thenReturn(latestVersion);
		when(container.getCreator()).thenReturn(user);
		when(container.getCreationTimestamp()).thenReturn(System.currentTimeMillis());
		when(container.getEditor()).thenReturn(user);
		when(container.getLastEditedTimestamp()).thenReturn(System.currentTimeMillis());
		return container;
	}

	public static SchemaModel mockContentSchema() {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("content");
		schema.setDescription("Content schema");
		schema.setDisplayField("string");
		// basic types
		schema.addField(new StringFieldSchemaImpl().setName("string").setRequired(true));
		schema.addField(new NumberFieldSchemaImpl().setName("number").setRequired(true));
		schema.addField(new BooleanFieldSchemaImpl().setName("boolean").setRequired(true));
		schema.addField(new DateFieldSchemaImpl().setName("date").setRequired(true));
		schema.addField(new HtmlFieldSchemaImpl().setName("html").setRequired(true));
		schema.addField(new NodeFieldSchemaImpl().setName("node").setRequired(true));
		schema.addField(new MicronodeFieldSchemaImpl().setName("micronode").setRequired(true));

		// lists types
		schema.addField(new ListFieldSchemaImpl().setListType("string").setName("stringList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("number").setName("numberList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("boolean").setName("booleanList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("date").setName("dateList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("html").setName("htmlList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("node").setName("nodeList").setRequired(true));
		schema.addField(new ListFieldSchemaImpl().setListType("micronode").setName("micronodeList").setRequired(true));

		return schema;
	}

	public static MicroschemaModel mockGeolocationMicroschema() {
		MicroschemaModel microschema = new MicroschemaModelImpl();
		microschema.setName("geolocation");
		microschema.setDescription("Microschema for Geolocations");

		microschema.addField(new NumberFieldSchemaImpl().setName("longitude").setLabel("Longitude").setRequired(true));
		microschema.addField(new NumberFieldSchemaImpl().setName("latitude").setLabel("Latitude").setRequired(true));

		return microschema;
	}

	public static Node mockNode(Node parentNode, Project project, User user, Language language, Tag tagA, Tag tagB) {
		Node node = mock(NodeImpl.class);

		when(node.getParentNode(anyString())).thenReturn(parentNode);
		when(node.getProject()).thenReturn(project);

		List<? extends Tag> tagList = Arrays.asList(tagA, tagB);
		Mockito.<List<? extends Tag>> when(node.getTags(any(Release.class))).thenReturn(tagList);

		SchemaContainer schemaContainer = mockSchemaContainer("content", user);
		SchemaContainerVersion latestVersion = schemaContainer.getLatestVersion();
		when(latestVersion.getUuid()).thenReturn(randomUUID());
		when(node.getSchemaContainer()).thenReturn(schemaContainer);
		when(node.getCreator()).thenReturn(user);
		when(node.getUuid()).thenReturn(randomUUID());

		NodeGraphFieldContainer container = mockContainer(language, user);
		when(container.getSchemaContainerVersion()).thenReturn(latestVersion);
		when(container.getParentNode()).thenReturn(node);
		when(node.getLatestDraftFieldContainer(language)).thenReturn(container);
		Mockito.<List<? extends NodeGraphFieldContainer>> when(node.getDraftGraphFieldContainers()).thenReturn(Arrays.asList(container));
		return node;
	}

	public static NodeGraphFieldContainer mockContainer(Language language, User user) {
		NodeGraphFieldContainer container = mock(NodeGraphFieldContainerImpl.class);
		when(container.getLanguage()).thenReturn(language);

		when(container.getEditor()).thenReturn(user);

		// String field
		StringGraphField stringField = mock(StringGraphFieldImpl.class);
		when(stringField.getString()).thenReturn("The name value");
		when(container.getString("string")).thenReturn(stringField);

		// Number field
		NumberGraphField numberField = mock(NumberGraphFieldImpl.class);
		when(numberField.getNumber()).thenReturn(0.146f);
		when(container.getNumber("number")).thenReturn(numberField);

		// Date field
		DateGraphField dateField = mock(DateGraphFieldImpl.class);
		when(dateField.getDate()).thenReturn(System.currentTimeMillis() / 1000);
		when(container.getDate("date")).thenReturn(dateField);

		// Boolean field
		BooleanGraphField booleanField = mock(BooleanGraphFieldImpl.class);
		when(booleanField.getBoolean()).thenReturn(true);
		when(container.getBoolean("boolean")).thenReturn(booleanField);

		// Node field
		NodeGraphField nodeField = mock(NodeGraphFieldImpl.class);
		Node nodeRef = mockNodeBasic("folder", user);
		when(nodeField.getNode()).thenReturn(nodeRef);
		when(container.getNode("node")).thenReturn(nodeField);

		// Html field
		HtmlGraphField htmlField = mock(HtmlGraphFieldImpl.class);
		when(htmlField.getHTML()).thenReturn("some<b>html");
		when(container.getHtml("html")).thenReturn(htmlField);

		// micronode field
		MicronodeGraphField micronodeField = mock(MicronodeGraphFieldImpl.class);
		Micronode micronode = mockMicronode("geolocation", user);
		when(micronodeField.getMicronode()).thenReturn(micronode);
		when(container.getMicronode("micronode")).thenReturn(micronodeField);

		// Node List Field
		NodeGraphFieldList nodeListField = mock(NodeGraphFieldListImpl.class);
		Mockito.<List<? extends NodeGraphField>> when(nodeListField.getList()).thenReturn(Arrays.asList(nodeField, nodeField, nodeField));
		when(container.getNodeList("nodeList")).thenReturn(nodeListField);

		// String List Field
		StringGraphFieldList stringListField = mock(StringGraphFieldListImpl.class);
		Mockito.<List<? extends StringGraphField>> when(stringListField.getList()).thenReturn(Arrays.asList(stringField, stringField, stringField));
		when(container.getStringList("stringList")).thenReturn(stringListField);

		// Boolean List Field
		BooleanGraphFieldList booleanListField = mock(BooleanGraphFieldListImpl.class);
		Mockito.<List<? extends BooleanGraphField>> when(booleanListField.getList())
				.thenReturn(Arrays.asList(booleanField, booleanField, booleanField));
		when(container.getBooleanList("booleanList")).thenReturn(booleanListField);

		// Date List Field
		DateGraphFieldList dateListField = mock(DateGraphFieldListImpl.class);
		Mockito.<List<? extends DateGraphField>> when(dateListField.getList()).thenReturn(Arrays.asList(dateField, dateField, dateField));
		when(container.getDateList("dateList")).thenReturn(dateListField);

		// Number List Field
		NumberGraphFieldList numberListField = mock(NumberGraphFieldListImpl.class);
		Mockito.<List<? extends NumberGraphField>> when(numberListField.getList()).thenReturn(Arrays.asList(numberField, numberField, numberField));
		when(container.getNumberList("numberList")).thenReturn(numberListField);

		// Html List Field
		HtmlGraphFieldList htmlListField = mock(HtmlGraphFieldListImpl.class);
		Mockito.<List<? extends HtmlGraphField>> when(htmlListField.getList()).thenReturn(Arrays.asList(htmlField, htmlField, htmlField));
		when(container.getHTMLList("htmlList")).thenReturn(htmlListField);

		// TODO currently, this mock is only used for the search document example, where we want to omit
		// fields of type "list of micronodes". We should better add an argument to the method to specify,
		// which types of fields should be added
		// // Micronode List Field
		// MicronodeGraphFieldList micronodeListField = mock(MicronodeGraphFieldListImpl.class);
		// Mockito.<List<? extends MicronodeGraphField>> when(micronodeListField.getList()).thenReturn(Arrays.asList(micronodeField, micronodeField,
		// micronodeField));
		// when(container.getMicronodeList("micronodeList")).thenReturn(micronodeListField);

		// TODO add select fields

		return container;
	}

	public static InternalActionContext getMockedInternalActionContext(String query, User user, Project project) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(getMockedRoutingContext(query, false, user, null));
		ac.data().put(RouterStorage.PROJECT_CONTEXT_KEY, project);
		return ac;
	}
	
	public static RoutingContext getMockedRoutingContext(String query, boolean noInternalMap, User user, Project project) {
		Map<String, Object> map = new HashMap<>();
		if (noInternalMap) {
			map = null;
		}
		RoutingContext rc = mock(RoutingContext.class);
		Session session = mock(Session.class);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(request.query()).thenReturn(query);
		Map<String, String> paramMap = HttpQueryUtils.splitQuery(query);
		MultiMap paramMultiMap = MultiMap.caseInsensitiveMultiMap();
		for (Entry<String, String> entry : paramMap.entrySet()) {
			paramMultiMap.add(entry.getKey(), entry.getValue());
		}
		when(request.params()).thenReturn(paramMultiMap);
		when(request.getParam(Mockito.anyString())).thenAnswer(in -> {
			String key = (String) in.getArguments()[0];
			return paramMap.get(key);
		});
		paramMap.entrySet().stream().forEach(entry -> when(request.getParam(entry.getKey())).thenReturn(entry.getValue()));
		if (user != null) {
			MeshAuthUserImpl requestUser = Tx.getActive().getGraph().frameElement(user.getElement(), MeshAuthUserImpl.class);
			when(rc.user()).thenReturn(requestUser);
			// JsonObject principal = new JsonObject();
			// principal.put("uuid", user.getUuid());
		}
		when(rc.data()).thenReturn(map);
		MultiMap headerMap = mock(MultiMap.class);
		when(headerMap.get("Accept-Language")).thenReturn("en, en-gb;q=0.8, en;q=0.72");
		when(request.headers()).thenReturn(headerMap);
		when(rc.request()).thenReturn(request);
		when(rc.session()).thenReturn(session);

		if (project != null) {
			when(rc.get(RouterStorage.PROJECT_CONTEXT_KEY)).thenReturn(project);
		}
		return rc;

	}
}
