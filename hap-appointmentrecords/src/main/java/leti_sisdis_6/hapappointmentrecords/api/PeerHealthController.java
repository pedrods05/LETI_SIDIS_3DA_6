package leti_sisdis_6.hapappointmentrecords.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/peers")
public class PeerHealthController {

    private final RestTemplate restTemplate;

    public PeerHealthController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${hap.physicians.base-url:http://localhost:8081}")
    private String physiciansBaseUrl;

    @Value("${hap.patients.base-url:http://localhost:8082}")
    private String patientsBaseUrl;

    @Value("${hap.auth.base-url:http://localhost:8084}")
    private String authBaseUrl;

    @GetMapping("/health")
    public ResponseEntity<List<Map<String, Object>>> health() {
        List<Map<String, Object>> results = Stream.of(
                Map.of("name", "physicians", "url", physiciansBaseUrl),
                Map.of("name", "patients", "url", patientsBaseUrl),
                Map.of("name", "auth", "url", authBaseUrl)
        ).map(peer -> check((String) peer.get("name"), (String) peer.get("url")))
         .toList();
        return ResponseEntity.ok(results);
    }

    private Map<String, Object> check(String name, String baseUrl) {
        String[] probes = new String[] {
                baseUrl + "/v3/api-docs",
                baseUrl + "/swagger-ui.html",
                baseUrl + "/",
        };
        for (String url : probes) {
            try {
                restTemplate.getForEntity(url, String.class);
                return Map.of(
                        "name", name,
                        "baseUrl", baseUrl,
                        "status", "UP",
                        "probe", url
                );
            } catch (Exception ignored) { }
        }
        return Map.of(
                "name", name,
                "baseUrl", baseUrl,
                "status", "DOWN"
        );
    }
}
