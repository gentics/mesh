package com.gentics.mesh.cli;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public final class MeshCLI {

	/**
	 * Parse the given command line arguments and return the parsed representation.
	 * 
	 * @param args
	 * @return
	 * @throws ParseException
	 */
	public static CommandLine parse(String[] args) throws ParseException {
		Options options = new Options();

		Option help = new Option("help", "print this message");
		options.addOption(help);

		Option initCluster = new Option("initCluster", false, "Flag which can be used to initialise the first instance of a cluster.");
		options.addOption(initCluster);

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		return cmd;
	}
}
