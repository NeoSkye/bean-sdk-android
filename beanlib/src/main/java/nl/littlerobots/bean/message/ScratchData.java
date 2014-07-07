/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Little Robots
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package nl.littlerobots.bean.message;

import android.os.Parcelable;

import java.io.UnsupportedEncodingException;

import auto.parcel.AutoParcel;
import okio.Buffer;

@AutoParcel
public abstract class ScratchData implements Parcelable, Message {
    public static ScratchData fromPayload(Buffer buffer) {
        return new AutoParcel_ScratchData((buffer.readByte() & 0xff) - 1, buffer.readByteArray());
    }

    public static ScratchData create(int number, byte[] data) {
        if (number < 0 || number > 4) {
            throw new IllegalArgumentException("Scratch number needs to be 0 - 4");
        }
        return new AutoParcel_ScratchData(number, data);
    }

    public static ScratchData create(int number, String data) {
        try {
            return create(number, data == null ? new byte[0] : data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract int number();

    public abstract byte[] data();

    public String getDataAsString() {
        try {
            return new String(data(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] toPayload() {
        Buffer buffer = new Buffer();
        buffer.writeByte((number() + 1) & 0xff);
        buffer.write(data());
        return buffer.readByteArray();
    }
}
