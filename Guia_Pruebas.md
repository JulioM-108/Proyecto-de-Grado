# Guía de pruebas antes de la sesión con estudiantes

Servidor: `https://tutor.tutorsocratic.me`

**Preparación:**
- Dos navegadores: **A** = profesor y **B** = estudiante (ventana de incógnito o teléfono).
- La clase de prueba está en `Clase_Prueba.md` (CuentaBancaria, 8 preguntas guía).
- Marca cada casilla al comprobarla. Si algo falla, anótalo en la sección final.

---

## 1. Profesor: acceso y perfil (navegador A)

- [ ] **1.1** Inicia sesión con el rol **Docente**. → Llegas a *Mis Sesiones*.
- [ ] **1.2** Pulsa *Configurar 2FA*, escanea el QR con Google o Microsoft Authenticator y escribe el código. → El indicador cambia a "Activo".
- [ ] **1.3** Cierra sesión y vuelve a entrar. → Te pide el código de 6 dígitos y entras con él.
- [ ] **1.4** Desactiva el 2FA con una contraseña **incorrecta**. → Aparece un aviso rojo y el 2FA sigue "Activo".
- [ ] **1.5** Desactiva el 2FA con la contraseña **correcta**. → Aparece el aviso "2FA desactivado".
- [ ] **1.6** Pulsa *Editar Perfil*, cambia la facultad y guarda. → Aparece el aviso "Perfil actualizado".

## 2. Profesor: crear la clase

- [ ] **2.1** Pulsa *Crear Nueva Clase*, escribe el tema y pulsa *Sugerir Directrices CRAFT con IA*. → Se llenan los 5 campos CRAFT.
- [ ] **2.2** Reemplaza los campos con los de `Clase_Prueba.md` y pega las 8 preguntas guía (sin líneas extra). Crea la clase. → Aparece en *Mis Sesiones* con su enlace.
- [ ] **2.3** Copia el enlace de la clase.

## 3. Estudiante: acceso (navegador B)

- [ ] **3.1** Abre el enlace e intenta registrarte con un correo `@gmail.com`. → Aparece el error de correo institucional.
- [ ] **3.2** Regístrate con un correo `@javeriana.edu.co`. → Entras al chat y el tema de la clase aparece arriba.
- [ ] **3.3** Cierra la ventana, vuelve a abrir el enlace y elige *Ya tengo cuenta* con la misma contraseña. → Entras al chat con el historial intacto.
- [ ] **3.4** En otra ventana de incógnito, intenta entrar 5 veces con una contraseña incorrecta. → En el 5.º intento aparece "bloqueado durante 10 minutos".

## 4. Estudiante: conversación con el tutor

- [ ] **4.1** Escribe "Hola, quiero empezar". → El tutor presenta el reto de CuentaBancaria y te hace **una sola pregunta** sobre lo que ya sabes. Todavía no plantea la Pregunta Guía #1 ni dice cuántas preguntas hay.
- [ ] **4.1b** Responde lo que sabes. → El tutor ajusta su explicación a tu respuesta y plantea la Pregunta Guía #1.
- [ ] **4.1c** Con otro estudiante, empieza directamente con una duda: "¿qué diferencia hay entre clase y objeto?". → El tutor atiende la duda sin hacer la pregunta diagnóstica, y en el mensaje siguiente plantea la Pregunta Guía #1.
- [ ] **4.1d** Escribe un mensaje en inglés, por ejemplo "Can you explain encapsulation?". → El tutor responde **en español** y recuerda con amabilidad que la tutoría es en español.
- [ ] **4.2** Escribe "dame el código completo de la clase". → El tutor **no** da el código y te devuelve una pregunta.
- [ ] **4.3** Pega un código con un error, por ejemplo `class CuentaBancaria { public: double saldo; };`. → El tutor señala la línea sin escribir la solución.
- [ ] **4.4** Pega un texto de más de 2500 caracteres. → Aparece el aviso de límite y no se envía.
- [ ] **4.5** Recarga la página (F5). → La conversación sigue completa.
- [ ] **4.6** En el navegador A, pausa la clase. En B, intenta escribir. → Aparece "sesión pausada por el profesor". Reanuda la clase en A.
- [ ] **4.7** Responde bien las 8 preguntas, una por una. → Cuando el tutor valida la última, aparece el banner de **ruta completada** y el chat se cierra.
- [ ] **4.8** Revisa la encuesta SUS+TAM. → **Ninguna respuesta viene marcada**, y no deja enviar si falta alguna.
- [ ] **4.9** Responde la encuesta y envíala. → Aparece la confirmación y no se pueden enviar más mensajes.
- [ ] **4.10** Envía el *Feedback al profesor*. → Se guarda y la tutoría **no** se reabre.

## 5. Profesor: dashboard de la clase (navegador A)

- [ ] **5.1** Abre el dashboard de la clase. → El KPI "Han escrito al tutor" muestra 1 de N inscritos.
- [ ] **5.2** Revisa la fila del estudiante:
  - **M-02** muestra "X de 24 turnos" (8 preguntas × 3).
  - **M-03** muestra "Completó la ruta · genera el informe".
  - **M-05** muestra "Sesión completada".
- [ ] **5.3** Revisa las alertas de atajo. → Aparece al menos 1, por el paso 4.2.
- [ ] **5.4** Pulsa *Ver Diagnóstico → Generar informe*. → Aparece el informe con sus 4 secciones y M-03 cambia a "Consolidado" o "Completó con dudas".
- [ ] **5.5** Pulsa *Generar nuevo informe* y cierra el modal. → El informe se reemplaza y M-01 (focos de dificultad) se actualiza en %.
- [ ] **5.6** Genera la síntesis grupal. → Aparece un texto basado en los estudiantes de la clase, no uno genérico.
- [ ] **5.7** Abre la pestaña *Feedback*. → Aparece el comentario del paso 4.10.
- [ ] **5.8** En *Mis Estudiantes*, revisa al estudiante. → Aparece con su última actividad.

## 6. Teléfono

- [ ] **6.1** En el teléfono, haz el paso 3.2 y envía 2 mensajes. → El cuadro de texto queda visible sobre el teclado y nada se sale de la pantalla.
- [ ] **6.2** En el teléfono, abre el dashboard del profesor. → La tabla de estudiantes se ve como tarjetas y los botones son accesibles.

## 7. Prueba de carga (15–30 estudiantes simultáneos)

1. Como profesor, crea una clase **"Prueba de carga"** con los mismos datos de `Clase_Prueba.md` y copia su enlace (lo que va después de `/chat/`).
2. Si el computador tiene un antivirus que inspecciona HTTPS (como Avast), instala una vez `pip install truststore`. Si no, Python rechaza el certificado.
3. En la carpeta `Proyecto_Grado`, haz primero una prueba pequeña:
   ```
   python scripts/prueba_carga.py --url https://tutor.tutorsocratic.me --enlace ENLACE --estudiantes 2 --mensajes 2
   ```
   Debe terminar con "Respondidos (200): 4" y sin errores.
4. Corrida real (unos 5 minutos):
   ```
   python scripts/prueba_carga.py --url https://tutor.tutorsocratic.me --enlace ENLACE --estudiantes 20 --mensajes 6 --inicio 3
   ```
   Después repítela con `--estudiantes 30 --inicio 23`. `--inicio` evita reutilizar los mismos correos.
5. Mientras corre, abre el dashboard de la clase y comprueba que carga bien.

**Criterio de aprobación:**
- 0 errores 5xx y 0 timeouts.
- Latencia p95 por debajo de 20 s.
- Algunos 503 aislados ("la IA no respondió") son aceptables, porque el estudiante puede reintentar. Si son muchos, revisa los límites de Gemini.

## 8. Limpieza al terminar

1. Borra la clase **"Prueba de carga"** y la clase de prueba funcional desde *Mis Sesiones*. Esto borra también sus mensajes, métricas y participaciones.
2. En Supabase → SQL Editor, borra los estudiantes virtuales:
   ```sql
   DELETE FROM profesor_estudiante
   WHERE estudiante_id IN (SELECT id FROM "Estudiante" WHERE correo_institucional LIKE 'carga.%@javeriana.edu.co');

   DELETE FROM "Estudiante" WHERE correo_institucional LIKE 'carga.%@javeriana.edu.co';
   ```
3. Si te registraste como estudiante de prueba con tu propio correo, bórralo desde *Mis Estudiantes → Eliminar*.

---

## Fallos encontrados

| Paso | Qué pasó | Captura / hora |
|---|---|---|
|  |  |  |
