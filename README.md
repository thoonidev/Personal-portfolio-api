# Portfolio API

Backend monolítico de contacto con Spring Boot 4.1.1, Maven y **Java 17**. Está separado del frontend, en la carpeta hermana `portfolio-api`.

## Arquitectura

- `controller/`: contacto y comprobación de salud.
- `dto/`: validaciones y respuestas públicas.
- `service/`: coordinación, envío HTTPS y límites en memoria.
- `config/`: propiedades, cliente HTTP, CORS y tamaño de petición.
- `exception/`: errores públicos sin datos privados del proveedor.

No requiere base de datos, SMTP ni perfiles de Spring adicionales. Se utiliza un solo `application.yml`.

## Por qué cambió el envío

Render gratuito bloquea los puertos SMTP habituales. El backend ahora llama a **Resend mediante HTTPS**, manteniendo Gmail como posible destinatario. Las variables antiguas `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME` y `MAIL_PASSWORD` ya no se leen y pueden quitarse de la configuración de ejecución de STS.

## Configurar Resend y probar en local

1. Crear una cuenta en Resend. Para recibir pruebas sin dominio propio, usar como correo de la cuenta la misma dirección en la que se recibirán los mensajes.
2. Crear una clave API con permisos de envío.
3. En STS, configurar las siguientes variables en **Run Configurations → Environment**:

| Variable | Valor |
| --- | --- |
| `CONTACT_ENABLED` | `true` |
| `RESEND_API_KEY` | Clave API de Resend. |
| `CONTACT_TO` | Tu correo receptor, por ejemplo tu Gmail. |
| `CONTACT_FROM` | Remitente autorizado; para pruebas propias, `onboarding@resend.dev`. |
| `CONTACT_ALLOWED_ORIGINS` | En local: `http://localhost:5173,http://127.0.0.1:5173`. |

El dominio `resend.dev` es **solo de pruebas** y únicamente permite enviar al correo asociado a la cuenta de Resend. No habilita envíos arbitrarios ni un remitente Gmail. Para un remitente propio o envíos a otros destinatarios se necesita verificar un dominio controlado por ti y actualizar `CONTACT_FROM`.

Referencias: https://resend.com/docs/api-reference/emails/send-email y https://resend.com/docs/knowledge-base/403-error-resend-dev-domain

No compartir claves en el chat ni guardarlas en código, Git o variables `VITE_*`. Spring no carga archivos `.env` automáticamente: configurar estas variables en STS o en el alojamiento.

Arrancar `PortfolioApiApplication` desde STS, sin activar perfiles. Por terminal:

```powershell
.\mvnw.cmd spring-boot:run
```

Después iniciar el frontend desde `../my-portfolio` con `npm run dev`. Vite conecta `/api` al backend en el puerto 8080. No configurar una URL pública en el frontend durante esta prueba.

Sin `CONTACT_ENABLED=true`, la API arranca pero rechaza el envío con HTTP 503. Si se habilita el correo, se exigen destinatario, remitente y clave configurados.

## Verificación automatizada

### Si el formulario muestra «No se pudo enviar el mensaje»

Reiniciar la aplicación tras cambiar variables en STS y comprobar:

- `CONTACT_ENABLED=true` en la configuración de ejecución que realmente se está iniciando.
- `RESEND_API_KEY` válida y con permiso de envío.
- `CONTACT_FROM=onboarding@resend.dev` para la prueba sin dominio propio; no utilizar aquí una dirección Gmail.
- `CONTACT_TO` igual al correo asociado a la cuenta de Resend para ese remitente de prueba.

La consola del backend distingue envío deshabilitado, fallo de conexión y código HTTP de Resend. El aviso `Resolved [ContactUnavailableException...]` solo señala que se devolvió el error al frontend: buscar la línea anterior que empieza con `Resend rechazó`, `Envío deshabilitado` o `No se pudo confirmar`. No compartir claves ni el contenido completo de las variables de entorno.

```powershell
.\mvnw.cmd verify
```

Las pruebas simulan la API del proveedor: no requieren claves reales ni envían correos. Cubren el contrato JSON de Resend, autorización, destinatario fijo, Reply-To, fallos del proveedor, validaciones, campo trampa, CORS y límites.

## Subir a GitHub

Crear un repositorio independiente para el contenido de **esta carpeta**, con `pom.xml`, `Dockerfile` y `render.yaml` en su raíz. No subir `target`, archivos de STS ni credenciales; están excluidos. El frontend mantiene su repositorio existente.

## Desplegar en Render

El `Dockerfile` compila y prueba con JDK 17 y ejecuta el JAR con JRE 17 como usuario sin privilegios. El puerto se obtiene de `PORT`. La memoria máxima del heap se limita al 65 % de la memoria del contenedor.

- Opción manual: nuevo **Web Service**, conectar el repositorio del backend, runtime **Docker**, plan **Free**.
- Opción Blueprint: utilizar el `render.yaml` incluido desde la raíz del repositorio del backend.
- Health check: `/api/health`; devuelve `{"status":"UP"}` sin consultar al proveedor de correo.
- Configurar `CONTACT_ENABLED=true`, `RESEND_API_KEY`, `CONTACT_TO` y `CONTACT_FROM`.
- Configurar `CONTACT_ALLOWED_ORIGINS=https://thoonidev.vercel.app`.

No se ha publicado ni creado ningún servicio automáticamente. El servicio no podrá enviar hasta configurar una clave válida y un remitente permitido por Resend.

Después, en Vercel, configurar `VITE_API_BASE_URL` con la URL HTTPS de Render, sin `/api/contact`, y volver a desplegar el frontend. El frontend espera hasta 120 segundos para tolerar el despertar del backend. No reintenta envíos automáticamente para evitar duplicados.

## Límites y primera comprobación pública

Se permiten 5 intentos por dirección de conexión y 50 globales cada 10 minutos; se cuentan intentos inválidos. El estado está acotado en memoria, se reinicia con el proceso y está pensado para una sola instancia.

No se confía en cabeceras de IP arbitrarias. Al desplegar, comprobar qué dirección recibe la aplicación detrás del proxy: varias personas podrían compartir el límite por conexión. No habilitar confianza indiscriminada en `X-Forwarded-For`. Para mayor tráfico, configurar proxies confiables o protección en la plataforma y límites compartidos si hay varias instancias.

Render gratuito puede dormir tras inactividad. La primera petición puede tardar. Verificar desde la web publicada el arranque en frío, errores, CORS y llegada a Gmail. Un HTTP 200 confirma que el proveedor aceptó el mensaje, no garantiza entrega a bandeja.

## Contrato

`POST /api/contact`, `Content-Type: application/json`:

```json
{"name":"Ana Pérez","email":"ana@example.com","subject":"Una oportunidad","message":"Me gustaría conversar sobre tu trabajo.","website":""}
```

- `200`: proveedor confirma la aceptación con un identificador.
- `400`: validaciones, JSON o campo trampa.
- `403`: origen no permitido.
- `413`: petición superior a 16 KiB.
- `415`: tipo de contenido no válido.
- `429`: límite de intentos, con `Retry-After`.
- `503`: servicio deshabilitado o proveedor no disponible.

El visitante solo establece el contenido y la dirección de respuesta. El remitente y destinatario se configuran exclusivamente en el servidor.
