package com.gentics.cailun.cli;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;

import com.jcraft.jsch.Session;

class CustomSessionFactory extends JschConfigSessionFactory {

	@Override
	protected void configure(Host hc, Session session) {
		session.setConfig("StrictHostKeyChecking", "no");
	}

}

public final class GitUtils {

	private GitUtils() {
	}

	public static void pull() throws Exception {

		SshSessionFactory.setInstance(new CustomSessionFactory());

		File localPath = new File("/home/johannes2/workspace_cailun/d1/");
		// now open the resulting repository with a FileRepositoryBuilder
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.setGitDir(new File(localPath, ".git")).readEnvironment().findGitDir().build();

		System.out.println("Having repository: " + repository.getDirectory());

		// the Ref holds an ObjectId for any type of object (tree, commit, blob, tree)
		Ref head = repository.getRef("refs/heads/master");
		System.out.println("Ref of refs/heads/master: " + head);

		repository.close();

		FileRepository localRepo = new FileRepository(localPath + "/.git");
		Git git = new Git(localRepo);

		PullCommand pullCmd = git.pull();
		pullCmd.call();

	}
}
