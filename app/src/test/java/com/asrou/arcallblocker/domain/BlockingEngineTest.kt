package com.asrou.arcallblocker.domain

import com.asrou.arcallblocker.core.model.BlockDecision
import com.asrou.arcallblocker.core.model.BlockReason
import com.asrou.arcallblocker.core.model.PrivateNumberPolicy
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BlockingEngineTest {

    private lateinit var engine: BlockingEngine

    @Before
    fun setUp() {
        engine = BlockingEngine()
    }

    @Test
    fun `when blocking is OFF, always allow calls`() {
        val decision = engine.evaluate(
            normalizedNumber = "+919876543210",
            isBlockingEnabled = false,
            isScheduleActive = true,
            isInBlacklist = true,
            isInWhitelist = false,
            isContact = false,
            privateNumberPolicy = PrivateNumberPolicy.BLOCK
        )

        assertEquals(BlockDecision.Action.ALLOW, decision.action)
        assertEquals(BlockReason.BLOCKING_DISABLED, decision.reason)
    }

    @Test
    fun `when schedule is inactive, always allow calls`() {
        val decision = engine.evaluate(
            normalizedNumber = "+919876543210",
            isBlockingEnabled = true,
            isScheduleActive = false,
            isInBlacklist = true,
            isInWhitelist = false,
            isContact = false,
            privateNumberPolicy = PrivateNumberPolicy.BLOCK
        )

        assertEquals(BlockDecision.Action.ALLOW, decision.action)
        assertEquals(BlockReason.SCHEDULE_INACTIVE, decision.reason)
    }

    @Test
    fun `when number is private and policy is BLOCK, block call`() {
        val decision = engine.evaluate(
            normalizedNumber = null,
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = false,
            isInWhitelist = false,
            isContact = false,
            privateNumberPolicy = PrivateNumberPolicy.BLOCK
        )

        assertEquals(BlockDecision.Action.BLOCK, decision.action)
        assertEquals(BlockReason.PRIVATE_NUMBER, decision.reason)
    }

    @Test
    fun `when number is private and policy is ALLOW, allow call`() {
        val decision = engine.evaluate(
            normalizedNumber = null,
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = false,
            isInWhitelist = false,
            isContact = false,
            privateNumberPolicy = PrivateNumberPolicy.ALLOW
        )

        assertEquals(BlockDecision.Action.ALLOW, decision.action)
        assertEquals(BlockReason.PRIVATE_NUMBER_ALLOWED, decision.reason)
    }

    @Test
    fun `when number is private and policy is FOLLOW_CONTACTS_ONLY, block call`() {
        val decision = engine.evaluate(
            normalizedNumber = "",
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = false,
            isInWhitelist = false,
            isContact = false,
            privateNumberPolicy = PrivateNumberPolicy.FOLLOW_CONTACTS_ONLY
        )

        assertEquals(BlockDecision.Action.BLOCK, decision.action)
        assertEquals(BlockReason.UNKNOWN_CALLER, decision.reason)
    }

    @Test
    fun `when number is in blacklist and action is BLOCK, block call even if in contacts`() {
        val decision = engine.evaluate(
            normalizedNumber = "+919876543210",
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = true,
            isInWhitelist = false,
            isContact = true,
            privateNumberPolicy = PrivateNumberPolicy.BLOCK,
            unknownCallAction = com.asrou.arcallblocker.core.model.UnknownCallAction.BLOCK
        )

        assertEquals(BlockDecision.Action.BLOCK, decision.action)
        assertEquals(BlockReason.BLACKLIST, decision.reason)
    }

    @Test
    fun `when number is in blacklist and action is SILENCE, silence call`() {
        val decision = engine.evaluate(
            normalizedNumber = "+919876543210",
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = true,
            isInWhitelist = false,
            isContact = true,
            privateNumberPolicy = PrivateNumberPolicy.BLOCK,
            unknownCallAction = com.asrou.arcallblocker.core.model.UnknownCallAction.SILENCE
        )

        assertEquals(BlockDecision.Action.SILENCE, decision.action)
        assertEquals(BlockReason.BLACKLIST, decision.reason)
    }

    @Test
    fun `when number is in blacklist and action is REJECT, reject call`() {
        val decision = engine.evaluate(
            normalizedNumber = "+919876543210",
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = true,
            isInWhitelist = false,
            isContact = true,
            privateNumberPolicy = PrivateNumberPolicy.BLOCK,
            unknownCallAction = com.asrou.arcallblocker.core.model.UnknownCallAction.REJECT
        )

        assertEquals(BlockDecision.Action.REJECT, decision.action)
        assertEquals(BlockReason.BLACKLIST, decision.reason)
    }

    @Test
    fun `when number is in whitelist, allow call even if not in contacts`() {
        val decision = engine.evaluate(
            normalizedNumber = "+919876543210",
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = false,
            isInWhitelist = true,
            isContact = false,
            privateNumberPolicy = PrivateNumberPolicy.BLOCK
        )

        assertEquals(BlockDecision.Action.ALLOW, decision.action)
        assertEquals(BlockReason.WHITELIST, decision.reason)
    }

    @Test
    fun `when number is in contacts, allow call`() {
        val decision = engine.evaluate(
            normalizedNumber = "+919876543210",
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = false,
            isInWhitelist = false,
            isContact = true,
            privateNumberPolicy = PrivateNumberPolicy.BLOCK
        )

        assertEquals(BlockDecision.Action.ALLOW, decision.action)
        assertEquals(BlockReason.CONTACT, decision.reason)
    }

    @Test
    fun `when number is not in contacts and no exceptions, block call`() {
        val decision = engine.evaluate(
            normalizedNumber = "+919876543210",
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = false,
            isInWhitelist = false,
            isContact = false,
            privateNumberPolicy = PrivateNumberPolicy.BLOCK,
            unknownCallAction = com.asrou.arcallblocker.core.model.UnknownCallAction.BLOCK
        )

        assertEquals(BlockDecision.Action.BLOCK, decision.action)
        assertEquals(BlockReason.NOT_IN_CONTACTS, decision.reason)
    }

    @Test
    fun `when number is not in contacts and action is SILENCE, silence call`() {
        val decision = engine.evaluate(
            normalizedNumber = "+919876543210",
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = false,
            isInWhitelist = false,
            isContact = false,
            privateNumberPolicy = PrivateNumberPolicy.BLOCK,
            unknownCallAction = com.asrou.arcallblocker.core.model.UnknownCallAction.SILENCE
        )

        assertEquals(BlockDecision.Action.SILENCE, decision.action)
        assertEquals(BlockReason.NOT_IN_CONTACTS, decision.reason)
    }

    @Test
    fun `when number is not in contacts and action is REJECT, reject call`() {
        val decision = engine.evaluate(
            normalizedNumber = "+919876543210",
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = false,
            isInWhitelist = false,
            isContact = false,
            privateNumberPolicy = PrivateNumberPolicy.BLOCK,
            unknownCallAction = com.asrou.arcallblocker.core.model.UnknownCallAction.REJECT
        )

        assertEquals(BlockDecision.Action.REJECT, decision.action)
        assertEquals(BlockReason.NOT_IN_CONTACTS, decision.reason)
    }

    @Test
    fun `when private number follows contacts policy and action is SILENCE, silence call`() {
        val decision = engine.evaluate(
            normalizedNumber = null,
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = false,
            isInWhitelist = false,
            isContact = false,
            privateNumberPolicy = PrivateNumberPolicy.FOLLOW_CONTACTS_ONLY,
            unknownCallAction = com.asrou.arcallblocker.core.model.UnknownCallAction.SILENCE
        )

        assertEquals(BlockDecision.Action.SILENCE, decision.action)
        assertEquals(BlockReason.UNKNOWN_CALLER, decision.reason)
    }

    @Test
    fun `when private number follows contacts policy and action is REJECT, reject call`() {
        val decision = engine.evaluate(
            normalizedNumber = null,
            isBlockingEnabled = true,
            isScheduleActive = true,
            isInBlacklist = false,
            isInWhitelist = false,
            isContact = false,
            privateNumberPolicy = PrivateNumberPolicy.FOLLOW_CONTACTS_ONLY,
            unknownCallAction = com.asrou.arcallblocker.core.model.UnknownCallAction.REJECT
        )

        assertEquals(BlockDecision.Action.REJECT, decision.action)
        assertEquals(BlockReason.UNKNOWN_CALLER, decision.reason)
    }
}
