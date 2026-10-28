# Personal Portfolio API

API de contacto del portafolio de thoonidev. Recibe mensajes del frontend, valida su contenido y los envía por correo mediante Resend, sin utilizar una base de datos.

## Funcionalidades

- Envío de mensajes con nombre, correo, asunto y contenido.
- Dirección del visitante como `reply_to` para responder al mensaje.
- Validación de campos, campo señuelo antispam y límite de cuerpo de 16 KiB.
- Límites de solicitudes en memoria y configuración de orígenes permitidos mediante CORS.
- Endpoint de salud y configuración de despliegue con Docker y Render.

## Tecnologías

Java 17 · Spring Boot 4.1.1 · Spring MVC · Bean Validation · RestClient · Maven · Resend · Docker

## Instalación y configuración

Requiere **JDK 17**, una cuenta de Resend y un remitente autorizado por el servicio. El repositorio incluye Maven Wrapper.

```bash
git clone https://github.com/thoonidev/Personal-portfolio-api.git
cd Personal-portfolio-api
```

Configura las variables de entorno antes de iniciar. Ejemplo en PowerShell:

```powershell
$env:CONTACT_ENABLED="true"
$env:RESEND_API_KEY="tu_clave_de_resend"
$env:CONTACT_TO="destinatario@tu-dominio.com"
$env:CONTACT_FROM="Portfolio <contacto@tu-dominio.com>"
$env:CONTACT_ALLOWED_ORIGINS="http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

En Linux o macOS, define las mismas variables con `export` y ejecuta `./mvnw spring-boot:run`. No publiques la clave de Resend. El envío está deshabilitado por defecto y el puerto es `8080`, modificable mediante `PORT`.

## Endpoints

| Método y ruta | Uso |
| --- | --- |
| `GET /api/health` | Devuelve `{"status":"UP"}`. |
| `POST /api/contact` | Valida y envía un mensaje a Resend. |

Envía el contacto con `Content-Type: application/json`:

```json
{
  "name": "Visitante",
  "email": "visitante@example.com",
  "subject": "Consulta sobre un proyecto",
  "message": "Hola, quisiera conversar sobre una propuesta.",
  "website": ""
}
```

`website` es un campo señuelo y debe quedar vacío u omitirse. Una respuesta exitosa confirma que el proveedor aceptó el mensaje, no su entrega en la bandeja del destinatario.

Los límites predeterminados son **5 solicitudes por dirección remota y 50 globales cada 10 minutos**. Se configuran en `application.yml`, se reinician con la aplicación y son locales a cada instancia. Detrás de un proxy, varios visitantes pueden compartir la dirección remota observada por el servidor.

## Pruebas y compilación

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Las pruebas cubren la API, el servicio de correo y el limitador. En Linux o macOS, sustituye `.\mvnw.cmd` por `./mvnw`. El JAR se genera en `target/`.

## Despliegue en Render

El repositorio incluye `Dockerfile` y `render.yaml` para desplegar desde su raíz. La imagen ejecuta `verify` durante la compilación y utiliza `/api/health` como comprobación de salud.

Configura `RESEND_API_KEY`, `CONTACT_TO` y `CONTACT_FROM` en Render. Ajusta `CONTACT_ALLOWED_ORIGINS` al dominio real del frontend; el archivo incluido usa `https://thoonidev.vercel.app`. El frontend debe enviar sus solicitudes a la URL pública de esta API seguida de `/api/contact`.
