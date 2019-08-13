package com.gentics.mesh.maven;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;


public final class MavenUtilities {

	/**
	 * Url that points to the internal repository
	 */
	public static final String INTERNAL_REPOSITORY_URL_SEGMENT = "maven.gentics.com/maven2";

	/**
	 * Return the artifact url for the given maven coordinates.
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param classifier
	 * @param extension
	 * @return
	 * @throws Exception
	 */
	public static URL getMavenArtifactUrl(String groupId, String artifactId, String version, String classifier, String extension) throws Exception {
		MavenMetadata metadata = getMavenMetadata(groupId, artifactId, version);
		return getLatestMavenArtifactUrl(metadata, classifier, extension);
	}

	/**
	 * Returns the url to the latest maven artifact
	 * 
	 * @param metadata
	 * @param classifier
	 * @param extension
	 * @return
	 * @throws Exception
	 */
	public static URL getLatestMavenArtifactUrl(MavenMetadata metadata, String classifier, String extension) throws Exception {

		// Set classifier to emptystring if it is empty
		if (classifier == null || classifier.trim().length() == 0) {
			classifier = "";
		} else {
			classifier = "-" + classifier;
		}

		String pathExtension = classifier + "." + extension;

		if (metadata.isSnapshot()) {
			pathExtension = "-" + metadata.getTimestamp() + "-" + metadata.getBuildNumber() + classifier + "." + extension;
		}
		String path = "http://" + INTERNAL_REPOSITORY_URL_SEGMENT + "/" + metadata.getGroupId().replace(".", "/") + "/" + metadata.getArtifactId()
				+ "/" + metadata.getVersion() + "/" + metadata.getArtifactId() + "-" + metadata.getVersion().replace("-SNAPSHOT", "") + pathExtension;

		try {
			return new URL(path);
		} catch (MalformedURLException e) {
			throw new Exception("Could not create url that points to the latest artifact.", e);
		}
	}

	/**
	 * Return the maven metadata for the given maven coordinates.
	 * 
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @return
	 * @throws Exception
	 */
	public static MavenMetadata getMavenMetadata(String groupId, String artifactId, String version) throws Exception {

		URL sourceUrl;

		try {

			if (version.contains("SNAPSHOT")) {
				sourceUrl = new URL("http://" + INTERNAL_REPOSITORY_URL_SEGMENT + "/" + groupId.replace(".", "/") + "/" + artifactId + "/" + version
						+ "/maven-metadata.xml");
			} else {
				sourceUrl = new URL(
						"http://" + INTERNAL_REPOSITORY_URL_SEGMENT + "/" + groupId.replace(".", "/") + "/" + artifactId + "/maven-metadata.xml");
			}
			MavenMetadata metadata = getMavenMetadata(sourceUrl);

			metadata.setVersion(version);
			return metadata;
		} catch (MalformedURLException e) {
			throw new Exception("Error while building url to maven-metadata.xml occured", e);
		}

	}

	/**
	 * Returns the MavenMetadata object for the given sourceUrl that points to a valid maven-metadata.xml
	 * 
	 * @param sourceUrl
	 * @return
	 * @throws Exception
	 * @throws Exception
	 */
	public static MavenMetadata getMavenMetadata(URL sourceUrl) throws Exception {

		try {

			URLConnection uc = sourceUrl.openConnection();
			InputStream ins = uc.getInputStream();
			try {

				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(ins);
				Element metadataNode = doc.getRootElement();
				if (metadataNode == null) {
					throw new Exception("Could not find metadata node");
				}
				String version = metadataNode.getChildText("version");
				String groupId = metadataNode.getChildText("groupId");
				String artifactId = metadataNode.getChildText("artifactId");
				MavenMetadata metadata = new MavenMetadata(groupId, artifactId, version);

				// Check if the metadata contains versioning information. This would
				// mean it is a snapshot artifact
				Element versioningNode = metadataNode.getChild("versioning");
				if (versioningNode != null) {

					Element snapshotNode = versioningNode.getChild("snapshot");

					if (snapshotNode == null) {
						metadata.setSnapshot(false);
					} else {
						int buildNumber = Integer.parseInt(snapshotNode.getChildText("buildNumber"));
						String timestamp = versioningNode.getChild("snapshot").getChildText("timestamp");
						//		String lastUpdated = versioningNode.getChildText("lastUpdated");
						//metadata.setLastUpdated(lastUpdated);
						metadata.setSnapshot(true);
						metadata.setTimestamp(timestamp);
						metadata.setBuildNumber(buildNumber);
					}

					List<String> versions = new ArrayList<>();
					Iterator<Element> it = versioningNode.getChild("versions").getChildren().iterator();
					while (it.hasNext()) {
						Element versionElement = (Element) it.next();
						versions.add(versionElement.getText());
					}
					metadata.setVersions(versions);
				} else {
					metadata.setSnapshot(false);
				}
				return metadata;
			} finally {
				ins.close();
			}
		} catch (Exception e) {
			throw new Exception("Could not parse xml from url {" + sourceUrl + "}", e);
		}

	}
}
