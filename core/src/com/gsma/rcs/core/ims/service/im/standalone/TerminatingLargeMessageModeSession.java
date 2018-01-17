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

package com.gsma.rcs.core.ims.service.im.standalone;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpConstants;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Vector;

/**
 * Terminating large message mode session
 */
public class TerminatingLargeMessageModeSession extends LargeMessageModeSession implements
        MsrpEventListener {

    private MsrpManager mMsrpMgr;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(TerminatingLargeMessageModeSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param invite Initial INVITE request
     * @param contact the remote contactId
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager The contact manager accessor
     * @throws PayloadException
     */
    public TerminatingLargeMessageModeSession(InstantMessagingService imService, SipRequest invite,
            ContactId contact, RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp,
            ContactManager contactManager) throws PayloadException {
        super(imService, PhoneUtils.formatContactIdToUri(contact), contact, null, rcsSettings,
                messagingLog, timestamp, contactManager);
        // Create dialog path
        createTerminatingDialogPath(invite);
        // Set contribution ID
        String id = ChatUtils.getContributionId(invite);
        setContributionID(id);
        // Set conversation ID
        id = ChatUtils.getConversationId(invite);
        setConversationID(id);
        // Session auto-accepted to receive the message
        setSessionAccepted();
    }

    /**
     * Background processing
     */
    public void run() {
        final boolean logActivated = sLogger.isActivated();
        try {
            if (logActivated) {
                sLogger.info("Initiate a new 1-1 large message mode session as terminating.");
            }
            SipDialogPath dialogPath = getDialogPath();
            /* Parse the remote SDP part */
            final SipRequest invite = dialogPath.getInvite();
            String remoteSdp = invite.getSdpContent();
            SipUtils.assertContentIsNotNull(remoteSdp, invite);
            SdpParser parser = new SdpParser(remoteSdp.getBytes(UTF8));
            Vector<MediaDescription> media = parser.getMediaDescriptions();
            MediaDescription mediaDesc = media.elementAt(0);
            String protocol = mediaDesc.mProtocol;
            boolean isSecured = false;
            if (protocol != null) {
                isSecured = protocol.equalsIgnoreCase(MsrpConstants.SOCKET_MSRP_SECURED_PROTOCOL);
            }
            MediaAttribute attr1 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr1.getValue();
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
            int remotePort = mediaDesc.mPort;
            /* Changed by Deutsche Telekom */
            String fingerprint = SdpUtils.extractFingerprint(parser, mediaDesc);
            /* Extract the "setup" parameter */
            String remoteSetup = "passive";
            MediaAttribute attr2 = mediaDesc.getMediaAttribute("setup");
            if (attr2 != null) {
                remoteSetup = attr2.getValue();
            }
            if (logActivated) {
                sLogger.debug("Remote setup attribute is ".concat(remoteSetup));
            }
            /* Set setup mode */
            String localSetup = createSetupAnswer(remoteSetup);
            if (logActivated) {
                sLogger.debug("Local setup attribute is ".concat(localSetup));
            }
            /* Set local port */
            int localMsrpPort;
            if (localSetup.equals("active")) {
                localMsrpPort = 9; /* See RFC4145, Page 4 */
            } else {
                localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort(mRcsSettings);
            }
            /* Create the MSRP manager */
            String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                    .getNetworkAccess().getIpAddress();
            mMsrpMgr = new MsrpManager(localIpAddress, localMsrpPort, getImsService(), mRcsSettings);
            mMsrpMgr.setSecured(isSecured);
            /* Build SDP part */
            String ipAddress = dialogPath.getSipStack().getLocalIpAddress();
            String sdp = SdpUtils.buildChatSDP(ipAddress, localMsrpPort,
                    mMsrpMgr.getLocalSocketProtocol(), getAcceptTypes(), getWrappedTypes(),
                    localSetup, mMsrpMgr.getLocalMsrpPath(), SdpUtils.DIRECTION_RECVONLY);
            /* Set the local SDP part in the dialog path */
            dialogPath.setLocalContent(sdp);
            /* Test if the session should be interrupted */
            if (isInterrupted()) {
                if (logActivated) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }
            /* Create a 200 OK response */
            if (logActivated) {
                sLogger.info("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(dialogPath,
                    getFeatureTags(), sdp);

            dialogPath.setSigEstablished();
            /* Send response */
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessage(resp);
            /* Create the MSRP server session */
            if (localSetup.equals("passive")) {
                /* Passive mode: client wait a connection */
                MsrpSession session = mMsrpMgr.createMsrpServerSession(remotePath, this);
                session.setFailureReportOption(true);
                session.setSuccessReportOption(false);
                mMsrpMgr.openMsrpSession(DEFAULT_SO_TIMEOUT);
                /*
                 * Even if local setup is passive, an empty chunk must be sent to open the NAT and
                 * so enable the active endpoint to initiate a MSRP connection.
                 */
                mMsrpMgr.sendEmptyChunk();
            }
            /* wait a response */
            getImsService().getImsModule().getSipManager().waitResponse(ctx);
            /* Test if the session should be interrupted */
            if (isInterrupted()) {
                if (logActivated) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }
            /* Analyze the received response */
            if (ctx.isSipAck()) {
                if (logActivated) {
                    sLogger.info("ACK request received");
                }
                dialogPath.setSessionEstablished();
                /* Create the MSRP client session */
                if (localSetup.equals("active")) {
                    /* Active mode: client should connect */
                    MsrpSession session = mMsrpMgr.createMsrpClientSession(remoteHost, remotePort,
                            remotePath, this, fingerprint);
                    session.setFailureReportOption(true);
                    session.setSuccessReportOption(false);
                    mMsrpMgr.openMsrpSession(DEFAULT_SO_TIMEOUT);
                    mMsrpMgr.sendEmptyChunk();
                }
                for (ImsSessionListener listener : getListeners()) {
                    listener.onSessionStarted(getRemoteContact());
                }
                /* Start session timer */
                SessionTimerManager sessionTimerManager = getSessionTimerManager();
                if (sessionTimerManager.isSessionTimerActivated(resp)) {
                    sessionTimerManager.start(SessionTimerManager.UAS_ROLE,
                            dialogPath.getSessionExpireTime());
                }
            } else {
                if (logActivated) {
                    sLogger.debug("No ACK received for INVITE");
                }

                /* No response received: timeout */
                handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED));
            }
        } catch (PayloadException e) {
            sLogger.error("Unable to send 200OK response!", e);
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));

        } catch (NetworkException e) {
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));

        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to initiate large message mode session as terminating!", e);
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        }
    }

    @Override
    public void prepareMediaSession() {
        /* Nothing to do in terminating side */
    }

    @Override
    public void openMediaSession() {
        /* Nothing to do in terminating side */
    }

    @Override
    public void startMediaTransfer() {
        /* Nothing to do in terminating side */
    }

    @Override
    public void closeMediaSession() {
        if (mMsrpMgr != null) {
            mMsrpMgr.closeSession();
            if (sLogger.isActivated()) {
                sLogger.debug("MSRP session has been closed");
            }
        }
    }

    @Override
    public void msrpDataTransferred(String msgId) {
        // Not used in terminating side
    }

    @Override
    public void receiveMsrpData(String msgId, byte[] data, String mimeType) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Data received (type " + mimeType + ")");
        }

        if (data == null || data.length == 0) {
            // By-pass empty data
            if (logActivated) {
                sLogger.debug("By-pass received empty data");
            }
            return;
        }
        String toUri = PhoneUtils.extractUriFromSipHeader(getDialogPath().getInvite().getToUri());
        ContactUtil.PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(toUri);
        ContactId toContact = ContactUtil.createContactIdFromValidatedData(number);
        boolean msgSupportsImdnReport = false;
        boolean imdnDisplayedRequested = false;
        if (ChatUtils.isTextPlainType(mimeType)) {
            long timestamp = getTimestamp();
            /**
             * Since legacy server can send non CPIM data (like plain text without timestamp) in the
             * payload, we need to fake timesampSent by using the local timestamp even if this is
             * not the real proper timestamp from the remote side in this case.
             */
            ChatMessage msg = new ChatMessage(msgId, getRemoteContact(), new String(data, UTF8),
                    ChatLog.Message.MimeType.TEXT_MESSAGE, timestamp, timestamp, null);
            receive(msg, msgSupportsImdnReport, imdnDisplayedRequested);
        } else if (ChatUtils.isMessageCpimType(mimeType)) {
            CpimMessage cpimMsg = new CpimParser(data).getCpimMessage();
            long timestamp = System.currentTimeMillis();
            long timestampSent = cpimMsg.getTimestampSent();
            String cpimMsgId = cpimMsg.getHeader(ImdnUtils.HEADER_IMDN_MSG_ID);
            if (cpimMsgId == null) {
                cpimMsgId = msgId;
            }
            String contentType = cpimMsg.getContentType();
            // String mime = ChatUtils.cvrtContentTypeToMime() TODO
            ContactId contact = getRemoteContact();
            // TODO mime should be not the same for diff msg
            ChatMessage msg = new ChatMessage(cpimMsgId, contact, cpimMsg.getMessageContent(),
                    ChatLog.Message.MimeType.TEXT_MESSAGE, timestamp, timestampSent, null);
            String dispositionNotification = cpimMsg.getHeader(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
            if (dispositionNotification != null
                    && dispositionNotification.contains(ImdnDocument.DISPLAY)
                    && mImdnManager.isSendOneToOneDeliveryDisplayedReportsEnabled()) {
                imdnDisplayedRequested = true;
            }
            if (dispositionNotification != null && dispositionNotification.contains("delivery")) {
                msgSupportsImdnReport = true;
            }
            receive(msg, msgSupportsImdnReport, imdnDisplayedRequested);
        } else {
            // Not supported content
            if (logActivated) {
                sLogger.debug("Not supported content " + mimeType
                        + " in large message mode session");
            }
        }
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
        /* Not used here */
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }
}
