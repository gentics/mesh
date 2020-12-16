package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

/**
 * Abstract implementation for an example generator that renders using handlebars.
 */
public class AbstractRenderingGenerator extends AbstractGenerator {

	public AbstractRenderingGenerator(File file, boolean cleanDir) throws IOException {
		super(file, cleanDir);
	}

	protected String renderTable(Map<String, Object> context, String templateSource) throws IOException {
		Handlebars handlebars = new Handlebars();
		Template template = handlebars.compileInline(templateSource);
		return template.apply(context);
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
		FileUtils.writeStringToFile(outputFile, content, StandardCharsets.UTF_8, false);
	}

	/**
	 * Load the template which is used to render the json data.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String getTemplate(String templateName) throws IOException {
		InputStream ins = TableGenerator.class.getResourceAsStream("/" + templateName);
		Objects.requireNonNull(ins, "Could not find template file {" + templateName + "}");
		return IOUtils.toString(ins, StandardCharsets.UTF_8);
	}

}
