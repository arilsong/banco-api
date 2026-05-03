package com.bni.api.core.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Builder.Default
    private String partyType = "CONSUMER";

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
