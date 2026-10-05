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
  └── 3.7 Implementación de la detección de solicitudes de respuesta directa (RF-10)
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
* **RF-04 (Persistencia de Historial dentro de la Sesión):** Almacenar el historial de interacción para que, si el estudiante sale de una sesión sin terminarla, al volver el tutor retome todo lo trabajado en esa misma sesión (el historial completo se inyecta en la Capa 3).
* **RF-05 (Configuración Guiada de Prompts):** Paso a paso estructurado asistido por IA para que el profesor defina tema, nivel y tono bajo CRAFT sin requerir conocimientos avanzados de ingeniería de prompts.
* **RF-06 (Detección de Falencias Críticas):** Visualización en el panel docente de los conceptos con mayor frecuencia de error a nivel individual y grupal.
* **RF-07 (Evolución dentro de la Sesión):** Seguimiento del desempeño del estudiante a lo largo de la sesión (estado de evolución y diagnóstico generado por el docente). *La comparativa entre sesiones distintas queda como trabajo futuro (§13).*
* **RF-08 (Resumen Pedagógico Automatizado):** Generación automática en lenguaje natural de un informe que interprete las métricas del grupo y sugiera intervenciones didácticas concretas al profesor.
* **RF-09 (Monitoreo de Desenganche y Abandono):** Registro de cantidad de sesiones, frecuencia de uso y duración promedio para detectar estudiantes en riesgo.
* **RF-10 (Detección de Solicitudes de Respuesta Directa):** La prevención de entregar código la garantizan las reglas inmutables de la Capa 1. RF-10 detecta y registra (métrica M-04) cada vez que el estudiante pide explícitamente el código o la solución, mediante un patrón de texto normalizado y la meta-etiqueta oculta `<!-- ATAJO_DETECTADO -->` emitida por el tutor (ver §9.1).

### 7.3. Requisitos No Funcionales (RNF)
* **RNF-01 (Confidencialidad y Aislamiento de Conversaciones):** El sistema restringe el acceso al chat exclusivamente al estudiante propietario y al docente titular de la sesión.
* **RNF-02 (Minimización de Datos Personales):** Registro estricto de datos esenciales (nombre y correo institucional), sin almacenar información sensible innecesaria.

### 7.4. Métricas de Seguimiento Pedagógico
| ID | Nombre de la Métrica | Descripción Técnica | Traducción Pedagógica para el Docente | Fuente |
| :--- | :--- | :--- | :--- | :--- |
| **M-01** | **Frecuencia de error por concepto** | Número de veces que un concepto específico genera error en el grupo durante una sesión. | *"Este es el tema que más le está costando al grupo. Considera dedicarle más tiempo en clase."* | (Colby, 2017) |
| **M-02** | **Intentos por concepto** | Número de turnos de conversación requeridos por el estudiante antes de alcanzar la comprensión. | *"Este estudiante necesita muchos intentos para entender el concepto. Puede requerir atención personalizada."* | (Colby, 2017; Kurup et al., 2016) |
| **M-03** | **Evolución en la sesión** | Estado de madurez cognitiva del estudiante en la sesión (*En diálogo activo*, *Dudas en progreso*, *Consolidado*), derivado del diagnóstico generado por el docente. *La comparación entre sesiones distintas queda como trabajo futuro.* | *"Este estudiante consolidó el concepto / sigue con dudas en este punto."* | (Kurup et al., 2016) |
| **M-04** | **Tasa de solicitudes de respuesta directa** | Conteo de ocasiones en que el alumno exige explícitamente el código o la solución directa. | *"Este estudiante muestra señales de frustración. Puede necesitar una estrategia didáctica diferente."* | (Castellanos et al., 2026) |
| **M-05** | **Frecuencia y duración de sesiones** | Número de sesiones completadas y tiempo promedio por sesión por estudiante. | *"Este estudiante no ha usado la plataforma en varios días. Puede estar en riesgo de abandono."* | (Castellanos et al., 2026) |

#### 7.4.1. Implementación real de las métricas
* **M-01** no tiene columna propia: el dashboard la extrae de la sección *"Errores recurrentes"* de los diagnósticos (`progreso_md`) que el docente genera manualmente. Cada barra muestra el **porcentaje de estudiantes con informe generado que presentan ese error** (cada estudiante cuenta una sola vez por error), ordenado de mayor a menor (ver §14.16). Mientras no haya informes, se invita al docente a generarlos.
* **M-02** se calcula con el conteo de mensajes del estudiante; **M-03** con el estado del diagnóstico; **M-05** con la fecha del último mensaje.
* Códigos almacenados en la tabla `metrica` (columna `codigo`):

| `codigo` | `concepto` | Significado | Métrica |
| :---: | :--- | :--- | :---: |
| 2 | Tema de la sesión | Mensajes acumulados en la participación tras cada turno del estudiante | M-02 |
| 3 | `sesion_cerrada` | Marca de que el docente generó el diagnóstico (valor = mensajes en ese momento) | — |
| 5 | `turnos_totales` | Total de mensajes al generar el diagnóstico | apoyo M-05 |
| 10 | `RF-10` | Solicitud de código / respuesta directa detectada en un turno | M-04 |

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
* **Modelo auxiliar de contingencia:** si el modelo principal (`gemini-3-flash-preview`) no responde tras los reintentos, `GeminiService` conmuta a **`gemini-3.1-flash-lite-preview`** (variables `GEMINI_MODEL` y `GEMINI_MODEL_AUXILIARY` del `.env`). Por ser menos fiable con reglas condicionales, sus respuestas **no pueden** cerrar la ruta socrática (§14.12).

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
│  - Historial COMPLETO de la sesión actual (misma participación).       │
│  - (No incluye progreso_md: el diagnóstico es del docente.)           │
│  - Nuevo mensaje enviado por el estudiante.                            │
└────────────────────────────────────────────────────────────────────────┘
```

### 9.1. Prevención de Respuestas Directas y Detección de Atajos (RF-10)
No existe un auditor que reformule las respuestas (decisión del §14.8.4): la prevención de entregar código o soluciones la garantizan las reglas inmutables de la **Capa 1**. RF-10 opera como **detección** de las solicitudes del estudiante:
1. **Patrón de texto** (`ChatService.esSolicitudAtajo`): normaliza el mensaje (sin tildes, mayúsculas ni puntuación) y reconoce verbos de petición + objeto (*"pásame el código"*, *"dame directamente la respuesta"*) y frases completas (*"no quiero preguntas"*).
2. **Señal del tutor**: la Capa 1 instruye al modelo a añadir la meta-etiqueta oculta `<!-- ATAJO_DETECTADO -->` cuando el estudiante pide explícitamente la solución; el backend la elimina antes de mostrar o guardar la respuesta.
Si cualquiera de las dos se activa, se registra la métrica `codigo=10` (M-04) en la tabla `metrica`.

### 9.2. Esquema de Persistencia Híbrida: Numérico + Markdown (`progreso_md`)
Se descartó el almacenamiento intermedio en JSON por riesgo de errores sintácticos de parseo y sobrecosto de deserialización. Se adoptó una persistencia híbrida en **Supabase**:
1. **Datos Cuantitativos:** Columnas numéricas indexadas (conteo de turnos, intentos por concepto, peticiones de respuesta directa) para consultas analíticas directas en SQL.
2. **Datos Cualitativos (`progreso_md`):** Diagnóstico Markdown estructurado que el **docente genera manualmente** desde el dashboard y que se guarda en la tabla `participacion`. Es material de análisis para el profesor: **no** se inyecta en la Capa 3 del tutor. *La reutilización del diagnóstico en sesiones futuras queda como trabajo futuro.*

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
2. Spring Boot recupera de Supabase el historial completo de la sesión actual (misma participación).
3. Se ensambla el prompt con las 3 capas:
   * **Capa 1:** Reglas fijas del sistema.
   * **Capa 2:** CRAFT + Preguntas guía de la sesión.
   * **Capa 3:** Historial completo de la sesión + Nuevo mensaje.
4. El cliente HTTP envía la petición a la API de Gemini 3 Flash (o al modelo auxiliar en contingencia).
5. Se procesan las meta-etiquetas: `ATAJO_DETECTADO` (RF-10) y `RUTA_COMPLETADA` (validada en servidor, §14.12).
6. Spring Boot almacena el mensaje del alumno y la respuesta en la tabla `mensaje`, y actualiza las métricas estructuradas en la tabla `metrica`. Si la IA no responde, no se guarda nada y el estudiante puede reintentar.
7. Thymeleaf renderiza el fragmento HTML y lo entrega al estudiante.

### Flujo 3: Cierre de la Tutoría y Diagnóstico Pedagógico (Manual)
1. La tutoría del estudiante se cierra cuando el tutor certifica (y el servidor valida) que completó la ruta de preguntas guía (`ruta_completada = true`), o cuando el estudiante envía la encuesta SUS+TAM. El *"Feedback Profe"* no cierra la tutoría (§14.17). Al cerrarse **no** se genera ningún diagnóstico.
2. El diagnóstico individual lo genera **solo el docente**, con el botón *"Ver / Generar"* del dashboard (`POST /docente/api/participaciones/{id}/generar-diagnostico`). Gemini responde con la plantilla de 4 secciones en Markdown, que se persiste en `participacion.progreso_md`. Generarlo no cierra la tutoría del estudiante.
3. Las métricas numéricas ya se acumulan en la tabla `metrica` en cada turno.
4. La síntesis grupal también es manual: el docente pulsa *"Generar Síntesis Consolidada con IA"* y Gemini redacta las recomendaciones para la siguiente clase presencial.

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
  * Detección de solicitudes de respuesta directa (RF-10) y validación de la meta-etiqueta de cierre de ruta.
* **Contenedor 3 - Base de Datos (PostgreSQL en Supabase):**
  * Instancia relacional gestionada que persiste usuarios, sesiones, mensajes y métricas.
* **Contenedor 4 - Infraestructura de Ejecución (Docker):**
  * Empaquetado en imagen ligera para ejecución local o en VPS (ej. Render/Servidor privado).
  * `Dockerfile` multi-etapa y `docker-compose.yml` (servicio `tutor-socratico-app`, variables desde `.env`); se ejecuta con `docker compose up --build`. La base de datos no forma parte del compose porque vive en Supabase (servicio externo).

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
3. **Evolución entre Sesiones:** Inyectar en la Capa 3 el diagnóstico de sesiones anteriores del mismo estudiante y comparar su desempeño entre sesiones (versión completa de RF-07 y M-03); el prototipo actual limita la memoria del tutor a la sesión en curso.
4. **Andamiaje (*Scaffolding*) Dinámico en la Zona de Desarrollo Próximo:** Retirar o elevar automáticamente el nivel de asistencia del tutor a medida que el modelo cognitivo confirme que el alumno consolida las competencias de programación.

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
* **Seguridad Criptográfica en Credenciales:** Se implementó el hasheo seguro y salado de contraseñas de estudiantes mediante `BCryptPasswordEncoder`, eliminando el almacenamiento en texto plano y alineando la autenticación de la plataforma con los estándares modernos de seguridad informática. *(Política vigente de contraseñas por rol: ver §14.8.1 #2.)*

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

2. **Política de Contraseñas por Rol:**
   * **Estudiantes (estricto):** la contraseña se hashea con BCrypt al registrarse y al cambiarla; al ingresar se verifica exclusivamente con `BCryptPasswordEncoder.matches()` (`AuthService.verificarPasswordEstudiante`). Una contraseña de estudiante almacenada en texto plano es rechazada.
   * **Docente (prueba controlada):** el docente de la evaluación se crea manualmente en Supabase, por lo que se admite su contraseña en texto plano **sin migrarla** (queda legible en Supabase); si ya fuera un hash BCrypt, se verifica como tal (`AuthService.verificarPasswordDocente`, comparación en tiempo constante).
   * Se eliminó toda migración automática de contraseñas a BCrypt durante el inicio de sesión.

3. **Corrección de IDOR en Eliminación de Estudiantes:**
   * En [`DocenteController.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/controller/DocenteController.java), el endpoint `POST /docente/estudiantes/{estudianteId}/eliminar` fue refactorizado para verificar que el estudiante a eliminar tenga participaciones exclusivamente en sesiones pertenecientes al docente autenticado. Se previene así la eliminación arbitraria de estudiantes ajenos.

4. **Generación Local de Códigos QR para 2FA:**
   * Se reemplazó la dependencia del servicio externo `api.qrserver.com` por la generación local del código QR en el servidor mediante la librería ZXing (`com.google.zxing:core`). El secreto TOTP del docente ya no se transmite a terceros y la imagen QR se retorna como cadena Base64 (`data:image/png;base64,...`) directamente en la vista.

5. **Desacoplamiento Transaccional de Llamadas a la API de Gemini (Connection Pool Starvation):**
   * En [`ChatService.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/service/ChatService.java), se separó el método `procesarMensajeEstudiante()` en tres fases: (1) persistencia del mensaje del estudiante y construcción del prompt en una transacción corta, (2) invocación de la API de Gemini fuera de contexto transaccional, y (3) persistencia de la respuesta del tutor en una segunda transacción corta. Esto evita que una conexión JDBC del pool HikariCP (configurado con un máximo de 13 conexiones, `spring.datasource.hikari.maximum-pool-size=13`) quede retenida durante los 5–35 segundos de la llamada HTTP externa.
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
* **Spring Security con roles formales:** Se mantiene la autenticación basada en sesión HTTP con validación manual por controlador como decisión de prototipo, dado que la implementación de roles Spring Security formales requeriría una reestructuración mayor del flujo de autenticación actual (correo institucional + 2FA manual). Concretamente, `SecurityConfig` deja pasar todas las rutas (`permitAll`) y el control de acceso real lo realizan:
  * los interceptores de `WebMvcConfig`: `/docente/**` exige sesión con rol `DOCENTE` (las rutas `/docente/api/**` responden `401` en JSON) y `/estudiante/**` exige rol `ESTUDIANTE`;
  * las validaciones de cada método de `ChatController` (`/chat/**`), que exigen un `estudianteActivo` en la sesión HTTP y verifican que la participación le pertenezca.

---

### 14.9. Mecanismo de Finalización de Tutoría por Preguntas Guía y Resguardos de Síntesis Pedagógica (`progreso_md`)

Para resolver la incertidumbre de determinar cuándo un estudiante finaliza su proceso formativo con el tutor socrático y garantizar que el docente siempre cuente con la síntesis diagnóstica en Markdown (`progreso_md`), se implementó un mecanismo híbrido de detección pedagógica de fin de ruta acompañado de cuatro (4) mecanismos de respaldo operativo:

#### 14.9.1. Detección Pedagógica Automática por Hitos Guiados (`<!-- RUTA_COMPLETADA -->`)
* **Enfoque Pedagógico:** En lugar de basarse en temporizadores rígidos o depender exclusivamente del botón manual `[Finalizar Sesión]` (el cual los estudiantes suelen omitir al cerrar la ventana o retirarse tras resolver su duda), el sistema utiliza la comprensión del diálogo por parte de la IA para certificar el cierre.
* **Prompt del Sistema (`capa1_socratico.md`):** Se incorporó la regla pedagógica *5. Culminación de la Ruta de Aprendizaje (Hito Final Superado)*. Cuando el estudiante demuestra asimilación conceptual del último hito o pregunta orientadora de la sesión, el tutor emite una felicitación reflexiva, sintetiza el logro alcanzado y añade de forma silenciosa la meta-etiqueta `<!-- RUTA_COMPLETADA -->`.
* **Procesamiento en Backend (`ChatService.java`):** Al recibir la respuesta del LLM, el servicio detecta la presencia de la meta-etiqueta, la suprime del texto visible presentado al estudiante para preservar la naturalidad del diálogo (`contenidoLimpio`), valida el cierre en servidor (§14.12), marca `participacion.ruta_completada = true` y la bandera `sesionCompletada = true` en el `MensajeDto`. *(Actualización §14.13: ya no se genera el diagnóstico automáticamente; solo el docente lo genera manualmente.)*
* **Interfaz de Chat (`chat.html`):** Al activarse la bandera `sesionCompletada`, la vista despliega un banner institucional de felicitación (`#bannerRutaCompletada`), deshabilita el área de texto para prevenir mensajes redundantes y ofrece acceso directo e inequívoco a la evaluación de usabilidad y aceptación tecnológica (SUS+TAM).

#### 14.9.2. Los Cuatro (4) Mecanismos de Respaldo Operativo
1. **Generación Manual en el Dashboard Docente (única vía del diagnóstico):**
   * El docente genera el diagnóstico de cualquier participación con interacción mediante el botón `[Generar Diagnóstico Pedagógico Ahora]`, haya o no completado el estudiante la ruta. Generarlo no cierra la tutoría del estudiante.
   * Implementado mediante el endpoint seguro `POST /docente/api/participaciones/{participacionId}/generar-diagnostico`, el cual valida que la sesión pertenezca al docente en sesión antes de invocar la IA.
2. **Pausar la Clase NO genera diagnósticos (mecanismo retirado):**
   * Una versión anterior generaba de forma masiva el `progreso_md` de todas las participaciones al pausar la sesión. Se eliminó (§14.11): `POST /docente/sesiones/{id}/estado` en [`DocenteController.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/controller/DocenteController.java) solo cambia el estado de la sesión; los diagnósticos se generan únicamente de forma manual por el docente.
3. **Detección Conversacional Socrática de Cierre (retirada en §14.17):**
   * *Se eliminó junto con el botón `[Finalizar Sesión]`, que no guardaba el cierre.* Versión anterior: en el cliente [`chat.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/estudiante/chat.html), un detector léxico analiza si el estudiante introduce expresiones coloquiales de cierre ("gracias tutor, ya entendí", "todo claro", "listo ya terminé", etc.) antes de la culminación formal, desplegando una tarjeta contextual invitándolo a presionar `[Finalizar Sesión]` y completar la encuesta.
4. **Alerta de Salida Accidental en el Navegador (`beforeunload`):**
   * Se integró un manejador de evento `window.addEventListener('beforeunload')` que intercepta intentos involuntarios de cerrar la pestaña o recargar mientras la sesión tenga interacciones activas y aún no haya sido completada o evaluada, previniendo el abandono accidental de la actividad académica.

---

### 14.10. Refinamiento Integral de UI, Detección de Atajos (RF-10) y Analítica Pedagógica

Para optimizar la experiencia de usuario y la precisión diagnóstica de la plataforma, se incorporaron seis (6) mejoras de alto impacto:

1. **Rediseño Modular de la Síntesis Pedagógica Consolidada:**
   * En [`dashboard.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/dashboard.html), se sustituyó el bloque de texto plano por tres (3) tarjetas semánticas estructuradas con la identidad visual institucional Javeriana Cali: *Diagnóstico Grupal*, *Focos de Confusión y Atajos*, y *Recomendación Docente*. Un analizador dinámico en JavaScript formatea automáticamente el Markdown generado por Gemini.
2. **Acceso Unificado para Estudiantes por Pestañas (Tabs):**
   * En [`registro.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/estudiante/registro.html), se implementó un selector de pestañas que diferencia con claridad *"Ya tengo cuenta (Iniciar Sesión)"* (solo solicita correo institucional y contraseña) de *"Soy nuevo estudiante (Registrarme)"* (solicita nombre, correo y contraseña).
   * En [`ChatController.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/controller/ChatController.java), se parametrizó el modo de ingreso, flexibilizando el campo nombre para inicios de sesión recurrentes.
3. **Robustecimiento del Diagnóstico Bajo Demanda:**
   * En [`DocenteController.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/controller/DocenteController.java), se calibraron las respuestas HTTP y mensajes diagnósticos cuando un alumno aún no registra consultas.
   * En [`GeminiService.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/service/GeminiService.java), se normalizó la resolución de alias para modelos auxiliares como `gemini-3.1-flash-lite-preview`.
   * En la interfaz, se integró un componente visual de carga (spinner interactivo) mientras el LLM sintetiza el diálogo.
4. **Detección Activa de Alertas de Atajo Cognitivo (RF-10):**
   * En [`ChatService.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/service/ChatService.java), se integró una expresión regular para capturar intenciones de solicitud de código directo o solución inmediata ("dame el código", "hazme la tarea", etc.), registrando de inmediato la métrica `RF-10` con código `10L` en la tabla `metrica` de Supabase. Esto permite que el contador del dashboard refleje en tiempo real las solicitudes reconducidas con éxito.
5. **Reclasificación Semántica de Turnos por Sesión:**
   * En [`AnalyticsService.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/service/AnalyticsService.java), se eliminó la penalización peyorativa en rojo para estudiantes con 8 o más turnos. Ahora se clasifican como *"Diálogo Perseverante"* si evidencian comprensión o interacción continua, reservando las alertas críticas en rojo únicamente para casos de confusión persistente o múltiples solicitudes de atajo.
6. **Visualización de la Hoja de Ruta de Preguntas Guía:**
   * En [`dashboard.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/dashboard.html), se desplegó la tarjeta *"🧭 Hoja de Ruta Pedagógica: Preguntas Guía de la Sesión"*, listando de forma numerada los hitos secuenciales configurados por el docente para contrastarlos directamente con la analítica de la clase.

---

### 14.11. Optimización del Ciclo de Vida de Métricas e Informes con IA (Lazy Synthesis & Persistencia)

Se atendió una optimización arquitectural crítica en el consumo del motor de IA y el ciclo de vida de las métricas:

1. **Desacoplamiento de Gemini en la Carga del Dashboard Docente (`Lazy Synthesis`):**
   * **Causa del problema previo:** Al acceder a `/docente/sesiones/{id}/metricas`, `AnalyticsService.obtenerMetricasPorSesion(...)` ejecutaba una llamada síncrona obligatoria a Gemini (`generarSintesisIa`), lo que demoraba la carga del panel entre 4 y 8 segundos y consumía tokens en cada recarga de página.
   * **Solución aplicada:** Se implementó una caché en memoria concurrente (`sintesisPorSesionCache`) en [`AnalyticsService.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/service/AnalyticsService.java). La carga del dashboard es ahora instantánea (~50ms) y la síntesis pedagógica consolidada se genera exclusivamente bajo demanda mediante el nuevo endpoint `POST /docente/api/sesiones/{id}/generar-sintesis` invocado desde el botón *"Generar Síntesis Consolidada con IA"* en [`dashboard.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/dashboard.html). Una vez generada, permanece en caché para futuras visitas sin re-invocar a Gemini.

2. **Separación Estricta entre Métricas en Tiempo Real e Informes Cualitativos con IA:**
   * **Métricas discretas acumuladas de inmediato:** Con cada mensaje enviado por el estudiante en [`ChatService.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/service/ChatService.java), se persiste en la BD (`metrica`) el conteo de turnos (`codigo=2L`), las dudas conceptuales y los intentos de atajo directo (`codigo=10L` / `RF-10`). Si el estudiante cierra o abandona la sesión sin terminar, sus métricas numéricas ya quedan consolidadas en tiempo real.
   * **Generación no prematura:** En [`ChatController.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/controller/ChatController.java) y [`DocenteController.java`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/java/Prototipo/Proyecto_Grado/controller/DocenteController.java), se eliminó la generación forzada de diagnósticos con IA al cerrar la pestaña o pausar la clase si la ruta no había culminado.
   * **Persistencia permanente del diagnóstico individual:** El diagnóstico pedagógico individual solo se genera cuando el docente pulsa *"Generar Diagnóstico Pedagógico Ahora"* (desde §14.13, completar la ruta ya no lo genera automáticamente). Una vez generado, se guarda de forma definitiva en `participacion.progreso_md` en la base de datos de Supabase. Al reingresar al panel, el sistema lee directamente el Markdown guardado sin volver a consumir la API de Gemini.

3. **Validación y Reparación Integral del Botón "Configurar Guía":**
   * En [`dashboard.html`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/dashboard.html), se corrigió el selector del modal y se añadió el manejador JavaScript `abrirModalEditarCraft()` que invoca de forma segura la API de Bootstrap (`bootstrap.Modal.getOrCreateInstance`), garantizando apertura fluida e instantánea.
   * Se reestructuró el modal [`#modalEditarCraft`](file:///C:/Users/julio/OneDrive/Universidad/Decimo_Semestre/Tesis/Desarrollo/Proyecto_Grado/src/main/resources/templates/docente/dashboard.html) destacando en la parte superior el área de *"Preguntas Guía del Docente (Hoja de Ruta Socrática)"* con indicaciones pedagógicas claras sobre cómo ingresarlas, seguido de las directrices del marco CRAFT. Los cambios se persisten exitosamente en la BD mediante `POST /docente/sesiones/{id}/craft`.

---

### 14.12. Robustez del Cierre de Tutoría y de la Detección de Atajos (RF-10)

Se corrigieron dos fallas observadas en pruebas: (a) el contador de solicitudes de respuesta directa casi nunca registraba alertas y (b) algunas sesiones se cerraban de forma repentina tras una pregunta del estudiante.

1. **Detección RF-10 normalizada (`ChatService.esSolicitudAtajo`):** el mensaje se normaliza (minúsculas, sin tildes ni puntuación) y se admite hasta 4 palabras de relleno entre el verbo y el objeto (*"dame directamente la respuesta"*, *"escribe un programa que…"*), además de frases completas (*"no quiero preguntas"*, *"dime cómo se hace"*, *"dámelo hecho"*). Los infinitivos solo cuentan tras un modal (*"me puedes dar…"*) para no marcar *"no sé cómo hacer el ejercicio"*.
2. **Doble señal de atajo:** la Capa 1 instruye al tutor a añadir la meta-etiqueta oculta `<!-- ATAJO_DETECTADO -->` ante solicitudes explícitas de código. La métrica `codigo=10` se registra si la detecta el patrón **o** el tutor; la etiqueta se elimina antes de mostrar o guardar la respuesta.
3. **Validación en servidor de `<!-- RUTA_COMPLETADA -->`:** el cierre ya no depende solo del criterio del modelo. Se acepta únicamente si (i) respondió el modelo principal (no el auxiliar *lite*), (ii) la sesión tiene preguntas guía y (iii) el estudiante lleva al menos `max(4, nº de preguntas guía)` turnos. La regla 5 de la Capa 1 se reforzó en el mismo sentido.
4. **El diagnóstico ya no cierra la tutoría:** se agregó la columna `participacion.ruta_completada BOOLEAN NOT NULL DEFAULT FALSE` (creada directamente en Supabase). La tutoría queda finalizada solo si `ruta_completada = true` o el estudiante envió la encuesta SUS+TAM (`Participacion#isTutoriaFinalizada`; el feedback al profesor dejó de cerrarla en §14.17). Generar el diagnóstico desde el panel docente (`progreso_md`) **no** bloquea al estudiante.
5. **Errores de IA fuera de la persistencia:** si ningún modelo responde, no se guarda ni el mensaje del estudiante ni el texto de error; el endpoint responde `503` reintentable. `cerrarSesionYGenerarProgreso` lanza `IllegalStateException` en lugar de guardar el texto de error como diagnóstico, y la síntesis grupal tampoco lo almacena en caché. Como efecto adicional, el mensaje nuevo ya no aparece duplicado en el prompt (antes iba en el historial y en la consulta).
6. **Frontend del chat sin recarga ante errores:** se eliminó el respaldo `form.submit()`. Ante un fallo se retira la burbuja del estudiante, se devuelve su texto al cuadro de entrada y se muestra el error dentro del chat; la entrada solo se bloquea si el servidor indica sesión pausada o tutoría realmente finalizada. Un 403 sin JSON (token CSRF expirado) se trata como sesión de ingreso expirada, no como cierre de tutoría.
---

### 14.13. Alineación Documento–Código (Octubre 2026)

Antes de la evaluación con usuarios reales se revisaron 15 inconsistencias entre este documento y el código. Decisiones aplicadas:

1. **Contraseñas por rol:** BCrypt estricto para estudiantes; el docente de prueba (creado en Supabase) puede usar texto plano, sin migración automática (§14.8.1 #2).
2. **Control de acceso:** se mantiene `permitAll` en Spring Security; la protección la hacen los interceptores de `WebMvcConfig` y las validaciones de `ChatController` (§14.8.4).
3. **Memoria del tutor limitada a la sesión actual:** la Capa 3 recibe el historial **completo** de la participación (antes, solo los últimos 14 mensajes) y ya no incluye `progreso_md`. La evolución entre sesiones pasa a trabajo futuro (§13).
4. **Pausar la clase no genera diagnósticos** (§14.9.2 #2).
5. **Diagnóstico pedagógico solo manual:** completar la ruta marca `ruta_completada` y cierra la tutoría, pero no genera el diagnóstico; los textos del chat dejaron de prometer una síntesis automática.
6. **Asistente CRAFT:** propone **una sola** pregunta orientadora, coherente con la Capa 1.
7. **Sin auditor reformulador:** el §9.1 describe el mecanismo real (reglas de la Capa 1 + detección RF-10); se eliminó el campo sin uso `MensajeDto.reformuladoPorAuditor`.
8. **Modelos de IA alineados con `.env`:** principal `gemini-3-flash-preview`, auxiliar `gemini-3.1-flash-lite-preview` (§8).
9. Diagrama de base de datos (§11.4): pendiente de actualizar.
10. **Códigos de `metrica` documentados** (§7.4.1).
11. **M-01 basada en diagnósticos manuales:** el dashboard indica *"Basado en N de M diagnósticos generados"* (§7.4.1).
12. **Pool HikariCP:** 13 conexiones (§14.8.1 #5).
13. **Sin modificaciones automáticas de datos al arrancar:** se eliminó `DatabaseSchemaInitializer` (creaba/vinculaba `profesor_estudiante`, borraba estudiantes `test.%` e insertaba alertas RF-10). También se eliminó la regla que contaba un atajo cuando el diagnóstico contenía palabras como "atajo" o "respuesta directa": M-04 se basa solo en las métricas `codigo=10` y, como respaldo para datos históricos, en el patrón aplicado a los mensajes del estudiante.
14. **Docker:** el repositorio incluye `Dockerfile` y `docker-compose.yml`; la base de datos vive en Supabase (§11.2).
15. **Código sin uso eliminado:** dependencias `spring-boot-starter-security-oauth2-resource-server` (y su variante de test) y `thymeleaf-extras-springsecurity6`, `AnalyticsService.obtenerMetricasDocente()` y la sobrecarga `PromptAssistantService.generarDirectricesCraft(tema, objetivos)`.

---

### 14.14. Correcciones de Seguridad y Disponibilidad (Octubre 2026)

1. **XSS almacenado en el dashboard docente (severidad alta):** el nombre del estudiante (dato que él mismo controla) se interpolaba dentro de atributos `onclick` del modal de diagnóstico; como un `onclick` decodifica las entidades HTML antes de ejecutar el JavaScript, un nombre con comillas podía ejecutar código con la sesión del docente. Los botones *"Generar / Reintentar"* ahora usan `data-accion` y un listener delegado, sin interpolar datos en código, y `escapeHtml` también escapa comillas.
2. **Límite de intentos en el ingreso por enlace de clase:** `POST /chat/{enlace}/registro` (pestaña *"Ya tengo cuenta"*) aplica el mismo `LoginRateLimiterService` que el login principal (5 fallos → bloqueo de 10 min por correo + IP).
3. **Autorización al eliminar estudiantes:** un docente solo puede eliminar a estudiantes de su cartera o con participaciones en sus sesiones; la cuenta solo se borra de la plataforma si el estudiante no pertenece a otro docente (cartera ni participaciones).
4. **Tiempo máximo de respuesta acotado:** un timeout de lectura del modelo principal pasa directamente al auxiliar (sin reintentar el mismo modelo) y el auxiliar tiene un solo intento, por lo que el peor caso baja de unos 145 s a unos 70 s. El chat aborta la petición a los 90 s y le indica al estudiante que recargue antes de reenviar.
5. **Decisión registrada:** se mantiene el envío del historial completo de la sesión en cada turno (§14.13 #3), aceptando el crecimiento del costo en tokens en sesiones largas.

---

### 14.15. Informe Actualizable y M-02 con Margen por Pregunta Guía (Octubre 2026)

1. **Generar un informe nuevo:** al abrir el diagnóstico de un estudiante, el modal muestra el informe vigente y el botón *"Generar nuevo informe"*. Este vuelve a analizar **toda la conversación actual** del estudiante y **reemplaza** el informe anterior (`POST /docente/api/participaciones/{id}/generar-diagnostico?regenerar=true` → `ChatService.generarDiagnostico(id, true)`). Las marcas `codigo=3/5` de la tabla `metrica` solo se registran en el primer informe.
2. **Actualización de M-01 y M-03:** como ambas métricas se derivan del informe, al cerrar el modal tras generar un informe el panel se recarga para reflejar los nuevos focos de dificultad y el nuevo estado de evolución.
3. **Fuente del informe:** únicamente el historial de mensajes de la participación (estudiante y tutor), enviado a Gemini con la plantilla de 4 secciones. No incluye las preguntas guía, CRAFT, métricas ni feedback.
4. **M-02 con margen por pregunta guía:** se espera un máximo de **3 preguntas del estudiante por cada pregunta guía** (sin preguntas guía se asume un margen de 9). Clasificación:

| Turnos del estudiante | Etiqueta | Color |
| :--- | :--- | :--- |
| 0 | Sin consultas | gris |
| ≤ mitad del margen | Fluido | verde |
| ≤ margen | Ritmo normal | celeste |
| > margen, con señales (≥2 solicitudes de atajo o confusión persistente en el informe) | Requiere apoyo | rojo |
| > margen, sin señales | Diálogo perseverante | azul |

   La etiqueta muestra el margen (por ejemplo *"15 de 15 turnos · Ritmo normal"*). Dentro del margen nunca se marca *"Requiere apoyo"*.

---

### 14.16. M-01 Expresada en Porcentaje de Estudiantes (Octubre 2026)

Antes, cada barra de *"Focos de Dificultad y Falencias (M-01)"* usaba una escala fija sin relación con el grupo: cada aparición llenaba un 20 % y la barra se completaba con 5 apariciones, sin importar el tamaño de la clase. Ahora:

* **Base (100 %):** los estudiantes de la sesión con informe generado.
* **Valor:** cuántos de esos estudiantes presentan el error. Cada estudiante cuenta **una sola vez** por error, aunque su informe lo repita.
* **Etiqueta:** por ejemplo *"50% · 2 de 4 estudiantes"*; barra llena = todos los estudiantes con informe presentan el error.
* **Orden:** de mayor a menor porcentaje.
* **Sin informes:** se muestra la invitación a generarlos. Se eliminó la barra *"Consultas en tema: X"*, que contaba mensajes y no errores.
* **Nota al pie:** indica la base del cálculo (*"Porcentajes calculados sobre N estudiantes con informe generado (de M en la clase)"*).
* Los errores se siguen agrupando por el título exacto que redacta la IA; no se usan categorías fijas.

---

### 14.17. Información Veraz en las Vistas (Octubre 2026)

Se revisó el código buscando información que la plataforma mostraba sin que fuera cierta: datos de relleno, textos que prometían algo que no ocurre y etiquetas que no correspondían a lo calculado. Correcciones:

1. **Síntesis Consolidada del grupo sin texto inventado:** si Gemini falla, se informa el error y no se guarda nada. Antes se mostraba (y se guardaba en caché) un texto prefabricado que afirmaba resultados no verificados.
2. **Síntesis basada en evidencia real:** antes se analizaban como máximo 5 mensajes de toda la clase. Ahora `AnalyticsService.generarYGuardarSintesisIa` envía a Gemini las preguntas guía, las métricas de la sesión, el **informe individual** de cada estudiante que lo tenga y, para quienes no lo tienen, **sus mensajes** (hasta 3000 caracteres por estudiante). Los estudiantes van anonimizados (*"Estudiante N"*) y se le exige a la IA no inventar datos.
3. **Asistente CRAFT honesto:** si la IA no responde, se cargan valores base con el aviso *"La IA no respondió…"* en lugar de *"Directrices generadas correctamente"*.
4. **M-03 sin palabras sueltas:** el estado depende de si el estudiante completó la ruta y de si la sección *"Errores recurrentes"* de su informe lista errores:

| Ruta completada | Informe | Estado |
| :--- | :--- | :--- |
| No | Sin informe | En diálogo activo |
| No | Con errores | Dudas en progreso |
| No | Sin errores | Avanzando sin errores |
| Sí | Sin informe | Completó la ruta · genera el informe |
| Sí | Con errores | **Completó con dudas** |
| Sí | Sin errores | **Consolidado** |

5. **M-02 rojo:** fuera del margen de turnos, *"Requiere apoyo"* exige ≥2 solicitudes de atajo **o** errores recurrentes en el informe (antes, palabras sueltas como "persiste").
6. **M-05:** quien completó la tutoría muestra *"Sesión completada"* y nunca *"En riesgo"*.
7. **KPI de estudiantes:** cuenta quienes han escrito al tutor (*"de N inscritos"*), no a todos los inscritos; el **promedio de turnos** se calcula solo entre ellos.
8. **Fechas de actividad reales:** *"Última actividad"* (directorio) y la fecha bajo M-05 (dashboard) son la del último mensaje del estudiante, no la de inscripción.
9. **Portal del estudiante:** una clase pausada se muestra como *"Pausada por el profesor"* (antes *"Finalizada"*) y se distingue la tutoría *"Completada"*.
10. **Textos del modal de diagnóstico:** *"Este estudiante aún no ha escrito"* o *"Aún no se ha generado el informe…"*, en lugar de *"sesión no cerrada formalmente"*.
11. **Resumen vacío:** sin interacciones no se muestra texto que prometa un análisis automático.
12. **Cierre del chat:** solo al completar la última pregunta guía o al enviar la encuesta SUS+TAM. El *"Feedback Profe"* ya no cierra la tutoría, y se eliminaron el botón *"Finalizar Sesión"* (no guardaba nada), el aviso de cierre por frases como "ya entendí" y el endpoint `POST /chat/{enlace}/cerrar`.
13. **Respuestas simuladas:** sin API key, el texto *"[Modo Mock/Desarrollo]"* ya no se guarda como informe ni como síntesis (`GeminiService.esTextoGeneradoValido`).
14. **CRAFT visible:** la tarjeta del dashboard muestra, para cada campo vacío, el valor que realmente usa el tutor (`ChatService.CRAFT_DEFECTO_*`).
