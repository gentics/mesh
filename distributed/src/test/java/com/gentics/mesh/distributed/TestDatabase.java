package com.gentics.mesh.distributed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.plugin.OServerPluginManager;

public class TestDatabase {

	private String nodeName;
	private File baseDir;

	public TestDatabase(String nodeName, File baseDir) {
		this.nodeName = nodeName;
		this.baseDir = baseDir;
	}

	private InputStream getOrientServerConfig() throws IOException {
		InputStream configIns = getClass().getResourceAsStream("/config/orientdb-server-config.xml");
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		configString = configString.replaceAll("%PLUGIN_DIRECTORY%", "orient-plugins");
		configString = configString.replaceAll("%CONSOLE_LOG_LEVEL%", "finest");
		configString = configString.replaceAll("%FILE_LOG_LEVEL%", "fine");
		// configString = configString.replaceAll("%NODENAME%", nodeName);
		File dbDir = new File(baseDir, "db");
		String safePath = escape(dbDir);
		String safeParentPath = escape(baseDir);
		configString = configString.replaceAll("%MESH_DB_PATH%", "plocal:" + safePath);
		configString = configString.replaceAll("%MESH_DB_PARENT_PATH%", safeParentPath);
		InputStream stream = new ByteArrayInputStream(configString.getBytes(StandardCharsets.UTF_8));
		return stream;
	}

	private String escape(File dbDir) {
		return StringEscapeUtils.escapeJava(StringEscapeUtils.escapeXml11(dbDir.getAbsolutePath()));
	}

	public void startOrientServer() throws Exception {
		String orientdbHome = new File("").getAbsolutePath();
		System.setProperty("ORIENTDB_HOME", orientdbHome);
		OServer server = OServerMain.create();

		InputStream ins = getClass().getResourceAsStream("/plugins/studio-2.2.zip");
		File pluginDirectory = new File("orient-plugins");
		pluginDirectory.mkdirs();
		IOUtils.copy(ins, new FileOutputStream(new File(pluginDirectory, "studio-2.2.zip")));

		server.startup(getOrientServerConfig());
		OServerPluginManager manager = new OServerPluginManager();
		manager.config(server);
		server.activate();
		manager.startup();
	}
}
