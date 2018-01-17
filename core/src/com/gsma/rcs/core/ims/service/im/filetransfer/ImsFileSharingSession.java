/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.filetransfer;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;

/**
 * Abstract IMS file transfer session
 * 
 * @author jexa7410
 */
public abstract class ImsFileSharingSession extends FileSharingSession {
    /**
     * Boundary tag
     */
    public final static String BOUNDARY_TAG = "boundary1";

    /**
     * Default SO_TIMEOUT value (in milliseconds)
     */
    public final static long DEFAULT_SO_TIMEOUT = 30000;

    private long mTransferredSize = 0;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(ImsFileSharingSession.class.getName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param content Content of file to be shared
     * @param contact Remote contact identifier
     * @param remoteUri the remote contact URI
     * @param fileIcon Content of file icon
     * @param filetransferId File transfer Id
     * @param rcsSettings RCS settings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public ImsFileSharingSession(InstantMessagingService imService, MmContent content,
            ContactId contact, Uri remoteUri, MmContent fileIcon, String filetransferId,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager) {
        super(imService, content, contact, remoteUri, fileIcon, filetransferId, rcsSettings,
                timestamp, contactManager);
    }

    @Override
    public boolean isHttpTransfer() {
        return false;
    }

    /**
     * Returns the "file-transfer-id" attribute
     * 
     * @return String
     */
    public String getFileTransferIdAttribute() {
        return getFileTransferId();
    }

    /**
     * Returns the "file-selector" attribute
     * 
     * @return String
     */
    public String getFileSelectorAttribute() {
        String selector = "name:\"" + getContent().getName() + "\"" + " type:"
                + getContent().getEncoding() + " size:" + getContent().getSize();
        if (mRcsSettings.isCmccRelease()) {
            String fingerprint = FileUtils.getFingerprintOfFile(getContent().getUri(), "SHA1");
            if (!TextUtils.isEmpty(fingerprint)) {
                selector += " hash:sha-1:" + fingerprint;
            }
        }
        return selector;
    }

    /**
     * Returns the "file-location" attribute
     * 
     * @return Uri
     */
    public Uri getFileLocationAttribute() {
        Uri file = getContent().getUri();
        if ((file != null) && file.getScheme().startsWith("http")) {
            return file;
        }
        return null;
    }

    /**
     * Returns the "file-range" attribute
     *
     * @return String
     */
    public String getFileRangeAttribute() {
        return (mTransferredSize + 1) + "-" + getContent().getSize();
    }

    /**
     * Get file transferred size
     *
     * @return transferred size
     */
    public long getFileTransferredSize() {
        return mTransferredSize;
    }

    /**
     * Set file transferred size
     *
     * @param size
     */
    public void setFileTransferredSize(long size) {
        mTransferredSize = size;
    }

    /**
     * Create an INVITE request
     * 
     * @return the INVITE request
     * @throws PayloadException
     */
    public SipRequest createInvite() throws PayloadException {
        try {
            String[] featureTags = {
                ChatUtils.getFeatureTagForFileTransfer(mRcsSettings)
            };
            SipRequest invite;
            SipDialogPath dialogPath = getDialogPath();
            String content = dialogPath.getLocalContent();
            Multipart multi = new Multipart(content, BOUNDARY_TAG);
            if (multi.isMultipart()) {
                invite = SipMessageFactory.createMultipartInvite(dialogPath, featureTags, content,
                        BOUNDARY_TAG);
            } else {
                invite = SipMessageFactory.createInvite(dialogPath, featureTags, content);
            }
            if (mRcsSettings.isCpmMsgTech()) {
                invite.addHeader(SipUtils.HEADER_P_PREFERRED_SERVICE, URLDecoder.decode(
                        FeatureTags.FEATURE_OMA_CPM_FILE_TRANSFER, StringUtils.UTF8_STR));
                invite.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
            }
            invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
            return invite;

        } catch (ParseException | UnsupportedEncodingException e) {
            throw new PayloadException("Failed to create invite request!", e);
        }
    }

    /**
     * Handle error
     * 
     * @param error Error
     */
    public void handleError(ImsServiceError error) {
        if (isSessionInterrupted()) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Session error: ").append(error.getErrorCode())
                    .append(", reason=").append(error.getMessage()).toString());
        }
        closeMediaSession();
        removeSession();

        ContactId contact = getRemoteContact();
        for (int j = 0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener) getListeners().get(j)).onTransferError(
                    new FileSharingError(error), contact);
        }
    }

    /**
     * Data transfer error
     * 
     * @param msgId Message ID
     * @param error Error code
     * @param typeMsrpChunk
     */
    public void msrpTransferError(String msgId, String error,
            MsrpSession.TypeMsrpChunk typeMsrpChunk) {
        try {
            if (isSessionInterrupted() || getDialogPath().isSessionTerminated()) {
                return;
            }
            if (sLogger.isActivated()) {
                sLogger.debug("Data transfer error: ".concat(error));
            }
            closeSession(ImsServiceSession.TerminationReason.TERMINATION_BY_SYSTEM);
            closeMediaSession();

            getImsService().getImsModule().getCapabilityService()
                    .requestContactCapabilities(getRemoteContact());

            removeSession();

            if (isFileTransferred()) {
                return;
            }
            ContactId contact = getRemoteContact();
            for (ImsSessionListener listener : getListeners()) {
                ((FileSharingSessionListener) listener).onTransferError(new FileSharingError(
                        FileSharingError.MEDIA_TRANSFER_FAILED, error), contact);
            }
        } catch (PayloadException e) {
            sLogger.error(
                    new StringBuilder("Failed to handle msrp error").append(error)
                            .append(" for message ").append(msgId).toString(), e);
        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error(
                    new StringBuilder("Failed to handle msrp error").append(error)
                            .append(" for message ").append(msgId).toString(), e);
        }
    }

    @Override
    public long getFileExpiration() {
        return FileTransferData.UNKNOWN_EXPIRATION;
    }

    @Override
    public long getIconExpiration() {
        return FileTransferData.UNKNOWN_EXPIRATION;
    }

    @Override
    public void receiveBye(SipRequest bye) throws PayloadException, NetworkException {
        super.receiveBye(bye);
        ContactId contact = getRemoteContact();
        /*
         * SIP BYE can be received from the sender if either the sender wishes to abort the session
         * or when the transfer of file is completed. In both cases, we need to close the session
         * and perform the clean up activities of the session. We will broadcast the state as
         * ABORTED and ABORTED_BY_REMOTE only if the file was not received successfully.
         */
        if (!isFileTransferred()) {
            for (ImsSessionListener listener : getListeners()) {
                listener.onSessionAborted(contact, TerminationReason.TERMINATION_BY_REMOTE);
            }
        }
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(contact);
    }
}
