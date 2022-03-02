package io.curity.authenticator.netid.common.client;

import se.curity.identityserver.sdk.http.HttpStatus;

/**
 * Custom HTTP status codes that are understood by the Curity JavaScript Poller at
 * {@code se.curity.utils.poller.startPolling}.
 * <p>
 * These status codes do not follow known HTTP semantics, but as they have been used for a long time as of writing,
 * we may not change them without causing likely breakages on customers, which would benefit no one. So here we
 * "formalize" the custom scheme and have to just live with it.
 */
public final class CustomPollerStatusCodes implements WebServicePoller.StatusCodeMapping
{
    public static final CustomPollerStatusCodes INSTANCE = new CustomPollerStatusCodes();

    @Override
    public HttpStatus keepPolling()
    {
        return HttpStatus.CREATED;
    }

    @Override
    public HttpStatus pollingFailure(boolean isFatalError)
    {
        // legacy logic - the JS poller will "see" the failure redirect without concern for the status code
        return HttpStatus.CREATED;
    }

    @Override
    public HttpStatus pollingDone()
    {
        return HttpStatus.ACCEPTED;
    }
}
