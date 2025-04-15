package Backend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class BinaryDecoderServer {

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/decode", new DecodeHandler());
        server.createContext("/api/encode", new EncodeHandler());
        server.createContext("/api/health", new HealthHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Servidor rodando na porta " + port);
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configura CORS
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String response = "Servidor do decodificador binário está funcionando!";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length());

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    static class DecodeHandler implements HttpHandler {
        private static final Pattern BINARY_PATTERN = Pattern.compile("^[01\\s]+$");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configura CORS
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Método não permitido");
                return;
            }

            try {
                InputStream requestBody = exchange.getRequestBody();
                String requestText = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);
                String binaryInput = parseInput(requestText, "binary");
                String decodedText = decodeBinary(binaryInput);

                String response = String.format(
                        "{\"text\": \"%s\", \"success\": true, \"message\": \"Decodificado com sucesso\"}",
                        escapeJson(decodedText));

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

            } catch (Exception e) {
                sendError(exchange, 400, "Erro ao processar: " + e.getMessage());
            }
        }

        private String decodeBinary(String binary) throws IllegalArgumentException {
            if (binary == null || binary.trim().isEmpty()) {
                throw new IllegalArgumentException("Entrada vazia");
            }

            if (!BINARY_PATTERN.matcher(binary).matches()) {
                throw new IllegalArgumentException("Entrada inválida - use apenas 0, 1 e espaços");
            }

            String[] bytes = binary.trim().split("\\s+");
            StringBuilder text = new StringBuilder();

            for (String byteStr : bytes) {
                if (byteStr.length() != 8) {
                    throw new IllegalArgumentException("Byte inválido: " + byteStr + " (deve ter 8 dígitos)");
                }
                try {
                    int charCode = Integer.parseInt(byteStr, 2);
                    if (charCode >= 32 && charCode <= 126) {
                        text.append((char) charCode);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Byte inválido: " + byteStr);
                }
            }

            return text.toString();
        }
    }

    static class EncodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configura CORS
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Método não permitido");
                return;
            }

            try {
                InputStream requestBody = exchange.getRequestBody();
                String requestText = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);
                String textInput = parseInput(requestText, "text");
                String binaryOutput = encodeText(textInput);

                String response = String.format(
                        "{\"binary\": \"%s\", \"success\": true, \"message\": \"Codificado com sucesso\"}",
                        escapeJson(binaryOutput));

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

            } catch (Exception e) {
                sendError(exchange, 400, "Erro ao processar: " + e.getMessage());
            }
        }

        private String encodeText(String text) throws IllegalArgumentException {
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Entrada vazia");
            }

            StringBuilder binary = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c > 255) {
                    throw new IllegalArgumentException("Caractere não suportado: " + c);
                }
                String binaryChar = Integer.toBinaryString(c);
                binary.append(String.format("%8s", binaryChar).replace(' ', '0')).append(" ");
            }

            return binary.toString().trim();
        }
    }

    // Métodos utilitários compartilhados
    private static String parseInput(String requestText, String fieldName) {
        if (requestText.startsWith("{")) {
            int start = requestText.indexOf("\"" + fieldName + "\":\"") + fieldName.length() + 4;
            int end = requestText.indexOf("\"", start);
            if (start >= fieldName.length() + 4 && end > start) {
                return requestText.substring(start, end);
            }
        }
        return requestText.trim();
    }

    private static String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String response = String.format("{\"success\": false, \"message\": \"%s\"}", escapeJson(message));
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.length());

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}