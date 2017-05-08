package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;

import com.gentics.mesh.core.rest.common.GenerateDocumentation;
import com.gentics.mesh.json.JsonUtil;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import io.vertx.core.json.JsonObject;

/**
 * Generator tool which extracts the schema field description from all models. The extracted information will be converted in tabular form to be included in the
 * documentation.
 */
public class ModelDescriptionTableGenerator extends AbstractGenerator {

	public static final String TEMPLATE_NAME = "model-props-table.hbs";

	private final String templateSource;

	public ModelDescriptionTableGenerator(File outputFolder) throws IOException {
		super(new File(outputFolder, "models"));
		this.templateSource = getTemplate();
	}

	public void run() throws IOException {
		for (Class<?> clazz : allFoundClassesAnnotatedWithEntityToBeScanned()) {
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
			renderTable(name, context);
		}
	}

	/**
	 * Load the template which is used to render the json data.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String getTemplate() throws IOException {
		InputStream ins = ModelDescriptionTableGenerator.class.getResourceAsStream("/" + TEMPLATE_NAME);
		Objects.requireNonNull(ins, "Could not find template file {" + TEMPLATE_NAME + "}");
		return IOUtils.toString(ins);
	}

	private void renderTable(String name, Map<String, Object> context) throws IOException {
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
		FileUtils.writeStringToFile(outputFile, content);
	}

	/**
	 * Locate all rest model classes.
	 * 
	 * @return
	 */
	public static Set<Class<?>> allFoundClassesAnnotatedWithEntityToBeScanned() {
		Reflections reflections = new Reflections("com.gentics.mesh.core.rest.*");
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(GenerateDocumentation.class);
		return annotated;
	}
}
