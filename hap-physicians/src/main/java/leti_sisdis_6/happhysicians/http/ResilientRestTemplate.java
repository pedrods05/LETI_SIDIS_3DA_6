package leti_sisdis_6.happhysicians.http;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ResilientRestTemplate {
    private final RestTemplate restTemplate;
    private final Map<String, LocalDateTime> failedPeers = new ConcurrentHashMap<>();

    private static final Duration COOLDOWN = Duration.ofSeconds(30);

    public ResilientRestTemplate() {
        this.restTemplate = new RestTemplate();
    }

    public ResilientRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
    }

    public <T> T getForObjectWithFallback(String url, Class<T> responseType) {
        if (isPeerHealthy(url)) {
            try {
                return restTemplate.getForObject(url, responseType);
            } catch (Exception e) {
                markPeerAsFailed(url);
                throw e;
            }
        }
        return null;
    }

    public <T> T getForObjectWithFallback(String url, HttpHeaders headers, Class<T> responseType) {
        if (isPeerHealthy(url)) {
            try {
                HttpEntity<Void> entity = new HttpEntity<>(headers != null ? headers : new HttpHeaders());
                ResponseEntity<T> resp = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
                return resp.getBody();
            } catch (Exception e) {
                markPeerAsFailed(url);
                throw e;
            }
        }
        return null;
    }

    private boolean isPeerHealthy(String url) {
        String key = peerKey(url);
        LocalDateTime failedAt = failedPeers.get(key);
        if (failedAt == null) return true;
        return LocalDateTime.now().isAfter(failedAt.plus(COOLDOWN));
    }

    private void markPeerAsFailed(String url) {
        failedPeers.put(peerKey(url), LocalDateTime.now());
    }

    private String peerKey(String url) {
        try {
            URI uri = new URI(url);
            String hostPart = uri.getScheme() + "://" + uri.getHost();
            int port = uri.getPort();
            if (port != -1) hostPart += ":" + port;
            return hostPart;
        } catch (URISyntaxException e) {
            // Fallback: use url prefix up to first path segment
            int idx = url.indexOf("/", url.indexOf("://") + 3);
            return idx > 0 ? url.substring(0, idx) : url;
        }
    }
}
