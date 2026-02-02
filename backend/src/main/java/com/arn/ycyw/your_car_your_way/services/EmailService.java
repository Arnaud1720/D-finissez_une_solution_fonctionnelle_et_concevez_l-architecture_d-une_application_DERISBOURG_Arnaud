package com.arn.ycyw.your_car_your_way.services;

import com.arn.ycyw.your_car_your_way.entity.Users;

public interface EmailService {

    /**
     * Envoie un email à l'admin pour valider/refuser un compte professionnel
     */
    void sendEmployeeValidationRequestToAdmin(Users employee, String verificationToken);

    /**
     * Envoie un email au professionnel pour l'informer du résultat de la vérification
     */
    void sendEmployeeVerificationResult(Users employee, boolean approved);
}
