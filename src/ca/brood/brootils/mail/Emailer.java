package ca.brood.brootils.mail;

import java.util.Properties;

import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

public class Emailer {
	private Properties props;
	private Logger log;
	protected String password;
	protected String username;
	private String smtpHost;
	private MimeMessage theMessage;
}
