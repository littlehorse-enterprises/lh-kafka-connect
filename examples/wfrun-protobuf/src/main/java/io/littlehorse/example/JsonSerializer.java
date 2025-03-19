package io.littlehorse.example;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

public class JsonSerializer {

    public static final JsonFormat.Printer printer =
            JsonFormat.printer().omittingInsignificantWhitespace();

    private JsonSerializer() {}

    public static String serialize(MessageOrBuilder object) {
        try {
            return printer.print(object);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
