package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.schema.impl.AbstractFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.google.common.collect.Streams;

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class SchemaUpdateEndpointTest extends AbstractMeshTest {

	@Parameterized.Parameters(name = "{index}: {1}")
	public static Collection<Object[]> parameters() {
		return Arrays.asList(new Object[][]{
			{(Supplier<AbstractFieldSchema>) StringFieldSchemaImpl::new, "StringFieldSchema"},
			{(Supplier<AbstractFieldSchema>) NumberFieldSchemaImpl::new, "NumberFieldSchema"},
			{(Supplier<AbstractFieldSchema>) HtmlFieldSchemaImpl::new, "HtmlFieldSchema"},
			{(Supplier<AbstractFieldSchema>) BooleanFieldSchemaImpl::new, "BooleanFieldSchema"},
			{(Supplier<AbstractFieldSchema>) DateFieldSchemaImpl::new, "DateFieldSchema"},
			{(Supplier<AbstractFieldSchema>) BinaryFieldSchemaImpl::new, "BinaryFieldSchema"},
			{(Supplier<AbstractFieldSchema>) NodeFieldSchemaImpl::new, "NodeFieldSchema"},
			{(Supplier<AbstractFieldSchema>) () -> (MicronodeFieldSchemaImpl) new MicronodeFieldSchemaImpl().setAllowedMicroSchemas(), "MicronodeFieldSchema"}
		});
	}

	@Parameterized.Parameter(0)
	public Supplier<AbstractFieldSchema> fieldSchemaSupplier;

	@Parameterized.Parameter(1)
	public String name;

	@Test
	public void updateSchemaFields() {
		SchemaResponse schema = createSchemaWithField(fieldSchemaSupplier);
		SchemaUpdateRequest request = changeFieldTypes(schema);
		call(() -> client().updateSchema(schema.getUuid(), request));
	}

	private SchemaResponse createSchemaWithField(Supplier<AbstractFieldSchema> fieldSchemaSupplier) {
		return createSchemaWithField(20, "testSchema", fieldSchemaSupplier);
	}

	private SchemaResponse createSchemaWithField(int fieldAmount, String schemaName, Supplier<AbstractFieldSchema> fieldSchemaSupplier) {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName(schemaName);
		request.setFields(
			IntStream.rangeClosed(1, fieldAmount)
				.mapToObj(i -> fieldSchemaSupplier.get().setName("field" + i))
				.collect(Collectors.toList())
		);
		return client().createSchema(request).blockingGet();
	}

	private SchemaUpdateRequest changeFieldTypes(SchemaResponse schema) {
		return schema.toUpdateRequest().setFields(Streams.mapWithIndex(
			createAllSchemaFields(),
			(field, i) -> field.setName("field" + i)
		).collect(Collectors.toList()));
	}

	private Stream<AbstractFieldSchema> createAllSchemaFields() {
		return Stream.concat(Stream.of(
			new StringFieldSchemaImpl(),
			new NumberFieldSchemaImpl(),
			new HtmlFieldSchemaImpl(),
			new BooleanFieldSchemaImpl(),
			new DateFieldSchemaImpl(),
			new BinaryFieldSchemaImpl(),
			new NodeFieldSchemaImpl(),
			(MicronodeFieldSchemaImpl) new MicronodeFieldSchemaImpl().setAllowedMicroSchemas()
		), generateAllListSchemaFields());
	}

	private Stream<AbstractFieldSchema> generateAllListSchemaFields() {
		return Stream.of(
			"string",
			"number",
			"date",
			"boolean",
			"html",
			"node",
			"micronode"
		).map(type -> (ListFieldSchemaImpl) new ListFieldSchemaImpl().setListType(type));
	}
}
