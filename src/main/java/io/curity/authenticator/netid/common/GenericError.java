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

package io.curity.authenticator.netid.common;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

/**
 * A generic error representation that only takes a message.
 * Used for simple errors.
 */
public final class GenericError implements ErrorMessage
{
    private final String _message;

    /**
     * Create a new GenericError.
     * <p>
     * This is a factory method that can be called from Velocity templates using the following syntax:
     * <pre>
     * <code>$Error.create("some.error")</code>
     * </pre>
     *
     * @param message to display
     * @return a new generic error
     */
    public static GenericError create(String message)
    {
        return new GenericError(message);
    }

    public GenericError(final String message)
    {
        _message = message;
    }

    /**
     * The error message. Usually a key to a localized error message.
     *
     * @return The message or key.
     */
    @Override
    public String getMessage()
    {
        return _message;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other == null || getClass() != other.getClass())
        {
            return false;
        }

        GenericError that = (GenericError) other;

        return new EqualsBuilder()
                .append(_message, that._message)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                .append(_message)
                .toHashCode();
    }
}
