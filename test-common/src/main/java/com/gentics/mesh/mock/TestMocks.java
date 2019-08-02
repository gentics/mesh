package com.gentics.mesh.mock;

import static com.gentics.mesh.example.AbstractExamples.TIMESTAMP_NEW;
import static com.gentics.mesh.example.AbstractExamples.TIMESTAMP_OLD;
import static com.gentics.mesh.example.ExampleUuids.GROUP_CLIENT_UUID;
import static com.gentics.mesh.example.ExampleUuids.LANGUAGE_UUID;
import static com.gentics.mesh.example.ExampleUuids.MICROSCHEMA_UUID;
import static com.gentics.mesh.example.ExampleUuids.NODE_DELOREAN_UUID;
import static com.gentics.mesh.example.ExampleUuids.PROJECT_DEMO2_UUID;
import static com.gentics.mesh.example.ExampleUuids.PROJECT_DEMO_UUID;
import static com.gentics.mesh.example.ExampleUuids.ROLE_CLIENT_UUID;
import static com.gentics.mesh.example.ExampleUuids.SCHEMA_VEHICLE_UUID;
import static com.gentics.mesh.example.ExampleUuids.TAGFAMILY_FUELS_UUID;
import static com.gentics.mesh.example.ExampleUuids.TAG_BLUE_UUID;
import static com.gentics.mesh.example.ExampleUuids.USER_EDITOR_UUID;
import static com.gentics.mesh.example.ExampleUuids.UUID_1;
import static com.gentics.mesh.example.ExampleUuids.UUID_2;
import static com.gentics.mesh.example.ExampleUuids.UUID_3;
import static com.gentics.mesh.example.ExampleUuids.UUID_4;
import static com.gentics.mesh.example.ExampleUuids.UUID_5;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mockito.Mockito;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.context.impl.GenericEntryContextImpl;
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
import com.gentics.mesh.madl.traversal.TraversalResult;

public final class TestMocks {

	private TestMocks() {

	}

	public static Project mockProject(User user) {
		Project project = mock(Project.class);
		when(project.getUuid()).thenReturn(PROJECT_DEMO2_UUID);
		when(project.getName()).thenReturn("dummyProject");
		when(project.getCreator()).thenReturn(user);
		when(project.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(project.getEditor()).thenReturn(user);
		when(project.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(project.getRolesWithPerm(GraphPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(project.getElementVersion()).thenReturn(UUID_1);
		return project;
	}

	public static Language mockLanguage(String code) {
		Language language = mock(Language.class);
		when(language.getUuid()).thenReturn(LANGUAGE_UUID);
		when(language.getLanguageTag()).thenReturn("de");
		when(language.getElementVersion()).thenReturn(UUID_2);
		return language;
	}

	public static Node mockNodeBasic(String schemaType, User user) {
		Node node = mock(Node.class);
		when(node.getUuid()).thenReturn(NODE_DELOREAN_UUID);
		SchemaContainer schemaContainer = mockSchemaContainer(schemaType, user);
		when(node.getSchemaContainer()).thenReturn(schemaContainer);
		return node;
	}

	public static Micronode mockMicronode(String microschemaName, User user) {
		Micronode micronode = mock(Micronode.class);
		when(micronode.getUuid()).thenReturn(UUID_1);
		MicroschemaContainer microschemaContainer = mockMicroschemaContainer(microschemaName, user);
		MicroschemaContainerVersion latestVersion = microschemaContainer.getLatestVersion();
		// TODO Review
		when(micronode.getAnySchemaContainerVersion()).thenReturn(latestVersion);
		MicroschemaModel microschema = microschemaContainer.getLatestVersion().getSchema();
		when(micronode.getAnySchemaContainerVersion().getSchema()).thenReturn(microschema);

		// longitude field
		NumberGraphField longitudeField = mock(NumberGraphField.class);
		when(longitudeField.getNumber()).thenReturn(16.373063840833);
		when(micronode.getNumber("longitude")).thenReturn(longitudeField);

		// latitude field
		NumberGraphField latitudeField = mock(NumberGraphField.class);
		when(latitudeField.getNumber()).thenReturn(16.373063840833);
		when(micronode.getNumber("latitude")).thenReturn(latitudeField);
		when(micronode.getElementVersion()).thenReturn(UUID_3);
		return micronode;
	}

	public static Role mockRole(String roleName, User creator) {
		Role role = mock(Role.class);
		when(role.getCreator()).thenReturn(creator);
		when(role.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(role.getEditor()).thenReturn(creator);
		when(role.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(role.getName()).thenReturn(roleName);
		when(role.getUuid()).thenReturn(ROLE_CLIENT_UUID);
		when(role.getRolesWithPerm(GraphPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(role.getElementVersion()).thenReturn(UUID_4);
		return role;
	}

	public static Group mockGroup(String groupName, User creator) {
		Group group = mock(Group.class);
		when(group.getCreator()).thenReturn(creator);
		when(group.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(group.getEditor()).thenReturn(creator);
		when(group.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(group.getName()).thenReturn(groupName);
		when(group.getUuid()).thenReturn(GROUP_CLIENT_UUID);
		when(group.getRolesWithPerm(GraphPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(group.getElementVersion()).thenReturn(UUID_5);
		return group;
	}

	public static User mockUser(String username, String firstname, String lastname) {
		return mockUser(username, firstname, lastname, null);
	}

	public static User mockUser(String username, String firstname, String lastname, User creator) {
		User user = mock(User.class);
		when(user.getUsername()).thenReturn(username);
		when(user.getFirstname()).thenReturn(firstname);
		when(user.getLastname()).thenReturn(lastname);
		when(user.getEmailAddress()).thenReturn(username + "@nowhere.tld");
		when(user.getUuid()).thenReturn(USER_EDITOR_UUID);
		when(user.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(user.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		if (creator != null) {
			when(user.getCreator()).thenReturn(creator);
			when(user.getEditor()).thenReturn(creator);
		}
		when(user.getRolesWithPerm(GraphPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(user.getElementVersion()).thenReturn(UUID_1);
		return user;
	}

	public static UpdateDocumentEntry mockUpdateDocumentEntry() {
		UpdateDocumentEntry entry = mock(UpdateDocumentEntry.class);
		GenericEntryContextImpl context = new GenericEntryContextImpl();
		context.setProjectUuid(PROJECT_DEMO_UUID);
		when(entry.getContext()).thenReturn(context);
		when(entry.getElementUuid()).thenReturn(UUID_3);
		return entry;
	}

	public static TagFamily mockTagFamily(String name, User user, Project project) {
		TagFamily tagFamily = mock(TagFamily.class);
		when(tagFamily.getCreator()).thenReturn(user);
		when(tagFamily.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(tagFamily.getEditor()).thenReturn(user);
		when(tagFamily.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(tagFamily.getName()).thenReturn(name);
		when(tagFamily.getUuid()).thenReturn(TAGFAMILY_FUELS_UUID);
		when(tagFamily.getProject()).thenReturn(project);
		when(tagFamily.getRolesWithPerm(GraphPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(tagFamily.getElementVersion()).thenReturn(UUID_2);
		return tagFamily;
	}

	public static Tag mockTag(String name, User user, TagFamily tagFamily, Project project) {
		Tag tag = mock(Tag.class);
		when(tag.getCreator()).thenReturn(user);
		when(tag.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(tag.getEditor()).thenReturn(user);
		when(tag.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(tag.getName()).thenReturn(name);
		when(tag.getUuid()).thenReturn(TAG_BLUE_UUID);
		when(tag.getTagFamily()).thenReturn(tagFamily);
		when(tag.getProject()).thenReturn(project);
		when(tag.getRolesWithPerm(GraphPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(tag.getElementVersion()).thenReturn(UUID_3);
		return tag;
	}

	private static <T> TraversalResult<T> createEmptyTraversal() {
		return new TraversalResult<>(Collections.emptyList());
	}

	public static SchemaContainer mockSchemaContainer(String name, User user) {
		SchemaContainer container = mock(SchemaContainer.class);
		when(container.getName()).thenReturn(name);
		when(container.getUuid()).thenReturn(SCHEMA_VEHICLE_UUID);
		SchemaContainerVersion latestVersion = mock(SchemaContainerVersion.class);
		when(latestVersion.getSchemaContainer()).thenReturn(container);
		when(latestVersion.getSchema()).thenReturn(mockContentSchema());
		when(latestVersion.getName()).thenReturn(name);
		when(container.getLatestVersion()).thenReturn(latestVersion);
		when(container.getCreator()).thenReturn(user);
		when(container.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(container.getEditor()).thenReturn(user);
		when(container.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(container.getRolesWithPerm(GraphPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		return container;
	}

	public static MicroschemaContainer mockMicroschemaContainer(String name, User user) {
		MicroschemaContainer container = mock(MicroschemaContainer.class);
		when(container.getName()).thenReturn(name);
		when(container.getUuid()).thenReturn(MICROSCHEMA_UUID);
		MicroschemaContainerVersion latestVersion = mock(MicroschemaContainerVersion.class);
		when(latestVersion.getSchema()).thenReturn(mockGeolocationMicroschema());

		when(container.getLatestVersion()).thenReturn(latestVersion);
		when(container.getCreator()).thenReturn(user);
		when(container.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(container.getEditor()).thenReturn(user);
		when(container.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(container.getRolesWithPerm(GraphPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(container.getElementVersion()).thenReturn(UUID_5);
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

	public static Node mockNode(Node parentNode, Project project, User user, String languageTag, Tag tagA, Tag tagB) {
		Node node = mock(Node.class);

		when(node.getParentNode(anyString())).thenReturn(parentNode);
		when(node.getProject()).thenReturn(project);

		TraversalResult<? extends Tag> tagResult = new TraversalResult<>(Arrays.asList(tagA, tagB));
		Mockito.<TraversalResult<? extends Tag>>when(node.getTags(any(Branch.class))).thenReturn(tagResult);

		SchemaContainer schemaContainer = mockSchemaContainer("content", user);
		SchemaContainerVersion latestVersion = schemaContainer.getLatestVersion();
		when(latestVersion.getUuid()).thenReturn(UUID_2);
		when(node.getSchemaContainer()).thenReturn(schemaContainer);
		when(node.getCreator()).thenReturn(user);
		when(node.getUuid()).thenReturn(NODE_DELOREAN_UUID);
		when(node.getRolesWithPerm(GraphPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(node.getRolesWithPerm(GraphPermission.READ_PUBLISHED_PERM)).thenReturn(createEmptyTraversal());

		NodeGraphFieldContainer container = mockContainer(languageTag, user);
		// TODO Review
		when(container.getAnySchemaContainerVersion()).thenReturn(latestVersion);
		when(container.getParentNode()).thenReturn(node);
		when(container.getElementVersion()).thenReturn(UUID_5);
		when(node.getLatestDraftFieldContainer(languageTag)).thenReturn(container);
		when(node.getElementVersion()).thenReturn(UUID_4);
		Mockito.<Iterable<? extends NodeGraphFieldContainer>> when(node.getDraftGraphFieldContainers()).thenReturn(createEmptyTraversal());
		return node;
	}

	public static NodeGraphFieldContainer mockContainer(String languageTag, User user) {
		NodeGraphFieldContainer container = mock(NodeGraphFieldContainer.class);
		when(container.getLanguageTag()).thenReturn(languageTag);

		when(container.getEditor()).thenReturn(user);

		// String field
		StringGraphField stringField = mock(StringGraphField.class);
		when(stringField.getString()).thenReturn("The name value");
		when(container.getString("string")).thenReturn(stringField);

		// Number field
		NumberGraphField numberField = mock(NumberGraphField.class);
		when(numberField.getNumber()).thenReturn(0.146f);
		when(container.getNumber("number")).thenReturn(numberField);

		// Date field
		DateGraphField dateField = mock(DateGraphField.class);
		when(dateField.getDate()).thenReturn(TIMESTAMP_NEW / 1000);
		when(container.getDate("date")).thenReturn(dateField);

		// Boolean field
		BooleanGraphField booleanField = mock(BooleanGraphField.class);
		when(booleanField.getBoolean()).thenReturn(true);
		when(container.getBoolean("boolean")).thenReturn(booleanField);

		// Node field
		NodeGraphField nodeField = mock(NodeGraphField.class);
		Node nodeRef = mockNodeBasic("folder", user);
		when(nodeField.getNode()).thenReturn(nodeRef);
		when(container.getNode("node")).thenReturn(nodeField);

		// Html field
		HtmlGraphField htmlField = mock(HtmlGraphField.class);
		when(htmlField.getHTML()).thenReturn("some<b>html");
		when(container.getHtml("html")).thenReturn(htmlField);

		// micronode field
		MicronodeGraphField micronodeField = mock(MicronodeGraphField.class);
		Micronode micronode = mockMicronode("geolocation", user);
		when(micronodeField.getMicronode()).thenReturn(micronode);
		when(container.getMicronode("micronode")).thenReturn(micronodeField);

		// Node List Field
		NodeGraphFieldList nodeListField = mock(NodeGraphFieldList.class);
		Mockito.<List<? extends NodeGraphField>> when(nodeListField.getList()).thenReturn(Arrays.asList(nodeField, nodeField, nodeField));
		when(container.getNodeList("nodeList")).thenReturn(nodeListField);

		// String List Field
		StringGraphFieldList stringListField = mock(StringGraphFieldList.class);
		Mockito.<List<? extends StringGraphField>> when(stringListField.getList()).thenReturn(Arrays.asList(stringField, stringField, stringField));
		when(container.getStringList("stringList")).thenReturn(stringListField);

		// Boolean List Field
		BooleanGraphFieldList booleanListField = mock(BooleanGraphFieldList.class);
		Mockito.<List<? extends BooleanGraphField>> when(booleanListField.getList())
				.thenReturn(Arrays.asList(booleanField, booleanField, booleanField));
		when(container.getBooleanList("booleanList")).thenReturn(booleanListField);

		// Date List Field
		DateGraphFieldList dateListField = mock(DateGraphFieldList.class);
		Mockito.<List<? extends DateGraphField>> when(dateListField.getList()).thenReturn(Arrays.asList(dateField, dateField, dateField));
		when(container.getDateList("dateList")).thenReturn(dateListField);

		// Number List Field
		NumberGraphFieldList numberListField = mock(NumberGraphFieldList.class);
		Mockito.<List<? extends NumberGraphField>> when(numberListField.getList()).thenReturn(Arrays.asList(numberField, numberField, numberField));
		when(container.getNumberList("numberList")).thenReturn(numberListField);

		// Html List Field
		HtmlGraphFieldList htmlListField = mock(HtmlGraphFieldList.class);
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

	
}
