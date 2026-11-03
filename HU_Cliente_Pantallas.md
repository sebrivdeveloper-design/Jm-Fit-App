# HU — Visualización y navegación de pantallas del cliente

**Módulo:** Cliente — JM FIT APP  
**Rol:** Cliente interno  
**Prioridad:** Alta  
**Tipo:** Interfaz y navegación básica

## Historia de usuario

**Como** cliente de JM FIT APP, 
**quiero** visualizar mi pantalla principal, consultar la información de mi entrenamiento y acceder a mi perfil mediante una interfaz intuitiva,  
**para** conocer la estructura de la aplicación y navegar entre sus secciones principales, sin necesidad de ejecutar todas las funcionalidades del sistema.

## Descripción

Se deberán implementar tres pantallas siguiendo los diseños de referencia proporcionados:

1. **Home (Inicio):** pantalla principal del cliente.
2. **Workout (Entrenamiento):** pantalla con el detalle de los ejercicios asignados.
3. **Profile (Perfil):** pantalla con la información personal y el programa del cliente.

El alcance de esta historia de usuario se limita a la visualización de información y navegación entre pantallas.

Los datos podrán ser estáticos o simulados, ya que no se requiere implementar la lógica completa del sistema.

## Criterios de aceptación

### CA01 — Visualización de la pantalla principal (Home)

**Dado que** el cliente ingresa a la aplicación,  
**cuando** accede a la pantalla de inicio,  
**entonces** deberá visualizar:

- Saludo personalizado con su nombre.
- Tarjeta del entrenamiento del día.
- Nombre del entrenamiento, cantidad de ejercicios y duración estimada.
- Botón "Start Workout".
- Resumen semanal: entrenamientos, peso y racha.
- Sección de fotografías de progreso.
- Tarjeta de mensaje del entrenador.
- Tarjeta promocional de suplementos.
- Barra de navegación inferior.

**Funcionalidad requerida:**

El botón **"Start Workout"** deberá dirigir al cliente a la pantalla de detalle del entrenamiento (Workout).

Los demás elementos serán únicamente visuales y no requieren acciones funcionales.

### CA02 — Visualización de la pantalla de entrenamiento (Workout)

**Dado que** el cliente presiona el botón "Start Workout",  
**cuando** se abre la pantalla de entrenamiento,  
**entonces** deberá visualizar:

- Nombre del entrenamiento.
- Día correspondiente al programa.
- Grupo muscular y semana del programa.
- Listado de ejercicios.
- Número de series.
- Número de repeticiones.
- Peso asignado.
- Tiempo de descanso.
- Indicaciones para realizar cada ejercicio.
- Casillas de verificación visuales.
- Barra de navegación inferior.

**Funcionalidad requerida:**

- El botón de regresar deberá permitir volver a Home.
- La barra inferior deberá permitir navegar hacia las pantallas implementadas.

**No se requiere:**

- Registrar series realizadas.
- Guardar entrenamientos.
- Actualizar el progreso.
- Iniciar cronómetros.
- Registrar pesos o repeticiones.
- Marcar ejercicios como completados de manera persistente.

Las casillas de verificación serán únicamente elementos visuales.

### CA03 — Visualización de la pantalla de perfil (Profile)

**Dado que** el cliente selecciona la opción "Profile",  
**cuando** ingresa a su perfil,  
**entonces** deberá visualizar:

- Fotografía o avatar del cliente.
- Nombre completo.
- Fecha de vinculación.
- Programa de entrenamiento actual.
- Entrenador asignado.
- Semana actual del programa.
- Barra de progreso.
- Sesiones por semana.
- Fecha de inicio y próximo seguimiento.
- Información corporal y objetivos.
- Observaciones del entrenador.
- Opciones de historial, pedidos, notificaciones y configuración.
- Botón "Log Out".
- Barra de navegación inferior.

**Funcionalidad requerida:**

La barra de navegación deberá permitir regresar a Home o acceder a Workout.

Los demás elementos serán únicamente visuales.

No se requiere implementar edición de perfil, historial, pedidos, notificaciones, configuración ni cierre de sesión.

### CA04 — Navegación entre pantallas

**Dado que** el cliente se encuentra en cualquiera de las pantallas implementadas,  
**cuando** utiliza las opciones de navegación disponibles,  
**entonces** el sistema deberá mostrar la pantalla correspondiente sin errores.

Se deberán permitir las siguientes transiciones:

| Origen | Acción | Destino |
|---|---|---|
| Home | Start Workout | Workout |
| Home | Profile | Profile |
| Workout | Botón regresar | Home |
| Workout | Home | Home |
| Workout | Profile | Profile |
| Profile | Home | Home |
| Profile | Workout | Workout |

Las opciones **Progress** y **Chat** podrán visualizarse en la barra inferior, pero no tendrán navegación funcional dentro de esta historia de usuario.

### CA05 — Diseño y consistencia visual

**Dado que** el cliente navega por las pantallas,  
**cuando** visualiza cada interfaz,  
**entonces** se deberá respetar el diseño de referencia, incluyendo:

- Paleta de colores: blanco, negro, grises y rojo.
- Tipografía y jerarquía visual.
- Distribución de tarjetas y secciones.
- Espaciados, bordes e iconografía.
- Barra de navegación inferior.
- Diseño adaptable a dispositivos móviles.
- Identificación visual de la sección activa.

### CA06 — Uso de datos simulados

**Dado que** las pantallas corresponden a una demostración académica,  
**cuando** se cargue la información,  
**entonces** se podrán utilizar datos estáticos para representar entrenamientos, estadísticas, información corporal y perfil.

No será obligatorio consultar una API ni almacenar esta información en la base de datos.

## Fuera del alcance

Esta historia de usuario **no incluye**:

- Integración con backend para estas vistas.
- Operaciones CRUD.
- Registro o actualización de entrenamientos.
- Cálculo automático de estadísticas.
- Carga real de fotografías.
- Chat funcional.
- Gestión de pedidos o compras.
- Edición de datos personales.
- Persistencia de cambios.

## Definición de terminado (DoD)

La historia se considerará completada cuando:

- [ ] Las tres pantallas estén implementadas.
- [ ] El diseño sea consistente con las referencias.
- [ ] El botón "Start Workout" abra la pantalla de entrenamiento.
- [ ] La navegación entre Home, Workout y Profile funcione.
- [ ] El botón de regresar funcione correctamente.
- [ ] Los elementos no funcionales se visualicen sin ejecutar acciones.
- [ ] La interfaz sea adaptable a pantallas móviles.
- [ ] La implementación no afecte otras funcionalidades existentes del proyecto.