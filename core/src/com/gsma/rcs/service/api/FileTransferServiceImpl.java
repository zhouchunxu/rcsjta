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

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.SessionNotEstablishedException;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.platform.file.FileDescription;
import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.FileTransferPersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.service.broadcaster.GroupFileTransferBroadcaster;
import com.gsma.rcs.service.broadcaster.OneToManyFileTransferBroadcaster;
import com.gsma.rcs.service.broadcaster.OneToOneFileTransferBroadcaster;
import com.gsma.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMode;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.IFileTransfer;
import com.gsma.services.rcs.filetransfer.IFileTransferService;
import com.gsma.services.rcs.filetransfer.IFileTransferServiceConfiguration;
import com.gsma.services.rcs.filetransfer.IGroupFileTransferListener;
import com.gsma.services.rcs.filetransfer.IOneToManyFileTransferListener;
import com.gsma.services.rcs.filetransfer.IOneToOneFileTransferListener;
import com.gsma.services.rcs.filetransfer.FileTransfer.Disposition;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * File transfer service implementation
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class FileTransferServiceImpl extends IFileTransferService.Stub {

    private final OneToOneFileTransferBroadcaster mOneToOneFileTransferBroadcaster = new OneToOneFileTransferBroadcaster();

    private final OneToManyFileTransferBroadcaster mOneToManyFileTransferBroadcaster = new OneToManyFileTransferBroadcaster();

    private final GroupFileTransferBroadcaster mGroupFileTransferBroadcaster = new GroupFileTransferBroadcaster();

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    private final InstantMessagingService mImService;

    private final ChatServiceImpl mChatService;

    private final MessagingLog mMessagingLog;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private final Context mCtx;

    private final Map<String, OneToOneFileTransferImpl> mOneToOneFileTransferCache = new HashMap<>();

    private final Map<String, OneToManyFileTransferImpl> mOneToManyFileTransferCache = new HashMap<>();

    private final Map<String, GroupFileTransferImpl> mGroupFileTransferCache = new HashMap<>();

    private static final Logger sLogger = Logger.getLogger(FileTransferServiceImpl.class
            .getSimpleName());

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    /**
     * Constructor
     * 
     * @param imService Instant Messaging Service
     * @param chatService Chat service
     * @param messagingLog Messaging Log
     * @param rcsSettings Rcs Settings
     * @param contactManager Contact Manager
     * @param ctx the context
     */
    public FileTransferServiceImpl(InstantMessagingService imService, ChatServiceImpl chatService,
            MessagingLog messagingLog, RcsSettings rcsSettings, ContactManager contactManager,
            Context ctx) {
        if (sLogger.isActivated()) {
            sLogger.info("File transfer service API is loaded");
        }
        mImService = imService;
        mImService.register(this);
        mChatService = chatService;
        mMessagingLog = messagingLog;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mCtx = ctx;
    }

    private ReasonCode imdnToFileTransferFailedReasonCode(ImdnDocument imdn) {
        String notificationType = imdn.getNotificationType();
        if (ImdnDocument.DELIVERY_NOTIFICATION.equals(notificationType)) {
            return ReasonCode.FAILED_DELIVERY;

        } else if (ImdnDocument.DISPLAY_NOTIFICATION.equals(notificationType)) {
            return ReasonCode.FAILED_DISPLAY;
        }

        throw new IllegalArgumentException("Received invalid IMDN notification type:'"
                + notificationType + "'");
    }

    public void ensureThumbnailIsDeleted(String transferId) {
        Uri icon = mMessagingLog.getFileTransferIcon(transferId);
        if (icon != null) {
            new File(icon.getPath()).delete();
        }
    }

    /**
     * Ensure copy of file if existing is deleted
     * 
     * @param transferId Unique Id of file transfer
     */
    public void ensureFileCopyIsDeletedIfExisting(String transferId) {
        if (Direction.INCOMING == mMessagingLog.getFileTransferDirection(transferId)) {
            return;
        }
        Uri file = mMessagingLog.getFile(transferId);
        if (file != null) {
            new File(file.getPath()).delete();
        }
    }

    /**
     * Close API
     */
    public void close() {
        /* Clear list of sessions */
        mOneToOneFileTransferCache.clear();
        mGroupFileTransferCache.clear();

        if (sLogger.isActivated()) {
            sLogger.info("File transfer service API is closed");
        }
    }

    /**
     * Remove a one-to-one file transfer from the list
     * 
     * @param fileTransferId File transfer ID
     */
    public void removeOneToOneFileTransfer(String fileTransferId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a file transfer from the list (size="
                    + mOneToOneFileTransferCache.size() + ")");
        }

        mOneToOneFileTransferCache.remove(fileTransferId);
    }

    /**
     * Remove a one-to-many file transfer from the list
     *
     * @param fileTransferId File transfer ID
     */
    public void removeOneToManyFileTransfer(String fileTransferId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a file transfer from the list (size="
                    + mOneToManyFileTransferCache.size() + ")");
        }

        mOneToManyFileTransferCache.remove(fileTransferId);
    }

    /**
     * Remove a group file transfer from the list
     * 
     * @param fileTransferId File transfer ID
     */
    public void removeGroupFileTransfer(String fileTransferId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a file transfer from the list (size="
                    + mGroupFileTransferCache.size() + ")");
        }

        mGroupFileTransferCache.remove(fileTransferId);
    }

    @Override
    public boolean isServiceRegistered() {
        return ServerApiUtils.isImsConnected();
    }

    @Override
    public int getServiceRegistrationReasonCode() {
        return ServerApiUtils.getServiceRegistrationReasonCode().toInt();
    }

    /**
     * Get or create group file transfer
     * 
     * @param chatId the chat ID
     * @param transferId th file transfer ID
     * @return GroupFileTransferImpl
     */
    public GroupFileTransferImpl getOrCreateGroupFileTransfer(String chatId, String transferId) {
        GroupFileTransferImpl groupFileTransfer = mGroupFileTransferCache.get(transferId);
        if (groupFileTransfer == null) {
            FileTransferPersistedStorageAccessor persistedStorage = new FileTransferPersistedStorageAccessor(
                    transferId, mMessagingLog);
            groupFileTransfer = new GroupFileTransferImpl(mImService, transferId,
                    mGroupFileTransferBroadcaster, persistedStorage, this, mRcsSettings,
                    mMessagingLog, mContactManager, chatId);
            mGroupFileTransferCache.put(transferId, groupFileTransfer);
        }
        return groupFileTransfer;
    }


    /**
     * Get or create one-to-many file transfer
     *
     * @param transferId th file transfer ID
     * @return OneToManyFileTransferImpl
     */
    public OneToManyFileTransferImpl getOrCreateOneToManyFileTransfer(Set<ContactId> contacts,
            String transferId) {
        OneToManyFileTransferImpl massFileTransfer = mOneToManyFileTransferCache.get(transferId);
        if (massFileTransfer == null) {
            FileTransferPersistedStorageAccessor persistedStorage = new FileTransferPersistedStorageAccessor(
                    transferId, mMessagingLog);
            massFileTransfer = new OneToManyFileTransferImpl(mImService, transferId,
                    mOneToManyFileTransferBroadcaster, persistedStorage, this, mRcsSettings,
                    mMessagingLog, mContactManager, contacts);
            mOneToManyFileTransferCache.put(transferId, massFileTransfer);
        }
        return massFileTransfer;
    }

    /**
     * Get or create one-one file transfer
     * 
     * @param transferId th file transfer ID
     * @return OneToOneFileTransferImpl
     */
    public OneToOneFileTransferImpl getOrCreateOneToOneFileTransfer(String transferId) {
        OneToOneFileTransferImpl oneToOneFileTransfer = mOneToOneFileTransferCache.get(transferId);
        if (oneToOneFileTransfer == null) {
            FileTransferPersistedStorageAccessor persistedStorage = new FileTransferPersistedStorageAccessor(
                    transferId, mMessagingLog);
            oneToOneFileTransfer = new OneToOneFileTransferImpl(mImService, transferId,
                    mOneToOneFileTransferBroadcaster, persistedStorage, this, mRcsSettings,
                    mMessagingLog, mContactManager);
            mOneToOneFileTransferCache.put(transferId, oneToOneFileTransfer);
        }
        return oneToOneFileTransfer;
    }

    @Override
    public void addEventListener(IRcsServiceRegistrationListener listener) {
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.addEventListener(listener);
        }
    }

    @Override
    public void removeEventListener(IRcsServiceRegistrationListener listener) {
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
     * Notifies un registration event
     * 
     * @param reasonCode for un registration
     */
    public void notifyUnRegistration(RcsServiceRegistration.ReasonCode reasonCode) {
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered(reasonCode);
        }
    }

    /**
     * Receive a new file transfer invitation
     * 
     * @param session File transfer session
     * @param isGroup is group file transfer
     * @param contact Contact ID
     * @param displayName the display name of the remote contact
     */
    public void receiveFileTransferInvitation(FileSharingSession session, boolean isGroup,
            ContactId contact, String displayName) {
        if (sLogger.isActivated()) {
            sLogger.info("Receive FT invitation from " + contact + " file="
                    + session.getContent().getName() + " size=" + session.getContent().getSize()
                    + " displayName=" + displayName + " isGroup=" + isGroup);
        }
        String fileTransferId = session.getFileTransferId();
        if (isGroup) {
            GroupFileTransferImpl groupFileTransfer = getOrCreateGroupFileTransfer(
                    session.getContributionID(), fileTransferId);
            session.addListener(groupFileTransfer);
        } else {
            OneToOneFileTransferImpl oneToOneFileTransfer = getOrCreateOneToOneFileTransfer(fileTransferId);
            session.addListener(oneToOneFileTransfer);
        }
    }

    /**
     * Receive a new resend file transfer invitation
     * 
     * @param session File transfer session
     * @param remoteContact Contact ID of remote contact
     * @param displayName the display name of the remote contact
     */
    public void receiveResendFileTransferInvitation(FileSharingSession session,
            ContactId remoteContact, String displayName) {
        if (sLogger.isActivated()) {
            sLogger.info("Receive resend FT invitation from " + remoteContact + " displayName="
                    + displayName);
        }
        OneToOneFileTransferImpl oneToOneFileTransfer = getOrCreateOneToOneFileTransfer(session
                .getFileTransferId());
        session.addListener(oneToOneFileTransfer);
    }

    /**
     * Returns the interface to the configuration of the file transfer service
     * 
     * @return IFileTransferServiceConfiguration instance
     * @throws RemoteException
     */
    public IFileTransferServiceConfiguration getConfiguration() throws RemoteException {
        try {
            return new FileTransferServiceConfigurationImpl(mRcsSettings);

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
     * Add outgoing one to one file transfer to DB
     * 
     * @param fileTransferId File transfer ID
     * @param chatId Chat ID of one-to-one chat
     * @param contact ContactId
     * @param content Content of file
     * @param fileicon Content of file icon
     * @param state State of the file transfer
     * @param timestamp Local timestamp of the file transfer
     * @param timestampSent Timestamp sent in payload of the file transfer
     */
    private void addOutgoingOneToOneFileTransfer(String fileTransferId, String chatId,
            ContactId contact, MmContent content, MmContent fileicon, State state, long timestamp,
            long timestampSent) {
        mMessagingLog.addOneToOneFileTransfer(fileTransferId, chatId, contact, Direction.OUTGOING,
                content, fileicon, state, ReasonCode.UNSPECIFIED, timestamp, timestampSent,
                FileTransferData.UNKNOWN_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION);
    }

    /**
     * Add outgoing one-to-many file transfer to DB
     *
     * @param fileTransferId File transfer ID
     * @param chatId Chat ID of one-to-many chat
     * @param contacts group of ContactIds
     * @param content Content of file
     * @param fileicon Content of fileicon
     * @param state state of file transfer
     * @param timestamp Local timestamp of the file transfer
     * @param timestampSent Timestamp sent in payload of the file transfer
     */
    private void addOutgoingOneToManyFileTransfer(String fileTransferId, String chatId,
            Set<ContactId> contacts, MmContent content, MmContent fileicon, State state,
            long timestamp, long timestampSent) {
        mMessagingLog.addOutgoingOneToManyFileTransfer(fileTransferId, chatId, contacts, content,
                fileicon, state, FileTransfer.ReasonCode.UNSPECIFIED, timestamp, timestampSent,
                FileTransferData.UNKNOWN_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION);
    }

    /**
     * Add outgoing group file transfer to DB
     * 
     * @param fileTransferId File transfer ID
     * @param chatId Chat ID of group chat
     * @param content Content of file
     * @param fileicon Content of fileicon
     * @param state state of file transfer
     * @param timestamp Local timestamp of the file transfer
     * @param timestampSent Timestamp sent in payload of the file transfer
     */
    private void addOutgoingGroupFileTransfer(String fileTransferId, String chatId,
            MmContent content, MmContent fileicon, State state, long timestamp, long timestampSent) {
        Set<ContactId> recipients = mChatService.getOrCreateGroupChat(chatId).getRecipients();
        if (recipients == null) {
            throw new ServerApiPersistentStorageException(
                    "Unable to determine recipients of the group chat " + chatId
                            + " to set as recipients for the the group file transfer "
                            + fileTransferId + "!");
        }
        mMessagingLog.addOutgoingGroupFileTransfer(fileTransferId, chatId, content, fileicon,
                recipients, state, FileTransfer.ReasonCode.UNSPECIFIED, timestamp, timestampSent);
    }

    /**
     * Set one-one file transfer state and timestamps
     * 
     * @param fileTransferId the file transfer ID
     * @param contact the contact
     * @param state the file transfer state
     * @param timestamp the timestamp
     * @param timestampSent the sent timestamp
     */
    public void setOneToOneFileTransferStateAndTimestamp(String fileTransferId, ContactId contact,
            State state, long timestamp, long timestampSent) {
        if (mMessagingLog.setFileTransferStateAndTimestamp(fileTransferId, state,
                ReasonCode.UNSPECIFIED, timestamp, timestampSent)) {
            mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId, state,
                    ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Set one-to-many file transfer state and timestamp
     *
     * @param fileTransferId the file transfer ID
     * @param chatId the chat ID
     * @param state the file transfer state
     * @param timestamp the timestamp
     * @param timestampSent the sent timestamp
     */
    public void setOneToManyFileTransferStateAndTimestamp(String fileTransferId, String chatId,
            State state, long timestamp, long timestampSent) {
        if (mMessagingLog.setFileTransferStateAndTimestamp(fileTransferId, state,
                ReasonCode.UNSPECIFIED, timestamp, timestampSent)) {
            mOneToManyFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId, state,
                    ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Set group file transfer state and timestamp
     * 
     * @param fileTransferId the file transfer ID
     * @param chatId the chat ID
     * @param state the file transfer state
     * @param timestamp the timestamp
     * @param timestampSent the sent timestamp
     */
    public void setGroupFileTransferStateAndTimestamp(String fileTransferId, String chatId,
            State state, long timestamp, long timestampSent) {
        if (mMessagingLog.setFileTransferStateAndTimestamp(fileTransferId, state,
                ReasonCode.UNSPECIFIED, timestamp, timestampSent)) {
            mGroupFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId, state,
                    ReasonCode.UNSPECIFIED);
        }
    }

    public FileTransferProtocol getFileTransferProtocolForOneToOneFileTransfer(ContactId contact) {
        Capabilities myCapabilities = mRcsSettings.getMyCapabilities();
        Capabilities remoteCapabilities = mContactManager.getContactCapabilities(contact);
        if (remoteCapabilities == null) {
            return null;
        }
        boolean ftMsrpSupportedforSelf = myCapabilities.isFileTransferMsrpSupported();
        boolean ftHttpSupportedforSelf = myCapabilities.isFileTransferHttpSupported();
        boolean ftMsrpSupportedforRemote = remoteCapabilities.isFileTransferMsrpSupported();
        boolean ftHttpSupportedforRemote = remoteCapabilities.isFileTransferHttpSupported();
        if (ftMsrpSupportedforSelf && ftMsrpSupportedforRemote) {
            if (ftHttpSupportedforSelf && ftHttpSupportedforRemote) {
                return mRcsSettings.getFtProtocol();
            }
            return FileTransferProtocol.MSRP;
        } else if (ftHttpSupportedforSelf && ftHttpSupportedforRemote) {
            return FileTransferProtocol.HTTP;
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("There are are no available capabilities : FTMsrp(Self)"
                        + ftMsrpSupportedforSelf + " FTHttp(Self)" + ftHttpSupportedforSelf
                        + " FTMsrp(Remote)" + ftMsrpSupportedforRemote + " FTHttp(Remote)"
                        + ftHttpSupportedforRemote);
            }
            return null;
        }
    }

    /**
     * Dequeue one-to-one file transfer
     *
     * @param chatId the chat ID
     * @param fileTransferId the file transfer ID
     * @param contact the contact
     * @param file the file
     * @param fileIcon the file icon
     */
    public void dequeueOneToOneFileTransfer(String chatId, String fileTransferId,
            ContactId contact, MmContent file, MmContent fileIcon) {
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        FileTransferProtocol ftProtocol = getFileTransferProtocolForOneToOneFileTransfer(contact);
        if (ftProtocol == null) {
            throw new ServerApiGenericException(
                    "No valid file transfer protocol could be determined for dequeueing file with fileTransferId '"
                            + fileTransferId + "'!");
        }
        FileSharingSession session = mImService.createFileTransferSession(chatId, fileTransferId,
                contact, file, fileIcon, timestamp, ftProtocol);
        OneToOneFileTransferImpl oneToOneFileTransfer = getOrCreateOneToOneFileTransfer(fileTransferId);
        session.addListener(oneToOneFileTransfer);
        setOneToOneFileTransferStateAndTimestamp(fileTransferId, contact, State.INITIATING,
                timestamp, timestampSent);
        session.startSession();
    }

    /**
     * 1-1 file re-send operation initiated
     * 
     * @param contact the contact
     * @param file the file
     * @param fileIcon the file icon
     * @param fileTransferId the file transfer ID
     */
    /* package private */void resendOneToOneFile(ContactId contact, MmContent file,
            MmContent fileIcon, String fileTransferId) {
        /* Set new timestamp for the resend file */
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        if (!ServerApiUtils.isImsConnected()) {
            /*
             * If the IMS is NOT connected at this time then re-queue transfer.
             */
            setOneToOneFileTransferStateAndTimestamp(fileTransferId, contact, State.QUEUED,
                    timestamp, timestampSent);
            return;
        }
        if (!mImService.isFileTransferSessionAvailable()
                || mImService.isMaxConcurrentOutgoingFileTransfersReached()) {
            if (sLogger.isActivated()) {
                sLogger.debug("The max number of file transfer sessions is achieved: re-queue the file transfer with Id "
                        .concat(fileTransferId));
            }
            setOneToOneFileTransferStateAndTimestamp(fileTransferId, contact, State.QUEUED,
                    timestamp, timestampSent);
            return;
        }

        setOneToOneFileTransferStateAndTimestamp(fileTransferId, contact, State.INITIATING,
                timestamp, timestampSent);
        FileTransferProtocol ftProtocol = getFileTransferProtocolForOneToOneFileTransfer(contact);
        if (ftProtocol == null) {
            throw new ServerApiGenericException(
                    "No valid file transfer protocol could be determined for resending file with Id '"
                            + fileTransferId + "'!");
        }

        String chatId = contact.toString();//TODO CPM is conversationId
        final FileSharingSession session = mImService.createFileTransferSession(chatId,
                fileTransferId, contact, file, fileIcon, timestamp, ftProtocol);
        OneToOneFileTransferImpl oneToOneFileTransfer = getOrCreateOneToOneFileTransfer(fileTransferId);
        session.addListener(oneToOneFileTransfer);
        session.startSession();
    }

    /**
     * Returns true if it is possible to initiate file transfer to the contact specified by the
     * contact parameter, else returns false.
     * 
     * @param contact Remote contact
     * @return boolean
     * @throws RemoteException
     */
    @Override
    public boolean isAllowedToTransferFile(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        try {
            Capabilities remoteCapabilities = mContactManager.getContactCapabilities(contact);
            if (remoteCapabilities == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot transfer file as the capabilities of contact " + contact
                            + " are not known.");
                }
                return false;
            }
            FileTransferProtocol protocol = getFileTransferProtocolForOneToOneFileTransfer(contact);
            if (protocol == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot transfer file as no valid file transfer protocol could be determined.");
                }
                return false;
            }
            MessagingMode mode = mRcsSettings.getMessagingMode();
            switch (mode) {
                case INTEGRATED:
                case SEAMLESS:
                    if ((FileTransferProtocol.MSRP == protocol && mRcsSettings.isFtAlwaysOn())
                            || (FileTransferProtocol.HTTP == protocol && mRcsSettings
                                    .isFtHttpCapAlwaysOn())) {
                        break;
                    }
                    if (!mImService.isCapabilitiesValid(remoteCapabilities)) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Cannot transfer file as the cached capabilities of contact "
                                    + contact
                                    + " are not valid anymore for one-to-one communication.");
                        }
                        return false;
                    }
                    break;
                default:
                    break;
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
     * Transfers a file to a contact. The parameter file contains the URI of the file to be
     * transferred (for a local or a remote file). The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI. If the
     * format of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact
     * @param file URI of file to transfer
     * @param attachFileIcon true if the stack must try to attach fileIcon
     * @return FileTransfer
     * @throws RemoteException
     */
    public IFileTransfer transferFile(final ContactId contact, Uri file, boolean attachFileIcon)
            throws RemoteException {
        return transferFile2(contact, file, Disposition.ATTACH.toInt(), attachFileIcon);
    }

    /**
     * Transfers a file to a contact. The parameter file contains the URI of the file to be
     * transferred (for a local or a remote file). The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI. If the
     * format of the contact is not supported an exception is thrown.
     *
     * @param contact Contact
     * @param file URI of file to transfer
     * @param disposition File disposition
     * @param attachFileIcon true if the stack must try to attach fileIcon
     * @return FileTransfer
     * @throws RemoteException
     */
    public IFileTransfer transferFile2(final ContactId contact, Uri file, int disposition,
            boolean attachFileIcon) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (file == null) {
            throw new ServerApiIllegalArgumentException("file must not be null!");
        }
        if (!FileUtils.isReadFromUriPossible(mCtx, file)) {
            throw new ServerApiIllegalArgumentException("file '" + file
                    + "' must refer to a file that exists and that is readable by stack!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Transfer file " + file + " to " + contact + " (fileIcon="
                    + attachFileIcon + ")");
        }
        try {
            Uri localFile = FileUtils.createCopyOfSentFile(file, mRcsSettings);
            FileDescription fileDescription = FileFactory.getFactory()
                    .getFileDescription(localFile);
            String mime = FileUtils.getMimeType(localFile);
            MmContent fileIconContent = null;
            final MmContent content = ContentManager.createMmContent(localFile, mime,
                    fileDescription.getSize(), fileDescription.getName());
            if (Disposition.RENDER == Disposition.valueOf(disposition)) {
                content.setPlayable(true);
            }
            if (mRcsSettings.isCpmMsgTech()) {
                content.setDeliveryMsgId(IdGenerator.generateMessageID());
            }
            final String fileTransferId = IdGenerator.generateMessageID();
            //TODO
            if (attachFileIcon && MimeManager.isImageType(content.getEncoding())) {
                fileIconContent = FileTransferUtils.createFileicon(localFile, fileTransferId,
                        mRcsSettings);
            }
            String chatId = mMessagingLog.getOrCreateCpmChatId();
            final long timestamp = System.currentTimeMillis();
            /* For outgoing file transfer, timestampSent = timestamp */
            final long timestampSent = timestamp;
            /* Always insert with State QUEUED */
            addOutgoingOneToOneFileTransfer(fileTransferId, chatId, contact, content,
                    fileIconContent, State.QUEUED, timestamp, timestampSent);

            OneToOneFileTransferImpl oneToOneFileTransfer = getOrCreateOneToOneFileTransfer(fileTransferId);
            mImService.tryToDequeueFileTransfers();
            return oneToOneFileTransfer;

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

    public FileTransferProtocol getFileTransferProtocolForOneToManyFileTransfer() {
        Capabilities myCapabilities = mRcsSettings.getMyCapabilities();
        boolean ftMsrpSupportedforSelf = myCapabilities.isFileTransferMsrpSupported();
        boolean ftHttpSupportedforSelf = myCapabilities.isFileTransferHttpSupported();
        if (ftMsrpSupportedforSelf && ftHttpSupportedforSelf) {
            return mRcsSettings.getFtProtocol();
        } else if (ftMsrpSupportedforSelf) {
            return FileTransferProtocol.MSRP;
        } else if (ftHttpSupportedforSelf) {
            return FileTransferProtocol.HTTP;
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("There are are no available capabilities : FTMsrp(Self)"
                        + ftMsrpSupportedforSelf + " FTHttp(Self)" + ftHttpSupportedforSelf);
            }
            return null;
        }
    }

    /**
     * Dequeue one-to-many file transfer
     *
     * @param chatId the chat ID
     * @param fileTransferId the file transfer ID
     * @param contacts
     * @param content the file content
     * @param fileIcon the file icon content
     * @param deliveryMsgId Delivery msgId of the file transfer
     * @throws PayloadException
     * @throws NetworkException
     * @throws SessionNotEstablishedException
     */
    public void dequeueOneToManyFileTransfer(String chatId, String fileTransferId,
            Set<ContactId> contacts, MmContent content, MmContent fileIcon, String deliveryMsgId) {
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        FileTransferProtocol ftProtocol = getFileTransferProtocolForOneToManyFileTransfer();
        if (ftProtocol == null) {
            throw new ServerApiGenericException(
                    "No valid file transfer protocol could be determined for dequeueing one-to-many file with fileTransferId '"
                            + fileTransferId + "'!");
        }
        FileSharingSession session = mImService.createOneToManyFileTransferSession(chatId,
                fileTransferId, contacts, content, fileIcon, timestamp, ftProtocol);
        OneToManyFileTransferImpl groupFileTransfer = getOrCreateOneToManyFileTransfer(contacts,
                fileTransferId);
        session.addListener(groupFileTransfer);
        setGroupFileTransferStateAndTimestamp(fileTransferId, chatId, State.INITIATING, timestamp,
                timestampSent);
        session.startSession();
    }

    /**
     * One-to-many file re-send operation initiated
     *
     * @param chatId the chat Id
     * @param contacts
     * @param file the file
     * @param fileIcon the file icon
     * @param fileTransferId the file transfer ID
     */
    /* package private */void resendOneToManyFile(String chatId, Set<ContactId> contacts,
            MmContent file, MmContent fileIcon, String fileTransferId) {
        /* Set new timestamp for the resend file */
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        if (!ServerApiUtils.isImsConnected()) {
            /*
             * If the IMS is NOT connected at this time then re-queue transfer.
             */
            setOneToManyFileTransferStateAndTimestamp(fileTransferId, chatId, State.QUEUED, timestamp,
                    timestampSent);
            return;
        }
        if (!mImService.isFileTransferSessionAvailable()
                || mImService.isMaxConcurrentOutgoingFileTransfersReached()) {
            if (sLogger.isActivated()) {
                sLogger.debug("The max number of file transfer sessions is achieved: re-queue the file transfer with Id "
                        .concat(fileTransferId));
            }
            setOneToManyFileTransferStateAndTimestamp(fileTransferId, chatId, State.QUEUED, timestamp,
                    timestampSent);
            return;
        }

        FileTransferProtocol ftProtocol = getFileTransferProtocolForOneToManyFileTransfer();
        if (ftProtocol == null) {
            throw new ServerApiGenericException(
                    "No valid file transfer protocol could be determined for resending file with Id '"
                            + fileTransferId + "'!");
        }
        final FileSharingSession session = mImService.createOneToManyFileTransferSession(chatId,
                fileTransferId, contacts, file, fileIcon, timestamp, ftProtocol);
        OneToManyFileTransferImpl massFileTransfer = getOrCreateOneToManyFileTransfer(contacts,
                fileTransferId);
        session.addListener(massFileTransfer);
        session.startSession();
        setOneToManyFileTransferStateAndTimestamp(fileTransferId, chatId, State.INITIATING,
                timestamp, timestampSent);
    }

    @Override
    public boolean isAllowedToTransferFileToMany(List<ContactId> contacts) throws RemoteException {
        for (ContactId contact : contacts) {
            if (!isAllowedToTransferFile(contact)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public IFileTransfer transferFileToMany(List<ContactId> contacts, Uri file,
            boolean attachFileIcon) throws RemoteException {
        return transferFileToMany2(contacts, file, Disposition.ATTACH.toInt(), attachFileIcon);
    }

    @Override
    public IFileTransfer transferFileToMany2(List<ContactId> contacts, Uri file, int disposition,
            boolean attachFileIcon) throws RemoteException {
        if (contacts == null) {
            throw new ServerApiIllegalArgumentException("contacts must not be null!");
        }
        if (file == null) {
            throw new ServerApiIllegalArgumentException("file must not be null!");
        }
        if (!FileUtils.isReadFromUriPossible(mCtx, file)) {
            throw new ServerApiIllegalArgumentException("file '" + file
                    + "' must refer to a file that exists and that is readable by stack!");
        }
        try {
            Uri localFile = FileUtils.createCopyOfSentFile(file, mRcsSettings);
            FileDescription fileDescription = FileFactory.getFactory()
                    .getFileDescription(localFile);
            String mime = FileUtils.getMimeType(localFile);
            MmContent fileIconContent = null;
            final MmContent content = ContentManager.createMmContent(localFile, mime,
                    fileDescription.getSize(), fileDescription.getName());
            if (Disposition.RENDER == Disposition.valueOf(disposition)) {
                content.setPlayable(true);
            }
            if (mRcsSettings.isCpmMsgTech()) {
                content.setDeliveryMsgId(IdGenerator.generateMessageID());
            }
            final String fileTransferId = IdGenerator.generateMessageID();
            if (attachFileIcon
                    && (MimeManager.isImageType(content.getEncoding()) || MimeManager
                            .isVideoType(content.getEncoding()))) {
                fileIconContent = FileTransferUtils.createFileicon(localFile, fileTransferId,
                        mRcsSettings);
            }

            String chatId = mMessagingLog.getOrCreateCpmChatId();
            final long timestamp = System.currentTimeMillis();
            /* For outgoing file transfer, timestampSent = timestamp */
            final long timestampSent = timestamp;
            /* Always insert with State QUEUED */
            addOutgoingOneToManyFileTransfer(fileTransferId, chatId, new HashSet<>(contacts),
                    content, fileIconContent, State.QUEUED, timestamp, timestampSent);

            OneToManyFileTransferImpl massFileTransfer = getOrCreateOneToManyFileTransfer(new HashSet<>(
                    contacts), fileTransferId);
            mImService.tryToDequeueFileTransfers();
            return massFileTransfer;

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

    public FileTransferProtocol getFileTransferProtocolForGroupFileTransfer() {
        Capabilities myCapabilities = mRcsSettings.getMyCapabilities();
        boolean ftMsrpSupportedforSelf = myCapabilities.isFileTransferMsrpSupported();
        boolean ftHttpSupportedforSelf = myCapabilities.isFileTransferHttpSupported();
        if (ftMsrpSupportedforSelf && ftHttpSupportedforSelf) {
            return mRcsSettings.getFtProtocol();
        } else if (ftMsrpSupportedforSelf) {
            return FileTransferProtocol.MSRP;
        } else if (ftHttpSupportedforSelf) {
            return FileTransferProtocol.HTTP;
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("There are are no available capabilities : FTMsrp(Self)"
                        + ftMsrpSupportedforSelf + " FTHttp(Self)" + ftHttpSupportedforSelf);
            }
            return null;
        }
    }

    /**
     * Dequeue group file transfer
     *
     * @param chatId the chat ID
     * @param fileTransferId the file transfer ID
     * @param content the file content
     * @param fileIcon the file icon content
     * @param deliveryMsgId Delivery msgId of the file transfer
     * @throws PayloadException
     * @throws NetworkException
     * @throws SessionNotEstablishedException
     */
    public void dequeueGroupFileTransfer(String chatId, String fileTransferId, MmContent content,
            MmContent fileIcon, String deliveryMsgId) throws PayloadException, NetworkException,
            SessionNotEstablishedException {
        GroupChatSession groupChatSession = mImService.getGroupChatSession(chatId);
        if (groupChatSession == null) {
            mImService.rejoinGroupChatAsPartOfSendOperation(chatId);
        } else if (groupChatSession.isMediaEstablished()) {
            long timestamp = System.currentTimeMillis();
            /* For outgoing file transfer, timestampSent = timestamp */
            long timestampSent = timestamp;
            FileTransferProtocol ftProtocol = getFileTransferProtocolForGroupFileTransfer();
            if (ftProtocol == null) {
                throw new ServerApiGenericException(
                        "No valid file transfer protocol could be determined for dequeueing group file with fileTransferId '"
                                + fileTransferId + "'!");
            }
            FileSharingSession session = mImService.createGroupFileTransferSession(fileTransferId,
                    content, fileIcon, chatId, timestamp, ftProtocol);
            GroupFileTransferImpl groupFileTransfer = getOrCreateGroupFileTransfer(chatId,
                    fileTransferId);
            session.addListener(groupFileTransfer);
            setGroupFileTransferStateAndTimestamp(fileTransferId, chatId, State.INITIATING,
                    timestamp, timestampSent);
            session.startSession();
        } else if (groupChatSession.isInitiatedByRemote()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Group chat session with chatId '" + chatId
                        + "' is pending for acceptance, accept it.");
            }
            groupChatSession.acceptSession();
        } else {
            throw new SessionNotEstablishedException(
                    "The existing group chat session with chatId '" + chatId
                            + "' is not established right now!");
        }
    }

    /**
     * Group file re-send operation initiated
     *
         * @param chatId the chat Id
         * @param file the file
         * @param fileIcon the file icon
         * @param fileTransferId the file transfer ID
         * @param deliveryMsgId Delivery msgId of the file transfer
         */
    /* package private */void resendGroupFile(String chatId, MmContent file, MmContent fileIcon,
            String fileTransferId, String deliveryMsgId) {
        /* Set new timestamp for the resend file */
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        if (!ServerApiUtils.isImsConnected()) {
            /*
             * If the IMS is NOT connected at this time then re-queue transfer.
             */
            setGroupFileTransferStateAndTimestamp(fileTransferId, chatId, State.QUEUED, timestamp,
                    timestampSent);
            return;
        }
        if (!mImService.isFileTransferSessionAvailable()
                || mImService.isMaxConcurrentOutgoingFileTransfersReached()) {
            if (sLogger.isActivated()) {
                sLogger.debug("The max number of file transfer sessions is achieved: re-queue the file transfer with Id "
                        .concat(fileTransferId));
            }
            setGroupFileTransferStateAndTimestamp(fileTransferId, chatId, State.QUEUED, timestamp,
                    timestampSent);
            return;
        }

        FileTransferProtocol ftProtocol = getFileTransferProtocolForGroupFileTransfer();
        if (ftProtocol == null) {
            throw new ServerApiGenericException(
                    "No valid file transfer protocol could be determined for resending file with Id '"
                            + fileTransferId + "'!");
        }
        final FileSharingSession session = mImService.createGroupFileTransferSession(
                fileTransferId, file, fileIcon, chatId, timestamp, ftProtocol);
        GroupFileTransferImpl groupFileTransfer = getOrCreateGroupFileTransfer(chatId,
                fileTransferId);
        session.addListener(groupFileTransfer);
        session.startSession();
        setGroupFileTransferStateAndTimestamp(fileTransferId, chatId, State.INITIATING, timestamp,
                timestampSent);
    }

    /**
     * Returns true if it is possible to initiate file transfer to the group chat specified by the
     * chatId parameter, else returns false.
     * 
     * @param chatId the chat ID
     * @return boolean
     * @throws RemoteException
     */
    public boolean isAllowedToTransferFileToGroupChat(String chatId) throws RemoteException {
        if (TextUtils.isEmpty(chatId)) {
            throw new ServerApiIllegalArgumentException("chatId must not be null or empty!");
        }
        try {
            if (!mRcsSettings.isGroupChatActivated()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot transfer file to group chat with group chat Id '"
                            + chatId + "' as group chat feature is not supported.");
                }
                return false;
            }
            if (!mRcsSettings.getMyCapabilities().isFileTransferHttpSupported()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot transfer file to group chat with group chat Id '"
                            + chatId + "' as FT over HTTP capabilities are not supported for self.");
                }
                return false;
            }
            if (mChatService.isGroupChatAbandoned(chatId)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot transfer file to group chat with group chat Id '"
                            + chatId
                            + "' as the group chat is abandoned and can no more be used to send or receive messages.");
                }
                return false;
            }
            GroupChatSession session = mImService.getGroupChatSession(chatId);
            if (session == null) {
                GroupChatInfo groupChat = mMessagingLog.getGroupChatInfo(chatId);
                if (groupChat == null) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Cannot transfer file to group chat with group chat Id '"
                                + chatId + "' as the group chat does not exist in DB.");
                    }
                    return false;
                }
                if (groupChat.getRejoinId() == null) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Cannot transfer file to group chat with group chat Id '"
                                + chatId
                                + "' as there is no ongoing session with corresponding chatId and there exists no rejoinId to rejoin the group chat.");
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

    /**
     * Dequeue group file transfer
     * 
     * @param fileTransferId the file transfer ID
     * @param content the file content
     * @param fileIcon the file icon content
     * @param chatId the chat ID
     * @throws PayloadException
     * @throws NetworkException
     * @throws SessionNotEstablishedException
     */
    public void dequeueGroupFileTransfer(String chatId, String fileTransferId, MmContent content,
            MmContent fileIcon) throws PayloadException, NetworkException,
            SessionNotEstablishedException {
        GroupChatSession groupChatSession = mImService.getGroupChatSession(chatId);
        if (groupChatSession == null) {
            mImService.rejoinGroupChatAsPartOfSendOperation(chatId);
        } else if (groupChatSession.isMediaEstablished()) {
            long timestamp = System.currentTimeMillis();
            /* For outgoing file transfer, timestampSent = timestamp */
            long timestampSent = timestamp;
            FileTransferProtocol ftProtocol = getFileTransferProtocolForGroupFileTransfer();
            if (ftProtocol == null) {
                throw new ServerApiGenericException(
                        "No valid file transfer protocol could be determined for resending file with Id '"
                                + fileTransferId + "'!");
            }
            FileSharingSession session = mImService.createGroupFileTransferSession(fileTransferId,
                    content, fileIcon, chatId, timestamp, ftProtocol);
            GroupFileTransferImpl groupFileTransfer = getOrCreateGroupFileTransfer(chatId,
                    fileTransferId);
            session.addListener(groupFileTransfer);
            setGroupFileTransferStateAndTimestamp(fileTransferId, chatId, State.INITIATING,
                    timestamp, timestampSent);
            session.startSession();
        } else if (groupChatSession.isInitiatedByRemote()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Group chat session with chatId '" + chatId
                        + "' is pending for acceptance, accept it.");
            }
            groupChatSession.acceptSession();
        } else {
            throw new SessionNotEstablishedException(
                    "The existing group chat session with chatId '" + chatId
                            + "' is not established right now!");
        }
    }

    /**
     * Transfers a file to participants. The parameter file contains the URI of the file to be
     * transferred (for a local or a remote file).
     * 
     * @param chatId ChatId of group chat
     * @param file Uri of file to transfer
     * @param attachFileIcon true if the stack must try to attach fileIcon
     * @return FileTransfer
     * @throws RemoteException
     */
    @Override
    public IFileTransfer transferFileToGroupChat(final String chatId, Uri file,
            boolean attachFileIcon) throws RemoteException {
        return transferFileToGroupChat2(chatId, file, Disposition.ATTACH.toInt(), attachFileIcon);
    }

    /**
     * Transfers a file to participants. The parameter file contains the URI of the file to be
     * transferred (for a local or a remote file).
     *
     * @param chatId ChatId of group chat
     * @param file Uri of file to transfer
     * @param disposition File disposition
     * @param attachFileIcon true if the stack must try to attach fileIcon
     * @return FileTransfer
     * @throws RemoteException
     */
    @Override
    public IFileTransfer transferFileToGroupChat2(final String chatId, Uri file, int disposition,
            boolean attachFileIcon) throws RemoteException {
        if (TextUtils.isEmpty(chatId)) {
            throw new ServerApiIllegalArgumentException("chatId must not be null or empty!");
        }
        if (file == null) {
            throw new ServerApiIllegalArgumentException("file must not be null!");
        }
        if (!FileUtils.isReadFromUriPossible(mCtx, file)) {
            throw new ServerApiIllegalArgumentException("file '" + file.toString()
                    + "' must refer to a file that exists and that is readable by stack!");
        }
        if (!isAllowedToTransferFileToGroupChat(chatId)) {
            throw new ServerApiPermissionDeniedException(
                    "No sufficient capabilities to transfer file to group chat!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("sendFile (file=" + file + ") (fileicon=" + attachFileIcon + ")");
        }
        try {
            Uri localFile = FileUtils.createCopyOfSentFile(file, mRcsSettings);
            FileDescription fileDescription = FileFactory.getFactory()
                    .getFileDescription(localFile);
            String mime = FileUtils.getMimeType(localFile);
            final MmContent content = ContentManager.createMmContent(localFile, mime,
                    fileDescription.getSize(), fileDescription.getName());
            if (Disposition.valueOf(disposition) == Disposition.RENDER) {
                content.setPlayable(true);
            }
            if (mRcsSettings.isCpmMsgTech()) {
                content.setDeliveryMsgId(IdGenerator.generateMessageID());
            }
            final String fileTransferId = IdGenerator.generateMessageID();
            MmContent fileIconContent = null;
            if (attachFileIcon && MimeManager.isImageType(content.getEncoding())) {
                fileIconContent = FileTransferUtils.createFileicon(content.getUri(),
                        fileTransferId, mRcsSettings);
            }
            final long timestamp = System.currentTimeMillis();
            /* For outgoing file transfer, timestampSent = timestamp */
            /* Always insert file transfer with status QUEUED */
            addOutgoingGroupFileTransfer(fileTransferId, chatId, content, fileIconContent,
                    State.QUEUED, timestamp, timestamp);
            if (!mChatService.isGroupChatActive(chatId)) {
                /*
                 * Set inactive group chat as active as it now has a queued entry that has to be
                 * dequeued after rejoining to the group chat on regaining IMS connection.
                 */
                mChatService.setGroupChatStateAndReasonCode(chatId, ChatLog.GroupChat.State.STARTED,
                        ChatLog.GroupChat.ReasonCode.UNSPECIFIED);
            }

            GroupFileTransferImpl groupFileTransfer = getOrCreateGroupFileTransfer(chatId,
                    fileTransferId);
            mImService.tryToDequeueGroupChatMessagesAndGroupFileTransfers(chatId);
            return groupFileTransfer;

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
     * Returns a current file transfer from its unique ID
     * 
     * @param transferId the file transfer ID
     * @return File transfer
     * @throws RemoteException
     */
    @Override
    public IFileTransfer getFileTransfer(String transferId) throws RemoteException {
        if (TextUtils.isEmpty(transferId)) {
            throw new ServerApiIllegalArgumentException("transferId must not be null or empty!");
        }
        try {
            IFileTransfer fileTransfer = mOneToOneFileTransferCache.get(transferId);
            if (fileTransfer != null) {
                return fileTransfer;
            }
            fileTransfer = mOneToManyFileTransferCache.get(transferId);
            if (fileTransfer != null) {
                return fileTransfer;
            }
            fileTransfer = mGroupFileTransferCache.get(transferId);
            if (fileTransfer != null) {
                return fileTransfer;
            }
            // FIXME: This is not fully thread safe
            if (mMessagingLog.isGroupFileTransfer(transferId)) {
                String chatId = mMessagingLog.getFileTransferChatId(transferId);
                return getOrCreateGroupFileTransfer(chatId, transferId);
            }
            // FIXME: This is not entirely correct... If there is no o2o ft in db matching this
            // transferId a ServerApiIllegalArgumentException should be thrown from here.
            return getOrCreateOneToOneFileTransfer(transferId);

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
     * Adds a listener on file transfer events
     * 
     * @param listener OneToOne file transfer listener
     * @throws RemoteException
     */
    @Override
    public void addEventListener2(IOneToOneFileTransferListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        try {
            synchronized (mLock) {
                mOneToOneFileTransferBroadcaster.addOneToOneFileTransferListener(listener);
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
     * Removes a listener on file transfer events
     * 
     * @param listener OneToOne file transfer listener
     * @throws RemoteException
     */
    @Override
    public void removeEventListener2(IOneToOneFileTransferListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        try {
            synchronized (mLock) {
                mOneToOneFileTransferBroadcaster.removeOneToOneFileTransferListener(listener);
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

    @Override
    public void addEventListener4(IOneToManyFileTransferListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        try {
            synchronized (mLock) {
                mOneToManyFileTransferBroadcaster.addOneToManyFileTransferListener(listener);
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

    @Override
    public void removeEventListener4(IOneToManyFileTransferListener listener)
            throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        try {
            synchronized (mLock) {
                mOneToManyFileTransferBroadcaster.removeOneToManyFileTransferListener(listener);
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
     * Adds a listener on group file transfer events
     * 
     * @param listener Group file transfer listener
     * @throws RemoteException
     */
    @Override
    public void addEventListener3(IGroupFileTransferListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        try {
            synchronized (mLock) {
                mGroupFileTransferBroadcaster.addGroupFileTransferListener(listener);
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
     * Removes a listener on group file transfer events
     * 
     * @param listener Group file transfer listener
     * @throws RemoteException
     */
    @Override
    public void removeEventListener3(IGroupFileTransferListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        try {
            synchronized (mLock) {
                mGroupFileTransferBroadcaster.removeGroupFileTransferListener(listener);
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
     * File Transfer delivery status. In FToHTTP, Delivered status is done just after download
     * information are received by the terminating, and Displayed status is done when the file is
     * downloaded. In FToMSRP, the two status are directly done just after MSRP transfer complete.
     * 
     * @param imdn Imdn document
     * @param contact contact who received file
     */
    public void receiveOneToOneFileDeliveryStatus(ImdnDocument imdn, ContactId contact) {
        ImdnDocument.DeliveryStatus status = imdn.getStatus();
        long timestamp = imdn.getDateTime();
        /* Note: File transfer ID always corresponds to message ID in the imdn pay-load */
        String fileTransferId = imdn.getMsgId();
        switch (status) {
            case DELIVERED:
                mImService.getDeliveryExpirationManager()
                        .cancelDeliveryTimeoutAlarm(fileTransferId);
                if (mMessagingLog.setFileTransferDelivered(fileTransferId, timestamp)) {
                    mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
                            State.DELIVERED, ReasonCode.UNSPECIFIED);
                }

                break;
            case DISPLAYED:
                mImService.getDeliveryExpirationManager()
                        .cancelDeliveryTimeoutAlarm(fileTransferId);
                if (mMessagingLog.setFileTransferDisplayed(fileTransferId, timestamp)) {
                    mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
                            State.DISPLAYED, ReasonCode.UNSPECIFIED);
                }

                break;
            case ERROR:
            case FAILED:
            case FORBIDDEN:
                ReasonCode reasonCode = imdnToFileTransferFailedReasonCode(imdn);

                if (mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId, State.FAILED,
                        reasonCode)) {
                    mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
                            State.FAILED, reasonCode);
                }
                break;
        }
    }

    public void receiveOneToManyFileDeliveryStatus(String chatId, ImdnDocument imdn,
            ContactId contact) {
        receiveGroupFileDeliveryStatus(chatId, imdn, contact);
    }

    private void setGroupFileDeliveryStatusDelivered(String chatId, String fileTransferId,
            ContactId contact, long timestampDelivered) {
        if (mMessagingLog.setGroupChatDeliveryInfoDelivered(chatId, contact, fileTransferId,
                timestampDelivered)) {
            mGroupFileTransferBroadcaster.broadcastDeliveryInfoChanged(chatId, contact,
                    fileTransferId, GroupDeliveryInfo.Status.DELIVERED,
                    GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
            if (mMessagingLog.isDeliveredToAllRecipients(fileTransferId)) {
                if (mMessagingLog.setFileTransferDelivered(fileTransferId, timestampDelivered)) {
                    mGroupFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId,
                            State.DELIVERED, ReasonCode.UNSPECIFIED);
                }
            }
        }
    }

    private void setGroupFileDeliveryStatusDisplayed(String chatId, String fileTransferId,
            ContactId contact, long timestampDisplayed) {
        if (mMessagingLog.setGroupChatDeliveryInfoDisplayed(chatId, contact, fileTransferId,
                timestampDisplayed)) {
            mGroupFileTransferBroadcaster.broadcastDeliveryInfoChanged(chatId, contact,
                    fileTransferId, GroupDeliveryInfo.Status.DISPLAYED,
                    GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
            if (mMessagingLog.isDisplayedByAllRecipients(fileTransferId)) {
                if (mMessagingLog.setFileTransferDisplayed(fileTransferId, timestampDisplayed)) {
                    mGroupFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId,
                            State.DISPLAYED, ReasonCode.UNSPECIFIED);
                }
            }
        }
    }

    private void setGroupFileDeliveryStatusFailed(String chatId, String fileTransferId,
            ContactId contact, ReasonCode reasonCode) {
        if (ReasonCode.FAILED_DELIVERY == reasonCode) {
            if (!mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(chatId, contact,
                    fileTransferId, GroupDeliveryInfo.Status.FAILED,
                    GroupDeliveryInfo.ReasonCode.FAILED_DELIVERY)) {
                /* Add entry with delivered and displayed timestamps set to 0. */
                mMessagingLog.addGroupChatDeliveryInfoEntry(chatId, contact, fileTransferId,
                        GroupDeliveryInfo.Status.FAILED,
                        GroupDeliveryInfo.ReasonCode.FAILED_DELIVERY, 0, 0);
            }
            mGroupFileTransferBroadcaster.broadcastDeliveryInfoChanged(chatId, contact,
                    fileTransferId, GroupDeliveryInfo.Status.FAILED,
                    GroupDeliveryInfo.ReasonCode.FAILED_DELIVERY);
            return;
        }
        if (!mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(chatId, contact,
                fileTransferId, GroupDeliveryInfo.Status.FAILED,
                GroupDeliveryInfo.ReasonCode.FAILED_DISPLAY)) {
            /* Add entry with delivered and displayed timestamps set to 0. */
            mMessagingLog.addGroupChatDeliveryInfoEntry(chatId, contact, fileTransferId,
                    GroupDeliveryInfo.Status.FAILED, GroupDeliveryInfo.ReasonCode.FAILED_DISPLAY,
                    0, 0);
        }
        mGroupFileTransferBroadcaster.broadcastDeliveryInfoChanged(chatId, contact, fileTransferId,
                GroupDeliveryInfo.Status.FAILED, GroupDeliveryInfo.ReasonCode.FAILED_DISPLAY);
    }

    /**
     * Handles group file transfer delivery status.
     *
     * @param chatId Chat ID
     * @param imdn Imdn Document
     * @param contact Contact ID
     */
    public void receiveGroupFileDeliveryStatus(String chatId, ImdnDocument imdn, ContactId contact) {
        ImdnDocument.DeliveryStatus status = imdn.getStatus();
        String msgId = imdn.getMsgId();
        long timestamp = imdn.getDateTime();
        if (sLogger.isActivated()) {
            sLogger.info("Handling group file delivery status; contact=" + contact + ", msgId="
                    + msgId + ", status=" + status + ", notificationType="
                    + imdn.getNotificationType());
        }
        switch (status) {
            case DELIVERED:
                setGroupFileDeliveryStatusDelivered(chatId, msgId, contact, timestamp);
                break;
            case DISPLAYED:
                setGroupFileDeliveryStatusDisplayed(chatId, msgId, contact, timestamp);
                break;
            case ERROR:
            case FAILED:
            case FORBIDDEN:
                ReasonCode reasonCode = imdnToFileTransferFailedReasonCode(imdn);
                setGroupFileDeliveryStatusFailed(chatId, msgId, contact, reasonCode);
                break;
        }
    }

    /**
     * Returns service version
     * 
     * @return Version
     * @see VERSION_CODES
     */
    public int getServiceVersion() {
        return RcsService.Build.API_VERSION;
    }

    /**
     * Resume an outgoing HTTP file transfer
     * 
     * @param session File transfer session
     * @param isGroup is group file transfer
     */
    public void resumeOutgoingFileTransfer(FileSharingSession session, boolean isGroup) {
        if (sLogger.isActivated()) {
            sLogger.info("Resume outgoing file transfer from " + session.getRemoteContact());
        }
        if (isGroup) {
            GroupFileTransferImpl groupFileTransfer = getOrCreateGroupFileTransfer(
                    session.getContributionID(), session.getFileTransferId());
            session.addListener(groupFileTransfer);
        } else {
            OneToOneFileTransferImpl oneToOneFileTransfer = getOrCreateOneToOneFileTransfer(session
                    .getFileTransferId());
            session.addListener(oneToOneFileTransfer);
        }
    }

    /**
     * Resume an incoming HTTP file transfer
     * 
     * @param session File transfer session
     * @param isGroup is group file transfer
     */
    public void resumeIncomingFileTransfer(FileSharingSession session, boolean isGroup) {
        if (sLogger.isActivated()) {
            sLogger.info("Resume incoming file transfer from " + session.getRemoteContact());
        }
        if (isGroup) {
            GroupFileTransferImpl groupFileTransfer = getOrCreateGroupFileTransfer(
                    session.getContributionID(), session.getFileTransferId());
            session.addListener(groupFileTransfer);
        } else {
            OneToOneFileTransferImpl oneToOneFileTransfer = getOrCreateOneToOneFileTransfer(session
                    .getFileTransferId());
            session.addListener(oneToOneFileTransfer);
        }
    }

    /**
     * Mark a received file transfer as read (i.e. the invitation or the file has been displayed in
     * the UI).
     * 
     * @param transferId File transfer ID
     * @throws RemoteException
     */
    @Override
    public void markFileTransferAsRead(final String transferId) throws RemoteException {
        if (TextUtils.isEmpty(transferId)) {
            throw new ServerApiIllegalArgumentException("transferId must not be null or empty!");
        }
        try {
            /* No notification type corresponds currently to mark as read */
            mMessagingLog.markFileTransferAsRead(transferId, System.currentTimeMillis());

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
     * Deletes all one to one file transfer history and abort/reject any associated ongoing session
     * if such exists.
     * 
     * @throws RemoteException
     */
    public void deleteOneToOneFileTransfers() throws RemoteException {
        mImService.tryToDeleteOneToOneFileTransfers();
    }

    /**
     * Deletes all one to many file transfer from history and abort/reject any associated ongoing
     * session if such exists.
     *
     * @throws RemoteException
     */
    public void deleteOneToManyFileTransfers() throws RemoteException {
        mImService.tryToDeleteOneToManyFileTransfers();
    }

    /**
     * Deletes all group file transfer from history and abort/reject any associated ongoing session
     * if such exists.
     * 
     * @throws RemoteException
     */
    public void deleteGroupFileTransfers() throws RemoteException {
        mImService.tryToDeleteGroupFileTransfers();
    }

    /**
     * Deletes file transfer corresponding to a given one to one chat specified by contact from
     * history and abort/reject any associated ongoing session if such exists.
     * 
     * @param contact the contact
     * @throws RemoteException
     */
    public void deleteOneToOneFileTransfers2(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        mImService.tryToDeleteFileTransfers(contact);
    }

    /**
     * Deletes file transfer corresponding to a given one to many chat specified by chat id from
     * history and abort/reject any associated ongoing session if such exists.
     *
     * @param chatId the chat ID
     * @throws RemoteException
     */
    public void deleteOneToManyFileTransfers2(String chatId) throws RemoteException {
        if (TextUtils.isEmpty(chatId)) {
            throw new ServerApiIllegalArgumentException("chatId must not be null or empty!");
        }
        mImService.tryToDeleteOneToManyFileTransfers(chatId);
    }

    /**
     * Deletes file transfer corresponding to a given group chat specified by chat id from history
     * and abort/reject any associated ongoing session if such exists.
     * 
     * @param chatId the chat ID
     * @throws RemoteException
     */
    public void deleteGroupFileTransfers2(String chatId) throws RemoteException {
        if (TextUtils.isEmpty(chatId)) {
            throw new ServerApiIllegalArgumentException("chatId must not be null or empty!");
        }
        mImService.tryToDeleteGroupFileTransfers(chatId);
    }

    /**
     * Deletes a file transfer by its unique id from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @param transferId the file transfer ID
     * @throws RemoteException
     */
    public void deleteFileTransfer(String transferId) throws RemoteException {
        if (TextUtils.isEmpty(transferId)) {
            throw new ServerApiIllegalArgumentException("transferId must not be null or empty!");
        }
        mImService.tryToDeleteFileTransfer(transferId);
    }

    /**
     * Disables and clears any delivery expiration for a set of file transfers regardless if the
     * delivery of them has expired already or not.
     * 
     * @param transferIds the file transfer IDs
     * @throws RemoteException
     */
    @Override
    public void clearFileTransferDeliveryExpiration(final List<String> transferIds)
            throws RemoteException {
        if (transferIds == null || transferIds.isEmpty()) {
            throw new ServerApiIllegalArgumentException(
                    "transferId list must not be null or empty!");
        }
        mImService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    for (String transferId : transferIds) {
                        mImService.getDeliveryExpirationManager().cancelDeliveryTimeoutAlarm(
                                transferId);
                    }
                    mMessagingLog.clearFileTransferDeliveryExpiration(transferIds);
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
     * Add and broadcast file transfer invitation rejections
     * 
     * @param contact Contact
     * @param content File content
     * @param fileIcon File content
     * @param reasonCode Reason code
     * @param timestamp Local timestamp when got invitation
     * @param timestampSent Timestamp sent in payload for the file transfer
     */
    public void addFileTransferInvitationRejected(ContactId contact, MmContent content,
            MmContent fileIcon, ReasonCode reasonCode, long timestamp, long timestampSent) {
        String fileTransferId = IdGenerator.generateMessageID();
        String chatId = null;
        // TODO add param fileTransferId get from invite request
        mMessagingLog.addOneToOneFileTransfer(fileTransferId, chatId, contact, Direction.INCOMING, content,
                fileIcon, State.REJECTED, reasonCode, timestamp, timestampSent,
                FileTransferData.UNKNOWN_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION);

        mOneToOneFileTransferBroadcaster.broadcastInvitation(fileTransferId);
    }

    /**
     * Set and broadcast resend file transfer invitation rejections
     * 
     * @param reasonCode Reason code
     * @param timestamp Local timestamp when got invitation
     * @param timestampSent Timestamp sent in payload for the file transfer
     */
    public void setResendFileTransferInvitationRejected(String fileTransferId,
            ReasonCode reasonCode, long timestamp, long timestampSent) {
        mMessagingLog.setFileTransferStateAndTimestamp(fileTransferId, State.REJECTED, reasonCode,
                timestamp, timestampSent);

        mOneToOneFileTransferBroadcaster.broadcastInvitation(fileTransferId);
    }

    /**
     * Returns the common service configuration
     * 
     * @return the common service configuration
     */
    public ICommonServiceConfiguration getCommonConfiguration() {
        return new CommonServiceConfigurationImpl(mRcsSettings);
    }

    /**
     * Set one-one file transfer state and reason code
     * 
     * @param fileTransferId the file transfer ID
     * @param contact the contact
     * @param state the file transfer state
     * @param reasonCode the reason code
     */
    public void setOneToOneFileTransferStateAndReasonCode(String fileTransferId, ContactId contact,
            State state, ReasonCode reasonCode) {
        if (mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId, state, reasonCode)) {
            mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId, state,
                    reasonCode);
        }
    }

    /**
     * Set group file transfer state and reason code
     * 
     * @param fileTransferId the file transfer ID
     * @param chatId the chat ID
     * @param state the file transfer state
     * @param reasonCode the reason code
     */
    public void setGroupFileTransferStateAndReasonCode(String fileTransferId, String chatId,
            State state, ReasonCode reasonCode) {
        if (mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId, state, reasonCode)) {
            mGroupFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId, state,
                    reasonCode);
        }
    }

    public void broadcastOneToOneFileTransferDeleted(ContactId contact, Set<String> transferIds) {
        mOneToOneFileTransferBroadcaster.broadcastFileTransferDeleted(contact, transferIds);
    }

    public void broadcastGroupFileTransfersDeleted(String chatId, Set<String> transferIds) {
        mGroupFileTransferBroadcaster.broadcastFileTransfersDeleted(chatId, transferIds);
    }
}
