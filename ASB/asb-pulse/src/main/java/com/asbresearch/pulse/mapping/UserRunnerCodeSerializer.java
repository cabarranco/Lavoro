package com.asbresearch.pulse.mapping;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

public class UserRunnerCodeSerializer extends StdSerializer<UserRunnerCode> {

    public UserRunnerCodeSerializer() {
        this(null);
    }

    public UserRunnerCodeSerializer(Class<UserRunnerCode> t) {
        super(t);
    }

    @Override
    public void serialize(UserRunnerCode userRunnerCode, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(userRunnerCode.getCode());
    }
}
