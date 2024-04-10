package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshEventModel;

/**
 * Example generator for the table of all Gentics Mesh event models.
 */
public class EventTableGenerator extends AbstractRenderingGenerator {

	public static final String TEMPLATE_NAME = "event-table.hbs";

	private final String eventTableTemplateSource;

	public EventTableGenerator(File outputFolder) throws IOException {
		super(new File(outputFolder, "tables"), false);
		this.eventTableTemplateSource = getTemplate(TEMPLATE_NAME);
	}

	/**
	 * Run the table generator.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// Generate asciidoc tables to be included in the docs.
		final File DOCS_FOLDER = new File("src/main/docs");
		final File OUTPUT_ROOT_FOLDER = new File(DOCS_FOLDER, "generated");

		EventTableGenerator tableGen = new EventTableGenerator(OUTPUT_ROOT_FOLDER);
		tableGen.run();
	}

	/**
	 * Run the generator.
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		// Render event tables
		writeFile(MeshEvent.class.getSimpleName() + ".adoc-include", renderEventTable());
	}

	private String renderEventTable() throws IOException {

		List<Map<String, String>> rows = new ArrayList<>();
		for (MeshEvent value : MeshEvent.values()) {
			Map<String, String> row = new HashMap<>();
			rows.add(row);
			row.put("adress", value.getAddress());
			row.put("description", value.getDescription());
			MeshEventModel example = value.example();
			if (example != null) {
				row.put("example", example.toJson(false));
			}
		}

		Map<String, Object> context = new HashMap<>();
		context.put("entries", rows);
		context.put("name", "Mesh Events");
		return renderTable(context, eventTableTemplateSource);
	}
}
