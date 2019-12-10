package com.gentics.mesh.cli;

import java.util.concurrent.Callable;

import com.gentics.mesh.cli.command.job.JobCommand;
import com.gentics.mesh.cli.command.job.sub.ListCommand;
import com.gentics.mesh.cli.command.server.ServerCommand;
import com.gentics.mesh.cli.command.user.UserCommand;
import com.gentics.mesh.cli.microschema.MicroschemaCommand;
import com.gentics.mesh.cli.plugin.PluginCommand;
import com.gentics.mesh.cli.schema.SchemaCommand;
import com.gentics.mesh.cli.search.SearchCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "mesh-cli", mixinStandardHelpOptions = true, subcommands = {
	 ServerCommand.class, UserCommand.class, JobCommand.class, SearchCommand.class,  PluginCommand.class, ListCommand.class, SchemaCommand.class, MicroschemaCommand.class }, version = "1.0", description = "Gentics Mesh CLI")
public class CLI implements Callable<Integer> {

//	@Parameters(index = "0", description = "The file whose checksum to calculate.")
//	private File file;
//
//	@Option(names = { "-a", "--algorithm" }, description = "MD5, SHA-1, SHA-256, ...")
//	private String algorithm = "MD5";

	// this example implements Callable, so parsing, error handling and handling user
	// requests for usage help or version help can be done with one line of code.
	public static void main(String... args) {
		int exitCode = new CommandLine(new CLI()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception { // your business logic goes here...
//		byte[] fileContents = Files.readAllBytes(file.toPath());
//		byte[] digest = MessageDigest.getInstance(algorithm).digest(fileContents);
//		System.out.printf("%0" + (digest.length * 2) + "x%n", new BigInteger(1, digest));
		return 0;
	}
}
