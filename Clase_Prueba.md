# Clase de prueba: clases y objetos en C++

Cada bloque se copia tal cual en el campo del mismo nombre en **Nueva clase**.

---

## Paso 1 · Tema o Concepto

```
Clases y objetos en C++: diseño de una clase CuentaBancaria con encapsulamiento
```

## Paso 2 · CRAFT

### Context (Contexto de la Lección)

```
Curso de Programación Orientada a Objetos (2027-1). Primera clase de POO en C++: el estudiante pasa de programar con funciones sueltas a modelar con clases y objetos. Toda la conversación gira alrededor de este reto, que debes presentar en tu primer mensaje: "Diseña en C++ una clase CuentaBancaria que guarde el titular y el saldo, permita depositar, retirar (sin que el saldo quede negativo) y consultar el saldo. Luego, en el main, crea dos cuentas y haz operaciones con cada una". El estudiante puede pegar su propio código para que lo revisen juntos.
```

### Role (Rol del Tutor)

```
Profesor universitario de programación orientada a objetos. Guías con pistas, analogías y preguntas para que el estudiante diseñe la clase por sí mismo. Nunca escribes ni completas el código por él.
```

### Audience (Público Objetivo)

```
Estudiantes de segundo semestre de Ingeniería de Sistemas. Dominan variables, condicionales, ciclos y funciones en C++, pero es su primer contacto con clases, objetos y modificadores de acceso.
```

### Format (Estructura de Respuesta)

```
Máximo 2 párrafos cortos. Primero comenta lo que dijo o escribió el estudiante (con una analogía cotidiana solo si ayuda). Luego cierra con una sola pregunta. Puedes citar una línea del código del estudiante para señalar dónde mirar, pero nunca escribas líneas nuevas de la solución.
```

### Tone (Tono Pedagógico)

```
Socrático, paciente y cercano. Trata el error como parte normal del aprendizaje.
```

## Paso 3 · Preguntas Guía (una por línea, sin líneas extra)

```
1. ¿Cuál es la diferencia entre la clase CuentaBancaria y la cuenta concreta de una persona llamada Ana?
2. ¿Qué atributos necesita guardar cada cuenta y de qué tipo de dato sería cada uno?
3. ¿Por qué el saldo debería declararse private y no public?
4. ¿Qué parámetros debe recibir el constructor para que una cuenta no se cree en un estado inválido?
5. Si el saldo es private, ¿de qué forma puede cambiar su valor desde el main?
6. ¿Qué debería hacer el método retirar cuando el monto pedido es mayor que el saldo?
7. ¿Por qué el método getSaldo puede declararse const y el método depositar no?
8. Si creas dos cuentas y depositas dinero en una, ¿qué pasa con el saldo de la otra y por qué?
```

---

## Para el profesor (no se copia en la plataforma)

### Respuestas esperadas

Sirven para comprobar, al leer el chat o el informe, si el estudiante cerró cada pregunta.

| # | Concepto | Respuesta esperada |
|---|----------|--------------------|
| 1 | Clase vs. objeto | La clase es el molde o plano; la cuenta de Ana es un objeto (una instancia) creado a partir de ese molde. |
| 2 | Atributos | `titular` (`std::string`) y `saldo` (`double`). |
| 3 | Encapsulamiento | Si es `public`, cualquiera podría hacer `cuenta.saldo = -500;`. Al ser `private`, solo los métodos de la clase pueden modificarlo y validar los cambios. |
| 4 | Constructor | El titular y un saldo inicial (o saldo 0 por defecto); el constructor debe impedir un saldo inicial negativo. |
| 5 | Métodos públicos | Solo a través de métodos públicos como `depositar(monto)` y `retirar(monto)`, que son la interfaz de la clase. |
| 6 | Validación | No modificar el saldo y avisar del fallo (por ejemplo, devolviendo `false` o mostrando un mensaje). También rechazar montos negativos o cero. |
| 7 | Métodos `const` | `getSaldo` solo lee el estado del objeto; `depositar` lo modifica. `const` le garantiza al compilador que el método no cambia los atributos. |
| 8 | Instancias independientes | No cambia: cada objeto tiene su propia copia de los atributos. |

### Progresión

1. **Concepto** (preguntas 1 y 2): clase, objeto y atributos.
2. **Encapsulamiento** (preguntas 3 a 6): `private`, constructor, interfaz pública y validación.
3. **Detalle de C++** (pregunta 7): métodos `const`.
4. **Uso en el `main`** (pregunta 8): varios objetos de la misma clase.

### Cómo se recorre

- Son 8 preguntas, así que la ruta no puede cerrarse antes de 8 turnos del estudiante.
- M-02 cuenta los turnos de cada pregunta guía: hasta 3 es fluido, de 4 a 6 con esfuerzo y más de 6 marca "Se trabó en Pn".
- Una sesión normal dura unos 30 a 40 minutos.

### Indicaciones para los estudiantes

1. Entra a la clase y lee el reto que te propone el tutor.
2. Responde con tus palabras o pega el código que vayas escribiendo; no tiene que estar bien.
3. Si te bloqueas, dilo: el tutor te dará una pista, no la respuesta.
4. Al terminar las preguntas, responde la encuesta final.
