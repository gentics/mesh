package com.gentics;

import static java.util.Collections.singletonList;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.util.DependencyScanner;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.util.DefaultScanResult;

/**
 * Scans all test classes of the project and all the test classes of the dependenciesToScan array
 * Each test class will be listed in a new line with the prefix 'TEST_CLASS: ${testClass}'
 */
@Mojo(name = "scan-test-files", defaultPhase = LifecyclePhase.NONE, requiresDependencyResolution = ResolutionScope.TEST)
public class TestScanner extends AbstractMojo {

	@Parameter(defaultValue = "${project.build.testOutputDirectory}")
	protected File testClassesDirectory;

	@Parameter(property = "dependenciesToScan")
	private String[] dependenciesToScan;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	private static final TestListResolver TEST_FILTER = new TestListResolver("*Test.class");

	public void execute() throws MojoFailureException {
		DirectoryScanner scanner = new DirectoryScanner(testClassesDirectory, TEST_FILTER);
		DefaultScanResult scan = scanner.scan();
		DefaultScanResult dependencyScan = scanDependencies();
		new HashSet<>(scan.append(dependencyScan).getClasses()).forEach(c -> System.out.println("TEST_CLASS: " + c));
	}

	@SuppressWarnings("unchecked")
	DefaultScanResult scanDependencies() throws MojoFailureException {
		if (getDependenciesToScan() == null) {
			return null;
		} else {
			try {
				DefaultScanResult result = null;
				List<Artifact> dependenciesToScan = DependencyScanner.filter(project.getTestArtifacts(), Arrays.asList(getDependenciesToScan()));

				for (Artifact artifact : dependenciesToScan) {
					String type = artifact.getType();
					File out = artifact.getFile();
					if (out == null || !out.exists() || !("jar".equals(type) || out.isDirectory() || out.getName().endsWith(".jar"))) {
						continue;
					}

					if (out.isFile()) {
						DependencyScanner scanner = new DependencyScanner(singletonList(out), TEST_FILTER);
						result = result == null ? scanner.scan() : result.append(scanner.scan());
					} else if (out.isDirectory()) {
						DirectoryScanner scanner = new DirectoryScanner(out, TEST_FILTER);
						result = result == null ? scanner.scan() : result.append(scanner.scan());
					}
				}

				return result;
			} catch (Exception e) {
				throw new MojoFailureException(e.getLocalizedMessage(), e);
			}
		}
	}

	private String[] getDependenciesToScan() {
		return dependenciesToScan;
	}

	public void setDependenciesToScan( String[] dependenciesToScan )
	{
		this.dependenciesToScan = dependenciesToScan;
	}

	public File getTestClassesDirectory() {
		return testClassesDirectory;
	}

	public void setTestClassesDirectory(File testClassesDirectory) {
		this.testClassesDirectory = testClassesDirectory;
	}

	public MavenProject getProject() {
		return project;
	}

	public void setProject(MavenProject project) {
		this.project = project;
	}
}
