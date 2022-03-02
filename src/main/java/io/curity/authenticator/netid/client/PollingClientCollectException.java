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

package io.curity.authenticator.netid.client;

import com.google.common.base.Enums;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollingClientCollectException extends PollingClientException
{
    private CollectFaultStatus _status;
    private static final Logger _logger = LoggerFactory.getLogger(PollingClientCollectException.class);

    public PollingClientCollectException(String message, CollectFaultStatus status)
    {
        super(message);
        _status = status;
    }

    public PollingClientCollectException(String message, Throwable cause, String faultString)
    {
        super(message, cause);
        _status = Enums.getIfPresent(CollectFaultStatus.class, faultString).or(() -> {
            _logger.info("Unknown fault status from server: " + faultString);
            return CollectFaultStatus.INTERNAL_ERROR;
        });
    }

    public CollectFaultStatus getStatus()
    {
        return _status;
    }

    public void setStatus(CollectFaultStatus status)
    {
        _status = status;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
                .append("_status", _status)
                .toString();
    }
}
