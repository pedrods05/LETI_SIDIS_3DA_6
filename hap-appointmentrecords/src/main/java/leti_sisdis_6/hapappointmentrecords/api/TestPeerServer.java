package leti_sisdis_6.hapappointmentrecords.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name = "hap.testing.enable-peer-server", havingValue = "true")
public class TestPeerServer {

    private static final Logger log = LoggerFactory.getLogger(TestPeerServer.class);
    private HttpServer server;

    @PostConstruct
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(8090), 0);
            server.createContext("/api/appointment-records", new AppointmentRecordHandler());
            server.setExecutor(null);
            server.start();
            log.info("TestPeerServer started on http://localhost:8090");
        } catch (BindException be) {
            // Port already in use — do not fail application startup, just log warning
            log.warn("TestPeerServer could not bind to port 8090 (already in use): {}", be.getMessage());
            server = null;
        } catch (IOException e) {
            // Log and continue without failing the whole app
            log.warn("TestPeerServer failed to start: {}", e.getMessage());
            server = null;
        }
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.stop(0);
            log.info("TestPeerServer stopped");
        }
    }

    static class AppointmentRecordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            // Expecting /api/appointment-records/{id}
            String[] parts = path.split("/");
            String id = parts.length >= 3 ? parts[parts.length - 1] : "unknown";
            String json = String.format("{\"recordId\":\"%s\",\"appointmentId\":\"a-%s\",\"physicianName\":\"Dr Mock\",\"diagnosis\":\"Mocked\",\"treatmentRecommendations\":\"None\",\"prescriptions\":\"None\"}", id, id);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
