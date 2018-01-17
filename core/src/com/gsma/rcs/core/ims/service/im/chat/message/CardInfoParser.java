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
 * Card info parser
 */
public class CardInfoParser extends DefaultHandler {

    /*
     * Card-Info SAMPLE: <?xml version="1.0"encoding="UTF-8"?> <vemoticon
     * xmlns="http://vemoticon.bj.ims.mnc000.mcc460.3gppnetwork.org/types"> <sms>微笑</sms>
     * <eid>E55A257E5B93CE76AC0F3DE43A3C284D</eid> </vemoticon >
     */

    private StringBuilder mAccumulator;
    private CardInfoDocument mCardInfo;
    private MediaArticle mCard;
    private static Logger sLogger = Logger.getLogger(CardInfoParser.class.getName());
    private final InputSource mInputSource;

    /**
     * Constructor
     *
     * @param inputSource Input source
     */
    public CardInfoParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the PIDF input
     * 
     * @return CardInfoParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public CardInfoParser parse() throws ParserConfigurationException, SAXException,
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

    public CardInfoDocument getCardInfo() {
        return mCardInfo;
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
        if ("msg_content".equals(localName)) {
            mCardInfo = new CardInfoDocument();
        } else if ("card".equals(localName)) {
            mCard = new MediaArticle();
        }
    }

    @Override
    public void endElement(String namespaceURL, String localName, String qname) {
        if ("media_type".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setMediaType(mAccumulator.toString().trim());
            }
        } else if ("operation_type".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setOperationType(mAccumulator.toString().trim());
            }
        } else if ("create_time".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setCreateTime(mAccumulator.toString().trim());
            }
        } else if ("pa_uuid".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setPaUuid(mAccumulator.toString().trim());
            }
        } else if ("forwordable".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setForwardable(mAccumulator.toString().trim());
            }
        } else if ("trust_level".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setTrustLevel(mAccumulator.toString().trim());
            }
        } else if ("access_id".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setAccessId(mAccumulator.toString().trim());
            }
        } else if ("auth_level".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setAuthLevel(mAccumulator.toString().trim());
            }
        } else if ("version".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setVersion(mAccumulator.toString().trim());
            }
        } else if ("display_style".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setDisplayStyle(mAccumulator.toString().trim());
            }
        } else if ("default_text".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setDefaultText(mAccumulator.toString().trim());
            }
        } else if ("default_link".equals(localName)) {
            if (mCardInfo != null) {
                mCardInfo.setDefaultLink(mAccumulator.toString().trim());
            }
        } else if ("card".equals(localName)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Card document complete");
            }
            if (mCardInfo != null) {
                mCardInfo.setCard(mCard);
            }
        }
        /* Parse card message */
        else if ("title".equals(localName)) {
            if (mCard != null) {
                mCard.setTitle(mAccumulator.toString().trim());
            }
        } else if ("author".equals(localName)) {
            if (mCard != null) {
                mCard.setAuthor(mAccumulator.toString().trim());
            }
        } else if ("thumb_link".equals(localName)) {
            if (mCard != null) {
                mCard.setThumbLink(mAccumulator.toString().trim());
            }
        } else if ("original_link".equals(localName)) {
            if (mCard != null) {
                mCard.setOriginalLink(mAccumulator.toString().trim());
            }
        } else if ("body_link".equals(localName)) {
            if (mCard != null) {
                mCard.setBodyLink(mAccumulator.toString().trim());
            }
        } else if ("source_link".equals(localName)) {
            if (mCard != null) {
                mCard.setSourceLink(mAccumulator.toString().trim());
            }
        } else if ("main_text".equals(localName)) {
            if (mCard != null) {
                mCard.setMainText(mAccumulator.toString().trim());
            }
        } else if ("media_uuid".equals(localName)) {
            if (mCard != null) {
                mCard.setMediaUuid(mAccumulator.toString().trim());
            }
        } else if ("msg_content".equals(localName)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Card-Info document complete");
            }
        }
    }

}
