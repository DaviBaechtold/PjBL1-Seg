package com.pucpr.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;
import com.pucpr.repository.UsuarioRepository;
import com.pucpr.service.JwtService;
import com.sun.net.httpserver.HttpExchange;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;

public class AuthHandler {
    private final UsuarioRepository repository;
    private final JwtService jwtService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthHandler(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    public void handleLogin(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            byte[] body = exchange.getRequestBody().readAllBytes();
            Map<?, ?> req = mapper.readValue(body, Map.class);
            String email    = (String) req.get("email");
            String password = (String) req.get("password");

            if (email == null || password == null) {
                sendJson(exchange, 400, Map.of("message", "E-mail e senha são obrigatórios."));
                return;
            }

            Optional<Usuario> optUser = repository.findByEmail(email);

            // Mensagem genérica — nunca revela qual campo está errado (anti-enumeração)
            if (optUser.isEmpty() || !BCrypt.checkpw(password, optUser.get().getSenhaHash())) {
                sendJson(exchange, 401, Map.of("message", "E-mail ou senha inválidos."));
                return;
            }

            String token = jwtService.generateToken(optUser.get());
            sendJson(exchange, 200, Map.of("token", token));

        } catch (Exception e) {
            System.err.println("Erro no login: " + e.getMessage());
            sendJson(exchange, 500, Map.of("message", "Erro interno do servidor."));
        }
    }

    public void handleRegister(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            byte[] body = exchange.getRequestBody().readAllBytes();
            Map<?, ?> req = mapper.readValue(body, Map.class);
            String name     = (String) req.get("name");
            String email    = (String) req.get("email");
            String password = (String) req.get("password");

            if (name == null || email == null || password == null) {
                sendJson(exchange, 400, Map.of("message", "Campos obrigatórios ausentes."));
                return;
            }

            if (repository.findByEmail(email).isPresent()) {
                sendJson(exchange, 400, Map.of("message", "E-mail já está em uso."));
                return;
            }

            // Nunca armazenar senha em texto claro — BCrypt com fator de custo 12
            String senhaHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            Usuario novo = new Usuario(name, email, senhaHash, "PACIENTE");
            repository.save(novo);

            sendJson(exchange, 201, Map.of("message", "Usuário criado com sucesso."));

        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Erro no cadastro: " + e.getMessage());
            sendJson(exchange, 500, Map.of("message", "Erro interno do servidor."));
        }
    }

    public void handleLogout(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        sendJson(exchange, 200, Map.of("message", "Logout realizado com sucesso."));
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] json = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, json.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json);
        }
    }
}
