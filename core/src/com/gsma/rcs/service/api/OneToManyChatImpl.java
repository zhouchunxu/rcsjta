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

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.OneToManyChatSessionListener;
import com.gsma.rcs.core.ims.service.im.chat.SessionUnavailableException;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.LargeMessageModeSession;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.ChatMessagePersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.ImSessionStartMode;
import com.gsma.rcs.service.broadcaster.IOneToManyChatEventBroadcaster;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.Card;
import com.gsma.services.rcs.chat.CloudFile;
import com.gsma.services.rcs.chat.Emoticon;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.chat.IOneToManyChat;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.os.RemoteException;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One-to-Many Chat implementation
 */
public class OneToManyChatImpl extends IOneToManyChat.Stub implements OneToManyChatSessionListener {

    private final String mChatId;

    private final Set<ContactId> mContacts;

    private final IOneToManyChatEventBroadcaster mBroadcaster;

    private final InstantMessagingService mImService;

    private final MessagingLog mMessagingLog;

    private final ChatServiceImpl mChatService;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    private static final Logger sLogger = Logger.getLogger(OneToManyChatImpl.class.getName());

    /**
     * Constructor
     *
     * @param chatId Chat ID
     * @param contacts Remote contact IDs
     * @param broadcaster IChatEventBroadcaster
     * @param imService InstantMessagingService
     * @param messagingLog MessagingLog
     * @param rcsSettings RcsSettings
     * @param chatService ChatServiceImpl
     * @param contactManager ContactManager
     */
    public OneToManyChatImpl(InstantMessagingService imService, String chatId,
            Set<ContactId> contacts, IOneToManyChatEventBroadcaster broadcaster,
            MessagingLog messagingLog, RcsSettings rcsSettings, ChatServiceImpl chatService,
            ContactManager contactManager) {
        mImService = imService;
        mChatId = chatId;
        mContacts = contacts;
        mBroadcaster = broadcaster;
        mMessagingLog = messagingLog;
        mChatService = chatService;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
    }

    /**
     * Get chat ID
     *
     * @return Chat ID
     */
    @Override
    public String getChatId() throws RemoteException {
        return mChatId;
    }

    /**
     * Returns the remote contact identifier
     *
     * @return ContactId
     */
    @Override
    public List<ContactId> getRemoteContacts() throws RemoteException {
        return new ArrayList<>(mContacts);
    }

    /**
     * Returns true if it is possible to send messages in this one to one chat right now, else
     * return false.
     *
     * @return boolean
     * @throws RemoteException
     */
    @Override
    public boolean isAllowedToSendMessage() throws RemoteException {
        try {
            if (!mRcsSettings.getMyCapabilities().isStandaloneMessagingSupported()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot send message on one-to-many chat with chatId '" + mChatId
                            + "' as Standalone Messaging capabilities are not supported for self.");
                }
                return false;
            }
            for (ContactId contact : mContacts) {
                Capabilities remoteCapabilities = mContactManager.getContactCapabilities(contact);
                if (remoteCapabilities == null
                        || !remoteCapabilities.isStandaloneMessagingSupported()) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Cannot send message on one-to-one chat with contact '"
                                + contact + "' as the contact's capabilities are not known.");
                    }
                    return false;
                }
            }
            return true;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    private long getDeliveryExpirationTime(long timestampSent) {
        if (mRcsSettings.isImAlwaysOn()) {
            return 0;
        }
        final long timeout = mRcsSettings.getMsgDeliveryTimeoutPeriod();
        if (timeout == 0L) {
            return 0;
        }
        return timestampSent + timeout;
    }

    /**
     * Add chat message to Db
     *
     * @param msg InstantMessage
     * @param status status of message
     * @throws PayloadException
     * @throws NetworkException
     */
    private void addOutgoingChatMessage(ChatMessage msg, Status status) throws PayloadException,
            NetworkException {
        mMessagingLog.addOutgoingOneToManyChatMessage(mChatId, msg, mContacts, status,
                ReasonCode.UNSPECIFIED, 0);
    }

    /**
     * Set chat message status
     *
     * @param msgId message ID
     * @param mimeType mime type
     * @param status status of message
     * @param reasonCode Reason code
     */
    private void setChatMessageStatusAndReasonCode(String msgId, String mimeType, Status status,
            ReasonCode reasonCode) {
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode)) {
                mBroadcaster.broadcastMessageStatusChanged(mChatId, mimeType, msgId, status,
                        reasonCode);
            }
        }
    }

    /**
     * Set chat message status and timestamp
     *
     * @param msg Chat message
     * @param status status of message
     */
    private void setChatMessageStatusAndTimestamp(ChatMessage msg, Status status) {
        String msgId = msg.getMessageId();
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndTimestamp(msgId, status,
                    ReasonCode.UNSPECIFIED, msg.getTimestamp(), msg.getTimestampSent())) {
                mBroadcaster.broadcastMessageStatusChanged(mChatId, msg.getMimeType(), msgId,
                        status, ReasonCode.UNSPECIFIED);
            }
        }
    }

    /**
     * Sends a plain text message
     * 
     * @param message Text message
     * @return Chat message
     * @throws RemoteException
     */
    @Override
    public IChatMessage sendMessage(String message) throws RemoteException {
        if (TextUtils.isEmpty(message)) {
            throw new ServerApiIllegalArgumentException("message must not be null or empty!");
        }
        int messageLength = message.length();
        int maxMessageLength = mRcsSettings.getMaxChatMessageLength();
        if (messageLength > maxMessageLength) {
            throw new ServerApiIllegalArgumentException("chat message length: " + messageLength
                    + " exceeds max chat message length: " + maxMessageLength + "!");
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Send text message.");
        }
        try {
            long timestamp = System.currentTimeMillis();
            /* For outgoing message, timestampSent = timestamp */
            final ChatMessage msg = ChatUtils
                    .createTextMessage(null, message, timestamp, timestamp);
            ChatMessagePersistedStorageAccessor persistedStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msg.getMessageId(), msg.getRemoteContact(), message,
                    msg.getMimeType(), mChatId, Direction.OUTGOING);
            /* Always insert message with status QUEUED */
            addOutgoingChatMessage(msg, Status.QUEUED);
            mImService.tryToDequeueOneToManyChatMessages(mChatId);
            return new ChatMessageImpl(persistedStorage);
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;
        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc
     * @return ChatMessage
     * @throws RemoteException
     */
    @Override
    public IChatMessage sendMessage2(Geoloc geoloc) throws RemoteException {
        if (geoloc == null) {
            throw new ServerApiIllegalArgumentException("geoloc must not be null!");
        }
        String label = geoloc.getLabel();
        if (label != null) {
            int labelLength = label.length();
            int labelMaxLength = mRcsSettings.getMaxGeolocLabelLength();
            if (labelLength > labelMaxLength) {
                throw new ServerApiIllegalArgumentException("geoloc message label length: "
                        + labelLength + " exceeds max length: " + labelMaxLength + "!");
            }
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Send geolocation message.");
        }
        try {
            long timestamp = System.currentTimeMillis();
            /** For outgoing message, timestampSent = timestamp */
            final ChatMessage geolocMsg = ChatUtils.createGeolocMessage(null, geoloc, timestamp,
                    timestamp);
            ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, geolocMsg.getMessageId(), geolocMsg.getRemoteContact(),
                    geolocMsg.getContent(), geolocMsg.getMimeType(), mChatId, Direction.OUTGOING);
            /* Always insert message with status QUEUED */
            addOutgoingChatMessage(geolocMsg, Status.QUEUED);
            mImService.tryToDequeueOneToManyChatMessages(mChatId);
            return new ChatMessageImpl(persistentStorage);
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;
        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public IChatMessage sendMessage3(Emoticon emoticon) throws RemoteException {
        // TODO
        throw new ServerApiUnsupportedOperationException("Unsupported");
    }

    @Override
    public IChatMessage sendMessage4(CloudFile cloudFile) throws RemoteException {
        // TODO
        throw new ServerApiUnsupportedOperationException("Unsupported");
    }

    @Override
    public IChatMessage sendMessage5(Card card) throws RemoteException {
        // TODO
        throw new ServerApiUnsupportedOperationException("Unsupported");
    }

    /**
     * Dequeue one-to-many chat message
     * 
     * @param msg Chat message
     * @throws SessionUnavailableException
     * @throws PayloadException
     * @throws NetworkException
     */
    public void dequeueOneToManyChatMessage(ChatMessage msg) throws PayloadException,
            NetworkException {
        String msgId = msg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug("Dequeue chat message msgId=".concat(msgId));
        }
        setChatMessageStatusAndTimestamp(msg, Status.SENDING);
        String content = msg.getContent();
        String resourceList = ChatUtils.generateChatResourceList(mContacts);
        int contentLength = Base64.encodeBase64ToString(content.getBytes()).length();
        if ((contentLength + resourceList.length()) > 900/*MAX_PAGER_MODE_SIZE*/) {
            final LargeMessageModeSession newSession = mImService.createLargeMessageModeSession(
                    mChatId, msg);
            newSession.addListener(this);
            newSession.startSession();
        } else {
            // Add request in the buffer for background processing
            mImService.getSmsManager().dequeueOneToOneMessage(mChatId, msg);
        }
    }

    /**
     * Dequeue one-one file info
     * 
     * @param fileTransferId File transfer ID
     * @param fileInfo File information
     * @param oneToManyFileTransfer One to many file transfer implementation
     * @throws PayloadException
     * @throws NetworkException
     * @throws SessionUnavailableException
     */
    public void dequeueOneToManyFileInfo(String fileTransferId, String fileInfo,
    /* boolean displayReportsEnabled, boolean deliverReportsEnabled, */
    OneToManyFileTransferImpl oneToManyFileTransfer) throws PayloadException, NetworkException,
            SessionUnavailableException {
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        mMessagingLog.setFileTransferTimestamps(fileTransferId, timestamp, timestampSent);
        ChatMessage msg = ChatUtils.createFileTransferMessage(null, fileInfo, fileTransferId,
                timestamp, timestampSent);
        mImService.getSmsManager().dequeueOneToOneMessage(mChatId, msg);
        oneToManyFileTransfer.onFileInfoDequeued();
    }

    /**
     * open the chat conversation.
     * 
     * @see ImSessionStartMode
     * @throws RemoteException
     */
    @Override
    public void openChat() throws RemoteException {
        // TODO
    }

    /**
     * Resend a message which previously failed.
     * 
     * @param msgId message ID
     * @throws RemoteException
     */
    @Override
    public void resendMessage(final String msgId) throws RemoteException {
        if (TextUtils.isEmpty(msgId)) {
            throw new ServerApiIllegalArgumentException(
                    "OneToManyChat messageId must not be null or empty!");
        }
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                        mMessagingLog, msgId);
                final String mimeType = persistentStorage.getMimeType();
                /* Set new timestamp for resend message */
                long timestamp = System.currentTimeMillis();
                /* For outgoing message, timestampSent = timestamp */
                final ChatMessage msg = new ChatMessage(msgId, null,
                        persistentStorage.getContent(), mimeType, timestamp, timestamp, null);
                try {
                    if (ServerApiUtils.isImsConnected()) {
                        dequeueOneToManyChatMessage(msg);
                    } else {
                        /* If the IMS is NOT connected at this time then re-queue message. */
                        setChatMessageStatusAndReasonCode(msgId, mimeType, Status.QUEUED,
                                ReasonCode.UNSPECIFIED);
                    }
                } catch (PayloadException | NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.error("Failed to send chat message with msgId '" + msgId + "'", e);
                    }
                    setChatMessageStatusAndReasonCode(msgId, mimeType, Status.FAILED,
                            ReasonCode.FAILED_SEND);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to send chat message with msgId '" + msg.getMessageId()
                            + "'", e);
                }
            }
        });
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    @Override
    public void onMessageReceived(ChatMessage msg, boolean imdnDisplayedRequested,
            boolean deliverySuccess) {
        // Not used
    }

    @Override
    public void onIsComposingEventReceived(ContactId contact, boolean status) {
        // Not used
    }

    @Override
    public void onMessageSent(String msgId, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.info("Chat message sent; msgId=" + msgId + ".");
        }
        synchronized (mLock) {
            setChatMessageStatusAndReasonCode(msgId, mimeType, Status.SENT, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void onMessageFailedSend(String msgId, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.info("Chat message send failed; msgId=" + msgId + ".");
        }
        synchronized (mLock) {
            setChatMessageStatusAndReasonCode(msgId, mimeType, Status.FAILED,
                    ReasonCode.FAILED_SEND);
        }
    }

    @Override
    public void onMessageDeliveryStatusReceived(ContactId contact, ImdnDocument imdn) {
        onMessageDeliveryStatusReceived(mChatId, contact, imdn);
    }

    @Override
    public void onDeliveryStatusReceived(String contributionId, ContactId contact, ImdnDocument imdn) {
        // Not used FIXME?
    }

    @Override
    public void onChatMessageDisplayReportSent(String msgId) {
        // Not used
    }

    private ReasonCode imdnToFailedReasonCode(ImdnDocument imdn) {
        String notificationType = imdn.getNotificationType();
        if (ImdnDocument.DELIVERY_NOTIFICATION.equals(notificationType)) {
            return ReasonCode.FAILED_DELIVERY;

        } else if (ImdnDocument.DISPLAY_NOTIFICATION.equals(notificationType)) {
            return ReasonCode.FAILED_DISPLAY;
        }
        throw new IllegalArgumentException(new StringBuilder(
                "Received invalid imdn notification type:'").append(notificationType).append("'")
                .toString());
    }

    // @Override
    private void onMessageDeliveryStatusReceived(String chatId, ContactId contact, ImdnDocument imdn) {
        ImdnDocument.DeliveryStatus status = imdn.getStatus();
        String msgId = imdn.getMsgId();
        long timestamp = imdn.getDateTime();
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        switch (status) {
            case DELIVERED:
                synchronized (mLock) {
                    if (mMessagingLog.setGroupChatDeliveryInfoDelivered(mChatId, contact, msgId,
                            timestamp)) {
                        mBroadcaster.broadcastMessageDeliveryInfoChanged(mChatId, contact,
                                mimeType, msgId, GroupDeliveryInfo.Status.DELIVERED,
                                GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
                        if (mMessagingLog.isDeliveredToAllRecipients(msgId)) {
                            if (mMessagingLog.setChatMessageStatusDelivered(msgId, timestamp)) {
                                mBroadcaster.broadcastMessageStatusChanged(mChatId, mimeType,
                                        msgId, Status.DELIVERED, ReasonCode.UNSPECIFIED);
                            }
                        }
                    }
                }
                break;
            case DISPLAYED:
                synchronized (mLock) {
                    if (mMessagingLog.setGroupChatDeliveryInfoDisplayed(mChatId, contact, msgId,
                            timestamp)) {
                        mBroadcaster.broadcastMessageDeliveryInfoChanged(mChatId, contact,
                                mimeType, msgId, GroupDeliveryInfo.Status.DISPLAYED,
                                GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
                        if (mMessagingLog.isDisplayedByAllRecipients(msgId)) {
                            if (mMessagingLog.setChatMessageStatusDisplayed(msgId, timestamp)) {
                                mBroadcaster.broadcastMessageStatusChanged(mChatId, mimeType,
                                        msgId, Status.DISPLAYED, ReasonCode.UNSPECIFIED);
                            }
                        }
                    }
                }
                break;
            case ERROR:
            case FAILED:
            case FORBIDDEN:
                ReasonCode reasonCode = imdnToFailedReasonCode(imdn);
                GroupDeliveryInfo.ReasonCode deliveryReasonCode = GroupDeliveryInfo.ReasonCode.UNSPECIFIED;
                if (ReasonCode.FAILED_DELIVERY == reasonCode) {
                    deliveryReasonCode = GroupDeliveryInfo.ReasonCode.FAILED_DELIVERY;
                } else if (ReasonCode.FAILED_DISPLAY == reasonCode) {
                    deliveryReasonCode = GroupDeliveryInfo.ReasonCode.FAILED_DISPLAY;
                }
                synchronized (mLock) {
                    if (!mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(mChatId,
                            contact, msgId, GroupDeliveryInfo.Status.FAILED, deliveryReasonCode)) {
                        /* Add entry with delivered and displayed timestamps set to 0. */
                        mMessagingLog.addGroupChatDeliveryInfoEntry(mChatId, contact, msgId,
                                GroupDeliveryInfo.Status.FAILED, deliveryReasonCode, 0, 0);
                    }
                    mBroadcaster.broadcastMessageDeliveryInfoChanged(mChatId, contact, mimeType,
                            msgId, GroupDeliveryInfo.Status.FAILED, deliveryReasonCode);
                    if (mMessagingLog.isFailedToAllRecipients(msgId)) {
                        if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.FAILED,
                                reasonCode)) {
                            mBroadcaster.broadcastMessageStatusChanged(mChatId, mimeType, msgId,
                                    Status.FAILED, ReasonCode.UNSPECIFIED);
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onSessionStarted(ContactId contact) {
        /* Not used by one-to-many chat session */
    }

    @Override
    public void onSessionAborted(ContactId contact, ImsServiceSession.TerminationReason reason) {
        /* Not used by one-to-many chat session */
    }

    @Override
    public void onSessionRejected(ContactId contact, ImsServiceSession.TerminationReason reason) {
        /* Not used by one-to-many chat session */
    }

    @Override
    public void onSessionAccepting(ContactId contact) {
        /* Not used by one-to-many chat session */
    }
}
