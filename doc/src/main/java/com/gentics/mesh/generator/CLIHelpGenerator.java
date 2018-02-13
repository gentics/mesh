package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.io.FileUtils;

import com.gentics.mesh.cli.MeshCLI;

public class CLIHelpGenerator extends AbstractGenerator {

	public CLIHelpGenerator(File outputFolder) {
		this.outputFolder = new File(outputFolder, "models");
	}

	public static void main(String[] args) throws IOException {
		new CLIHelpGenerator(new File("target", "output/models")).run();
	}

	public void run() throws IOException {
		System.out.println("Writing files to  {" + outputFolder.getAbsolutePath() + "}");
		HelpFormatter formatter = new HelpFormatter();
		StringWriter pw = new StringWriter();
		PrintWriter pw2 = new PrintWriter(pw);
		formatter.printHelp(pw2, 74, "mesh.jar", null, MeshCLI.options(), 1, 3, null, false);
		pw.flush();
		File outputFile = new File(outputFolder, "mesh-cli-help.txt");
		String text = pw.toString();
		FileUtils.writeStringToFile(outputFile, text, Charset.defaultCharset(), false);
	}

}
