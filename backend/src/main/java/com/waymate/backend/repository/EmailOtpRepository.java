package com.waymate.backend.repository;


import com.waymate.backend.entity.EmailOtp;
import com.waymate.backend.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {

    //gets the latest OTP record for an email.
    Optional<EmailOtp> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    //removes older OTP records when a new OTP is requested.
    void deleteByEmailAndPurpose(String email,OtpPurpose purpose);
}
