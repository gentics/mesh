package com.gentics.mesh.core.data.torrent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.common.Torrent;

public class DataDistributorTest {

	@Test
	public void distribute() throws UnknownHostException, IOException {
		Torrent tor = new Torrent();
		SharedTorrent torrent = new SharedTorrent(tor, new File("/tmp/torrent"));
		Client client = new Client(InetAddress.getLocalHost(), torrent);

		client.setMaxDownloadRate(50.0);
		client.setMaxUploadRate(50.0);

		client.download();

		client.waitForCompletion();
	}
}
