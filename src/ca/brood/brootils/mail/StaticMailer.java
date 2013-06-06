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
package ca.brood.brootils.mail;

/** Primitive mailing class that can be used statically to send email.
 * To use it, first call the {@link #configureSmtp(String, String, String)} method to prepare it.
 * Then messages can be sent using {@link #sendEmail(String, String, String, String)}.
 * This class wraps the {@link Emailer} class.  For more functionality, use that class directly.
 * @author Charles Hache
 *
 */
public class StaticMailer {
	private static Emailer emailer;
	
	static {
		emailer = new Emailer();
	}
	
	private StaticMailer() {
		
	}
	
	/** Configures the SMTP settings for this static mailer.
	 * @param host The SMTP host.
	 * @param user The user to log in as.
	 * @param password The user's password.
	 */
	public static synchronized void configureSmtp(String host, String user, String password) {
		emailer.configureSmtp(host, user, password);
	}
	
	/** Attempts to send an email.
	 * @param to The 'to' email address.
	 * @param from The 'from' email address.
	 * @param subject The subject of the email.
	 * @param body The text of the body of the email.
	 * @return true if we were able to dispatch the message, false otherwise.
	 */
	public static synchronized boolean sendEmail(String to, String from, String subject, String body) {
		return emailer.sendEmailSimple(to, from, subject, body);
	}
}
