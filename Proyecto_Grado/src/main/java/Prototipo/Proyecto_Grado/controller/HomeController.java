package Prototipo.Proyecto_Grado.controller;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * Controlador de la página principal del prototipo.
 */
@Controller
public class HomeController {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/")
    public String index() {
        // Redirige automáticamente al inicio de sesión como pantalla de entrada obligatoria
        return "redirect:/login";
    }

    @GetMapping("/api/db-tables")
    @ResponseBody
    public List<Map<String, Object>> verTablas() {
        if (jdbcTemplate == null) return List.of(Map.of("error", "JdbcTemplate no disponible"));
        return jdbcTemplate.queryForList(
            "SELECT table_schema, table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name"
        );
    }
}


