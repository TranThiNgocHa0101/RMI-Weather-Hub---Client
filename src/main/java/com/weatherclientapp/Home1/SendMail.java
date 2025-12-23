package com.weatherclientapp.Home1;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class SendMail {

    public static void send(String toEmail, String subject, String body) throws Exception {
        if(toEmail == null || toEmail.trim().isEmpty())
            throw new IllegalArgumentException("Recipient email is empty");

        final String fromEmail = "hattn.23itb@vku.udn.vn";
        final String password = "yngmkfpnbspgykce";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });
        session.setDebug(true);

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(fromEmail));
        InternetAddress emailAddr = new InternetAddress(toEmail.trim());
        emailAddr.validate();
        msg.setRecipient(Message.RecipientType.TO, emailAddr);
        msg.setSubject(subject);
        msg.setText(body);

        Transport.send(msg);
        System.out.println("Email sent successfully to " + toEmail);
    }
}
