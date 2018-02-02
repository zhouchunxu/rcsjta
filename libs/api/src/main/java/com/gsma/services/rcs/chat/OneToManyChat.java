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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsIllegalArgumentException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

/**
 * One-to-Many Chat
 */
public class OneToManyChat {

    /**
     * Chat interface
     */
    private final IOneToManyChat mOneToManyChatInf;

    /**
     * Constructor
     *
     * @param chatIntf Chat interface
     */
    /* package private */OneToManyChat(IOneToManyChat chatIntf) {
        mOneToManyChatInf = chatIntf;
    }

    /**
     * Returns the remote contact
     * 
     * @return ContactId
     * @throws RcsGenericException
     */
    public Set<ContactId> getRemoteContacts() throws RcsGenericException {
        try {
            return null;//mOneToManyChatInf.getRemoteContacts();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to send messages in this one to one chat right now, else
     * return false.
     * 
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToSendMessage() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return mOneToManyChatInf.isAllowedToSendMessage();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a chat message
     * 
     * @param message Message
     * @return ChatMessage
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ChatMessage sendMessage(String message) throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return new ChatMessage(mOneToManyChatInf.sendMessage(message));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc info
     * @return ChatMessage
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ChatMessage sendMessage(Geoloc geoloc) throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return new ChatMessage(mOneToManyChatInf.sendMessage2(geoloc));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends an emoticon message
     *
     * @param emoticon Emoticon info
     * @return ChatMessage
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ChatMessage sendMessage(Emoticon emoticon) throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return new ChatMessage(mOneToManyChatInf.sendMessage3(emoticon));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a cloudFile message
     *
     * @param cloudFile CloudFile info
     * @return ChatMessage
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ChatMessage sendMessage(CloudFile cloudFile) throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return new ChatMessage(mOneToManyChatInf.sendMessage4(cloudFile));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a card message
     *
     * @param card Card info
     * @return ChatMessage
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ChatMessage sendMessage(Card card) throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return new ChatMessage(mOneToManyChatInf.sendMessage5(card));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * open the chat conversation.<br>
     * Note: if it is an incoming pending chat session and the parameter IM SESSION START is 0 then
     * the session is accepted now.
     * 
     * @throws RcsGenericException
     */
    public void openChat() throws RcsGenericException {
        try {
           //FIXME mOneToOneChatInf.openChat();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Resend a message which previously failed.
     * 
     * @param msgId the message ID
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void resendMessage(String msgId) throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            mOneToManyChatInf.resendMessage(msgId);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
