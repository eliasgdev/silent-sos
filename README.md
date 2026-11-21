# SilentSOS v1.2

App Android (Kotlin + Jetpack Compose + Hilt) para pedir ayuda de forma discreta. Al activarse:

1. Obtiene la ubicación GPS (espera 8 s como máximo).
2. Envía un **SMS** silencioso a cada contacto de emergencia (hasta 3) con el enlace de Google Maps, la precisión, la hora y la batería.
3. Abre **WhatsApp** con el mismo mensaje ya escrito. Si el *envío automático* está activo, lo envía sin tocar la pantalla y además comparte la **ubicación en tiempo real oficial de WhatsApp** (8 h), a todos los contactos uno tras otro. Si no está activo, solo se abre el chat del contacto principal.
4. Inicia el **seguimiento en tiempo real**: un servicio en primer plano envía la ubicación por SMS cada 1, 2, 5 o 10 min hasta que se pulsa "Estoy a salvo".
5. **Llama** al número de emergencia configurado (123 por defecto).

## Formas de activar la alerta
- Botón **SOS** en la pantalla principal, con 5 s de cuenta regresiva para poder cancelar.
- Botón SOS en la pantalla de ingreso, sin escribir el PIN (opcional).
- **Acceso rápido** (cuenta regresiva de 5 s a pantalla completa, cancelable):
  - Mantener presionado el ícono de la app → *Enviar SOS*.
  - Botón **SOS** en los ajustes rápidos (bajar el panel → editar → arrastrar "SOS").
- **Doble apertura**: abrir la app 2 veces en menos de 10 s envía la alerta sin cuenta regresiva (opcional). Después de una alerta hay 2 min de espera antes de que pueda volver a dispararse.

## Otras funciones
- **Hasta 3 contactos**: se escriben a mano o se eligen de la agenda (el selector del sistema no requiere el permiso de contactos). No se aceptan números repetidos.
- **Historial** de alertas y pruebas: origen, hora, SMS y WhatsApp enviados, si hubo llamada y un enlace para ver la ubicación en el mapa. Se guardan las últimas 50.
- **Cambiar PIN** desde Configuración → Seguridad.
- **Confirmación de SMS**: la app pide al operador confirmación de envío y de entrega de cada SMS y muestra por contacto "✓ entregado", "enviado, esperando confirmación" o el motivo del fallo (sin señal, modo avión, rechazado por la red…). Si el envío falla, lo reintenta una vez a los 15 s.
- **Aviso de envío automático apagado** en la pantalla principal, con acceso directo para activarlo.
- **Aviso si no hay contactos**: si se pide una alerta sin contactos guardados (por ejemplo desde el acceso rápido antes de configurar la app), se muestra "La alerta no se envió" en lugar de no hacer nada.

## Arquitectura
```
ui/ (screens + viewmodel)  →  domain/usecase  →  data/repository/EmergencyRepository
                                                   ├─ utils/LocationHelper        (Fused Location)
                                                   ├─ utils/SmsAlertManager       (SmsManager)
                                                   ├─ utils/WhatsAppManager       (intent api.whatsapp.com/send)
                                                   ├─ utils/EmergencyCallManager  (TelecomManager.placeCall)
                                                   ├─ utils/AlertMessageBuilder   (textos de los mensajes)
                                                   ├─ utils/AppOpenTracker        (doble apertura)
                                                   ├─ utils/TextoSms              (texto apto para SMS)
                                                   └─ utils/PinHasher             (hash del PIN)
domain/model                     ContactoEmergencia, RegistroAlerta, EstadoAlerta
service/TrackingService          servicio en primer plano (tipo location) para el seguimiento
service/WhatsAppAutoSendService  accesibilidad: envía el mensaje y la ubicación en tiempo real en WhatsApp
service/SosTileService           botón SOS de los ajustes rápidos
```
Los datos se guardan solo en el teléfono (SharedPreferences). El PIN se guarda como hash SHA-256 con salt.

## Cómo funciona WhatsApp
WhatsApp no tiene una API pública para que una app envíe mensajes desde la cuenta personal del usuario. SilentSOS abre el chat del contacto con el mensaje escrito y luego:
- **Con envío automático** (servicio de accesibilidad activo), el servicio:
  1. pulsa "Enviar" para mandar el mensaje;
  2. abre 📎 → Ubicación → *Ubicación en tiempo real*, elige 8 h (15 min en el mensaje de prueba) y pulsa Enviar. Si no se puede compartir en tiempo real, envía la ubicación actual;
  3. vuelve a la pantalla de inicio. Con varios contactos espera a terminar un chat antes de abrir el siguiente.

  Solo actúa durante los 35 s siguientes a que SilentSOS prepara una alerta.
- **Sin envío automático**: el usuario tiene que tocar "Enviar" (y compartir la ubicación a mano si quiere).
- Las actualizaciones periódicas van solo por SMS: el mapa de WhatsApp ya se actualiza solo.
- Si WhatsApp tiene **bloqueo con huella**, la alerta se detiene en esa pantalla. Para que funcione: WhatsApp → Ajustes → Privacidad → Bloqueo de app → *Después de 30 minutos* (o desactivarlo).

Para activar el envío automático: Configuración → *Activar envío automático* → Apps instaladas → SilentSOS → activar.
En Android 13 o superior, con la APK instalada fuera de la Play Store, Android lo bloquea como "Ajuste restringido". Para desbloquearlo: Ajustes → Apps → SilentSOS → ⋮ → *Permitir ajustes restringidos*, y luego activarlo.

## Compilar, probar e instalar
```bash
./gradlew testDebugUnitTest   # 28 pruebas unitarias
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
APK generada: `app/build/outputs/apk/debug/app-debug.apk`

## Probar sin generar una emergencia real
1. Configura como contacto un número tuyo o de un compañero, con el indicativo (ej. `+57 300…`).
2. En "Llamada", cambia el número 123 por un número de prueba o desactiva la llamada.
3. Usa **Enviar mensaje de prueba**: envía el SMS y el WhatsApp con la ubicación, sin llamar ni iniciar el seguimiento.

## Pruebas
**Unitarias** (`app/src/test`, 28 pruebas): limpieza de números y JSON de contactos, historial, conversión de texto para SMS, hash y validación del PIN, y la regla de la doble apertura (ventana, enfriamiento, reloj hacia atrás).

**En emulador (Android 17, Pixel 8):**
| Caso | Resultado |
|---|---|
| Bienvenida → crear cuenta (validación de nombre, PIN y confirmación) → inicio | OK |
| Mensaje de prueba: SMS con enlace de Maps y precisión | OK |
| SOS con cuenta regresiva → SMS de alerta + seguimiento + llamada | OK |
| Seguimiento: SMS "actualización #1" con la nueva posición al minuto | OK |
| "Estoy a salvo" → servicio detenido + SMS final con la última ubicación | OK |
| Doble apertura (2 aperturas en < 10 s) → alerta silenciosa | OK |
| v1.1: migración del contacto único de la v1.0 a la lista | OK |
| v1.1: agregar contacto (rechaza número vacío o repetido) | OK |
| v1.1: cambiar PIN (rechaza un PIN actual incorrecto, el nuevo funciona al reingresar) | OK |
| v1.1: acceso rápido → cuenta regresiva → alerta a 2 contactos (SMS 2/2) | OK |
| v1.1: acceso rápido → Cancelar → no se envía nada | OK |
| v1.1: ajuste rápido "SOS" abre la cuenta regresiva | OK |
| v1.1: historial con el registro y el enlace al mapa | OK |
| v1.1.1: acceso rápido sin contactos → aviso "La alerta no se envió" | OK |

**En teléfono real (Samsung Galaxy A36, Android 16):**
| Caso | Resultado |
|---|---|
| SOS → SMS de alerta con ubicación (±12 m), hora y batería | OK |
| Seguimiento: SMS "actualización #1" al minuto | OK |
| WhatsApp sin envío automático: abre el chat con la alerta escrita y vista previa del mapa | OK |
| SMS: confirmación de entrega del operador (estado "✓ entregado") | OK |
| SMS rechazado por el operador → la app lo muestra con el código | OK |
| WhatsApp con envío automático: texto + ubicación en tiempo real (15 min en la prueba) a 2 contactos seguidos | OK |
| "Estoy a salvo" → seguimiento detenido + SMS final | OK |
| Aviso en la pantalla principal cuando el envío automático está apagado | OK |

Los SMS se envían sin tildes ni símbolos especiales a propósito: así caben 160 caracteres por SMS (en vez de 70) y se evita un error de Android 16+ al dividir mensajes Unicode.

## Limitaciones conocidas
- Android no deja que una app que no es el marcador predeterminado llame directamente a números de emergencia oficiales (123, 911). En ese caso se abre el marcador con el número listo para confirmar.
- El envío automático por WhatsApp depende de los ids de la interfaz de WhatsApp (`send`, `input_attach_button`, `pickfiletype_location_holder`, `live_location_btn`, `duration_480_min`). Si WhatsApp cambia su interfaz, puede ser necesario actualizarlos.
- La ubicación en tiempo real de WhatsApp sigue activa las 8 h aunque se pulse "Estoy a salvo"; se detiene desde el chat de WhatsApp.
- El SMS tiene el costo normal del plan del usuario. Si el operador no lo entrega (saldo, filtros antispam, envío al mismo número), la app lo muestra pero no puede evitarlo.
- Si se **fuerza la detención** de SilentSOS (Ajustes → Apps → Forzar detención), Android apaga su servicio de accesibilidad y hay que volver a activarlo. La pantalla principal avisa cuando pasa.

---

<a href="https://avirexis.com/?utm_source=github&utm_medium=readme&utm_campaign=silent-sos"><picture><source media="(prefers-color-scheme: dark)" srcset="https://avirexis.com/avx-logo-nav-white.png"><img src="https://avirexis.com/avx-logo-nav.png" alt="Avirexis" height="22"></picture></a>

Hecho por [Avirexis](https://avirexis.com/?utm_source=github&utm_medium=readme&utm_campaign=silent-sos), software de gestión para negocios hecho en Colombia.
