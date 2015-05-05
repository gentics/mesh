package com.gentics.mesh.hdfs;

import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

public class HDFSTest {

	@Test
//	@Ignore("Not yet implemented")
	public void testHDFS() throws IOException {
		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(conf);
		conf.set("fs.defaultFS", "hdfs://localhost");
		String path = "hdfs://localhost/user/joe/TestFile.txt";
		URI uri=URI.create (path);
		// Hadoop DFS deals with Path
		Path inFile = new Path("/tmp/input.txt");
		Path outFile = new Path("/tmp/output.txt");

		// Read from and write to new file
		FSDataInputStream in = fs.open(inFile);
		FSDataOutputStream out = fs.create(outFile);
		byte buffer[] = new byte[256];
		try {
			int bytesRead = 0;
			while ((bytesRead = in.read(buffer)) > 0) {
				out.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			System.out.println("Error while copying file");
		} finally {
			in.close();
			out.close();
		}
	}
}
