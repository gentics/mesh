package com.gentics.mesh.cli;

import picocli.CommandLine.Command;

@Command(name = "sub", mixinStandardHelpOptions = true, description = "Executes sub command 1", subcommands =  { SubCmd2.class })
public class SubCmd {


}
