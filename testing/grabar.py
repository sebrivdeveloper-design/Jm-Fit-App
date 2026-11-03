"""
Graba un flujo de Maestro en video y le agrega narración con voz neural.

Uso:
    python testing/grabar.py testing/flows/login_admin.yaml
    python testing/grabar.py testing/flows/login_admin.yaml --voz es-CL-LorenzoNeural

Resultado en testing/videos/<flujo>.mp4 (más un .srt con subtítulos).
Requiere: emulador o teléfono conectado, Maestro, ffmpeg y `pip install edge-tts`.
"""
import argparse
import asyncio
import glob
import http.server
import os
import shutil
import subprocess
import sys
import tempfile
import threading
import time
from pathlib import Path

import edge_tts

PUERTO = 8765
LIMITE_SEGMENTO = 170  # screenrecord en Android corta a los 180 s; grabamos por tramos


def buscar(nombre, extras):
    ruta = shutil.which(nombre)
    if ruta:
        return ruta
    for patron in extras:
        hallados = glob.glob(os.path.expandvars(os.path.expanduser(patron)))
        if hallados:
            return hallados[0]
    sys.exit(f"No encontré '{nombre}'. Revisa que esté instalado.")


ADB = buscar("adb", [r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"])
MAESTRO = buscar("maestro", [r"~\.maestro\maestro\bin\maestro.bat"])
FFMPEG = buscar("ffmpeg", [r"%LOCALAPPDATA%\Microsoft\WinGet\Packages\Gyan.FFmpeg*\*\bin\ffmpeg.exe"])
FFPROBE = str(Path(FFMPEG).with_name("ffprobe.exe" if os.name == "nt" else "ffprobe"))


def duracion(archivo):
    salida = subprocess.run(
        [FFPROBE, "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", archivo],
        capture_output=True, text=True, check=True,
    )
    return float(salida.stdout.strip())


def adb(*args):
    return subprocess.run([ADB, *args], capture_output=True, text=True)


class Grabadora:
    """
    Graba la pantalla en tramos de menos de 180 s (límite de Android y del emulador).
    En el emulador graba desde el PC (`adb emu screenrecord`), porque el screenrecord
    interno de algunas imágenes de emulador (ej. Android 7) falla. En un teléfono usa screenrecord.
    """

    def __init__(self, carpeta):
        self.carpeta = carpeta
        self.tramos = []
        self.parar = threading.Event()
        self.hilo = threading.Thread(target=self._grabar, daemon=True)
        self.inicio = None
        self.error = None
        serial = adb("get-serialno").stdout.strip()
        if not serial or serial == "unknown":
            sys.exit("No hay ningún emulador o teléfono conectado (adb devices).")
        self.es_emulador = serial.startswith("emulator-")

    def _tramo_emulador(self, n):
        local = os.path.join(self.carpeta, f"tramo_{n}.webm")
        r = adb("emu", "screenrecord", "start", "--time-limit", str(LIMITE_SEGMENTO), local)
        if "OK" not in r.stdout:
            raise RuntimeError(f"El emulador no pudo grabar: {r.stdout}{r.stderr}")
        return local, None

    def _tramo_telefono(self, n):
        remoto = f"/sdcard/jmfit_grabacion_{n}.mp4"
        proceso = subprocess.Popen(
            [ADB, "shell", "screenrecord", "--bit-rate", "6000000",
             "--time-limit", str(LIMITE_SEGMENTO), remoto],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        return remoto, proceso

    def _detener_tramo(self, proceso):
        if self.es_emulador:
            adb("emu", "screenrecord", "stop")
        else:
            adb("shell", "pkill", "-2", "screenrecord")  # SIGINT cierra bien el mp4
            proceso.wait()

    def _grabar(self):
        n = 0
        try:
            while not self.parar.is_set():
                ruta, proceso = (self._tramo_emulador if self.es_emulador else self._tramo_telefono)(n)
                comienzo = time.monotonic()
                if self.inicio is None:
                    time.sleep(0.4)  # la grabación tarda un poco en arrancar
                    self.inicio = time.monotonic()
                self.tramos.append(ruta)
                while not self.parar.is_set() and time.monotonic() - comienzo < LIMITE_SEGMENTO - 1:
                    if proceso is not None and proceso.poll() is not None:
                        break
                    time.sleep(0.2)
                if proceso is not None and proceso.poll() is not None and time.monotonic() - comienzo < 3:
                    raise RuntimeError("screenrecord falló al arrancar en el teléfono.")
                self._detener_tramo(proceso)
                n += 1
        except Exception as e:  # no reintentar en bucle: guardar el error y salir
            self.error = e
            if self.inicio is None:
                self.inicio = time.monotonic()

    def empezar(self):
        self.hilo.start()
        while self.inicio is None:
            time.sleep(0.05)
        if self.error:
            sys.exit(f"No se pudo grabar la pantalla: {self.error}")

    def terminar(self):
        self.parar.set()
        self.hilo.join()
        time.sleep(1.5)  # dar tiempo a que el archivo se cierre
        if self.es_emulador:
            return [t for t in self.tramos if os.path.exists(t) and os.path.getsize(t) > 0]
        locales = []
        for i, remoto in enumerate(self.tramos):
            local = os.path.join(self.carpeta, f"tramo_{i}.mp4")
            if adb("pull", remoto, local).returncode == 0:
                locales.append(local)
            adb("shell", "rm", "-f", remoto)
        return locales


def servidor_narracion(carpeta, voz, grabadora, narraciones):
    class Manejador(http.server.BaseHTTPRequestHandler):
        def do_POST(self):
            largo = int(self.headers.get("Content-Length", 0))
            texto = self.rfile.read(largo).decode("utf-8").strip()
            archivo = os.path.join(carpeta, f"voz_{len(narraciones)}.mp3")
            asyncio.run(edge_tts.Communicate(texto, voz).save(archivo))
            segs = duracion(archivo)
            momento = time.monotonic() - grabadora.inicio
            narraciones.append((momento, segs, texto, archivo))
            print(f"  [{momento:6.1f}s] 🔊 {texto}")
            time.sleep(segs + 0.3)  # el test espera a que termine de hablar
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"ok")

        def log_message(self, *args):
            pass

    servidor = http.server.ThreadingHTTPServer(("127.0.0.1", PUERTO), Manejador)
    threading.Thread(target=servidor.serve_forever, daemon=True).start()
    return servidor


def tiempo_srt(s):
    ms = int(round(s * 1000))
    return f"{ms // 3600000:02}:{ms // 60000 % 60:02}:{ms // 1000 % 60:02},{ms % 1000:03}"


def armar_video(tramos, narraciones, salida, carpeta):
    # Quitar el inicio muerto (escritorio mientras Maestro arranca): empezar 2 s antes de la primera narración.
    recorte = max(0.0, narraciones[0][0] - 2) if narraciones else 0.0

    lista = os.path.join(carpeta, "tramos.txt")
    with open(lista, "w", encoding="utf-8") as f:
        for t in tramos:
            f.write(f"file '{Path(t).as_posix()}'\n")

    comando = [FFMPEG, "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", lista]
    for _, _, _, audio in narraciones:
        comando += ["-i", audio]

    if narraciones:
        filtros = []
        for i, (momento, _, _, _) in enumerate(narraciones, start=1):
            ms = max(0, int(momento * 1000))
            filtros.append(f"[{i}:a]adelay={ms}|{ms}[a{i}]")
        entradas = "".join(f"[a{i}]" for i in range(1, len(narraciones) + 1))
        filtros.append(f"{entradas}amix=inputs={len(narraciones)}:normalize=0,apad[voz]")
        comando += ["-filter_complex", ";".join(filtros), "-map", "0:v", "-map", "[voz]",
                    "-c:a", "aac", "-b:a", "160k", "-shortest"]
    else:
        comando += ["-map", "0:v"]

    comando += ["-ss", f"{recorte:.2f}", "-vf", "fps=30", "-c:v", "libx264", "-pix_fmt", "yuv420p",
                "-crf", "20", "-movflags", "+faststart", salida]
    subprocess.run(comando, check=True)

    with open(Path(salida).with_suffix(".srt"), "w", encoding="utf-8") as f:
        for i, (momento, segs, texto, _) in enumerate(narraciones, start=1):
            t = momento - recorte
            f.write(f"{i}\n{tiempo_srt(t)} --> {tiempo_srt(t + segs)}\n{texto}\n\n")


def main():
    parser = argparse.ArgumentParser(description="Graba un flujo de Maestro con narración.")
    parser.add_argument("flujo", help="archivo .yaml de Maestro")
    parser.add_argument("--voz", default="es-MX-DaliaNeural",
                        help="voz de edge-tts (ej. es-CL-CatalinaNeural, es-ES-AlvaroNeural)")
    parser.add_argument("--salida", help="ruta del video final (.mp4)")
    args = parser.parse_args()

    flujo = Path(args.flujo).resolve()
    salida = Path(args.salida or Path(__file__).parent / "videos" / f"{flujo.stem}.mp4").resolve()
    salida.parent.mkdir(parents=True, exist_ok=True)

    carpeta = tempfile.mkdtemp(prefix="jmfit_grabacion_")
    narraciones = []
    grabadora = Grabadora(carpeta)

    print(f"▶ Grabando {flujo.name} ...")
    grabadora.empezar()
    servidor = servidor_narracion(carpeta, args.voz, grabadora, narraciones)
    entorno = dict(os.environ, MAESTRO_CLI_NO_ANALYTICS="1",
                   MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED="true")
    try:
        resultado = subprocess.run([MAESTRO, "test", str(flujo)], env=entorno)
    finally:
        servidor.shutdown()
        time.sleep(1)  # un segundo extra al final del video
        tramos = grabadora.terminar()

    if not tramos:
        sys.exit("No quedó ningún video grabado.")
    print("🎬 Armando video con narración ...")
    armar_video(tramos, narraciones, str(salida), carpeta)
    shutil.rmtree(carpeta, ignore_errors=True)

    estado = "✅ Test pasó" if resultado.returncode == 0 else "❌ Test falló (el video muestra hasta dónde llegó)"
    print(f"{estado}\n📹 {salida}\n📝 {salida.with_suffix('.srt')}")
    sys.exit(resultado.returncode)


if __name__ == "__main__":
    main()
