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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.brood.brootils.xml.XMLConfigurable;

/** Class to describe a local or remote port forward.
 * Can be used to send a forward to an {@link SSHSession}, although
 * there are helper functions for local and remote forwards that wrap
 * this functionality.
 * @author Charles Hache
 *
 */
public class PortForward implements XMLConfigurable {
	int localPort;
	String host;
	int remotePort;
	boolean remoteForward;
	/** Empty constructor.
	 * The intent is to call {@link #configure(Node)} after instantiating an empty {@link PortForward}
	 */
	public PortForward() {
		this(Integer.MAX_VALUE, "", Integer.MAX_VALUE, false);
	}
	public PortForward(int localPort, String host, int remotePort, boolean remoteForward) {
		this.localPort=localPort;
		this.host = host;
		this.remotePort = remotePort;
		this.remoteForward = remoteForward;
	}
	public String toString() {
		if (host.equals("") || localPort == Integer.MAX_VALUE || remotePort == Integer.MAX_VALUE) {
			return "PortForward not configured";
		}
		if (remoteForward) {
			return "R:"+remotePort+":"+host+":"+localPort;
		} else {
			return "L:"+localPort+":"+host+":"+remotePort;
		}
	}
	@Override
	public boolean configure(Node rootNode) {
		NodeList elements = rootNode.getChildNodes();
		
		if ("L".equalsIgnoreCase(rootNode.getAttributes().item(0).getTextContent())||
				"local".equalsIgnoreCase(rootNode.getAttributes().item(0).getTextContent())) {
			this.remoteForward = false;
		}
		if ("R".equalsIgnoreCase(rootNode.getAttributes().item(0).getTextContent())||
				"remote".equalsIgnoreCase(rootNode.getAttributes().item(0).getTextContent())) {
			this.remoteForward = true;
		}
		
		for (int i=0; i<elements.getLength(); i++) {
			Node element = elements.item(i);
			
			if (("#text".equalsIgnoreCase(element.getNodeName()))||
					("#comment".equalsIgnoreCase(element.getNodeName())))	{
				continue;
			} else if ("localPort".equalsIgnoreCase(element.getNodeName())) {
				try {
					localPort = Integer.parseInt(element.getFirstChild().getNodeValue());
				} catch (Exception e) {
					return false;
				}
			} else if ("host".equalsIgnoreCase(element.getNodeName())) {
				host = element.getFirstChild().getNodeValue(); 
			} else if ("remotePort".equalsIgnoreCase(element.getNodeName())) {
				try {
					remotePort = Integer.parseInt(element.getFirstChild().getNodeValue());
				} catch (Exception e) {
					return false;
				} 
			}
		}
		
		if (host.equals("") || localPort == Integer.MAX_VALUE || remotePort == Integer.MAX_VALUE) {
			return false;
		}
		
		return true;
	}
}
