package com.gentics.mesh.cli.command.user;

import com.gentics.mesh.cli.MeshCommand;
import com.gentics.mesh.cli.command.user.sub.APIKeyCommand;

import picocli.CommandLine.Command;

@Command(name = "user", aliases = { "u" }, subcommands = { APIKeyCommand.class }, description = "User commands")
public class UserCommand extends MeshCommand {

}
