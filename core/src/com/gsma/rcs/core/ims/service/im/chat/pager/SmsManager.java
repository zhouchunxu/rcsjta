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

package com.gsma.rcs.core.ims.service.im.chat.pager;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.ContributionIdGenerator;
import com.gsma.rcs.core.ims.service.im.chat.LargeMessageModeSession;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.FifoBuffer;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Set;
import java.util.Vector;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.RequireHeader;
import javax2.sip.message.Response;

/**
 * Standalone messaging service manager
 */
public class SmsManager extends Thread {
    /**
     * Boundary tag
     */
    private final static String BOUNDARY_TAG = "boundary1";

    private final static int MAX_PAGER_MODE_SIZE = 900;// 1300

    private final InstantMessagingService mImService;

    private final ImdnManager mImdnManager;

    private final RcsSettings mRcsSettings;

    private final FifoBuffer mBuffer = new FifoBuffer();

    private final static Logger sLogger = Logger.getLogger(SmsManager.class.getSimpleName());

    /**
     * Constructor
     *
     * @param imService InstantMessagingService
     * @param rcsSettings RCS settings accessor
     * @param contactManager Contact manager accessor
     * @param messagingLog MessagingLog
     */
    public SmsManager(InstantMessagingService imService, RcsSettings rcsSettings,
            ContactManager contactManager, MessagingLog messagingLog) {
        mImService = imService;
        mImdnManager = imService.getImdnManager();
        mRcsSettings = rcsSettings;
    }

    /**
     * Terminate manager
     */
    public void terminate() {
        if (sLogger.isActivated()) {
            sLogger.info("Terminate the sms manager");
        }
        mBuffer.close();
    }

    /**
     * Background processing
     */
    public void run() {
        PagerMessage message;
        while ((message = (PagerMessage) mBuffer.getObject()) != null) {
            String chatId = message.getChatId();
            String msgId = message.getChatMsg().getMessageId();
            String mimeType = message.getChatMsg().getMimeType();
            try {
                if (message.getRemoteContacts() != null) {
                    sendOneToManyPagerMessageImmediately(message);
                } else {
                    sendOneToOnePagerMessageImmediately(message);
                }
                // Notify listeners
                // for (StandaloneMessagingSessionListener listener : getListeners()) {
                // listener.onMessageSent(chatId, msgId, mimeType);
                // }

            } catch (NetworkException | PayloadException | RuntimeException e) {
                sLogger.error("Failed to send message for chatId: " + chatId, e);
                // for (StandaloneMessagingSessionListener listener : getListeners()) {
                // listener.onMessageFailedSend(chatId, msgId, mimeType);
                // }
            }
        }
    }

    /**
     * Dequeue 1-1 standalone message
     *
     * @param chatId
     * @param chatMsg
     * @throws PayloadException
     * @throws NetworkException
     */
    public void dequeueOneToOneMessage(String chatId, ChatMessage chatMsg) throws PayloadException,
            NetworkException {
        String content = chatMsg.getContent();
        int contentLength = Base64.encodeBase64ToString(content.getBytes()).length();
        if (contentLength > MAX_PAGER_MODE_SIZE) {
            final LargeMessageModeSession newSession = mImService.createLargeMessageModeSession(
                    chatId, chatMsg);
            // for (StandaloneMessagingSessionListener listener : getListeners()) {
            // //newSession.addEventListener(listener);
            // }
            newSession.start();
        } else {
            // Add request in the buffer for background processing
            PagerMessage pager = new PagerMessage(chatId, chatMsg);
            mBuffer.addObject(pager);
        }
    }

    /**
     * Dequeue 1-n standalone message
     *
     * @param chatId
     * @param remoteContacts
     * @param chatMsg
     * @throws PayloadException
     * @throws NetworkException
     */
    public void dequeueOneToManyMessage(String chatId, Set<ContactId> remoteContacts,
            ChatMessage chatMsg) throws PayloadException, NetworkException {
        String content = chatMsg.getContent();
        String resourceList = ChatUtils.generateChatResourceList(remoteContacts);
        int contentLength = Base64.encodeBase64ToString(content.getBytes()).length();
        if ((contentLength + resourceList.length()) > MAX_PAGER_MODE_SIZE) {
            final LargeMessageModeSession newSession = mImService
                    .createOneToManyLargeMessageModeSession(chatId, remoteContacts, chatMsg);
            // for (StandaloneMessagingSessionListener listener : getListeners()) {
            // // newSession.addEventListener(listener);
            // }
            newSession.start();
        } else {
            // Add request in the buffer for background processing
            PagerMessage pager = new PagerMessage(chatId, remoteContacts, chatMsg);
            mBuffer.addObject(pager);
        }
    }

    /**
     * Create originating dialog path
     */
    private SipDialogPath createOriginatingDialogPath(Uri remoteId) {
        // Set Call-Id
        String callId = mImService.getImsModule().getSipManager().getSipStack().generateCallId();
        // Set the route path
        Vector<String> route = mImService.getImsModule().getSipManager().getSipStack()
                .getServiceRoutePath();
        // Create a dialog path
        SipDialogPath dialogPath = new SipDialogPath(mImService.getImsModule().getSipManager()
                .getSipStack(), callId, 1, remoteId.toString(), ImsModule.getImsUserProfile()
                .getPublicUri(), remoteId.toString(), route, mRcsSettings);
        return dialogPath;
    }

    /**
     * Build a CPIM message
     *
     * @param from
     * @param to
     * @param chatMessage
     * @param displayRequested
     * @return
     */
    private String buildCpimMessage(String from, String to, ChatMessage chatMessage,
            boolean displayRequested) {
        String mimeType = chatMessage.getMimeType();
        String msgId = chatMessage.getMessageId();
        long timestampSent = chatMessage.getTimestampSent();
        String networkMimeType = ChatUtils.apiMimeTypeToNetworkMimeType(mimeType);
        String networkContent = chatMessage.getContent();
        // String networkContent = ChatUtils.persistedContentToNetworkContent(mimeType,
        // chatMessage.getContent(), msgId, timestampSent);
        if (displayRequested) {
            return ChatUtils.buildCpimMessageWithImdn(from, to, msgId, networkContent,
                    networkMimeType, timestampSent);
        } else if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
            return ChatUtils.buildCpimMessageWithoutDisplayedImdn(from, to, msgId, networkContent,
                    networkMimeType, timestampSent);
        }
        return ChatUtils.buildCpimMessage(from, to, networkContent, networkMimeType, timestampSent);
    }

    private String buildMultipartContent(String cpim, Set<ContactId> recipients, String boundaryTag) {
        // Generate the resource list for given recipients
        String resourceList = ChatUtils.generateChatResourceList(recipients);
        // @formatter:off
        String content = Multipart.BOUNDARY_DELIMITER + boundaryTag + SipUtils.CRLF
                + "Content-Type: application/resource-lists+xml" + SipUtils.CRLF
                + "Content-Length: " + resourceList.getBytes(UTF8).length + SipUtils.CRLF
                + "Content-Disposition: recipient-list" + SipUtils.CRLF
                + SipUtils.CRLF
                + resourceList + SipUtils.CRLF
                + SipUtils.CRLF
                + Multipart.BOUNDARY_DELIMITER + boundaryTag + SipUtils.CRLF
                + "Content-Type: " + CpimMessage.MIME_TYPE + SipUtils.CRLF
                + "Content-Length: " + cpim.getBytes(UTF8).length + SipUtils.CRLF
                + SipUtils.CRLF
                + cpim + SipUtils.CRLF
                + Multipart.BOUNDARY_DELIMITER + boundaryTag + Multipart.BOUNDARY_DELIMITER;
        // @formatter:on
        return content;
    }

    /**
     * Create a SIP MESSAGE request
     *
     * @param dialogPath
     * @param chatId
     * @param cpim Content
     * @return the MESSAGE request
     * @throws PayloadException
     */
    private SipRequest createMessageRequest(SipDialogPath dialogPath, String chatId, String cpim)
            throws PayloadException {
        try {
            String featureTag = FeatureTags.FEATURE_3GPP_CPM_MSG;
            String contentType = CpimMessage.MIME_TYPE;
            SipRequest message = SipMessageFactory.createMessage(dialogPath, featureTag,
                    contentType, cpim.getBytes());
            message.addHeader(SipUtils.HEADER_P_PREFERRED_SERVICE,
                    URLDecoder.decode(FeatureTags.FEATURE_OMA_CPM_MSG, StringUtils.UTF8_STR));
            message.addHeader(ChatUtils.HEADER_CONVERSATION_ID, chatId);
            message.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID,
                    ContributionIdGenerator.getContributionId(dialogPath.getCallId()));
            return message;
        } catch (ParseException | UnsupportedEncodingException e) {
            throw new PayloadException("Can't create SIP MESSAGE request!", e);
        }
    }

    /**
     * Create a SIP MESSAGE request
     *
     * @param content Content
     * @return the MESSAGE request
     * @throws PayloadException
     */
    private SipRequest createMultipartMessageRequest(SipDialogPath dialogPath, String chatId,
            String content) throws PayloadException {
        try {
            String featureTag = FeatureTags.FEATURE_3GPP_CPM_MSG;
            String contentType = "multipart/mixed;boundary=" + BOUNDARY_TAG;
            SipRequest message = SipMessageFactory.createMessage(dialogPath, featureTag,
                    contentType, content.getBytes());
            message.addHeader(RequireHeader.NAME, "recipient-list-message");
            message.addHeader(SipUtils.HEADER_P_PREFERRED_SERVICE,
                    URLDecoder.decode(FeatureTags.FEATURE_OMA_CPM_MSG_GROUP, StringUtils.UTF8_STR));
            message.addHeader(ChatUtils.HEADER_CONVERSATION_ID, chatId);
            message.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID,
                    ContributionIdGenerator.getContributionId(dialogPath.getCallId()));
            return message;
        } catch (ParseException | UnsupportedEncodingException e) {
            throw new PayloadException("Can't create multipart SIP MESSAGE request!", e);
        }
    }

    public void sendOneToOnePagerMessageImmediately(PagerMessage pager) throws PayloadException,
            NetworkException {
        // Create authentication agent
        SessionAuthenticationAgent authenticationAgent = new SessionAuthenticationAgent(
                mImService.getImsModule());
        // Create sip dialog path
        Uri remoteId = PhoneUtils.formatContactIdToUri(pager.getChatMsg().getRemoteContact());
        SipDialogPath dialogPath = createOriginatingDialogPath(remoteId);
        // Build CPIM message content
        String cpim = buildCpimMessage(dialogPath.getLocalParty(), dialogPath.getRemoteParty(),
                pager.getChatMsg(), mImdnManager.isRequestOneToOneDeliveryDisplayedReportsEnabled());
        // Create Sip message
        String chatId = pager.getChatId();
        SipRequest message = createMessageRequest(dialogPath, chatId, cpim);
        // Send MESSAGE request
        SipTransactionContext ctx = mImService.getImsModule().getSipManager()
                .sendSipMessageAndWait(message);
        analyzeSipMessageResponse(ctx, authenticationAgent, dialogPath, chatId, cpim);
    }

    public void sendOneToManyPagerMessageImmediately(PagerMessage pager) throws PayloadException,
            NetworkException {
        // Create authentication agent
        SessionAuthenticationAgent authenticationAgent = new SessionAuthenticationAgent(
                mImService.getImsModule());
        // Create sip dialog path
        SipDialogPath dialogPath = createOriginatingDialogPath(mRcsSettings.getImMassUri());
        // Build multipart content
        String cpim = buildCpimMessage(dialogPath.getLocalParty(), dialogPath.getRemoteParty(),
                pager.mChatMessage,
                mImdnManager.isRequestOneToManyDeliveryDisplayedReportsEnabled());
        String content = buildMultipartContent(cpim, pager.mRecipients, BOUNDARY_TAG);
        // Create Sip message
        String chatId = pager.getChatId();
        SipRequest message = createMultipartMessageRequest(dialogPath, chatId, content);
        // Send MESSAGE request
        SipTransactionContext ctx = mImService.getImsModule().getSipManager()
                .sendSipMessageAndWait(message);
        analyzeSipMessageResponse(ctx, authenticationAgent, dialogPath, chatId, content);
    }

    private void analyzeSipMessageResponse(SipTransactionContext ctx,
            SessionAuthenticationAgent authenticationAgent, SipDialogPath dialogPath,
            String chatId, String content) throws NetworkException, PayloadException {
        try {
            int statusCode = ctx.getStatusCode();
            switch (statusCode) {
                case Response.OK:
                case Response.ACCEPTED:
                    if (sLogger.isActivated()) {
                        sLogger.info("20x OK response received");
                    }
                    break;
                case Response.PROXY_AUTHENTICATION_REQUIRED:
                    if (sLogger.isActivated()) {
                        sLogger.info("407 response received");
                    }
                    // Set the remote tag
                    dialogPath.setRemoteTag(ctx.getSipResponse().getToTag());
                    // Update the authentication agent
                    authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());
                    // Increment the Cseq number of the dialog path
                    dialogPath.incrementCseq();
                    // Create a second MESSAGE request with the right token
                    SipRequest message;
                    if (new Multipart(content, BOUNDARY_TAG).isMultipart()) {
                        message = createMultipartMessageRequest(dialogPath, chatId, content);
                    } else {
                        message = createMessageRequest(dialogPath, chatId, content);
                    }
                    // Set the Proxy-Authorization header
                    authenticationAgent.setProxyAuthorizationHeader(message);
                    // Send a second MESSAGE request
                    ctx = mImService.getImsModule().getSipManager().sendSipMessageAndWait(message);
                    analyzeSipMessageResponse(ctx, authenticationAgent, dialogPath, chatId, content);
                    break;
                default:
                    throw new NetworkException("Invalid response: " + statusCode + " received");
            }
        } catch (InvalidArgumentException | ParseException e) {
            throw new NetworkException("Set proxy authorization failed");
        }
    }

    /**
     * Pager mode standalone message
     */
    private static class PagerMessage {
        /**
         * Chat ID
         */
        private String mChatId;

        /**
         * List of recipients for one-to-many session
         */
        private Set<ContactId> mRecipients;

        /**
         * Chat message
         */
        private final ChatMessage mChatMessage;

        /**
         * Constructor
         *
         * @param chatId
         * @param msg ChatMessage
         */
        public PagerMessage(String chatId, ChatMessage msg) {
            mChatId = chatId;
            mChatMessage = msg;
        }

        /**
         * Constructor
         *
         * @param chatId
         * @param recipients
         * @param msg ChatMessage
         */
        public PagerMessage(String chatId, Set<ContactId> recipients, ChatMessage msg) {
            mChatId = chatId;
            mChatMessage = msg;
            mRecipients = recipients;
        }

        public String getChatId() {
            return mChatId;
        }

        public ChatMessage getChatMsg() {
            return mChatMessage;
        }

        public Set<ContactId> getRemoteContacts() {
            return mRecipients;
        }
    }
}
