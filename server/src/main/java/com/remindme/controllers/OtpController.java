package com.remindme.controllers;

import com.remindme.services.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendOtp(@Valid HttpServletRequest request, @RequestParam String phoneNumber) {
        String userId = (String) request.getAttribute("userId");
        boolean success = otpService.sendOtp(userId, phoneNumber);

        if (success) {
            return ResponseEntity.ok("OTP sent successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send OTP");
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<String> validateOtp(@Valid HttpServletRequest request, @RequestParam String otpCode) {
        String userId = (String) request.getAttribute("userId");
        boolean success = otpService.validateOtp(userId, otpCode);

        if (success) {
            return ResponseEntity.ok("OTP validated successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid OTP");
        }
    }
}
