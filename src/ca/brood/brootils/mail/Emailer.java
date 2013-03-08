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

public class Emailer {
	private Properties props;
	private Logger log;
	private String password;
	private String username;
	private String smtpHost;
	private MimeMessage theMessage;
	
	public Emailer() {
		props = new Properties();
		log = Logger.getLogger(Emailer.class);
	}
	
	public void configureSmtp(String host, String user, String password) {
		this.smtpHost = host;
		this.props.put("mail.smtp.host", host);
		
		this.username = user;
		this.props.put("mail.smtp.user", user);
		
		this.password = password;
		if (this.password.length() > 0) {
			this.props.put("mail.smtp.auth", "true");
		}
		
		spawnMessage();
	}
	
	public void addRecipient(String to) {
		addRecipient(Message.RecipientType.TO, to);
	}
	
	public void addAllRecipients(Set<String> allTo) {
		for (String s : allTo)
			addRecipient(s);
	}
	
	public void addRecipientBCC(String to) {
		addRecipient(Message.RecipientType.BCC, to);
	}
	
	public boolean sendEmail(String from, String subject, String body) {
		return sendEmailContent(from, subject, body, "text/plain; charset=utf-8");		
	}
	
	public boolean sendHtmlEmail(String from, String subject, String body) {
		return sendEmailContent(from, subject, body, "text/html; charset=utf-8");
	}
	
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
	
	private void addRecipient(Message.RecipientType type, String address) {
		try {
			theMessage.addRecipient( type, new InternetAddress(address)	);
		} catch (Exception e) {
			log.error("Got exception while attempting to add receipient to email address.", e);
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
	
	private class SMTPAuthenticator extends javax.mail.Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
           return new PasswordAuthentication(username, password);
        }
    }
	
	private void spawnMessage() {
		Authenticator auth = new SMTPAuthenticator();
		Session session = Session.getDefaultInstance( props, auth );
	    theMessage = new MimeMessage( session );
	}
}
