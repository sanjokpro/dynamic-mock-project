# Third-Party Licenses

This document lists the open-source libraries used by Dynamic Mock and their respective licenses.
This is informational only and does not constitute legal advice.

---

## Backend Dependencies

### jPOS
- **Artifact**: `org.jpos:jpos:2.1.9`
- **License**: GNU Affero General Public License v3 (AGPL-3.0)
- **URL**: https://jpos.org
- **Source**: https://github.com/jpos/jPOS
- **Usage**: Used as an unmodified runtime library for ISO8583 message parsing and TCP server management via the Q2 framework.
- **Note**: Dynamic Mock is distributed under the Apache License 2.0. jPOS is distributed separately under AGPL-3.0 and is used by Dynamic Mock as an unmodified runtime dependency. Organizations should independently review the licensing requirements of all third-party dependencies and consult qualified legal counsel if they have questions regarding their specific usage, distribution, hosting, or compliance obligations. For organizations that require commercial licensing terms for jPOS, additional licensing options may be available from the jPOS project at https://jpos.org/license.

---

### GraalVM Polyglot SDK
- **Artifact**: `org.graalvm.polyglot:polyglot:23.1.1`, `org.graalvm.polyglot:js:23.1.1`, `org.graalvm.polyglot:python:23.1.1`
- **License**: Universal Permissive License v1.0 (UPL-1.0)
- **URL**: https://www.graalvm.org
- **Source**: https://github.com/oracle/graal
- **Usage**: Provides polyglot scripting support for JavaScript and Python response scripts.

---

### Spring Boot
- **Artifact**: `org.springframework.boot:spring-boot-starter-*:4.0.1`
- **License**: Apache License 2.0
- **URL**: https://spring.io/projects/spring-boot
- **Source**: https://github.com/spring-projects/spring-boot

---

### Spring Data MongoDB
- **Artifact**: `org.springframework.boot:spring-boot-starter-data-mongodb`
- **License**: Apache License 2.0
- **URL**: https://spring.io/projects/spring-data-mongodb
- **Usage**: MongoDB persistence layer.

---

### Spring Data Redis / Lettuce
- **Artifact**: `org.springframework.boot:spring-boot-starter-data-redis`, `io.lettuce:lettuce-core`
- **License**: Apache License 2.0
- **URL**: https://lettuce.io
- **Usage**: Redis client for stateful scenario state storage.

---

### gRPC
- **Artifact**: `io.grpc:grpc-netty-shaded:1.60.0`, `io.grpc:grpc-protobuf:1.60.0`, `io.grpc:grpc-stub:1.60.0`
- **License**: Apache License 2.0
- **URL**: https://grpc.io
- **Source**: https://github.com/grpc/grpc-java

---

### Protocol Buffers
- **Artifact**: `com.google.protobuf:protobuf-java:3.25.1`
- **License**: BSD 3-Clause License
- **URL**: https://protobuf.dev
- **Source**: https://github.com/protocolbuffers/protobuf

---

### Handlebars.java
- **Artifact**: `com.github.jknack:handlebars:4.3.1`
- **License**: Apache License 2.0
- **URL**: https://github.com/jknack/handlebars.java
- **Usage**: Response template engine.

---

### Jackson
- **Artifact**: `com.fasterxml.jackson.core:jackson-databind`, `jackson-core`, `jackson-annotations`, `jackson-datatype-jsr310`
- **License**: Apache License 2.0
- **URL**: https://github.com/FasterXML/jackson

---

### JSONPath
- **Artifact**: `com.jayway.jsonpath:json-path:2.9.0`
- **License**: Apache License 2.0
- **URL**: https://github.com/json-path/JsonPath

---

### Lombok
- **Artifact**: `org.projectlombok:lombok`
- **License**: MIT License
- **URL**: https://projectlombok.org

---

## Frontend Dependencies

### Next.js
- **Package**: `next@16.2.9`
- **License**: MIT License
- **URL**: https://nextjs.org

### React
- **Package**: `react@19.2.4`, `react-dom@19.2.4`
- **License**: MIT License
- **URL**: https://react.dev

### TanStack Query (React Query)
- **Package**: `@tanstack/react-query@5.x`
- **License**: MIT License
- **URL**: https://tanstack.com/query

### Tailwind CSS
- **Package**: `tailwindcss@4.x`
- **License**: MIT License
- **URL**: https://tailwindcss.com

### Axios
- **Package**: `axios@1.x`
- **License**: MIT License
- **URL**: https://axios-http.com

### Monaco Editor for React
- **Package**: `@monaco-editor/react@4.x`
- **License**: MIT License
- **URL**: https://github.com/suren-atoyan/monaco-react

### Lucide React
- **Package**: `lucide-react@1.x`
- **License**: ISC License
- **URL**: https://lucide.dev

### Sonner
- **Package**: `sonner@2.x`
- **License**: MIT License
- **URL**: https://sonner.emilkowal.ski

### XY Flow (React Flow)
- **Package**: `@xyflow/react@12.x`
- **License**: MIT License
- **URL**: https://reactflow.dev

### SockJS Client
- **Package**: `sockjs-client@1.x`
- **License**: MIT License
- **URL**: https://github.com/sockjs/sockjs-client

### STOMP.js
- **Package**: `@stomp/stompjs@7.x`
- **License**: Apache License 2.0
- **URL**: https://stomp-js.github.io

---

## Note on jPOS and AGPL-3.0

Dynamic Mock is distributed under the Apache License 2.0.

jPOS is distributed separately under the GNU Affero General Public License v3 (AGPL v3) and is used by Dynamic Mock as an unmodified runtime dependency. Dynamic Mock's ISO8583 simulation is implemented in its own adapter layer (`Iso8583Server.java`, `Q2ServerManager.java`), which interacts with jPOS through its public API.

Organizations should independently review the licensing requirements of all third-party dependencies and consult qualified legal counsel if they have questions regarding their specific usage, distribution, hosting, or compliance obligations.

For organizations that require commercial licensing terms for jPOS, additional licensing options may be available from the jPOS project. Contact information: https://jpos.org/license

> **This document is informational only and does not constitute legal advice.**
