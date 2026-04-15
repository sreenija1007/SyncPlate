package com.biobite.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String toEmail, String name, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("kanugondasreenija@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("Verify your Syncplate account 🥗");

            String html = """
                <div style="font-family: 'Segoe UI', sans-serif; max-width: 600px; margin: 0 auto; background: #faf7f2; padding: 40px; border-radius: 16px;">
                    <div style="text-align: center; margin-bottom: 32px;">
                        <h1 style="color: #1a3a2a; font-size: 32px; margin: 0;">🥗 Syncplate</h1>
                        <p style="color: #8a8a8a; margin-top: 8px;">Food that works for YOUR body.</p>
                    </div>
                    <div style="background: white; border-radius: 16px; padding: 32px;">
                        <h2 style="color: #1a1a1a; margin-bottom: 12px;">Hi %s! 👋</h2>
                        <p style="color: #4a4a4a; line-height: 1.6;">
                            Welcome to Syncplate! You're one step away from getting personalized
                            food recommendations tailored to your unique health profile.
                        </p>
                        <p style="color: #4a4a4a; line-height: 1.6;">
                            Please verify your email address to activate your account:
                        </p>
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="http://localhost:3000/verify?token=%s"
                               style="background: #1a3a2a; color: white; padding: 16px 36px;
                                      border-radius: 30px; text-decoration: none; font-size: 16px;
                                      font-weight: 600;">
                                ✅ Verify My Email
                            </a>
                        </div>
                        <p style="color: #8a8a8a; font-size: 13px; text-align: center;">
                            This link expires in 24 hours. If you didn't create a Syncplate account,
                            you can safely ignore this email.
                        </p>
                    </div>
                    <p style="color: #8a8a8a; font-size: 12px; text-align: center; margin-top: 24px;">
                        © 2025 Syncplate • Food that works for YOUR body.
                    </p>
                </div>
            """.formatted(name, token);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Failed to send verification email: " + e.getMessage());
        }
    }
}
