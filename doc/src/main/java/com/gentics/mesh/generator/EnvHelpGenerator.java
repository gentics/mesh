package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;

import com.gentics.mesh.etc.config.env.EnvironmentVariable;

public class EnvHelpGenerator extends AbstractRenderingGenerator {

	public static File DOCS_FOLDER = new File("src/main/docs");
	public static File OUTPUT_ROOT_FOLDER = new File(DOCS_FOLDER, "generated");

	public static final String ENV_TABLE_TEMPLATE_NAME = "env-table.hbs";

	public EnvHelpGenerator(File outputFolder) throws IOException {
		super(new File(outputFolder, "tables"), false);
	}

	public static void main(String[] args) throws IOException {
		new EnvHelpGenerator(OUTPUT_ROOT_FOLDER).run();
	}

	public void run() throws IOException {
		System.out.println("Writing files to  {" + outputFolder.getAbsolutePath() + "}");

		List<Map<String, String>> list = new ArrayList<>();
		boolean isEmpty = true;
		Reflections reflections = new Reflections("com.gentics.mesh.etc.config", new FieldAnnotationsScanner());
		for (Field field : reflections.getFieldsAnnotatedWith(EnvironmentVariable.class)) {
			if (field.isAnnotationPresent(EnvironmentVariable.class)) {
				EnvironmentVariable envInfo = field.getAnnotation(EnvironmentVariable.class);
				String name = envInfo.name();
				String description = envInfo.description();
				Map<String, String> map = new HashMap<>();
				map.put("name", name);
				map.put("description", description);
				list.add(map);
				isEmpty = false;
			}
		}

		if (isEmpty) {
			throw new RuntimeException("Generator was unable to find any annotated fields");
		}

		Map<String, Object> context = new HashMap<>();
		context.put("entries", list);
		String table = renderTable(context, getTemplate(ENV_TABLE_TEMPLATE_NAME));
		writeFile("mesh-env.adoc-include", table);
	}

}
