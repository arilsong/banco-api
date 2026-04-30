package com.bca.api.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
    private String msisdn;

    // "CONSUMER" or "BUSINESS" — determines party type returned to Mojaloop
    @Builder.Default
    private String partyType = "CONSUMER";

    // Business registration number — used when idType=BUSINESS in party lookup
    private String businessId;

    private String displayName;
    private String firstName;
    private String lastName;
    private String dateOfBirth;

    private BigDecimal balance;
    private String currency;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private CoreUser user;
}
