package com.pucpr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Representa a entidade de usuário no sistema.
 * Esta classe é compatível com a biblioteca Jackson para persistência em JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usuario {

    private String id;
    private String nome;
    private String email;

    // Nomeamos como senhaHash para reforçar o conceito de segurança
    @JsonProperty("senhaHash")
    private String senhaHash;

    private String role; // Ex: "PACIENTE", "MEDICO", "ADMIN"

    // Construtor padrão necessário para o Jackson
    public Usuario() {}

    public Usuario(String nome, String email, String senhaHash, String role) {
        this.id = UUID.randomUUID().toString(); // Gera um ID único automaticamente
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.role = role;
    }

    // --- Getters e Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * IMPORTANTE: Nunca retorne a senha em texto claro.
     * O campo armazena apenas o hash (BCrypt).
     */
    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "Usuario{email='" + email + "', role='" + role + "'}";
    }
}