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

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Large message mode standalone message session
 */
public abstract class LargeMessageModeSession extends ImsServiceSession {
    /**
     * Boundary tag
     */
    /* private */final static String BOUNDARY_TAG = "boundary1";

    /**
     * Default SO_TIMEOUT value (in milliseconds)
     */
    public final static long DEFAULT_SO_TIMEOUT = 30000;

    /**
     * Chat message has been transferred to remote
     */
    private boolean mMsgTransferred = false;

    /**
     * Chat message
     */
    private final ChatMessage mChatMsg;

    /**
     * Chat ID
     */
    private String mChatId;

    /**
     * Conversation ID
     */
    private String mConversationId;

    /**
     * Contribution ID
     */
    private String mContributionId;

    private List<String> mFeatureTags = new ArrayList<>();
    private List<String> mAcceptContactTags = new ArrayList<>();
    private String mAcceptTypes = "";
    private String mWrappedTypes = "";

    protected final MessagingLog mMessagingLog;

    protected final InstantMessagingService mImService;

    protected final ImdnManager mImdnManager;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(LargeMessageModeSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param remoteUri Remote URI
     * @param chatMsg Chat message
     * @param rcsSettings RCS settings
     * @param messagingLog Messaging log
     * @param chatMsg Chat message of this session
     * @param timestamp Local timestamp for the session
     * @param contactManager The contact manager accessor
     */
    public LargeMessageModeSession(InstantMessagingService imService, Uri remoteUri,
            ContactId contact, ChatMessage chatMsg, RcsSettings rcsSettings,
            MessagingLog messagingLog, long timestamp, ContactManager contactManager) {
        super(imService, contact, remoteUri, rcsSettings, timestamp, contactManager);
        mImService = imService;
        mImdnManager = imService.getImdnManager();
        mChatMsg = chatMsg;
        mMessagingLog = messagingLog;
        List<String> featureTags = new ArrayList<>();
        featureTags.add(ChatUtils.getFeatureTagForLargeMessageMode(rcsSettings));
        setFeatureTags(featureTags);
        setAcceptContactTags(featureTags);
        addAcceptTypes(CpimMessage.MIME_TYPE);
        addWrappedTypes(MimeType.TEXT_MESSAGE);
        addWrappedTypes(ImdnDocument.MIME_TYPE);
        if (mRcsSettings.isGeoLocationPushSupported()) {
            addWrappedTypes(GeolocInfoDocument.MIME_TYPE);
        }
        if (mRcsSettings.isFileTransferHttpSupported()) {
            addWrappedTypes(FileTransferHttpInfoDocument.MIME_TYPE);
        }
    }

    /**
     * Get feature tags
     * 
     * @return Feature tags
     */
    public String[] getFeatureTags() {
        return mFeatureTags.toArray(new String[mFeatureTags.size()]);
    }

    /**
     * Set feature tags
     * 
     * @param tags Feature tags
     */
    public void setFeatureTags(List<String> tags) {
        this.mFeatureTags = tags;
    }

    /**
     * Get Accept-Contact tags
     * 
     * @return Feature tags
     */
    public String[] getAcceptContactTags() {
        return mAcceptContactTags.toArray(new String[mAcceptContactTags.size()]);
    }

    /**
     * Set Accept-Contact tags
     * 
     * @param tags Feature tags
     */
    public void setAcceptContactTags(List<String> tags) {
        this.mAcceptContactTags = tags;
    }

    /**
     * Get accept types
     *
     * @return Accept types
     */
    protected String getAcceptTypes() {
        return mAcceptTypes;
    }

    /**
     * Add types to accept types
     *
     * @param types Accept types
     */
    protected void addAcceptTypes(String types) {
        if (mAcceptTypes.isEmpty()) {
            mAcceptTypes = types;
        } else {
            mAcceptTypes += " " + types;
        }
    }

    /**
     * Get wrapped types
     *
     * @return Wrapped types
     */
    protected String getWrappedTypes() {
        return mWrappedTypes;
    }

    /**
     * Add types to wrapped types
     *
     * @param types Wrapped types
     */
    protected void addWrappedTypes(String types) {
        if (mWrappedTypes.isEmpty()) {
            mWrappedTypes = types;
        } else {
            mWrappedTypes += " " + types;
        }
    }

    /**
     * Return the Chat ID
     *
     * @return Chat ID
     */
    public String getChatID() {
        return mRcsSettings.isCpmMsgTech() ? mConversationId : mContributionId;
    }

    /**
     * Return the Conversation ID
     * 
     * @return Conversation ID
     */
    public String getConversationID() {
        return mConversationId;
    }

    /**
     * Set the Conversation ID
     * 
     * @param id Conversation ID
     */
    public void setConversationID(String id) {
        this.mConversationId = id;
    }

    /**
     * Return the contribution ID
     * 
     * @return Contribution ID
     */
    public String getContributionID() {
        return mContributionId;
    }

    /**
     * Set the contribution ID
     * 
     * @param id Contribution ID
     */
    public void setContributionID(String id) {
        this.mContributionId = id;
    }

    // /**
    // * Add a listener for receiving events
    // *
    // * @param listener Listener
    // */
    // public void addEventListener(ChatSessionListener listener) {
    // mEventListeners.add(listener);
    // }
    //
    // /**
    // * Remove a listener
    // *
    // * @param listener Listener to remove
    // */
    // public void removeEventListener(ChatSessionListener listener) {
    // mEventListeners.remove(listener);
    // }
    //
    // /**
    // * Returns the event listeners
    // *
    // * @return Listeners
    // */
    // public List<ChatSessionListener> getEventListeners() {
    // return mEventListeners;
    // }

    /**
     * Get chat message
     * 
     * @return chat message
     */
    public ChatMessage getChatMessage() {
        return mChatMsg;
    }

    /**
     * Set message transferred
     */
    public void setMsgTransferred() {
        mMsgTransferred = true;
    }

    /**
     * Is message transferred
     * 
     * @return Boolean
     */
    public boolean isMsgTransferred() {
        return mMsgTransferred;
    }

    /**
     * Receive chat message
     * 
     * @param msg Chat message
     * @param msgSupportsImdnReport True if the message type supports imdn reports
     * @param imdnDisplayedRequested Indicates whether display report was requested
     */
    protected void receive(final ChatMessage msg, final boolean msgSupportsImdnReport,
            final boolean imdnDisplayedRequested) {
        if (mMessagingLog.isMessagePersisted(msg.getMessageId())) {
            // Message already received
            return;
        }
        if (msgSupportsImdnReport && mImdnManager.isDeliveryDeliveredReportsEnabled()) {
            /* Send message delivery status via a SIP MESSAGE */
            String chatId = getConversationID();
            mImdnManager.sendMessageDeliveryStatus(chatId, msg.getRemoteContact(),
                    msg.getMessageId(), ImdnDocument.DeliveryStatus.DELIVERED, getTimestamp());
        }
        for (ImsSessionListener listener : getListeners()) {
            ((ChatSessionListener) listener).onMessageReceived(msg, imdnDisplayedRequested, true);
        }
    }

    @Override
    public SipRequest createInvite() throws PayloadException {
        try {
            SipRequest invite;
            SipDialogPath dialogPath = getDialogPath();
            String content = dialogPath.getLocalContent();
            Multipart multi = new Multipart(content, BOUNDARY_TAG);
            if (multi.isMultipart()) {
                invite = SipMessageFactory.createMultipartInvite(dialogPath, getFeatureTags(),
                        content, BOUNDARY_TAG);
            } else {
                invite = SipMessageFactory.createInvite(dialogPath, getFeatureTags(), content);
            }
            if (mRcsSettings.isCpmMsgTech()) {
                invite.addHeader(SipUtils.HEADER_P_PREFERRED_SERVICE, URLDecoder.decode(
                        FeatureTags.FEATURE_OMA_CPM_LARGE_MSG_GROUP, StringUtils.UTF8_STR));
                invite.addHeader(ChatUtils.HEADER_CONVERSATION_ID, getConversationID());
            }
            invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
            return invite;
        } catch (ParseException | UnsupportedEncodingException e) {
            throw new PayloadException("Failed to create invite request!", e);
        }
    }

    /**
     * Session inactivity event, used for chat session
     */
    @Override
    public void handleInactivityEvent() throws PayloadException, NetworkException {
        /* Not need in this class */
    }

    @Override
    public void handleError(ImsServiceError error) {
        if (isSessionInterrupted()) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.info("Session error: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }
        closeMediaSession();
        removeSession();

        if (!isInitiatedByRemote()) {
            String msgId = mChatMsg.getMessageId();
            String mimeType = mChatMsg.getMimeType();
            for (ImsSessionListener listener : getListeners()) {
                ((ChatSessionListener) listener).onMessageFailedSend(msgId, mimeType);
            }
        }
    }

    @Override
    public void startSession() throws PayloadException, NetworkException {
        // getImsService().getImsModule().getInstantMessagingService().addSession(this);
        start();
    }

    @Override
    public void removeSession() {
        // getImsService().getImsModule().getInstantMessagingService().removeSession(this);
    }

    @Override
    public void terminateSession(TerminationReason reason) throws PayloadException,
            NetworkException {
        super.terminateSession(reason);
        if (!isInitiatedByRemote()) {
            if (!isMsgTransferred()) {
                String msgId = mChatMsg.getMessageId();
                String mimeType = mChatMsg.getMimeType();
                for (ImsSessionListener listener : getListeners()) {
                    ((ChatSessionListener) listener).onMessageFailedSend(msgId, mimeType);
                }
            }
        }
    }
}
