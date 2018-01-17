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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsIllegalArgumentException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsUnsupportedOperationException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.Participant;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Group chat
 * 
 * @author Jean-Marc AUFFRET
 */
public class GroupChat {

    /**
     * Group chat interface
     */
    private final IGroupChat mGroupChatInf;

    /**
     * Constructor
     * 
     * @param chatIntf Group chat interface
     */
    /* package private */GroupChat(IGroupChat chatIntf) {
        mGroupChatInf = chatIntf;
    }

    /**
     * Returns the chat ID
     * 
     * @throws RcsGenericException
     * @return chat Id
     */
    public String getChatId() throws RcsGenericException {
        try {
            return mGroupChatInf.getChatId();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the direction of the group chat
     * 
     * @return Direction
     * @see Direction
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public Direction getDirection() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return Direction.valueOf(mGroupChatInf.getDirection());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the state of the group chat
     * 
     * @return State
     * @see State
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public State getState() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return State.valueOf(mGroupChatInf.getState());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the reason code of the state of the group chat
     * 
     * @return ReasonCode
     * @see ReasonCode
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ReasonCode getReasonCode() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return ReasonCode.valueOf(mGroupChatInf.getReasonCode());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the remote contact
     * 
     * @return ContactId
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ContactId getRemoteContact() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mGroupChatInf.getRemoteContact();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the subject of the group chat
     * 
     * @return String
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public String getSubject() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mGroupChatInf.getSubject();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the map of participants and associated status .
     * 
     * @return Map&lt;ContactId, Status&gt;
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    /*
     * Unchecked cast must be suppressed since AIDL provides a raw Map type that must be cast.
     */
    @SuppressWarnings("unchecked")
    public Map<ContactId, Participant.Status> getParticipants()
            throws RcsPersistentStorageException, RcsGenericException {
        try {
            Map<ContactId, Integer> apiParticipants = mGroupChatInf.getParticipants();
            Map<ContactId, Participant.Status> participants = new HashMap<>();

            for (Map.Entry<ContactId, Integer> apiParticipant : apiParticipants.entrySet()) {
                participants.put(apiParticipant.getKey(),
                        Participant.Status.valueOf(apiParticipant.getValue()));
            }

            return participants;

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the local timestamp of when the group chat invitation was initiated for outgoing
     * group chats or the local timestamp of when the group chat invitation was received for
     * incoming group chat invitations.
     * 
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestamp() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mGroupChatInf.getTimestamp();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the max number of participants in the group chat. This limit is read during the
     * conference event subscription and overrides the provisioning parameter.
     *
     * @return int
     * @throws RcsGenericException
     */
    public int getMaxParticipants() throws RcsGenericException {
        try {
            return mGroupChatInf.getMaxParticipants();

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
            mGroupChatInf.resendMessage(msgId);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to send messages in the group chat right now, else returns
     * false.
     * 
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToSendMessage() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return mGroupChatInf.isAllowedToSendMessage();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a text message to the group
     * 
     * @param text Message
     * @return ChatMessage
     * @throws RcsPermissionDeniedException
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ChatMessage sendMessage(String text) throws RcsPermissionDeniedException,
            RcsPersistentStorageException, RcsGenericException {
        try {
            return new ChatMessage(mGroupChatInf.sendMessage(text));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc info
     * @return ChatMessage
     * @throws RcsPermissionDeniedException
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ChatMessage sendMessage(Geoloc geoloc) throws RcsPermissionDeniedException,
            RcsPersistentStorageException, RcsGenericException {
        try {
            return new ChatMessage(mGroupChatInf.sendMessage2(geoloc));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
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
    public ChatMessage sendMessage(Emoticon emoticon) throws RcsPermissionDeniedException,
            RcsPersistentStorageException, RcsGenericException {
        try {
            return new ChatMessage(mGroupChatInf.sendMessage3(emoticon));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
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
    public ChatMessage sendMessage(CloudFile cloudFile) throws RcsPermissionDeniedException,
            RcsPersistentStorageException, RcsGenericException {
        try {
            return new ChatMessage(mGroupChatInf.sendMessage4(cloudFile));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
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
    public ChatMessage sendMessage(Card card) throws RcsPermissionDeniedException,
            RcsPersistentStorageException, RcsGenericException {
        try {
            return new ChatMessage(mGroupChatInf.sendMessage5(card));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a "courtesy copy" text message
     *
     * @param recipients Set of recipients
     * @param text Message
     * @return ChatMessage
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ChatMessage sendCcMessage(Set<ContactId> recipients, String text)
            throws RcsPermissionDeniedException, RcsPersistentStorageException, RcsGenericException {
        try {
            return new ChatMessage(mGroupChatInf.sendCcMessage(new ArrayList<>(recipients), text));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * This method should be called to notify the stack of if there is ongoing composing or not in
     * this GroupChat. If there is an ongoing chat session established with the remote side
     * corresponding to this GroupChat this means that a call to this method will send the
     * 'is-composing' event or the 'is-not-composing' event to the remote side. However since this
     * method can be called at any time even when there is no chat session established with the
     * remote side or when the stack is not even connected to the IMS server then the stack
     * implementation needs to hold the last given information (i.e. composing or not composing) in
     * memory and then send it later when there is an established session available to relay this
     * information on. Note: if this GroupChat corresponds to an incoming pending chat session and
     * the parameter IM SESSION START is 1 then the session is accepted before sending the
     * 'is-composing' event.
     * 
     * @param ongoing True is client application is composing
     * @throws RcsGenericException
     */
    public void setComposingStatus(boolean ongoing) throws RcsGenericException {
        try {
            mGroupChatInf.setComposingStatus(ongoing);
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to send group announcement message in the group chat right
     * now, else returns false.
     *
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToSendAnnouncement() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return mGroupChatInf.isAllowedToSendAnnouncement();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends an announcement message to the group
     *
     * @param text Message
     * @return ChatMessage
     * @throws RcsPermissionDeniedException
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ChatMessage sendAnnouncement(String text) throws RcsPermissionDeniedException,
            RcsPersistentStorageException, RcsGenericException {
        try {
            return new ChatMessage(mGroupChatInf.sendAnnouncement(text));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to invite additional participants to the group chat right now,
     * else returns false.
     * 
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToInviteParticipants() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return mGroupChatInf.isAllowedToInviteParticipants();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to invite the specified participant to the group chat right
     * now, else returns false.
     * 
     * @param participant the contact ID
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToInviteParticipant(ContactId participant)
            throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mGroupChatInf.isAllowedToInviteParticipant(participant);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Invite additional participants to this group chat.
     * 
     * @param participants List of participants
     * @throws RcsPermissionDeniedException
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void inviteParticipants(Set<ContactId> participants)
            throws RcsPermissionDeniedException, RcsPersistentStorageException, RcsGenericException {
        try {
            mGroupChatInf.inviteParticipants(new ArrayList<>(participants));
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsUnsupportedOperationException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to remove reduced participants from the group chat right now,
     * else returns false.
     *
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToRemoveParticipants() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return mGroupChatInf.isAllowedToRemoveParticipants();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to remove the specified participant from the group chat right
     * now, else returns false.
     *
     * @param participant the contact ID
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToRemoveParticipant(ContactId participant)
            throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mGroupChatInf.isAllowedToRemoveParticipant(participant);

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Invite additional participants to this group chat.
     *
     * @param participants List of participants
     * @throws RcsPermissionDeniedException
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void removeParticipants(Set<ContactId> participants)
            throws RcsPermissionDeniedException, RcsPersistentStorageException, RcsGenericException {
        try {
            mGroupChatInf.removeParticipants(new ArrayList<>(participants));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsUnsupportedOperationException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to transfer ownership in the group chat right now, else
     * returns false.
     *
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToTransferOwnership() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return mGroupChatInf.isAllowedToTransferOwnership();

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to transfer ownership to the specified participant in the
     * group chat right now, else returns false.
     *
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToTakeOwnership(ContactId participant)
            throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mGroupChatInf.isAllowedToTransferOwnership2(participant);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Invite additional participants to this group chat.
     *
     * @param participant the contact ID
     * @throws RcsPermissionDeniedException
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void transferOwnership(ContactId participant) throws RcsPermissionDeniedException,
            RcsGenericException {
        try {
            mGroupChatInf.transferOwnership(participant);

        } catch (Exception e) {
            RcsUnsupportedOperationException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to rename the subject of the group chat right now, else
     * returns false.
     *
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToRenameSubject() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return mGroupChatInf.isAllowedToRenameSubject();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Rename subject of the group chat.
     *
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void renameSubject(String subject) throws RcsPermissionDeniedException,
            RcsGenericException {
        try {
            mGroupChatInf.renameSubject(subject);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsUnsupportedOperationException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to rename own alias in the group chat right now, else returns
     * false.
     *
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToRenameAlias() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return mGroupChatInf.isAllowedToRenameAlias();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Rename your alias displayed to other participants in the group chat.
     *
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void renameAlias(String alias) throws RcsPermissionDeniedException, RcsGenericException {
        try {
            mGroupChatInf.renameAlias(alias);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsUnsupportedOperationException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to leave this group chat.
     * 
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToLeave() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mGroupChatInf.isAllowedToLeave();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Leaves a group chat willingly and permanently. The group chat will continue between other
     * participants if there are enough participants.
     * 
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void leave() throws RcsPersistentStorageException, RcsGenericException {
        try {
            mGroupChatInf.leave();
        } catch (Exception e) {
            RcsUnsupportedOperationException.assertException(e);
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
            mGroupChatInf.openChat();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }
}
