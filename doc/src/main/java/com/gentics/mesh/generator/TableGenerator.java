package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.raml.model.parameter.QueryParameter;
import org.reflections.Reflections;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.AbstractParameters;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import io.vertx.core.json.JsonObject;

/**
 * Generator tool which extracts the schema field description from all models. The extracted information will be converted in tabular form to be included in the
 * documentation. The generator will also generate tables for query parameter information.
 */
public class TableGenerator extends AbstractGenerator {

	public static final String MODEL_TABLE_TEMPLATE_NAME = "model-props-table.hbs";
	public static final String PARAM_TABLE_TEMPLATE_NAME = "param-props-table.hbs";

	private final String modelTableTemplateSource;
	private final String paramTableTemplateSource;

	public TableGenerator(File outputFolder) throws IOException {
		super(new File(outputFolder, "tables"));
		this.modelTableTemplateSource = getTemplate(MODEL_TABLE_TEMPLATE_NAME);
		this.paramTableTemplateSource = getTemplate(PARAM_TABLE_TEMPLATE_NAME);
	}

	public void run() throws Exception {
		for (Class<?> clazz : allFoundClassesAnnotatedWithEntityToBeScanned()) {

			// Don't handle abstract classes
			if (clazz.getSimpleName().startsWith("Abstract")) {
				continue;
			}

			// Render mesh options table
			if (clazz.equals(MeshOptions.class)) {
				renderFlatTable(clazz);
				continue;
			}

			// Handle query param tables
			if (AbstractParameters.class.isAssignableFrom(clazz)) {
				renderParameterTable(clazz);
			} else {
				renderModelTableViaSchema(clazz);
			}
		}

	}

	private void renderFlatTable(Class<?> clazz) throws Exception {
		List<Map<String, String>> rows = new ArrayList<>();
		getInfoForClass(rows, clazz, null);

		String name = clazz.getSimpleName();
		Map<String, Object> context = new HashMap<>();
		context.put("entries", rows);
		context.put("name", name);
		renderTable(name, context, modelTableTemplateSource);
	}

	private void getInfoForClass(List<Map<String, String>> rows, Class<?> clazz, String prefix) throws Exception {
		Object instance = clazz.newInstance();
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			field.setAccessible(true);
			String fieldName = field.getName();
			String description = null;
			boolean isRequired = false;
			Object defaultValue = null;
			try {
				defaultValue = field.get(instance);
			} catch (IllegalArgumentException e) {

			}
			for (Annotation annotation : field.getAnnotations()) {
				if (annotation instanceof JsonProperty) {
					JsonProperty propAnn = (JsonProperty) annotation;
					String name = propAnn.value();
					if (!StringUtils.isEmpty(name)) {
						fieldName = name;
					}
					isRequired = propAnn.required();
				}
				if (annotation instanceof JsonPropertyDescription) {
					JsonPropertyDescription descAnn = (JsonPropertyDescription) annotation;
					description = descAnn.value();
				}
			}
			if (hasGenerateDocumentation(field)) {
				getInfoForClass(rows, field.getType(), fieldName);
			} else if (description != null) {
				Map<String, String> entries = new HashMap<>();
				if (prefix != null) {
					fieldName = prefix + "." + fieldName;
				}
				String type = field.getType().getSimpleName().toLowerCase();
				entries.put("key", fieldName);
				entries.put("description", description);
				entries.put("type", type.toLowerCase());
				entries.put("defaultValue", String.valueOf(defaultValue));
				entries.put("required", String.valueOf(isRequired));
				rows.add(entries);
			}
		}
	}

	private boolean hasGenerateDocumentation(Field field) {
		GenerateDocumentation[] annotation = field.getType().getAnnotationsByType(GenerateDocumentation.class);
		return annotation.length != 0;
	}

	private void renderModelTableViaSchema(Class<?> clazz) throws IOException {
		String name = clazz.getSimpleName();
		String jsonSchema = JsonUtil.getJsonSchema(clazz);
		JsonObject schema = new JsonObject(jsonSchema);
		List<Map<String, String>> list = new ArrayList<>();
		flattenSchema(clazz, list, null, schema);
		// Order list by field key
		Collections.sort(list, new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> a1, Map<String, String> a2) {
				return a1.get("key").compareTo(a2.get("key"));
			}
		});

		Map<String, Object> context = new HashMap<>();
		context.put("entries", list);
		context.put("name", name);
		renderTable(name, context, modelTableTemplateSource);
	}

	private void renderParameterTable(Class<?> clazz) throws Exception {
		System.out.println(clazz.getName());
		AbstractParameters parameterObj = (AbstractParameters) clazz.newInstance();
		Map<? extends String, ? extends QueryParameter> ramlParameters = parameterObj.getRAMLParameters();

		Map<String, Object> context = new HashMap<>();
		List<Map<String, String>> list = new ArrayList<>();
		for (Entry<? extends String, ? extends QueryParameter> entry : ramlParameters.entrySet()) {
			String name = entry.getKey();
			QueryParameter value = entry.getValue();
			Map<String, String> paramDescription = new HashMap<>();
			paramDescription.put("name", name);
			paramDescription.put("type", value.getType().name().toLowerCase());
			paramDescription.put("default", value.getDefaultValue());
			paramDescription.put("example", value.getExample());
			paramDescription.put("description", value.getDescription());
			paramDescription.put("required", String.valueOf(value.isRequired()));
			list.add(paramDescription);
		}

		// Order list by field parameter name
		Collections.sort(list, new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> a1, Map<String, String> a2) {
				return a1.get("name").compareTo(a2.get("name"));
			}
		});

		context.put("entries", list);
		context.put("name", parameterObj.getName());
		renderTable(clazz.getSimpleName(), context, paramTableTemplateSource);

	}

	/**
	 * Load the template which is used to render the json data.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String getTemplate(String tableName) throws IOException {
		InputStream ins = TableGenerator.class.getResourceAsStream("/" + tableName);
		Objects.requireNonNull(ins, "Could not find template file {" + tableName + "}");
		return IOUtils.toString(ins);
	}

	private void renderTable(String name, Map<String, Object> context, String templateSource) throws IOException {
		Handlebars handlebars = new Handlebars();
		Template template = handlebars.compileInline(templateSource);
		String output = template.apply(context);
		writeFile(name + ".adoc", output);
	}

	/**
	 * Flatten the schema down and add found properties to the list.
	 * 
	 * @param list
	 *            List of entries which describe a property
	 * @param key
	 *            Current key of the element being flattened
	 * @param obj
	 *            Current object being flattened
	 */
	private void flattenSchema(Class<?> clazz, List<Map<String, String>> list, String key, JsonObject obj) {
		// Check whether the current object already describes a property
		if (obj.containsKey("type") && obj.containsKey("description")) {
			if (key == null) {
				key = "properties";
			}
			String type = obj.getString("type");
			String description = obj.getString("description");
			Boolean required = obj.getBoolean("required");
			Map<String, String> attr = new HashMap<>();
			attr.put("type", type);
			attr.put("description", description);
			attr.put("key", key);
			String requiredStr = "false";
			if (required != null) {
				requiredStr = String.valueOf(required);
			}
			attr.put("required", requiredStr);
			list.add(attr);
		} else {
			// Otherwise just continue to process all remaining objects
			for (Map.Entry<String, Object> entry : obj) {
				try {
					String newKey = entry.getKey();
					if (!newKey.equals("properties")) {
						if (key != null) {
							newKey = key + "." + newKey;
						}
					} else {
						newKey = key;
					}
					flattenSchema(clazz, list, newKey, (JsonObject) entry.getValue());
				} catch (ClassCastException e) {
					// Ignored
				}
			}
		}
	}

	/**
	 * Save the string content to the given file in the output folder.
	 * 
	 * @param filename
	 *            Name of the file to be written to
	 * @param content
	 *            Content to be written
	 * @throws IOException
	 */
	public void writeFile(String filename, String content) throws IOException {
		File outputFile = new File(outputFolder, filename);
		System.out.println("Wrote: " + outputFile.getAbsolutePath());
		FileUtils.writeStringToFile(outputFile, content);
	}

	/**
	 * Locate all rest model classes.
	 * 
	 * @return
	 */
	public static Set<Class<?>> allFoundClassesAnnotatedWithEntityToBeScanned() {
		Reflections reflections = new Reflections("com.gentics.mesh.*");
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(GenerateDocumentation.class);
		return annotated;
	}
}
