// Adapted from Manevolent/ts3j db57d60c989e399626aa16d921390f5033e6cdeb (Apache-2.0).
// Isolated test fixture only. See README.md for the retransmission cache patch.
package com.github.manevolent.ts3j.protocol.packet.transformation;

import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Pair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.EAXBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PacketTransformation {
    private static final int MAC_LEN = 8;
    private final byte[] ivStruct;
    private final byte[] fakeMac;

    private final EAXBlockCipher cipher;
    private final EAXBlockCipher decryptionCipher;
    private byte[] previousInput;
    private byte[] previousOutput;

    public PacketTransformation(byte[] ivStruct, byte[] fakeMac) {
        this.ivStruct = ivStruct;
        this.cipher = new EAXBlockCipher(new AESEngine());
        this.decryptionCipher = new EAXBlockCipher(new AESEngine());
        this.fakeMac = fakeMac;
    }

    public byte[] getFakeMac() {
        return fakeMac;
    }

    public Pair<byte[], byte[]> computeParameters(PacketHeader header) {
        ByteBuffer temporaryByteBuffer = ByteBuffer
                .allocate(ivStruct.length == 20 ? 26 : 70)
                .order(ByteOrder.BIG_ENDIAN);

        temporaryByteBuffer.put((byte) (header.getRole() == ProtocolRole.SERVER ? 0x30 : 0x31));
        temporaryByteBuffer.put((byte) (header.getType().getIndex() & 0xFF));
        temporaryByteBuffer.putInt(header.getGeneration());

        temporaryByteBuffer.put(ivStruct);

        byte[] arrayHash = temporaryByteBuffer.array();

        byte[] key_nonce = Ts3Crypt.hash256(arrayHash);

        byte[] key = new byte[16];
        byte[] nonce = new byte[16];

        System.arraycopy(key_nonce, 0, key, 0, 16);
        System.arraycopy(key_nonce, 16, nonce, 0, 16);

        // ivStruct[0]          = ivStruct[0] xor ((PId & 0xFF00) >> 8)
        // ivStruct[1]          = ivStruct[1] xor ((PId & 0x00FF) >> 0)

        key[0] = (byte) (key[0] ^ (header.getPacketId() >> 8) & 0xFF);
        key[1] = (byte) (key[1] ^ (header.getPacketId() >> 0) & 0xFF);

        return new Pair<>(key, nonce);
    }

    public synchronized ByteBuffer encrypt(Packet packet) {
        Pair<byte[], byte[]> parameters = computeParameters(packet.getHeader());

        // Write header (this is temporary)
        ByteBuffer headerBuffer = packet.writeHeader(
                ByteBuffer
                .allocate(packet.getHeader().getSize())
                .order(ByteOrder.BIG_ENDIAN)
        );

        // Get the header without a MAC for the associated text field
        byte[] headerWithoutMac = new byte[packet.getHeader().getSize() - MAC_LEN];
        System.arraycopy(headerBuffer.array(), MAC_LEN, headerWithoutMac, 0, headerWithoutMac.length);

        CipherParameters cipherParameters = new AEADParameters(
                new KeyParameter(parameters.getKey()),
                8 * MAC_LEN,
                parameters.getValue(),
                headerWithoutMac
        );

        int dataLen = packet.getBody().getSize();
        ByteBuffer packetBuffer = packet.writeBody(ByteBuffer.allocate(dataLen).order(ByteOrder.BIG_ENDIAN));

        byte[] result;
        int len;

        byte[] input = ByteBuffer.allocate(parameters.getKey().length + parameters.getValue().length + headerWithoutMac.length + dataLen)
                .put(parameters.getKey()).put(parameters.getValue()).put(headerWithoutMac).put(packetBuffer.array()).array();
        // A UDP retransmission must resend identical ciphertext, not encrypt the same nonce again.
        if (java.util.Arrays.equals(input, previousInput)) {
            System.arraycopy(previousOutput, 0, packet.getHeader().getMac(), 0, MAC_LEN);
            return ByteBuffer.wrap(previousOutput.clone());
        }
        cipher.init(true, cipherParameters);

        result = new byte[cipher.getOutputSize(dataLen)];
        len = cipher.processBytes(packetBuffer.array(), 0, dataLen, result, 0);

        try {
            len += cipher.doFinal(result, len);
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException(e);
        }

        ByteBuffer outputBuffer = ByteBuffer.allocate(MAC_LEN + headerWithoutMac.length + (len - MAC_LEN));

        // Copy the mac to the packet header
        System.arraycopy(result, len - MAC_LEN, packet.getHeader().getMac(), 0, MAC_LEN);

        // MAC
        outputBuffer.put(result, len - MAC_LEN, MAC_LEN);

        // Rest of header
        outputBuffer.put(headerWithoutMac);

        // Encrypted body
        outputBuffer.put(result, 0, len - MAC_LEN);

        previousInput = input;
        previousOutput = outputBuffer.array().clone();
        return outputBuffer;
    }

    public byte[] decrypt(PacketHeader header, ByteBuffer buffer, int dataLen)
            throws InvalidCipherTextException {
        Pair<byte[], byte[]> parameters = computeParameters(header);

        byte[] headerWithoutMac = new byte[header.getSize() - MAC_LEN];
        System.arraycopy(buffer.array(), MAC_LEN, headerWithoutMac, 0, headerWithoutMac.length);

        // Get the header without a MAC for the associated text field
        CipherParameters decryptionCipherParameters = new AEADParameters(
                new KeyParameter(parameters.getKey()),
                8 * MAC_LEN,
                parameters.getValue(),
                headerWithoutMac
        );

        byte[] result;
        int len;

        synchronized (decryptionCipher) {
            decryptionCipher.init(false, decryptionCipherParameters);
            result = new byte[decryptionCipher.getOutputSize(dataLen + MAC_LEN)];

            len = decryptionCipher.processBytes(buffer.array(), header.getSize(), dataLen, result, 0);
            len += decryptionCipher.processBytes(buffer.array(), 0, MAC_LEN, result, len);
            len += decryptionCipher.doFinal(result, len);

            if (len != dataLen)
                throw new IllegalArgumentException(len + " != " + dataLen);
        }

        return result;
    }
}
