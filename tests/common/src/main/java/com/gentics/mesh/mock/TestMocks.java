package com.gentics.mesh.mock;

import static com.gentics.mesh.example.AbstractExamples.TIMESTAMP_NEW;
import static com.gentics.mesh.example.AbstractExamples.TIMESTAMP_OLD;
import static com.gentics.mesh.example.ExampleUuids.GROUP_CLIENT_UUID;
import static com.gentics.mesh.example.ExampleUuids.LANGUAGE_UUID;
import static com.gentics.mesh.example.ExampleUuids.MICROSCHEMA_UUID;
import static com.gentics.mesh.example.ExampleUuids.NODE_DELOREAN_UUID;
import static com.gentics.mesh.example.ExampleUuids.PROJECT_DEMO2_UUID;
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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mockito.Mockito;

import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.result.TraversalResult;

public final class TestMocks {

	private TestMocks() {

	}

	public static HibProject mockProject(HibUser user) {
		HibProject project = mock(HibProject.class);
		when(project.getUuid()).thenReturn(PROJECT_DEMO2_UUID);
		when(project.getName()).thenReturn("dummyProject");
		when(project.getCreator()).thenReturn(user);
		when(project.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(project.getEditor()).thenReturn(user);
		when(project.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(project.getElementVersion()).thenReturn(UUID_1);
		return project;
	}

	public static HibLanguage mockLanguage(String code) {
		HibLanguage language = mock(HibLanguage.class);
		when(language.getUuid()).thenReturn(LANGUAGE_UUID);
		when(language.getLanguageTag()).thenReturn("de");
		when(language.getElementVersion()).thenReturn(UUID_2);
		return language;
	}

	public static HibNode mockNodeBasic(String schemaType, HibUser user) {
		HibNode node = mock(HibNode.class);
		when(node.getUuid()).thenReturn(NODE_DELOREAN_UUID);
		HibSchema schemaContainer = mockSchemaContainer(schemaType, user);
		when(node.getSchemaContainer()).thenReturn(schemaContainer);
		return node;
	}

	public static HibMicronode mockMicronode(String microschemaName, HibUser user) {
		HibMicronode micronode = mock(HibMicronode.class);
		when(micronode.getUuid()).thenReturn(UUID_1);
		HibMicroschema microschemaContainer = mockMicroschemaContainer(microschemaName, user);
		HibMicroschemaVersion latestVersion = microschemaContainer.getLatestVersion();
		// TODO find a away to convert this correctly
		when(micronode.getSchemaContainerVersion()).thenReturn(latestVersion);
		MicroschemaVersionModel microschema = microschemaContainer.getLatestVersion().getSchema();
		when(micronode.getSchemaContainerVersion().getSchema()).thenReturn(microschema);

		// longitude field
		HibNumberField longitudeField = mock(HibNumberField.class);
		when(longitudeField.getNumber()).thenReturn(16.373063840833);
		when(micronode.getNumber("longitude")).thenReturn(longitudeField);

		// latitude field
		HibNumberField latitudeField = mock(HibNumberField.class);
		when(latitudeField.getNumber()).thenReturn(16.373063840833);
		when(micronode.getNumber("latitude")).thenReturn(latitudeField);
		when(micronode.getElementVersion()).thenReturn(UUID_3);
		return micronode;
	}

	public static HibRole mockRole(String roleName, HibUser creator) {
		HibRole role = mock(HibRole.class);
		when(role.getCreator()).thenReturn(creator);
		when(role.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(role.getEditor()).thenReturn(creator);
		when(role.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(role.getName()).thenReturn(roleName);
		when(role.getUuid()).thenReturn(ROLE_CLIENT_UUID);
		when(role.getElementVersion()).thenReturn(UUID_4);
		return role;
	}

	public static HibGroup mockGroup(String groupName, HibUser creator) {
		HibGroup group = mock(HibGroup.class);
		when(group.getCreator()).thenReturn(creator);
		when(group.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(group.getEditor()).thenReturn(creator);
		when(group.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(group.getName()).thenReturn(groupName);
		when(group.getUuid()).thenReturn(GROUP_CLIENT_UUID);
		//when(group.getRolesWithPerm(InternalPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(group.getElementVersion()).thenReturn(UUID_5);
		return group;
	}

	public static HibUser mockUser(String username, String firstname, String lastname) {
		return mockUser(username, firstname, lastname, null);
	}

	public static HibUser mockUser(String username, String firstname, String lastname, HibUser creator) {
		HibUser user = mock(HibUser.class);
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
		when(user.getElementVersion()).thenReturn(UUID_1);
		return user;
	}

	public static HibTagFamily mockTagFamily(String name, HibUser user, HibProject project) {
		HibTagFamily tagFamily = mock(HibTagFamily.class);
		when(tagFamily.getCreator()).thenReturn(user);
		when(tagFamily.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(tagFamily.getEditor()).thenReturn(user);
		when(tagFamily.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(tagFamily.getName()).thenReturn(name);
		when(tagFamily.getUuid()).thenReturn(TAGFAMILY_FUELS_UUID);
		when(tagFamily.getProject()).thenReturn(project);
		when(tagFamily.getElementVersion()).thenReturn(UUID_2);
		return tagFamily;
	}

	public static HibTag mockTag(String name, HibUser user, HibTagFamily tagFamily, HibProject project) {
		HibTag tag = mock(HibTag.class);
		when(tag.getCreator()).thenReturn(user);
		when(tag.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(tag.getEditor()).thenReturn(user);
		when(tag.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		when(tag.getName()).thenReturn(name);
		when(tag.getUuid()).thenReturn(TAG_BLUE_UUID);
		when(tag.getTagFamily()).thenReturn(tagFamily);
		when(tag.getProject()).thenReturn(project);
		when(tag.getElementVersion()).thenReturn(UUID_3);
		return tag;
	}

	private static <T> TraversalResult<T> createEmptyTraversal() {
		return new TraversalResult<>(Collections.emptyList());
	}

	public static HibSchema mockSchemaContainer(String name, HibUser user) {
		HibSchema container = mock(HibSchema.class);
		when(container.getName()).thenReturn(name);
		when(container.getUuid()).thenReturn(SCHEMA_VEHICLE_UUID);
		HibSchemaVersion latestVersion = mock(HibSchemaVersion.class);
		when(latestVersion.getSchemaContainer()).thenReturn(container);
		when(latestVersion.getSchema()).thenReturn(mockContentSchema());
		when(latestVersion.getName()).thenReturn(name);
		when(container.getLatestVersion()).thenReturn(latestVersion);
		when(container.getCreator()).thenReturn(user);
		when(container.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(container.getEditor()).thenReturn(user);
		when(container.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		//when(container.getRolesWithPerm(InternalPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		return container;
	}

	public static HibMicroschema mockMicroschemaContainer(String name, HibUser user) {
		HibMicroschema schema = mock(HibMicroschema.class);
		when(schema.getName()).thenReturn(name);
		when(schema.getUuid()).thenReturn(MICROSCHEMA_UUID);
		HibMicroschemaVersion latestVersion = mock(HibMicroschemaVersion.class);
		when(latestVersion.getSchema()).thenReturn(mockGeolocationMicroschema());

		when(schema.getLatestVersion()).thenReturn(latestVersion);
		when(schema.getCreator()).thenReturn(user);
		when(schema.getCreationTimestamp()).thenReturn(TIMESTAMP_OLD);
		when(schema.getEditor()).thenReturn(user);
		when(schema.getLastEditedTimestamp()).thenReturn(TIMESTAMP_NEW);
		//when(schema.getRolesWithPerm(InternalPermission.READ_PERM)).thenReturn(createEmptyTraversal());
		when(schema.getElementVersion()).thenReturn(UUID_5);
		return schema;
	}

	public static SchemaVersionModel mockContentSchema() {
		SchemaVersionModel schema = new SchemaModelImpl();
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

	public static MicroschemaVersionModel mockGeolocationMicroschema() {
		MicroschemaVersionModel microschema = new MicroschemaModelImpl();
		microschema.setName("geolocation");
		microschema.setDescription("Microschema for Geolocations");

		microschema.addField(new NumberFieldSchemaImpl().setName("longitude").setLabel("Longitude").setRequired(true));
		microschema.addField(new NumberFieldSchemaImpl().setName("latitude").setLabel("Latitude").setRequired(true));

		return microschema;
	}

	public static HibNode mockNode(NodeDao nodeDao, ContentDao contentDao, TagDao tagDao, HibNode parent, HibProject project, HibUser user, String languageTag, HibTag tagA, HibTag tagB) {
		HibNode node = mock(HibNode.class);

		when(node.getProject()).thenReturn(project);
		when(nodeDao.getParentNode(same(node), Mockito.any())).thenReturn(parent);

		TraversalResult<HibTag> tagResult = new TraversalResult<>(Arrays.asList(tagA, tagB));
		when(tagDao.getTags(same(node), Mockito.any())).thenReturn(tagResult);

		HibSchema schemaContainer = mockSchemaContainer("content", user);
		HibSchemaVersion latestVersion = schemaContainer.getLatestVersion();
		when(latestVersion.getUuid()).thenReturn(UUID_2);
		when(node.getSchemaContainer()).thenReturn(schemaContainer);
		when(node.getCreator()).thenReturn(user);
		when(node.getUuid()).thenReturn(NODE_DELOREAN_UUID);
		
		HibNodeFieldContainer container = mockContainer(languageTag, user);
		when(contentDao.getSchemaContainerVersion(container)).thenReturn(latestVersion);
		when(contentDao.getNode(container)).thenReturn(node);
		when(container.getNode()).thenReturn(node);
		when(container.getElementVersion()).thenReturn(UUID_5);

		when(contentDao.getLatestDraftFieldContainer(node, languageTag)).thenReturn(container);

		when(node.getElementVersion()).thenReturn(UUID_4);
		return node;
	}

	public static HibNodeFieldContainer mockContainer(String languageTag, HibUser user) {
		HibNodeFieldContainer container = mock(HibNodeFieldContainer.class);
		when(container.getLanguageTag()).thenReturn(languageTag);

		when(container.getEditor()).thenReturn(user);

		// String field
		HibStringField stringField = mock(HibStringField.class);
		when(stringField.getString()).thenReturn("The name value");
		when(container.getString("string")).thenReturn(stringField);

		// Number field
		HibNumberField numberField = mock(HibNumberField.class);
		when(numberField.getNumber()).thenReturn(0.146f);
		when(container.getNumber("number")).thenReturn(numberField);

		// Date field
		HibDateField dateField = mock(HibDateField.class);
		when(dateField.getDate()).thenReturn(TIMESTAMP_NEW / 1000);
		when(container.getDate("date")).thenReturn(dateField);

		// Boolean field
		HibBooleanField booleanField = mock(HibBooleanField.class);
		when(booleanField.getBoolean()).thenReturn(true);
		when(container.getBoolean("boolean")).thenReturn(booleanField);

		// Node field
		HibNodeField nodeField = mock(HibNodeField.class);
		HibNode nodeRef = mockNodeBasic("folder", user);
		when(nodeField.getNode()).thenReturn(nodeRef);
		when(container.getNode("node")).thenReturn(nodeField);

		// Html field
		HibHtmlField htmlField = mock(HibHtmlField.class);
		when(htmlField.getHTML()).thenReturn("some<b>html");
		when(container.getHtml("html")).thenReturn(htmlField);

		// micronode field
		HibMicronodeField micronodeField = mock(HibMicronodeField.class);
		HibMicronode micronode = mockMicronode("geolocation", user);
		when(micronodeField.getMicronode()).thenReturn(micronode);
		when(container.getMicronode("micronode")).thenReturn(micronodeField);

		// Node List Field
		HibNodeFieldList nodeListField = mock(HibNodeFieldList.class);
		Mockito.<List<? extends HibNodeField>> when(nodeListField.getList()).thenReturn(Arrays.asList(nodeField, nodeField, nodeField));
		when(container.getNodeList("nodeList")).thenReturn(nodeListField);

		// String List Field
		HibStringFieldList stringListField = mock(HibStringFieldList.class);
		Mockito.<List<? extends HibStringField>> when(stringListField.getList()).thenReturn(Arrays.asList(stringField, stringField, stringField));
		when(container.getStringList("stringList")).thenReturn(stringListField);

		// Boolean List Field
		HibBooleanFieldList booleanListField = mock(HibBooleanFieldList.class);
		Mockito.<List<? extends HibBooleanField>> when(booleanListField.getList())
				.thenReturn(Arrays.asList(booleanField, booleanField, booleanField));
		when(container.getBooleanList("booleanList")).thenReturn(booleanListField);

		// Date List Field
		HibDateFieldList dateListField = mock(HibDateFieldList.class);
		Mockito.<List<? extends HibDateField>> when(dateListField.getList()).thenReturn(Arrays.asList(dateField, dateField, dateField));
		when(container.getDateList("dateList")).thenReturn(dateListField);

		// Number List Field
		HibNumberFieldList numberListField = mock(HibNumberFieldList.class);
		Mockito.<List<? extends HibNumberField>> when(numberListField.getList()).thenReturn(Arrays.asList(numberField, numberField, numberField));
		when(container.getNumberList("numberList")).thenReturn(numberListField);

		// Html List Field
		HibHtmlFieldList htmlListField = mock(HibHtmlFieldList.class);
		Mockito.<List<? extends HibHtmlField>> when(htmlListField.getList()).thenReturn(Arrays.asList(htmlField, htmlField, htmlField));
		when(container.getHTMLList("htmlList")).thenReturn(htmlListField);

		// TODO currently, this mock is only used for the search document example, where we want to omit
		// fields of type "list of micronodes". We should better add an argument to the method to specify,
		// which types of fields should be added
		// // Micronode List Field
		// HibMicronodeFieldList micronodeListField = mock(HibMicronodeFieldListImpl.class);
		// Mockito.<List<? extends HibMicronodeField>> when(micronodeListField.getList()).thenReturn(Arrays.asList(micronodeField, micronodeField,
		// micronodeField));
		// when(container.getMicronodeList("micronodeList")).thenReturn(micronodeListField);

		// TODO add select fields

		return container;
	}

	
}
