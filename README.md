# Grupo Arquinova · Backend

API de la página web de **Grupo Arquinova S.A.S.** (arquitectura, ingeniería y gestión de proyectos, Armenia, Quindío). Es el "cerebro" del sitio: guarda y entrega los proyectos, atiende el formulario de contacto, controla quién puede hacer qué en el panel de administración y habla con los servicios externos (imágenes, correo, IA, Google Wallet).

> El sitio tiene dos repositorios: este (**backend**) y el [**frontend**](https://github.com/GrupoArquinova/Proyecto-Fronted) (la página y el panel que ve la gente).

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje y framework | Java 21 · Spring Boot 3.5 (Web, Data JPA, Security, Validation, AOP, Cache, Actuator) |
| Base de datos | MySQL 8 · migraciones versionadas con **Flyway** (V1 a V14) |
| Sesiones | **JWT** (jjwt) sin estado, con renovación y revocación al cerrar sesión |
| Límites y resiliencia | Bucket4j (límite de peticiones) · Resilience4j |
| Caché y colas (opcionales) | Redis · RabbitMQ — si no están, el backend usa memoria interna |
| Correo | Spring Mail (SMTP) |
| Documentos | Apache POI (Excel, Word) · Apache PDFBox (PDF) |
| Servicios externos | Cloudinary (archivos) · Cloudflare Turnstile (captcha) · Google Gemini (asistente de IA) · Google Wallet |
| Observabilidad | Actuator · Micrometer + Prometheus · Grafana · logs JSON (Logstash) |
| Documentación de la API | springdoc OpenAPI / Swagger UI (cerrada por defecto) |
| Pruebas | JUnit 5 · Mockito · Spring Test / MockMvc |
| Contenedores | Dockerfile · docker-compose · manifiestos de Kubernetes (`k8s/`) |

## Cómo funciona el sitio

```
Personas ──► Frontend (Angular · Vercel) ──► Backend (Spring Boot · Render) ──► MySQL (Aiven)
                                                    │
                       Cloudinary · Turnstile · Gemini · SMTP · Google Wallet
```

El sitio tiene **dos caras**:

- **La parte pública** (sin iniciar sesión): inicio, listado de proyectos y un **micrositio por proyecto** con sus secciones (bienvenida, ubicación, zonas comunes, casa modelo, lotes, disponibilidad, respaldo, videos y contacto). El menú solo muestra las secciones que tienen contenido. Incluye recorridos **360°**, un plano con los lotes y su estado comercial, formulario de contacto, botón de WhatsApp, un **asistente de IA**, la tarjeta de **Google Wallet** e idioma español/inglés.
- **El panel de administración** (con usuario y contraseña): crear y publicar proyectos (con un asistente paso a paso), gestionar etapas, lotes, zonas comunes, casas modelo, multimedia y ubicaciones, atender solicitudes, ver métricas, administrar usuarios, consultar el historial de cambios y sacar reportes.

### Roles del panel

| Rol | Puede |
|---|---|
| **ADMINISTRADOR** | Todo: contenido, solicitudes, métricas, usuarios, reportes e historial |
| **EDITOR** | Editar el contenido del sitio (proyectos, ubicaciones, lotes, multimedia, textos) |
| **COMERCIAL** | Atender las solicitudes de contacto y ver las métricas (solo lectura) |

El rol se vuelve a comprobar en **cada petición**: lo público es de solo lectura y escribir es del administrador, salvo lo que se abre expresamente al editor o al comercial. Un endpoint nuevo sin regla queda cerrado por defecto.

### Qué pasa cuando alguien envía el formulario de contacto

1. La persona acepta el tratamiento de datos (Ley 1581) y pasa el captcha de Turnstile.
2. El backend aplica límites por IP, topes de tamaño y **limpia el texto** (sin HTML, scripts ni caracteres invisibles).
3. Guarda la solicitud con estado `NUEVO` y la prueba del consentimiento (versión de la política, fecha, IP y navegador).
4. Cuenta el evento en la analítica y envía, sin hacer esperar a la persona, un **aviso al equipo** y una **confirmación al visitante** (máximo una por hora por dirección).
5. El equipo comercial la atiende en el panel: `NUEVO` → `EN_GESTION` → `CERRADO`.

Si el correo falla, la solicitud queda guardada igual.

### Asistente de IA y tarjeta de Wallet

- **Asistente:** chat público con Google Gemini y datos reales de los proyectos publicados. Pregunta de máximo 500 caracteres, 6 turnos de historial, 10 preguntas por minuto por IP, 5 consultas simultáneas y cuota diaria global. Solo se guarda la **pregunta** clasificada por tema.
- **Tarjeta de Google Wallet (Android):** el visitante recibe por correo un enlace firmado para guardar la tarjeta "Grupo Arquinova" con un QR al sitio. La llave privada vive fuera del repositorio (ver `docs/OPERACION.md`).

## Modelo de datos

Todo cuelga de un **proyecto**: ubicación, etapas (con sus lotes y su estado `DISPONIBLE` / `RESERVADO` / `VENDIDO`), zonas comunes (con imágenes), casas modelo, multimedia (imágenes, videos, PDF, planos, panorámicas 360°…), puntos 360° y solicitudes de contacto. Además: empresa, contenido institucional, usuarios y roles, historial (auditoría), analítica sin cookies y preguntas del chat.

El público solo recibe lo que está **publicado y activo**.

## Seguridad

- Contraseñas con BCrypt; recuperación por enlace temporal enviado al correo.
- JWT de 1 hora que se renueva mientras se trabaja (tope absoluto de 12 h); el token se revoca al cerrar sesión.
- Bloqueo de 15 minutos tras 5 intentos fallidos de acceso.
- Límite de peticiones por IP y minuto (acceso, públicas, chat, analítica, brochure, Wallet) y topes globales.
- Limpieza de entradas, escape de datos en correos, protección de celdas de Excel contra fórmulas, tamaño máximo de petición.
- Cabeceras de seguridad y CSP; CORS cerrado a los orígenes configurados.
- Historial de acciones del panel y **retención de datos personales** (depuración diaria: solicitudes 24 meses, analítica 24, chat 12, historial 24).
- Los secretos nunca van en el código: variables de entorno o archivos secretos. Un escáner (gitleaks) revisa el repositorio.

Detalles en [`docs/OPERACION.md`](docs/OPERACION.md) y [`docs/SEGURIDAD_HISTORIAL.md`](docs/SEGURIDAD_HISTORIAL.md).

## Estructura del proyecto

```
src/main/java/com/constructora_backend/
  controller/   Endpoints REST
  service/      Lógica de negocio (solicitudes, IA, Wallet, reportes, sitemap...)
  entity/       Entidades JPA
  repository/   Acceso a datos
  security/     JWT, filtros, bloqueo de intentos, revocación de tokens
  config/       Seguridad, CORS, límites, Jackson, colas
  dto/ enums/ util/ exception/ documento/
src/main/resources/db/migration/   Migraciones Flyway (V1 a V14)
docs/          Operación, seguridad y datos iniciales de una base local
k8s/ grafana/ prometheus/ tests/load/ scripts/
```

## Cómo correrlo en tu computador

Necesitas **Java 21**, **MySQL 8** y **Git**. (Maven ya viene incluido con `mvnw`.)

1. **Clona** el repositorio y entra a la rama de trabajo.
2. **Crea una base de datos vacía:**
   ```sql
   CREATE DATABASE constructora_web CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. **Crea tu archivo `.env`** desde la plantilla y completa al menos `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`; agrega `FLYWAY_ENABLED=true` para que se creen las tablas:
   ```bash
   cp .env.example .env
   ```
4. **Arranca el backend** (la primera vez descarga dependencias y tarda unos minutos):
   ```bash
   ./mvnw spring-boot:run        # en Windows PowerShell: .\mvnw.cmd spring-boot:run
   ```
   Queda en `http://localhost:8080`.
5. **Carga los datos iniciales** ejecutando [`docs/datos-iniciales-local.sql`](docs/datos-iniciales-local.sql) en la base: crea la empresa, los estados de lote y un administrador de **prueba** para entrar al panel (solo para tu computador).
6. Levanta el [frontend](https://github.com/GrupoArquinova/Proyecto-Fronted) y entra a `http://localhost:4200`.

Redis y RabbitMQ son opcionales: sin ellos verás avisos en el log, pero todo funciona. Sin las claves de correo, Cloudinary y Gemini la página funciona, pero no envía correos, no sube imágenes nuevas y el asistente no responde.

### Variables de entorno principales

Todas están documentadas en [`.env.example`](.env.example). Las más importantes:

| Variable | Para qué |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a MySQL |
| `JWT_SECRET`, `JWT_EXPIRATION` | Firma y duración de la sesión |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend permitidos |
| `MAIL_*`, `APP_CONTACTO_CORREO_AVISO` | Correo saliente y destinatario de los avisos |
| `CLOUDINARY_*` | Subida de imágenes y archivos |
| `GEMINI_API_KEY` | Asistente de IA |
| `TURNSTILE_SECRET` | Captcha del formulario |
| `APP_SITIO_URL` | Dirección pública (sitemap, enlaces de correos, QR) |
| `GOOGLE_WALLET_CREDENCIALES_ARCHIVO` | Llave de Google Wallet (archivo fuera del repositorio) |
| `APP_DOCS_PUBLICAS` | `true` solo en tu equipo para abrir Swagger UI |

> **Nunca subas tu `.env` ni llaves a GitHub.** `.env` y `credenciales/` están en `.gitignore`.

## Pruebas

```bash
./mvnw test
```

## Despliegue

La demostración corre así: frontend en **Vercel**, backend en **Render** (con `Dockerfile`) y base de datos en **Aiven (MySQL)**. Para un entorno propio hay `docker-compose.yml` (MySQL, Redis, RabbitMQ, Prometheus y Grafana) y manifiestos de Kubernetes en `k8s/` (copia `secret.example.yaml` y completa tus valores). Las migraciones se aplican solas al arrancar. Más detalle en [`docs/OPERACION.md`](docs/OPERACION.md).

## Documentación de la API

Con `APP_DOCS_PUBLICAS=true` (solo en local) abre `http://localhost:8080/swagger-ui.html`. En producción solo la ve un administrador.
