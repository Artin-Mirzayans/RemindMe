package com.remindme.services;

import software.amazon.awssdk.services.pinpointsmsvoicev2.PinpointSmsVoiceV2Client;
import software.amazon.awssdk.services.pinpointsmsvoicev2.model.SendTextMessageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.remindme.models.User;
import com.remindme.repositories.UserRepository;

@Service
public class OtpService {

    private final PinpointSmsVoiceV2Client pinpointSmsVoiceV2Client;
    private final UserRepository userRepository;
    private final String originationIdentity;

    public OtpService(PinpointSmsVoiceV2Client pinpointSmsVoiceV2Client, UserRepository userRepository,
            @Value("${pinpoint.origination_number}") String originationIdentity) {
        this.pinpointSmsVoiceV2Client = pinpointSmsVoiceV2Client;
        this.userRepository = userRepository;
        this.originationIdentity = originationIdentity;
    }

    public boolean sendOtp(String userId, String phoneNumber) {
        String otpCode = generateOtpCode();
        String formattedPhoneNumber = formatPhoneNumberToE164(phoneNumber);
        String messageBody = "Your RemindMe verification code is: " + otpCode;

        boolean updateSuccess = userRepository.createOrUpdateUser(phoneNumber, otpCode, userId);

        if (!updateSuccess) {
            return false;
        }

        try {
            SendTextMessageRequest request = SendTextMessageRequest.builder()
                    .destinationPhoneNumber(formattedPhoneNumber)
                    .messageBody(messageBody)
                    .originationIdentity(originationIdentity)
                    .build();

            pinpointSmsVoiceV2Client.sendTextMessage(request);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean validateOtp(String userId, String otpCode) {
        User user = userRepository.getUser(userId);
        if (user == null) {
            return false;
        }

        if (otpCode.equals(user.getOtpCode())) {
            userRepository.updateVerificationStatus(userId, true);
            return true;
        } else {
            return false;
        }
    }

    private String generateOtpCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    public static String formatPhoneNumberToE164(String phoneNumber) {
        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");
        return "+1" + digitsOnly;
    }
}
