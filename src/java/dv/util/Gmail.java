package dv.util;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Gmail {

	static Properties mailServerProperties;
	static Session getMailSession;
	static MimeMessage generateMailMessage;

	public static void send(String sender, String senderPw,
    String tos, String ccs, String subject, String body, String mimeType0) throws AddressException, MessagingException {

		// Step1
		System.out.println("\n 1st ===> setup Mail Server Properties..");
		mailServerProperties = System.getProperties();
		mailServerProperties.put("mail.smtp.port", "587");
		mailServerProperties.put("mail.smtp.auth", "true");
		mailServerProperties.put("mail.smtp.starttls.enable", "true");
		System.out.println("Mail Server Properties have been setup successfully..");

		// Step2
		System.out.println("\n\n 2nd ===> get Mail Session..");
		getMailSession = Session.getDefaultInstance(mailServerProperties, null);
		generateMailMessage = new MimeMessage(getMailSession);
    String[] tosArr = tos.split(",");
    for (String to: tosArr) {
        if (to == null || to.trim().length() == 0) {
            continue;
        }
    		generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to.trim()));
    }

    String[] ccsArr = ccs.split(",");
    for (String cc: ccsArr) {
        if (cc == null || cc.trim().length() == 0) {
            continue;
        }
    		generateMailMessage.addRecipient(Message.RecipientType.CC, new InternetAddress(cc.trim()));
    }

		generateMailMessage.setSubject(subject);
    String mimeType = mimeType0;
    if (mimeType == null) {
        mimeType = "text/plain";
    }
		generateMailMessage.setContent(body, mimeType);
		System.out.println("Mail Session has been created successfully..");

		// Step3
		System.out.println("\n\n 3rd ===> Get Session and Send mail");
		Transport transport = getMailSession.getTransport("smtp");

		// Enter your correct gmail UserID and Password
		// if you have 2FA enabled then provide App Specific Password
		transport.connect("smtp.gmail.com", sender, senderPw);
		transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
		transport.close();
	}
}
