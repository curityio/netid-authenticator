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

import com.secmaker.netid.nias.DeviceInfoType;
import com.secmaker.netid.nias.ResultCollect;
import com.secmaker.netid.nias.UserInfoType;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.AttributeName;
import se.curity.identityserver.sdk.attribute.AuthenticationAttributes;
import se.curity.identityserver.sdk.attribute.ContextAttributes;
import se.curity.identityserver.sdk.attribute.ListAttributeValue;
import se.curity.identityserver.sdk.attribute.SubjectAttributes;
import se.curity.identityserver.sdk.attribute.scim.v2.ComplexAttribute;
import se.curity.identityserver.sdk.attribute.scim.v2.Name;
import se.curity.identityserver.sdk.attribute.scim.v2.multivalued.X509Certificates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.curity.authenticator.netid.utils.NullUtils.ifNotNull;
import static io.curity.authenticator.netid.utils.NullUtils.valueOrError;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static se.curity.identityserver.sdk.attribute.AccountAttributes.NAME;
import static se.curity.identityserver.sdk.attribute.AccountAttributes.X509_CERTIFICATES;
import static se.curity.identityserver.sdk.attribute.SubjectAttributes.SUBJECT;

public class NetIdAccessAuthenticationAttributes extends AuthenticationAttributes
{
    private NetIdAccessAuthenticationAttributes(SubjectAttributes subjectAttributes,
                                                ContextAttributes contextAttributes)
    {
        super(subjectAttributes, contextAttributes);
    }

    /**
     * Parse the result of the NetiD authentication into Authentication Attributes.
     * <p>
     * Will use either personalNumber or userId as the subject. personalNumber has precedence.
     *
     * @param result The NetiD authn result
     * @return an AuthenticationAttributes object
     * @throws RuntimeException if no UserInfo is returned
     * @throws RuntimeException when both personalNumber and userId is empty
     */
    public static NetIdAccessAuthenticationAttributes of(ResultCollect result)
    {
        if (!CollectStatus.COMPLETE.name().equals(result.getProgressStatus()))
        {
            throw new IllegalArgumentException("Authentication was not completed: " + result.getProgressStatus());
        }

        return new NetIdAccessAuthenticationAttributes(getSubjectAttributes(result), getContextAttributes(result));
    }

    private static SubjectAttributes getSubjectAttributes(ResultCollect result)
    {
        UserInfoType userInfo = valueOrError(result.getUserInfo(), "Did not get UserInfo in response");

        List<Attribute> subjectAttributes = new ArrayList<>(3);
        handleSubject(userInfo, subjectAttributes);

        Map<String, String> nameMap = new HashMap<>();
        ifNotNull(userInfo.getGivenName(), givenName -> nameMap.put(Name.GIVEN_NAME, givenName));
        ifNotNull(userInfo.getSurname(), name -> nameMap.put(Name.FAMILY_NAME, name));
        ifNotNull(userInfo.getName(), name -> nameMap.put(Name.FORMATTED, name));

        if (!nameMap.isEmpty())
        {
            subjectAttributes.add(Attribute.of(NAME, Name.of(nameMap)));
        }

        if (userInfo.getCertificate() != null)
        {
            X509Certificates certificate = X509Certificates.of(userInfo.getCertificate(), true);
            ListAttributeValue certificateList = ListAttributeValue.of(Collections.singleton(certificate));
            ComplexAttribute<X509Certificates> certificates = ComplexAttribute.of(AttributeName.of(X509_CERTIFICATES),
                    certificateList, Attribute.NO_AUTHORITY, X509Certificates::of);
            subjectAttributes.add(certificates);
        }

        return SubjectAttributes.of(subjectAttributes);
    }

    private static void handleSubject(UserInfoType userInfo, List<Attribute> subjectAttributes)
    {
        @Nullable String personalNumber = userInfo.getPersonalNumber();
        @Nullable String userId = userInfo.getUserUniqueName();
        if (personalNumber == null && userId == null)
        {
            throw new RuntimeException("Did not get a user identifier in NetiD response");
        }

        String subject = personalNumber != null ? personalNumber : userId;
        subjectAttributes.add(Attribute.of(SUBJECT, subject));
        ifNotNull(userId, id -> subjectAttributes.add(Attribute.of("userId", id)));
        ifNotNull(personalNumber, pn -> subjectAttributes.add(Attribute.of("personalNumber", pn)));
    }

    private static ContextAttributes getContextAttributes(ResultCollect result)
    {
        List<Attribute> contextAttributes = new ArrayList<>(4);

        DeviceInfoType deviceInfo = valueOrError(result.getDeviceInfo(), "Did not get DeviceInfo in response");

        contextAttributes.add(Attribute.of("deviceType", deviceInfo.getName()));
        contextAttributes.add(Attribute.of("ipAddress", deviceInfo.getAddress()));

        if (isNotEmpty(result.getOcspResponse()))
        {
            contextAttributes.add(Attribute.of("ocspResponse", result.getOcspResponse()));
        }
        return ContextAttributes.of(contextAttributes);
    }
}
