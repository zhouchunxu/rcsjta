/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.logger.Logger;

import java.util.Set;

public class GroupFileTransferDeleteTask extends DeleteTask.GroupedByChatId {

    private static final Logger sLogger = Logger.getLogger(GroupFileTransferDeleteTask.class
            .getName());

    private static final String SELECTION_ALL_GROUP_FILETRANSFERS = FileTransferData.KEY_CHAT_ID
            + "<>" + FileTransferData.KEY_CONTACT + " OR " + FileTransferData.KEY_CONTACT
            + " IS NULL";

    private static final String SELECTION_FILETRANSFER_BY_CHATID = FileTransferData.KEY_CHAT_ID
            + "=?";

    private static final String SELECTION_GROUPDELIVERY_BY_CHATID = GroupDeliveryInfoData.KEY_CHAT_ID
            + "=?";

    private static final String SELECTION_GROUPDELIVERY_BY_FILEID = GroupDeliveryInfoData.KEY_ID
            + "=?";

    private final FileTransferServiceImpl mFileTransferService;

    private final InstantMessagingService mImService;

    /**
     * Deletion of all group file transfers.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     */
    public GroupFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver) {
        super(contentResolver, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID,
                FileTransferData.KEY_CHAT_ID, SELECTION_ALL_GROUP_FILETRANSFERS);
        mFileTransferService = fileTransferService;
        mImService = imService;
    }

    /**
     * Deletion of all file transfers that belong to the specified group chat.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param chatId the chat id
     */
    public GroupFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver, String chatId) {
        super(contentResolver, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID,
                FileTransferData.KEY_CHAT_ID, SELECTION_FILETRANSFER_BY_CHATID, chatId);
        mFileTransferService = fileTransferService;
        mImService = imService;
    }

    /**
     * Deletion of a specific file transfer.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param chatId the chat id
     * @param transferId the transfer id
     */
    public GroupFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver, String chatId,
            String transferId) {
        super(contentResolver, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID,
                FileTransferData.KEY_CHAT_ID, null, transferId);
        mFileTransferService = fileTransferService;
        mImService = imService;
    }

    @Override
    protected void onRowDelete(String chatId, String transferId) throws PayloadException {
        if (isSingleRowDelete()) {
            mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI,
                    SELECTION_GROUPDELIVERY_BY_FILEID, new String[] {
                        transferId
                    });
        }
        try {
            FileSharingSession session = mImService.getFileSharingSession(transferId);
            if (session != null) {
                session.deleteSession();
            }
        } catch (NetworkException e) {
            /*
             * If network is lost during a delete operation the remaining part of the delete
             * operation (delete from persistent storage) can succeed to 100% anyway since delete
             * can be executed anyway while no network connectivity is present and still succeed.
             */
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        }
        mFileTransferService.ensureThumbnailIsDeleted(transferId);
        mFileTransferService.ensureFileCopyIsDeletedIfExisting(transferId);
        mFileTransferService.removeGroupFileTransfer(transferId);
    }

    @Override
    protected void onCompleted(String chatId, Set<String> transferIds) {
        if (!isSingleRowDelete()) {
            mLocalContentResolver.delete(GroupDeliveryInfoData.CONTENT_URI,
                    SELECTION_GROUPDELIVERY_BY_CHATID, new String[] {
                            chatId
                    });
        }
        mFileTransferService.broadcastGroupFileTransfersDeleted(chatId, transferIds);
    }

}
