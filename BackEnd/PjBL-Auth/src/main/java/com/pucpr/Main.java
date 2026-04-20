package com.pucpr;

import com.pucpr.handlers.AuthHandler;
import com.pucpr.repository.UsuarioRepository;
import com.pucpr.service.JwtService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        UsuarioRepository repository = new UsuarioRepository();
        JwtService jwtService        = new JwtService();
        AuthHandler authHandler      = new AuthHandler(repository, jwtService);

        server.createContext("/api/auth/register", authHandler::handleRegister);
        server.createContext("/api/auth/login",    authHandler::handleLogin);
        server.createContext("/api/auth/logout",   authHandler::handleLogout);

        server.setExecutor(null);
        System.out.println("Servidor iniciado na porta 8080...");
        server.start();
    }
}
