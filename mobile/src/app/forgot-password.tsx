import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import {
  AuthColors,
  AuthField,
  AuthHeading,
  AuthPage,
  BackButton,
  FormMessage,
  PrimaryButton,
  authStyles,
} from '@/components/auth-ui';
import { authApi } from '@/lib/auth-api';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type ResetStep = 1 | 2 | 3 | 4;

export default function ForgotPasswordScreen() {
  const [step, setStep] = useState<ResetStep>(1);
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (step !== 4) {
      return;
    }

    const redirectTimer = setTimeout(() => router.replace('/login'), 1800);
    return () => clearTimeout(redirectTimer);
  }, [step]);

  function goBack() {
    if (step === 1) {
      router.back();
      return;
    }
    if (step === 2) {
      setStep(1);
    } else if (step === 3) {
      setStep(2);
    } else {
      return;
    }
    setMessage('');
    setSuccess(false);
  }

  async function requestReset() {
    const normalizedEmail = email.trim().toLowerCase();
    if (!emailPattern.test(normalizedEmail)) {
      setMessage('Enter a valid email address.');
      setSuccess(false);
      return;
    }

    setLoading(true);
    setMessage('');
    setSuccess(false);
    try {
      const response = await authApi.forgotPassword(normalizedEmail);
      setEmail(normalizedEmail);
      setOtp('');
      setMessage(response.message || 'A verification code has been sent.');
      setSuccess(true);
      setStep(2);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not request a password reset.');
    } finally {
      setLoading(false);
    }
  }

  async function verifyResetOtp() {
    if (!/^\d{6}$/.test(otp)) {
      setMessage('Enter the 6-digit code from your email.');
      setSuccess(false);
      return;
    }

    setLoading(true);
    setMessage('');
    setSuccess(false);
    try {
      const response = await authApi.verifyResetOtp(email, otp);
      if (!response.verified) {
        throw new Error(response.message || 'That code could not be verified.');
      }
      if (response.email) {
        setEmail(response.email);
      }
      setMessage(response.message || 'OTP verified. Choose a new password.');
      setSuccess(true);
      setStep(3);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not verify the OTP.');
    } finally {
      setLoading(false);
    }
  }

  async function resetPassword() {
    if (newPassword.length < 8) {
      setMessage('Your new password must be at least 8 characters.');
      setSuccess(false);
      return;
    }

    setLoading(true);
    setMessage('');
    setSuccess(false);
    try {
      const response = await authApi.resetPassword(email, newPassword);
      setMessage(
        typeof response === 'string' && response.trim()
          ? response.trim()
          : 'Password reset successfully!',
      );
      setSuccess(true);
      setNewPassword('');
      setStep(4);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not reset your password.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthPage>
      {step !== 4 && <BackButton onPress={goBack} />}
      {step === 1 && (
        <AuthHeading
          title="Forgot password?"
          subtitle="Enter your registered email and we’ll send you a 6-digit verification code."
        />
      )}
      {step === 2 && (
        <AuthHeading
          title="Verify your email"
          subtitle={`Enter the 6-digit code sent to ${email}.`}
        />
      )}
      {step === 3 && (
        <AuthHeading title="Choose a new password" subtitle="Use at least 8 characters." />
      )}
      {step === 4 && (
        <AuthHeading
          title="Password updated"
          subtitle="You can now sign in with your new password."
        />
      )}
      <View style={authStyles.form}>
        {step === 1 && (
          <>
            <AuthField
              accessibilityLabel="Email address"
              placeholder="Email"
              keyboardType="email-address"
              autoCapitalize="none"
              autoComplete="email"
              textContentType="emailAddress"
              returnKeyType="send"
              value={email}
              onChangeText={setEmail}
              onSubmitEditing={requestReset}
              editable={!loading}
            />
            <FormMessage message={message} success={success} />
            <PrimaryButton title="Send Verification Code" loading={loading} onPress={requestReset} />
          </>
        )}
        {step === 2 && (
          <>
            <AuthField
              accessibilityLabel="6-digit reset code"
              placeholder="Enter 6-digit code"
              keyboardType="number-pad"
              autoCapitalize="none"
              autoComplete="one-time-code"
              textContentType="oneTimeCode"
              maxLength={6}
              returnKeyType="done"
              value={otp}
              onChangeText={(value) => setOtp(value.replace(/\D/g, '').slice(0, 6))}
              onSubmitEditing={verifyResetOtp}
              editable={!loading}
            />
            <FormMessage message={message} success={success} />
            <PrimaryButton title="Verify Code" loading={loading} onPress={verifyResetOtp} />
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ disabled: loading, busy: loading }}
              disabled={loading}
              onPress={requestReset}
              style={({ pressed }) => [styles.resendButton, pressed && !loading && styles.pressed]}>
              <Text style={styles.resendText}>Resend code</Text>
            </Pressable>
          </>
        )}
        {step === 3 && (
          <>
            <AuthField
              accessibilityLabel="New password"
              placeholder="Enter new password"
              secureTextEntry
              autoCapitalize="none"
              autoComplete="new-password"
              textContentType="newPassword"
              value={newPassword}
              onChangeText={setNewPassword}
              onSubmitEditing={resetPassword}
              editable={!loading}
            />
            <FormMessage message={message} success={success} />
            <PrimaryButton title="Reset Password" loading={loading} onPress={resetPassword} />
          </>
        )}
        {step === 4 && (
          <>
            <FormMessage message={message} success />
            <PrimaryButton
              title="Go to Log In"
              onPress={() => router.replace('/login')}
            />
          </>
        )}
      </View>
    </AuthPage>
  );
}

const styles = {
  resendButton: {
    alignSelf: 'center' as const,
    padding: 8,
  },
  resendText: {
    color: AuthColors.green,
    fontSize: 14,
    fontWeight: '600' as const,
  },
  pressed: {
    opacity: 0.7,
  },
};
