package com.gentics.cailun.etc;

public interface CaiLunConfiguration {
	
	public static final String HTTP_PORT_KEY = "httpPort";
	public static final int DEFAULT_HTTP_PORT = 8080;

	public int getPort();

	public void setPort(int port);
}
