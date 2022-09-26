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

package io.curity.authenticator.netid;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

public class PollingAuthenticatorConstants
{

    public static class Endpoints
    {
        public static final String FAILED = "failed";
        public static final String LAUNCH = "launch";
        public static final String WAIT = "wait";
        public static final String CANCEL = "cancel";
        // this is used as an alias for WAIT
        public static final String POLLER = "poller";
    }

    public static class ResponseModelFields
    {
        public static final String AUTHN_COMPLETE = "authncomplete";
    }

    public static class FormValueNames
    {
        public static final String RESTART_URL = "_restartUrl";
        public static final String CANCEL_URL = "_cancelUrl";
        public static final String ERROR_MESSAGE = "_errorMessage";
        public static final String RETURN_TO_URL = "_returnToUrl";
        public static final String FAILURE_URL = "_failureUrl";
        public static final String SERVICE_MESSAGE = "_serviceMessage";
        public static final String POLL_URL = "_pollUrl";
        public static final String POLLING_DONE = "_pollingDone";
        public static final String AUTOSTART_TOKEN = "_autostartToken";
        public static final String FORM_LAUNCH_COUNT = "_launchCount";
    }

    public static class EndUserMessageKeys
    {
        /**
         * An error has occurred (1). Try again.
         */
        public static final String ACCESS_DENIED_RP = "access_denied_rp";
        /**
         * An error has occurred (2). Try again.
         */
        public static final String INVALID_PARAMETERS = "invalid_parameters";
        /**
         * Wrong personal number.
         */
        public static final String UNKNOWN_PERSONAL_NUMBER = "unknown_personal_number";

        /**
         * Blocks authentication requests during the specified request block time if a user has an active authentication request
         * and a new request is issued. Request block time is set in server's settings.xml.
         */
        public static final String USER_BLOCKED ="user_blocked";

        /**
         * Start your authenticator app
         */
        public static final String START_APP = "start_app";

        /**
         * Action cancelled. Please try again
         */
        public static final String CANCELLED = "cancelled";

        /**
         * Internal error. Please try again
         */
        public static final String INTERNAL_ERROR = "internal_error";

        /**
         * Action cancelled
         */
        public static final String USER_CANCELLED = "user_cancelled";

        /**
         * The authenticator app is not responding.
         * Please check that the program is started and that you have internet access, then try again
         */
        public static final String EXPIRED_TRANSACTION = "expired_transaction";

        /**
         * Enter your security code in the authenticator app and select Identify or Sign
         */
        public static final String USER_SIGN = "user_sign";

        /**
         * Internal error. Update your authenticator app and try again
         */
        public static final String CLIENT_ERROR = "client_error";

        /**
         * Trying to start your authenticator app
         */
        public static final String OUTSTANDING_TRANSACTION = "outstanding_transaction";

        /**
         * You do not have an ID which can be used for this login/signature on this computer.
         * If you have a smart card, please insert it into your card reader.
         * If you have an ID on another device you can start the authenticator app on that device
         */
        public static final String NO_APP = "no_app";

        /**
         * You do not have an ID which can be used for this login/signature on this computer.
         * If you have a smart card, please insert it into your card reader.
         */
        public static final String NO_APP_TRY_OTHER_DEVICE = "no_app_try_other_device";

        /**
         * The ID you are trying to use is revoked or too old.
         * Please use another id or order a new one
         */
        public static final String CERTIFICATE_ERROR = "certificate_error";

        /**
         * The authenticator app couldn't be found on your computer or mobile device.
         * Please install it and order a new Net iD
         */
        public static final String START_FAILED = "start_failed";

        /**
         * An error has occurred (3).
         */
        public static final String UNKNOWN_ERROR = "unknown_error";

        /**
         * General error
         */
        public static final String GENERAL_ERROR = "validation.error.general";

        /**
         * An order is already in progress.
         */
        public static final String IN_PROGRESS = "inprogress";
    }

    public static class SessionKeys
    {
        public static final String AUTHENTICATION_STATE = "POLLING_AUTHENTICATION_STATE";
        public static final String ERROR_MESSAGE = "POLLING_ERROR_MESSAGE";
        public static final String ORDER_REF = "POLLING_ORDER_REF";
        public static final String RESULT_ATTRIBUTES = "RESULT_ATTRIBUTES";
        public static final String RESULT_SUBJECT = "RESULT_SUBJECT";
        public static final String SESSION_LAUNCH_COUNT = "POLLING_LAUNCH_COUNT";
        public static final String AUTOSTART_TOKEN = "POLLING_AUTOSTART_TOKEN";
        public static final String USE_SAME_DEVICE = "POLLING_USE_SAME_DEVICE";
        public static final String INIT_TIME = "POLLING_INIT_TIME";

        public static final ImmutableCollection<String> all = ImmutableList.of(
                AUTHENTICATION_STATE,
                ERROR_MESSAGE,
                ORDER_REF,
                RESULT_ATTRIBUTES,
                RESULT_SUBJECT,
                SESSION_LAUNCH_COUNT,
                AUTOSTART_TOKEN,
                USE_SAME_DEVICE,
                INIT_TIME
        );
    }
}
