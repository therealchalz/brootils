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

import org.apache.logging.log4j.Logger;
import org.xml.sax.*;

public class SimpleXMLErrorHandler implements ErrorHandler {
	private Logger log;
	private XMLErrorCallback ec;
	public SimpleXMLErrorHandler (XMLErrorCallback ec) {
		this.log = null;
		this.ec = ec;
	}
	public SimpleXMLErrorHandler (Logger log, XMLErrorCallback ec) {
		this.log = log;
		this.ec = ec;
	}
    public void warning(SAXParseException e) throws SAXException {
    	if (log != null)
    		log.warn(e.getMessage());
    }

    public void error(SAXParseException e) throws SAXException {
    	if (log != null)
    		log.error(e.getMessage());
    	ec.setConfigValid(false);
    }

    public void fatalError(SAXParseException e) throws SAXException {
    	if (log != null)
    		log.fatal(e.getMessage());
    	ec.setConfigValid(false);
    }
}
