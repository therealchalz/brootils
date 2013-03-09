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
package ca.brood.brootils.xml;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XMLFileLoader {
	private String filename;
	private XMLConfigurable rootObject;
	private Logger log;
	private boolean validateXML;
	
	public XMLFileLoader(String filename, XMLConfigurable rootNodeObject) {
		this.filename = filename;
		rootObject = rootNodeObject;
		validateXML = true;
		
		log = Logger.getLogger(XMLFileLoader.class);
	}
	
	public boolean load() throws Exception {
		
		if (this.rootObject == null) {
			throw new NullPointerException("Root object cannot be null");
		}
		
		File xmlFile = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		XMLErrorCallback error = new XMLErrorCallback();
		Document doc;
		
		dbFactory.setValidating(validateXML);
		
		//Is this required?
		//dbFactory.setNamespaceAware(true);
		
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			
			dBuilder.setErrorHandler(new SimpleXMLErrorHandler(log, error));
			
			doc = dBuilder.parse(xmlFile);
			
			doc.getDocumentElement().normalize();
			
			if (!error.isConfigValid()) {
				throw new Exception("Config doesn't conform to schema.");
			}
		} catch (Exception e) {
			throw e;
		}
		
		Node rootNode = doc.getDocumentElement();
		
		return rootObject.configure(rootNode);
	}
	
	public void setXMLValidation(boolean validate) {
		validateXML = validate;
	}
}
