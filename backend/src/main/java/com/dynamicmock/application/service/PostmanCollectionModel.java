package com.dynamicmock.application.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * APPLICATION LAYER - Postman Collection v2.1 JSON model.
 * Typed DTOs used by {@link PostmanImportService} to parse Postman exports.
 * Tolerant of unknown fields; Postman exports vary in shape (e.g. url may be
 * a plain string or a structured object).
 */
public final class PostmanCollectionModel {

    private PostmanCollectionModel() {
        // utility holder - no instances
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PostmanCollection {
        private Info info;
        private List<Item> item;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        private String name;
        private String description;
        private String schema;
    }

    /**
     * A Postman item is either a folder (has nested "item") or a request.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String name;
        private List<Item> item; // present when this item is a folder
        private Request request;
        private List<PostmanResponse> response; // saved examples
        private List<Event> event; // prerequest/test scripts (ignored on import)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request {
        private static final com.fasterxml.jackson.databind.ObjectMapper URL_MAPPER =
                new com.fasterxml.jackson.databind.ObjectMapper();

        private String method;
        private Url url; // may deserialize from a plain string via custom setter below
        private List<Header> header;
        private Body body;
        private String description;
        // Legacy exports sometimes nest saved examples and scripts inside the request
        private List<PostmanResponse> response;
        private List<Event> event;

        /**
         * Postman exports may serialize "url" as a plain string.
         */
        public void setUrl(Object url) {
            if (url == null) {
                this.url = null;
            } else if (url instanceof String s) {
                this.url = Url.builder().raw(s).build();
            } else {
                // Jackson will have populated a Url object - rebind through conversion
                this.url = URL_MAPPER.convertValue(url, Url.class);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Url {
        private String raw;
        private String protocol;
        private String host; // simplified: Postman may use array; handled by custom setter
        private List<String> path;
        private List<QueryParam> query;

        public void setHost(Object host) {
            if (host == null) {
                this.host = null;
            } else if (host instanceof java.util.List<?> list && !list.isEmpty()) {
                this.host = String.join(".", list.stream().map(String::valueOf).toList());
            } else {
                this.host = String.valueOf(host);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QueryParam {
        private String key;
        private String value;
        private Boolean disabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String key;
        private String value;
        private Boolean disabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private String mode; // raw, urlencoded, formdata, file, graphql
        private String raw;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PostmanResponse {
        private String name;
        private Integer code;
        private String body;
        private List<Header> header;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        private String listen; // "prerequest" or "test"
        private Script script;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Script {
        private String type;
        private List<String> exec;
    }

    /**
     * Postman environment export shape: { name, values: [{key, value, enabled}] }
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PostmanEnvironment {
        private String name;
        @JsonProperty("values")
        private List<EnvironmentValue> values;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class EnvironmentValue {
            private String key;
            private String value;
            private Boolean enabled;
        }
    }
}
