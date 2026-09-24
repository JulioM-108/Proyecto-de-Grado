# Plan de Mejora de Prompt_Reglas.md (Capa Fija del Tutor Socrático)

## 1. Descripción del Objetivo y Contexto

El archivo `Prompt_Reglas.md` constituye la **Capa 1 (Fija / Sistema)** de la arquitectura de prompts en tres capas de la plataforma web de tutoría inteligente. Su función es establecer de manera inmutable las reglas pedagógicas del **método socrático**, garantizando que el modelo de lenguaje (Gemini 3 Flash) guíe al estudiante hacia el aprendizaje significativo en Programación Orientada a Objetos (POO), sin entregar soluciones de código completas o directas que el estudiante pueda copiar.

El objetivo de este documento es definir el diseño y la evolución de `Prompt_Reglas.md`, incorporando:
1. Los principios formales del paper de **Edward Y. Chang (Stanford University, IEEE CCWC 2023): *"Prompting Large Language Models With the Socratic Method"***.
2. La **anatomía equilibrada de la intervención socrática** (Feedback Elaborado + Analogías/Ejemplos + Pregunta Orientadora), evitando que el tutor se convierta en un interrogador frustrante que solo contesta con preguntas.
3. El protocolo de **orquestación de las Preguntas Guía del docente (Capa 2)** para garantizar convergencia temática hacia los objetivos de la clase.

---

## 2. Diagnóstico: Por qué el tutor NO debe limitarse a hacer preguntas

Una interpretación rígida o ingenua del método socrático suele instruir al LLM con órdenes como *"responde siempre y únicamente con una pregunta"*. Esto genera tres graves fallas pedagógicas documentadas en la literatura:
* **Frustración y fatiga cognitiva:** El estudiante siente que habla con una pared o un interrogador policial, lo que deteriora la *seguridad psicológica* (*psychological safety*, advertida por Stoddard & O'Dell, 2016 y Chang, 2023).
* **Bloqueo ante vacíos conceptuales:** Si el estudiante carece por completo del concepto base (ej. no sabe qué es memoria estática vs dinámica, o qué es polimorfismo), ninguna cantidad de preguntas le permitirá "adivinar" algo que nunca ha estudiado.
* **Falsa retención o abandono:** El alumno se desmotiva al no recibir retroalimentación concreta sobre por qué su razonamiento inicial estuvo bien o mal encaminado.

### La Solución Pedagógica: Anatomía de la Intervención Socrática
De acuerdo con la teoría de **Retroalimentación Elaborada (E)** (Castellanos et al., 2026; Evans, 2013) y el paper de Chang (2023), cada turno del tutor socrático debe articular **tres componentes armónicos**:

```
┌────────────────────────────────────────────────────────────────────────┐
│             ANATOMÍA DE UNA RESPUESTA DEL TUTOR SOCRÁTICO               │
├────────────────────────────────────────────────────────────────────────┤
│ 1. Validación Empática y Diagnóstico Inicial                           │
│    - Reconoce el esfuerzo o el acierto parcial del estudiante.          │
│    - Señala con calidez el desajuste conceptual sin juzgar.            │
├────────────────────────────────────────────────────────────────────────┤
│ 2. Andamiaje Conceptual, Analogía o Ejemplo en Dominio Neutro           │
│    - Explica el "modelo mental" detrás del concepto de POO.           │
│    - Ofrece una analogía de la vida real (ej. moldes, planos, cajas).   │
│    - Proporciona un mini-ejemplo conceptual en un contexto NEUTRO      │
│      (ej. Biblioteca, Vehículos) que NO resuelva la tarea del alumno.  │
├────────────────────────────────────────────────────────────────────────┤
│ 3. Pregunta Orientadora de Transferencia (Cierre de Turno)             │
│    - Formula UNA SOLA pregunta reflexiva al final.                     │
│    - Conecta la analogía/ejemplo con el código del alumno o con la     │
│      Pregunta Guía activa del docente.                                 │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Fundamentos del Paper de Edward Y. Chang (2023)

El estudio sistemático de Chang en Stanford formaliza cómo traducir el método socrático a prompts de LLMs:

1. **Pre-dialogue Intent & Task Context (Anclaje de Intención):**
   * El LLM requiere que la meta pedagógica esté definida antes de iniciar el diálogo. En nuestra plataforma, ese rol lo cumplen las **Preguntas Guía del profesor**.
2. **One-by-One Prompting (Interrogación secuencial paso a paso):**
   * Hacer **una sola pregunta de transferencia al final de cada turno**. Múltiples preguntas simultáneas degradan la calidad del razonamiento del estudiante y dispersan la conversación.
3. **Elenchus (Contraexamen causal $r \Rightarrow \Omega$):**
   * Poner a prueba la coherencia lógica de las hipótesis del alumno: *"¿Por qué consideras que hacer ese método estático soluciona el problema de instancias?"*.
4. **Generalización Inductiva:**
   * Elevar la conversación del arreglo del error puntual en una línea de código hacia el **principio general de diseño de POO** (ej. encapsulamiento, bajo acoplamiento, alta cohesión).
5. **Razonamiento Contrafactual (*What-if*):**
   * Probar escenarios alternativos y casos límite para afianzar el modelo mental: *"¿Qué pasaría con esa lista si el usuario no ingresa ningún elemento?"*.

---

## 4. Orquestación de las "Preguntas Guía del Profesor" (Capa 2)

El docente ingresa en la Capa 2 un conjunto estructurado de preguntas guía para la lección. La Capa Fija debe gobernar la navegación mediante cuatro reglas de interacción:

1. **Activación Proactiva al Inicio:**
   * Si el estudiante saluda o dice algo genérico (*"Hola"*, *"Estoy listo"*), el tutor no responde pasivamente. Saluda con entusiasmo y lanza la **Pregunta Guía #1** apoyada en una breve analogía introductoria.
2. **Técnica del Puente (*Bridging*):**
   * Si el estudiante tiene una duda puntual o un error de compilación, el tutor atiende y aclara esa duda socráticamente, pero formula su pregunta de cierre vinculándola hacia la **Pregunta Guía activa**.
3. **Criterio de Progresión Rigurosa:**
   * El tutor solo avanza a la siguiente pregunta guía del docente cuando el estudiante demuestra que comprende y es capaz de explicar el concepto de la pregunta actual con sus propias palabras.
4. **Protección contra la Divagación:**
   * Si el diálogo se desvía de los temas del curso, el tutor reorienta amablemente al estudiante recordando el objetivo de la lección actual.

---

## 5. Estructura Definitiva de `Prompt_Reglas.md`

El archivo de la capa fija se organizará en las siguientes secciones:
1. **Identidad, Rol y Filosofía Pedagógica del Tutor Socrático.**
2. **Restricciones Inviolables Anti-Solución Directa (Guardrails).**
3. **Anatomía de la Respuesta del Tutor (Validación + Explicación/Analogía + Pregunta Única).**
4. **Orquestación de las Preguntas Guía del Docente (Capa 2).**
5. **Métodos de Diálogo Socrático Formales (Chang, 2023):**
   * Mayéutica (descubrimiento guiado).
   * Definición (aclaración de términos ambiguos).
   * Elenchus / Contraexamen (evaluación de consistencia y justificaciones).
   * Generalización Inductiva (de la sintaxis local al principio general de POO).
   * Razonamiento Contrafactual (explorar mutaciones y casos límite).
   * Dialéctica (evaluar alternativas de diseño en tensión).
6. **Escalamiento de Pistas Progresivas (*Scaffolding*):**
   * Nivel 1: Pregunta reflexiva conceptual.
   * Nivel 2: Pista de puntero (aislamiento de atributos AT) + Ejemplo en dominio neutro.
   * Nivel 3: Analogía explicativa profunda de la vida real.
   * *Límite inquebrantable:* Nunca revelar el código del ejercicio del estudiante.
7. **Tipos de Respuesta Prohibidos vs. Respuestas Esperadas (Ejemplos Contrastados).**
8. **Cierre de Hito y Consolidación Metacognitiva.**
