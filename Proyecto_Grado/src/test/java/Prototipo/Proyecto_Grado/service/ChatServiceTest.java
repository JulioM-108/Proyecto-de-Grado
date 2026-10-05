package Prototipo.Proyecto_Grado.service;

import Prototipo.Proyecto_Grado.model.dto.MensajeDto;
import Prototipo.Proyecto_Grado.model.entity.*;
import Prototipo.Proyecto_Grado.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final String CORREO = "estudiante@javerianacali.edu.co";
    private static final String ENLACE = "ABC123";
    private static final String RESPUESTA_CIERRE =
            "¡Felicitaciones! Consolidaste encapsulamiento y herencia.\n" + ChatService.ETIQUETA_RUTA_COMPLETADA;

    @Mock private GeminiService geminiService;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private SesionRepository sesionRepository;
    @Mock private ParticipacionRepository participacionRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private MetricaRepository metricaRepository;
    @Mock private ProfesorEstudianteRepository profesorEstudianteRepository;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ChatService chatService;

    private Participacion participacion;

    @BeforeEach
    void setUp() {
        Estudiante estudiante = Estudiante.builder().id(UUID.randomUUID()).nombre("Ana").correo(CORREO).build();
        Sesion sesion = Sesion.builder()
                .id(UUID.randomUUID())
                .tema("Encapsulamiento")
                .enlaceUnico(ENLACE)
                .preguntasGuia("1. ¿Qué es un atributo privado?\n2. ¿Para qué sirve un getter?\n\n")
                .estado(true)
                .build();
        participacion = Participacion.builder()
                .id(UUID.randomUUID())
                .estudiante(estudiante)
                .sesion(sesion)
                .build();

        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv ->
                inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        lenient().when(estudianteRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(estudiante));
        lenient().when(sesionRepository.findByEnlaceUnico(ENLACE)).thenReturn(Optional.of(sesion));
        lenient().when(participacionRepository.findFirstByEstudianteAndSesionOrderByCreatedAtDesc(estudiante, sesion))
                .thenReturn(Optional.of(participacion));
        lenient().when(participacionRepository.findById(participacion.getId())).thenReturn(Optional.of(participacion));
        lenient().when(mensajeRepository.findByParticipacionOrderByCreatedAtAsc(participacion)).thenReturn(List.of());
    }

    private void conTurnosPrevios(long turnos) {
        when(mensajeRepository.countByParticipacionAndEmisor(participacion, true)).thenReturn(turnos);
    }

    private void conRespuestaGemini(String texto, boolean auxiliar, boolean error) {
        when(geminiService.generarRespuestaDetallada(anyString()))
                .thenReturn(new GeminiService.RespuestaGemini(texto, auxiliar, error));
    }

    @Test
    @DisplayName("RUTA_COMPLETADA se ignora si el estudiante aún no tiene los turnos mínimos")
    void rutaCompletadaPrematuraSeIgnora() {
        conTurnosPrevios(0); // primer mensaje
        conRespuestaGemini(RESPUESTA_CIERRE, false, false);

        MensajeDto dto = chatService.procesarMensajeEstudiante(CORREO, ENLACE, "hola", null);

        assertFalse(dto.isSesionCompletada());
        assertFalse(participacion.getRutaCompletada());
        assertFalse(dto.getContenido().contains("RUTA_COMPLETADA"), "La etiqueta nunca debe llegar al estudiante");
    }

    @Test
    @DisplayName("RUTA_COMPLETADA se acepta con turnos suficientes y modelo principal")
    void rutaCompletadaValidaSeAcepta() {
        conTurnosPrevios(ChatService.MIN_TURNOS_CIERRE_RUTA - 1);
        conRespuestaGemini(RESPUESTA_CIERRE, false, false);

        MensajeDto dto = chatService.procesarMensajeEstudiante(CORREO, ENLACE, "Un getter expone el valor sin permitir modificarlo", null);

        assertTrue(dto.isSesionCompletada());
        assertTrue(participacion.getRutaCompletada());
        // El diagnóstico solo lo genera el docente manualmente
        verify(geminiService, never()).generarRespuesta(anyString());
    }

    @Test
    @DisplayName("La Capa 3 incluye todo el historial de la sesión y no el diagnóstico del docente")
    void capa3IncluyeHistorialCompletoSinDiagnostico() {
        participacion.setProgresoMd("## Conceptos dominados\n- DIAGNOSTICO_DOCENTE");
        List<Mensaje> historial = new java.util.ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            historial.add(Mensaje.builder().emisor(i % 2 == 1).contenido("turno-" + i).participacion(participacion).build());
        }
        when(mensajeRepository.findByParticipacionOrderByCreatedAtAsc(participacion)).thenReturn(historial);
        conTurnosPrevios(10);
        conRespuestaGemini("¿Qué opinas?", false, false);

        chatService.procesarMensajeEstudiante(CORREO, ENLACE, "sigo aquí", null);

        org.mockito.ArgumentCaptor<String> prompt = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(geminiService).generarRespuestaDetallada(prompt.capture());
        assertTrue(prompt.getValue().contains("turno-1\n"), "Debe incluir el primer turno de la sesión");
        assertTrue(prompt.getValue().contains("turno-20"));
        assertFalse(prompt.getValue().contains("DIAGNOSTICO_DOCENTE"));
    }

    @Test
    @DisplayName("RUTA_COMPLETADA se ignora si respondió el modelo auxiliar")
    void rutaCompletadaDelModeloAuxiliarSeIgnora() {
        conTurnosPrevios(10);
        conRespuestaGemini(RESPUESTA_CIERRE, true, false);

        MensajeDto dto = chatService.procesarMensajeEstudiante(CORREO, ENLACE, "listo", null);

        assertFalse(dto.isSesionCompletada());
        assertFalse(participacion.getRutaCompletada());
    }

    @Test
    @DisplayName("Si la IA falla no se persiste nada y el error se marca como reintentable")
    void errorDeIaNoPersisteNada() {
        conTurnosPrevios(2);
        conRespuestaGemini(GeminiService.MENSAJE_ERROR_TEMPORAL, false, true);

        MensajeDto dto = chatService.procesarMensajeEstudiante(CORREO, ENLACE, "¿qué es un objeto?", null);

        assertTrue(dto.isErrorIa());
        assertFalse(dto.isSesionCompletada());
        verify(mensajeRepository, never()).save(any());
        verify(metricaRepository, never()).save(any());
    }

    @Test
    @DisplayName("La señal ATAJO_DETECTADO del tutor registra la métrica RF-10 y no se muestra al estudiante")
    void senalDeAtajoDelTutorRegistraMetrica() {
        conTurnosPrevios(1);
        conRespuestaGemini("Entiendo tu prisa. ¿Qué crees que hace el ciclo?\n" + ChatService.ETIQUETA_ATAJO_DETECTADO, false, false);

        MensajeDto dto = chatService.procesarMensajeEstudiante(CORREO, ENLACE, "venga, ya, sácame de esto", null);

        verify(metricaRepository).save(argThat(m -> Long.valueOf(10L).equals(m.getCodigo())));
        assertFalse(dto.getContenido().contains("ATAJO_DETECTADO"));
        verify(mensajeRepository, times(2)).save(any()); // mensaje del estudiante + respuesta del tutor
    }

    @Test
    @DisplayName("Generar nuevo informe reemplaza el anterior con la conversación actual y no duplica las marcas")
    void regenerarDiagnosticoReemplazaElAnterior() {
        participacion.setProgresoMd("## Conceptos dominados\n- INFORME_VIEJO");
        lenient().doAnswer(inv -> { inv.<java.util.function.Consumer<Object>>getArgument(0).accept(null); return null; })
                .when(transactionTemplate).executeWithoutResult(any());
        when(geminiService.generarRespuesta(anyString())).thenReturn("## Conceptos dominados\n- INFORME_NUEVO");

        // Sin regenerar: devuelve el existente sin llamar a Gemini
        assertTrue(chatService.generarDiagnostico(participacion.getId(), false).contains("INFORME_VIEJO"));
        verify(geminiService, never()).generarRespuesta(anyString());

        // Regenerando: llama a Gemini y reemplaza
        String nuevo = chatService.generarDiagnostico(participacion.getId(), true);
        assertTrue(nuevo.contains("INFORME_NUEVO"));
        assertTrue(participacion.getProgresoMd().contains("INFORME_NUEVO"));
        verify(metricaRepository, never()).save(any()); // marcas 3 y 5 solo en el primer informe
    }

    @Test
    @DisplayName("contarPreguntasGuia ignora líneas vacías")
    void contarPreguntasGuia() {
        assertEquals(0, ChatService.contarPreguntasGuia(null));
        assertEquals(0, ChatService.contarPreguntasGuia("  \n "));
        assertEquals(2, ChatService.contarPreguntasGuia("1. A\n\n2. B\n"));
    }
}
