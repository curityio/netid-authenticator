package io.curity.authenticator.netid.config;

import io.curity.authenticator.netid.common.client.CustomPollerStatusCodes;
import io.curity.authenticator.netid.common.client.SemanticHttpPollerStatusCodeMapping;
import io.curity.authenticator.netid.common.client.WebServicePoller;
import io.curity.authenticator.netid.common.model.PollerPaths;
import se.curity.identityserver.sdk.web.Request;

import static io.curity.authenticator.netid.common.utils.SdkConstants.AUTH_API_ACCEPT_TYPE;

public class PluginComposer
{
    public static PollerPaths getPollerPaths(Request request)
    {
        var acceptableMediaTypes = request.getAcceptableMediaTypes();
        if (acceptableMediaTypes.contains(AUTH_API_ACCEPT_TYPE))
        {
            return PollerPaths.forHttpSemanticLogic();
        }
        else
        {
            return PollerPaths.getDefault();
        }
    }

    public static WebServicePoller.StatusCodeMapping getStatusCodeMapping(Request request)
    {
        var acceptableMediaTypes = request.getAcceptableMediaTypes();
        if (acceptableMediaTypes.contains(AUTH_API_ACCEPT_TYPE))
        {
            return SemanticHttpPollerStatusCodeMapping.INSTANCE;
        }
        else
        {
            return CustomPollerStatusCodes.INSTANCE;
        }
    }
}
