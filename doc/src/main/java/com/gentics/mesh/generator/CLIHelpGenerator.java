package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.io.FileUtils;

import com.gentics.mesh.cli.MeshCLI;

/**
 * Example generator for the Mesh Server CLI help output. Note this is not related to the Mesh CLI tool.
 */
public class CLIHelpGenerator extends AbstractGenerator {

	public CLIHelpGenerator(File outputFolder) {
		this.outputFolder = new File(outputFolder, "models");
	}

	/**
	 * Start the generator and write data to the output folder.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new CLIHelpGenerator(new File("target", "output/models")).run();
	}

	/**
	 * Start the generator.
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		System.out.println("Writing files to  {" + outputFolder.getAbsolutePath() + "}");
		HelpFormatter formatter = new HelpFormatter();
		StringWriter pw = new StringWriter();
		PrintWriter pw2 = new PrintWriter(pw);
		formatter.printHelp(pw2, 74, "mesh.jar", null, MeshCLI.options(), 1, 3, null, false);
		pw.flush();
		File outputFile = new File(outputFolder, "mesh-cli-help.txt");
		String text = pw.toString();
		FileUtils.writeStringToFile(outputFile, text, StandardCharsets.UTF_8, false);
	}

}
