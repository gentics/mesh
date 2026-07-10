package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;

/**
 * Example generator for the environment settings table.
 */
public class EnvHelpGenerator extends AbstractRenderingGenerator {

	public static File DOCS_FOLDER = new File("src/main/docs");
	public static File OUTPUT_ROOT_FOLDER = new File(DOCS_FOLDER, "generated");

	public static final String ENV_TABLE_TEMPLATE_NAME = "env-table.hbs";

	public EnvHelpGenerator(File outputFolder) throws IOException {
		super(new File(outputFolder, "tables"), false);
	}

	/**
	 * Run the generator and write to the docs folder.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new EnvHelpGenerator(OUTPUT_ROOT_FOLDER).run();
	}

	/**
	 * Start the generator.
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		System.out.println("Writing files to  {" + outputFolder.getAbsolutePath() + "}");

		Map<String, Map<String, String>> list = new TreeMap<>(new MeshFirstComparator());
		boolean notEmpty = false;
		Reflections reflections = new Reflections("com.gentics.mesh.etc.config");
		for (Class<?> clazz : reflections.getTypesAnnotatedWith(GenerateDocumentation.class)) {
			notEmpty |= processClass(clazz, list, Optional.empty());
		}
		if (!notEmpty) {
			throw new RuntimeException("Generator was unable to find any annotated fields");
		}

		Map<String, Object> context = new HashMap<>();
		context.put("entries", list);
		String table = renderTable(context, getTemplate(ENV_TABLE_TEMPLATE_NAME));
		writeFile("mesh-env.adoc-include", table);
	}

	protected boolean processClass(Class<?> clazz, Map<String, Map<String, String>> list, Optional<String> maybePrefix) {
		boolean notEmpty = false;
		for (Field field: clazz.getDeclaredFields()) {
			String fieldName = field.getName();
            {
            	JsonProperty jsonAnnotation = field.getAnnotation(JsonProperty.class);
                if (jsonAnnotation != null && StringUtils.isNotBlank(jsonAnnotation.value())) {
                	fieldName = jsonAnnotation.value();
                }
            }
            if (field.isAnnotationPresent(EnvironmentVariable.class)) {
				EnvironmentVariable envInfo = field.getAnnotation(EnvironmentVariable.class);
				String name = envInfo.name();
				String description = envInfo.description();
				Map<String, String> map = list.getOrDefault(name, new HashMap<>());
				map.put("name", name);
				map.put("description", description);
				if (!envInfo.isNoField()) {
					if (!map.getOrDefault("field", StringUtils.EMPTY).contains(".")) {
						map.put("field", maybePrefix.map(p -> p + ".").orElse(StringUtils.EMPTY) + fieldName);
					}
				}
                list.putIfAbsent(name, map);
				notEmpty |= true;
			} else if (field.getType().isAnnotationPresent(GenerateDocumentation.class)) {
				notEmpty |= processClass(field.getType(), list, Optional.of(fieldName + maybePrefix.map(p -> p + ".").orElse(StringUtils.EMPTY)));
			}
		}
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null && superclass != Object.class) {
			notEmpty |= processClass(superclass, list, maybePrefix);
		}
		return notEmpty;
	}

	class MeshFirstComparator implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			if (o1 != null && o2 != null) {
				boolean o1IsMesh = o1.startsWith("MESH_");
				boolean o2IsMesh = o2.startsWith("MESH_");
				if (o1IsMesh && !o2IsMesh) {
					return -1;
				}
				if (o2IsMesh && !o1IsMesh) {
					return 1;
				}
				return o1.compareTo(o2);
			}
			return Comparator.comparing((String s) -> s).compare(o1, o2);
		}		
	}
}
