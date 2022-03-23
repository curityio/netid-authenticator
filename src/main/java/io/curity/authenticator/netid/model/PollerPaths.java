/*
 *  Copyright 2022 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.authenticator.netid.model;

import static io.curity.authenticator.netid.PollingAuthenticatorConstants.Endpoints.CANCEL;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.Endpoints.FAILED;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.Endpoints.LAUNCH;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.Endpoints.POLLER;
import static io.curity.authenticator.netid.PollingAuthenticatorConstants.Endpoints.WAIT;

public final class PollerPaths
{
    private final String _pollerPath;
    private final String _cancelPath;
    private final String _failedPath;
    private final String _launcherPath;
    private final FailureMode _failureMode;

    public PollerPaths(String pollerPath, String cancelPath,
                       String failedPath, String launcherPath,
                       FailureMode failureMode)
    {
        _pollerPath = pollerPath;
        _cancelPath = cancelPath;
        _failedPath = failedPath;
        _launcherPath = launcherPath;
        _failureMode = failureMode;
    }

    public static PollerPaths getDefault()
    {
        return new PollerPaths(WAIT, CANCEL, FAILED, LAUNCH, FailureMode.REDIRECT_CLIENT);
    }

    public static PollerPaths forHttpSemanticLogic()
    {
        return new PollerPaths(POLLER, CANCEL, FAILED, LAUNCH, FailureMode.PROBLEM_JSON);
    }

    public String getPollerPath()
    {
        return _pollerPath;
    }

    public String getCancelPath()
    {
        return _cancelPath;
    }

    public String getFailedPath()
    {
        return _failedPath;
    }

    public FailureMode getFailureMode()
    {
        return _failureMode;
    }

    public String getLauncherPath()
    {
        return _launcherPath;
    }

    public enum FailureMode
    {
        REDIRECT_CLIENT, PROBLEM_JSON
    }
}
