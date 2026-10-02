package com.dynamicmock.adapter.out.protocol.iso8583;

import com.dynamicmock.adapter.out.script.ScriptContext;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.application.service.ScenarioService;
import com.dynamicmock.domain.entity.Iso8583Endpoint;
import com.dynamicmock.domain.entity.Scenario.ScenarioState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Iso8583ServerTest {

    @Mock
    private ResponseTemplateEngine templateEngine;

    @Mock
    private ScriptEngine scriptEngine;

    @Mock
    private Q2ServerManager q2ServerManager;

    @Mock
    private ScenarioService scenarioService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private Iso8583Server server;

    private GenericPackager packager;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/packager/iso87ascii.xml")) {
            if (is != null) {
                packager = new GenericPackager(is);
            }
        }
        // Fallback for tests if resource is not found (jPOS loads its own default)
        if (packager == null) {
            packager = new GenericPackager("jar:packager/iso87ascii.xml");
        }
    }

    @Test
    void processMessage_withScenario_shouldOverrideFieldsAndTriggerTransition() throws Exception {
        // Given
        ISOMsg request = new ISOMsg();
        request.setPackager(packager);
        request.setMTI("0100");
        request.set(2, "4111111111111111");
        request.set(3, "000000");
        request.set(4, "000000005000");

        Iso8583Endpoint endpoint = new Iso8583Endpoint();
        endpoint.setEncoding("ASCII");
        endpoint.setHeaderLengthType("2BYTE");

        Iso8583Endpoint.Iso8583Mock mock = new Iso8583Endpoint.Iso8583Mock();
        mock.setName("Test Mock");
        mock.setMti("0100");
        mock.setScenarioName("test-scenario");
        mock.setResponseFields(new HashMap<>());
        mock.setDelayMs(0);
        endpoint.setMocks(List.of(mock));

        ScenarioState scenarioState = new ScenarioState();
        scenarioState.setName("state-1");
        // We will mock objectMapper parsing for this JSON
        scenarioState.setResponseTemplate("{\"39\": \"00\", \"38\": \"123456\"}");
        scenarioState.setDelayMs(50);

        when(scenarioService.getCurrentStateObject("test-scenario")).thenReturn(scenarioState);
        when(objectMapper.readValue(eq("{\"39\": \"00\", \"38\": \"123456\"}"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Map.of("39", "00", "38", "123456"));
        
        when(templateEngine.render(eq("00"), any())).thenReturn("00");
        when(templateEngine.render(eq("123456"), any())).thenReturn("123456");

        // When
        ISOMsg response = server.processMessage(request, endpoint);

        // Then
        assertNotNull(response);
        assertEquals("0110", response.getMTI()); // Auto-computed response MTI for 0100
        assertEquals("00", response.getString(39)); // From scenario override
        assertEquals("123456", response.getString(38)); // From scenario override

        // Verify transition was processed
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
        verify(scenarioService).processTransition(eq("test-scenario"), contextCaptor.capture());
        
        Map<String, Object> transitionContext = contextCaptor.getValue();
        assertNotNull(transitionContext.get("request"));
        assertNotNull(transitionContext.get("response"));
        assertNotNull(transitionContext.get("state"));
        
        Map<String, String> responseMap = (Map<String, String>) transitionContext.get("response");
        assertEquals("0110", responseMap.get("mti"));
        assertEquals("00", responseMap.get("39"));
        assertEquals("123456", responseMap.get("38"));
    }
    
    @Test
    void processMessage_withoutScenario_shouldUseMockDefaults() throws Exception {
        // Given
        ISOMsg request = new ISOMsg();
        request.setPackager(packager);
        request.setMTI("0800");

        Iso8583Endpoint endpoint = new Iso8583Endpoint();
        endpoint.setEncoding("ASCII");
        endpoint.setHeaderLengthType("2BYTE");

        Iso8583Endpoint.Iso8583Mock mock = new Iso8583Endpoint.Iso8583Mock();
        mock.setName("Network Echo");
        mock.setMti("0800");
        mock.setResponseCode("00");
        endpoint.setMocks(List.of(mock));

        // When
        ISOMsg response = server.processMessage(request, endpoint);

        // Then
        assertNotNull(response);
        assertEquals("0810", response.getMTI());
        assertEquals("00", response.getString(39));
        
        verify(scenarioService, never()).processTransition(any(), any());
    }
}
