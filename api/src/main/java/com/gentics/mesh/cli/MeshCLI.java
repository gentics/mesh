package com.gentics.mesh.cli;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public final class MeshCLI {

	public static final String INIT_CLUSTER = "initCluster";

	public static final String NODE_NAME = "nodeName";

	public static final String CLUSTER_NAME = "clusterName";

	public static final String HTTP_PORT = "httpPort";

	/**
	 * Parse the given command line arguments and return the parsed representation.
	 * 
	 * @param args
	 * @return
	 * @throws ParseException
	 */
	public static CommandLine parse(String... args) throws ParseException {
		Options options = new Options();

		Option help = new Option("help", "print this message");
		options.addOption(help);

		Option initCluster = new Option(INIT_CLUSTER, false, "Flag which can be used to initialise the first instance of a cluster.");
		options.addOption(initCluster);

		Option nodeName = new Option(NODE_NAME, true, "Node instance name");
		options.addOption(nodeName);

		Option clusterName = new Option(CLUSTER_NAME, true, "Cluster name");
		options.addOption(clusterName);

		Option httpPort = new Option(HTTP_PORT, true, "Server HTTP port");
		options.addOption(httpPort);

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		return cmd;
	}
}
