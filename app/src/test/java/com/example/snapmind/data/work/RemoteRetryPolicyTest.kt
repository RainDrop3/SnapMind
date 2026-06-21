package com.example.snapmind.data.work

import com.example.snapmind.core.result.AppError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteRetryPolicyTest {
    @Test
    fun `network and timeout failures are retryable`() {
        assertTrue(AppError.NetworkUnavailable.isRetryableRemoteFailure())
        assertTrue(AppError.ApiTimeout.isRetryableRemoteFailure())
    }

    @Test
    fun `server errors are retryable but client errors are not`() {
        assertTrue(AppError.Http(503, null).isRetryableRemoteFailure())
        assertFalse(AppError.Http(400, null).isRetryableRemoteFailure())
    }

    @Test
    fun `authentication and quota errors are not retried automatically`() {
        assertFalse(AppError.ApiUnauthorized.isRetryableRemoteFailure())
        assertFalse(AppError.ApiQuotaExceeded.isRetryableRemoteFailure())
    }
}
