package com.arn.ycyw.your_car_your_way.services.impl;

import com.arn.ycyw.your_car_your_way.entity.Users;
import com.arn.ycyw.your_car_your_way.services.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.admin.email:arnaud1720@gmail.com}")
    private String adminEmail;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmployeeValidationRequestToAdmin(Users employee, String verificationToken) {
        try {
            System.out.println("➡️ Envoi de demande de validation pour " + employee.getEmail() + " à l'admin");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("🆕 Nouvelle demande de compte professionnel - "
                    + employee.getFirstName() + " " + employee.getLastName());

            String validateUrl = baseUrl + "/api/user/verify?token=" + verificationToken + "&action=approve";
            String rejectUrl = baseUrl + "/api/user/verify?token=" + verificationToken + "&action=reject";

            String htmlContent = buildAdminValidationEmailHtml(employee, validateUrl, rejectUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("✅ Email de demande de validation envoyé à " + adminEmail);

        } catch (Exception e) {
            System.out.println("❌ Erreur lors de l'envoi de l'email de validation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void sendEmployeeVerificationResult(Users employee, boolean approved) {
        try {
            System.out.println("➡️ Envoi du résultat de vérification à " + employee.getEmail());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(employee.getEmail());

            if (approved) {
                helper.setSubject("✅ Votre compte professionnel a été validé - Your Car Your Way");
            } else {
                helper.setSubject("❌ Votre demande de compte professionnel a été refusée - Your Car Your Way");
            }

            String htmlContent = buildVerificationResultEmailHtml(employee, approved);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("✅ Email de résultat envoyé à " + employee.getEmail());

        } catch (Exception e) {
            System.out.println("❌ Erreur lors de l'envoi de l'email de résultat : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildAdminValidationEmailHtml(Users employee, String validateUrl, String rejectUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .info-card { background: #f8fafc; border-radius: 8px; padding: 20px; margin: 15px 0; }
                    .buttons { text-align: center; margin: 30px 0; }
                    .btn { display: inline-block; padding: 15px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 0 10px; }
                    .btn-approve { background: #22c55e; color: white; }
                    .btn-reject { background: #ef4444; color: white; }
                    .footer { background: #f8fafc; padding: 20px; text-align: center; color: #64748b; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚗 Your Car Your Way</h1>
                        <p>Nouvelle demande de compte professionnel</p>
                    </div>
                    <div class="content">
                        <h2>Demande de validation</h2>
                        <p>Un nouvel utilisateur souhaite s'inscrire en tant que <strong>professionnel</strong>.</p>
                        <div class="info-card">
                            <p><strong>Nom :</strong> %s %s</p>
                            <p><strong>Email :</strong> %s</p>
                        </div>
                        <div class="buttons">
                            <a href="%s" class="btn btn-approve">✓ Valider le compte</a>
                            <a href="%s" class="btn btn-reject">✕ Refuser</a>
                        </div>
                    </div>
                    <div class="footer"><p>Your Car Your Way - Administration</p></div>
                </div>
            </body>
            </html>
            """.formatted(
                employee.getFirstName(), employee.getLastName(),
                employee.getEmail(), validateUrl, rejectUrl
        );
    }

    private String buildVerificationResultEmailHtml(Users employee, boolean approved) {
        String status = approved ? "validé" : "refusé";
        String color = approved ? "#22c55e" : "#ef4444";
        String badge = approved ? "✓ Validé" : "✕ Refusé";
        String body = approved
                ? "Votre demande de compte professionnel a été <strong>approuvée</strong>. Vous pouvez maintenant vous connecter."
                : "Votre demande de compte professionnel a été <strong>refusée</strong>. Contactez-nous pour plus d'informations.";

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; }
                    .header { background: %s; color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .badge { display: inline-block; background: %s; color: white; padding: 8px 16px; border-radius: 20px; font-weight: bold; }
                    .footer { background: #f8fafc; padding: 20px; text-align: center; color: #64748b; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚗 Your Car Your Way</h1>
                        <p>Compte professionnel %s</p>
                    </div>
                    <div class="content">
                        <span class="badge">%s</span>
                        <h2>Bonjour %s,</h2>
                        <p>%s</p>
                    </div>
                    <div class="footer"><p>Your Car Your Way - Location de véhicules en Europe</p></div>
                </div>
            </body>
            </html>
            """.formatted(color, color, status, badge, employee.getFirstName(), body);
    }
}
