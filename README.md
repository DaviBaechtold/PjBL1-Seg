# PjBL — Autenticação Segura com JWT

> Projeto da disciplina **Segurança da Informação** — Prof. Mateus Nunes  
> API REST em **Java SE Nativo** (sem frameworks) com autenticação JWT, hashing BCrypt e persistência em JSON.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Segurança](#segurança)
- [Pré-requisitos](#pré-requisitos)
- [Como executar](#como-executar)
- [Endpoints](#endpoints)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Perguntas do Relatório](#perguntas-do-relatório)

---

## Visão Geral

O sistema implementa o fluxo completo de autenticação:

1. **Cadastro** — recebe nome, e-mail e senha; armazena o hash BCrypt no arquivo `usuarios.json`
2. **Login** — valida credenciais com `BCrypt.checkpw` e emite um JWT assinado com HS256
3. **Proteção de rotas** — o token é validado a cada requisição; assinatura inválida ou expiração são rejeitadas

O frontend fornecido (HTML/JS puro) se conecta a `http://localhost:8080` e armazena o token **apenas em memória** (padrão IIFE), nunca em `localStorage`.

---

## Arquitetura

```
src/main/java/com/pucpr/
├── Main.java                  # Servidor HTTP, registro das rotas
├── handlers/
│   └── AuthHandler.java       # Controller: /register, /login, /logout
├── model/
│   └── Usuario.java           # Entidade persistida no JSON
├── repository/
│   └── UsuarioRepository.java # Leitura e escrita em usuarios.json (Jackson)
└── service/
    └── JwtService.java        # Geração, validação e proteção alg:none
```

### Fluxo de uma requisição

```
Cliente (Frontend)
      │
      │  POST /api/auth/login  { email, password }
      ▼
 AuthHandler.handleLogin()
      │
      ├─ repository.findByEmail()  →  lê usuarios.json
      │
      ├─ BCrypt.checkpw(senhaInformada, hash)
      │
      └─ jwtService.generateToken()  →  200 OK  { token: "eyJ..." }
```

---

## Segurança

### Hashing de senhas (BCrypt)

```java
// Cadastro — fator de custo 12
String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

// Login — nunca .equals(), sempre checkpw
BCrypt.checkpw(senhaInformada, usuario.getSenhaHash());
```

O fator 12 significa 2¹² = 4096 iterações, tornando ataques de força bruta computacionalmente inviáveis. O salt é gerado aleatoriamente a cada cadastro e está embutido no próprio hash (`$2a$12$...`), eliminando a vulnerabilidade de *Rainbow Tables*.

### JWT com HS256

O token carrega as claims:

| Claim | Valor | Descrição |
|-------|-------|-----------|
| `sub` | e-mail | Identificador do usuário |
| `role` | PACIENTE | Perfil de acesso |
| `name` | nome | Nome completo |
| `iat` | timestamp | Emitido em |
| `exp` | iat + 15 min | Expira em |

A chave secreta é lida da **variável de ambiente** `JWT_SECRET` — nunca do código-fonte.

### Proteção contra alg:none (CVE-2015-9235)

O ataque `alg:none` consiste em forjar um JWT com `{"alg":"none"}` no header e remover a assinatura, fazendo servidores vulneráveis aceitarem o token sem verificação.

Nossa defesa opera em **duas camadas independentes**:

**Camada 1 — verificação manual do header (antes de qualquer parse):**
```java
private void rejectUnsecuredToken(String token) {
    String headerJson = decodeBase64url(token.split("\\.")[0]);
    String lower = headerJson.toLowerCase();
    if (lower.contains("\"alg\"") && lower.contains("none")) {
        throw new SecurityException("alg:none não é permitido.");
    }
    if (!lower.contains("\"hs256\"")) {
        throw new SecurityException("Algoritmo de assinatura não aceito.");
    }
}
```

**Camada 2 — JJWT 0.12.5 `parseSignedClaims()`** rejeita automaticamente tokens não assinados (JWU), pois espera exclusivamente um JWS.

### Mensagens de erro genéricas (anti-enumeração)

Em caso de falha no login, a API sempre retorna `"E-mail ou senha inválidos."` — independentemente de qual dos dois campos está errado. Isso impede o ataque de *User Enumeration*, onde um atacante descobre quais e-mails estão cadastrados observando mensagens de erro diferentes.

### Token armazenado em memória (IIFE)

```javascript
const AuthStore = (() => {
  let _token = null;           // variável privada — inacessível de fora
  return {
    set(t)  { _token = t; },
    get()   { return _token; },
    clear() { _token = null; }
  };
})();
```

A variável `_token` existe apenas no escopo da IIFE. Ao fechar a aba, o token é descartado automaticamente, eliminando a superfície de ataque de *XSS + localStorage*.

---

## Pré-requisitos

- Java 17+
- Maven (ou use o wrapper `./mvnw` incluído no projeto)

---

## Como executar

### 1. Definir a variável de ambiente

```bash
# Linux / macOS
export JWT_SECRET="sua_chave_secreta_com_no_minimo_32_chars"

# Windows (PowerShell)
$env:JWT_SECRET = "sua_chave_secreta_com_no_minimo_32_chars"
```

### 2. Compilar e empacotar

```bash
./mvnw clean package
```

### 3. Iniciar o servidor

```bash
java -cp "target/PjBL-Auth.jar:target/libs/*" com.pucpr.Main
# Servidor iniciado na porta 8080...
```

### 4. Abrir o frontend

Abra o arquivo `../../Frontend/index.html` diretamente no navegador.

---

## Endpoints

| Método | Rota | Descrição | Auth |
|--------|------|-----------|------|
| POST | `/api/auth/register` | Cadastrar novo usuário | Não |
| POST | `/api/auth/login` | Autenticar e obter JWT | Não |
| POST | `/api/auth/logout` | Invalidar sessão | Bearer Token |

### POST /api/auth/register

**Request:**
```json
{ "name": "Maria Silva", "email": "maria@email.com", "password": "Senha123!" }
```

**Responses:**
```json
// 201 Created
{ "message": "Usuário criado com sucesso." }

// 400 Bad Request
{ "message": "E-mail já está em uso." }
```

### POST /api/auth/login

**Request:**
```json
{ "email": "maria@email.com", "password": "Senha123!" }
```

**Responses:**
```json
// 200 OK
{ "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIi..." }

// 401 Unauthorized
{ "message": "E-mail ou senha inválidos." }
```

---

## Estrutura do projeto

```
PjBL-Auth/
├── src/main/java/com/pucpr/
│   ├── Main.java
│   ├── handlers/AuthHandler.java
│   ├── model/Usuario.java
│   ├── repository/UsuarioRepository.java
│   └── service/JwtService.java
├── usuarios.json          # Banco de dados em arquivo JSON
├── pom.xml
└── README.md
```

---

## Perguntas do Relatório

### 1. Por que não usar `String.equals()` para comparar senhas?

`String.equals()` compara caracteres um a um e retorna `false` assim que encontra uma diferença — isso torna a operação vulnerável a **ataques de temporização** (*timing attacks*): um atacante pode medir microsegundos de diferença nas respostas para deduzir o hash armazenado.

Além disso, a senha armazenada não é a senha original, mas sim seu **hash BCrypt** (`$2a$12$...`). O BCrypt é uma função de hash unidirecional: não é possível reverter o hash para obter a senha. O `BCrypt.checkpw()` refaz o processo de hash sobre a senha informada (usando o mesmo salt embutido no hash) e compara os resultados de forma segura e em tempo constante.

### 2. Consequência de um atacante obter a SECRET_KEY

A `SECRET_KEY` é usada para assinar e verificar a assinatura HMAC-SHA256 dos tokens. Se um atacante a obtiver, ele pode:
- **Forjar tokens** para qualquer usuário/role sem precisar de credenciais
- **Assumir identidade de administrador** simplesmente gerando um JWT com `"role":"ADMIN"`
- Os tokens forjados passarão em todas as verificações do servidor

Por isso a chave é carregada via `System.getenv("JWT_SECRET")` e nunca deve ser versionada no repositório.

### 3. Ataque de Enumeração de Usuários

O ataque consiste em enviar tentativas de login e analisar a mensagem de erro: se o sistema responde `"Usuário não encontrado"` para e-mails inválidos e `"Senha incorreta"` para e-mails válidos, o atacante consegue mapear quais contas existem para depois aplicar força bruta apenas nelas.

Nossa implementação retorna sempre `"E-mail ou senha inválidos."` em ambos os casos, eliminando essa distinção. O código trata os dois cenários com o mesmo caminho de execução:
```java
if (optUser.isEmpty() || !BCrypt.checkpw(password, optUser.get().getSenhaHash())) {
    sendJson(exchange, 401, Map.of("message", "E-mail ou senha inválidos."));
}
```

### 4. Codificação (Base64) vs Assinatura (HMAC)

**Base64url** é apenas uma *codificação* — transforma bytes em texto legível. É **reversível**: qualquer pessoa pode decodificar o header e o payload de um JWT sem qualquer chave. Isso significa que o payload é **público**.

**HMAC-SHA256** é uma *assinatura criptográfica* — garante que o conteúdo não foi alterado. Sem a `SECRET_KEY`, é computacionalmente impossível gerar uma assinatura válida.

Armazenar uma senha no payload seria um erro grave: como o payload é apenas Base64url, qualquer pessoa que intercepte o token pode ler a senha em texto claro, sem precisar de nenhuma chave.

### 5. Token em memória vs localStorage

`localStorage` persiste entre sessões e é acessível via `document.localStorage` em qualquer script JavaScript da página. Um ataque **XSS** (*Cross-Site Scripting*) — injeção de código JS malicioso — pode simplesmente executar `localStorage.getItem('token')` e enviar o token para um servidor do atacante.

Armazenando o token em uma variável de closure (IIFE), ele fica inacessível fora do escopo da função. Scripts injetados não conseguem referenciá-lo diretamente. Além disso, ao fechar a aba, a variável é destruída, eliminando tokens "esquecidos" no navegador.
