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

package com.gsma.rcs.core.ims.service.im.chat.cpim;

import com.gsma.rcs.utils.DateUtils;

import java.util.Hashtable;

/**
 * CPIM message
 * 
 * @author jexa7410
 */
public class CpimMessage {
    /**
     * MIME type
     */
    public static final String MIME_TYPE = "message/cpim";

    /**
     * Header "Content-type"
     */
    public static final String HEADER_CONTENT_TYPE = "Content-type";
    public static final String HEADER_CONTENT_TYPE2 = "Content-Type";

    /**
     * Header "From"
     */
    public static final String HEADER_FROM = "From";

    /**
     * Header "To"
     */
    public static final String HEADER_TO = "To";

    /**
     * Header "cc". Specifies a non-primary recipient ("courtesy copy") for a message
     */
    public static final String HEADER_CC = "cc";

    /**
     * Header "DateTime"
     */
    public static final String HEADER_DATETIME = "DateTime";

    /**
     * Header "Silence" of CMCC extend
     */
    @Deprecated
    public static final String HEADER_SILENCE_SUPPORTED = "CMCCfeature.silenceSupported";

    /**
     * Header "NS"
     */
    public static final String HEADER_NS = "NS";

    /**
     * Header "Content-length"
     */
    public static final String HEADER_CONTENT_LENGTH = "Content-length";

    /**
     * Header "Content-Disposition"
     */
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

    /**
     * Header "Content-Transfer-Encoding"
     */
    public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";

    /**
     * Message content
     */
    private final String mMsgContent;

    /**
     * MIME headers
     */
    private final Hashtable<String, String> mHeaders;

    /**
     * MIME content headers
     */
    private final Hashtable<String, String> mContentHeaders;

    /**
     * Constructor
     * 
     * @param headers MIME headers
     * @param contentHeaders MIME content headers
     * @param msgContent Content
     */
    public CpimMessage(Hashtable<String, String> headers, Hashtable<String, String> contentHeaders,
            String msgContent) {
        mHeaders = headers;
        mContentHeaders = contentHeaders;
        mMsgContent = msgContent;
    }

    /**
     * Returns content type
     * 
     * @return Content type
     */
    public String getContentType() {
        String type = mContentHeaders.get(CpimMessage.HEADER_CONTENT_TYPE);
        if (type == null) {
            return mContentHeaders.get(CpimMessage.HEADER_CONTENT_TYPE2);
        }
        return type;
    }

    /**
     * Returns MIME header
     * 
     * @param name Header name
     * @return Header value
     */
    public String getHeader(String name) {
        return mHeaders.get(name);
    }

    /**
     * Returns MIME content header
     * 
     * @param name Header name
     * @return Header value
     */
    public String getContentHeader(String name) {
        return mContentHeaders.get(name);
    }

    /**
     * Returns message content
     * 
     * @return Content
     */
    public String getMessageContent() {
        return mMsgContent;
    }

    /**
     * Returns message timestamp sent
     * 
     * @return timestamp sent in payload
     */
    public long getTimestampSent() {
        String header = getHeader(CpimMessage.HEADER_DATETIME);
        if (header != null) {
            return DateUtils.decodeDate(header);
        }
        return 0;
    }

    @Deprecated
    public boolean isSilence() {
        String header = getHeader(CpimMessage.HEADER_NS);
        if (header != null && header.equals("CMCCfeature<urn: CMCCFeatures@rcs.chinamobile.com>")) {
            header = getHeader(CpimMessage.HEADER_SILENCE_SUPPORTED);
            if (header != null && header.equals("true")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns message id
     *
     * @return message id in payload
     */
    public long getMessageId() {
        String header = getHeader(CpimMessage.HEADER_FROM);
        if (header != null) {
            return DateUtils.decodeDate(header);
        }
        return 0;
    }

    /**
     * Cpim message builder class
     */
    public static class CpimMessageBuilder {
        //TODO
        private String mFrom = null;
        private String mTo = null;
        private String mCc = null;

        /**
         * Default constructor
         */
        public CpimMessageBuilder() {

        }

        /**
         * Copy constructor
         *
         * @param cpim to copy or null if construct with default values
         */
        public CpimMessageBuilder(CpimMessage cpim) {

        }

        public String build() {
            return "";
        }
    }

}
