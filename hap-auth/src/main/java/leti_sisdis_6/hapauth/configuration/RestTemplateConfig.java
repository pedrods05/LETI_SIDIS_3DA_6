package leti_sisdis_6.hapauth.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(PeerConfig peerConfig) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(peerConfig.getTimeoutMs());
        factory.setReadTimeout(peerConfig.getTimeoutMs());
        
        return new RestTemplate(factory);
    }
}
