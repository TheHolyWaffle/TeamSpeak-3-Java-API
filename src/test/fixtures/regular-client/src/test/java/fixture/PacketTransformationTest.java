package fixture;

import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.packet.PacketBody6Ack;
import com.github.manevolent.ts3j.protocol.packet.PacketBodyType;
import com.github.manevolent.ts3j.protocol.packet.transformation.InitPacketTransformation;
import com.github.manevolent.ts3j.protocol.packet.transformation.PacketTransformation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PacketTransformationTest {
    private static Packet acknowledgement(int acknowledgedPacket) {
        var header = new ClientPacketHeader();
        header.setType(PacketBodyType.ACK);
        var packet = new Packet(ProtocolRole.CLIENT, header);
        packet.setBody(new PacketBody6Ack(ProtocolRole.CLIENT, acknowledgedPacket));
        return packet;
    }

    @Test void identicalRetransmissionResendsCiphertextAndRestoresMac() {
        var transformation = new PacketTransformation(new byte[20], new byte[8]);
        var original = acknowledgement(12);
        var retry = acknowledgement(12);
        var encoded = transformation.encrypt(original).array();
        assertArrayEquals(encoded, transformation.encrypt(retry).array());
        assertArrayEquals(original.getHeader().getMac(), retry.getHeader().getMac());
        encoded[0] ^= 1;
        assertArrayEquals(original.getHeader().getMac(), java.util.Arrays.copyOf(transformation.encrypt(retry).array(), 8));
    }

    @Test void changedPlaintextWithSameKeyAndNonceStillFailsFast() {
        var transformation = new PacketTransformation(new byte[20], new byte[8]);
        transformation.encrypt(acknowledgement(12));
        var error = assertThrows(IllegalArgumentException.class, () -> transformation.encrypt(acknowledgement(13)));
        assertTrue(error.getMessage().contains("reuse nonce"));
    }

    @Test void alteredMacOrCiphertextIsRejected() {
        for (int position : new int[]{0, 14}) {
            var sender = new PacketTransformation(new byte[20], new byte[8]);
            var receiver = new PacketTransformation(new byte[20], new byte[8]);
            var packet = acknowledgement(12);
            byte[] encoded = sender.encrypt(packet).array();
            encoded[position] ^= 1;
            assertThrows(org.bouncycastle.crypto.InvalidCipherTextException.class,
                () -> receiver.decrypt(packet.getHeader(), java.nio.ByteBuffer.wrap(encoded), 2));
        }
    }

    @Test void fixedPublicHandshakeCipherAllowsTheProtocolPackets() {
        var handshake = new InitPacketTransformation();
        assertDoesNotThrow(() -> handshake.encrypt(acknowledgement(12)));
        assertDoesNotThrow(() -> handshake.encrypt(acknowledgement(13)));
    }

    @Test void decryptionDoesNotPolluteEncryptionReuseGuard() throws Exception {
        var sender = new PacketTransformation(new byte[20], new byte[8]);
        var receiver = new PacketTransformation(new byte[20], new byte[8]);
        var incoming = acknowledgement(12);
        var bytes = sender.encrypt(incoming);
        assertArrayEquals(new byte[]{0, 12}, receiver.decrypt(incoming.getHeader(), bytes, 2));
        assertDoesNotThrow(() -> receiver.encrypt(acknowledgement(13)));
    }
}
