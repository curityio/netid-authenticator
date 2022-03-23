package io.curity.authenticator.netid.client;

import se.curity.identityserver.sdk.http.HttpStatus;

/**
 * A more HTTP-friendly status code mapping for the poller.
 * <p>
 * May be injected into the {@link WebServicePoller} to change status codes it reports.
 */
public final class SemanticHttpPollerStatusCodeMapping implements WebServicePoller.StatusCodeMapping
{
    public static final SemanticHttpPollerStatusCodeMapping INSTANCE = new SemanticHttpPollerStatusCodeMapping();

    @Override
    public HttpStatus keepPolling()
    {
        return HttpStatus.OK;
    }

    @Override
    public HttpStatus pollingFailure(boolean isFatalError)
    {
        return isFatalError ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
    }

    @Override
    public HttpStatus pollingDone()
    {
        return HttpStatus.OK;
    }
}
