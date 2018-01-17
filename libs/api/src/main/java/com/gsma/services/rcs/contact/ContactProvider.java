/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.services.rcs.contact;

/**
 * Contact provider for RCS info integrated in the native address book
 * 
 * @author Jean-Marc AUFFRET
 */
public class ContactProvider {
    /**
     * RCS phone number
     */
    public final static String MIME_TYPE_PHONE_NUMBER = "vnd.android.cursor.item/com.gsma.services.rcs.number";

    /**
     * Registration state
     */
    public final static String MIME_TYPE_REGISTRATION_STATE = "vnd.android.cursor.item/com.gsma.services.rcs.registration-state";

    /**
     * Blocking contact
     */
    public final static String MIME_TYPE_BLOCKING_STATE = "vnd.android.cursor.item/com.gsma.services.rcs.blocking-state";

    /**
     * Image sharing capability support
     */
    public final static String MIME_TYPE_IMAGE_SHARE = "vnd.android.cursor.item/com.gsma.services.rcs.image-share";

    /**
     * Video sharing capability support
     */
    public final static String MIME_TYPE_VIDEO_SHARE = "vnd.android.cursor.item/com.gsma.services.rcs.video-share";

    /**
     * Standalone messaging capability support
     */
    public final static String MIME_TYPE_STANDALONE_MESSAGING = "vnd.android.cursor.item/com.gsma.services.rcs.standalone-messaging";

    /**
     * IM/Chat capability support
     */
    public final static String MIME_TYPE_IM_SESSION = "vnd.android.cursor.item/com.gsma.services.rcs.im-session";

    /**
     * File transfer capability support
     */
    public final static String MIME_TYPE_FILE_TRANSFER = "vnd.android.cursor.item/com.gsma.services.rcs.file-transfer";

    /**
     * Geolocation push capability support
     */
    public final static String MIME_TYPE_GEOLOC_PUSH = "vnd.android.cursor.item/com.gsma.services.rcs.geoloc-push";

    /**
     * Mime type for the time stamp when the blocking was activated
     */
    public final static String MIME_TYPE_BLOCKING_TIMESTAMP = "vnd.android.cursor.item/com.gsma.services.rcs.blocking-timestamp";

    /**
     * IP voice call capability support
     */
    // public final static String MIME_TYPE_IP_VOICE_CALL =
    // "vnd.android.cursor.item/com.gsma.services.rcs.ip-voice-call";

    /**
     * IP video call capability support
     */
    // public final static String MIME_TYPE_IP_VIDEO_CALL =
    // "vnd.android.cursor.item/com.gsma.services.rcs.ip-video-call";

    /**
     * RCS extensions supported
     */
    public final static String MIME_TYPE_EXTENSIONS = "vnd.android.cursor.item/com.gsma.services.rcs.extensions";

    private ContactProvider() {
    }
}
