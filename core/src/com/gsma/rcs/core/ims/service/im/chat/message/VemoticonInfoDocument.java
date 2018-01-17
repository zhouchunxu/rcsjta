/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.service.im.chat.message;

/**
 * Geolocation info document
 * 
 * @author vfml3370
 */
public class VemoticonInfoDocument {
    /**
     * MIME type
     */
    public static final String MIME_TYPE = "application/vemoticon+xml";

    /**
     * Sms
     */
    private String sms = null;

    /**
     * Eid
     */
    private String eid = null;

    /**
     * Constructor
     *
     */
    public VemoticonInfoDocument() {
    }

    public String getSms() {
        return sms;
    }

    public void setSms(String sms) {
        this.sms = sms;
    }

    public String getEid() {
        return eid;
    }

    public void setEid(String eid) {
        this.eid = eid;
    }
}
