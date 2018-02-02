/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2017 China Mobile.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.chat;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Set;

import javax2.sip.header.RequireHeader;

/**
 * Originating large message mode standalone message session
 */
public class OriginatingOneToManyLargeMessageModeSession extends OriginatingLargeMessageModeSession {
    /**
     * List of recipients for session
     */
    private final Set<ContactId> mRecipients;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger
            .getLogger(OriginatingOneToManyLargeMessageModeSession.class.getName());

    /**
     * Constructor
     *
     * @param imService InstantMessagingService
     * @param remoteUri Remote id
     * @param chatId Chat id
     * @param recipients Remote recipients of the session
     * @param chatMsg Chat message of the session
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager The contact manager accessor
     */
    public OriginatingOneToManyLargeMessageModeSession(InstantMessagingService imService,
            String chatId, Uri remoteUri, Set<ContactId> recipients, ChatMessage chatMsg,
            RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp,
            ContactManager contactManager) {
        super(imService, chatId, remoteUri, chatMsg, rcsSettings, messagingLog, timestamp,
                contactManager);
        if (sLogger.isActivated()) {
            sLogger.info("OriginatingOneToManyLargeMessageModeSession chatId=" + chatId + " msgId="
                    + chatMsg.getMessageId());
        }
        mRecipients = recipients;
    }

    /**
     * Create INVITE request
     *
     * @return Request
     * @throws PayloadException
     */
    @Override
    public SipRequest createInvite() throws PayloadException {
        SipRequest invite = super.createInvite();
        try {
            invite.addHeader(RequireHeader.NAME, "recipient-list-invite");
            invite.getStackMessage().removeHeader(SipUtils.HEADER_P_PREFERRED_SERVICE);
            invite.addHeader(SipUtils.HEADER_P_PREFERRED_SERVICE, URLDecoder.decode(
                    FeatureTags.FEATURE_OMA_CPM_FILE_TRANSFER_GROUP, StringUtils.UTF8_STR));
            return invite;
        } catch (ParseException | UnsupportedEncodingException e) {
            throw new PayloadException("Failed to create invite request!", e);
        }
    }

    @Override
    public String getMultipartContent(String sdp) {
        // Generate the resource list for given remote contacts
        String resourceList = ChatUtils.generateChatResourceList(mRecipients);
        // @formatter:off
        String multipart = new StringBuilder(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG).append(SipUtils.CRLF)
                .append("Content-Type: application/sdp").append(SipUtils.CRLF)
                .append("Content-Length: ").append(sdp.getBytes(UTF8).length).append(SipUtils.CRLF)
                .append(SipUtils.CRLF)
                .append(sdp).append(SipUtils.CRLF)
                .append(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG).append(SipUtils.CRLF)
                .append("Content-Type: application/resource-lists+xml").append(SipUtils.CRLF)
                .append("Content-Length: ").append(resourceList.getBytes(UTF8).length).append(SipUtils.CRLF)
                .append("Content-Disposition: recipient-list").append(SipUtils.CRLF)
                .append(SipUtils.CRLF)
                .append(resourceList).append(SipUtils.CRLF)
                .append(SipUtils.CRLF)
                .append(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG).append(Multipart.BOUNDARY_DELIMITER)
                .toString();
        // @formatter:on
        return multipart;
    }
}
