package Prototipo.Proyecto_Grado.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la transferencia de mensajes en la interacción del chat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeDto {
    private String emisor; // "ESTUDIANTE" o "TUTOR"
    private String contenido;
    private LocalDateTime timestamp;
    private boolean sesionCompletada; // Indica si el tutor socrático dio por concluida la ruta de preguntas guía
    private boolean errorIa; // Ningún modelo respondió: no se persistió nada y el estudiante puede reintentar
}
