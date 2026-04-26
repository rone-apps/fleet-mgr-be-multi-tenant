package com.taxi.domain.receipt.converter;

import com.taxi.domain.receipt.model.ReceiptType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReceiptTypeConverterRegistry {
    private final Map<ReceiptType, ReceiptTypeConverter> registry;

    public ReceiptTypeConverterRegistry(List<ReceiptTypeConverter> converters) {
        this.registry = converters.stream()
            .collect(Collectors.toMap(ReceiptTypeConverter::getSupportedType, c -> c));
    }

    public Optional<ReceiptTypeConverter> getConverter(ReceiptType type) {
        return Optional.ofNullable(registry.get(type));
    }
}
