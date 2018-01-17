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

package com.gsma.rcs.core.ims.service.im.filetransfer.msrp;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Set;

import javax2.sip.header.RequireHeader;

/**
 * Originating one-to-many file transfer session
 */
public class OriginatingMsrpOneToManyFileSharingSession extends OriginatingMsrpFileSharingSession {
    /**
     * Recipients set
     */
    private final Set<ContactId> mRecipients;

    private static final Logger sLogger = Logger
            .getLogger(OriginatingMsrpOneToManyFileSharingSession.class.getSimpleName());

    /**
     * Constructor
     *
     * @param imService InstantMessagingService
     * @param chatId Chat Id
     * @param fileTransferId File transfer Id
     * @param content Content to be shared
     * @param recipients Remote contact identifiers list
     * @param remoteUri Remote id
     * @param fileIcon Content of file icon
     * @param rcsSettings The RCS settings accessor
     * @param timestamp Local timestamp for the session
     * @param contactManager The contact manager accessor
     */
    public OriginatingMsrpOneToManyFileSharingSession(InstantMessagingService imService,
            String chatId, String fileTransferId, MmContent content, Set<ContactId> recipients,
            Uri remoteUri, MmContent fileIcon, RcsSettings rcsSettings, long timestamp,
            ContactManager contactManager) {
        super(imService, chatId, fileTransferId, content, null, remoteUri, fileIcon, rcsSettings,
                timestamp, contactManager);
        if (sLogger.isActivated()) {
            sLogger.debug("OriginatingMsrpOneToManyFileSharingSession chatId=" + chatId
                    + " filename=" + content.getName());
        }
        mRecipients = recipients;
    }

    @Override
    public SipRequest createInvite() throws PayloadException {
        SipRequest invite = super.createInvite();
        try {
            invite.addHeader(RequireHeader.NAME, "recipient-list-message");
            invite.getStackMessage().removeHeader(SipUtils.HEADER_P_PREFERRED_SERVICE);
            invite.addHeader(SipUtils.HEADER_P_PREFERRED_SERVICE, URLDecoder.decode(
                    FeatureTags.FEATURE_OMA_CPM_FILE_TRANSFER_GROUP, StringUtils.UTF8_STR));
            return invite;
        } catch (ParseException | UnsupportedEncodingException e) {
            throw new PayloadException("Failed to create invite request!", e);
        }
    }

    /**
     * Is fileIcon supported
     *
     * @return supported
     */
    @Override
    public boolean isFileIconSupported() {
        for (ContactId remote : mRecipients) {
            Capabilities remoteCapabilities = mContactManager.getContactCapabilities(remote);
            boolean supported = remoteCapabilities != null
                    && remoteCapabilities.isFileTransferThumbnailSupported();
            if (!supported) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String buildLocalContent(String sdpContent) throws NetworkException {
        /* Build CPIM part */
        String cpim = buildCpimMessageWithImdn(getContent().getDeliveryMsgId(),
                mImdnManager.isRequestOneToManyDeliveryDisplayedReportsEnabled(),
                mImdnManager.isDeliveryDeliveredReportsEnabled());
        /* Build and set local content */
        String resourceList = ChatUtils.generateChatResourceList(mRecipients);
        StringBuilder multipart = new StringBuilder(Multipart.BOUNDARY_DELIMITER)
                .append(BOUNDARY_TAG).append(SipUtils.CRLF).append("Content-Type: application/sdp")
                .append(SipUtils.CRLF).append("Content-Length: ")
                .append(sdpContent.getBytes(UTF8).length).append(SipUtils.CRLF)
                .append(SipUtils.CRLF).append(sdpContent).append(SipUtils.CRLF)
                .append(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG).append(SipUtils.CRLF)
                .append("Content-Type: application/resource-lists+xml").append(SipUtils.CRLF)
                .append("Content-Length: ").append(resourceList.getBytes(UTF8).length)
                .append(SipUtils.CRLF).append("Content-Disposition: recipient-list")
                .append(SipUtils.CRLF).append(SipUtils.CRLF).append(resourceList)
                .append(SipUtils.CRLF);
        if (!TextUtils.isEmpty(cpim)) {
            multipart.append(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG)
                    .append(SipUtils.CRLF).append("Content-Type: ").append(CpimMessage.MIME_TYPE)
                    .append(SipUtils.CRLF).append("Content-Length: ")
                    .append(cpim.getBytes(UTF8).length).append(SipUtils.CRLF).append(cpim)
                    .append(SipUtils.CRLF);
        }
        MmContent fileIcon = getFileicon();
        boolean fileIconSupported = isFileIconSupported();
        if (fileIcon != null && fileIconSupported) {
            /* Encode the file icon file */
            String imageEncoded = Base64.encodeBase64ToString(getFileData(fileIcon.getUri(),
                    (int) fileIcon.getSize()));
            multipart.append(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG)
                    .append(SipUtils.CRLF).append("Content-Type: " + fileIcon.getEncoding())
                    .append(SipUtils.CRLF).append(SipUtils.HEADER_CONTENT_TRANSFER_ENCODING)
                    .append(": base64").append(SipUtils.CRLF).append(SipUtils.HEADER_CONTENT_ID)
                    .append(": ").append(getFileiconCid()).append(SipUtils.CRLF)
                    .append("Content-Length: ").append(imageEncoded.length()).append(SipUtils.CRLF)
                    .append("Content-Disposition: icon").append(SipUtils.CRLF)
                    .append(SipUtils.CRLF).append(imageEncoded).append(SipUtils.CRLF);
        }
        multipart.append(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG)
                .append(Multipart.BOUNDARY_DELIMITER);
        return multipart.toString();
    }
}
