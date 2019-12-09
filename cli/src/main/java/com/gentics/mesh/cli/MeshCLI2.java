package com.gentics.mesh.cli;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "mesh-cli", mixinStandardHelpOptions = true, subcommands = {
	SubCmd.class }, version = "checksum 4.0", description = "Prints the checksum (MD5 by default) of a file to STDOUT.")
public class MeshCLI2 implements Callable<Integer> {

	@Parameters(index = "0", description = "The file whose checksum to calculate.")
	private File file;

	@Option(names = { "-a", "--algorithm" }, description = "MD5, SHA-1, SHA-256, ...")
	private String algorithm = "MD5";

	// this example implements Callable, so parsing, error handling and handling user
	// requests for usage help or version help can be done with one line of code.
	public static void main(String... args) {
		int exitCode = new CommandLine(new MeshCLI2()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception { // your business logic goes here...
		byte[] fileContents = Files.readAllBytes(file.toPath());
		byte[] digest = MessageDigest.getInstance(algorithm).digest(fileContents);
		System.out.printf("%0" + (digest.length * 2) + "x%n", new BigInteger(1, digest));
		return 0;
	}
}
