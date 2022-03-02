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

package io.curity.authenticator.netid.common.model;

import com.google.common.collect.ImmutableMap;
import io.curity.authenticator.netid.common.client.CollectFaultStatus;
import io.curity.authenticator.netid.common.utils.NullUtils;
import org.apache.commons.lang3.StringUtils;
import se.curity.identityserver.sdk.web.ResponseModel;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class PollingResult implements ResponseModel
{
    private static final String POLLER_TYPE = "_pollingResult:type";

    private enum ResultType
    {
        SUCCESS, FAILED, PENDING
    }

    private static final String POLLER_STOP_POLLING_KEY = "stopPolling";
    private static final String POLLER_MESSAGE_KEY = "message";
    private static final String POLLER_URL = "pollingUrl";
    private static final String FINISH_OFF_URL = "finishOffUrl";
    private static final String CANCEL_URL = "cancelUrl";
    private static final String REDIRECT_URL = "redirectUrl";
    private static final String USER_MESSAGE = "userMessage";

    private final boolean _stopPolling;

    private PollingResult(boolean stopPolling)
    {
        _stopPolling = stopPolling;
    }

    public static void removePollerTypeResponseModelKey(ImmutableMap.Builder<String, Object> map)
    {
        // tryConvert will ignore a poller type that's not of the right type (ResultType)
        map.put(POLLER_TYPE, "");
    }

    public static Optional<PollingResult> tryConvert(Map<String, Object> map)
    {
        @Nullable Object pollerType = map.get(POLLER_TYPE);
        if (pollerType instanceof ResultType)
        {
            return Optional.of(convert(map, (ResultType) pollerType));
        }
        else
        {
            return Optional.empty();
        }
    }

    private static PollingResult convert(Map<String, Object> map,
                                         ResultType resultType)
    {
        switch (resultType)
        {
            case SUCCESS:
                return new Success(map);
            case FAILED:
                return new Failed(map);
            case PENDING:
                return new Pending(map);
            default:
                throw new IllegalStateException("ResultType is unknown: " + resultType);
        }
    }

    public abstract <T> T match(Function<Pending, T> onPending,
                                Function<Failed, T> onFailed,
                                Function<Success, T> onSuccess);

    protected ImmutableMap.Builder<String, Object> baseModelBuilder()
    {
        return ImmutableMap.<String, Object>builder()
                .put(POLLER_TYPE, getType())
                .put(POLLER_STOP_POLLING_KEY, _stopPolling);
    }

    protected abstract ResultType getType();

    public interface NotDone
    {
        String getPollUrl();

        String getCancelUrl();
    }

    public static final class Pending extends PollingResult implements NotDone
    {
        private final String _messageId;
        private final String _pollUrl;
        private final String _cancelUrl;

        public Pending(String messageId, String pollUrl, String cancelUrl)
        {
            super(false);
            _messageId = StringUtils.isBlank(messageId) ? "" : messageId;
            _pollUrl = pollUrl;
            _cancelUrl = cancelUrl;
        }

        private Pending(Map<String, Object> map)
        {
            this(PollingResult.extractMessageEntry(map, USER_MESSAGE),
                    NullUtils.valueOrError(String.class, map.get(POLLER_URL), POLLER_URL + " is missing"),
                    NullUtils.valueOrError(String.class, map.get(CANCEL_URL), CANCEL_URL + " is missing"));
        }

        @Override
        protected ResultType getType()
        {
            return ResultType.PENDING;
        }

        public String getMessageId()
        {
            return _messageId;
        }

        @Override
        public String getPollUrl()
        {
            return _pollUrl;
        }

        @Override
        public String getCancelUrl()
        {
            return _cancelUrl;
        }

        @Override
        public <T> T match(Function<Pending, T> onPending,
                           Function<Failed, T> onFailed,
                           Function<Success, T> onSuccess)
        {
            return onPending.apply(this);
        }

        @Override
        public Map<String, Object> getViewData()
        {
            ImmutableMap.Builder<Object, Object> message = ImmutableMap.builder().put(USER_MESSAGE, _messageId);
            return baseModelBuilder()
                    .put(POLLER_MESSAGE_KEY, message.build())
                    .put(POLLER_URL, _pollUrl)
                    .put(CANCEL_URL, _cancelUrl)
                    .build();
        }
    }

    public static final class Failed extends PollingResult implements NotDone
    {
        private static final String COLLECT_FAULT_STATUS = "_collectFaultStatus";

        /**
         * This URL is used by the Velocity templates to redirect the client to the FAILED
         * template... it should not be used in the hypermedia API or any other view-renderers.
         */
        private final String _redirectUrl;

        private final String _pollUrl;
        private final String _cancelUrl;
        private final String _messageId;

        @Nullable
        private final CollectFaultStatus _collectFaultStatus;

        public Failed(String redirectUrl, String pollUrl, String cancelUrl,
                      String messageId,
                      @Nullable CollectFaultStatus collectFaultStatus)
        {
            // stopPolling is set to false here by default because this is in line with the original behaviour
            super(NullUtils.safeBoolean(NullUtils.map(collectFaultStatus, CollectFaultStatus::isFatal), false));
            _redirectUrl = redirectUrl;
            _pollUrl = pollUrl;
            _cancelUrl = cancelUrl;
            _messageId = messageId;
            _collectFaultStatus = collectFaultStatus;
        }

        private Failed(Map<String, Object> map)
        {
            this(PollingResult.extractMessageEntry(map, REDIRECT_URL),
                    NullUtils.valueOrError(String.class, map.get(POLLER_URL), POLLER_URL + " is missing"),
                    NullUtils.valueOrError(String.class, map.get(CANCEL_URL), CANCEL_URL + " is missing"),
                    PollingResult.extractMessageEntry(map, POLLER_MESSAGE_KEY),
                    NullUtils.valueOfType(CollectFaultStatus.class, map.get(COLLECT_FAULT_STATUS), null));
        }

        @Override
        public Map<String, Object> getViewData()
        {
            ImmutableMap.Builder<String, Object> builder = baseModelBuilder()
                    .put(POLLER_MESSAGE_KEY, ImmutableMap.of(
                            REDIRECT_URL, _redirectUrl,
                            POLLER_MESSAGE_KEY, _messageId))
                    .put(POLLER_URL, _pollUrl)
                    .put(CANCEL_URL, _cancelUrl);

            if (_collectFaultStatus != null)
            {
                builder.put(COLLECT_FAULT_STATUS, _collectFaultStatus);
            }

            return builder.build();
        }

        @Override
        protected ResultType getType()
        {
            return ResultType.FAILED;
        }

        public String getRedirectUrl()
        {
            return _redirectUrl;
        }

        @Override
        public String getPollUrl()
        {
            return _pollUrl;
        }

        @Override
        public String getCancelUrl()
        {
            return _cancelUrl;
        }

        public String getMessageId()
        {
            return _messageId;
        }

        public Optional<CollectFaultStatus> getCollectFaultStatus()
        {
            return Optional.ofNullable(_collectFaultStatus);
        }

        @Override
        public <T> T match(Function<Pending, T> onPending,
                           Function<Failed, T> onFailed,
                           Function<Success, T> onSuccess)
        {
            return onFailed.apply(this);
        }
    }

    public static final class Success extends PollingResult
    {
        private final String _finishOffUrl;

        public Success(Map<String, Object> map)
        {
            this(NullUtils.valueOrError(String.class, map.get(FINISH_OFF_URL), FINISH_OFF_URL + " is missing"));
        }

        public Success(String finishOffUrl)
        {
            super(true);
            _finishOffUrl = finishOffUrl;
        }

        public String getFinishOffUrl()
        {
            return _finishOffUrl;
        }

        @Override
        protected ResultType getType()
        {
            return ResultType.SUCCESS;
        }

        @Override
        public <T> T match(Function<Pending, T> onPending,
                           Function<Failed, T> onFailed,
                           Function<Success, T> onSuccess)
        {
            return onSuccess.apply(this);
        }

        @Override
        public Map<String, Object> getViewData()
        {
            return baseModelBuilder()
                    .put(FINISH_OFF_URL, _finishOffUrl)
                    .build();
        }
    }

    private static String extractMessageEntry(Map<String, Object> map,
                                              String messageKey)
    {
        Object messageMap = map.get(POLLER_MESSAGE_KEY);
        if (messageMap instanceof Map)
        {
            return NullUtils.valueOrError(String.class, ((Map<?, ?>) messageMap).get(messageKey),
                    messageKey + " is missing");
        }

        throw new IllegalArgumentException("Map does not contain key or has wrong type: " + POLLER_MESSAGE_KEY);
    }

    @Nullable
    private static String extractMessageEntryOrNull(Map<String, Object> map, String messageKey)
    {
        Object messageMap = map.get(POLLER_MESSAGE_KEY);
        if (messageMap instanceof Map)
        {
            return NullUtils.optionalValueOfType(String.class, ((Map<?, ?>) messageMap).get(messageKey));
        }
        return null;
    }
}
