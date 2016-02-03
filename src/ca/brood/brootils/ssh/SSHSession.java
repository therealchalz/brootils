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
//Uses code taken from various blogs and tutorials online.
package ca.brood.brootils.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
/*
 * TODO:
 * Support log4j logging (need to wrap com.jcraft.jsch.Logger)
 */

/** This class encapsulates JSCH functionality.
 * This class is not for remote command execution, setting up shells etc.
 * It is intended to simplify the setup of port forwards (aka tunnels)
 * and to also allow for SCPing files to and from a remote host.
 * <p>
 * See {@link TunnelKeepaliveThread} for a utility class to keep your tunnels alive.
 * @author Charles Hache
 *
 */
public class SSHSession {
	private Logger log;
	
	private String username;
	private String host;
	private String password = null;
	private int port;
	private JSch jsch;
	private int timeoutSeconds = 20;
	private Session tunnelSession = null;
	
	/**
	 * Creates a new {@link SSHSession}.
	 */
	public SSHSession() {
		log = LogManager.getLogger(SSHSession.class);
		jsch = new JSch();
	}
	
	/** Check the internal SSH session to see if the tunnels are active and connected.
	 * @return false if no tunnels are configured or if the tunnels are not connected.
	 */
	public boolean areTunnelsActive() {
		if (tunnelSession == null)
			return false;
		return tunnelSession.isConnected();
	}
	
	/**
	 * Closes any tunnels associated with this {@link SSHSession}
	 */
	public synchronized void close() {
		if (tunnelSession != null) {
			tunnelSession.disconnect();
			tunnelSession = null;
		}
	}
	
	/** Configures this {@link SSHSession}'s connection.
	 * @param host The hostname or IP address to connect to.
	 * @param port The port to connect to.
	 * @param user The username to login as.
	 */
	public void configure(String host, int port, String user) {
		this.username = user;
		this.host = host;
		this.port = port;
	}
	
	/** Assigns a password to this {@link SSHSession}.
	 * One must either {@link #setPasswordAuth(String)} or {@link #setKeyfileAuth(String, String)} to connect to a remote machine.
	 * If this is called multiple times, only the latest is used for the next session creation.
	 * @param password The password to connect with.
	 */
	public void setPasswordAuth(String password) {
		this.password = password;
	}
	
	/** Assigns a keyfile, and optionally a passphrase, to this {@link SSHSession}
	 * One must either {@link #setPasswordAuth(String)} or {@link #setKeyfileAuth(String, String)} to connect to a remote machine.
	 * If the keyfile does not required a passphrase, then pass in null.
	 * If this is called multiple times then all keys are attached to the session.
	 * @param keyFilePath Path to the private key.
	 * @param passphrase Passphrase for the private key, or null if the key requires no passphrase.
	 * @throws Exception 
	 */
	public void setKeyfileAuth(String keyFilePath, String passphrase) throws Exception {
		if (passphrase != null) {
			jsch.addIdentity(keyFilePath, passphrase);
		} else {
			jsch.addIdentity(keyFilePath);
		}
	}
	
	/** Ensures this {@link SSHSession} is connected then attempts to add a forwarded port.
	 * If this session hasn't already been connected to the host, then it tries to connect.
	 * @param forward The port to forward.
	 * @throws Exception If the port cannot be forwarded.
	 */
	public synchronized void forwardPort(PortForward forward) throws Exception {
		Session ts = getTunnelSession();
		
		if (forward.remoteForward) {
			ts.setPortForwardingR(forward.remotePort, forward.host, forward.localPort);
		} else {
			ts.setPortForwardingL(forward.localPort, forward.host, forward.remotePort);
		}
	}
	
	/** Ensures this {@link SSHSession} is connected then attempts to add a local port forward.
	 * @param localPort The local port to forward.
	 * @param remoteHost The host on the remote end to forward to.
	 * @param remotePort The port on the remote end to forward to.
	 * @throws Exception If the port cannot be forwarded.
	 */
	public void localForward(int localPort, String remoteHost, int remotePort) throws Exception {
		forwardPort(new PortForward(localPort, remoteHost, remotePort, false));
	}
	
	/** Ensures this {@link SSHSession} is connected then attempts to add a remote port forward.
	 * @param remotePort The port on the remote end to forward from.
	 * @param localHost The host on the local end to forward to.
	 * @param localPort The port on the local end to forward to.
	 * @throws Exception If the port cannot be forwarded.
	 */
	public void remoteForward(int remotePort, String localHost, int localPort) throws Exception {
		forwardPort(new PortForward(localPort, localHost, remotePort, true));
	}
	
	/** Attempts to connect this {@link SSHSession} and SCP a file from the remote host to the local machine.
	 * @param remoteFile The path of the remote file to download.
	 * @param localFile The path of the local file to save to.
	 * @return true if the file was transferred, false otherwise.
	 */
	public boolean scpFileFromRemote (String remoteFile, String localFile) {
		return scpFile(remoteFile, localFile, false);
	}
	/** Attempts to connect this {@link SSHSession} and SCP a file from the local machine to the remote host.
	 * @param remoteFile The path of the remote file to save to.
	 * @param localFile The path of the local file to upload.
	 * @return true if the file was transferred, false otherwise.
	 */
	public boolean scpFileToRemote(String remoteFile, String localFile) {
		return scpFile(remoteFile, localFile, true);
	}
	
	private int checkAck(InputStream in) throws IOException{
		int b=in.read();
		// b may be 0 for success,
		//          1 for error,
		//          2 for fatal error,
		//          -1
		if(b==0) return b;
		if(b==-1) return b;
		
		if(b==1 || b==2){
			StringBuffer sb=new StringBuffer();
			int c;
			do {
				c=in.read();
				sb.append((char)c);
			} while(c!='\n');
			if(b==1){ // error
				log.error(sb.toString());
			}
			if(b==2){ // fatal error
				log.fatal(sb.toString());
			}
		}
		return b;
	}
	
	private Session createSession() throws Exception {
		Session session = jsch.getSession(this.username, this.host, this.port);
		if (this.password != null)
			session.setPassword(this.password);
		
		session.setTimeout(timeoutSeconds*1000);
		//TODO: review this (StrictHostKeyChecking)
		session.setConfig("StrictHostKeyChecking", "no");
		return session;
	}
	
	private synchronized Session getTunnelSession() throws Exception {
		if (tunnelSession == null) {
			tunnelSession = createSession();
			tunnelSession.connect();
		}
		return tunnelSession;
	}
	
	//return 0 if we got all the data successfully
	//return -1 on fatal error
	private int receiveFile(InputStream in, OutputStream out, String localFileName) throws IOException {
		int ret = -1;
		byte[] buf=new byte[1024];
		log.trace("Receiving file.");
		// read '0644 '
		if (in.read(buf, 0, 5) != 5) {
			return -1;
		}

		// read file size
		long bytesRemaining=0L;
		while(in.read(buf, 0, 1)>0){
			if(buf[0]==' ')break;
			bytesRemaining=bytesRemaining*10L+(long)(buf[0]-'0');
		}
		
		//Allow size 0 files
//		if (bytesRemaining == 0) {
//			return -1;
//		}

		String fileName=null;
		for(int i=0;i<256;i++){
			in.read(buf, i, 1);
			if(buf[i]==(byte)0x0a){
				fileName=new String(buf, 0, i);
				break;
			}
		}
		
		if (fileName == null) {
			return -1;
		}

		log.debug("bytesRemaining="+bytesRemaining+", fileName="+fileName);

		// send '\0'
		buf[0]=0; out.write(buf, 0, 1); out.flush();
		
		FileOutputStream fos = null;
		try {
			File localFile = new File(localFileName);
			if(localFile.isDirectory()){
				localFileName=localFile+File.separator+fileName;
				fos = new FileOutputStream(localFileName);
			} else {
				fos = new FileOutputStream(localFile);
			}
			
			int length;
			int readRet;
			while(bytesRemaining >0){
				if (buf.length < bytesRemaining) 
					length = buf.length;
				else 
					length = (int)bytesRemaining;
				
				readRet = in.read(buf, 0, length);
				if(readRet < 0){ 
					break;
				}
				fos.write(buf, 0, readRet);
				
				bytesRemaining -= readRet;
			}
			
			if (bytesRemaining != 0) {
				return -1;
			}
	
			if(checkAck(in) == 0){
				ret = 0;
			}
			
			// send '\0'
			buf[0]=0; out.write(buf, 0, 1); out.flush();
		} finally {
			fos.close();
		}
		
		return ret;
	}
	
	private boolean receiveFiles(InputStream in, OutputStream out, String localFile) {
		int success = -1;
		byte[] buf=new byte[1];
		try {
			// send '\0'
			buf[0]=0; out.write(buf, 0, 1); out.flush();
		
			do {
				int c=checkAck(in);
				if(c!='C'){
					break;
				}
				
				success = receiveFile(in, out, localFile);
			} while (success != 0);
		} catch (Exception e) {
			log.error("Exception while receiving files", e);
		}
		if (success == 0)
			return true;
		return false;
	}

	private synchronized boolean scpFile(String remoteFile, String localFile, boolean toRemote) {
		Session session = null;
		boolean ret = false;
		
		try {
	
			session = createSession();
			session.connect();
	
			String command;
			if (toRemote) {
				command="scp -t "+remoteFile;

				//Make sure the remote directory exists
				String mkdir = "mkdir -p "+new File(remoteFile).getParent();
				
				Channel mkdirChannel = session.openChannel("exec");
				((ChannelExec)mkdirChannel).setCommand(mkdir);
				((ChannelExec)mkdirChannel).setErrStream(System.out, true);
				mkdirChannel.connect();
			} else {
				command="scp -f "+remoteFile;
			}

			Channel channel=session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);
	
			// get I/O streams for remote scp
			OutputStream out=channel.getOutputStream();
			InputStream in=channel.getInputStream();
	
			channel.connect();
			
			if (toRemote) {
				ret = sendFile(in, out, localFile, null);
			} else {
				ret = receiveFiles(in, out, localFile);
			}
			
    	} catch(Exception e){
    		log.error("Error SCPing file", e);
    		ret = false;
    	} finally {
    		if (session != null)
    			session.disconnect();
    	}
		return ret;
	}
	
	private boolean sendFile(InputStream in, OutputStream out, String localFileName, String remoteFileName) throws IOException {
		
		if(checkAck(in)!=0){
			log.error("Can't start because didn't receive ack");
			return false;
		}
		
		File localFile = new File(localFileName);
		
		if (localFile.isDirectory()) {
			log.error("Sending whole directories is not implemented");
			return false;
		}

		if (!localFile.canRead()) {
			log.error("Cannot read the file: "+localFile.getAbsolutePath());
			return false;
		}
		
		 // send "C0644 filesize filename", where filename should not include '/'
		long filesize=localFile.length();
		String command = "C0644 "+filesize+" ";
		if (remoteFileName == null) {
			if(localFileName.lastIndexOf('/')>0){
				command+=localFileName.substring(localFileName.lastIndexOf('/')+1);
			} else {
				command+=localFileName;
			}
		} else {
			if(remoteFileName.lastIndexOf('/')>0){
				command+=remoteFileName.substring(remoteFileName.lastIndexOf('/')+1);
			} else {
				command+=remoteFileName;
			}
		}
		command+="\n";
		
		out.write(command.getBytes()); out.flush();
		
		if(checkAck(in)!=0){
			log.error("Lost ack after sending file info");
			return false;
		}
		
		// send a content of lfile
		FileInputStream fis=new FileInputStream(localFile);
		byte[] buf=new byte[1024];
		
		try {
			int len;
			while( (len = fis.read(buf, 0, buf.length) ) > 0 ){
				out.write(buf, 0, len);
			}
		} finally {
			fis.close();
		}
			
		// send '\0'
		buf[0]=0; out.write(buf, 0, 1); out.flush();
		
		if(checkAck(in)!=0){
			return false;
		}
		
		return true;
	}
}
