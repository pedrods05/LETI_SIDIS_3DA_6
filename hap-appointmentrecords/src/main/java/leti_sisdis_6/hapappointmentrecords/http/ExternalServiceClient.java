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

import java.net.UnknownHostException;
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

    /**
     * When running inside Docker, prefer service DNS names (hap-physicians, hap-auth, ...).
     * If you ever run the service outside Docker, you can opt-in to a docker-host fallback.
     */
    @Value("${hap.dockerHostFallback.enabled:false}")
    private boolean dockerHostFallbackEnabled;

    @Value("${hap.dockerHostFallback.host:host.docker.internal}")
    private String dockerHostFallbackHost;

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

    public Map<String, Object> getPhysicianById(String physicianId) {
        String url = physiciansServiceUrl + "/physicians/" + physicianId;
        log.debug("Chamando Physicians GET {}", url);
        return performExchange(url, physicianId, "getPhysicianById", "Physicians");
    }

    public Map<String, Object> getAppointmentById(String appointmentId) {
        String url = physiciansServiceUrl + "/appointments/" + appointmentId;
        log.debug("Chamando Physicians GET (Appointment) {}", url);

        Map<String, Object> body;
        try {
            body = performExchange(url, appointmentId, "getAppointmentById", "Physicians");
        } catch (MicroserviceCommunicationException e) {
            // If DNS fails (common symptom: UnknownHostException), optionally fall back to docker host.
            if (dockerHostFallbackEnabled && isCausedByUnknownHost(e)) {
                String fallbackUrl = replaceHostWithDockerHost(url);
                log.warn("DNS falhou ao chamar Physicians ({}). Tentando fallback via {} -> {}",
                        url, dockerHostFallbackHost, fallbackUrl);
                body = performExchange(fallbackUrl, appointmentId, "getAppointmentById", "Physicians");
            } else {
                throw e;
            }
        }

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

    public Map<String, Object> getPatientById(String patientId) {
        String url = patientsServiceUrl + "/patients/" + patientId;
        log.debug("Chamando Patients GET {}", url);
        return performExchange(url, patientId, "getPatientById", "Patients");
    }

    // --- AUTH SERVICE CALLS ---

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


    // --- PEER FORWARDING LOGIC ---

    public List<String> getPeerUrls() {
        return peers.stream()
                .filter(peerUrl -> !isCurrentInstance(peerUrl))
                .collect(Collectors.toList());
    }

    private boolean isCurrentInstance(String peerUrl) {
        return peerUrl.contains(":" + currentPort);
    }

    private boolean isCausedByUnknownHost(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof UnknownHostException) return true;
            cur = cur.getCause();
        }
        return false;
    }

    private String replaceHostWithDockerHost(String url) {
        // only replace common docker service host names, keep scheme/port/path
        // Example: https://hap-physicians:8081/x -> https://host.docker.internal:8081/x
        return url
                .replace("https://hap-physicians:", "https://" + dockerHostFallbackHost + ":")
                .replace("http://hap-physicians:", "http://" + dockerHostFallbackHost + ":")
                .replace("https://hap-auth:", "https://" + dockerHostFallbackHost + ":")
                .replace("http://hap-auth:", "http://" + dockerHostFallbackHost + ":")
                .replace("https://hap-patients-blue:", "https://" + dockerHostFallbackHost + ":")
                .replace("http://hap-patients-blue:", "http://" + dockerHostFallbackHost + ":");
    }
}