# JM FIT · Base común (login, navegación por rol, datos)


## Usuarios de prueba
| Usuario | Contraseña | Rol / notas |
|---|---|---|
| `admin` | `Admin#2026` | Superadministrador |
| `entrenador` | `Entren#26` | Entrenador (Juan Madroñero). Tiene a Camila, Felipe y Laura |
| `entrenador2` | `Entren#26` | Entrenador (Carolina Ortiz). Tiene a Diego |
| `camila`, `felipe`, `laura`, `diego` | `Cliente#26` | Clientes internos |
| `inactivo` | `Cliente#26` | Cliente interno INHABILITADO (prueba del mensaje de cuenta inhabilitada) |

Los datos de ejemplo se cargan solo la primera vez (`DatabaseSeeder`), incluyendo 15 ejercicios, 2 rutinas estándar
y 2 rutinas personalizadas del entrenador (Hipertrofia — Fase 2 de Camila y Recomposición corporal de Felipe).

## Qué trae
- **Login único** (`ui/auth`): usuario y contraseña, ojo para mostrar/ocultar, mensajes de las HU-1/26/49 adaptados a móvil,
  bloqueo de 5 minutos tras 5 intentos fallidos, sesión persistente y cierre de sesión.
- **Navegación**: un grafo raíz (`nav_main.xml`) y un subgrafo por rol:
  `nav_trainer.xml` (Sebastián), `nav_admin.xml`, `nav_intern.xml`, `nav_store.xml`.
  Cada uno trae una pantalla provisional (`RolePlaceholderFragment`). **Para poner tu pantalla real, cambia el
  `android:name` del `<fragment>` de tu archivo.** El cliente externo entra a `nav_store` desde el enlace
  "Ver catálogo de suplementos" del login (no necesita sesión).
- **Base de datos** (`model/`, `data/`): entidades del MER simplificadas, 4 DAOs y `AppContainer`
  (`(requireActivity().application as JmFitApplication).container`) para obtener repositorios y la sesión.
- **Tema**: colores, botones y campos tomados del diseño (`values/colors.xml`, `styles.xml`, `themes.xml`).
- **Pruebas unitarias**: `PasswordHasherTest` (con vectores calculados en Python) y `LockoutPolicyTest`.

## Acuerdos de datos (rutinas)
- `Rutina` → `RutinaDia` (`diaSemana` 1=lunes…7=domingo) → `RutinaBloque` → `RutinaEjercicio`.
- Cada ejercicio va en su propio bloque `SIMPLE`; series, repeticiones y `pesoKg` (opcional) están en `RutinaEjercicio`.
- Estados: `Rutina` = `ACTIVA` / `INHABILITADA`. `AsignacionRutina` = `ACTIVA` / `FINALIZADA`.
- Un cliente solo puede tener UNA `AsignacionRutina` activa (lo garantiza el repositorio, Room no soporta índices parciales).
- Los nombres de columnas van en camelCase (`diaSemana`, `pesoKg`), no en snake_case como el MER.

## Cómo agregar tus entidades (equipo)
1. Crea tu entidad en `model/` y agrégala a `@Database(entities = [...])` en `AppDatabase`.
2. Sube `version` (queda en 2 con esta base; usa siempre el número más alto al integrar).
3. Crea tu DAO en `data/dao`, expónlo en `AppDatabase` y crea tu repositorio en `data/repository`.
4. Si necesitas datos de ejemplo, agrégalos en `DatabaseSeeder` respetando el orden de las claves foráneas.

