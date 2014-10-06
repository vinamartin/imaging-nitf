/*
 * Copyright (c) 2014, Codice
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.codice.imaging.cgm;

import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 */
class StringFixedArgumentElement extends ElementHelpers implements AbstractElement {

    StringFixedArgumentElement(int elementClass, int elementId, String elementName) {
        super(elementClass, elementId, elementName);
    }
    
    private String text;
    
    public String getText() {
        return text;
    }

    @Override
    public void readParameters(final DataInputStream dataStream, final int parameterLength) throws IOException {
        text = getStringFixed(dataStream);
        System.out.println("\tRead text:" + text);
    }

    private String getStringFixed(DataInputStream dataStream) throws IOException {
        int count = dataStream.readUnsignedByte();
        if (count > 254) {
            System.out.println("Need to handle long count");
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; ++i) {
            builder.append((char)dataStream.readByte());
        }
        return builder.toString();
    }
}