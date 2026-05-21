package com.dynamicmock.infrastructure.config;

import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.application.service.RouteService;
import com.dynamicmock.application.service.ScenarioService;
import com.dynamicmock.domain.entity.Scenario;
import com.dynamicmock.domain.port.out.MockRouteRepository;
import com.dynamicmock.domain.port.out.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Seeds demo routes and scenarios when the "demo" profile is active.
 * Keeps the app instantly usable after `docker compose up`.
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataLoader implements CommandLineRunner {

    private final RouteService routeService;
    private final ScenarioService scenarioService;
    private final MockRouteRepository mockRouteRepository;
    private final ScenarioRepository scenarioRepository;

    @Override
    public void run(String... args) {
        log.info("Demo profile active - loading sample routes and scenarios");
        seedHelloRoute();
        seedOrderScenario();
        scenarioService.loadActiveScenarios();
    }

    private void seedHelloRoute() {
        boolean routeExists = !mockRouteRepository.findByPathAndMethod("/hello", "GET").isEmpty();
        if (routeExists) {
            log.info("Demo hello route already present");
            return;
        }

        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/hello");
        request.setMethod("GET");
        request.setResponseTemplate(
            "{\"message\":\"Hello from Dynamic Mock\",\"id\":\"{{$randomUUID}}\",\"echo\":\"{{request.queryParams.name}}\"}"
        );
        request.setResponseStatus(200);
        request.setResponseHeaders(Map.of("X-Demo-Route", "hello"));
        request.setScriptLanguage("js");
        request.setDelayMs(0);

        RouteResponse created = routeService.createRoute(request);
        routeService.activateRoute(created.getId());

        log.info("Created demo hello route at /mock/hello");
    }

    private void seedOrderScenario() {
        if (!scenarioRepository.existsByName("order-demo")) {
            Scenario scenario = Scenario.builder()
                .name("order-demo")
                .description("Simple order lifecycle showing scenario-based responses")
                .initialState("cart")
                .states(List.of(
                    Scenario.ScenarioState.builder()
                        .name("cart")
                        .description("Cart review")
                        .responseStatus(200)
                        .responseTemplate("{\"state\":\"cart\",\"message\":\"Call /mock/orders/demo?action=pay to move to payment\"}")
                        .transitions(List.of(
                            Scenario.StateTransition.builder()
                                .name("pay")
                                .condition("request.queryParams.action == 'pay'")
                                .targetState("paid")
                                .priority(1)
                                .build()
                        ))
                        .build(),
                    Scenario.ScenarioState.builder()
                        .name("paid")
                        .description("Payment captured")
                        .responseStatus(200)
                        .responseTemplate("{\"state\":\"paid\",\"message\":\"Call /mock/orders/demo?action=ship to move to shipped\",\"orderId\":\"DM-{{$randomInt 1000 9999}}\"}")
                        .transitions(List.of(
                            Scenario.StateTransition.builder()
                                .name("ship")
                                .condition("request.queryParams.action == 'ship'")
                                .targetState("shipped")
                                .priority(1)
                                .build()
                        ))
                        .build(),
                    Scenario.ScenarioState.builder()
                        .name("shipped")
                        .description("Package shipped - auto-resets after this state")
                        .responseStatus(200)
                        .responseTemplate("{\"state\":\"shipped\",\"tracking\":\"DM-{{$randomInt 100000 999999}}\",\"note\":\"Auto-resets after this\"}")
                        .build()
                ))
                .active(true)
                .autoReset(true)
                .maxExecutions(50)
                .build();

            scenarioService.createScenario(scenario);
            log.info("Created demo scenario 'order-demo'");
        }

        boolean routeExists = !mockRouteRepository.findByPathAndMethod("/orders/demo", "GET").isEmpty();
        if (routeExists) {
            log.info("Demo order scenario route already present");
            return;
        }

        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/orders/demo");
        request.setMethod("GET");
        request.setResponseTemplate("{\"state\":\"cart\",\"hint\":\"Use ?action=pay then ?action=ship\"}");
        request.setResponseStatus(200);
        request.setScenarioName("order-demo");
        request.setResponseHeaders(Map.of("X-Demo-Route", "order-demo"));

        RouteResponse created = routeService.createRoute(request);
        routeService.activateRoute(created.getId());
        log.info("Created demo scenario-backed route at /mock/orders/demo");
    }
}

