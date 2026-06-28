package com.bancox.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoLancamentoConverter implements AttributeConverter<TipoLancamento, String> {

    @Override
    public String convertToDatabaseColumn(TipoLancamento tipo) {
        if (tipo == null) return null;
        return tipo.name().toLowerCase();
    }

    @Override
    public TipoLancamento convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return TipoLancamento.valueOf(dbData.toUpperCase());
    }
}
