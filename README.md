# BroReminder (Sanitized Showcase Edition)

This repository is a **public, sanitized demo/showcase version** of an Android reminder app.

It preserves the core architecture and feature flow (reminders, overlays, reports, voice-assisted input), while removing or replacing confidential and environment-specific values.

## Sanitization notes

The following categories were sanitized for public sharing:

- hardcoded SMTP/infra endpoints and credentials
- embedded secret-like constants
- verbose logs that exposed auth details
- private export artifacts and bundled internal snapshots

Placeholders are used where configuration is required:

- `YOUR_SERVER_IP`
- `YOUR_CLIENT_ID`
- `YOUR_API_KEY`
- `YOUR_MQTT_BROKER`
- `YOUR_TOPIC`
- `YOUR_CLIENT_ID`

> Note: This project currently uses SMTP for report email. MQTT placeholders are included for consistency if you extend the app with broker-based messaging.

## Required configuration

Copy the example file and replace placeholders with your own values:

- `app/config.example.properties`

Then wire values into your local build/runtime setup before testing email delivery.

## Build instructions

Use Android Studio (recommended) or Gradle wrapper:

```bash
./gradlew assembleDebug
```

## Scope

This codebase is intended for:

- architecture review
- portfolio/showcase use
- feature demonstration

It is **not** intended to expose any production infrastructure, customer data, or private deployment details.
