# Entorno local de prueba: gestión de brigadistas y auto asignación

Levanta lo mínimo para probar en el navegador el panel del coordinador y la auto asignación del brigadista, con
login real y pasando por el API Gateway:

| Pieza | Dónde | Puerto |
|---|---|---|
| BD de MS01 (con usuarios de prueba) | Docker `local-ms01-db` | 5442 |
| BD de MS05 (esquema + migraciones V2–V5) | Docker `local-ms05-db` | 5435 |
| MS01 Identidad (`Login/login`) | `java -jar` | 8081 |
| API Gateway (`ApiGateway`) | `java -jar` | 8080 |
| MS05 Brigadistas (este repo) | `java -jar` | 8085 |
| Frontend (`Frontend`, Vite) | `npm run dev` | 5173 |

MS06 (eventos) no se levanta: `cargar-datos.ps1` y `evento.ps1` hacen la llamada que MS06 le haría a MS05
(`PUT /api/v1/bei/eventos/{id}/estado` con `X-Service-Name: ms06`). Por eso, en el panel de responsabilidades del
brigadista aparece un aviso de que el servicio de eventos no responde; no afecta a las pantallas nuevas.

Requisitos: Docker Desktop abierto, Java 21, Maven y Node. Los scripts son de PowerShell.

## Uso

```powershell
cd Brigadistas\local-test
.\iniciar.ps1            # levanta todo, compila lo que haya cambiado y carga los datos de prueba
.\cargar-datos.ps1       # deja los datos como al inicio (se puede repetir)
.\evento.ps1 -Evento real -Estado FINALIZADO        # cambia el estado de un evento, como MS06
.\evento.ps1 -Evento simulacro -Estado EN_EJECUCION
.\detener.ps1            # apaga todo (conserva las BD)
.\detener.ps1 -Borrar    # apaga y borra las BD
```

Si PowerShell bloquea los scripts: `powershell -ExecutionPolicy Bypass -File .\iniciar.ps1`.
Los registros de cada servicio quedan en `logs\` (ignorado por Git).

Abrir http://localhost:5173.

## Usuarios (contraseña de todos: `CampusSeguro2026*`)

| Correo | Rol | Estado inicial en MS05 |
|---|---|---|
| `coordinador@escuelaing.edu.co` | Coordinador | — |
| `organizador@escuelaing.edu.co` | Organizador (con permisos de MS05 agregados solo en este entorno) | — |
| `brigadista1@escuelaing.edu.co` (Ana María Díaz) | Brigadista | Activa · única en el Edificio A del evento real · 1 incidente en atención, 2 resueltos |
| `brigadista2@escuelaing.edu.co` (Carlos Pardo) | Brigadista | Activo · Edificio B (real) · único en el Edificio F (simulacro) · 1 resuelto |
| `brigadista3@escuelaing.edu.co` (Laura Gómez) | Brigadista | Activa · Edificio B (real) |
| `brigadista4@escuelaing.edu.co` (Diego Ruiz) | Brigadista | Activo · sin edificio |
| `brigadista5@escuelaing.edu.co` (Sofía Mora) | Brigadista | Inactiva |

## Eventos de prueba

* **Evacuación por sismo** — evento real, en ejecución. Edificios A, B, C, D, G, L, Coliseo y "Laboratorio de
  Robótica" (este último no está en el mapa: sale como botón debajo).
* **Simulacro general de evacuación** — simulacro, programado. Edificios B, F, H, I, Biblioteca y Aula Máxima.

## Qué probar

Coordinador → menú › Gestión de brigadistas:
1. Orden: activos agrupados por edificio (A, luego B), activos sin edificio y al final inactivos.
2. Buscar por nombre y filtrar Activos / Inactivos.
3. Desplegar a Ana: 1 en atención y 2 resueltos.
4. Cambiar asignación de Laura (B → C): funciona porque Carlos sigue en B.
5. Cambiar asignación de Ana (A → C): se rechaza porque es la única en A.
6. Pestaña del simulacro: no aparece "Cambiar asignación" (solo eventos reales).

Brigadista → menú › Auto asignación:
1. Sofía (inactiva): ve "Activarse"; tocar un edificio pide activarse primero.
2. Al activarse, toca un edificio del mapa, acepta y queda en rojo como "tu edificio".
3. Cambiarse a otro edificio y quitar la asignación.
4. Ana no puede salir del Edificio A ni quitarse (es la única); Carlos tampoco en el F del simulacro.
5. Con `.\evento.ps1 -Evento simulacro -Estado FINALIZADO` el simulacro desaparece de la auto asignación.

> Solo para desarrollo local: la clave de servicio y la contraseña de estos usuarios no sirven en ningún otro
> entorno. Las llaves JWT se leen de `ApiGateway/private.pem` y `ApiGateway/public.pem`.
