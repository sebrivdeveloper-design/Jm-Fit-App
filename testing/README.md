# Pruebas grabadas con narración

Graba un test de [Maestro](https://maestro.dev) en video y le agrega voz en español (edge-tts).

## Grabar

Con el emulador abierto y la app instalada:

```powershell
python testing\grabar.py testing\flows\login_admin.yaml
```

Sale en `testing/videos/login_admin.mp4` (más un `.srt` con subtítulos).

Cambiar la voz: `--voz es-CL-CatalinaNeural` (otras: `es-MX-DaliaNeural`, `es-MX-JorgeNeural`,
`es-CL-LorenzoNeural`, `es-ES-ElviraNeural`). Lista completa: `python -m edge_tts --list-voices`.

## Escribir un flujo nuevo

Copia `flows/login_admin.yaml`. Para narrar, pon este bloque donde quieras que hable;
el test se pausa mientras habla:

```yaml
- runScript:
    file: ../narrar.js
    env:
      TEXTO: "Ahora abrimos el carrito."
```

Acciones útiles: `tapOn: { id: "btnX" }`, `tapOn: "Texto visible"`, `inputText: "..."`,
`scrollUntilVisible`, `assertVisible`, `back`. Para ver los ids de la pantalla: `maestro studio`.

Correr sin grabar: `maestro test testing\flows\login_admin.yaml`.

`launchApp: clearState: true` borra los datos de la app en el dispositivo para empezar desde el login.

## Requisitos (ya instalados en este PC)

- Maestro (`~\.maestro\maestro\bin`), Java 17+
- ffmpeg (`winget install Gyan.FFmpeg`)
- `pip install edge-tts` (necesita internet para generar la voz)
