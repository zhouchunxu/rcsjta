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

package com.gsma.rcs.core.ims.service.im.filetransfer.msrp;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.ContributionIdGenerator;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSessionListener;
import com.gsma.rcs.core.ims.service.im.filetransfer.ImsFileSharingSession;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;
import android.text.TextUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import javax2.sip.InvalidArgumentException;

/**
 * Originating file transfer session
 * 
 * @author jexa7410
 */
public class OriginatingMsrpFileSharingSession extends ImsFileSharingSession implements
        MsrpEventListener {

    /*private*/ MsrpManager mMsrpMgr;

    /*private*/ final InstantMessagingService mImService;

    private static final Logger sLogger = Logger.getLogger(OriginatingMsrpFileSharingSession.class
            .getSimpleName());

    /**
     * Constructor
     *
     * @param imService InstantMessagingService
     * @param chatId Chat Id
     * @param fileTransferId File transfer Id
     * @param content Content to be shared
     * @param contact Remote contact identifier
     * @param remoteUri Remote id
     * @param fileIcon Content of file icon
     * @param rcsSettings The RCS settings accessor
     * @param timestamp Local timestamp for the session
     * @param contactManager The contact manager accessor
     */
    public OriginatingMsrpFileSharingSession(InstantMessagingService imService, String chatId,
            String fileTransferId, MmContent content, ContactId contact, Uri remoteUri,
            MmContent fileIcon, RcsSettings rcsSettings, long timestamp,
            ContactManager contactManager) {
        super(imService, content, contact, remoteUri, fileIcon, fileTransferId, rcsSettings,
                timestamp, contactManager);
        if (sLogger.isActivated()) {
            sLogger.debug("OriginatingFileSharingSession contact=" + contact + " filename="
                    + content.getName());
        }
        mImService = imService;
        // Create dialog path
        createOriginatingDialogPath();
        // Set conversation ID
        setConversationID(chatId);
        // Set contribution ID
        String id = ContributionIdGenerator.getContributionId(getDialogPath().getCallId());
        setContributionID(id);
    }

    /*private*/ byte[] getFileData(Uri file, int size) throws NetworkException {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = (FileInputStream) AndroidFactory.getApplicationContext()
                    .getContentResolver().openInputStream(file);
            byte[] data = new byte[size];
            if (size != fileInputStream.read(data, 0, size)) {
                throw new NetworkException("Unable to retrive data from " + file);
            }
            return data;

        } catch (IOException e) {
            throw new NetworkException("Failed to get file data for uri : " + file, e);

        } finally {
            CloseableUtils.tryToClose(fileInputStream);
        }
    }

    @Override
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a file transfer session as originating");
            }
            /* Set setup mode */
            String localSetup = createSetupOffer();
            if (sLogger.isActivated()) {
                sLogger.debug("Local setup attribute is ".concat(localSetup));
            }
            /* Set local port */
            int localMsrpPort;
            if ("active".equals(localSetup)) {
                localMsrpPort = 9; /* See RFC4145, Page 4 */
            } else {
                localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(mRcsSettings);
            }
            /* Create the MSRP manager */
            String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                    .getNetworkAccess().getIpAddress();
            mMsrpMgr = new MsrpManager(localIpAddress, localMsrpPort, getImsService(), mRcsSettings);
            /* Build SDP part */
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String encoding = getContent().getEncoding();
            long maxSize = mRcsSettings.getMaxFileTransferSize();
            /* Set File-selector attribute */
            String selector = getFileSelectorAttribute();
            StringBuilder sdp = new StringBuilder(SdpUtils.buildFileSDP(ipAddress, localMsrpPort,
                    mMsrpMgr.getLocalSocketProtocol(), encoding, getFileTransferIdAttribute(),
                    selector, getFileDisposition(), localSetup, mMsrpMgr.getLocalMsrpPath(),
                    SdpUtils.DIRECTION_SENDONLY, maxSize));
            /* Set File-location attribute */
            Uri location = getFileLocationAttribute();
            if (location != null) {
                sdp.append("a=file-location:").append(location.toString()).append(SipUtils.CRLF);
            }
            /* Set File-range attribute */
            String range = getFileRangeAttribute();
            if (range != null) {
                sdp.append("a=file-range:").append(range).append(SipUtils.CRLF);
            }
            /* Set Supportting-inactive information in cmnet */
            if (mRcsSettings.isCmccRelease()) {
                sdp.append("i=supportting inactive").append(SipUtils.CRLF);
            }
            /* Set File-icon attribute */
            MmContent fileIcon = getFileicon();
            boolean fileIconSupported = isFileIconSupported();
            if (fileIcon != null && fileIconSupported) {
                sdp.append("a=file-icon:cid:").append(getFileiconCid()).append(SipUtils.CRLF);
            }
            /* Build and set local content */
            String content = buildLocalContent(sdp.toString());
            getDialogPath().setLocalContent(content);

            /* Create an INVITE request */
            if (sLogger.isActivated()) {
                sLogger.info("Send INVITE");
            }
            SipRequest invite = createInvite();
            /* Set the Authorization header */
            getAuthenticationAgent().setAuthorizationHeader(invite);
            /* Set initial request in the dialog path */
            getDialogPath().setInvite(invite);
            /* Send INVITE request */
            sendInvite(invite);

        } catch (InvalidArgumentException | ParseException e) {
            sLogger.error("Unable to set authorization header!", e);
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (FileAccessException | PayloadException e) {
            sLogger.error("Unable to set and send initial invite!", e);
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (NetworkException e) {
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to initiate a file transfer session!", e);
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, e));
        }
    }

    @Override
    public void prepareMediaSession() {
        // Get the remote SDP part
        byte[] sdp = getDialogPath().getRemoteContent().getBytes(UTF8);
        // Create the MSRP session
        MsrpSession session = mMsrpMgr.createMsrpSession(sdp, this);
        session.setFailureReportOption(true);
        session.setSuccessReportOption(false);
        // Do not use right now the mapping to do not increase memory and cpu consumption
        session.setMapMsgIdFromTransationId(false);
    }

    @Override
    public void openMediaSession() throws PayloadException, NetworkException {
        mMsrpMgr.openMsrpSession();
        mMsrpMgr.sendEmptyChunk(); // Required in cmnet
    }

    @Override
    public void startMediaTransfer() throws NetworkException, FileAccessException {
        try {
            /* Start sending data chunks */
            InputStream stream = AndroidFactory.getApplicationContext().getContentResolver()
                    .openInputStream(getContent().getUri());
            mMsrpMgr.sendChunks(stream, IdGenerator.generateMessageID(),
                    getContent().getEncoding(), getContent().getSize(), TypeMsrpChunk.FileSharing);

        } catch (FileNotFoundException e) {
            throw new FileAccessException("Failed to initiate media transfer!", e);

        } catch (SecurityException e) {
            sLogger.error("Session initiation has failed due to that the file is not accessible!",
                    e);
            ContactId contact = getRemoteContact();
            for (ImsSessionListener listener : getListeners()) {
                ((FileSharingSessionListener) listener).onTransferNotAllowedToSend(contact);
            }
        }
    }

    @Override
    public void msrpDataTransferred(String msgId) {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Data transferred");
            }
            long timestamp = System.currentTimeMillis();
            setFileTransferred();
            closeMediaSession();
            closeSession(TerminationReason.TERMINATION_BY_USER);
            removeSession();
            ContactId contact = getRemoteContact();
            MmContent content = getContent();
            for (ImsSessionListener listener : getListeners()) {
                ((FileSharingSessionListener) listener).onFileTransferred(content, contact,
                        FileTransferData.UNKNOWN_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION,
                        FileTransferProtocol.MSRP);
            }
            // mImService.receiveOneToOneFileDeliveryStatus(contact, new ImdnDocument(
            // getFileTransferId(), ImdnDocument.DISPLAY,
            // ImdnDocument.DeliveryStatus.DISPLAYED, timestamp));

        } catch (PayloadException e) {
            sLogger.error("Failed to notify MSRP data transferred for msgId : " + msgId, e);

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
            sLogger.error("Failed to notify msrp data transfered for msgId : " + msgId, e);
        }
    }

    @Override
    public void receiveMsrpData(String msgId, byte[] data, String mimeType) {
        // Not used in originating side
    }

    @Override
    public void msrpTransferProgress(long currentSize, long totalSize) {
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((FileSharingSessionListener) listener).onTransferProgress(contact, currentSize,
                    totalSize);
        }
    }

    @Override
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        // Not used in originating side
        return false;
    }

    @Override
    public void msrpTransferAborted() {
        if (sLogger.isActivated()) {
            sLogger.info("Data transfer aborted");
        }
    }

    @Override
    public void closeMediaSession() {
        // Close MSRP session
        if (mMsrpMgr != null) {
            mMsrpMgr.closeSession();
        }
        if (sLogger.isActivated()) {
            sLogger.debug("MSRP session has been closed");
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }

    @Override
    public void handle200OK(SipResponse resp) throws PayloadException, NetworkException,
            FileAccessException {
        // long timestamp = System.currentTimeMillis();
        // mImService.receiveOneToOneFileDeliveryStatus(getRemoteContact(), new ImdnDocument(
        // getFileTransferId(), ImdnDocument.POSITIVE_DELIVERY,
        // ImdnDocument.DeliveryStatus.DELIVERED, timestamp));
        super.handle200OK(resp);
    }

    public String buildCpimMessageWithImdn(String msgId, boolean display, boolean delivery) {
        if (mRcsSettings.isCpmMsgTech()) {
            String from = ChatUtils.ANONYMOUS_URI;
            String to = ChatUtils.ANONYMOUS_URI;
            String networkContent = "";
            String networkMimeType = ChatUtils
                    .apiMimeTypeToNetworkMimeType(ChatLog.Message.MimeType.TEXT_MESSAGE);
            if (display) {
                return ChatUtils.buildCpimMessageWithImdn(from, to, msgId, networkContent,
                        networkMimeType, getTimestamp());
            } else if (delivery) {
                return ChatUtils.buildCpimMessageWithoutDisplayedImdn(from, to, msgId,
                        networkContent, networkMimeType, getTimestamp());
            }
        }
        return null;
    }

    /**
     * Is fileIcon supported
     *
     * @return supported
     */
    // TODO to avoid different call caused double logic
    public boolean isFileIconSupported() {
        ContactId remote = getRemoteContact();
        if (remote == null) {
            return false;
        }
        Capabilities remoteCapabilities = mContactManager.getContactCapabilities(remote);
        return remoteCapabilities != null && remoteCapabilities.isFileTransferThumbnailSupported();
    }

    /**
     * Build local content
     *
     * @param sdpContent
     * @return content string
     * @throws NetworkException
     */
    public String buildLocalContent(String sdpContent) throws NetworkException {
        /* Build CPIM part */
        String cpim = buildCpimMessageWithImdn(getContent().getDeliveryMsgId(),
                mImdnManager.isRequestOneToOneDeliveryDisplayedReportsEnabled(),
                mImdnManager.isDeliveryDeliveredReportsEnabled());
        MmContent fileIcon = getFileicon();
        boolean fileIconSupported = isFileIconSupported();
        /* Build and set local content */
        if (!TextUtils.isEmpty(cpim) || (fileIcon != null && fileIconSupported)) {
            StringBuilder multipart = new StringBuilder(Multipart.BOUNDARY_DELIMITER)
                    .append(BOUNDARY_TAG).append(SipUtils.CRLF)
                    .append("Content-Type: application/sdp").append(SipUtils.CRLF)
                    .append("Content-Length: ").append(sdpContent.getBytes(UTF8).length)
                    .append(SipUtils.CRLF).append(SipUtils.CRLF).append(sdpContent)
                    .append(SipUtils.CRLF);
            if (!TextUtils.isEmpty(cpim)) {
                multipart.append(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG)
                        .append(SipUtils.CRLF).append("Content-Type: ")
                        .append(CpimMessage.MIME_TYPE).append(SipUtils.CRLF)
                        .append("Content-Length: ").append(cpim.getBytes(UTF8).length)
                        .append(SipUtils.CRLF).append(SipUtils.CRLF).append(cpim);
            }
            if (fileIcon != null && fileIconSupported) {
                /* Encode the file icon file */
                String imageEncoded = Base64.encodeBase64ToString(getFileData(fileIcon.getUri(),
                        (int) fileIcon.getSize()));
                multipart.append(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG)
                        .append(SipUtils.CRLF).append("Content-Type: " + fileIcon.getEncoding())
                        .append(SipUtils.CRLF).append(SipUtils.HEADER_CONTENT_TRANSFER_ENCODING)
                        .append(": base64").append(SipUtils.CRLF)
                        .append(SipUtils.HEADER_CONTENT_ID).append(": ").append(getFileiconCid())
                        .append(SipUtils.CRLF).append("Content-Length: ")
                        .append(imageEncoded.length()).append(SipUtils.CRLF)
                        .append("Content-Disposition: icon").append(SipUtils.CRLF)
                        .append(SipUtils.CRLF).append(imageEncoded).append(SipUtils.CRLF);
            }
            multipart.append(Multipart.BOUNDARY_DELIMITER).append(BOUNDARY_TAG)
                    .append(Multipart.BOUNDARY_DELIMITER);
            return multipart.toString();
        } else {
            return sdpContent;
        }
    }
}
