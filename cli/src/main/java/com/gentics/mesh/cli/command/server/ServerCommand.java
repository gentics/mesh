package com.gentics.mesh.cli.command.server;

import com.gentics.mesh.cli.MeshCommand;
import com.gentics.mesh.cli.command.server.sub.BackupCommand;
import com.gentics.mesh.cli.command.server.sub.CheckCommand;
import com.gentics.mesh.cli.command.server.sub.DebugCommand;
import com.gentics.mesh.cli.command.server.sub.ExportCommand;
import com.gentics.mesh.cli.command.server.sub.ImportCommand;
import com.gentics.mesh.cli.command.server.sub.InfoCommand;
import com.gentics.mesh.cli.command.server.sub.RepairCommand;
import com.gentics.mesh.cli.command.server.sub.RestoreCommand;

import picocli.CommandLine.Command;

@Command(name = "server", aliases = { "s" }, subcommands = {
	BackupCommand.class,
	RestoreCommand.class,
	ExportCommand.class,
	ImportCommand.class,
	RepairCommand.class,
	CheckCommand.class,
	InfoCommand.class,
	DebugCommand.class }, description = "Server commands")
public class ServerCommand extends MeshCommand {

}
