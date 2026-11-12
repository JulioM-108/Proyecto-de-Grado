package Prototipo.Proyecto_Grado.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de la raíz del prototipo ({@code /}). No hay página de inicio: la raíz envía al login único.
 */
@Controller
public class HomeController {

    /** GET {@code /}: redirige a {@code /login}. */
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }
}


