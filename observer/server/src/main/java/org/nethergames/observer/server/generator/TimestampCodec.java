package org.nethergames.observer.server.generator;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.sql.Timestamp;

public class TimestampCodec implements Codec<Timestamp> {
    @Override
    public Timestamp decode(BsonReader bsonReader, DecoderContext decoderContext) {
        return new Timestamp(bsonReader.readDateTime());
    }

    @Override
    public void encode(BsonWriter bsonWriter, Timestamp timestamp, EncoderContext encoderContext) {
        bsonWriter.writeDateTime(timestamp.getTime());
    }

    @Override
    public Class<Timestamp> getEncoderClass() {
        return Timestamp.class;
    }
}
