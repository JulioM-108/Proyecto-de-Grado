package Prototipo.Proyecto_Grado.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de la página principal del prototipo.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        // Redirige automáticamente al inicio de sesión como pantalla de entrada obligatoria
        return "redirect:/login";
    }
}


