package com.remindme.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.remindme.models.User;
import com.remindme.repositories.UserRepository;

import software.amazon.awssdk.services.pinpointsmsvoicev2.PinpointSmsVoiceV2Client;
import software.amazon.awssdk.services.pinpointsmsvoicev2.model.SendTextMessageRequest;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String USER = "someone@example.com";

    @Mock
    private PinpointSmsVoiceV2Client pinpointClient;

    @Mock
    private UserRepository userRepository;

    private OtpService otpService() {
        return new OtpService(pinpointClient, userRepository, "+18667773219");
    }

    @Test
    @DisplayName("formats a typed phone number into E.164")
    void formatsPhoneNumber() {
        assertThat(OtpService.formatPhoneNumberToE164("(555) 867-5309")).isEqualTo("+15558675309");
    }

    @Test
    @DisplayName("strips any punctuation the user typed")
    void formatsMessyPhoneNumber() {
        assertThat(OtpService.formatPhoneNumberToE164("555.867.5309")).isEqualTo("+15558675309");
    }

    @Test
    @DisplayName("does not send a message when the rate limit rejects the request")
    void doesNotSendWhenRateLimited() {
        when(userRepository.createOrUpdateUser(any(), any(), any())).thenReturn(false);

        assertThat(otpService().sendOtp(USER, "(555) 867-5309")).isFalse();

        verify(pinpointClient, never()).sendTextMessage(any(SendTextMessageRequest.class));
    }

    @Test
    @DisplayName("sends the code once the user record is updated")
    void sendsCodeWhenAllowed() {
        when(userRepository.createOrUpdateUser(any(), any(), any())).thenReturn(true);

        assertThat(otpService().sendOtp(USER, "(555) 867-5309")).isTrue();

        verify(pinpointClient).sendTextMessage(any(SendTextMessageRequest.class));
    }

    @Test
    @DisplayName("rejects a code for a user that does not exist")
    void rejectsCodeForUnknownUser() {
        when(userRepository.getUser(USER)).thenReturn(null);

        assertThat(otpService().validateOtp(USER, "123456")).isFalse();
    }

    @Test
    @DisplayName("marks the user verified when the code matches")
    void marksVerifiedOnMatchingCode() {
        User user = new User();
        user.setOtpCode("123456");
        when(userRepository.getUser(USER)).thenReturn(user);

        assertThat(otpService().validateOtp(USER, "123456")).isTrue();

        verify(userRepository).updateVerificationStatus(USER, true);
    }

    @Test
    @DisplayName("leaves the user unverified when the code does not match")
    void rejectsMismatchedCode() {
        User user = new User();
        user.setOtpCode("123456");
        when(userRepository.getUser(USER)).thenReturn(user);

        assertThat(otpService().validateOtp(USER, "999999")).isFalse();

        verify(userRepository, never()).updateVerificationStatus(any(), anyBoolean());
    }

    private static boolean anyBoolean() {
        return org.mockito.ArgumentMatchers.anyBoolean();
    }
}
