package com.pucpr.handlers;

import com.pucpr.repository.UsuarioRepository;
import com.pucpr.service.JwtService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/**
 * Classe responsável por gerenciar as requisições de Autenticação.
 * Aqui o aluno aprenderá a manipular o corpo de requisições HTTP e
 * aplicar conceitos de hashing e proteção de dados.
 */
public class AuthHandler {
    private final UsuarioRepository repository;
    private final JwtService jwtService;

    public AuthHandler(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    /**
     * Gerencia o processo de Login.
     * Objetivo: Validar credenciais e emitir um passaporte (JWT).
     */
    public void handleLogin(HttpExchange exchange) throws IOException {
        // DICA DIDÁTICA: Em APIs REST, o Login sempre deve ser POST para
        // garantir que a senha viaje no corpo (body) e não na URL.
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            return;
        }

        // TODO: O ALUNO DEVE IMPLEMENTAR OS SEGUINTES PASSOS:

        // 1. EXTRAÇÃO: Use exchange.getRequestBody() para ler os bytes do JSON enviado.
        // 2. CONVERSÃO: Transforme esse JSON em um objeto (ex: LoginRequest) usando Jackson.

        // 3. BUSCA E SEGURANÇA:
        //    a) Busque o usuário no 'repository' pelo e-mail fornecido.
        //    b) Se existir, use BCrypt.checkpw(senhaInformada, senhaDoArquivo) para validar.

        // 4. REGRA DE OURO DA SEGURANÇA:
        //    - NUNCA use .equals() ou == para comparar senhas. O BCrypt é a sugestão.
        //    - Em caso de falha, retorne uma mensagem GENÉRICA (ex: "E-mail ou senha inválidos").
        //      Revelar qual dos dois está errado ajuda atacantes em técnicas de enumeração.

        // 5. RESPOSTA:
        //    - Se as credenciais estiverem OK: Gere o Token via jwtService e retorne 200 OK.
        //    - Se falhar: Retorne 401 Unauthorized com o JSON de erro.
    }

    /**
     * Gerencia o processo de Cadastro (Registro).
     * Objetivo: Criar um novo usuário de forma segura.
     */
    public void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // TODO: O ALUNO DEVE IMPLEMENTAR OS SEGUINTES PASSOS:

        // 1. VALIDAÇÃO DE EXISTÊNCIA:
        //    Antes de cadastrar, verifique se o e-mail já está em uso no 'repository'.
        //    Se já existir, interrompa e retorne 400 Bad Request.

        // 2. CRIPTOGRAFIA (Hashing):
        //    A senha recebida NUNCA deve chegar ao arquivo em texto claro.
        //    Gere o hash: BCrypt.hashpw(senhaPura, BCrypt.gensalt(12)).
        //    O "salt" (fator 12) protege contra ataques de Rainbow Tables.

        // 3. PERSISTÊNCIA:
        //    Crie uma nova instância de Usuario (model) com a senha já HASHEADA.
        //    Use o repository.save(novoUsuario) para gravar no arquivo JSON.

        // 4. RESPOSTA: Se tudo der certo, retorne 201 Created.
    }
}