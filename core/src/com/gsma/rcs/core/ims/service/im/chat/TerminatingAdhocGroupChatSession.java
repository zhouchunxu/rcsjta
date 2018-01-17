/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.service.im.chat;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
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
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.Participant.Status;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.util.Collection;
import java.util.Map;
import java.util.Vector;

/**
 * Terminating ad-hoc group chat session
 * 
 * @author jexa7410
 */
public class TerminatingAdhocGroupChatSession extends GroupChatSession {

    private static final Logger sLogger = Logger.getLogger(TerminatingAdhocGroupChatSession.class
            .getName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param invite Initial INVITE request
     * @param contact remote contact
     * @param participantsFromInvite Map of participants
     * @param remoteContact the remote contact Uri
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param timestamp Local timestamp for the session
     * @param contactManager Contact manager accessor
     * @throws PayloadException Thrown if constructor fails to get information from payload
     */
    public TerminatingAdhocGroupChatSession(InstantMessagingService imService, SipRequest invite,
            ContactId contact, Map<ContactId, Status> participantsFromInvite,
            Uri remoteContact, RcsSettings rcsSettings, MessagingLog messagingLog, long timestamp,
            ContactManager contactManager) throws PayloadException {
        super(imService, contact, remoteContact, participantsFromInvite, rcsSettings, messagingLog,
                timestamp, contactManager);
        String subject = ChatUtils.getSubject(invite);
        setSubject(subject);
        createTerminatingDialogPath(invite);
        setRemoteDisplayName(SipUtils.getDisplayNameFromInvite(invite));
        String chatId = ChatUtils.getContributionId(invite);
        setContributionID(chatId);
        if (shouldBeAutoAccepted()) {
            setSessionAccepted();
        }
    }

    /**
     * Check is session should be auto accepted. This method should only be called once per session
     * 
     * @return true if group chat session should be auto accepted
     * @throws PayloadException
     */
    private boolean shouldBeAutoAccepted() throws PayloadException {
        /*
         * In case the invite contains a http file transfer info the chat session should be
         * auto-accepted so that the file transfer session can be started.
         */
        return FileTransferUtils.getHttpFTInfo(getDialogPath().getInvite(), mRcsSettings) != null
                || mRcsSettings.isGroupChatAutoAccepted();
    }

    @Override
    public void run() {
        final boolean logActivated = sLogger.isActivated();
        try {
            if (logActivated) {
                sLogger.info("Initiate a new ad-hoc group chat session as terminating");
            }
            Collection<ImsSessionListener> listeners = getListeners();
            ContactId contact = getRemoteContact();
            String subject = getSubject();
            Map<ContactId, Status> participants = getParticipants();
            /* Check if session should be auto-accepted once */
            long timestamp = getTimestamp();
            SipDialogPath dialogPath = getDialogPath();
            if (isSessionAccepted()) {
                if (logActivated) {
                    sLogger.debug("Received group chat invitation marked for auto-accept");
                }
                for (ImsSessionListener listener : listeners) {
                    ((GroupChatSessionListener) listener).onSessionAutoAccepted(contact, subject,
                            participants, timestamp);
                }
            } else {
                if (logActivated) {
                    sLogger.debug("Received group chat invitation marked for manual accept");
                }
                for (ImsSessionListener listener : listeners) {
                    ((GroupChatSessionListener) listener).onSessionInvited(contact, subject,
                            participants, timestamp);
                }
                send180Ringing(dialogPath.getInvite(), dialogPath.getLocalTag());
                InvitationStatus answer = waitInvitationAnswer();
                switch (answer) {
                    case INVITATION_REJECTED_DECLINE:
                        /* Intentional fall through */
                    case INVITATION_REJECTED_BUSY_HERE:
                        if (logActivated) {
                            sLogger.debug("Session has been rejected by user");
                        }
                        sendErrorResponse(dialogPath.getInvite(), dialogPath.getLocalTag(), answer);
                        removeSession();
                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionRejected(contact,
                                    TerminationReason.TERMINATION_BY_USER);
                        }
                        return;

                    case INVITATION_TIMEOUT:
                        if (logActivated) {
                            sLogger.debug("Session has been rejected on timeout");
                        }
                        /* Ringing period timeout */
                        send486Busy(dialogPath.getInvite(), dialogPath.getLocalTag());
                        removeSession();
                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionRejected(contact,
                                    TerminationReason.TERMINATION_BY_TIMEOUT);
                        }
                        return;

                    case INVITATION_REJECTED_BY_SYSTEM:
                        if (logActivated) {
                            sLogger.debug("Session has been aborted by system");
                        }
                        removeSession();
                        return;

                    case INVITATION_CANCELED:
                        if (logActivated) {
                            sLogger.debug("Session has been rejected by remote");
                        }
                        removeSession();
                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionRejected(contact,
                                    TerminationReason.TERMINATION_BY_REMOTE);
                        }
                        return;

                    case INVITATION_ACCEPTED:
                        setSessionAccepted();
                        for (ImsSessionListener listener : listeners) {
                            listener.onSessionAccepting(contact);
                        }
                        break;

                    case INVITATION_DELETED:
                        if (sLogger.isActivated()) {
                            sLogger.debug("Session has been deleted");
                        }
                        removeSession();
                        return;

                    default:
                        throw new IllegalArgumentException(
                                "Unknown invitation answer in run; answer=" + answer);
                }
            }
            /* Parse the remote SDP part */
            final SipRequest invite = dialogPath.getInvite();
            String remoteSdp = invite.getSdpContent();
            SipUtils.assertContentIsNotNull(remoteSdp, invite);
            SdpParser parser = new SdpParser(remoteSdp.getBytes(UTF8));
            Vector<MediaDescription> media = parser.getMediaDescriptions();
            MediaDescription mediaDesc = media.elementAt(0);
            MediaAttribute attr1 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr1.getValue();
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
            int remotePort = mediaDesc.mPort;
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
                localMsrpPort = getMsrpMgr().getLocalMsrpPort();
            }
            /* Build SDP part */
            String ipAddress = dialogPath.getSipStack().getLocalIpAddress();
            String sdp = SdpUtils.buildGroupChatSDP(ipAddress, localMsrpPort, getMsrpMgr()
                    .getLocalSocketProtocol(), getAcceptTypes(), getWrappedTypes(), localSetup,
                    getMsrpMgr().getLocalMsrpPath(), SdpUtils.DIRECTION_SENDRECV);
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
                    getFeatureTags(), getAcceptContactTags(), sdp);
            dialogPath.setSigEstablished();
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessage(resp);
            /* Create the MSRP server session */
            if (localSetup.equals("passive")) {
                /* Passive mode: client wait a connection */
                MsrpSession session = getMsrpMgr().createMsrpServerSession(remotePath, this);
                session.setFailureReportOption(false);
                session.setSuccessReportOption(false);
                getMsrpMgr().openMsrpSession();
                /*
                 * Even if local setup is passive, an empty packet must be sent to open the NAT and
                 * so enable the active endpoint to initiate a MSRP connection.
                 */
                sendEmptyDataChunk();
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
                    MsrpSession session = getMsrpMgr().createMsrpClientSession(remoteHost,
                            remotePort, remotePath, this, fingerprint);
                    session.setFailureReportOption(false);
                    session.setSuccessReportOption(false);
                    getMsrpMgr().openMsrpSession();
                    sendEmptyDataChunk();
                }
                for (ImsSessionListener listener : listeners) {
                    listener.onSessionStarted(contact);
                }
                getConferenceEventSubscriber().subscribe();
                SessionTimerManager sessionTimerManager = getSessionTimerManager();
                if (sessionTimerManager.isSessionTimerActivated(resp)) {
                    sessionTimerManager.start(SessionTimerManager.UAS_ROLE,
                            dialogPath.getSessionExpireTime());
                }
            } else {
                /* No response received: timeout */
                handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED));
            }
        } catch (PayloadException | RuntimeException | NetworkException e) {
            handleError(new ChatError(ChatError.SEND_RESPONSE_FAILED, e));
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }

    @Override
    public void startSession() {
        final boolean logActivated = sLogger.isActivated();
        String chatId = getContributionID();
        if (logActivated) {
            sLogger.debug("Start GroupChatSession with chatID: ".concat(chatId));
        }
        InstantMessagingService imService = getImsService().getImsModule()
                .getInstantMessagingService();
        GroupChatSession currentSession = imService.getGroupChatSession(chatId);
        if (currentSession != null) {
            /*
             * If there is already a groupchat session with same chatId existing, we should not
             * reject the new session but update cache with this groupchat session and mark the old
             * groupchat session pending for removal which will timeout eventually
             */
            if (logActivated) {
                sLogger.debug("Ongoing GrooupChat session detected for chatId '" + chatId
                        + "' marking that session pending for removal");
            }
            currentSession.markForPendingRemoval();
            /*
             * Since the current session was already established and we are now replacing that
             * session with a new session then we make sure to auto-accept that new replacement
             * session also so to leave the client in the same situation for the replacement session
             * as for the original "current" session regardless if the the provisioning setting for
             * chat is set to non-auto-accept or not.
             */
            if (currentSession.getDialogPath().isSessionEstablished()) {
                setSessionAccepted();
            }
        }
        imService.addSession(this);
        start();
    }

    /**
     * Request capabilities to contact
     * 
     * @param contact the contact
     */
    private void requestContactCapabilities(String contact) {
        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(contact);
        if (number != null) {
            ContactId remote = ContactUtil.createContactIdFromValidatedData(number);
            getImsService().getImsModule().getCapabilityService()
                    .requestContactCapabilities(remote);
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("Failed to request capabilities: invalid contact '" + contact + "'");
            }
        }
    }

    @Override
    public void receiveBye(SipRequest bye) throws PayloadException, NetworkException {
        super.receiveBye(bye);
        requestContactCapabilities(getDialogPath().getRemoteParty());
    }

    /**
     * Receive CANCEL request
     * 
     * @param cancel CANCEL request
     * @throws PayloadException
     * @throws NetworkException
     */
    public void receiveCancel(SipRequest cancel) throws NetworkException, PayloadException {
        super.receiveCancel(cancel);
        requestContactCapabilities(getDialogPath().getRemoteParty());
    }
}
