/* Generated SBE (Simple Binary Encoding) message codec. */
package com.exchange;

import org.agrona.MutableDirectBuffer;


/**
 * Order filled by exchange
 */
@SuppressWarnings("all")
public final class OrderFilledEncoder
{
    public static final int BLOCK_LENGTH = 20;
    public static final int TEMPLATE_ID = 3;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final String SEMANTIC_VERSION = "0.1";
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final OrderFilledEncoder parentMessage = this;
    private MutableDirectBuffer buffer;
    private int offset;
    private int limit;

    public int sbeBlockLength()
    {
        return BLOCK_LENGTH;
    }

    public int sbeTemplateId()
    {
        return TEMPLATE_ID;
    }

    public int sbeSchemaId()
    {
        return SCHEMA_ID;
    }

    public int sbeSchemaVersion()
    {
        return SCHEMA_VERSION;
    }

    public String sbeSemanticType()
    {
        return "";
    }

    public MutableDirectBuffer buffer()
    {
        return buffer;
    }

    public int offset()
    {
        return offset;
    }

    public OrderFilledEncoder wrap(final MutableDirectBuffer buffer, final int offset)
    {
        if (buffer != this.buffer)
        {
            this.buffer = buffer;
        }
        this.offset = offset;
        limit(offset + BLOCK_LENGTH);

        return this;
    }

    public OrderFilledEncoder wrapAndApplyHeader(
        final MutableDirectBuffer buffer, final int offset, final MessageHeaderEncoder headerEncoder)
    {
        headerEncoder
            .wrap(buffer, offset)
            .blockLength(BLOCK_LENGTH)
            .templateId(TEMPLATE_ID)
            .schemaId(SCHEMA_ID)
            .version(SCHEMA_VERSION);

        return wrap(buffer, offset + MessageHeaderEncoder.ENCODED_LENGTH);
    }

    public int encodedLength()
    {
        return limit - offset;
    }

    public int limit()
    {
        return limit;
    }

    public void limit(final int limit)
    {
        this.limit = limit;
    }

    public static int orderIdId()
    {
        return 1;
    }

    public static int orderIdSinceVersion()
    {
        return 0;
    }

    public static int orderIdEncodingOffset()
    {
        return 0;
    }

    public static int orderIdEncodingLength()
    {
        return 8;
    }

    public static String orderIdMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public static long orderIdNullValue()
    {
        return 0xffffffffffffffffL;
    }

    public static long orderIdMinValue()
    {
        return 0x0L;
    }

    public static long orderIdMaxValue()
    {
        return 0xfffffffffffffffeL;
    }

    public OrderFilledEncoder orderId(final long value)
    {
        buffer.putLong(offset + 0, value, BYTE_ORDER);
        return this;
    }


    public static int filledQuantityId()
    {
        return 2;
    }

    public static int filledQuantitySinceVersion()
    {
        return 0;
    }

    public static int filledQuantityEncodingOffset()
    {
        return 8;
    }

    public static int filledQuantityEncodingLength()
    {
        return 4;
    }

    public static String filledQuantityMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public static long filledQuantityNullValue()
    {
        return 4294967295L;
    }

    public static long filledQuantityMinValue()
    {
        return 0L;
    }

    public static long filledQuantityMaxValue()
    {
        return 4294967294L;
    }

    public OrderFilledEncoder filledQuantity(final long value)
    {
        buffer.putInt(offset + 8, (int)value, BYTE_ORDER);
        return this;
    }


    public static int filledPriceId()
    {
        return 3;
    }

    public static int filledPriceSinceVersion()
    {
        return 0;
    }

    public static int filledPriceEncodingOffset()
    {
        return 12;
    }

    public static int filledPriceEncodingLength()
    {
        return 8;
    }

    public static String filledPriceMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public static long filledPriceNullValue()
    {
        return 0xffffffffffffffffL;
    }

    public static long filledPriceMinValue()
    {
        return 0x0L;
    }

    public static long filledPriceMaxValue()
    {
        return 0xfffffffffffffffeL;
    }

    public OrderFilledEncoder filledPrice(final long value)
    {
        buffer.putLong(offset + 12, value, BYTE_ORDER);
        return this;
    }


    public String toString()
    {
        if (null == buffer)
        {
            return "";
        }

        return appendTo(new StringBuilder()).toString();
    }

    public StringBuilder appendTo(final StringBuilder builder)
    {
        if (null == buffer)
        {
            return builder;
        }

        final OrderFilledDecoder decoder = new OrderFilledDecoder();
        decoder.wrap(buffer, offset, BLOCK_LENGTH, SCHEMA_VERSION);

        return decoder.appendTo(builder);
    }
}
