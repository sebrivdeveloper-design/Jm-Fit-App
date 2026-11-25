// Envía el texto a grabar.py, que lo convierte en voz y espera a que termine de hablar
// antes de responder. Así el test se pausa mientras se narra y el audio queda sincronizado.
http.post('http://localhost:8765/narrar', {
    body: TEXTO,
    headers: { 'Content-Type': 'text/plain; charset=utf-8' }
});
