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

package io.curity.identityserver.plugin.authenticators.netidaccess.client

import com.secmaker.netid.nias.DeviceInfoType
import com.secmaker.netid.nias.ResultCollect
import com.secmaker.netid.nias.UserInfoType
import io.curity.authenticator.netid.client.NetIdAccessAuthenticationAttributes
import io.curity.authenticator.netid.client.CollectResponse
import io.curity.authenticator.netid.client.CollectStatus
import se.curity.identityserver.sdk.attribute.AuthenticationAttributes
import se.curity.identityserver.sdk.attribute.ContextAttributes
import se.curity.identityserver.sdk.attribute.MapAttributeValue
import se.curity.identityserver.sdk.attribute.SubjectAttributes
import spock.lang.Specification

class NetIdAccessAuthenticationAttributesTest extends Specification {

    def "Authentication attributes can be created from netid response"() {
        given: "A result object from netid"
        ResultCollect collected = createResult()

        when: "Creating authentication attributes from the result"
        NetIdAccessAuthenticationAttributes attributes = NetIdAccessAuthenticationAttributes.of(collected)

        then: "The authentication attributes contains the expected data"
        attributes.subject == collected.userInfo.personalNumber
        SubjectAttributes subjectAttributes = attributes.subjectAttributes

        MapAttributeValue name = MapAttributeValue.of(subjectAttributes.attributesByName?.name?.value)
        name.attributesByName.familyName.value as String == collected.userInfo.surname
        name.attributesByName.givenName.value as String == collected.userInfo.givenName
        name.attributesByName.formatted.value as String == collected.userInfo.name

        subjectAttributes['x509Certificates']?.value instanceof List
        List certificates = subjectAttributes['x509Certificates']?.value
        certificates.size() == 1
        certificates.first().value as String == collected.userInfo.certificate

        ContextAttributes contextAttributes = attributes.contextAttributes
        contextAttributes?.ipAddress?.value == collected.deviceInfo.address
        contextAttributes?.deviceType?.value == collected.deviceInfo.name
    }

    def "A custom subject can be set"() {
        given: "A result object from netid"
        ResultCollect collected = createResult()

        when: "Creating authentication attributes from the result with a custom subject"
        CollectResponse collectResponse = new CollectResponse(CollectStatus.COMPLETE,
                NetIdAccessAuthenticationAttributes.of(collected))
        AuthenticationAttributes attributes = collectResponse.getAuthenticationAttributes('customSubject')

        then: "The authentication attributes contains the expected data"
        attributes.subject == 'customSubject'
    }

    def "Can parse a response with minimal userinfo"() {
        given: "A result object with only minmal amount of attributes"
        ResultCollect collected = minimalResult()

        when: "Creating authentication attributes from the result with a custom subject"
        def attributes = NetIdAccessAuthenticationAttributes.of(collected)

        then: "The authentication attributes contains the expected data"
        attributes.subject == "198212311234"
    }

    def "Choose proper subject when a combination of personalNumber, userId, userUniqueName, and requestedUserId is in the response"() {
        given: "A result object"
        ResultCollect collected = new ResultCollect()
        collected.progressStatus = CollectStatus.COMPLETE.name()
        UserInfoType userInfo = new UserInfoType()
        userInfo.userUniqueName = userUniqueName
        userInfo.personalNumber = personalNumber
        userInfo.userId = userId
        collected.requestedUserId = requestedUserId
        collected.userInfo = userInfo
        collected.deviceInfo = getDefaultDeviceInfo()

        when: "Creating authentication attributes from the result with a custom subject"
        def attributes = NetIdAccessAuthenticationAttributes.of(collected)

        then: "The authentication attributes contains the expected data"
        attributes?.subjectAttributes?.userId?.value == expectedUserId
        attributes.subject == expectedSubject
        attributes.subjectAttributes.personalNumber?.value == expectedPersonalNumber
        attributes.subjectAttributes.userUniqueName?.value == expectedUniqueName
        attributes.subjectAttributes.requestedUserId?.value == expectedRequestedUserId

        // The subject is chosen in the following order: personalNumber -> userId -> uniqueUserName -> requestedUserId
        where:
        userId | personalNumber | userUniqueName | requestedUserId || expectedSubject | expectedUserId | expectedPersonalNumber | expectedUniqueName | expectedRequestedUserId
        'ted'  | '007'          |  'teddy'       | 'TED'           || '007'           | 'ted'          | '007'                  | 'teddy'            | 'TED'
        'ted'  | '007'          |  'teddy'       | null            || '007'           | 'ted'          | '007'                  | 'teddy'            | null
        null   | '007'          |  'teddy'       | 'TED'           || '007'           | null           | '007'                  | 'teddy'            | 'TED'
        null   | '007'          |  'teddy'       | null            || '007'           | null           | '007'                  | 'teddy'            | null
        null   | null           |  'teddy'       | 'TED'           || 'teddy'         | null           | null                   | 'teddy'            | 'TED'
        null   | null           |  'teddy'       | null            || 'teddy'         | null           | null                   | 'teddy'            | null
        null   | '007'          |  null          | 'TED'           || '007'           | null           | '007'                  | null               | 'TED'
        null   | '007'          |  null          | null            || '007'           | null           | '007'                  | null               | null
        'ted'  | null           |  null          | 'TED'           || 'ted'           | 'ted'          | null                   | null               | 'TED'
        'ted'  | null           |  null          | null            || 'ted'           | 'ted'          | null                   | null               | null
        'ted'  | null           |  'teddy'       | 'TED'           || 'ted'           | 'ted'          | null                   | 'teddy'            | 'TED'
        'ted'  | null           |  'teddy'       | null            || 'ted'           | 'ted'          | null                   | 'teddy'            | null
        'ted'  | '007'          |  null          | 'TED'           || '007'           | 'ted'          | '007'                  | null               | 'TED'
        'ted'  | '007'          |  null          | null            || '007'           | 'ted'          | '007'                  | null               | null
        null   | null           |  null          | 'TED'           || 'TED'           | null           | null                   | null               | 'TED'
    }

    def "A response without userUniqueName, userId, requestedUserId, or personalNumber throws exception"() {
        given: "A result object missing userId and personalNumber"
        ResultCollect collected = new ResultCollect()
        UserInfoType userInfo = new UserInfoType()
        collected.userInfo = userInfo
        collected.deviceInfo = getDefaultDeviceInfo()
        collected.progressStatus = CollectStatus.COMPLETE.name()

        when: "Creating authentication attributes from the result with a custom subject"
        NetIdAccessAuthenticationAttributes.of(collected)

        then: "An exception is thrown"
        thrown(RuntimeException)

    }

    ResultCollect minimalResult() {
        ResultCollect collected = new ResultCollect()
        UserInfoType userInfo = new UserInfoType()
        userInfo.personalNumber = '198212311234'

        collected.userInfo = userInfo
        collected.deviceInfo = getDefaultDeviceInfo()
        collected.progressStatus = CollectStatus.COMPLETE.name()
        return collected
    }

    ResultCollect createResult() {
        ResultCollect collected = new ResultCollect()
        UserInfoType userInfo = new UserInfoType()
        userInfo.name = 'Teddie Bear'
        userInfo.givenName = 'Teddie'
        userInfo.surname = 'Bear'
        userInfo.personalNumber = '198212311234'
        userInfo.certificate = 'foobar-certificate'

        collected.userInfo = userInfo
        collected.deviceInfo = getDefaultDeviceInfo()
        collected.ocspResponse = 'foobar-ocspresponse'
        collected.progressStatus = CollectStatus.COMPLETE.name()
        return collected
    }

    private DeviceInfoType getDefaultDeviceInfo() {
        DeviceInfoType deviceInfo = new DeviceInfoType()
        deviceInfo.name = 'tPhone'
        deviceInfo.address = '1.2.3.4'
        deviceInfo.version = 'v1'
        deviceInfo
    }
}
