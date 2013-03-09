/*******************************************************************************
 * Copyright (c) 2013 Charles Hache <chache@brood.ca>. All rights reserved. 
 * 
 * This file is part of the brootils project.
 * brootils is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * brootils is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with brootils.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Charles Hache <chache@brood.ca> - initial API and implementation
 ******************************************************************************/
package ca.brood.brootils.ssh;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.brootils.xml.XMLConfigurable;

public class TunnelKeepaliveThread implements Runnable, XMLConfigurable {
	private int pollingInterval;
	private Logger log;
	private Collection<PortForward> portForwards;
	private SSHSession ssh;
	private boolean keepRunning;
	private Object runningLock;
	private Thread myThread;
	
	public TunnelKeepaliveThread() {
		log = Logger.getLogger(TunnelKeepaliveThread.class);
		keepRunning = true;
		ssh = new SSHSession();
		runningLock = new Object();
		pollingInterval = 10000;	//10 seconds
		portForwards = new ArrayList<PortForward>();
		myThread = new Thread(this);
	}
	
	public void configure(String host, int port, String user, String password) {
		ssh.configure(host, port, user, password);
		log = Logger.getLogger(TunnelKeepaliveThread.class.getName()+"("+host+":"+port+")");
	}
	
	public boolean addLocalForward(int localPort, String host, int remotePort) {
		PortForward toAdd = new PortForward(localPort, host, remotePort, false); 
		portForwards.add(toAdd);
		if (!ssh.forwardPort(toAdd)) {
			log.error("Failed for forward port: "+toAdd);
			return false;
		}
		return true;
	}
	
	public boolean addRemoteForward(int remotePort, String host, int localPort) {
		PortForward toAdd = new PortForward(localPort, host, remotePort, true); 
		portForwards.add(toAdd);
		if (!ssh.forwardPort(toAdd)) {
			log.error("Failed for forward port: "+toAdd);
			return false;
		}
		return true;
	}
	
	public boolean getRunning() {
		synchronized(runningLock) {
			return keepRunning;
		}
	}
	
	public void setRunning(boolean running) {
		synchronized(runningLock) {
			keepRunning = running;
		}
	}
	
	private boolean respawn() {
		boolean ret = true;
		ssh.close();
		for (PortForward p : this.portForwards) {
			if (!ssh.forwardPort(p)) {
				log.warn("Failed to forward port: "+p);
				ret = false;
			}
		}
		return ret;
	}
	
	@Override
	public void run() {
		log.info("Thread started");
		
		while (getRunning()) {
			if (!ssh.areTunnelsActive()) {
				log.warn("Tunnels appear to be down... Restarting them");
				if (!respawn()) {
					log.warn("***Some tunnels failed to restart***");
				}
			}
			
			try {
				Thread.sleep(pollingInterval);
			} catch (InterruptedException e) { }
		}
		
		ssh.close();
		
		log.info("Thread exiting");
	}
	
	public void start() {
		myThread.start();
	}
	
	public void stop() {
		setRunning(false);
		myThread.interrupt();
		myThread = new Thread(this);
	}

	@Override
	public boolean configure(Node rootNode) {
		
		String user = "";
		String password = "";
		String host = "";
		String keyfile = "";
		int port = 22;
		
		NodeList elements = rootNode.getChildNodes();
		for (int i=0; i<elements.getLength(); i++) {
			Node element = elements.item(i);
			
			if (("#text".equalsIgnoreCase(element.getNodeName()))||
					("#comment".equalsIgnoreCase(element.getNodeName())))	{
				continue;
			} else if ("user".equalsIgnoreCase(element.getNodeName())) {
				user = element.getFirstChild().getNodeValue();
			} else if ("host".equalsIgnoreCase(element.getNodeName())) {
				host = element.getFirstChild().getNodeValue(); 
			} else if ("password".equalsIgnoreCase(element.getNodeName())) {
				password = element.getFirstChild().getNodeValue(); 
			} else if ("keyfile".equalsIgnoreCase(element.getNodeName())) {
				keyfile = element.getFirstChild().getNodeValue(); 
			} else if ("port".equalsIgnoreCase(element.getNodeName())) {
				try {
					port = Integer.parseInt(element.getFirstChild().getNodeValue());
				} catch (Exception e) {
					log.warn("Invalid port number specified: "+element.getFirstChild().getNodeValue()+". Using default of 22.");
				}
			} else if ("forward".equalsIgnoreCase(element.getNodeName())) {
				PortForward f = new PortForward();
				if (f.configure(element)) {
					portForwards.add(f);
				}
			} else {
				log.warn("Got unexpected node in config: "+element.getNodeName());
			}			
		}

		if (password.equals("")) {
			log.fatal("No password configured.  Keyfile auth isn't supported yet...");
			return false;
		}
		if (user.equals("")) {
			log.fatal("No user configured.");
			return false;
		}
		if (host.equals("")) {
			log.fatal("No host configured.");
			return false;
		}
		if (portForwards.size() == 0) {
			log.fatal("No port forwards configured.");
			return false;
		}
		
		this.configure(host, port, user, password);
		
		for (PortForward f : portForwards) {
			log.info("Adding forward: "+f);
			ssh.forwardPort(f);
		}
		
		return true;
	}
}
