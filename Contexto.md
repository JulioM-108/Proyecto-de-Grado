# Contexto del Proyecto: Prototipo de Plataforma Web de Tutoría Inteligente Socrática

## 1. Información General del Proyecto
* **Título:** Prototipo de plataforma web para la identificación de falencias y la retroalimentación docente en procesos de aprendizaje asistido por un sistema de tutoría inteligente[cite: 1].
* **Institución:** Pontificia Universidad Javeriana Cali — Carrera de Ingeniería de Sistemas y Computación[cite: 1].
* **Autor:** Julio Alejandro Mazo Reyes[cite: 1].
* **Director:** MSc. Mario Julián Mora Cardona[cite: 1].

---

## 2. Descripción del Problema y Propósito del Sistema
En los cursos de programación (focalizado en Programación Orientada a Objetos - POO) existe una "brecha de retroalimentación" debido a la dificultad de los docentes para dar seguimiento individualizado a grupos grandes[cite: 1]. 

Esta plataforma resuelve ese problema integrando un **Sistema de Tutoría Inteligente (ITS)** impulsado por IA Generativa que interactúa con el estudiante mediante el **método socrático** (haciendo preguntas y dando analogías sin entregar la respuesta directa de código)[cite: 1]. A la par, el sistema procesa el desempeño del estudiante y genera métricas consolidadas en un **panel de control docente** para identificar falencias recurrentes en el grupo[cite: 1].

---

## 3. Stack Tecnológico de la Aplicación
* **Backend:** Java (OpenJDK 21) con **Spring Boot**[cite: 1].
* **Capa Web / Infraestructura:** `spring-boot-starter-web` (Spring MVC + Tomcat Embebido)[cite: 1].
* **Capa de Presentación:** **Thymeleaf** (renderizado del lado del servidor - SSR)[cite: 1].
* **Base de Datos & Persistencia:** **Supabase (PostgreSQL)**[cite: 1].
* **Motor de IA (LLM):** **Gemini 3 Flash** (Google) vía API REST HTTP[cite: 1].
* **Contenerización y Despliegue:** **Docker**[cite: 1] (listo para desplegar en VPS o servidor local).

---

## 4. Componentes y Módulos de la Plataforma
1. **Módulo de Tutoría Socrática (Estudiante):**
   * Interfaz de chat en tiempo real renderizada mediante Thymeleaf.
   * Envío de mensajes al backend donde Spring MVC procesa la consulta y la envía a la API de Gemini 3 Flash[cite: 1].
   * Guardado del historial de chat y estado del estudiante en Supabase[cite: 1].

2. **Módulo de Doble Verificación (RF-10 - Auditor de Respuestas):**
   * Mecanismo de control interno tras recibir la respuesta de Gemini 3 Flash[cite: 1].
   * Evalúa si la respuesta generada entregó la solución de código directa; de ser así, solicita una reformulación automática en formato socrático antes de enviar el HTML al cliente[cite: 1].

3. **Módulo de Dashboard / Analítica (Docente):**
   * Panel visual con métricas de desempeño, niveles de dominio y temas de mayor dificultad[cite: 1].
   * Configuración dinámica del tutor socrático mediante la estructura de prompting **CRAFT** (*Context, Role, Audience, Format, Tone*)[cite: 1].

---

## 5. Arquitectura de Prompts en 3 Capas
* **Capa 1 (Sistema / Fija):** Reglas globales del tutor socrático (nunca dar código completo, guiar mediante preguntas, adaptar el nivel cognitivo)[cite: 1].
* **Capa 2 (Docente / Configurable):** Parámetros CRAFT definidos por el profesor según la lección actual[cite: 1].
* **Capa 3 (Estudiante / Dinámica):** Contexto dinámico de la sesión actual + resumen de progreso heredado de sesiones anteriores almacenado en Supabase (`progreso_md`)[cite: 1].

---

## 6. Modelo de Persistencia en Supabase
* **Datos Cualitativos:** Historial de interacción y contexto guardados en formato Markdown (`progreso_md`)[cite: 1].
* **Datos Cuantitativos:** Métricas discretas, conteo de intentos, banderas de error recurrente y puntajes estructurados[cite: 1].

---

## 7. Flujo General de Ejecución de una Petición (Spring Boot + MVC + Thymeleaf)
1. **Petición HTTP:** El estudiante envía una duda desde el frontend HTML (`POST /chat/mensaje`).
2. **Spring Boot / Tomcat:** Recibe la petición en el puerto `8080` a través del contenedor de servlets embebido[cite: 1].
3. **DispatcherServlet (Spring MVC):** Intercepta la petición y la enruta a la clase `@Controller` encargada del chat[cite: 1].
4. **Capa de Servicios:**
   * Recupera el historial del estudiante desde **Supabase**[cite: 1].
   * Construye el prompt (Capas 1, 2 y 3) y llama a la API de **Gemini 3 Flash**[cite: 1].
   * Valida la regla socrática (RF-10)[cite: 1].
   * Persiste la nueva interacción y actualiza métricas en Supabase[cite: 1].
5. **ViewResolver & Thymeleaf:** El `@Controller` pasa los datos al `Model` y llama a la vista `chat.html`. Thymeleaf renderiza las respuestas en el HTML[cite: 1].
6. **Respuesta:** El servidor devuelve la página HTML procesada al cliente.