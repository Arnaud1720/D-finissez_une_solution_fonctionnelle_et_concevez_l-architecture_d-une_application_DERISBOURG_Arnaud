package com.arn.ycyw.your_car_your_way.entity;

public enum VerificationStatus {
    NONE,       // Pas de verification requise (USER)
    PENDING,    // En attente de verification (EMPLOYEE)
    VERIFIED,   // Verifie par l'admin
    REJECTED    // Refuse par l'admin
}
