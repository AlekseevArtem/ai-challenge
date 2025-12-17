package ru.alekseev.myapplication.di

import org.koin.core.module.Module

/**
 * Platform-specific module that provides platform-dependent dependencies
 * like AlertDispatcher and other platform-specific services.
 *
 * Each platform must provide its own implementation.
 */
expect val platformModule: Module
