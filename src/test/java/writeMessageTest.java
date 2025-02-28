import com.exchange.MessageHeaderEncoder;
import com.exchange.NewOrderEncoder;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class writeMessageTest {

    private NewOrder newOrder;

    @BeforeEach()
    void setUp() {
        newOrder = new NewOrder(12345L, "AAPL", (short) 0, 10, 10015L);
    }

//    @Test
//    void givenNewOrder_whenEncode_thenDecodedValuesMatch() {
//        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(128));
//        MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
//        NewOrderEncoder orderEncoder = new NewOrderEncoder();
//
//        NewOrderEncoder encoder = orderEncoder.wrapAndApplyHeader(buffer, 0, headerEncoder);
//        encoder.newo
//    }
}

