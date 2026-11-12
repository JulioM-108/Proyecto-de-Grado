"""
Prueba por perfiles: simula estudiantes con comportamientos distintos para ver cómo se registra cada
caso en las métricas del dashboard (M-01 a M-04), el informe automático y la síntesis grupal.
Al final muestra una tabla por estudiante y lo que debería verse en el dashboard para cada perfil.

Las respuestas y palabras clave están escritas para la clase "Relaciones e interacciones entre objetos"
(5 preguntas guía, ver CLAVES y RESPUESTAS). Para otra clase hay que cambiar esos dos bloques.

Requisitos: Python 3 con requests (pip install requests); truststore es opcional.

Uso (antes definir la contraseña de los estudiantes virtuales: $env:PRUEBA_CONTRASENA = "<contraseña>"):
    python scripts/prueba_perfiles.py --url http://localhost:8080 --enlace <ENLACE>
    python scripts/prueba_perfiles.py --url http://localhost:8080 --enlace <ENLACE> --perfiles fluido=2,atajos=1 --inicio 10

Perfiles (correo carga.<perfil>.NN@javeriana.edu.co; el nombre visible se toma de NOMBRES):
    fluido    responde bien cada pregunta guía hasta completar la ruta
    trabado   responde bien, pero se confunde varias veces en la pregunta 3 (qué tipo de relación usar)
    atajos    pide el código o la solución en la mitad de sus mensajes y abandona
    abandona  responde las 2 primeras preguntas y se va sin terminar
    directo   empieza con una duda concreta (sin saludar) y se va a mitad de ruta
    ingles    escribe en inglés para comprobar que el tutor responde en español

Cada mensaje es una llamada real a Gemini (más el informe automático de quienes completan la ruta).
Limpieza: al terminar, borre los estudiantes virtuales (correos que empiezan por "carga.") desde la
sección Mis Estudiantes.
"""
import argparse
import os
import random
import re
import threading
import time
import unicodedata

import requests

try:
    import truststore
    truststore.inject_into_ssl()
except ImportError:
    pass

# Contraseña de los estudiantes virtuales: se lee del entorno para no dejarla en el repositorio
CONTRASENA = os.environ.get("PRUEBA_CONTRASENA") or exit("Define la variable de entorno PRUEBA_CONTRASENA "
                                                         "con la contraseña de los estudiantes virtuales.")
TIMEOUT_S = 95

# ─── Clase de la prueba: "Relaciones e interacciones entre objetos" (5 preguntas guía) ──
# 1. Diferencia entre asociación, agregación y composición, con un ejemplo de cada una.
# 2. Cómo se representan las relaciones en UML y qué aporta la multiplicidad.
# 3. Qué analizar para decidir qué tipo de relación usar entre dos clases.
# 4. Cómo colaboran los objetos para una funcionalidad que ninguno resuelve solo.
# 5. Cómo determinar qué responsabilidades debe asumir cada objeto.

# Palabras clave de cada pregunta (sobre texto normalizado, sin tildes)
CLAVES = {
    1: r"asociacion|agregacion|composicion|todo.parte|ejemplo de cada|tipos de relacion",
    2: r"\buml\b|diagrama|multiplicidad|rombo|flecha|cardinalidad|0\.\.\*|1\.\.\*",
    3: r"decidir|elegir|criterio|que tipo de relacion|ciclo de vida|al disenar|cuando usar",
    4: r"colabor|mensaje|delega|funcionalidad|ninguno puede|interactu",
    5: r"responsabilidad|quien deberia|experto en (la )?informacion|cohesion|acoplamiento|grasp",
}

RESPUESTAS = {
    1: ["La asociación es una relación de uso entre objetos independientes, como un profesor y un estudiante. La agregación es un todo-parte débil: un equipo tiene jugadores que pueden existir sin el equipo. La composición es un todo-parte fuerte: una casa y sus habitaciones, que desaparecen si se destruye la casa.",
        "La diferencia clave es el ciclo de vida: en la asociación cada objeto vive por su cuenta, en la agregación las partes sobreviven al todo y en la composición las partes mueren con el todo."],
    2: ["En UML la asociación es una línea simple, la agregación lleva un rombo vacío del lado del todo y la composición un rombo relleno. La multiplicidad, como 1, 0..1 o 1..*, indica cuántos objetos de una clase se relacionan con uno de la otra.",
        "Por ejemplo, Casa 1 ◆── 1..* Habitación: cada casa tiene al menos una habitación y cada habitación pertenece a una sola casa. La multiplicidad agrega esa restricción de cantidad que la línea sola no dice."],
    3: ["Analizaría si una clase puede existir sin la otra, quién controla el ciclo de vida de las partes, si las partes se pueden compartir entre varios todos y qué tan fuerte es la dependencia.",
        "Si la parte no tiene sentido sin el todo y no se comparte, uso composición; si puede existir sola o compartirse, agregación; y si solo se usan temporalmente, asociación."],
    4: ["Colaboran enviándose mensajes, es decir, llamando a métodos de otros objetos y delegando tareas. Por ejemplo, un Pedido le pide a cada Producto su precio y a un objeto Pago que cobre el total; ninguno puede hacerlo todo solo.",
        "Cada objeto hace su parte y delega el resto: el pedido coordina, los productos saben su precio y el pago conoce cómo cobrar."],
    5: ["Asignaría cada responsabilidad al objeto que tiene la información necesaria para cumplirla, como dice el principio del experto en información, buscando alta cohesión y bajo acoplamiento.",
        "Por ejemplo, calcular el total le corresponde al Pedido porque conoce sus productos; así cada clase tiene responsabilidades claras y depende lo menos posible de las demás."],
}

DIAGNOSTICO = "Sí, ya sé crear clases y objetos; que un objeto pueda tener otros objetos como atributos lo he visto, pero no las relaciones entre clases."

# Perfil "trabado": confusiones antes de entender la pregunta 3
PREGUNTA_TRABADO = 3
CONFUSIONES_TRABADO = [
    "No sé cómo decidir, ¿no es lo mismo agregación y composición?",
    "Creo que siempre usaría composición porque es más fuerte, ¿está mal?",
    "No entiendo qué tiene que ver el ciclo de vida con elegir la relación",
    "¿Entonces la herencia también es una agregación?",
    "Me perdí, ¿la multiplicidad decide si es composición?",
    "Sigo sin ver qué criterio usar, para mí cualquiera sirve",
]

# Nombres ficticios para que cada estudiante virtual se distinga en el dashboard
NOMBRES = [
    "Valentina Rojas", "Santiago Gómez", "Isabella Martínez", "Mateo Hernández", "Mariana López",
    "Sebastián Díaz", "Gabriela Torres", "Nicolás Ramírez", "Daniela Castro", "Samuel Vargas",
    "Sara Moreno", "Alejandro Jiménez", "Luciana Ruiz", "Tomás Herrera", "Antonella Medina",
    "Emiliano Aguilar", "Salomé Ortiz", "Martín Restrepo", "Juliana Cárdenas", "David Ospina",
    "Camila Peña", "Andrés Salazar", "Paula Quintero", "Felipe Arango", "Laura Mejía",
    "Esteban Cortés", "Manuela Giraldo", "Juan Pablo Ríos", "Natalia Duque", "Simón Valencia",
]

ATAJOS = [
    "Dame el diagrama UML completo con todas las relaciones ya hechas",
    "No quiero más preguntas, pásame la respuesta",
    "Hazlo tú y escríbeme el código de las clases, por favor",
]


def normalizar(texto):
    """Minúsculas, sin tildes y con espacios simples, para comparar con CLAVES."""
    t = unicodedata.normalize("NFD", texto.lower())
    t = "".join(c for c in t if unicodedata.category(c) != "Mn")
    return re.sub(r"\s+", " ", t)


def pregunta_detectada(respuesta, actual):
    """Pregunta guía a la que se refiere el final de la respuesta del tutor (donde va su pregunta)."""
    final = normalizar(respuesta)[-450:]
    hay = {n: bool(re.search(p, final)) for n, p in CLAVES.items()}
    # Prefiere avanzar: si el cierre del tutor ya habla de la siguiente pregunta, pasa a esa
    if hay.get(actual + 1):
        return actual + 1
    if hay.get(actual):
        return actual
    # Si salta más adelante (p. ej. el tutor agrupó preguntas), sigue a la más cercana
    adelante = [n for n, h in hay.items() if h and n > actual]
    return min(adelante) if adelante else actual


def parece_espanol(texto):
    """Heurística: True si en el texto hay más palabras frecuentes del español que del inglés."""
    t = " " + normalizar(texto) + " "
    es = sum(t.count(w) for w in (" el ", " la ", " que ", " de ", " es ", " una ", " para "))
    en = sum(t.count(w) for w in (" the ", " is ", " and ", " you ", " what ", " of "))
    return es > en


PATRON_CSRF = [
    re.compile(r'name="_csrf"\s+value="([^"]+)"'),
    re.compile(r'value="([^"]+)"\s+name="_csrf"'),
    re.compile(r'<meta[^>]*name="_csrf"[^>]*content="([^"]+)"'),
]


def extraer_csrf(html):
    """Devuelve el token CSRF del formulario o de la etiqueta meta de la página, o None si no hay."""
    for patron in PATRON_CSRF:
        m = patron.search(html)
        if m:
            return m.group(1)
    return None


class Estudiante:
    """Estudiante virtual con su propia sesión HTTP: ejecuta el guion de su perfil (métodos perfil_*)
    y acumula turnos, atajos, errores y si completó la ruta, para el resumen final."""
    def __init__(self, perfil, numero, args, nombre=None):
        self.perfil, self.numero, self.args = perfil, numero, args
        self.etiqueta = f"{perfil}.{numero:02d}"
        self.correo = f"carga.{perfil}.{numero:02d}@javeriana.edu.co"
        self.nombre = nombre or f"Carga {perfil.capitalize()} {numero:02d}"
        self.base = f"{args.url.rstrip('/')}/chat/{args.enlace}"
        self.s = requests.Session()
        self.s.headers["User-Agent"] = "prueba-perfiles-tutor/1.0"
        self.csrf = None
        self.turnos = 0
        self.atajos = 0
        self.completada = False
        self.errores = []
        self.respuestas_en_espanol = []
        self.ultima = ""

    def log(self, texto):
        print(f"[{self.etiqueta:<12}] {texto}", flush=True)

    def entrar(self):
        """Entra al chat por el enlace: prueba el registro y luego el inicio de sesión hasta llegar al chat,
        y guarda el token CSRF de esa página."""
        r = self.s.get(self.base, timeout=30)
        r.raise_for_status()
        for modo in ("registro", "login"):
            datos = {"modo": modo, "nombre": self.nombre, "correo": self.correo, "contrasena": CONTRASENA}
            csrf = extraer_csrf(r.text)
            if csrf:
                datos["_csrf"] = csrf
            self.s.post(f"{self.base}/registro", data=datos, timeout=30)
            r = self.s.get(self.base, timeout=30)
            if 'id="chatForm"' in r.text:
                self.csrf = extraer_csrf(r.text)
                return
        raise RuntimeError("no llegó al chat (¿ya completó la ruta en una corrida anterior?)")

    def enviar(self, texto):
        """Envía un mensaje (con una pausa aleatoria antes, salvo el primero); devuelve el texto del tutor o None si falló."""
        if self.turnos > 0:
            time.sleep(random.uniform(self.args.espera_min, self.args.espera_max))
        datos = {"mensaje": texto}
        if self.csrf:
            datos["_csrf"] = self.csrf
        inicio = time.monotonic()
        try:
            r = self.s.post(f"{self.base}/mensaje", data=datos, timeout=TIMEOUT_S,
                            headers={"X-Requested-With": "XMLHttpRequest", "Accept": "application/json"})
        except requests.RequestException as e:
            self.errores.append(str(e))
            self.log(f"✗ error de red: {e}")
            return None
        latencia = time.monotonic() - inicio
        self.turnos += 1
        if r.status_code != 200:
            self.errores.append(f"HTTP {r.status_code}")
            self.log(f"✗ turno {self.turnos} → HTTP {r.status_code} ({latencia:.1f} s)")
            return None
        data = r.json()
        self.ultima = data.get("tutorContenido", "") or ""
        self.completada = bool(data.get("sesionCompletada"))
        corto = texto if len(texto) <= 60 else texto[:57] + "..."
        self.log(f"✓ turno {self.turnos} ({latencia:.1f} s): «{corto}»" + ("  → RUTA COMPLETADA" if self.completada else ""))
        return self.ultima

    # ─── Perfiles ────────────────────────────────────────────────────────────

    def recorrer_ruta(self, max_turnos, confundirse_en=None, confusiones=()):
        """Responde la pregunta guía que plantea el tutor hasta completar la ruta o agotar turnos."""
        resp = self.enviar("Hola, quiero empezar")
        if resp is None:
            return
        resp = self.enviar(DIAGNOSTICO)
        actual, usados = 1, {}
        confusiones = list(confusiones)
        while resp is not None and not self.completada and self.turnos < max_turnos:
            actual = pregunta_detectada(resp, actual)
            if actual == confundirse_en and confusiones:
                texto = confusiones.pop(0)
            else:
                variantes = RESPUESTAS.get(actual, RESPUESTAS[max(RESPUESTAS)])
                k = usados.get(actual, 0)
                texto = variantes[min(k, len(variantes) - 1)]
                if k >= len(variantes):
                    texto += " ¿Con eso ya quedaría clara esta parte?"
                usados[actual] = k + 1
            resp = self.enviar(texto)

    def perfil_fluido(self):
        self.recorrer_ruta(max_turnos=18)

    def perfil_trabado(self):
        self.recorrer_ruta(max_turnos=24, confundirse_en=PREGUNTA_TRABADO, confusiones=CONFUSIONES_TRABADO)

    def perfil_atajos(self):
        guion = ["Hola", ATAJOS[0], RESPUESTAS[1][0], ATAJOS[1], "No sé, ¿qué es la multiplicidad?", ATAJOS[2]]
        for texto in guion:
            if self.enviar(texto) is None:
                return
            if texto in ATAJOS:
                self.atajos += 1

    def perfil_abandona(self):
        self.enviar("Hola, quiero empezar")
        self.enviar("Nunca he visto relaciones entre clases, solo clases sueltas.")
        self.enviar(RESPUESTAS[1][0])
        self.enviar(RESPUESTAS[2][0])

    def perfil_directo(self):
        self.enviar("¿Cuál es la diferencia entre agregación y composición? No me queda clara.")
        self.enviar(RESPUESTAS[1][0])
        self.enviar(RESPUESTAS[2][0])
        self.enviar(RESPUESTAS[3][0])

    def perfil_ingles(self):
        for texto in ("Hi, can you explain what composition between classes is?",
                      "I think composition means the part cannot exist without the whole."):
            resp = self.enviar(texto)
            if resp is None:
                return
            self.respuestas_en_espanol.append(parece_espanol(resp))

    def correr(self):
        """Destino del hilo: entra al chat y ejecuta el método perfil_<perfil>."""
        try:
            self.entrar()
        except Exception as e:
            self.errores.append(f"registro: {e}")
            self.log(f"✗ registro: {e}")
            return
        getattr(self, f"perfil_{self.perfil}")()


# Lo que debería verse en el dashboard para cada perfil (se imprime al final)
ESPERADO = {
    "fluido":   "M-02 'Ruta completada en X turnos' con P1…P5 cortos · informe automático ~15 s después · M-04 'Sesión completada'",
    "trabado":  "M-02 'Ruta completada · se trabó en P3' (P3 con > 6 turnos) · en el grupo, P3 'la que más le cuesta'",
    "atajos":   "M-03 '3 · 50 % · Posible frustración' · M-02 sin completar · aparece en 'informes pendientes'",
    "abandona": "M-02 'Avanzando · va en P3 de 5' (o P2) · aparece en 'informes pendientes'",
    "directo":  "El tutor atiende la duda sin pregunta diagnóstica · M-02 'Avanzando' · informe pendiente",
    "ingles":   "Respuestas del tutor en español · M-02 'Avanzando' · informe pendiente",
}


def main():
    """Crea los estudiantes de cada perfil, lanza un hilo por estudiante cada 1,5 s y muestra el resumen."""
    ap = argparse.ArgumentParser(description="Prueba por perfiles de las métricas del dashboard")
    ap.add_argument("--url", required=True, help="p. ej. http://localhost:8080")
    ap.add_argument("--enlace", required=True, help="enlace de una clase creada con Clase_Prueba.md")
    ap.add_argument("--perfiles", default="fluido=6,trabado=4,atajos=4,abandona=4,directo=3,ingles=3",
                    help="perfiles y cuántos estudiantes de cada uno, p. ej. fluido=2,atajos=1")
    ap.add_argument("--inicio", type=int, default=1, help="número del primer estudiante de cada perfil")
    ap.add_argument("--espera-min", type=float, default=6)
    ap.add_argument("--espera-max", type=float, default=14)
    args = ap.parse_args()

    estudiantes = []
    for item in [p.strip() for p in args.perfiles.split(",") if p.strip()]:
        perfil, _, cantidad = item.partition("=")
        if not hasattr(Estudiante, f"perfil_{perfil}"):
            raise SystemExit(f"Perfil desconocido: {perfil}")
        for k in range(int(cantidad or 1)):
            nombre = NOMBRES[len(estudiantes) % len(NOMBRES)]
            estudiantes.append(Estudiante(perfil, args.inicio + k, args, nombre))

    print(f"Iniciando {len(estudiantes)} estudiantes contra {args.url}/chat/{args.enlace}\n")
    t0 = time.monotonic()
    hilos = []
    for e in estudiantes:
        h = threading.Thread(target=e.correr, daemon=True)
        h.start()
        hilos.append(h)
        time.sleep(1.5)
    for h in hilos:
        h.join()

    print("\n" + "=" * 100)
    print(f"Duración: {(time.monotonic() - t0) / 60:.1f} min\n")
    print(f"{'Estudiante':<26}{'Perfil':<10}{'Turnos':>7}  {'Ruta':<10}{'Atajos':>7}  Observación")
    for e in estudiantes:
        obs = []
        if e.perfil == "ingles" and e.respuestas_en_espanol:
            obs.append("tutor en español: " + ("sí" if all(e.respuestas_en_espanol) else "NO"))
        if e.errores:
            obs.append("errores: " + ", ".join(e.errores[:3]))
        print(f"{e.nombre:<26}{e.perfil:<10}{e.turnos:>7}  {'completada' if e.completada else '—':<10}{e.atajos:>7}  {'; '.join(obs)}")
    print("\nQué deberías ver en el dashboard:")
    for perfil in dict.fromkeys(e.perfil for e in estudiantes):
        print(f"  · {perfil:<9} {ESPERADO[perfil]}")
    print("\nDespués: pulsa 'Generar informes pendientes' y, al terminar, revisa M-01 y la síntesis (se regenera sola).")
    print("=" * 100)


if __name__ == "__main__":
    main()
