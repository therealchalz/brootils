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
package ca.brood.brootils.daemon;


import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.log4j.Logger;

import ca.brood.brootils.ssh.TunnelKeepaliveThread;

public class BrootilsDaemon implements Daemon {
	private static BrootilsDaemon brootilsDaemon;
	
	private TunnelKeepaliveThread tunnelThread;
	private Logger log;
	
	static {
		brootilsDaemon = new BrootilsDaemon();
	}
	
	public BrootilsDaemon() {
		log = Logger.getLogger(BrootilsDaemon.class);
		tunnelThread = new TunnelKeepaliveThread();
	}
	
	private void brootilsStart() {
		log.info("BrootilsDaemon is starting...");
		tunnelThread.start();
	}
	private void brootilsStop() {
		log.info("BrootilsDaemon is stopping...");
		tunnelThread.stop();
		log.info("BrootilsDaemon is done.");
	}
	
	@Override
    public void destroy() {
        log.info("Linux daemon received destroy command");
    }

    @Override
    public void init(DaemonContext arg0) throws DaemonInitException, Exception {
        /* I think if jsvc is configured correctly, then this method is 
         * called as the root user.  After it returns, then start is called
         * as the regular user.
         */
    	//TODO: get an xml config file from the command line
    	log.info("Linux daemon received init");
    }

    @Override
    public void start() throws Exception {
    	log.info("Linux daemon received start command");
    	brootilsDaemon.brootilsStart();
    }

    @Override
    public void stop() throws Exception {
    	log.info("Linux daemon received stop command");
    	brootilsDaemon.brootilsStop();
    }
    /**
     * Static methods called by prunsrv to start/stop
     * the Windows service.  Pass the argument "start"
     * to start the service, and pass "stop" to
     * stop the service.
     * Stolen from FAQ at commons daemon.
     */
    public static void windowsService(String[] args) {
        String cmd = "start";
        if (args.length > 0) {
            cmd = args[0];
        }

        if ("start".equals(cmd)) {
        	brootilsDaemon.log.info("Windows service received Start command");
        	brootilsDaemon.brootilsStart();
        } else if ("stop".equals(cmd)) {
        	brootilsDaemon.log.info("Windows service received Stop command");
        	brootilsDaemon.brootilsStop();
        } else {
        	brootilsDaemon.log.error("Unrecognized service option: "+cmd);
        }
    }

}
