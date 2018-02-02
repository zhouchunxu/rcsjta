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

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;

import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.text.ParseException;

import javax2.sip.InvalidArgumentException;

/**
 * Originating large message mode standalone message
 */
public class OriginatingLargeMessageModeSession extends LargeMessageModeSession implements
        MsrpEventListener {
    /**
     * MSRP manager
     */
    private MsrpManager mMsrpMgr;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(OriginatingLargeMessageModeSession.class
            .getName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param chatId Chat id
     * @param remoteUri Remote id
     * @param chatMsg Chat message of the session
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager The contact manager accessor
     */
    public OriginatingLargeMessageModeSession(InstantMessagingService imService, String chatId,
            Uri remoteUri, ChatMessage chatMsg, RcsSettings rcsSettings, MessagingLog messagingLog,
            long timestamp, ContactManager contactManager) {
        super(imService, remoteUri, null, chatMsg, rcsSettings, messagingLog, timestamp,
                contactManager);
        // Create dialog path
        createOriginatingDialogPath();
        // Set Contribution ID
        String id = ContributionIdGenerator.getContributionId(getDialogPath().getCallId());
        setContributionID(id);
        // Set Conversation ID
        setConversationID(chatId);
        // Set InReplyTo Contribution ID, etc.

    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a new large message mode session as originating.");
            }
            // Set setup mode
            String localSetup = createSetupOffer();
            if (sLogger.isActivated()) {
                sLogger.debug("Local setup attribute is " + localSetup);
            }
            // Set local port
            int localMsrpPort;
            if ("active".equals(localSetup)) {
                localMsrpPort = 9; // See RFC4145, Page 4
            } else {
                localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(mRcsSettings);
            }
            // Create the MSRP manager
            String localIpAddress = mImService.getImsModule().getCurrentNetworkInterface()
                    .getNetworkAccess().getIpAddress();
            mMsrpMgr = new MsrpManager(localIpAddress, localMsrpPort, getImsService(), mRcsSettings);
            // Build SDP part
            // String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String sdp = SdpUtils.buildChatSDP(ipAddress, localMsrpPort,
                    mMsrpMgr.getLocalSocketProtocol(), getAcceptTypes(), getWrappedTypes(),
                    localSetup, mMsrpMgr.getLocalMsrpPath(), SdpUtils.DIRECTION_SENDONLY);
            // Build and set the local content in the dialog path
            String multipart = getMultipartContent(sdp);
            if (multipart != null) {
                getDialogPath().setLocalContent(multipart);
            } else {
                getDialogPath().setLocalContent(sdp);
            }
            // Create SIP INVITE request
            SipRequest invite = createInvite();
            // Set the Authorization header
            getAuthenticationAgent().setAuthorizationHeader(invite);
            // Set initial request in the dialog path
            getDialogPath().setInvite(invite);
            // Send INVITE request
            sendInvite(invite);

        } catch (InvalidArgumentException | ParseException e) {
            sLogger.error("Unable to set authorization header for chat invite!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));

        } catch (PayloadException | NetworkException | FileAccessException e) {
            sLogger.error("Unable to send  initial invite!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed initiating large message mode session!", e);
            handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED, e));
        }
    }

    @Override
    public void prepareMediaSession() throws PayloadException, NetworkException {
        // Get the remote SDP part
        byte[] sdp = getDialogPath().getRemoteContent().getBytes(UTF8);
        // Create the MSRP session
        MsrpSession session = mMsrpMgr.createMsrpSession(sdp, this);
        session.setFailureReportOption(true);
        session.setSuccessReportOption(false);
    }

    @Override
    public void openMediaSession() throws PayloadException, NetworkException {
        mMsrpMgr.openMsrpSession(DEFAULT_SO_TIMEOUT);
        mMsrpMgr.sendEmptyChunk(); // Required in cmnet
    }

    @Override
    public void startMediaTransfer() throws PayloadException, NetworkException, FileAccessException {
        ChatMessage msg = getChatMessage();
        String msgId = msg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.info("Start transfer data (msgId " + msgId + ")");
        }
        String mimeType = msg.getMimeType();
        String networkMimeType = ChatUtils.apiMimeTypeToNetworkMimeType(mimeType);
        if (ChatUtils.isGeolocType(networkMimeType)) {

        }
        String data = buildCpimMessage(msg);
        byte[] bytes = data.getBytes(UTF8);
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        mMsrpMgr.sendChunks(stream, msgId, CpimMessage.MIME_TYPE, bytes.length,
                MsrpSession.TypeMsrpChunk.TextMessage);
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
    public void msrpDataTransferred(String msgId) {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Data transfered (msgId " + msgId + ")");
            }
            // Message has been transfered
            setMsgTransferred();
            closeMediaSession();
            closeSession(TerminationReason.TERMINATION_BY_USER);
            removeSession();
            String mimeType = mMessagingLog.getMessageMimeType(msgId);
            for (ImsSessionListener listener : getListeners()) {
                ((ChatSessionListener) listener).onMessageSent(msgId, mimeType);
            }
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
    public void receiveMsrpData(String msgId, byte[] data, String mimeType)
            throws PayloadException, NetworkException, ContactManagerException {
        /* Not used here */
    }

    @Override
    public void msrpTransferProgress(long currentSize, long totalSize) {
        /* Not used here */
    }

    @Override
    public boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data) {
        /* Not used here */
        return false;
    }

    @Override
    public void msrpTransferAborted() {
        /* Not used here */
    }

    @Override
    public void msrpTransferError(String msgId, String error,
            MsrpSession.TypeMsrpChunk typeMsrpChunk) {
        try {
            if (isSessionInterrupted() || getDialogPath().isSessionTerminated()) {
                return;
            }
            if (sLogger.isActivated()) {
                sLogger.info("Data transfer error " + error + " for message " + msgId
                        + " (MSRP chunk type: " + typeMsrpChunk + ")");
            }
            closeMediaSession();
            closeSession(ImsServiceSession.TerminationReason.TERMINATION_BY_SYSTEM);
            getImsService().getImsModule().getCapabilityService()
                    .requestContactCapabilities(getRemoteContact());
            removeSession();

            if (isMsgTransferred()) {
                return;
            }
            if ((msgId != null) && MsrpSession.TypeMsrpChunk.TextMessage.equals(typeMsrpChunk)) {
                String mimeType = mMessagingLog.getMessageMimeType(msgId);
                for (ImsSessionListener listener : getListeners()) {
                    ((ChatSessionListener) listener).onMessageFailedSend(msgId, mimeType);
                }
            } else {
                // do nothing
                sLogger.error("MSRP transfer error not handled for message '" + msgId
                        + "' and chunk type : '" + typeMsrpChunk + "'!");
            }
        } catch (PayloadException e) {
            sLogger.error("Failed to handle msrp error" + error + " for message " + msgId, e);
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
            sLogger.error("Failed to handle msrp error" + error + " for message " + msgId, e);
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }

    /**
     * Get multipart content of the session invite
     *
     * @param sdp SDP string
     * @return content string
     */
    public String getMultipartContent(String sdp) {
        return null;
    }

    /**
     * Build a CPIM message
     *
     * @param chatMessage ChatMessage
     */
    protected String buildCpimMessage(ChatMessage chatMessage) {
        String cpim;
        String from = getDialogPath().getLocalParty();
        String to = getDialogPath().getRemoteParty();
        String mimeType = chatMessage.getMimeType();
        String networkMimeType = ChatUtils.apiMimeTypeToNetworkMimeType(mimeType);
        String msgId = chatMessage.getMessageId();
        long timestampSent = chatMessage.getTimestampSent();
        String networkContent = chatMessage.getContent();
        // String networkContent = ChatUtils.persistedContentToNetworkContent(mimeType,
        // chatMessage.getContent(), msgId, timestampSent);
        if (mImdnManager.isRequestOneToManyDeliveryDisplayedReportsEnabled()) {
            cpim = ChatUtils.buildCpimMessageWithImdn(from, to, msgId, networkContent,
                    networkMimeType, timestampSent);
        } else if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
            cpim = ChatUtils.buildCpimMessageWithoutDisplayedImdn(from, to, msgId, networkContent,
                    networkMimeType, timestampSent);
        } else {
            cpim = ChatUtils.buildCpimMessage(from, to, networkContent, networkMimeType,
                    timestampSent);
        }
        return cpim;
    }
}
