package com.gentics.mesh.cli.search;

import com.gentics.mesh.cli.MeshCommand;
import com.gentics.mesh.cli.search.sub.ClearCommand;
import com.gentics.mesh.cli.search.sub.StatusCommand;
import com.gentics.mesh.cli.search.sub.SyncCommand;

import picocli.CommandLine.Command;

@Command(name = "search", description = "Search commands", subcommands = { ClearCommand.class, StatusCommand.class,
	SyncCommand.class })
public class SearchCommand extends MeshCommand {

}
