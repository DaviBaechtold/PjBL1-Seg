package com.pucpr.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {
    private final String FILE_PATH = "usuarios.json";
    private final ObjectMapper mapper = new ObjectMapper();

    public Optional<Usuario> findByEmail(String email) {
        return findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public List<Usuario> findAll() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(file, new TypeReference<List<Usuario>>(){});
        } catch (IOException e) {
            System.err.println("Erro ao ler usuarios.json: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void save(Usuario usuario) throws IOException {
        List<Usuario> usuarios = findAll();
        boolean emailJaExiste = usuarios.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(usuario.getEmail()));
        if (emailJaExiste) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }
        usuarios.add(usuario);
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), usuarios);
    }
}
