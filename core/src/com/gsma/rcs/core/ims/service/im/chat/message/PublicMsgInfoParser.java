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
 * PublicMsg info parser
 */
public class PublicMsgInfoParser extends DefaultHandler {

    private StringBuilder mAccumulator;
    private PublicMsgInfoDocument mPublicMsgInfo;
    private MediaArticle mCard;
    private static Logger sLogger = Logger.getLogger(PublicMsgInfoParser.class.getName());
    private final InputSource mInputSource;

    /**
     * Constructor
     *
     * @param inputSource Input source
     */
    public PublicMsgInfoParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the PIDF input
     * 
     * @return PublicMsgInfoParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public PublicMsgInfoParser parse() throws ParserConfigurationException, SAXException,
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

    public PublicMsgInfoDocument getPublicMsgInfo() {
        return mPublicMsgInfo;
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
            mPublicMsgInfo = new PublicMsgInfoDocument();
        } else if ("card".equals(localName)) {

        }
    }

    @Override
    public void endElement(String namespaceURL, String localName, String qname) {
    }

}
