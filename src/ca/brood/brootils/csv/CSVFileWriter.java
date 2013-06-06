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
package ca.brood.brootils.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;


/** Simple class for writing CSV files
 * @author Charles Hache
 *
 */
public class CSVFileWriter {
	private String filename;
	private File file;
	private boolean writeHeaders;
	private Logger log;
	private ArrayList<String> headers;
	private byte[] newline;
	
	/** Creates a writer that will write to the given filename.
	 * @param fileName The name of the CSV file.
	 */
	public CSVFileWriter (String fileName) {
		log = Logger.getLogger(CSVFileWriter.class);
		this.filename = fileName;
		writeHeaders = true;
		headers = new ArrayList<String>();
		newline = System.getProperty("line.separator").getBytes();
	}
	
	/** Copy constructor.
	 * @param o Other CSV writer to copy.
	 */
	public CSVFileWriter(CSVFileWriter o) {
		this(o.filename);
		this.writeHeaders = o.writeHeaders;
		for (String s : o.headers) {
			this.headers.add(s);
		}
		newline = new byte[o.newline.length];
		System.arraycopy(o.newline, 0, newline, 0, newline.length);
	}
	
	/** Gets the filename of this writer.
	 * @return The filename.
	 */
	public String getFilename() {
		return filename;
	}
	
	/** Changes the filename of this writer.
	 * The next time data is written, the new file will be created.
	 * Headers will be written automatically.
	 * @param name The new filename.
	 */
	public synchronized void setFilename(String name) {
		filename = name;
		file = null;	//recreate the file next write
	}
	
	/** Sets the headers for this CSV file.
	 * @param heads An array of headers.  One element per column.
	 */
	public synchronized void setHeaders(ArrayList<String> heads) {
		headers = heads;
	}
		
	private synchronized void initialize() {
		
		log.debug("Initializing file: "+filename);
		file = new File(filename);
		try {
			file.createNewFile();
		} catch (IOException e) {
			log.error("Error creating file", e);
		}
		writeHeaders = true;
	}
	
	/** Writes data to the CSV file.
	 * Creates the file if it doesn't exist.
	 * Writes the headers if a new file is created.
	 * @param values The data to write.
	 * @return true if all went well, false otherwise.
	 */
	public synchronized boolean writeData(ArrayList<String> values) {
		boolean ret = false;
		
		if (file == null)
			initialize();
		if (!file.canWrite()) {
			log.debug("Can't write to file.");
			initialize();
			if (!file.canWrite()) {
				log.error("Couldn't get a writable file. Aborting");
				return false;
			}
		}
		
		FileOutputStream fos = null;
		for (int tryNo = 1; tryNo <=2; tryNo++) {
			try {
				fos = new FileOutputStream(file, true);
			} catch (Exception e) {
				if (tryNo == 1) {
					log.error("Couldn't get an output stream on the file, trying again.");
					initialize();
				} else if (tryNo == 2) {
					log.error("Failed to get an output stream on a file. Aborting.");
					return false;
				}
			}
		}
		
		try {
			boolean first = true;
			if (writeHeaders) {
				for (String head : headers) {
					if (!first)
						fos.write(", ".getBytes());
					else
						first = false;
					writeEscapedCSVData(fos, head);
				}
				fos.write(newline);
				writeHeaders = false;
			}
			first = true;
			for (String val : values) {
				if (!first)
					fos.write(", ".getBytes());
				else
					first = false;
				writeEscapedCSVData(fos, val);
			}
			fos.write(newline);
			ret = true;
		} catch (Exception e) {
			log.error("Failed in writing to file", e);
			ret = false;
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
				log.error("Failed to close output stream",e);
				ret = false;
			}
		}
		return ret;
	}
	private void writeEscapedCSVData(OutputStream o, String data) throws IOException {
		//If there is a comma in the data, then we need to enclose it in double quotes.
		//But, then if there is a double quote in there, we need to replace it with double doublequotes.
		if (data.contains(",")) {
			if (data.contains("\"")) {
				data = data.replace("\"", "\"\"");
			}
			o.write(("\""+data+"\"").getBytes());
		} else {
			o.write(data.getBytes());
		}
	}
}
