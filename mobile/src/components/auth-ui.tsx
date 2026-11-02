import type { PropsWithChildren } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  type TextInputProps,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

export const AuthColors = {
  cream: '#F7F2EC',
  paper: '#FCF9F5',
  green: '#294A3A',
  greenLight: '#597967',
  text: '#263D33',
  muted: '#747A75',
  line: '#DEDAD3',
  error: '#A44138',
  sky: '#DCE8E8',
};

export function AuthPage({
  children,
  welcome = false,
}: PropsWithChildren<{ welcome?: boolean }>) {
  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView
          contentContainerStyle={[styles.scrollContent, welcome && styles.welcomeScroll]}
          keyboardShouldPersistTaps="handled">
          <MountainScene welcome={welcome} />
          <View style={[styles.content, welcome && styles.welcomeContent]}>{children}</View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function MountainScene({ welcome }: { welcome: boolean }) {
  return (
    <View style={[styles.scene, welcome ? styles.heroScene : styles.compactScene]}>
      <View style={styles.sun} />
      <View style={[styles.mountain, styles.mountainFarLeft]} />
      <View style={[styles.mountain, styles.mountainFarCenter]} />
      <View style={[styles.mountain, styles.mountainFarRight]} />
      <View style={[styles.mountain, styles.mountainNearLeft]} />
      <View style={[styles.mountain, styles.mountainNearRight]} />
      <View style={styles.foreground} />
      {welcome && (
        <View style={styles.brand}>
          <Text style={styles.brandName}>WayMate</Text>
          <Text style={styles.brandTagline}>Find your way to the places that matter.</Text>
        </View>
      )}
    </View>
  );
}

export function AuthHeading({
  title,
  subtitle,
}: {
  title: string;
  subtitle?: string;
}) {
  return (
    <View style={styles.heading}>
      <Text style={styles.headingTitle}>{title}</Text>
      {subtitle ? <Text style={styles.headingSubtitle}>{subtitle}</Text> : null}
    </View>
  );
}

export function AuthField({ style, ...props }: TextInputProps) {
  return (
    <TextInput
      placeholderTextColor={AuthColors.muted}
      style={[styles.field, props.editable === false && styles.readOnlyField, style]}
      autoCorrect={false}
      {...props}
    />
  );
}

export function PrimaryButton({
  title,
  onPress,
  loading = false,
  disabled = false,
}: {
  title: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
}) {
  const isDisabled = loading || disabled;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: isDisabled, busy: loading }}
      disabled={isDisabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.primaryButton,
        isDisabled && styles.disabledButton,
        pressed && !isDisabled && styles.pressedButton,
      ]}>
      {loading ? (
        <ActivityIndicator color={AuthColors.paper} />
      ) : (
        <Text style={styles.primaryButtonText}>{title}</Text>
      )}
    </Pressable>
  );
}

export function SecondaryButton({
  title,
  onPress,
}: {
  title: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [styles.secondaryButton, pressed && styles.pressedButton]}>
      <Text style={styles.secondaryButtonText}>{title}</Text>
    </Pressable>
  );
}

export function BackButton({ onPress }: { onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={styles.backButton}>
      <Text style={styles.backButtonText}>‹</Text>
      <Text style={styles.backLabel}>Back</Text>
    </Pressable>
  );
}

export function FormMessage({
  message,
  success = false,
}: {
  message: string;
  success?: boolean;
}) {
  if (!message) {
    return null;
  }
  return (
    <Text
      accessibilityRole="alert"
      style={[styles.message, success ? styles.successMessage : styles.errorMessage]}>
      {message}
    </Text>
  );
}

export const authStyles = StyleSheet.create({
  form: {
    gap: 14,
    width: '100%',
  },
  center: {
    alignItems: 'center',
    textAlign: 'center',
  },
  linkText: {
    color: AuthColors.green,
    fontSize: 14,
    fontWeight: '600',
    textAlign: 'center',
    paddingVertical: 8,
  },
});

const styles = StyleSheet.create({
  flex: { flex: 1 },
  safeArea: {
    flex: 1,
    backgroundColor: AuthColors.cream,
  },
  scrollContent: {
    flexGrow: 1,
    alignItems: 'center',
  },
  welcomeScroll: {
    justifyContent: 'center',
  },
  scene: {
    width: '100%',
    backgroundColor: AuthColors.sky,
    overflow: 'hidden',
    position: 'relative',
  },
  heroScene: {
    height: 360,
  },
  compactScene: {
    height: 174,
  },
  sun: {
    width: 54,
    height: 54,
    borderRadius: 27,
    backgroundColor: '#F4D6A9',
    opacity: 0.9,
    position: 'absolute',
    top: '18%',
    right: '22%',
  },
  mountain: {
    position: 'absolute',
    width: 0,
    height: 0,
    borderLeftColor: 'transparent',
    borderRightColor: 'transparent',
    borderLeftWidth: 120,
    borderRightWidth: 120,
    borderBottomWidth: 100,
  },
  mountainFarLeft: {
    left: -88,
    bottom: 23,
    borderBottomColor: '#9BB8BB',
    borderLeftWidth: 130,
    borderRightWidth: 130,
    borderBottomWidth: 106,
  },
  mountainFarCenter: {
    left: '28%',
    bottom: 30,
    borderBottomColor: '#A8C0C1',
    borderLeftWidth: 150,
    borderRightWidth: 150,
    borderBottomWidth: 126,
  },
  mountainFarRight: {
    right: -110,
    bottom: 27,
    borderBottomColor: '#8EADB1',
    borderLeftWidth: 145,
    borderRightWidth: 145,
    borderBottomWidth: 116,
  },
  mountainNearLeft: {
    left: '1%',
    bottom: 1,
    borderBottomColor: '#78989A',
    borderLeftWidth: 160,
    borderRightWidth: 160,
    borderBottomWidth: 91,
  },
  mountainNearRight: {
    right: '-20%',
    bottom: 1,
    borderBottomColor: '#779397',
    borderLeftWidth: 180,
    borderRightWidth: 180,
    borderBottomWidth: 90,
  },
  foreground: {
    height: 24,
    backgroundColor: AuthColors.cream,
    position: 'absolute',
    left: -20,
    right: -20,
    bottom: -16,
    borderTopLeftRadius: 70,
    borderTopRightRadius: 100,
  },
  brand: {
    position: 'absolute',
    left: 24,
    right: 24,
    bottom: 40,
    alignItems: 'center',
  },
  brandName: {
    color: AuthColors.green,
    fontSize: 48,
    fontStyle: 'italic',
    fontWeight: '500',
    letterSpacing: -2,
  },
  brandTagline: {
    color: AuthColors.text,
    fontSize: 13,
    letterSpacing: 0.5,
    marginTop: 4,
    textAlign: 'center',
  },
  content: {
    gap: 22,
    paddingHorizontal: 24,
    paddingTop: 24,
    paddingBottom: 36,
    width: '100%',
    maxWidth: 520,
    alignSelf: 'center',
  },
  welcomeContent: {
    paddingTop: 28,
    gap: 14,
  },
  heading: {
    gap: 8,
    alignItems: 'center',
  },
  headingTitle: {
    color: AuthColors.text,
    fontSize: 25,
    lineHeight: 32,
    fontWeight: '700',
    textAlign: 'center',
  },
  headingSubtitle: {
    color: AuthColors.muted,
    fontSize: 15,
    lineHeight: 22,
    textAlign: 'center',
    maxWidth: 360,
  },
  field: {
    minHeight: 52,
    borderWidth: 1,
    borderColor: AuthColors.line,
    borderRadius: 17,
    paddingHorizontal: 17,
    paddingVertical: 12,
    color: AuthColors.text,
    backgroundColor: AuthColors.paper,
    fontSize: 16,
  },
  readOnlyField: {
    backgroundColor: '#F0ECE6',
    color: AuthColors.muted,
  },
  primaryButton: {
    minHeight: 54,
    borderRadius: 28,
    backgroundColor: AuthColors.green,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 22,
    marginTop: 4,
  },
  disabledButton: {
    opacity: 0.6,
  },
  pressedButton: {
    opacity: 0.82,
  },
  primaryButtonText: {
    color: AuthColors.paper,
    fontSize: 16,
    fontWeight: '600',
  },
  secondaryButton: {
    minHeight: 52,
    borderRadius: 28,
    borderWidth: 1,
    borderColor: AuthColors.green,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 22,
  },
  secondaryButtonText: {
    color: AuthColors.green,
    fontSize: 16,
    fontWeight: '600',
  },
  backButton: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    gap: 5,
    paddingVertical: 4,
  },
  backButtonText: {
    color: AuthColors.green,
    fontSize: 31,
    lineHeight: 32,
  },
  backLabel: {
    color: AuthColors.green,
    fontSize: 15,
    fontWeight: '600',
  },
  message: {
    fontSize: 14,
    lineHeight: 20,
    textAlign: 'center',
    paddingHorizontal: 4,
  },
  errorMessage: {
    color: AuthColors.error,
  },
  successMessage: {
    color: AuthColors.green,
  },
});
