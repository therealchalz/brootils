
//Uses code taken from various blogs and tutorials online.
package ca.brood.brootils.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
/*
 * TODO:
 * Support keyfile auth (with and without passphrase)
 */

public class SSHSession {
	private Logger log;
	
	private String username;
	private String host;
	private String password;
	private int port;
	private JSch jsch;
	private int timeoutSeconds;
	private Session tunnelSession;
	
	public SSHSession() {
		log = Logger.getLogger(SSHSession.class);
		jsch = new JSch();
		timeoutSeconds = 5;
		tunnelSession = null;
	}
	
	public boolean areTunnelsActive() {
		if (tunnelSession == null)
			return false;
		return tunnelSession.isConnected();
	}
	
	public void close() {
		if (tunnelSession != null) {
			tunnelSession.disconnect();
		}
	}
	
	public void configure(String host, int port, String user, String password) {
		this.username = user;
		this.host = host;
		this.password = password;
		this.port = port;
	}
	
	public boolean forwardPort(PortForward forward) {
		boolean ret = false;
		try {
			Session ts = getTunnelSession();
			
			if (forward.remoteForward) {
				ts.setPortForwardingR(forward.remotePort, forward.host, forward.localPort);
			} else {
				ts.setPortForwardingL(forward.localPort, forward.host, forward.remotePort);
			}
			
			ret = true;
		} catch (Exception e) {
			log.error("Error setting up tunnel", e);
		}
		return ret;
	}
	
	public boolean localForward(int localPort, String remoteHost, int remotePort) {
		return forwardPort(new PortForward(localPort, remoteHost, remotePort, false));
	}
	
	public boolean remoteForward(int remotePort, String localHost, int localPort) {
		return forwardPort(new PortForward(localPort, localHost, remotePort, true));
	}
	
	public boolean scpFileFromRemote (String remoteFile, String localFile) {
		return scpFile(remoteFile, localFile, false);
	}
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
		session.setPassword(this.password);
		
		session.setTimeout(timeoutSeconds*1000);
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

	private boolean scpFile(String remoteFile, String localFile, boolean toRemote) {
		Session session = null;
		boolean ret = false;
		
		try {
	
			session = createSession();
			session.connect();
	
			String command;
			if (toRemote) {
				command="scp -t "+remoteFile;
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
    	} finally {
    		if (session != null)
    			session.disconnect();
    	}
		return ret;
	}
	
	private boolean sendFile(InputStream in, OutputStream out, String localFileName, String remoteFileName) throws IOException {
		boolean ret = false;
		
		if(checkAck(in)!=0){
			log.error("Can't start because didn't receive ack");
			return false;
		}
		
		File localFile = new File(localFileName);
		
		if (localFile.isDirectory()) {
			log.error("Sending whole directories is not implemented");
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
