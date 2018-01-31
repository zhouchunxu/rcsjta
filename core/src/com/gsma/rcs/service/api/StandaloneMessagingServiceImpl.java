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
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.core.ims.service.im.standalone.StandaloneMessagingSessionListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.ChatMessagePersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.broadcaster.OneToManyStandaloneMessagingEventBroadcaster;
import com.gsma.rcs.service.broadcaster.OneToOneStandaloneMessagingEventBroadcaster;
import com.gsma.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.chat.Card;
import com.gsma.services.rcs.chat.CloudFile;
import com.gsma.services.rcs.chat.Emoticon;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.sms.IOneToManyStandaloneMessagingListener;
import com.gsma.services.rcs.chat.sms.IOneToOneStandaloneMessagingListener;
import com.gsma.services.rcs.chat.sms.IStandaloneMessagingService;
import com.gsma.services.rcs.chat.sms.IStandaloneMessagingServiceConfiguration;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Standalone messaging service implementation
 */
public class StandaloneMessagingServiceImpl extends IStandaloneMessagingService.Stub implements
        StandaloneMessagingSessionListener {

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    private final OneToOneStandaloneMessagingEventBroadcaster mOneToOneStandaloneMessagingEventBroadcaster = new OneToOneStandaloneMessagingEventBroadcaster();

    private final OneToManyStandaloneMessagingEventBroadcaster mOneToManyStandaloneMessagingEventBroadcaster = new OneToManyStandaloneMessagingEventBroadcaster();

    private final InstantMessagingService mImService;

    private final MessagingLog mMessagingLog;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private static final Logger sLogger = Logger.getLogger(StandaloneMessagingServiceImpl.class
            .getSimpleName());

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    /**
     * Constructor
     * 
     * @param imService Instant Messaging Service
     * @param messagingLog Messaging Log
     * @param rcsSettings Rcs Settings
     * @param contactManager Contact Manager
     * @param ctx the context
     */
    public StandaloneMessagingServiceImpl(InstantMessagingService imService,
            MessagingLog messagingLog, RcsSettings rcsSettings, ContactManager contactManager,
            Context ctx) {
        if (sLogger.isActivated()) {
            sLogger.info("Standalone messaging service API is loaded");
        }
        mImService = imService;
        // mImService.register(this);
        mMessagingLog = messagingLog;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
    }

    /**
     * Close API
     */
    public void close() {
        if (sLogger.isActivated()) {
            sLogger.info("Standalone Messaging service API is closed");
        }
    }

    /**
     * Returns true if the service is registered to the platform, else returns false
     * 
     * @return Returns true if registered else returns false
     */
    @Override
    public boolean isServiceRegistered() {
        return ServerApiUtils.isImsConnected();
    }

    /**
     * Return the reason code for IMS service registration
     * 
     * @return the reason code for IMS service registration
     */
    @Override
    public int getServiceRegistrationReasonCode() {
        return ServerApiUtils.getServiceRegistrationReasonCode().toInt();
    }

    /**
     * Registers a listener on service registration events
     * 
     * @param listener Service registration listener
     */
    @Override
    public void addEventListener(IRcsServiceRegistrationListener listener) {
        if (sLogger.isActivated()) {
            sLogger.info("Add a service listener");
        }
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.addEventListener(listener);
        }
    }

    /**
     * Unregisters a listener on service registration events
     * 
     * @param listener Service registration listener
     */
    @Override
    public void removeEventListener(IRcsServiceRegistrationListener listener) {
        if (sLogger.isActivated()) {
            sLogger.info("Remove a service listener");
        }
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.removeEventListener(listener);
        }
    }

    /**
     * Notifies registration event
     */
    public void notifyRegistration() {
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceRegistered();
        }
    }

    /**
     * Notifies unregistration event
     *
     * @param reasonCode for unregistration
     */
    public void notifyUnRegistration(RcsServiceRegistration.ReasonCode reasonCode) {
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered(reasonCode);
        }
    }

    /**
     * Returns service version
     * 
     * @return Version
     * @see VERSION_CODES
     */
    @Override
    public int getServiceVersion() {
        return RcsService.Build.API_VERSION;
    }

    /**
     * Returns the common service configuration
     * 
     * @return the common service configuration
     */
    @Override
    public ICommonServiceConfiguration getCommonConfiguration() {
        return new CommonServiceConfigurationImpl(mRcsSettings);
    }

    /**
     * Returns the interface to the configuration of the standalone messaging service
     * 
     * @return IStandaloneMessagingServiceConfiguration instance
     * @throws RemoteException
     */
    @Override
    public IStandaloneMessagingServiceConfiguration getConfiguration() throws RemoteException {
        try {
            return new StandaloneMessagingServiceConfigurationImpl(mRcsSettings);

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
     * Tries to send a displayed delivery report for a received standalone message
     * 
     * @param msgId Message ID
     * @param remote Remote contact
     * @param timestamp Timestamp sent in payload for IMDN datetime
     * @throws NetworkException
     * @throws PayloadException
     */
    public void sendDisplayedDeliveryReport(String msgId, ContactId remote, long timestamp)
            throws NetworkException, PayloadException {
        mImService.getImdnManager().sendMessageDeliveryStatus(remote.toString(), remote, msgId,
                ImdnDocument.DeliveryStatus.DISPLAYED, timestamp);
    }

    /**
     * Set one to one message status
     * 
     * @param chatId Chat ID
     * @param msgId Message ID
     * @param mimeType Mime type
     * @param status Status of message
     * @param reasonCode Reason code
     */
    public void setOneToOneMessageStatusAndReasonCode(String chatId, String msgId,
            String mimeType, Status status, ReasonCode reasonCode) {
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode)) {
                mOneToOneStandaloneMessagingEventBroadcaster.broadcastMessageStatusChanged(chatId,
                        mimeType, msgId, status, reasonCode);
            }
        }
    }

    /**
     * Set one to many message status
     * 
     * @param chatId Chat ID
     * @param msgId Message ID
     * @param mimeType Mime type
     * @param status Status of message
     * @param reasonCode Reason code
     */
    public void setOneToManyMessageStatusAndReasonCode(String chatId, String msgId,
            String mimeType, Status status, ReasonCode reasonCode) {
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode)) {
                mOneToManyStandaloneMessagingEventBroadcaster.broadcastMessageStatusChanged(chatId,
                        mimeType, msgId, status, reasonCode);
            }
        }
    }

    /**
     * Add one to one message to Db
     *
     * @param chatId Chat ID
     * @param msg InstantMessage
     * @param status Status of message
     */
    private void addOutgoingOneToOneMessage(String chatId, ChatMessage msg, Status status) {
        mMessagingLog
                .addOutgoingOneToOneChatMessage(chatId, msg, status, ReasonCode.UNSPECIFIED, 0);
    }

    /**
     * Add one to many message to Db
     *
     * @param chatId Chat ID
     * @param msg InstantMessage
     * @param contacts Contact Id set
     * @param status Status of message
     * @throws PayloadException
     * @throws NetworkException
     */
    private void addOutgoingOneToManyChatMessage(String chatId, ChatMessage msg,
            Set<ContactId> contacts, Status status) throws PayloadException, NetworkException {
        mMessagingLog.addOutgoingOneToManyChatMessage(chatId, msg, contacts, status,
                ReasonCode.UNSPECIFIED, 0);
    }

    /**
     * Set one to one message status and timestamp
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param status Status of message
     */
    private void setOneToOneMessageStatusAndTimestamp(String chatId, ChatMessage msg,
            Status status) {
        String msgId = msg.getMessageId();
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndTimestamp(msgId, status,
                    ReasonCode.UNSPECIFIED, msg.getTimestamp(), msg.getTimestampSent())) {
                mOneToOneStandaloneMessagingEventBroadcaster.broadcastMessageStatusChanged(chatId,
                        msg.getMimeType(), msgId, status, ReasonCode.UNSPECIFIED);
            }
        }
    }

    /**
     * Set one to many message status and timestamp
     * 
     * @param chatId Chat ID
     * @param msg Chat message
     * @param status status of message
     */
    private void setOneToManyMessageStatusAndTimestamp(String chatId, ChatMessage msg, Status status) {
        String msgId = msg.getMessageId();
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndTimestamp(msgId, status,
                    ReasonCode.UNSPECIFIED, msg.getTimestamp(), msg.getTimestampSent())) {
                mOneToManyStandaloneMessagingEventBroadcaster.broadcastMessageStatusChanged(chatId,
                        msg.getMimeType(), msgId, status, ReasonCode.UNSPECIFIED);
            }
        }
    }

    /**
     * Dequeue the specific one to one standalone message
     * 
     * @param chatId Chat Id
     * @param chatMsg
     * @throws PayloadException
     * @throws NetworkException
     */
    public void dequeueOneToOneStandaloneMessage(String chatId, ChatMessage chatMsg)
            throws PayloadException, NetworkException {
        String msgId = chatMsg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug("Dequeue one to one standalone message msgId=".concat(msgId));
        }
        setOneToOneMessageStatusAndTimestamp(chatId, chatMsg, Status.SENDING);
        mImService.getSmsManager().dequeueOneToOneMessage(chatId, chatMsg);
    }

    /**
     * Dequeue the specific one to many standalone message
     * 
     * @param chatId Chat Id
     * @param contacts Contact list
     * @param chatMsg
     * @throws PayloadException
     * @throws NetworkException
     */
    public void dequeueOneToManyStandaloneMessage(String chatId, Set<ContactId> contacts,
            ChatMessage chatMsg) throws PayloadException, NetworkException {
        String msgId = chatMsg.getMessageId();
        if (sLogger.isActivated()) {
            sLogger.debug("Dequeue one to many standalone message msgId=".concat(msgId));
        }
        setOneToManyMessageStatusAndTimestamp(chatId, chatMsg, Status.SENDING);
        mImService.getSmsManager().dequeueOneToManyMessage(chatId, contacts, chatMsg);
    }

    /**
     * Returns true if it is possible to send a standalone message to the remote specified by the
     * contact parameter right now, else returns false.
     * 
     * @param contact Remote contact
     * @return boolean
     * @throws RemoteException
     */
    @Override
    public boolean isAllowedToSendMessage(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        try {
            if (!mRcsSettings.getMyCapabilities().isStandaloneMessagingSupported()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot send message with contact '" + contact
                            + "' as standalone message capabilities are not supported for self.");
                }
                return false;
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

    /**
     * Returns true if it is possible to send a standalone message to many specified by the contacts
     * parameter right now, else returns false.
     * 
     * @param contacts Remote contacts
     * @return boolean
     * @throws RemoteException
     */
    @Override
    public boolean isAllowedToSendMessageToMany(List<ContactId> contacts) throws RemoteException {
        if (contacts == null || contacts.isEmpty()) {
            throw new ServerApiIllegalArgumentException("contacts must not be null or empty!");
        }
        try {
            if (!mRcsSettings.getMyCapabilities().isStandaloneMessagingSupported()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot send message on group as standalone message capabilities are not supported for self.");
                }
                return false;
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

    /**
     * Sends an one to one plain text message
     * 
     * @param contact
     * @param message Text message
     * @return Chat message
     * @throws RemoteException
     */
    @Override
    public IChatMessage sendMessage(ContactId contact, String message) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null or empty!");
        }
        if (TextUtils.isEmpty(message)) {
            throw new ServerApiIllegalArgumentException("message must not be null or empty!");
        }

        int messageLength = message.getBytes(StringUtils.UTF8).length;
        int maxMessageLength = mRcsSettings.getMaxStandaloneMsgLength();
        if (messageLength > maxMessageLength) {
            throw new ServerApiIllegalArgumentException("standalone message length: "
                    + messageLength + " exceeds max standalone message length: " + maxMessageLength
                    + "!");
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Send text message.");
        }
        try {
            String chatId = mMessagingLog.getOrCreateCpmChatId();
            long timestamp = System.currentTimeMillis();
            /* For outgoing message, timestampSent = timestamp */
            final ChatMessage msg = ChatUtils.createTextMessage(contact, message, timestamp,
                    timestamp);
            ChatMessagePersistedStorageAccessor persistedStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msg.getMessageId(), contact, message, msg.getMimeType(), chatId,
                    Direction.OUTGOING);
            /* Always insert message with status SMS_QUEUED */
            addOutgoingOneToOneMessage(chatId, msg, Status.SMS_QUEUED);

            mImService.tryToDequeueStandaloneMessages(contact.toString());
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
    public IChatMessage sendMessage2(ContactId contact, Geoloc geoloc) throws RemoteException {
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
            String chatId = mMessagingLog.getOrCreateCpmChatId();
            long timestamp = System.currentTimeMillis();
            /** For outgoing message, timestampSent = timestamp */
            final ChatMessage geolocMsg = ChatUtils.createGeolocMessage(contact, geoloc, timestamp,
                    timestamp);
            ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, geolocMsg.getMessageId(), geolocMsg.getRemoteContact(),
                    geolocMsg.getContent(), geolocMsg.getMimeType(), contact.toString(),
                    Direction.OUTGOING);
            /* Always insert message with status SMS_QUEUED */
            addOutgoingOneToOneMessage(chatId, geolocMsg, Status.SMS_QUEUED);

            mImService.tryToDequeueStandaloneMessages(contact.toString());
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
    public IChatMessage sendMessage3(ContactId contact, Emoticon emoticon) throws RemoteException {
        if (emoticon == null) {
            throw new ServerApiIllegalArgumentException("emoticon must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Send emoticon message.");
        }
        try {
            String chatId = mMessagingLog.getOrCreateCpmChatId();
            long timestamp = System.currentTimeMillis();
            /* For outgoing message, timestampSent = timestamp */
            final ChatMessage msg = ChatUtils.createEmoticonMessage(contact, emoticon, timestamp,
                    timestamp);
            ChatMessagePersistedStorageAccessor persistedStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msg.getMessageId(), msg.getRemoteContact(), msg.getContent(),
                    msg.getMimeType(), contact.toString(), Direction.OUTGOING);
            /* Always insert message with status SMS_QUEUED */
            addOutgoingOneToOneMessage(chatId, msg, Status.SMS_QUEUED);

            mImService.tryToDequeueStandaloneMessages(contact.toString());
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

    @Override
    public IChatMessage sendMessage4(ContactId contact, CloudFile cloudFile) throws RemoteException {
        if (cloudFile == null) {
            throw new ServerApiIllegalArgumentException("cloud file must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Send cloud file message.");
        }
        try {
            String chatId = mMessagingLog.getOrCreateCpmChatId();
            long timestamp = System.currentTimeMillis();
            /* For outgoing message, timestampSent = timestamp */
            final ChatMessage msg = ChatUtils.createCloudFileMessage(contact, cloudFile, timestamp,
                    timestamp);
            ChatMessagePersistedStorageAccessor persistedStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msg.getMessageId(), msg.getRemoteContact(), msg.getContent(),
                    msg.getMimeType(), contact.toString(), Direction.OUTGOING);
            /* Always insert message with status SMS_QUEUED */
            addOutgoingOneToOneMessage(chatId, msg, Status.SMS_QUEUED);

            mImService.tryToDequeueStandaloneMessages(contact.toString());
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

    @Override
    public IChatMessage sendMessage5(ContactId contact, Card card) throws RemoteException {
        if (card == null) {
            throw new ServerApiIllegalArgumentException("card must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Send card message.");
        }
        try {
            String chatId = mMessagingLog.getOrCreateCpmChatId();
            long timestamp = System.currentTimeMillis();
            /* For outgoing message, timestampSent = timestamp */
            final ChatMessage msg = ChatUtils
                    .createCardMessage(contact, card, timestamp, timestamp);
            ChatMessagePersistedStorageAccessor persistedStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msg.getMessageId(), msg.getRemoteContact(), msg.getContent(),
                    msg.getMimeType(), contact.toString(), Direction.OUTGOING);
            /* Always insert message with status SMS_QUEUED */
            addOutgoingOneToOneMessage(chatId, msg, Status.SMS_QUEUED);

            mImService.tryToDequeueStandaloneMessages(contact.toString());
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
     * Sends a group plain text message
     * 
     * @param contacts Contact list
     * @param message Text message
     * @return Chat message
     * @throws RemoteException
     */
    @Override
    public IChatMessage sendMessageToMany(List<ContactId> contacts, String message)
            throws RemoteException {
        if (contacts == null || contacts.isEmpty()) {
            throw new ServerApiIllegalArgumentException("contacts must not be null or empty!");
        }
        if (TextUtils.isEmpty(message)) {
            throw new ServerApiIllegalArgumentException("message must not be null or empty!");
        }

        int messageLength = message.getBytes(StringUtils.UTF8).length;
        int maxMessageLength = mRcsSettings.getMaxStandaloneMsgLength();
        if (messageLength > maxMessageLength) {
            throw new ServerApiIllegalArgumentException("standalone message length: "
                    + messageLength + " exceeds max standalone message length: " + maxMessageLength
                    + "!");
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Send text message to many.");
        }
        try {
            String chatId = mMessagingLog.getOrCreateCpmChatId();
            long timestamp = System.currentTimeMillis();
            /* For outgoing message, timestampSent = timestamp */
            final ChatMessage msg = ChatUtils
                    .createTextMessage(null, message, timestamp, timestamp);
            ChatMessagePersistedStorageAccessor persistedStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msg.getMessageId(), null, message, msg.getMimeType(), chatId,
                    Direction.OUTGOING);
            /* Always insert message with status SMS_QUEUED */
            addOutgoingOneToManyChatMessage(chatId, msg, new HashSet<>(contacts), Status.SMS_QUEUED);

            mImService.tryToDequeueStandaloneMessages(chatId);
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

    @Override
    public IChatMessage sendMessageToMany2(List<ContactId> contacts, Geoloc geoloc)
            throws RemoteException {
        return null;// TODO
    }

    @Override
    public IChatMessage sendMessageToMany3(List<ContactId> contacts, Emoticon emoticon)
            throws RemoteException {
        return null;// TODO
    }

    @Override
    public IChatMessage sendMessageToMany4(List<ContactId> contacts, CloudFile cloudFile)
            throws RemoteException {
        return null;// TODO
    }

    @Override
    public IChatMessage sendMessageToMany5(List<ContactId> contacts, Card card)
            throws RemoteException {
        return null;// TODO
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
                    "Standalone message id must not be null or empty!");
        }
        mImService.scheduleImOperation(new Runnable() {
            public void run() {
                ChatMessagePersistedStorageAccessor persistentStorage = new ChatMessagePersistedStorageAccessor(
                        mMessagingLog, msgId);
                final String mimeType = persistentStorage.getMimeType();
                final String chatId = persistentStorage.getChatId();
                // final String conversationId = persistentStorage.getConversationId();
                final ContactId remote = persistentStorage.getRemoteContact();
                /* One to one or one to mass message */
                final boolean single = (remote != null) && chatId.equals(remote.toString());
                /* Set new timestamp for resend message */
                long timestamp = System.currentTimeMillis();
                /* For outgoing message, timestampSent = timestamp */
                final ChatMessage msg = new ChatMessage(msgId, remote, persistentStorage
                        .getContent(), mimeType, timestamp, timestamp, null);
                try {
                    if (ServerApiUtils.isImsConnected()) {
                        if (remote != null) {
                            dequeueOneToOneStandaloneMessage(chatId, msg);
                        } else  {
                            final Set<ContactId> recipents = null;//FIXME
                            dequeueOneToManyStandaloneMessage(chatId, recipents, msg);
                        }
                    } else {
                        /* If the IMS is NOT connected at this time then re-queue message. */
                        if (single) {
                            setOneToOneMessageStatusAndReasonCode(chatId, msgId, mimeType,
                                    Status.QUEUED, ReasonCode.UNSPECIFIED);
                        } else {
                            setOneToManyMessageStatusAndReasonCode(chatId, msgId, mimeType,
                                    Status.QUEUED, ReasonCode.UNSPECIFIED);
                        }
                    }
                } catch (NetworkException | PayloadException e) {
                    sLogger.error("Failed to send standalone message with msgId '" + msgId + "'", e);
                    if (single) {
                        setOneToOneMessageStatusAndReasonCode(chatId, msgId, mimeType,
                                Status.FAILED, ReasonCode.FAILED_SEND);
                    } else {
                        setOneToManyMessageStatusAndReasonCode(chatId, msgId, mimeType,
                                Status.FAILED, ReasonCode.FAILED_SEND);
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to send standalone message with msgId '" + msgId + "'", e);
                }
            }
        });
    }

    /**
     * Mark a received message as read (ie. displayed in the UI)
     * 
     * @param msgId Message ID
     * @throws RemoteException
     */
    @Override
    public void markMessageAsRead(final String msgId) throws RemoteException {
        if (TextUtils.isEmpty(msgId)) {
            throw new ServerApiIllegalArgumentException("msgId must not be null or empty!");
        }
        /*
         * Firstly message is marked as read in the chat provider. Operation is done synchronously
         * to avoid multiple mark as read requests.
         */
        try {
            if (mMessagingLog.markMessageAsRead(msgId, System.currentTimeMillis()) == 0) {
                /* no reporting towards the network if message is already marked as read */
                if (sLogger.isActivated()) {
                    sLogger.info("Message with ID " + msgId + " is already marked as read!");
                }
                return;
            }

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
        /*
         * Then reporting towards the network is performed asynchronously (i.e. in background).
         */
        mImService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mRcsSettings.isImReportsActivated()
                            && mRcsSettings.isRespondToDisplayReports()) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("tryToDispatchAllPendingDisplayNotifications for msgID "
                                    .concat(msgId));
                        }
                        ImdnManager imdnManager = mImService.getImdnManager();
                        if (imdnManager.isSendOneToOneDeliveryDisplayedReportsEnabled()
                                || imdnManager.isSendGroupDeliveryDisplayedReportsEnabled()) {
                            mImService.tryToDispatchAllPendingDisplayNotifications();
                        }
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to mark message as read!", e);
                }
            }
        });
    }

    /**
     * Adds a listener on one to one standalone messaging events
     * 
     * @param listener One to one standalone messaging event listener
     * @throws RemoteException
     */
    @Override
    public void addEventListener2(IOneToOneStandaloneMessagingListener listener)
            throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be nulSl!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add a one to one standalone messaging event listener");
        }
        try {
            synchronized (mLock) {
                mOneToOneStandaloneMessagingEventBroadcaster
                        .addStandaloneMessagingEventListener(listener);
            }
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
     * Removes a listener on one to one standalone messaging events
     * 
     * @param listener One to one standalone messaging event listener
     * @throws RemoteException
     */
    @Override
    public void removeEventListener2(IOneToOneStandaloneMessagingListener listener)
            throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove a one to one standalone messaging event listener");
        }
        try {
            synchronized (mLock) {
                mOneToOneStandaloneMessagingEventBroadcaster
                        .removeStandaloneMessagingEventListener(listener);
            }
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
     * Adds a listener on one to many standalone messaging events
     * 
     * @param listener One to many standalone messaging event listener
     * @throws RemoteException
     */
    @Override
    public void addEventListener3(IOneToManyStandaloneMessagingListener listener)
            throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be nulSl!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add a one to mass standalone messaging event listener");
        }
        try {
            synchronized (mLock) {
                mOneToManyStandaloneMessagingEventBroadcaster
                        .addStandaloneMessagingEventListener(listener);
            }
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
     * Removes a listener on one to many standalone messaging events
     * 
     * @param listener One to many standalone messaging event listener
     * @throws RemoteException
     */
    @Override
    public void removeEventListener3(IOneToManyStandaloneMessagingListener listener)
            throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove a one to mass standalone messaging event listener");
        }
        try {
            synchronized (mLock) {
                mOneToManyStandaloneMessagingEventBroadcaster
                        .removeStandaloneMessagingEventListener(listener);
            }
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
     * Deletes all one to one standalone messages from history.
     */
    @Override
    public void deleteOneToOneStandaloneMessages() throws RemoteException {
        mImService.tryToDeleteOneToOneChats();
    }

    /**
     * Deletes all one to mass standalone messages from history.
     */
    @Override
    public void deleteOneToManyStandaloneMessages() throws RemoteException {
//        mImService.tryToDeleteOneToManyMessages();
    }

    /**
     * Deletes standalone messages corresponding to a given one to one conversation specified by
     * contact from history.
     * 
     * @param contact the contact
     * @throws RemoteException
     */
    @Override
    public void deleteOneToOneStandaloneMessages2(ContactId contact) throws RemoteException {
        mImService.tryToDeleteOneToOneStandaloneMessages(contact);
    }

    /**
     * Deletes standalone messages corresponding to a given one to mass conversation specified by
     * list of contacts from history.
     *
     * @param contacts group of contacts
     * @throws RemoteException
     */
    @Override
    public void deleteOneToManyStandaloneMessages2(List<ContactId> contacts) throws RemoteException {

        //TODO
    }

    /**
     * Delete a standalone message from its message id from history. Will resolve if the message is
     * one to one or from a one to mass.
     * 
     * @param msgId Message Id
     */
    @Override
    public void deleteMessage(String msgId) throws RemoteException {
//        mImService.tryToDeleteMessage(msgId);
    }

    /**
     * Returns a standalone message from its unique ID
     * 
     * @param msgId Message Id
     * @return IStandaloneMessage
     * @throws RemoteException
     */
    @Override
    public IChatMessage getMessage(String msgId) throws RemoteException {
        if (TextUtils.isEmpty(msgId)) {
            throw new ServerApiIllegalArgumentException("msgId must not be null or empty!");
        }
        try {
            ChatMessagePersistedStorageAccessor persistedStorage = new ChatMessagePersistedStorageAccessor(
                    mMessagingLog, msgId);
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

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    @Override
    public void onMessageReceived(String chatId, ChatMessage msg, boolean imdnDisplayedRequested,
            boolean deliverySuccess) {
        try {
            ContactId remote = msg.getRemoteContact();
            String msgId = msg.getMessageId();
            if (sLogger.isActivated()) {
                sLogger.info("New standalone message with messageId '" + msgId + "' received from "
                        + remote + ".");
            }
            synchronized (mLock) {
                if (mContactManager.isBlockedForContact(remote)) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Contact " + remote
                                + " is blocked: store the message to spam folder.");
                    }
                    mMessagingLog.addOneToOneSpamMessage(chatId, msg);
                } else {
                    if (deliverySuccess) {
                        mMessagingLog.addIncomingOneToOneChatMessage(chatId, msg,
                                imdnDisplayedRequested);
                    } else {
                        mMessagingLog.addOneToOneFailedDeliveryMessage(chatId, msg);
                    }
                }
                mOneToOneStandaloneMessagingEventBroadcaster.broadcastMessageReceived(
                        msg.getMimeType(), msgId);
            }
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to receive the standalone message!", e);
        }
    }

    @Override
    public void onMessageSent(String chatId, String msgId, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.info("Standalone message sent; msgId=" + msgId + ".");
        }
        boolean single = mMessagingLog.isOneToOneChatMessage(msgId);
        synchronized (mLock) {
            if (single) {
                setOneToOneMessageStatusAndReasonCode(chatId, msgId, mimeType, Status.SENT,
                        ReasonCode.UNSPECIFIED);
            } else {
                setOneToManyMessageStatusAndReasonCode(chatId, msgId, mimeType, Status.SENT,
                        ReasonCode.UNSPECIFIED);
            }
        }
    }

    @Override
    public void onMessageFailedSend(String chatId, String msgId, String mimeType) {
        if (sLogger.isActivated()) {
            sLogger.info("Standalone message send failed; msgId=" + msgId + ".");
        }
        boolean single = mMessagingLog.isOneToOneChatMessage(msgId);
        synchronized (mLock) {
            if (single) {
                setOneToOneMessageStatusAndReasonCode(chatId, msgId, mimeType, Status.FAILED,
                        ReasonCode.FAILED_SEND);
            } else {
                setOneToManyMessageStatusAndReasonCode(chatId, msgId, mimeType, Status.FAILED,
                        ReasonCode.FAILED_SEND);
            }
        }
    }

    @Override
    public void onMessageDeliveryStatusReceived(String chatId, ContactId contact, ImdnDocument imdn) {
        if (chatId.equals(contact.toString())) {
            onOneToOneMessageDeliveryStatusReceived(chatId, contact, imdn);
        } else {
            onOneToManyMessageDeliveryStatusReceived(chatId, contact, imdn);
        }
    }

    @Override
    public void onMessageDisplayReportSent(String chatId, String msgId) {
        if (Status.RECEIVED.equals(mMessagingLog.getMessageStatus(msgId))) {
            return;
        }
        synchronized (mLock) {
            if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.RECEIVED,
                    ReasonCode.UNSPECIFIED)) {
                String apiMimeType = mMessagingLog.getMessageMimeType(msgId);
                boolean single = mMessagingLog.isOneToOneChatMessage(msgId);
                if (single) {
                    mOneToOneStandaloneMessagingEventBroadcaster.broadcastMessageStatusChanged(
                            chatId, apiMimeType, msgId, Status.RECEIVED, ReasonCode.UNSPECIFIED);
                } else {
                    mOneToManyStandaloneMessagingEventBroadcaster.broadcastMessageStatusChanged(
                            chatId, apiMimeType, msgId, Status.RECEIVED, ReasonCode.UNSPECIFIED);
                }
            }
        }
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

    private void onOneToOneMessageDeliveryStatusReceived(String chatId, ContactId contact, ImdnDocument imdn) {
        ImdnDocument.DeliveryStatus status = imdn.getStatus();
        String msgId = imdn.getMsgId();
        long timestamp = imdn.getDateTime();
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        switch (status) {
            case DELIVERED:
                synchronized (mLock) {
                    if (mMessagingLog.setChatMessageStatusDelivered(msgId, timestamp)) {
                        mOneToOneStandaloneMessagingEventBroadcaster.broadcastMessageStatusChanged(
                                chatId, mimeType, msgId, Status.DELIVERED, ReasonCode.UNSPECIFIED);
                    }
                }
                break;
            case DISPLAYED:
                synchronized (mLock) {
                    if (mMessagingLog.setChatMessageStatusDisplayed(msgId, timestamp)) {
                        mOneToOneStandaloneMessagingEventBroadcaster.broadcastMessageStatusChanged(
                                chatId, mimeType, msgId, Status.DISPLAYED, ReasonCode.UNSPECIFIED);
                    }
                }
                break;
            case ERROR:
            case FAILED:
            case FORBIDDEN:
                ReasonCode reasonCode = imdnToFailedReasonCode(imdn);
                synchronized (mLock) {
                    if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.FAILED,
                            reasonCode)) {
                        mOneToOneStandaloneMessagingEventBroadcaster.broadcastMessageStatusChanged(
                                chatId, mimeType, msgId, Status.FAILED, reasonCode);
                    }
                }
                break;
        }
    }

    private void onOneToManyMessageDeliveryStatusReceived(String chatId, ContactId contact,
            ImdnDocument imdn) {
        ImdnDocument.DeliveryStatus status = imdn.getStatus();
        String msgId = imdn.getMsgId();
        long timestamp = imdn.getDateTime();
        String mimeType = mMessagingLog.getMessageMimeType(msgId);
        switch (status) {
            case DELIVERED:
                synchronized (mLock) {
                    if (mMessagingLog.setGroupChatDeliveryInfoDelivered(chatId, contact, msgId,
                            timestamp)) {
                        mOneToManyStandaloneMessagingEventBroadcaster
                                .broadcastMessageDeliveryInfoChanged(chatId, contact, mimeType,
                                        msgId, GroupDeliveryInfo.Status.DELIVERED,
                                        GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
                        if (mMessagingLog.isDeliveredToAllRecipients(msgId)) {
                            if (mMessagingLog.setChatMessageStatusDelivered(msgId, timestamp)) {
                                mOneToManyStandaloneMessagingEventBroadcaster
                                        .broadcastMessageStatusChanged(chatId, mimeType, msgId,
                                                Status.DELIVERED, ReasonCode.UNSPECIFIED);
                            }
                        }
                    }
                }
                break;
            case DISPLAYED:
                synchronized (mLock) {
                    if (mMessagingLog.setGroupChatDeliveryInfoDisplayed(chatId, contact, msgId,
                            timestamp)) {
                        mOneToManyStandaloneMessagingEventBroadcaster
                                .broadcastMessageDeliveryInfoChanged(chatId, contact, mimeType,
                                        msgId, GroupDeliveryInfo.Status.DISPLAYED,
                                        GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
                        if (mMessagingLog.isDisplayedByAllRecipients(msgId)) {
                            if (mMessagingLog.setChatMessageStatusDisplayed(msgId, timestamp)) {
                                mOneToManyStandaloneMessagingEventBroadcaster
                                        .broadcastMessageStatusChanged(chatId, mimeType, msgId,
                                                Status.DISPLAYED, ReasonCode.UNSPECIFIED);
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
                    if (!mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(chatId, contact,
                            msgId, GroupDeliveryInfo.Status.FAILED, deliveryReasonCode)) {
                        /* Add entry with delivered and displayed timestamps set to 0. */
                        mMessagingLog.addGroupChatDeliveryInfoEntry(chatId, contact, msgId,
                                GroupDeliveryInfo.Status.FAILED, deliveryReasonCode, 0, 0);
                    }
                    mOneToManyStandaloneMessagingEventBroadcaster
                            .broadcastMessageDeliveryInfoChanged(chatId, contact, mimeType, msgId,
                                    GroupDeliveryInfo.Status.FAILED, deliveryReasonCode);
                    if (mMessagingLog.isFailedToAllRecipients(msgId)) {
                        if (mMessagingLog.setChatMessageStatusAndReasonCode(msgId, Status.FAILED,
                                reasonCode)) {
                            mOneToManyStandaloneMessagingEventBroadcaster
                                    .broadcastMessageStatusChanged(chatId, mimeType, msgId,
                                            Status.FAILED, ReasonCode.UNSPECIFIED);
                        }
                    }
                }
                break;
        }
    }

    public void broadcastOneToOneStandaloneMessagesDeleted(String chatId, Set<String> msgIds) {
        mOneToOneStandaloneMessagingEventBroadcaster.broadcastMessagesDeleted(chatId, msgIds);
    }

    public void broadcastOneToManyStandaloneMessagesDeleted(String chatId, Set<String> msgIds) {
        mOneToManyStandaloneMessagingEventBroadcaster.broadcastMessagesDeleted(chatId, msgIds);
    }
}
