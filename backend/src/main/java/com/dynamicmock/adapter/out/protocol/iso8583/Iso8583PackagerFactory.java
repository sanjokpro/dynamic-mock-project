package com.dynamicmock.adapter.out.protocol.iso8583;

import com.dynamicmock.domain.entity.Iso8583Endpoint;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.iso.packager.ISO87APackager;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Shared factory for creating an ISO8583 ISOPackager from an endpoint configuration.
 *
 * Priority chain (same logic used by both the server and the simulator):
 *  1. Custom packager XML uploaded by the user (stored in endpoint.packagerXmlContent)
 *  2. Bundled default packager (/iso8583/packager.xml on the classpath)
 *  3. jPOS ISO87A packager fallback
 *
 * Extracted from Iso8583Server so that the simulator and any future
 * components can share the identical packager resolution logic.
 */
@Slf4j
@Component
public class Iso8583PackagerFactory {

    /**
     * Create the appropriate ISOPackager for the given endpoint.
     *
     * @param endpoint the ISO8583 endpoint configuration
     * @return an ISOPackager ready for packing/unpacking messages
     */
    public ISOPackager create(Iso8583Endpoint endpoint) {
        // 1. Custom uploaded packager takes precedence
        if (endpoint.getPackagerXmlContent() != null && !endpoint.getPackagerXmlContent().isBlank()) {
            try {
                InputStream customStream = new ByteArrayInputStream(
                        endpoint.getPackagerXmlContent().getBytes(StandardCharsets.UTF_8));
                log.debug("Using custom packager '{}' for endpoint '{}'",
                        endpoint.getPackagerName(), endpoint.getName());
                return new GenericPackager(customStream);
            } catch (ISOException e) {
                log.warn("Custom packager '{}' is invalid, falling back to bundled: {}",
                        endpoint.getPackagerName(), e.getMessage());
            }
        }
        // 2. Bundled default packager
        try {
            InputStream packagerStream = getClass().getResourceAsStream("/iso8583/packager.xml");
            if (packagerStream != null) {
                log.debug("Using bundled packager.xml for endpoint '{}'", endpoint.getName());
                return new GenericPackager(packagerStream);
            }
        } catch (ISOException e) {
            log.warn("Failed to load bundled packager, falling back to ISO87A: {}", e.getMessage());
        }
        // 3. ISO87A fallback
        log.debug("Using ISO87APackager fallback for endpoint '{}'", endpoint.getName());
        return new ISO87APackager();
    }
}
