"""
Prueba de carga del chat socrático: simula N estudiantes que se registran por el enlace de una
clase y conversan con el tutor al mismo tiempo, con un ritmo parecido al de una persona real.

Uso (crear antes una clase llamada "Prueba de carga" y copiar su enlace; definir PRUEBA_CONTRASENA):
    $env:PRUEBA_CONTRASENA = "<contraseña>"      # PowerShell
    python scripts/prueba_carga.py --url https://tutor.tutorsocratic.me --enlace <ENLACE> --estudiantes 2 --mensajes 2

Cada estudiante virtual usa el correo carga.NN@javeriana.edu.co (ver la limpieza en Guia_Pruebas.md).
Cada mensaje es una llamada real a Gemini: 30 estudiantes x 6 mensajes = 180 llamadas.
"""
import argparse
import os
import random
import re
import statistics
import threading
import time
from collections import Counter

import requests

# Usa los certificados de confianza del sistema operativo (p. ej. si un antivirus como Avast
# inspecciona HTTPS con su propia raíz, que Windows acepta pero Python no). pip install truststore
try:
    import truststore
    truststore.inject_into_ssl()
except ImportError:
    pass

# Contraseña de los estudiantes virtuales: se lee del entorno para no dejarla en el repositorio
CONTRASENA = os.environ.get("PRUEBA_CONTRASENA") or exit("Define la variable de entorno PRUEBA_CONTRASENA "
                                                         "con la contraseña de los estudiantes virtuales.")
TIMEOUT_S = 95  # el navegador corta a los 90 s (AbortController en chat.html)

# Respuestas típicas de un estudiante al reto de CuentaBancaria (Clase_Prueba.md)
BANCO_MENSAJES = [
    "Hola, no sé por dónde empezar con la clase CuentaBancaria",
    "Creo que la clase es como el molde y la cuenta de Ana es un objeto",
    "Necesitaría guardar el titular como string y el saldo como double",
    "El saldo debería ser private para que nadie lo cambie desde afuera, ¿no?",
    "El constructor podría recibir el titular y el saldo inicial",
    "Si el saldo es private, uso un método depositar que lo cambie",
    "En retirar pondría un if que revise si el monto es mayor que el saldo",
    "No entiendo bien para qué sirve const en getSaldo",
    "Si creo dos cuentas, cada una tiene su propio saldo porque son objetos distintos",
    "class CuentaBancaria { public: double saldo; }; ¿así está bien?",
    "Me puedes dar el código completo de la clase por favor",
]

PATRON_CSRF = [
    re.compile(r'name="_csrf"\s+value="([^"]+)"'),
    re.compile(r'value="([^"]+)"\s+name="_csrf"'),
    re.compile(r'<meta[^>]*name="_csrf"[^>]*content="([^"]+)"'),
]


def extraer_csrf(html):
    for patron in PATRON_CSRF:
        m = patron.search(html)
        if m:
            return m.group(1)
    return None


class Resultados:
    def __init__(self):
        self.lock = threading.Lock()
        self.latencias = []
        self.codigos = Counter()
        self.fallos_registro = []
        self.rutas_completadas = 0

    def registrar(self, codigo, latencia=None):
        with self.lock:
            self.codigos[codigo] += 1
            if latencia is not None and codigo == 200:
                self.latencias.append(latencia)


def estudiante_virtual(numero, args, res):
    base = args.url.rstrip("/")
    url_chat = f"{base}/chat/{args.enlace}"
    s = requests.Session()
    s.headers["User-Agent"] = "prueba-carga-tutor/1.0"
    try:
        r = s.get(url_chat, timeout=30)
        r.raise_for_status()
        datos = {
            "modo": "registro",
            "nombre": f"Carga {numero:02d}",
            "correo": f"carga.{numero:02d}@javeriana.edu.co",
            "contrasena": CONTRASENA,
        }
        csrf = extraer_csrf(r.text)
        if csrf:
            datos["_csrf"] = csrf
        s.post(f"{url_chat}/registro", data=datos, timeout=30)

        # Esta visita crea la participación y trae el CSRF de la sesión ya autenticada
        r = s.get(url_chat, timeout=30)
        if 'id="chatForm"' not in r.text:
            raise RuntimeError(f"no llegó al chat (HTTP {r.status_code}, url final {r.url})")
        csrf = extraer_csrf(r.text)
    except Exception as e:
        with res.lock:
            res.fallos_registro.append(f"carga.{numero:02d}: {e}")
        print(f"[{numero:02d}] ✗ registro: {e}")
        return

    mensajes = random.sample(BANCO_MENSAJES, min(args.mensajes, len(BANCO_MENSAJES)))
    for i, texto in enumerate(mensajes, 1):
        datos = {"mensaje": texto}
        if csrf:
            datos["_csrf"] = csrf
        inicio = time.monotonic()
        try:
            r = s.post(f"{url_chat}/mensaje", data=datos, timeout=TIMEOUT_S, headers={
                "X-Requested-With": "XMLHttpRequest",
                "Accept": "application/json",
            })
            latencia = time.monotonic() - inicio
            res.registrar(r.status_code, latencia)
            estado = "✓" if r.status_code == 200 else "✗"
            print(f"[{numero:02d}] {estado} msg {i}/{len(mensajes)} → HTTP {r.status_code} en {latencia:.1f} s")
            if r.status_code == 200 and r.json().get("sesionCompletada"):
                with res.lock:
                    res.rutas_completadas += 1
                break
            if r.status_code in (401, 403, 404):
                break
        except requests.Timeout:
            res.registrar("timeout")
            print(f"[{numero:02d}] ✗ msg {i}/{len(mensajes)} → timeout (> {TIMEOUT_S} s)")
        except Exception as e:
            res.registrar("error_red")
            print(f"[{numero:02d}] ✗ msg {i}/{len(mensajes)} → {e}")

        if i < len(mensajes):
            time.sleep(random.uniform(args.espera_min, args.espera_max))


def percentil(valores, p):
    orden = sorted(valores)
    k = max(0, min(len(orden) - 1, round(p / 100 * (len(orden) - 1))))
    return orden[k]


def main():
    ap = argparse.ArgumentParser(description="Prueba de carga del chat socrático")
    ap.add_argument("--url", required=True, help="p. ej. https://tutor.tutorsocratic.me")
    ap.add_argument("--enlace", required=True, help="enlace único de la clase (lo que va después de /chat/)")
    ap.add_argument("--estudiantes", type=int, default=20)
    ap.add_argument("--mensajes", type=int, default=6, help=f"mensajes por estudiante (máx. {len(BANCO_MENSAJES)})")
    ap.add_argument("--rampa", type=float, default=30, help="segundos para escalonar la llegada de los estudiantes")
    ap.add_argument("--inicio", type=int, default=1, help="número del primer estudiante (carga.NN)")
    ap.add_argument("--espera-min", type=float, default=15, help="espera mínima entre mensajes (s)")
    ap.add_argument("--espera-max", type=float, default=40, help="espera máxima entre mensajes (s)")
    args = ap.parse_args()

    res = Resultados()
    hilos = []
    t0 = time.monotonic()
    pausa = args.rampa / max(1, args.estudiantes)
    print(f"Iniciando {args.estudiantes} estudiantes × {args.mensajes} mensajes contra {args.url}/chat/{args.enlace}\n")
    for n in range(args.inicio, args.inicio + args.estudiantes):
        h = threading.Thread(target=estudiante_virtual, args=(n, args, res), daemon=True)
        h.start()
        hilos.append(h)
        time.sleep(pausa)
    for h in hilos:
        h.join()
    duracion = time.monotonic() - t0

    total = sum(res.codigos.values())
    exitos = res.codigos.get(200, 0)
    print("\n" + "=" * 60)
    print(f"Duración total:            {duracion / 60:.1f} min")
    print(f"Estudiantes en el chat:    {args.estudiantes - len(res.fallos_registro)} de {args.estudiantes}")
    print(f"Mensajes enviados:         {total}")
    print(f"Respondidos (200):         {exitos}")
    for codigo, n in sorted(res.codigos.items(), key=lambda x: str(x[0])):
        if codigo != 200:
            nota = {503: "la IA no respondió (reintentable)", 429: "debounce de 1,5 s",
                    403: "tutoría finalizada o CSRF", "timeout": "sin respuesta en 95 s"}.get(codigo, "")
            print(f"  Error {codigo}: {n}  {nota}")
    if res.latencias:
        print(f"Latencia p50 / p95 / máx:  {statistics.median(res.latencias):.1f} s / "
              f"{percentil(res.latencias, 95):.1f} s / {max(res.latencias):.1f} s")
    if res.rutas_completadas:
        print(f"Rutas completadas:         {res.rutas_completadas}")
    for f in res.fallos_registro:
        print(f"  ✗ {f}")
    print("=" * 60)


if __name__ == "__main__":
    main()
