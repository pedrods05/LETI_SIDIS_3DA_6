package leti_sisdis_6.hapauth.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "hap.peers")
@Data
public class PeerConfig {
    
    /**
     * List of peer URLs for HTTP forwarding
     * Default values for development/testing
     */
    private List<String> urls = Arrays.asList(
        "http://localhost:8085", // instance2
        "http://localhost:8086"  // instance3
    );
    
    /**
     * Current instance URL to exclude from peer list
     */
    private String currentInstanceUrl;
    
    /**
     * Timeout for peer HTTP calls in milliseconds
     */
    private int timeoutMs = 5000;
    
    /**
     * Maximum number of retries for peer calls
     */
    private int maxRetries = 2;
    
    /**
     * Enable/disable peer forwarding
     */
    private boolean enabled = true;
    
    /**
     * Get peer URLs excluding current instance
     */
    public List<String> getActivePeers() {
        if (currentInstanceUrl == null || currentInstanceUrl.isEmpty()) {
            return urls;
        }
        return urls.stream()
                .filter(url -> !url.equals(currentInstanceUrl))
                .toList();
    }
}
