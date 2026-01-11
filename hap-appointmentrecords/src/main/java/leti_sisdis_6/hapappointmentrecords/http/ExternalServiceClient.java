package leti_sisdis_6.hapappointmentrecords.http;

import leti_sisdis_6.hapappointmentrecords.exceptions.MicroserviceCommunicationException;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cliente para comunicações externas com suporte a Resiliência (Circuit Breaker, Retry, Bulkhead)
 * e Observabilidade (Logs estruturados para ELK e Tracing via Micrometer/Zipkin).
 */
@Service
@Slf4j
public class ExternalServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${hap.physicians.base-url:http://localhost:8081}")
    private String physiciansServiceUrl;

    @Value("${hap.patients.base-url:http://localhost:8082}")
    private String patientsServiceUrl;

    @Value("${hap.auth.base-url:http://localhost:8084}")
    private String authServiceUrl;

    @Value("${server.port:8083}")
    private String currentPort;

    // Lista estática de peers para instâncias do appointmentrecords
    private final List<String> peers = Arrays.asList(
            "http://localhost:8083",
            "http://localhost:8090"
    );

    @PostConstruct
    void init() {
        log.info("AppointmentRecords peers configurados na porta {}: {}", currentPort, peers);
        log.info("Peers ativos (excluindo self): {}", getPeerUrls());
    }

    /**
     * Constrói headers para propagar identidade e tracing entre microserviços.
     */
    private HttpHeaders buildForwardHeaders() {
        HttpHeaders headers = new HttpHeaders();
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String auth = req.getHeader("Authorization");
            String userId = req.getHeader("X-User-Id");
            String userRole = req.getHeader("X-User-Role");

            // Propaga tokens e contexto de utilizador
            if (auth != null && !auth.isBlank()) headers.add("Authorization", auth);
            if (userId != null && !userId.isBlank()) headers.add("X-User-Id", userId);
            if (userRole != null && !userRole.isBlank()) headers.add("X-User-Role", userRole);
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    // --- PHYSICIAN SERVICE CALLS ---

    @Retry(name = "externalService")
    @CircuitBreaker(name = "physicians", fallbackMethod = "fallbackPhysicians")
    @Bulkhead(name = "physicians")
    public Map<String, Object> getPhysicianById(String physicianId) {
        String url = physiciansServiceUrl + "/physicians/" + physicianId;
        log.debug("Chamando Physicians GET {}", url);
        return performExchange(url, physicianId, "getPhysicianById", "Physicians");
    }

    @Retry(name = "externalService")
    @CircuitBreaker(name = "physicians", fallbackMethod = "fallbackPhysicians")
    @Bulkhead(name = "physicians")
    public Map<String, Object> getAppointmentById(String appointmentId) {
        String url = physiciansServiceUrl + "/appointments/" + appointmentId;
        log.debug("Chamando Physicians GET (Appointment) {}", url);

        Map<String, Object> body = performExchange(url, appointmentId, "getAppointmentById", "Physicians");

        // Normalização de dados para o modelo interno
        if (body != null && body.get("physicianId") == null) {
            Object physicianObj = body.get("physician");
            if (physicianObj instanceof Map<?, ?> physMap) {
                Object nestedId = physMap.get("physicianId");
                if (nestedId instanceof String pid) {
                    body.put("physicianId", pid);
                }
            }
        }
        return body;
    }

    // --- PATIENT SERVICE CALLS ---

    @Retry(name = "externalService")
    @CircuitBreaker(name = "patients", fallbackMethod = "fallbackPatients")
    @Bulkhead(name = "patients")
    public Map<String, Object> getPatientById(String patientId) {
        String url = patientsServiceUrl + "/patients/" + patientId;
        log.debug("Chamando Patients GET {}", url);
        return performExchange(url, patientId, "getPatientById", "Patients");
    }

    // --- AUTH SERVICE CALLS ---

    @Retry(name = "externalService")
    @CircuitBreaker(name = "auth", fallbackMethod = "fallbackAuth")
    @Bulkhead(name = "auth")
    public Map<String, Object> validateToken(String token) {
        String url = authServiceUrl + "/api/auth/validate";
        try {
            log.debug("Validando token no Auth Service");
            return restTemplate.getForObject(url + "?token=" + token, Map.class);
        } catch (Exception e) {
            log.error("Falha na validação do token: {}", e.getMessage());
            throw new MicroserviceCommunicationException("Auth", "validateToken", e.getMessage(), e);
        }
    }

    // --- HELPER METHODS ---

    private Map<String, Object> performExchange(String url, String id, String method, String serviceName) {
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>(){}
            );
            return resp.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.info("{} retornou 404 para id={}", serviceName, id);
            throw new NotFoundException(serviceName + " não encontrado: " + id, e);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.warn("{} erro de segurança para id={}: {}", serviceName, id, e.getMessage());
            throw new MicroserviceCommunicationException(serviceName, method, "Erro de acesso", e);
        } catch (Exception e) {
            log.error("{} falhou para id={}: {}", serviceName, id, e.getMessage());
            throw new MicroserviceCommunicationException(serviceName, method, e.getMessage(), e);
        }
    }

    // --- FALLBACKS  ---

    private Map<String, Object> fallbackPhysicians(String id, Throwable t) {
        log.error("CircuitBreaker 'physicians' ABERTO ou falha crítica. ID={}. Erro: {}", id, t.getMessage());
        throw handleFallbackException("Physicians", t);
    }

    private Map<String, Object> fallbackPatients(String id, Throwable t) {
        log.error("CircuitBreaker 'patients' ABERTO ou falha crítica. ID={}. Erro: {}", id, t.getMessage());
        throw handleFallbackException("Patients", t);
    }

    private Map<String, Object> fallbackAuth(String token, Throwable t) {
        log.error("CircuitBreaker 'auth' ABERTO. Falha ao validar segurança: {}", t.getMessage());
        throw handleFallbackException("Auth", t);
    }

    private RuntimeException handleFallbackException(String service, Throwable t) {
        if (t instanceof NotFoundException) return (NotFoundException) t;
        return new MicroserviceCommunicationException(service, "CircuitBreaker", "Serviço temporariamente indisponível (Resilience4j)", t);
    }

    // --- PEER FORWARDING LOGIC ---

    public List<String> getPeerUrls() {
        return peers.stream()
                .filter(peerUrl -> !isCurrentInstance(peerUrl))
                .collect(Collectors.toList());
    }

    private boolean isCurrentInstance(String peerUrl) {
        return peerUrl.contains(":" + currentPort);
    }
}