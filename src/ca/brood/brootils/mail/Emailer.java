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
package ca.brood.brootils.mail;

import java.util.Date;
import java.util.Properties;
import java.util.Set;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

/** This is a class for sending emails.
 * @author Charles
 *
 */
public class Emailer {
	private class SMTPAuthenticator extends javax.mail.Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
           return new PasswordAuthentication(username, password);
        }
    }
	private Properties props;
	private Logger log;
	private String password;
	private String username;
	private String smtpHost;
	
	private MimeMessage theMessage;
	
	/** Initializes an unconfigured emailer.
	 * 
	 */
	public Emailer() {
		props = new Properties();
		log = Logger.getLogger(Emailer.class);
	}
	
	/** Adds all the passed in email addresses to the 'To' field of the next email to send.
	 * The emailer must be configured before you do this.
	 * @param allTo Array of email addresses.
	 */
	public void addAllRecipients(Set<String> allTo) {
		for (String s : allTo)
			addRecipient(s);
	}
	
	/** Adds the passed in email address to the 'To' field of the next email to send.
	 * The emailer must be configured before you do this.
	 * @param to The address to add.
	 */
	public void addRecipient(String to) {
		addRecipient(Message.RecipientType.TO, to);
	}
	
	/** Adds the passed in email address to the 'Bcc' field of the next email to send.
	 * The emailer must be configured before you do this.
	 * @param to The address to add.
	 */
	public void addRecipientBCC(String to) {
		addRecipient(Message.RecipientType.BCC, to);
	}
	
	/** Adds the passed in email address to the 'Cc' field of the next email to send.
	 * The emailer must be configured before you do this.
	 * @param to The address to add.
	 */
	public void addRecipientCC(String to) {
		addRecipient(Message.RecipientType.CC, to);
	}
	
	/** Configures this emailer's SMTP session.
	 * The emailer must be configured before anything else can happen (adding recipients, for example).
	 * @param host The host of the SMTP server.
	 * @param user The SMTP username.
	 * @param password The SMTP user's password.  Pass null or an empty string if no auth is required.
	 */
	public void configureSmtp(String host, String user, String password) {
		this.smtpHost = host;
		this.props.put("mail.smtp.host", host);
		
		this.username = user;
		this.props.put("mail.smtp.user", user);
		
		this.password = password;
		if (this.password != null && this.password.length() > 0) {
			this.props.put("mail.smtp.auth", "true");
		}
		
		spawnMessage();
	}
	
	/** Sends a text email to recipients that are already configured.
	 * @param from The from email address.
	 * @param subject The subject of the email.
	 * @param body The body of the email.
	 * @return true if the email was dispatched, false otherwise.
	 */
	public boolean sendEmail(String from, String subject, String body) {
		return sendEmailContent(from, subject, body, "text/plain; charset=utf-8");		
	}
	
	/** Sends a text based email.
	 * Typically the message only goes to the address in the 'to' parameter, but if you previously 
	 * called one of the addRecipient functions and haven't sent the message yet, then it will go there too.
	 * @param to Email address destination.
	 * @param from The 'from' email address.
	 * @param subject The subject of the message.
	 * @param body The body of the message.
	 * @return true if the message was dispatched, false otherwise.
	 */
	public boolean sendEmailSimple(String to, String from, String subject, String body) {
		if (theMessage == null) {
			log.error("Trying to send message but SMTP is not configured.");
			return false;
		}
		try {
			addRecipient(to);
			return sendEmail(from, subject, body);
		} catch (Exception e) {
			log.error("Got exception while sending email.", e);
		}
		return false;
	}
	
	/** Similar to {@link #sendEmail(String, String, String)}, except for HTML encoded messages.
	 * @param from The 'from' email address;
	 * @param subject The subject of the email.
	 * @param body The HTML body of the email.
	 * @return true if the message was dispatched, false otherwise.
	 */
	public boolean sendHtmlEmail(String from, String subject, String body) {
		return sendEmailContent(from, subject, body, "text/html; charset=utf-8");
	}
	
	private void addRecipient(Message.RecipientType type, String address) {
		if (theMessage == null) {
			log.error("Emailer not configured - cannot add recipients yet.");
			throw new RuntimeException("Emailer not configured");
		}
		try {
			theMessage.addRecipient( type, new InternetAddress(address)	);
		} catch (Exception e) {
			log.error("Got exception while attempting to add recipient to email.", e);
		}
	}
	
	private boolean sendEmailContent(String from, String subject, String body, String mimeType) {
		if (smtpHost == null || theMessage == null) {
			log.error("Emailer not configured - can't send email.");
			return false;
		}
		
		try {
			if (theMessage.getAllRecipients().length < 1) {
				log.error("Email has no recipients - can't send email");
				return false;
			}
		} catch (MessagingException e1) {
			log.error("Error while retrieving recipients", e1);
			return false;
		}
		
		boolean ret = false;

		try {
			theMessage.setFrom(new InternetAddress(from));
			theMessage.setSubject( subject );
			theMessage.setContent( body, mimeType );
			theMessage.setSentDate(new Date());
			
			Transport.send( theMessage );
			
			spawnMessage();
			
			ret = true;
		} catch (MessagingException e) {
			log.error("Cannot send email. ",e);
		} 
		
		return ret;
	}
	
	private void spawnMessage() {
		Authenticator auth = new SMTPAuthenticator();
		Session session = Session.getDefaultInstance( props, auth );
	    theMessage = new MimeMessage( session );
	}
}
