package com.carboncredit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MailConfig.MailProperties.class)
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(MailConfig.MailProperties mailProperties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Get properties from environment variables first, then fall back to app properties
        String host = System.getenv("MAIL_HOST");
        String port = System.getenv("MAIL_PORT");
        String username = System.getenv("EMAIL_USERNAME");
        String password = System.getenv("EMAIL_PASSWORD");
        
        // Use application.yml properties as fallback
        if (host == null) host = mailProperties.getHost();
        if (port == null) port = String.valueOf(mailProperties.getPort());
        if (username == null) username = mailProperties.getUsername();
        if (password == null) password = mailProperties.getPassword();
        
        // Final defaults if still not set
        if (host == null || host.isEmpty()) host = "smtp.gmail.com";
        if (port == null || port.isEmpty()) port = "587";
        if (username == null || username.isEmpty()) username = "your-email@gmail.com";
        if (password == null || password.isEmpty()) password = "your-app-password";
        
        mailSender.setHost(host);
        try {
            mailSender.setPort(Integer.parseInt(port));
        } catch (NumberFormatException e) {
            mailSender.setPort(587);
        }
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.mime.charset", "UTF-8");
        
        return mailSender;
    }
    
    /**
     * POJO for mail properties from application.yml
     */
    @ConfigurationProperties(prefix = "spring.mail")
    public static class MailProperties {
        private String host;
        private int port = 587;
        private String username;
        private String password;
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
