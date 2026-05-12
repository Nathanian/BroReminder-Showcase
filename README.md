# BroReminder Showcase

A showcase version of my Android reminder and voice-assistant application developed in Java for Android 7 devices.

This project combines reminders, overlay-based interactions, voice commands, speech processing, and background services into a lightweight Android application designed for older hardware and kiosk-like systems.

## Features

* Voice-triggered reminder creation
* Overlay-based UI interactions
* Background reminder scheduling
* Daily report generation
* Notification and alarm system
* Runtime voice command handling
* Offline-oriented architecture
* Android 7 (API 24/25) compatibility
* Lightweight service-focused design for low-end hardware

## Technical Highlights

* Java-based Android application
* Fragment-based UI architecture
* Foreground/background Android services
* Broadcast receivers for scheduled events
* Runtime overlay management
* Modular voice-processing utilities
* Config-based setup for public showcase version
* Repository cleaned and sanitized for public demonstration

## Architecture

The application is structured into multiple modules:

* `activities` → Main Android activities
* `fragments` → UI logic and reminder screens
* `receiver` → Alarm and scheduled event receivers
* `reminder` → Reminder storage and scheduling logic
* `utils` → Overlay and utility services
* `voice` → Voice processing and command handling
* `viewmodels` → State and UI data handling

## Android Compatibility

This project was intentionally developed with compatibility for older Android systems in mind, especially Android 7 devices commonly found in embedded systems and robotics environments.

## Public Showcase Notes

This repository is a sanitized showcase/demo version of the original internal project.

The following were removed or replaced:

* Internal infrastructure references
* Sensitive configuration values
* Private endpoints and credentials
* Company-specific assets and setup details

Placeholder values are used where required.

## Tech Stack

* Java
* Android SDK
* Android Services
* Broadcast Receivers
* Gradle

## Screenshots

<img width="822" height="489" alt="Reminder-erstellen" src="https://github.com/user-attachments/assets/db79035a-9426-4fbb-941e-42a7d2235581" />
<img width="826" height="490" alt="Main-Screen" src="https://github.com/user-attachments/assets/b1c6af12-079a-4e42-ac2b-556981f57636" />
<img width="822" height="484" alt="Berichte" src="https://github.com/user-attachments/assets/5d9a0c78-124d-47a5-9948-d758e586f67b" />

## Author

Jan Herold
Application Developer / Android & Robotics Development


## Scope

This codebase is intended for:

- architecture review
- portfolio/showcase use
- feature demonstration

It is **not** intended to expose any production infrastructure, customer data, or private deployment details.
