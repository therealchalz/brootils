package ca.brood.brootils.ssh;

class PortForward {
	int localPort;
	String host;
	int remotePort;
	boolean remoteForward;
	public PortForward(int localPort, String host, int remotePort, boolean remoteForward) {
		this.localPort=localPort;
		this.host = host;
		this.remotePort = remotePort;
		this.remoteForward = remoteForward;
	}
	public String toString() {
		if (remoteForward) {
			return "R:"+remotePort+":"+host+":"+localPort;
		} else {
			return "L:"+localPort+":"+host+":"+remotePort;
		}
	}
}