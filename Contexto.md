# Contexto del Proyecto: Prototipo de Plataforma Web de Tutoría Inteligente Socrática

## 1. Información General del Proyecto
* **Título:** Prototipo de plataforma web para la identificación de falencias y la retroalimentación docente en procesos de aprendizaje asistido por un sistema de tutoría inteligente.
* **Institución:** Pontificia Universidad Javeriana Cali — Facultad de Ingeniería.
* **Programa Académico:** Carrera de Ingeniería de Sistemas y Computación.
* **Autor:** Julio Alejandro Mazo Reyes.
* **Director de Tesis:** MSc. Mario Julián Mora Cardona.
* **Fecha de Documentación Base:** Mayo de 2026.
* **Tipo de Estudio:** Proyecto aplicado con enfoque de diseño de ingeniería (investigación aplicada orientada a la construcción y evaluación de un prototipo de software funcional).

---

## 2. Descripción y Planteamiento del Problema

### 2.1. Planteamiento del Problema y Brecha de Retroalimentación
En la educación superior en ingeniería, la enseñanza de fundamentos y programación avanzada (focalizada en **Programación Orientada a Objetos - POO**) presenta retos cognitivos considerables debido a la alta abstracción requerida (clases, encapsulamiento, polimorfismo, herencia, asignación de memoria). Tradicionalmente, los docentes enfrentan cursos numerosos, lo que desencadena una **"brecha de retroalimentación" (*feedback gap*)**: la imposibilidad práctica del docente de acompañar individualmente los ciclos de ensayo y error de cada estudiante en tiempo real.

Por otro lado, los asistentes automatizados tradicionales o autocalificadores sintácticos suelen incurrir en dos fallas pedagógicas:
1. **Respuesta directa prematura (*Correct Response - CR*):** Entregan el código corregido o la solución directa, entrenando al estudiante para "responder la pregunta" o copiar la sintaxis, en lugar de internalizar el modelo mental (*falsa retención*).
2. **Falta de visibilidad docente:** La interacción entre el estudiante y las herramientas automáticas o IAs comerciales suele ser una caja negra aislada, dejando al profesor sin métricas agrupadas para detectar conceptos críticos donde el grupo flaquea.

### 2.2. Formulación del Problema
> **Pregunta Principal de Investigación:**  
> *¿Cómo desarrollar un prototipo de plataforma web que permita identificar falencias en el aprendizaje y generar retroalimentación mediante un sistema de tutoría inteligente, facilitando el seguimiento de los procesos de aprendizaje de los estudiantes de programación?*

### 2.3. Sistematización del Problema (Subpreguntas de Investigación)
1. ¿Qué requisitos funcionales y métricas de uso son necesarios para desarrollar una plataforma web que permita a los docentes realizar el seguimiento al aprendizaje y al comportamiento de los estudiantes durante el proceso de aprendizaje?
2. ¿Cómo diseñar la plataforma web mediante la estructuración de sus módulos a través de un marco visual de arquitectura?
3. ¿Cómo implementar una plataforma web que integre un sistema de tutoría inteligente (ITS) capaz de guiar la enseñanza de conceptos de programación y gestionar hilos de conversación persistentes para adaptar el progreso educativo al ritmo del estudiante?
4. ¿Cómo evaluar la usabilidad y aceptación del prototipo mediante encuestas de percepción aplicadas a sus usuarios?

---

## 3. Objetivos del Proyecto

### 3.1. Objetivo General
Desarrollar un prototipo de plataforma web para la identificación de falencias en el aprendizaje y generación de retroalimentación mediante un sistema de tutoría inteligente socrático, que facilite el seguimiento del proceso de aprendizaje de los estudiantes de programación por parte de los docentes.

### 3.2. Objetivos Específicos
1. **Identificar** los requisitos funcionales y las métricas de uso de la plataforma que permitan a los docentes el seguimiento al aprendizaje de los estudiantes de programación y su comportamiento durante el proceso de aprendizaje.
2. **Diseñar** la plataforma web, mediante la estructuración de sus módulos utilizando el modelo C4 como marco de visualización arquitectónica.
3. **Implementar** el prototipo de la plataforma web con un sistema de tutoría inteligente socrático basado en IA, configurado mediante técnicas de prompting, para el apoyo de las funciones de seguimiento y generación de retroalimentación pedagógica.
4. **Evaluar** la usabilidad y aceptación del prototipo mediante la realización de encuestas de percepción sobre la plataforma.

---

## 4. Justificación, Alcances y Delimitaciones

### 4.1. Justificación
* **Pedagógica:** Fomentar el aprendizaje significativo mediante el **método socrático** (haciendo preguntas reflexivas, señalando inconsistencias lógicas y ofreciendo analogías sin dar código directo), reduciendo la frustración y quebrando la ilusión de falsa retención de conocimientos.
* **Docente:** Dotar al profesor de una herramienta analítica que consolida información de errores frecuentes individuales y colectivos, permitiéndole reorientar sus clases presenciales hacia los vacíos conceptuales reales del grupo.
* **Técnica:** Demostrar cómo una arquitectura estructurada en capas (Spring Boot, Supabase y Gemini API) combinada con ingeniería de prompts gobernada por frameworks educativos (**CRAFT**) resuelve de forma costo-eficiente el seguimiento educativo sin requerir entrenar modelos fundacionales desde cero.

### 4.2. Delimitaciones y Restricciones
* **Enfoque de IA:** No contempla entrenamiento ni fine-tuning desde cero de LLMs; utiliza APIs de modelos comerciales de última generación parametrizados estrictamente mediante prompting multi-capa.
* **Dominio Temático de Prueba:** Enfocado en **Programación Orientada a Objetos (POO)** por su abstracción y pertinencia curricular.
* **Rol del Sistema:** Herramienta de apoyo pedagógico y diagnóstico formativo; **no emite notas ni calificaciones sumativas definitivas**, respetando la autonomía evaluativa del docente.
* **Población de Evaluación:** Pruebas controladas de usabilidad y aceptación con estudiantes y docentes de la Pontificia Universidad Javeriana Cali.

### 4.3. Entregables del Proyecto
1. **Prototipo funcional:** Aplicación web desarrollada en Java Spring Boot con frontend en Thymeleaf y contenedor Docker.
2. **Documentación de arquitectura C4:** Diagramas formales de Contexto, Contenedores, Componentes y Código/Base de Datos.
3. **Repositorio de código:** Repositorio en GitHub con la lógica backend, scripts de base de datos en Supabase y archivos Dockerfile / docker-compose.
4. **Batería de prompts de prueba:** Prompts validados de POO para pruebas del tutor socrático.
5. **Guía de gestión pedagógica docente:** Manual de usuario para que el profesor configure lecciones con CRAFT e interprete las analíticas.
6. **Resultados de evaluación:** Informe de análisis de encuestas de percepción y usabilidad.

---

## 5. Metodología de Desarrollo y Fases de Trabajo

El proyecto adopta un **modelo incremental y evolutivo**, organizado en cuatro fases vinculadas con el ciclo de vida de diseño de ingeniería:

```
[Fase 1: Requisitos y Métricas]
  ├── 1.1 Identificación de requisitos funcionales y no funcionales
  ├── 1.2 Determinación de métricas de seguimiento pedagógico
  └── 1.3 Definición de flujos de datos y persistencia
           │
           ▼
[Fase 2: Diseño de Arquitectura C4 e Interfaces]
  ├── 2.1 Diagramas de Contexto y Contenedores
  ├── 2.2 Diagramas de Componentes y Código
  ├── 2.3 Esquema relacional de Base de Datos (Supabase)
  └── 2.4 Prototipado de interfaces de usuario (Chat y Dashboard)
           │
           ▼
[Fase 3: Implementación del Prototipo e Integración de IA]
  ├── 3.1 Evaluación y selección de API de IA (Gemini 3 Flash)
  ├── 3.2 Configuración de infraestructura y entorno Docker
  ├── 3.3 Verificación de comunicación Backend - Frontend - API
  ├── 3.4 Desarrollo del Panel de Configuración de Prompts (CRAFT)
  ├── 3.5 Construcción del Panel de Métricas Docente (Analítica)
  ├── 3.6 Implementación del Chat Socrático del Estudiante
  └── 3.7 Implementación del filtro auditor de respuestas directas (RF-10)
           │
           ▼
[Fase 4: Evaluación del Prototipo]
  ├── 4.1 Aplicación de encuestas de percepción y usabilidad
  └── 4.2 Procesamiento de datos y consolidación de mejoras futuras
```

---

## 6. Marco Teórico y Fundamentos Pedagógicos

### 6.1. Taxonomía del Feedback en la Enseñanza de Programación
A partir de la revisión de literatura (Evans, 2013; Castellanos et al., 2026; Gusukuma et al., 2018), la plataforma clasifica el feedback en:
* **Verificación (V):** Notifica únicamente si la respuesta o código es correcto o incorrecto (insuficiente para el aprendizaje profundo).
* **Respuesta Correcta (CR):** Muestra el código de referencia solucionado. *Peligro pedagógico:* Si se da prematuramente, desmotiva la reflexión y genera dependencia.
* **Retroalimentación Elaborada (E):** Explica el "porqué" y orienta el proceso reflexivo. Sus subtipos esenciales son:
  * **Aislamiento de Atributos (AT):** Dirige la atención cognitiva hacia un componente específico del problema (ej. "Revisa la jerarquía de herencia entre las clases").
  * **Pistas o Señales (*Hints/Cues* - HC):** Mensajes orientadores que invitan a reflexionar sin dar la solución (ej. "¿Qué sucede con el atributo privado si no defines un método de acceso?").
* **Retroalimentación Basada en Conceptos Erróneos (*Misconception Feedback* - BM):** Distingue entre un simple "error de código" (sintaxis rota) y un "concepto erróneo" (modelo mental defectuoso). Busca desarmar la falla cognitiva (ej. confundir un contador con un acumulador, o sobreescritura con sobrecarga).

### 6.2. Componentes de un Sistema de Tutoría Inteligente (ITS)
Un ITS constructivista incorpora:
1. **Modelo del Dominio:** El conocimiento a impartir (conceptos de POO).
2. **Modelo del Tutor:** Estrategias pedagógicas (método socrático, andamiaje/scaffolding, secuencias de pistas de puntero y didácticas).
3. **Modelo del Estudiante:** Representación de lo que el alumno comprende, sus errores persistentes y ritmo de avance.
4. **Interfaz de Usuario:** Interacción conversacional intuitiva y accesible.

### 6.3. Framework CRAFT para Prompting Pedagógico
Frente a otros marcos como CO-STAR (orientado a tareas genéricas de NLP) o RCFOR, la plataforma adopta **CRAFT** (Joshi et al., 2026) en la configuración docente por haber sido diseñado y validado empíricamente con profesores sin experiencia técnica previa:
* **C - Context (Contexto):** Asignatura, nivel académico y tema concreto de la sesión (ej. Clases y Objetos en Java).
* **R - Role (Rol):** Identidad pedagógica del agente (ej. "Tutor socrático empático y analítico").
* **A - Audience (Audiencia):** Perfil de los alumnos (ej. "Estudiantes de tercer semestre con dificultades en modelado abstracto").
* **F - Format (Formato):** Estructura de la interacción (respuestas conversacionales concisas, preguntas reflexivas breves).
* **T - Tone (Tono):** Actitud del tutor (motivador, constructivo, paciente y profesional).
* *Extensión incorporada en la tesis:* **Preguntas Guía**, definidas por el profesor para delimitar los hitos que el estudiante debe comprender durante la sesión, impidiendo que el LLM divague en digresiones fuera del temario.

### 6.4. Trabajos Relacionados y Diferenciadores del Proyecto
| Plataforma / Enfoque | Características Principales | Limitaciones Frente al Presente Proyecto |
| :--- | :--- | :--- |
| **OATutor** (Pardos et al., 2023) | Tutor de código abierto basado en algoritmos rígidos de BKT (Bayesian Knowledge Tracing). | No permite flexibilidad de prompting ni adaptación pedagógica dinámica por parte del docente en tiempo real. |
| **CodeEdu** (Zhao et al., 2025) | Tutoría mediante agentes LLM en tiempo real. | Carece de panel de seguimiento analítico docente individual y colectivo. |
| **Classroom Simulacra** (Xu et al., 2025) | Simulación de estudiantes virtuales mediante gemelos digitales y agentes generativos. | No interactúa con estudiantes reales en el aula ni genera retroalimentación a partir de sesiones efectivas. |
| **Plataforma Propuesta (Este Proyecto)** | **Dualidad estudiante-docente:** Chat socrático interactivo con historial persistente + Panel de métricas agregadas con resúmenes por IA + Configuración docente bajo CRAFT. | Solución integral de bajo costo, abierta y contextualizada al currículo universitario. |

---

## 7. Módulos, Requisitos del Sistema y Métricas

### 7.1. Módulos de la Plataforma
1. **Módulo de Inicio de Sesión / Registro:** Gestión de acceso para estudiantes y profesores. Soporte de autenticación multifactor (MFA) opcional para docentes. Los estudiantes acceden mediante registro mínimo rápido al abrir un enlace único de clase.
2. **Módulo de Interfaz de Estudiante:** Panel de bienvenida donde el estudiante consulta sus sesiones activas y su historial de participación.
3. **Interfaz de Interacción Socrática (Estudiante):** Chat conversacional en tiempo real, renderizado en servidor (Thymeleaf), con inyección de progreso histórico y feedback al cierre de sesión.
4. **Módulo de Administración de Profesor:** Centro de control del docente para crear sesiones, consultar participaciones, editar perfil institucional y revisar opiniones enviadas por los alumnos.
5. **Panel de Configuración de Prompts con IA (Docente):** Asistente guiado que ayuda al docente a construir el prompt de la clase utilizando los 5 componentes de CRAFT y sus preguntas guía.
6. **Panel de Métricas Docente (Analítica y Seguimiento):** Visualización gráfica e interactiva de indicadores colectivos e individuales, complementada con un resumen narrativo generado automáticamente por Gemini para proponer acciones pedagógicas.

### 7.2. Requisitos Funcionales (RF)
* **RF-01 (Acceso simplificado a sesiones):** El docente crea una sesión que genera automáticamente un enlace único de acceso. Al abrirlo, el estudiante realiza un registro mínimo con nombre y correo institucional, quedando vinculado inmediatamente sin pasos extra.
* **RF-02 (Tutoría Socrática):** El chat con el ITS debe implementar el método socrático, utilizando pistas y preguntas reflexivas sin revelar código ni soluciones directas.
* **RF-03 (Retroalimentación Inmediata):** Respuestas conversacionales instantáneas en cada turno del estudiante para favorecer ciclos rápidos de ensayo y error.
* **RF-04 (Persistencia de Historial y Evolución):** Almacenar el historial de interacción para retomar sesiones previas, detectar patrones de error y adaptar el discurso evolutivamente.
* **RF-05 (Configuración Guiada de Prompts):** Paso a paso estructurado asistido por IA para que el profesor defina tema, nivel y tono bajo CRAFT sin requerir conocimientos avanzados de ingeniería de prompts.
* **RF-06 (Detección de Falencias Críticas):** Visualización en el panel docente de los conceptos con mayor frecuencia de error a nivel individual y grupal.
* **RF-07 (Evolución entre Sesiones):** Comparativa del desempeño del estudiante a lo largo del tiempo, indicando si supera o reincide en los mismos errores.
* **RF-08 (Resumen Pedagógico Automatizado):** Generación automática en lenguaje natural de un informe que interprete las métricas del grupo y sugiera intervenciones didácticas concretas al profesor.
* **RF-09 (Monitoreo de Desenganche y Abandono):** Registro de cantidad de sesiones, frecuencia de uso y duración promedio para detectar estudiantes en riesgo.
* **RF-10 (Auditor Socrático / Doble Verificación Interna):** Mecanismo de control interno que intercepta y rechaza cualquier respuesta del modelo que filtre la solución directa de código, forzando su reformulación antes de ser enviada al frontend.

### 7.3. Requisitos No Funcionales (RNF)
* **RNF-01 (Confidencialidad y Aislamiento de Conversaciones):** El sistema restringe el acceso al chat exclusivamente al estudiante propietario y al docente titular de la sesión.
* **RNF-02 (Minimización de Datos Personales):** Registro estricto de datos esenciales (nombre y correo institucional), sin almacenar información sensible innecesaria.

### 7.4. Métricas de Seguimiento Pedagógico
| ID | Nombre de la Métrica | Descripción Técnica | Traducción Pedagógica para el Docente | Fuente |
| :--- | :--- | :--- | :--- | :--- |
| **M-01** | **Frecuencia de error por concepto** | Número de veces que un concepto específico genera error en el grupo durante una sesión. | *"Este es el tema que más le está costando al grupo. Considera dedicarle más tiempo en clase."* | (Colby, 2017) |
| **M-02** | **Intentos por concepto** | Número de turnos de conversación requeridos por el estudiante antes de alcanzar la comprensión. | *"Este estudiante necesita muchos intentos para entender el concepto. Puede requerir atención personalizada."* | (Colby, 2017; Kurup et al., 2016) |
| **M-03** | **Evolución entre sesiones** | Comparación del desempeño del estudiante en un mismo concepto en sesiones distintas. | *"Este estudiante mejoró / sigue fallando en el mismo punto que la semana pasada."* | (Kurup et al., 2016) |
| **M-04** | **Tasa de solicitudes de respuesta directa** | Conteo de ocasiones en que el alumno exige explícitamente el código o la solución directa. | *"Este estudiante muestra señales de frustración. Puede necesitar una estrategia didáctica diferente."* | (Castellanos et al., 2026) |
| **M-05** | **Frecuencia y duración de sesiones** | Número de sesiones completadas y tiempo promedio por sesión por estudiante. | *"Este estudiante no ha usado la plataforma en varios días. Puede estar en riesgo de abandono."* | (Castellanos et al., 2026) |

---

## 8. Decisiones de Diseño de IA y Selección del LLM

Para seleccionar el modelo de lenguaje se utilizaron datos objetivos de benchmarking de **Artificial Analysis (2026)**, evaluando la gama costo-eficiente de los principales proveedores:

| Modelo | Proveedor | Intelligence Index | LiveCodeBench | Costo Input ($ / 1M tok) | Costo Output ($ / 1M tok) |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Gemini 3 Flash** | Google | **39 pts (Líder)** | **Líder (+~7% ventaja)** | $0.50 | $3.00 |
| **Claude 4.5 Haiku** | Anthropic | 30 pts | Segundo lugar | $1.00 | $5.00 |
| **Grok 4 Fast** | xAI | 28 pts | Tercer lugar | $0.20 | $0.50 |
| **GPT-5 mini (high)** | OpenAI | 26 pts | Cuarto lugar | $0.25 | $2.00 |

* **Justificación de la Selección:** **Gemini 3 Flash** obtuvo el puntaje más alto en el índice general de inteligencia y en **LiveCodeBench** (evaluación de generación, autorreparación y ejecución de código competitivo libre de contaminación). Aunque modelos de gama ultra alta (como Claude Opus 5) ofrecen puntajes elevados, su costo es inviable para un proyecto autofinanciado. Gemini 3 Flash ofrece la mejor relación calidad-costo-latencia para diálogos educativos de programación.

---

## 9. Arquitectura de Prompts en 3 Capas y Persistencia

En lugar de un prompt monolítico (difícil de mantener y propenso a que el docente altere accidentalmente las restricciones socráticas), la plataforma implementa una **arquitectura desacoplada en tres capas**:

```
┌────────────────────────────────────────────────────────────────────────┐
│  CAPA 1: FIJA (Sistema)                                                │
│  - Reglas inmutables del Método Socrático (Chang, 2023).              │
│  - Prohibición absoluta de entregar código o soluciones directas.      │
│  - Guía reflexiva mediante analogías y preguntas mayéuticas.          │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼─────────────────────────────────────┐
│  CAPA 2: CONFIGURABLE (Docente - Framework CRAFT)                     │
│  - Context: Asignatura y conceptos de POO a evaluar.                   │
│  - Role, Audience, Format, Tone adaptados a la clase.                  │
│  - Preguntas Guía: Hitos obligatorios que el tutor debe indagar.       │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼─────────────────────────────────────┐
│  CAPA 3: DINÁMICA (Estudiante / Sesión)                                │
│  - Progreso Markdown heredado de sesiones anteriores (si existe).      │
│  - Historial secuencial de mensajes de la sesión activa.              │
│  - Nuevo mensaje enviado por el estudiante.                            │
└────────────────────────────────────────────────────────────────────────┘
```

### 9.1. Doble Verificación Interna (Auditor Socrático - RF-10)
Al recibir la respuesta de Gemini 3 Flash, el backend ejecuta una validación para asegurar que no se hayan filtrado fragmentos de código resolutivos. Si la regla se viola, el servicio solicita automáticamente una reformulación en modo socrático antes de enviar la respuesta al navegador.

### 9.2. Esquema de Persistencia Híbrida: Numérico + Markdown (`progreso_md`)
Se descartó el almacenamiento intermedio en JSON por riesgo de errores sintácticos de parseo y sobrecosto de deserialización. Se adoptó una persistencia híbrida en **Supabase**:
1. **Datos Cuantitativos:** Columnas numéricas indexadas (conteo de turnos, intentos por concepto, peticiones de respuesta directa) para consultas analíticas directas en SQL.
2. **Datos Cualitativos (`progreso_md`):** Texto Markdown estructurado que se genera al cierre de la sesión y se guarda en la tabla `participacion`. Al iniciar una sesión futura del mismo tema, este texto se inyecta directamente en la Capa 3 sin requerir transformación.

#### Estructura Estricta de Cierre de Sesión (`progreso_md`):
```markdown
## Conceptos dominados
- [Concepto 1]
- [Concepto 2]

## Conceptos en progreso
- [Concepto 1]
- [Concepto 2]

## Errores recurrentes
- [Descripción del error 1]
- [Descripción del error 2]

## Recomendación para la siguiente sesión
- [Recomendación puntual y accionable]
```

---

## 10. Flujos de Datos y Persistencia

### Flujo 1: Configuración de Sesión y Enlace Único
1. El docente ingresa con credenciales (y MFA opcional) a su panel de administración.
2. Configura el prompt mediante el formulario guiado CRAFT e ingresa las **preguntas guía**.
3. Spring Boot almacena los campos de CRAFT y preguntas en columnas dedicadas de la tabla `sesion` en Supabase y genera un `enlace_unico`.
4. El docente distribuye el enlace.
5. El estudiante abre el enlace; Spring Boot valida que la sesión esté activa.
6. El alumno ingresa nombre y correo institucional, quedando vinculado a la sesión con una nueva fila en la tabla `participacion`.

### Flujo 2: Interacción Conversacional Socrática
1. El estudiante envía su duda desde la interfaz Thymeleaf (`POST /chat/mensaje`).
2. Spring Boot recupera de Supabase el historial de la sesión y el `progreso_md` previo si existe.
3. Se ensambla el prompt con las 3 capas:
   * **Capa 1:** Reglas fijas del sistema.
   * **Capa 2:** CRAFT + Preguntas guía de la sesión.
   * **Capa 3:** Progreso heredado + Historial de turnos + Nuevo mensaje.
4. El cliente HTTP envía la petición a la API de Gemini 3 Flash.
5. Se valida el cumplimiento socrático (RF-10).
6. Spring Boot almacena el mensaje del alumno y la respuesta en la tabla `mensaje`, y actualiza las métricas estructuradas en la tabla `metrica`.
7. Thymeleaf renderiza el fragmento HTML y lo entrega al estudiante.

### Flujo 3: Cierre de Sesión y Síntesis Pedagógica
1. Al terminar la sesión, Spring Boot solicita a Gemini un análisis global del historial completo.
2. Gemini responde con la plantilla de 4 secciones en Markdown, la cual se persiste en la columna `progreso_md` de la tabla `participacion`.
3. Las métricas numéricas acumuladas se consolidan en la tabla `metrica`.
4. Cuando el docente abre el panel analítico, Spring Boot consulta las métricas estructuradas y le pide a Gemini un resumen en lenguaje natural con recomendaciones directas para la siguiente clase magistral.

---

## 11. Arquitectura de Software bajo el Modelo C4

### 11.1. Nivel 1: Diagrama de Contexto del Sistema
* **Actores:**
  * **Profesor Universitario [Persona]:** Configura sesiones bajo CRAFT, consulta el panel de métricas y recibe retroalimentación de los alumnos.
  * **Estudiante Universitario [Persona]:** Resuelve dudas de POO, recibe guía socrática y califica las sesiones.
* **Sistema Central:** **Sistema de Tutoría Inteligente (ITS)**.
* **Sistemas Externos:**
  * **API de Google Gemini 3 Flash:** Procesa los prompts y genera el diálogo socrático y las síntesis analíticas.
  * **Base de Datos Supabase (PostgreSQL):** Almacena perfiles, clases, mensajes, métricas y progreso cualitativo.

### 11.2. Nivel 2: Diagrama de Contenedores
* **Contenedor 1 - Aplicación Web (Spring Boot + Thymeleaf):**
  * Servidor web embebido Tomcat (puerto 8080).
  * Renderizado SSR con Thymeleaf.
  * Gestión de sesiones HTTP, autenticación y controladores Spring MVC.
* **Contenedor 2 - Módulo de Orquestación de IA (Servicios Spring):**
  * Ensamblador de prompts multi-capa.
  * Cliente HTTP REST para comunicación con Gemini 3 Flash.
  * Auditor de respuestas socráticas (RF-10).
* **Contenedor 3 - Base de Datos (PostgreSQL en Supabase):**
  * Instancia relacional gestionada que persiste usuarios, sesiones, mensajes y métricas.
* **Contenedor 4 - Infraestructura de Ejecución (Docker):**
  * Empaquetado en imagen ligera para ejecución local o en VPS (ej. Render/Servidor privado).

### 11.3. Nivel 3: Diagrama de Componentes (Módulo de Orquestación de IA)
1. **Controlador de Sesión Socrática (`@Controller` / `@Service`):** Coordina la recepción de la petición y delega la ejecución.
2. **Constructor de Prompt (`PromptBuilderService`):** Orquesta el ensamblaje de:
   * *Capa Fija del Prompt:* Plantilla estática inmutable con las reglas socráticas.
   * *Capa Configurable del Prompt:* Datos CRAFT + preguntas guía desde la sesión docente.
   * *Capa Dinámica del Prompt:* Progreso Markdown + historial reciente de mensajes.
3. **Repositorio de Progreso (`Spring Data JPA`):** Acceso a datos de Supabase vía JDBC/Postgres.
4. **Cliente de API de IA (`GeminiApiClient`):** Serializa la petición JSON, gestiona cabeceras HTTP y realiza la llamada a Gemini.
5. **Deserializador de Respuesta (`Jackson ObjectMapper`):** Procesa la respuesta para validación, persistencia y visualización.

### 11.4. Nivel 4: Esquema de Base de Datos Relacional (Supabase PostgreSQL)

```
┌────────────────────────────────────────────────────────┐
│                        Profesor                        │
├────────────────────────────────────────────────────────┤
│ PK  id: UUID                                           │
│     nombre: VARCHAR                                    │
│     correo_institucional: VARCHAR (unique)             │
│     contraseña: VARCHAR                                │
│     facultad: VARCHAR                                  │
│     fecha_registro: TIMESTAMPTZ                        │
└───────────────────────────┬────────────────────────────┘
                            │ 1
                            │
                            │ n
┌───────────────────────────▼────────────────────────────┐
│                         Sesion                         │
├────────────────────────────────────────────────────────┤
│ PK  id: UUID                                           │
│ FK  profesor_id: UUID                                  │
│     tema: VARCHAR                                      │
│     craft_contexto: TEXT                               │
│     craft_rol: TEXT                                    │
│     craft_audiencia: TEXT                              │
│     craft_formato: TEXT                                │
│     craft_tono: TEXT                                   │
│     enlace_unico: VARCHAR (unique)                     │
│     estado: BOOLEAN (Activa / Cerrada)                 │
│     fecha_creacion: TIMESTAMPTZ                        │
└───────────────────────────┬────────────────────────────┘
                            │ 1
                            │
                            │ n
┌───────────────────────────▼────────────────────────────┐       ┌────────────────────────────────────────────────────────┐
│                      participacion                     │       │                       estudiante                       │
├────────────────────────────────────────────────────────┤       ├────────────────────────────────────────────────────────┤
│ PK  id: UUID                                           │       │ PK  id: UUID                                           │
│ FK  estudiante_id: UUID ───────────────────────────────┼───────┤     nombre: VARCHAR                                    │
│ FK  sesion_id: UUID                                    │ n   1 │     correo_institucional: VARCHAR (unique)             │
│     progreso_md: TEXT                                  │       │     contraseña: VARCHAR                                │
│     feedback_estudiante: TEXT                          │       │     fecha_registro: TIMESTAMPTZ                        │
│     valoracion_estudiante: INT4                        │       │                                                        │
│     fecha_creacion: TIMESTAMPTZ                        │       │                                                        │
└─────────────────────┬──────────────────────────┬───────┘       └────────────────────────────────────────────────────────┘
                      │ 1                        │ 1
                      │                          │
                      │ n                        │ n
┌─────────────────────▼──────────────┐   ┌───────▼────────────────────────┐
│               mensaje              │   │            metrica             │
├────────────────────────────────────┤   ├────────────────────────────────┤
│ PK  id: UUID                       │   │ PK  id: UUID                   │
│ FK  participacion_id: UUID         │   │ FK  participacion_id: UUID     │
│     emisor: BOOLEAN (Estud./Mod.)  │   │     codigo: INT8 (M-01 a M-05) │
│     contenido: TEXT                │   │     concepto: VARCHAR NULLABLE │
│     fecha_creacion: TIMESTAMPTZ    │   │     valor_numerico: INT8       │
└────────────────────────────────────┘   │     fecha_registro: TIMESTAMPTZ│
                                         └────────────────────────────────┘
```

---

## 12. Decisiones de Arquitectura e Infraestructura Tecnológica

1. **Alineación Institucional:** La adopción de Java, Spring Boot y Supabase responde a los estándares de desarrollo e infraestructura avalados por el área de TI de la Pontificia Universidad Javeriana Cali.
2. **Spring MVC + `spring-boot-starter-web`:**
   * Patrón *Front Controller* con `DispatcherServlet` centralizando las peticiones hacia `@Controller` y `@RestController`.
   * Servidor embebido Apache Tomcat preconfigurado (puerto `8080`), permitiendo generar un archivo ejecutable `.jar` autónomo.
   * `HttpMessageConverters` para intercambio asíncrono seguro vía JSON con servicios externos.
3. **Thymeleaf (Server-Side Rendering):** Renderizado seguro de vistas HTML en el servidor, minimizando la complejidad del cliente y garantizando la vinculación fluida de datos de sesión.
4. **Contenedor Efímero vs. Persistencia en Supabase:** Al empaquetar la aplicación en Docker, el contenedor no guarda estado en el disco local; toda la memoria conversacional y analítica radica de forma segura en PostgreSQL en Supabase.

---

## 13. Conclusiones y Líneas de Trabajo Futuro

El prototipo valida la viabilidad de integrar IA Generativa en la educación superior sin fomentar la dependencia de respuestas automáticas, salvaguardando el rigor pedagógico mediante el método socrático y empoderando al docente mediante métricas oportunas. Como líneas de evolución futura se definen:

1. **Secuencias de Pistas Progresivas Automatizadas:** Implementar un agente especializado (o ajuste fino de modelo) capaz de calibrar la progresividad de las pistas (*puntero*, *didáctica* y *de fondo*) de manera determinística, mitigando la variabilidad inherente de los LLMs.
2. **Modelado Cognitivo Probabilístico (BKT):** Incorporar algoritmos de *Bayesian Knowledge Tracing* para transitar del seguimiento descriptivo en Markdown hacia una estimación matemática continua de la probabilidad de maestría por habilidad.
3. **Andamiaje (*Scaffolding*) Dinámico en la Zona de Desarrollo Próximo:** Retirar o elevar automáticamente el nivel de asistencia del tutor a medida que el modelo cognitivo confirme que el alumno consolida las competencias de programación.

---

## 14. Últimas Modificaciones Importantes

Durante las fases de iteración, integración y preparación para la validación empírica con usuarios reales (estudiantes y docentes de la Pontificia Universidad Javeriana Cali), se implementaron mejoras sustanciales en la plataforma web. Estas intervenciones abarcan la retroalimentación bidireccional, la gestión y asignación de cohortes, la clarificación operativa del ciclo de vida de las sesiones, la consolidación de la identidad visual institucional y la optimización de persistencia y seguridad.

### 14.1. Sistema Integral de Retroalimentación del Estudiante (Feedback y Valoración)
* **Justificación Pedagógica:** Tradicionalmente, la analítica del aprendizaje modela el desempeño del estudiante desde la perspectiva del sistema o del docente. Para cerrar el ciclo de evaluación formativa y evaluar la percepción del acompañamiento socrático generativo, se habilitó un canal formal donde el estudiante califica la experiencia y expresa dudas o sugerencias sobre el andamiaje pedagógico recibido.
* **Extensión del Modelo de Datos:** Se agregaron las columnas `feedback_estudiante` (`TEXT`) y `valoracion_estudiante` (`INT4`, escala Likert de 1 a 5 estrellas) a la tabla `participacion` en Supabase / PostgreSQL, mapeadas en la entidad Java [`Participacion.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/model/entity/Participacion.java).
* **Canales de Entrada para el Estudiante:**
  1. *Durante la Interacción Socrática (Chat):* En la interfaz de conversación ([`chat.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/estudiante/chat.html)), el estudiante dispone del botón *"Dar Feedback"*, que despliega un modal interactivo con selección de estrellas y campo de justificación textual, enviando la información mediante `POST /chat/{enlace}/feedback` gestionado por [`ChatController.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/controller/ChatController.java).
  2. *Desde el Portal del Estudiante:* En la vista de asignaturas matriculadas (`/estudiante/clases`), cada tarjeta de clase expone la insignia de valoración otorgada (o indicador de "Sin valorar") y un botón para ingresar o actualizar su retroalimentación en cualquier momento vía `POST /estudiante/clases/{participacionId}/feedback` en [`EstudianteController.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/controller/EstudianteController.java).
* **Módulos de Explotación Analítica para el Docente:**
  1. *Dashboard Específico por Sesión (`/docente/sesiones/{id}/metricas`):* Se integró la sección *"Retroalimentación de Estudiantes"*, permitiendo al profesor contrastar las métricas cognitivas cuantitativas (conceptos erróneos y fallas lógicas) con la percepción subjetiva de satisfacción y comentarios cualitativos de esa cohorte específica.
  2. *Módulo Consolidado Global (`/docente/feedback`):* Interfaz centralizada ([`feedback.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/feedback.html)) con métricas agregadas a nivel de profesor:
     * **Promedio General de Satisfacción:** Media aritmética de todas las valoraciones recibidas.
     * **Total de Opiniones Registradas:** Conteo de participaciones con retroalimentación textual o calificación.
     * **Tasa de Aprobación Positiva:** Porcentaje de opiniones con valoración de 4 o 5 estrellas.
     * **Filtro Dinámico por Sesión y Visualización Detallada:** Tarjetas que desglosan nombre del alumno, sesión asociada, fecha y contenido del feedback.

### 14.2. Directorio Centralizado y Asignación Flexible de Estudiantes
* **Directorio de Estudiantes (`/docente/estudiantes`):** Se habilitó un panel administrativo que lista a todos los estudiantes registrados en la plataforma bajo la tutela del docente, reportando sus datos de contacto y cantidad de clases matriculadas.
* **Asignación Directa a Clases (`POST /docente/estudiantes/{id}/asignar-sesion`):** Se eliminó la fricción de exigir que un estudiante previamente registrado deba reingresar obligatoriamente por el enlace público de invitación para cada nueva clase. El docente puede seleccionar a cualquier estudiante de la lista y matricularlo directamente en una sesión activa mediante el modal `#modalAsignarClase`, validando previamente la no existencia de inscripciones duplicadas.
* **Sincronización y Persistencia de Participaciones:** En el portal del estudiante (`/estudiante/clases`), se garantiza la recuperación inmediata de las clases a las que ha sido asignado administrativamente, permitiéndole ingresar al chat correspondiente sin barreras operativas.

### 14.3. Semántica Operativa del Ciclo de Vida de las Sesiones: Pausar vs. Eliminar
Para evitar ambigüedades operativas y pérdidas involuntarias de datos durante el uso docente, se delimitó claramente el comportamiento entre pausar y eliminar una clase:
* **Pausar Sesión (`sesion.estado = false`):**
  * *Naturaleza:* Operación reversible e inocua sobre los datos.
  * *Efecto Pedagógico y Técnico:* Congela temporalmente la interacción socrática para los estudiantes. Si un alumno intenta enviar un mensaje en una sesión pausada, [`ChatController.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/controller/ChatController.java) bloquea la invocación del modelo de IA, redirige con parámetro de alerta (`?pausada=true`) y deshabilita los controles de entrada (*textarea* y botón de envío).
  * *Persistencia:* Todo el historial de conversación, progreso pedagógico, métricas y retroalimentaciones se conserva íntegro. El profesor puede reactivar la sesión en cualquier instante con un solo clic.
* **Eliminar Sesión:**
  * *Naturaleza:* Operación destructiva e irreversible.
  * *Efecto Pedagógico y Técnico:* Requiere confirmación explícita mediante diálogo modal. Ejecuta la eliminación en cascada de todos los registros subordinados en la base de datos (mensajes de chat, métricas generadas, participaciones de estudiantes y metadatos de la sesión). Se utiliza exclusivamente para depurar clases de prueba o cohortes obsoletas.

### 14.4. Rediseño de Navegación e Identidad Visual Javeriana Cali
* **Barra de Navegación de Alto Contraste (*Nav-Pills*):** En el panel del docente ([`sesiones.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/sesiones.html), [`estudiantes.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/estudiantes.html), [`feedback.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/feedback.html)), se sustituyeron los enlaces simples de texto tenue por botones de navegación en bloque (*pills*) con alto contraste visual (`#2c5697` activo, `#1b345c` en hover y fondo institucional `#E8EFF8`), facilitando el cambio rápido e intuitivo entre *"Mis Sesiones"*, *"Mis Estudiantes"* y *"Feedback Recibido"*.
* **Depuración de Controles Redundantes:** Se removió el botón "Vista de estudiante" de la barra docente, el cual causaba confusión de roles, redirigiendo la experiencia de prueba a los flujos naturales de enlace y registro.
* **Armonización de Paleta de Color:** Se aplicó de manera homogénea la guía de estilo institucional de la Javeriana Cali en plantillas de autenticación, administración docente y chat del estudiante, manteniendo consistencia cromática, legibilidad y cumplimiento de pautas de contraste WCAG.
* **Portal del Estudiante y Edición de Perfil:** Se corrigió la presentación de tarjetas de cursos y se habilitó la actualización de información personal del estudiante en `/estudiante/perfil`, con retroalimentación visual clara mediante alertas contextuales.

### 14.5. Fortalecimiento Arquitectónico, Persistencia y Seguridad
* **Mitigación de `LazyInitializationException`:** Se optimizó el repositorio [`ParticipacionRepository.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/repository/ParticipacionRepository.java) mediante consultas JPQL con `JOIN FETCH` (`findByProfesorWithEstudianteAndSesion`, `findByEstudianteWithSesionOrderByCreatedAtDesc`), asegurando la carga ansiosa controlada de las relaciones `estudiante` y `sesion` en una sola consulta SQL, evitando consultas *N+1* y fallos en contextos desacoplados.
* **Habilitación de *Open-In-View*:** Se configuró explícitamente `spring.jpa.open-in-view=true` en `application.properties`, garantizando la disponibilidad de la sesión de Hibernate durante la fase de renderizado de vistas en Thymeleaf.
* **Seguridad Criptográfica en Credenciales:** Se implementó el hasheo seguro y salado de contraseñas de estudiantes mediante `BCryptPasswordEncoder`, eliminando el almacenamiento en texto plano y alineando la autenticación de la plataforma con los estándares modernos de seguridad informática.

### 14.6. Rediseño del Dashboard Analítico y Adecuación Pedagógica de Métricas (Eliminación de 'RF-10' en UI)
* **Depuración de Jerga Técnica ('RF-10'):** Se eliminó la etiqueta de requisito de software *"Auditor RF-10"* de la interfaz gráfica docente. Al profesor se le presentan ahora métricas orientadas al aprendizaje del estudiante y no a mecanismos de control interno del backend.
* **Reconfiguración de los 4 KPIs de Cabecera:**
  1. *Estudiantes Conectados:* Total de alumnos matriculados en la sesión.
  2. *Preguntas Realizadas:* Volumen total de consultas emitidas por los estudiantes.
  3. *Promedio de Turnos (`M-02`):* Promedio de intervenciones requeridas por estudiante para resolver dudas (`promedioTurnosPorEstudiante`).
  4. *Alertas de Atajo / Solicitudes Directas (`M-04`):* Conteo de ocasiones en que los alumnos exigieron código directo y fueron reconducidos por el tutor socrático hacia la reflexión autónoma.
* **Tabla Enriquecida de Seguimiento Individual (`M-02`, `M-03`, `M-05`):**
  * *M-02 (Turnos y Nivel de Dificultad):* Clasificación semafórica (*🟢 Fluido [≤3 turnos]*, *🟡 Guiado [4-7 turnos]*, *🔴 Requiere Apoyo [≥8 turnos]*).
  * *M-03 (Evolución Pedagógica):* Estado de madurez cognitiva (*Consolidado*, *En diálogo activo*, *Dudas en progreso*).
  * *M-05 (Actividad y Compromiso):* Indicador temporal relativo (*Activo hoy*, *Hace X días*, *Sin participación / En riesgo*).
* **Modal de Diagnóstico Cognitivo Individual:** Integración del botón *"Ver Diagnóstico"* en cada fila de estudiante, desplegando un modal interactivo con los 4 bloques cualitativos estructurados por Gemini (*Conceptos dominados*, *Conceptos en progreso*, *Errores recurrentes* y *Recomendación para la siguiente sesión*), superando la limitación de texto truncado en celda.

### 14.7. Módulo Integrado de Evaluación Científica de Usabilidad y Aceptación (Instrumentos Estandarizados SUS y TAM)
Para dar cumplimiento riguroso al **Objetivo Específico 4**, a la **Subpregunta 4** y al **Entregable 6** de la investigación de grado, se integró en la plataforma un sistema formal de evaluación empírica con usuarios reales, fundamentado en los dos modelos psicométricos de mayor validez en la literatura de Ingeniería de Software e Informática Educativa:

1. **System Usability Scale (SUS - Brooke, 1986):**
   * *Propósito:* Evaluar la usabilidad técnica, ergonomía, consistencia y facilidad de uso de la aplicación web.
   * *Estructura:* 10 afirmaciones estandarizadas en escala Likert (1: Totalmente en desacuerdo a 5: Totalmente de acuerdo), alternando 5 ítems de tono positivo y 5 de tono negativo.
   * *Fórmula de Calificación Oficial (0 a 100 puntos):*
     $$\text{Puntaje SUS} = \left[ \sum_{i \in \{1,3,5,7,9\}} (\text{Ítem}_i - 1) + \sum_{j \in \{2,4,6,8,10\}} (5 - \text{Ítem}_j) \right] \times 2.5$$
     *(Puntaje de referencia en la industria: $\ge 68$ puntos para usabilidad aceptable; $\ge 80$ puntos para usabilidad excelente).*

2. **Technology Acceptance Model (TAM - Davis, 1989):**
   * *Propósito:* Medir la aceptación del modelo pedagógico socrático asistido por IA generativa frente a métodos tradicionales de enseñanza y herramientas de respuesta directa.
   * *Batería Adaptada de 6 Constructos Clave (Agnóstica a un lenguaje específico de programación):*
     * *Utilidad Pedagógica (PU):* Efectividad de recibir preguntas orientadoras y analogías en vez del código resuelto para comprender la lógica de POO.
     * *Autonomía Cognitiva (PU):* Capacidad del tutor para ayudar al estudiante a identificar sus propios errores conceptuales y resolver el problema por sí mismo.
     * *Manejo del Desafío y Frustración (Carga Cognitiva y Afectiva):* Percepción de reto formativo constructivo en lugar de frustración por no recibir el código inmediatamente.
     * *Claridad de la Mediación (PEOU):* Efectividad del ritmo conversacional (una sola pregunta a la vez) y de las analogías del mundo real.
     * *Ventaja Comparativa Relativa:* Percepción de mayor valor formativo frente al uso de herramientas comerciales de IA genéricas que entregan el código terminado (como ChatGPT).
     * *Intención de Uso Futuro (BIU):* Disposición voluntaria para adoptar la plataforma en asignaturas futuras de programación.

3. **Arquitectura de Persistencia JSON Desacoplada (`evaluacion_sus_tam TEXT`):**
   * Para asegurar un modelo relacional limpio en Supabase / PostgreSQL sin saturar la tabla `participacion` con múltiples columnas, se habilitó el atributo `evaluacion_sus_tam TEXT` en la tabla `participacion`, mapeado en [`Participacion.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/model/entity/Participacion.java).
   * Almacena un objeto JSON estructurado que encapsula el puntaje SUS calculado automáticamente en backend (`susScore`), el vector de respuestas SUS (`susRespuestas`), el mapa de respuestas TAM (`tamRespuestas`), la retroalimentación cualitativa opcional (`comentario`) y la marca temporal de envío. Esta arquitectura posibilita la exportación inmediata hacia software estadístico (Excel, SPSS, R) para la elaboración de los análisis empíricos del Capítulo 5.

4. **Doble Canal Diferenciado en la Experiencia del Estudiante:**
   * *Canal 1 (Feedback de Aula al Docente - Existente):* Calificación tradicional de 1 a 5 estrellas y dudas pedagógicas para la clase presencial (color dorado/azul), concebido como canal no vinculante.
   * *Canal 2 (Evaluación Científica de la Plataforma SUS + TAM - Nuevo):* Botón prioritario con estilo **Púrpura / Amatista Institucional** (`#5A3793`) accesible desde la cabecera del chat ([`chat.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/estudiante/chat.html)) y desde la tarjeta de clases ([`clases.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/estudiante/clases.html)). Al finalizar la sesión socrática, se enfatiza su diligenciamiento para la validación de la tesis, mostrando un estado visual de completitud una vez enviado.

5. **Explotación Analítica en el Panel Docente:**
   * En [`feedback.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/feedback.html), el profesor cuenta con un KPI centralizado que computa la media aritmética del puntaje SUS global obtenido en sus sesiones, complementado con un modal de detalle por estudiante que expone el desglose de respuestas de usabilidad y aceptación pedagógica.

### 14.8. Auditoría Integral de Seguridad y Eficiencia — Correcciones Implementadas (Septiembre 2026)

Se realizó una auditoría exhaustiva de seguridad y eficiencia sobre la totalidad del código fuente (controladores, servicios, entidades, repositorios, configuración y plantillas HTML). La auditoría identificó **36 hallazgos** categorizados por severidad (6 críticos, 9 altos, 12 medios, 9 bajos). A continuación se documentan las correcciones implementadas:

#### 14.8.1. Correcciones de Severidad Crítica

1. **Eliminación de Credenciales en Texto Plano del Repositorio:**
   * Se removieron los valores por defecto de las credenciales de la base de datos en `application.properties` (URL, usuario y contraseña de Supabase). Las propiedades ahora exigen estrictamente la provisión de variables de entorno (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`) sin fallback, previniendo la exposición accidental de secretos en el control de versiones.
   * Se procedió a rotar la contraseña de la base de datos en el panel de administración de Supabase.

2. **Eliminación del Fallback de Contraseñas en Texto Plano:**
   * En [`AuthService.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/service/AuthService.java), se eliminó el bloque de comparación directa (`passwordIngresada.equals(passwordAlmacenada)`) que permitía autenticar usuarios con contraseñas no hasheadas. La verificación ahora opera exclusivamente a través de `BCryptPasswordEncoder.matches()`.

3. **Corrección de IDOR en Eliminación de Estudiantes:**
   * En [`DocenteController.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/controller/DocenteController.java), el endpoint `POST /docente/estudiantes/{estudianteId}/eliminar` fue refactorizado para verificar que el estudiante a eliminar tenga participaciones exclusivamente en sesiones pertenecientes al docente autenticado. Se previene así la eliminación arbitraria de estudiantes ajenos.

4. **Generación Local de Códigos QR para 2FA:**
   * Se reemplazó la dependencia del servicio externo `api.qrserver.com` por la generación local del código QR en el servidor mediante la librería ZXing (`com.google.zxing:core`). El secreto TOTP del docente ya no se transmite a terceros y la imagen QR se retorna como cadena Base64 (`data:image/png;base64,...`) directamente en la vista.

5. **Desacoplamiento Transaccional de Llamadas a la API de Gemini (Connection Pool Starvation):**
   * En [`ChatService.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/service/ChatService.java), se separó el método `procesarMensajeEstudiante()` en tres fases: (1) persistencia del mensaje del estudiante y construcción del prompt en una transacción corta, (2) invocación de la API de Gemini fuera de contexto transaccional, y (3) persistencia de la respuesta del tutor en una segunda transacción corta. Esto evita que una conexión JDBC del pool HikariCP (limitado a 5) quede retenida durante los 5–35 segundos de la llamada HTTP externa.
   * Se desactivó `spring.jpa.open-in-view` (`false`) para evitar la retención innecesaria de conexiones durante el renderizado de vistas Thymeleaf.

#### 14.8.2. Correcciones de Severidad Alta

6. **Neutralización de Cross-Site Scripting (XSS) en Chat y Dashboard:**
   * Se integró la librería **DOMPurify** en [`chat.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/estudiante/chat.html) y [`dashboard.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/dashboard.html) para sanitizar todo contenido Markdown parseado por `marked.js` antes de su inyección en el DOM (`DOMPurify.sanitize(marked.parse(contenido))`).
   * Se migraron las concatenaciones inseguras en atributos `onclick` de [`sesiones.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/sesiones.html) y [`estudiantes.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/estudiantes.html) a atributos HTML5 `data-*`, previniendo la inyección de JavaScript mediante nombres con caracteres especiales.

7. **Protección contra Desactivación No Autorizada de 2FA:**
   * El endpoint `POST /docente/2fa/desactivar` ahora exige la confirmación de la contraseña actual del docente antes de proceder a desactivar el segundo factor de autenticación.

8. **Restricción de Visibilidad de Estudiantes por Docente:**
   * La vista de directorio de estudiantes (`/docente/estudiantes`) fue modificada para mostrar únicamente estudiantes con participaciones en sesiones del docente autenticado, respetando el principio de mínimo privilegio y protección de datos personales.

#### 14.8.3. Correcciones de Severidad Media

9. **Unificación de Mensajes de Error en Autenticación:**
   * Se homogenizaron los mensajes de error en [`AuthService.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/service/AuthService.java) para evitar la enumeración de usuarios. Tanto el caso de "correo no encontrado" como el de "contraseña incorrecta" retornan el mismo mensaje genérico.

10. **Validación de Rango en Evaluaciones SUS+TAM:**
    * Se implementó validación de rango `[1, 5]` en los controladores que reciben los reactivos de las evaluaciones psicométricas, previniendo la corrupción de datos de investigación.

11. **Corrección de Bug del Buscador de Estudiantes:**
    * Se corrigió el error de JavaScript en [`estudiantes.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/estudiantes.html) causado por un ID faltante en el elemento de KPI, que impedía la inicialización del buscador en tiempo real.

#### 14.8.4. Decisiones de Diseño sobre Hallazgos No Corregidos

* **RF-10 (Auditor Socrático):** Se decidió explícitamente no implementar el servicio `SocraticAuditorService` como componente independiente. La auditoría socrática se gestiona de forma unificada dentro de las reglas de la Capa 1 del prompt del sistema, las cuales prohíben al modelo entregar código o soluciones directas. Se actualizaron los comentarios del código para reflejar esta decisión arquitectónica.
* **Spring Security con roles formales:** Se mantiene la autenticación basada en sesión HTTP con validación manual por controlador como decisión de prototipo, dado que la implementación de roles Spring Security formales requeriría una reestructuración mayor del flujo de autenticación actual (correo institucional + 2FA manual).

---

### 14.9. Mecanismo de Finalización de Tutoría por Preguntas Guía y Resguardos de Síntesis Pedagógica (`progreso_md`)

Para resolver la incertidumbre de determinar cuándo un estudiante finaliza su proceso formativo con el tutor socrático y garantizar que el docente siempre cuente con la síntesis diagnóstica en Markdown (`progreso_md`), se implementó un mecanismo híbrido de detección pedagógica de fin de ruta acompañado de cuatro (4) mecanismos de respaldo operativo:

#### 14.9.1. Detección Pedagógica Automática por Hitos Guiados (`<!-- RUTA_COMPLETADA -->`)
* **Enfoque Pedagógico:** En lugar de basarse en temporizadores rígidos o depender exclusivamente del botón manual `[Finalizar Sesión]` (el cual los estudiantes suelen omitir al cerrar la ventana o retirarse tras resolver su duda), el sistema utiliza la comprensión del diálogo por parte de la IA para certificar el cierre.
* **Prompt del Sistema (`capa1_socratico.md`):** Se incorporó la regla pedagógica *5. Culminación de la Ruta de Aprendizaje (Hito Final Superado)*. Cuando el estudiante demuestra asimilación conceptual del último hito o pregunta orientadora de la sesión, el tutor emite una felicitación reflexiva, sintetiza el logro alcanzado y añade de forma silenciosa la meta-etiqueta `<!-- RUTA_COMPLETADA -->`.
* **Procesamiento en Backend (`ChatService.java`):** Al recibir la respuesta del LLM, el servicio detecta la presencia de la meta-etiqueta, la suprime del texto visible presentado al estudiante para preservar la naturalidad del diálogo (`contenidoLimpio`), invoca de manera automática la generación de la síntesis diagnóstica (`cerrarSesionYGenerarProgreso(participacionId)`) y marca la bandera `sesionCompletada = true` en el `MensajeDto`.
* **Interfaz de Chat (`chat.html`):** Al activarse la bandera `sesionCompletada`, la vista despliega un banner institucional de felicitación (`#bannerRutaCompletada`), deshabilita el área de texto para prevenir mensajes redundantes y ofrece acceso directo e inequívoco a la evaluación de usabilidad y aceptación tecnológica (SUS+TAM).

#### 14.9.2. Los Cuatro (4) Mecanismos de Respaldo Operativo
1. **Generación Bajo Demanda en el Dashboard Docente (Lazy Synthesis):**
   * Si un estudiante interactuó con el tutor pero interrumpió la sesión antes de que se detonara el cierre formal, el docente puede visualizar su participación en el dashboard y generar el diagnóstico en tiempo real mediante el botón `[Generar Diagnóstico Pedagógico Ahora]`.
   * Implementado mediante el endpoint seguro `POST /docente/api/participaciones/{participacionId}/generar-diagnostico`, el cual valida que la sesión pertenezca al docente en sesión antes de invocar la IA.
2. **Cierre Masivo al Pausar o Cerrar la Clase:**
   * En [`DocenteController.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/controller/DocenteController.java), al alternar el estado de la sesión (`POST /docente/sesiones/{id}/estado`) hacia inactivo (`activa = false`), el sistema busca automáticamente todas las participaciones abiertas con interacción real (dos o más mensajes) y genera de forma asíncrona/secuencial su respectivo `progreso_md`, asegurando reportes completos al terminar la jornada de clase.
3. **Detección Conversacional Socrática de Cierre:**
   * En el cliente [`chat.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/estudiante/chat.html), un detector léxico analiza si el estudiante introduce expresiones coloquiales de cierre ("gracias tutor, ya entendí", "todo claro", "listo ya terminé", etc.) antes de la culminación formal, desplegando una tarjeta contextual invitándolo a presionar `[Finalizar Sesión]` y completar la encuesta.
4. **Alerta de Salida Accidental en el Navegador (`beforeunload`):**
   * Se integró un manejador de evento `window.addEventListener('beforeunload')` que intercepta intentos involuntarios de cerrar la pestaña o recargar mientras la sesión tenga interacciones activas y aún no haya sido completada o evaluada, previniendo el abandono accidental de la actividad académica.