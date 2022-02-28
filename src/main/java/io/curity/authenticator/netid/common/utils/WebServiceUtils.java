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

package io.curity.authenticator.netid.common.utils;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class WebServiceUtils
{
    private static final Logger _logger = LoggerFactory.getLogger(WebServiceUtils.class);
    private static final int MAX_RETRY_COUNT = 2;

    public static <U> CompletableFuture<U> callWebServiceWithRetry(Supplier<U> webServiceCall,
                                                                   Supplier<? extends RuntimeException> throwOnError)
    {
        return callWebServiceWithRetry(webServiceCall, 0, throwOnError);
    }

    private static <U> CompletableFuture<U> callWebServiceWithRetry(Supplier<U> webServiceCall,
                                                                    int retries,
                                                                    Supplier<? extends RuntimeException> throwOnError)
    {
        try
        {
            return CompletableFuture.supplyAsync(webServiceCall);
        }
        catch (RuntimeException ex)
        {
            Throwable cause = Throwables.getRootCause(ex);
            String errorMessage;
            if (cause.getMessage() != null)
            {
                errorMessage = cause.getMessage();
            }
            else
            {
                errorMessage = "No additional details";
            }

            if (cause instanceof SocketTimeoutException && retries < MAX_RETRY_COUNT)
            {
                _logger.info("Caught an exception from Encap. Error was {}. Retrying (attempts: {}, retries: {})",
                        errorMessage, retries + 1, retries);

                return callWebServiceWithRetry(webServiceCall, retries + 1, throwOnError);
            }

            _logger.warn("Web service call failed. Web service returned the following error: {}. retries: {}.",
                    errorMessage, retries);

            RuntimeException runtimeException = throwOnError.get();
            runtimeException.initCause(ex);
            throw runtimeException;
        }
    }
}
