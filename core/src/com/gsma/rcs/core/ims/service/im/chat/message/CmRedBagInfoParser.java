/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2016 China Mobile.
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

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.utils.logger.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Vemoticon info parser
 */
public class CmRedBagInfoParser extends DefaultHandler {

    /*
     * Vemoticon-Info SAMPLE: <?xml version="1.0"encoding="UTF-8"?> <vemoticon
     * xmlns="http://vemoticon.bj.ims.mnc000.mcc460.3gppnetwork.org/types"> <sms>微笑</sms>
     * <eid>E55A257E5B93CE76AC0F3DE43A3C284D</eid> </vemoticon >
     */

    private StringBuilder mAccumulator;
    private VemoticonInfoDocument mVemoticon;
    private static Logger sLogger = Logger.getLogger(CmRedBagInfoParser.class.getName());
    private final InputSource mInputSource;

    /**
     * Constructor
     *
     * @param inputSource Input source
     */
    public CmRedBagInfoParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the PIDF input
     * 
     * @return VemoticonInfoParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public CmRedBagInfoParser parse() throws ParserConfigurationException, SAXException,
            ParseFailureException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(mInputSource, this);
            return this;

        } catch (IOException e) {
            throw new ParseFailureException("Failed to parse input source!", e);
        }
    }

    public VemoticonInfoDocument getVemoticonInfo() {
        return mVemoticon;
    }

    @Override
    public void startDocument() {
        mAccumulator = new StringBuilder();
    }

    @Override
    public void characters(char buffer[], int start, int length) {
        mAccumulator.append(buffer, start, length);
    }

    @Override
    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        mAccumulator.setLength(0);
        if ("vemoticon".equals(localName)) {
            mVemoticon = new VemoticonInfoDocument();
        }
    }

    @Override
    public void endElement(String namespaceURL, String localName, String qname) {
        if ("sms".equals(localName)) {
            if (mVemoticon != null) {
                mVemoticon.setSms(mAccumulator.toString().trim());
            }
        } else if ("eid".equals(localName)) {
            if (mVemoticon != null) {
                mVemoticon.setEid(mAccumulator.toString().trim());
            }
        } else if ("vemoticon".equals(localName)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Vemoticon-Info document complete");
            }
        }
    }

}
