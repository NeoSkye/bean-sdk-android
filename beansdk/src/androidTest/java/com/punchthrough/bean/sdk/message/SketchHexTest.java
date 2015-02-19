package com.punchthrough.bean.sdk.message;

import android.test.AndroidTestCase;

import com.punchthrough.bean.sdk.internal.exception.HexParsingException;
import com.punchthrough.bean.sdk.internal.exception.NameLengthException;

import static com.punchthrough.bean.sdk.internal.utility.Misc.intArrayToByteArray;
import static org.assertj.core.api.Assertions.assertThat;

public class SketchHexTest extends AndroidTestCase {

    // From http://en.wikipedia.org/wiki/Intel_HEX
    final String asciiHexData =
            //        Bytes 1-8       Bytes 9-16
            //        |...............|...............
            ":10010000214601360121470136007EFE09D2190140\r\n" +
            ":100110002146017E17C20001FF5F16002148011928\r\n" +
            ":10012000194E79234623965778239EDA3F01B2CAA7\r\n" +
            ":100130003F0156702B5E712B722B732146013421C7\r\n" +
            ":00000001FF";

    // Produced from the above hex by http://hex2bin.sourceforge.net/
    final int[] rawHexDataInts = new int[]{
            0x21, 0x46, 0x01, 0x36, 0x01, 0x21, 0x47, 0x01,  //  1-16
            0x36, 0x00, 0x7E, 0xFE, 0x09, 0xD2, 0x19, 0x01,
            0x21, 0x46, 0x01, 0x7E, 0x17, 0xC2, 0x00, 0x01,  // 17-32
            0xFF, 0x5F, 0x16, 0x00, 0x21, 0x48, 0x01, 0x19,
            0x19, 0x4E, 0x79, 0x23, 0x46, 0x23, 0x96, 0x57,  // 33-48
            0x78, 0x23, 0x9E, 0xDA, 0x3F, 0x01, 0xB2, 0xCA,
            0x3F, 0x01, 0x56, 0x70, 0x2B, 0x5E, 0x71, 0x2B,  // 49-64
            0x72, 0x2B, 0x73, 0x21, 0x46, 0x01, 0x34, 0x21
    };

    final byte[] rawHexData = intArrayToByteArray(rawHexDataInts);

    public void testHexParsingAndSketchNaming() throws HexParsingException, NameLengthException {

        assertThat(SketchHex.create("StairCar").sketchName()).isEqualTo("StairCar");

        try {
            SketchHex.create("LongerThan20CharactersByQuiteABit_TheSequel_TurboEdition");
            fail();
        } catch (NameLengthException e) {
            assertThat(e).isNotNull();
        }

        assertThat(SketchHex.create("", asciiHexData).bytes()).isEqualTo(rawHexData);
    }

    public void testGetBytesAndChunks() throws NameLengthException, HexParsingException {

        SketchHex hex = SketchHex.create("", asciiHexData);

        // Queries that don't extend past the array boundary should work as expected

        // Chunk starting at 6 and ending at 7
        assertThat(hex.getBytes(6, 2)).isEqualTo(intArrayToByteArray(
                new int[]{ 0x47, 0x01 }));

        // Chunk starting at 8 and ending at 11
        assertThat(hex.getChunk(4, 2)).isEqualTo(intArrayToByteArray(
                new int[]{ 0x36, 0x00, 0x7E, 0xFE }));

        // Queries that extend past the array boundary should be truncated

        // Chunk starting at 60 and ending at 67, truncated at 63
        assertThat(hex.getBytes(60, 8)).isEqualTo(intArrayToByteArray(
                new int[]{ 0x46, 0x01 ,0x34, 0x21 }));

        // Chunk starting at 60 and ending at 64, truncated at 63
        assertThat(hex.getChunk(5, 12)).isEqualTo(intArrayToByteArray(
                new int[]{ 0x46, 0x01 ,0x34, 0x21 }));

    }

}
