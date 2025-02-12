package com.gentics.mesh.generator;

import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.generator.TableGenerator;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.reflections.scanners.Scanners.FieldsAnnotated;

/**
 * Class responsible for invoking all documentation generators.
 */
public class DocGeneratorRunner {
    public static File DOCS_FOLDER = new File("src/main/docs");
    public static File OUTPUT_ROOT_FOLDER = new File(DOCS_FOLDER, "generated");

    /**
     * Main method, called during the exec maven goal.
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        if (!OUTPUT_ROOT_FOLDER.exists()) {
            OUTPUT_ROOT_FOLDER.mkdirs();
        }

        // Generate environment variable table
        EnvHelpGenerator envGen = new EnvHelpGenerator(OUTPUT_ROOT_FOLDER);
        envGen.run();
    }

    /**
     * A generator responsible for generating an asciidoc file containing a table listing all environment
     * variable options with their description.
     */
    private static class EnvHelpGenerator {
        public static final String ENV_TABLE_TEMPLATE_NAME = "env-table.hbs";
        private final File outputFolder;

        EnvHelpGenerator(File outputRootFolder) {
            this.outputFolder = new File(outputRootFolder, "tables");
        }

        public void run() throws IOException {
            System.out.println("Writing files to  {" + outputFolder.getAbsolutePath() + "}");

            List<Map<String, String>> list = new ArrayList<>();
            Reflections reflections = new Reflections("com.gentics.mesh.etc.config", FieldsAnnotated);
            for (Field field : reflections.get(FieldsAnnotated.of(EnvironmentVariable.class).as(Field.class))) {
                EnvironmentVariable envInfo = field.getAnnotation(EnvironmentVariable.class);
                String name = envInfo.name();
                String description = envInfo.description();
                Map<String, String> map = new HashMap<>();
                map.put("name", name);
                map.put("description", description);
                list.add(map);
            }

            Map<String, Object> context = new HashMap<>();
            context.put("entries", list);
            String table = renderTable(context, getTemplate(ENV_TABLE_TEMPLATE_NAME));
            writeFile("mesh-env.adoc-include", table);
        }

        private String renderTable(Map<String, Object> context, String templateSource) throws IOException {
            Handlebars handlebars = new Handlebars();
            Template template = handlebars.compileInline(templateSource);
            return template.apply(context);
        }

        private String getTemplate(String templateName) throws IOException {
            InputStream ins = TableGenerator.class.getResourceAsStream("/" + templateName);
            Objects.requireNonNull(ins, "Could not find template file {" + templateName + "}");
            return IOUtils.toString(ins, StandardCharsets.UTF_8);
        }

        private void writeFile(String filename, String content) throws IOException {
            File outputFile = new File(outputFolder, filename);
            FileUtils.writeStringToFile(outputFile, content, StandardCharsets.UTF_8, false);
        }
    }
}
