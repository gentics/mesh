package com.gentics.mesh.cli;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Internal command line interface option handling for the server jars.
 */
public final class MeshCLI {

	public static final String HELP = "help";

	public static final String INIT_CLUSTER = "initCluster";

	public static final String NODE_NAME = "nodeName";

	public static final String CLUSTER_NAME = "clusterName";

	public static final String HTTP_PORT = "httpPort";

	public static final String RESET_ADMIN_PASSWORD = "resetAdminPassword";

	public static final String ELASTICSEARCH_URL = "elasticsearchUrl";

	public static final String DISABLE_ELASTICSEARCH = "disableElasticsearch";

	/**
	 * Parse the given command line arguments and return the parsed representation.
	 * 
	 * @param args
	 * @return
	 * @throws ParseException
	 */
	public static CommandLine parse(String... args) throws ParseException {
		CommandLineParser parser = new BasicParser();
		return parser.parse(options(), args);
	}

	/**
	 * Generate the options for the server CLI.
	 * 
	 * @return
	 */
	public static Options options() {
		Options options = new Options();

		Option help = new Option(HELP, "This output");
		options.addOption(help);

		Option disableElasticsearch = new Option(DISABLE_ELASTICSEARCH, "Flag which can be used to disable the Elasticsearch integration.");
		options.addOption(disableElasticsearch);

		Option elasticsearchUrl = new Option(ELASTICSEARCH_URL, true, "Elasticsearch URL to be used.");
		elasticsearchUrl.setArgName("url");
		options.addOption(elasticsearchUrl);

		// TODO remove this and replace it by an option which will read a new password from stdin
		Option resetAdminPassword = new Option(RESET_ADMIN_PASSWORD, true,
			"Reset the admin password. It is advised to change the password once again after the reset has been performed. The command will also recreate the admin user if it can't be found in the system.");
		resetAdminPassword.setArgName("password");
		options.addOption(resetAdminPassword);

		Option initCluster = new Option(INIT_CLUSTER, false,
			"Flag which can be used to initialise the first instance of a cluster. This is usually only used for testing or setup of fresh cluster instances.");
		options.addOption(initCluster);

		Option nodeName = new Option(NODE_NAME, true, "Override the configured node name.");
		nodeName.setArgName("name");
		options.addOption(nodeName);

		Option clusterName = new Option(CLUSTER_NAME, true, "Override the cluster name. Setting a cluster name will also enable clustering.");
		clusterName.setArgName("name");
		options.addOption(clusterName);

		Option httpPort = new Option(HTTP_PORT, true, "Override the configured server HTTP port.");
		httpPort.setArgName("port");
		options.addOption(httpPort);
		return options;
	}

	/**
	 * Print the server CLI help.
	 */
	public static void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("mesh.jar", options());
	}
}
