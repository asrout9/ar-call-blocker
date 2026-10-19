package com.asrou.arcallblocker.core.util

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Normalizes phone numbers into a canonical representation for reliable comparison.
 *
 * The normalization strategy:
 * 1. Try to format to E.164 using Android's PhoneNumberUtils with the device's default country.
 * 2. If E.164 normalization succeeds, use it (e.g., "+919876543210").
 * 3. If it fails (e.g., short codes, malformed numbers), fall back to stripping
 *    non-digit characters (keeping the leading '+' if present).
 *
 * This handles the key Indian number formats:
 *   +91 98765 43210  →  +919876543210
 *   +919876543210    →  +919876543210
 *   919876543210     →  +919876543210
 *   09876543210      →  +919876543210
 *   9876543210       →  +919876543210
 *
 * The class is thread-safe and stateless after construction.
 */
class PhoneNumberNormalizer(context: Context) {

    /**
     * The ISO 3166-1 alpha-2 country code derived from the device's SIM or network.
     * Falls back to the device's locale country, then to "IN" (India) as a last resort.
     */
    val defaultCountryIso: String

    init {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val simCountry = telephonyManager?.simCountryIso?.uppercase(Locale.ROOT)
        val networkCountry = telephonyManager?.networkCountryIso?.uppercase(Locale.ROOT)
        val localeCountry = Locale.getDefault().country.uppercase(Locale.ROOT)

        defaultCountryIso = when {
            !simCountry.isNullOrBlank() -> simCountry
            !networkCountry.isNullOrBlank() -> networkCountry
            localeCountry.isNotBlank() -> localeCountry
            else -> DEFAULT_COUNTRY
        }
    }

    /**
     * Normalize a phone number to a canonical form for comparison.
     *
     * @param number The raw phone number string (may contain spaces, dashes, parentheses).
     * @return The normalized number, or null if the input is null/blank/unparseable.
     */
    fun normalize(number: String?): String? {
        if (number.isNullOrBlank()) return null

        val trimmed = number.trim()

        // Try E.164 normalization via Android's built-in utility
        val e164 = PhoneNumberUtils.formatNumberToE164(trimmed, defaultCountryIso)
        if (!e164.isNullOrBlank()) {
            return e164
        }

        // Fallback: strip non-digit characters but preserve leading '+'
        return stripToDigits(trimmed)
    }

    /**
     * Normalize a number and produce all reasonable canonical forms for cache lookup.
     * This ensures we can match regardless of how the number was stored in contacts
     * vs how it arrives from the carrier.
     *
     * @param number The raw phone number string.
     * @return A set of normalized forms (typically 1-2 entries), or empty if unparseable.
     */
    fun normalizeToSet(number: String?): Set<String> {
        if (number.isNullOrBlank()) return emptySet()

        val result = mutableSetOf<String>()
        val trimmed = number.trim()

        // Primary: E.164
        val e164 = PhoneNumberUtils.formatNumberToE164(trimmed, defaultCountryIso)
        if (!e164.isNullOrBlank()) {
            result.add(e164)
        }

        // Secondary: stripped digits (catches cases where E.164 normalization
        // produces a different form than what the carrier sends)
        val stripped = stripToDigits(trimmed)
        if (stripped != null) {
            result.add(stripped)

            // For Indian numbers: if we have a 10-digit number, also add +91 prefix
            if (defaultCountryIso == "IN" && stripped.length == 10 && stripped.all { it.isDigit() }) {
                result.add("+91$stripped")
            }
        }

        return result
    }

    /**
     * Check if two phone numbers likely represent the same destination.
     * Uses normalization to compare rather than raw string equality.
     */
    fun areEqual(number1: String?, number2: String?): Boolean {
        val n1 = normalize(number1) ?: return false
        val n2 = normalize(number2) ?: return false
        if (n1 == n2) return true

        // Additional check: compare last N significant digits
        // This handles edge cases where E.164 normalization differs
        val d1 = n1.filter { it.isDigit() }
        val d2 = n2.filter { it.isDigit() }
        if (d1.length >= MIN_SIGNIFICANT_DIGITS && d2.length >= MIN_SIGNIFICANT_DIGITS) {
            val significantLength = minOf(d1.length, d2.length, MAX_SIGNIFICANT_DIGITS)
            return d1.takeLast(significantLength) == d2.takeLast(significantLength)
        }

        return false
    }

    private fun stripToDigits(number: String): String? {
        val hasPlus = number.startsWith("+")
        val digits = number.filter { it.isDigit() }
        if (digits.isEmpty()) return null

        return if (hasPlus) "+$digits" else digits
    }

    companion object {
        private const val DEFAULT_COUNTRY = "IN"

        /**
         * Minimum number of trailing digits required for the significant-digit comparison.
         * Numbers shorter than this won't use the trailing-digit fallback.
         */
        private const val MIN_SIGNIFICANT_DIGITS = 7

        /**
         * Maximum trailing digits to compare. Using 10 covers full national numbers
         * for most countries including India (10 digits) and US (10 digits).
         */
        private const val MAX_SIGNIFICANT_DIGITS = 10
    }
}
