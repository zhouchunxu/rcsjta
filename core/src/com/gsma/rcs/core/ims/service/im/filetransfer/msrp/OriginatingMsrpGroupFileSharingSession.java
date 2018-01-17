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

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;

/**
 * Originating group file transfer session
 */
public class OriginatingMsrpGroupFileSharingSession extends OriginatingMsrpFileSharingSession {

    private static final Logger sLogger = Logger
            .getLogger(OriginatingMsrpGroupFileSharingSession.class.getSimpleName());

    /**
     * Constructor
     *
     * @param imService InstantMessagingService
     * @param chatId Chat Id
     * @param fileTransferId File transfer Id
     * @param content Content to be shared
     * @param fileIcon Content of file icon
     * @param remoteUri Remote id
     * @param rcsSettings The RCS settings accessor
     * @param timestamp Local timestamp for the session
     * @param contactManager The contact manager accessor
     */
    public OriginatingMsrpGroupFileSharingSession(InstantMessagingService imService, String chatId,
            String fileTransferId, MmContent content, MmContent fileIcon, Uri remoteUri,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager) {
        super(imService, chatId, fileTransferId, content, null, remoteUri, fileIcon, rcsSettings,
                timestamp, contactManager);
        if (sLogger.isActivated()) {
            sLogger.debug("OriginatingMsrpGroupFileSharingSession chatId=" + chatId + " filename="
                    + content.getName());
        }
    }

    /**
     * Is fileIcon supported
     *
     * @return supported
     */
    @Override
    public boolean isFileIconSupported() {
        return mRcsSettings.isFileTransferThumbnailSupported();
    }

    @Override
    public SipRequest createInvite() throws PayloadException {
        SipRequest invite = super.createInvite();
        try {
            invite.getStackMessage().removeHeader(SipUtils.HEADER_P_PREFERRED_SERVICE);
            invite.addHeader(SipUtils.HEADER_P_PREFERRED_SERVICE, URLDecoder.decode(
                    FeatureTags.FEATURE_OMA_CPM_FILE_TRANSFER_GROUP, StringUtils.UTF8_STR));
            return invite;
        } catch (ParseException | UnsupportedEncodingException e) {
            throw new PayloadException("Failed to create invite request!", e);
        }
    }
}
