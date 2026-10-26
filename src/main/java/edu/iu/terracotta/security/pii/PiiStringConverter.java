package edu.iu.terracotta.security.pii;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores a PII string column encrypted. Hibernate gets this converter from Spring, so the
 * configured keys are injected.
 *
 * With pii.encryption.decrypt-all on (only while preparing to roll back to a build without
 * encryption), values are saved in plain text, so nothing is re-encrypted after the startup
 * decryption has run.
 */
@Component
@Converter
public class PiiStringConverter implements AttributeConverter<String, String> {

    private final PiiCipher piiCipher;
    private final boolean decryptAll;

    public PiiStringConverter(PiiCipher piiCipher, @Value("${pii.encryption.decrypt-all:false}") boolean decryptAll) {
        this.piiCipher = piiCipher;
        this.decryptAll = decryptAll;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return decryptAll ? piiCipher.decrypt(attribute) : piiCipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return piiCipher.decrypt(dbData);
    }

}
