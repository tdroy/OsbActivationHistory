package com.troy;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailUtil {

    public void sendEmail(String userName, String passWord, String smtpHost, String smtpPort, String smtpAuth, String smtpTls, 
    		String fromAddress, String toAddress, String subject, String emailBody) {

        final String username = userName;
        final String password = passWord;

        Properties prop = new Properties();
        //prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.host", smtpHost);
        //prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.port", smtpPort);
        //prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.auth", smtpAuth);
        //prop.put("mail.smtp.starttls.enable", "true"); //TLS
        prop.put("mail.smtp.starttls.enable", smtpTls);
        
        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
        	System.out.println("\n[TROY-SendEmail] Sending email to : " + toAddress + " With Subject : " + subject);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(toAddress)
            );
/*            message.setRecipients(
                    Message.RecipientType.CC,
                    InternetAddress.parse(ccAddress)
            );*/
            message.setSubject(subject);
            //message.setText(emailBody);
            message.setContent(emailBody, "text/html");
            
            Transport.send(message);
            

            System.out.println("[TROY-SendEmail] Email sent.");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

}
