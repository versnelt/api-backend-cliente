package com.netbull.apiclient.utility;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class JsonLocalDateDeserializer extends StdDeserializer<LocalDate> {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    protected JsonLocalDateDeserializer(Class<LocalDate> t) {
        super(t);
    }

    public JsonLocalDateDeserializer() {
        this(null);
    }

    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {

        try {
            return  LocalDate.parse(jsonParser.getText(), formatter);
        } catch (Exception e) {
            throw new RuntimeException("Erro na deserialização da data");
        }
    }


}
