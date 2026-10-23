# Guía de operación — Grupo Arquinova

Qué es cada pieza, cómo se publica, cómo se respalda, cómo se vigila y qué hacer cuando algo falla.
Está pensada para quien administra el sitio, no solo para quien programa. Aquí **nunca** van contraseñas ni claves: solo sus nombres y dónde viven.

## 1. Mapa de la plataforma

| Pieza | Dónde vive | Cómo se publica | Notas |
| --- | --- | --- | --- |
| Sitio web (Angular) | Vercel, proyecto `arquinova` → https://arquinova.vercel.app | `npx vercel --prod` desde la carpeta del frontend | Estático. No está conectado a Git: no se actualiza solo. |
| API (Spring Boot) | Render, servicio `arquinova-backend` → https://arquinova-backend.onrender.com | Render → *Manual Deploy → Deploy latest commit* | Lee la rama `feature/CorrecionesOActualizacionesDeErrores` de GitHub. Se duerme tras 15 min sin tráfico (plan gratuito). |
| Base de datos (MySQL 8.4) | Aiven, servicio `mysql-a2a837`, base `defaultdb` | Migraciones a mano (Flyway está apagado en Render) | El plan gratuito **se apaga por inactividad** (hay que encenderlo desde la consola). |
| Caché y sesiones revocadas | Render Key Value (Redis) | — | La lista pública de proyectos se guarda 2 horas. |
| Imágenes, videos y 360° | Cloudinary | Se suben desde el panel (firma del backend) | No están en la base: la base solo guarda sus URL. |
| Código | GitHub `GrupoArquinova/Proyecto-Arquinova` (público) | — | Público: **nada secreto en el repositorio**. |
| Respaldos y monitoreo | GitHub Actions (`.github/workflows`) | Se ejecutan solos | Ver secciones 4 y 5. |

## 2. Variables de entorno (solo nombres)

**Render (backend)** — *Environment*:
`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_SSL_MODE`, `JWT_SECRET`, `JWT_EXPIRATION` (3600000 = 1 hora),
`CORS_ALLOWED_ORIGINS`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`,
`CLOUDINARY_API_SECRET`, `CLOUDINARY_FOLDER`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`,
`FLYWAY_ENABLED` (false en Render), `TURNSTILE_SECRET` (clave secreta del captcha; vacía = captcha desactivado, ver 7.4), `APP_SITIO_URL` (enlace del correo de recuperación de contraseña y de la política de datos; si falta, usa el primer origen de `CORS_ALLOWED_ORIGINS`; cámbiala cuando haya dominio).
**No** definir `APP_DOCS_PUBLICAS` (Swagger debe quedar cerrado). `METRICAS_TOKEN` solo si hay un Prometheus que lo use.
Cuidado al pegar valores: sin espacios sobrantes y **sin los signos `< >` de las plantillas** (una vez `CLOUDINARY_CLOUD_NAME` quedó como `<dn3s7utod>` y las subidas fallaron con 401).

**Vercel (frontend)**: ninguna. La dirección de la API y los datos públicos de contacto están en `src/environments/environment.ts`.

**GitHub (Settings → Secrets and variables → Actions)**, para el respaldo:
`AIVEN_DB_HOST`, `AIVEN_DB_PORT`, `AIVEN_DB_USER`, `AIVEN_DB_PASSWORD`, `AIVEN_DB_NAME`, más el cifrado (`BACKUP_AGE_RECIPIENT`, recomendado; o `BACKUP_PASSPHRASE` de 32+ caracteres) y el destino (`BACKUP_S3_BUCKET`, `BACKUP_S3_ENDPOINT`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`). Detalle en la sección 4 ("Respaldos: cifrado y destino").
Opcionales (pestaña *Variables*): `URL_BACKEND`, `URL_FRONTEND` si cambian los dominios.

## 3. Publicar una versión

1. Hacer commit y `git push` (backend y frontend son repositorios distintos).
2. **Primero el backend**: Render → *Manual Deploy → Deploy latest commit*. Esperar a `Started ConstructoraBackendApplication` en los logs.
   Si hay migraciones nuevas (`src/main/resources/db/migration`), aplicarlas **antes** en Aiven (Workbench, pestaña *Aiven*), porque Flyway no corre en Render.
3. Comprobar: `https://arquinova-backend.onrender.com/actuator/health/readiness` debe decir `"status":"UP"`.
4. **Después el frontend**: `npx vercel --prod`. Si dice *Not authorized*: `npx vercel logout` y `npx vercel login`.
5. Abrir el sitio en una ventana de incógnito y revisar el inicio, un proyecto y el panel.
6. Si cambiaron datos directamente en la base (UPDATE a mano), guardar un proyecto desde el panel para limpiar la caché de 2 horas.

> Cuando cambia la autenticación o el formato del token, el orden backend → frontend es obligatorio, y todas las personas deben iniciar sesión de nuevo.

## 4. Respaldos

### Qué hay
- **Automático** (`.github/workflows/respaldo-bd.yml`): todos los días a las 03:30 (hora de Colombia). Hace una copia completa de la base, **comprueba que se puede restaurar** (la carga en un MySQL temporal y cuenta tablas, usuarios y proyectos), la **cifra** (`age`, o AES-256 heredado) y la guarda **fuera de la exposición pública**: en un bucket S3/R2 si está configurado, o como artefacto de 30 días solo si el repositorio es privado (en un repo público el workflow falla en vez de publicar el archivo).
  La base tiene datos personales y hashes de contraseñas: por eso el archivo nunca sale sin cifrar. Ver "Respaldos: cifrado y destino" más abajo.
- **Manual**: `scripts\respaldar-aiven.ps1` (Windows). Pide la contraseña en pantalla y deja el archivo en `respaldos\` (ignorada por git).
- **Aiven**: la consola tiene una pestaña *Backups*. Revisa allí qué conserva tu plan; no la tomes como único respaldo.
- **No están en la base**: las imágenes (Cloudinary guarda las suyas; no las borres desde su consola) y el código (GitHub).

### Puesta en marcha (una sola vez)
1. Crear la clave de cifrado (recomendado `age`, ver "Respaldos: cifrado y destino"; la clave privada se guarda fuera de línea, nunca en GitHub).
2. En GitHub → *Settings → Secrets and variables → Actions → New repository secret*: crear los secrets de la sección 2 (base de datos, cifrado y destino).
   Para `AIVEN_DB_*` usa los datos de *Connection information* de Aiven.
   Idealmente crea en Aiven un usuario aparte solo para esto, en vez de usar `avnadmin`: le bastan los permisos `SELECT`, `SHOW VIEW`, `TRIGGER` y `EVENT` sobre la base.
3. Los workflows con horario **solo corren desde la rama por defecto del repositorio** (`main`/`master`). Hasta que ese archivo llegue allí, no se ejecutan solos.
   Para probar el respaldo: pestaña *Actions → Respaldo diario → Run workflow* (también necesita el archivo en la rama por defecto).
4. Revisar que el primer respaldo salga en verde y que en el resumen aparezcan las tablas, usuarios y proyectos restaurados.

### Descargar y abrir un respaldo
1. Si usas bucket: descarga el último archivo de `respaldos/` desde la consola de R2/S3. Si usas artefacto (repo privado): GitHub → *Actions* → el último "Respaldo diario" en verde → *Artifacts* → `respaldo-bd-cifrado`.
2. Descifrar. Con `age` (archivo `.sql.gz.age`): `age --decrypt -i clave-privada.txt -o respaldo.sql.gz archivo.sql.gz.age`. Con el método heredado (archivo `.sql.gz.enc`; en Git Bash o WSL, que traen `openssl`):
   ```bash
   openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 -in respaldo-aiven-AAAAMMDD-HHMM.sql.gz.enc -out respaldo.sql.gz -pass pass:"TU_CLAVE"
   gunzip respaldo.sql.gz        # queda respaldo.sql
   ```

### Restaurar
**Siempre primero en una base nueva** para comprobar el contenido (en Aiven: pestaña *Databases → Create database*, por ejemplo `prueba_restauracion`):
```bash
mysql --host=HOST --port=PUERTO --user=USUARIO --ssl-mode=REQUIRED -p prueba_restauracion < respaldo.sql
```
(En Workbench: *Server → Data Import → Import from Self-Contained File*, eligiendo la base de destino.)

Para **recuperar producción**: el respaldo trae `DROP TABLE IF EXISTS` antes de cada tabla, así que restaurarlo sobre `defaultdb` **reemplaza todo su contenido** por el del respaldo (se pierde lo posterior a esa copia).
1. Haz antes un respaldo manual del estado actual (`scripts\respaldar-aiven.ps1`), aunque esté dañado.
2. Restaura sobre `defaultdb` con el mismo comando.
3. Guarda un proyecto desde el panel (limpia la caché) y revisa el sitio.

### Objetivos
- **Pérdida máxima aceptable (RPO): 24 horas** (frecuencia del respaldo).
- **Tiempo de recuperación (RTO): 2 horas** (descargar, descifrar, restaurar y verificar).
- **Probar una restauración real cada trimestre** y anotar la fecha. Un respaldo que nunca se ha restaurado no es un respaldo.

### Respaldos: cifrado y destino

**Por qué cambió.** Un artefacto de GitHub Actions de un repositorio público lo puede descargar cualquier usuario de GitHub con sesión iniciada. Aunque esté cifrado, no debe quedar a su alcance: un passphrase débil o filtrado lo abriría, y con AES-CBC no hay autenticación del archivo. Ahora el cifrado es con `age` (clave pública en GitHub, privada solo tuya) y el destino es un bucket privado.

**Qué hace el workflow** (`.github/workflows/respaldo-bd.yml`), antes de tocar la base:
| Situación | Resultado |
| --- | --- |
| Existe `BACKUP_AGE_RECIPIENT` | Cifra con `age` (archivo `.sql.gz.age`). |
| No existe y `BACKUP_PASSPHRASE` tiene 32+ caracteres | Cifra con openssl AES-256-CBC (`.sql.gz.enc`) y deja un **aviso**: migra a `age`. |
| No existe y la passphrase falta o tiene menos de 32 caracteres | **Falla.** |
| Existen los cuatro secrets del bucket | Sube el cifrado al bucket y **no** publica artefacto. |
| Faltan algunos (pero no todos) los secrets del bucket | **Falla** (configuración incompleta). |
| Sin bucket y repositorio privado | Publica el artefacto 30 días. |
| Sin bucket y repositorio público | **Falla** con un mensaje que explica qué configurar. |

**1. Crear un bucket gratuito en Cloudflare R2**
1. Cuenta en Cloudflare → *R2 Object Storage* → *Create bucket* (por ejemplo `arquinova-respaldos`; ubicación automática). El plan gratuito incluye 10 GB; R2 pide registrar una tarjeta aunque no se cobre dentro del gratuito.
2. Deja el bucket **privado** (no actives *Public access* ni dominio personalizado).
3. En el bucket → *Settings → Object lifecycle rules → Add rule*: borrar objetos con prefijo `respaldos/` a los **30 días**. Esa regla es la retención (el workflow no borra nada por sí mismo).
4. *R2 → Manage R2 API Tokens → Create API token*: permiso **Object Read & Write**, limitado a ese bucket. Copia *Access Key ID*, *Secret Access Key* y la dirección S3 (`https://<ID_DE_CUENTA>.r2.cloudflarestorage.com`). El secreto solo se muestra una vez.
5. En GitHub → *Settings → Secrets and variables → Actions* crea: `BACKUP_S3_BUCKET` (nombre del bucket), `BACKUP_S3_ENDPOINT` (la dirección anterior, con `https://`), `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY`. Cualquier servicio compatible con S3 sirve igual (Backblaze B2, Wasabi, AWS S3).

**2. Generar la clave `age`** (en tu computador, nunca en GitHub)
```bash
age-keygen -o clave-respaldos.txt        # imprime "Public key: age1..."
```
- Instalar `age`: Windows `winget install FiloSottile.age`; Linux `apt install age`; macOS `brew install age`.
- El archivo `clave-respaldos.txt` contiene la clave **privada** (`AGE-SECRET-KEY-...`): guárdalo **fuera de línea** (memoria USB en un lugar seguro, más una copia en el gestor de contraseñas). Sin él los respaldos no se pueden abrir y nadie, ni GitHub ni Cloudflare, puede recuperarlo. No lo subas a git ni lo pegues en un chat.
- En GitHub crea el secret `BACKUP_AGE_RECIPIENT` con **solo la clave pública** (la línea `age1...`).
- Cuando `BACKUP_AGE_RECIPIENT` funcione, borra `BACKUP_PASSPHRASE`.

**3. Probar el workflow y la restauración**
1. *Actions → Respaldo diario → Run workflow*. Revisa que pase en verde; el log dice si usó `age` y que subió al bucket (sin imprimir nombres de bucket ni claves).
2. Descarga el archivo de la consola de R2 (bucket → `respaldos/`) a una carpeta temporal.
3. Descifra e importa en una base de prueba (nunca directamente en producción):
   ```bash
   age --decrypt -i clave-respaldos.txt -o respaldo.sql.gz respaldo-aiven-AAAAMMDD-HHMM.sql.gz.age
   gunzip respaldo.sql.gz
   mysql --host=HOST --port=PUERTO --user=USUARIO --ssl-mode=REQUIRED -p prueba_restauracion < respaldo.sql
   ```
4. Comprueba tablas, usuarios y proyectos (por ejemplo `SELECT COUNT(*) FROM usuarios;`), anota la fecha de la prueba y borra `respaldo.sql` (contiene datos personales) y la base de prueba.

**4. Rotación**
- **Clave `age`** (cada año o si sospechas que se filtró la privada): genera un par nuevo con `age-keygen`, actualiza `BACKUP_AGE_RECIPIENT` con la nueva pública y **conserva la privada vieja** mientras existan respaldos cifrados con ella (30 días).
- **Claves del bucket** (cada 6 meses o si salen del equipo): crea un token nuevo en R2, actualiza `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY` en GitHub, lanza el workflow a mano y, si sale en verde, revoca el token viejo.
- **Passphrase heredada** (solo si aún no migras): `openssl rand -base64 48`; guarda también la vieja para abrir los respaldos anteriores.

**Si alguna vez un respaldo estuvo como artefacto en un repositorio público**, trátalo como expuesto: rota las contraseñas de los usuarios de la aplicación y los secretos de la base, y borra esos artefactos (*Actions → run → Delete artifact*).

## 5. Monitoreo y alertas

`.github/workflows/monitoreo.yml` revisa **cada 15 minutos**: que la API esté viva y lista (base de datos), que entregue proyectos, que el sitio cargue, que Swagger y las métricas sigan cerrados, y que el sitio y la API mantengan sus cabeceras de seguridad (CSP, HSTS).
Si algo falla, el workflow queda en rojo y GitHub envía un correo. Reintenta durante unos 2,5 minutos antes de alertar, porque la API gratuita tarda hasta ~50 s en despertar.
De paso mantiene despierta la API (se duerme tras 15 min sin tráfico). Ten presente que GitHub desactiva los workflows con horario si el repositorio pasa 60 días sin actividad.

Complemento recomendado y gratuito: un servicio externo como UptimeRobot o Better Stack apuntando a `https://arquinova.vercel.app` y `https://arquinova-backend.onrender.com/actuator/health/liveness`, con aviso por correo o WhatsApp.

Dónde mirar cuando suene una alarma:
- Render → *Logs* (errores de arranque, de base de datos o de memoria).
- Aiven → *Overview* (¿está *Running* o *Powered off*?) y *Metrics*.
- Vercel → *Deployments*.

Para producción real con más tráfico, el repositorio ya trae `prometheus/`, `grafana/` y `k8s/` (Prometheus + Grafana + alertas). El endpoint de métricas exige el token `METRICAS_TOKEN`.

## 6. Cuando algo falla

| Síntoma | Causa probable | Qué hacer |
| --- | --- | --- |
| El sitio abre pero sin proyectos, o el panel da error de conexión | Aiven apagado o Render dormido/caído | Aiven → *Overview*: si dice *Powered off*, encenderlo (⋯ → *Power on*) y esperar a *Running*. Luego Render → *Manual Deploy*. |
| Render no arranca y el log dice `UnknownHostException ... aivencloud.com` | Aiven apagado o borrado | Igual que arriba. Si el servicio ya no existe, crear uno nuevo, restaurar el último respaldo (sección 4) y actualizar las variables `DB_*` en Render. |
| El log dice `Schema-validation: missing column [...]` | Falta aplicar una migración en Aiven | Aplicar los scripts de `db/migration` pendientes y redesplegar. |
| Error 401 al subir imágenes | `CLOUDINARY_*` mal pegadas (corchetes, espacios) | Corregir las variables en Render. |
| Todos son sacados del panel tras un despliegue | Cambió el formato del token | Normal: iniciar sesión otra vez. |
| El navegador bloquea contenido (consola: *Content Security Policy*) | Se agregó un servicio externo nuevo | Añadir su dominio a la CSP de `vercel.json` y correr `npm run e2e:csp`. |
| Los correos no salen | Render gratuito bloquea el SMTP | Usar un proveedor por API (Brevo, SendGrid, Resend) o un plan de pago. |
| Un cambio hecho con SQL no se ve en el sitio | Caché de 2 horas | Guardar un proyecto desde el panel. |
| Cambia el dominio | CORS, CSP y SEO apuntan al anterior | Actualizar: `CORS_ALLOWED_ORIGINS` (Render), `connect-src` de `vercel.json`, `APP_SITIO_URL`, `sitioUrl` en `environment.ts` y las variables `URL_*` del monitoreo. |

## 7. Rotar secretos

Cada vez que alguien con acceso deja el proyecto, o si una clave se pegó donde no debía:
1. `JWT_SECRET`: cambiarlo en Render → todas las sesiones se cierran (es lo esperado).
2. Contraseña de la base: cambiarla en Aiven (*Users*) → actualizar `DB_PASSWORD` en Render y `AIVEN_DB_PASSWORD` en GitHub.
3. Cloudinary: regenerar la clave en su consola → `CLOUDINARY_API_KEY` y `CLOUDINARY_API_SECRET` en Render.
4. `BACKUP_AGE_RECIPIENT` / `BACKUP_PASSPHRASE` (ver "Respaldos: cifrado y destino"): al cambiarlas, los respaldos anteriores siguen necesitando la clave vieja; conserva ambas anotadas.

### 7.1 Rotación de secretos tras exposición en git

El repositorio es público y el historial de git no se puede "limpiar" de verdad (copias, forks, cachés). Si en algún momento se subió un valor secreto, **trátalo como comprometido y cámbialo**, aunque ya se haya borrado del archivo. Orden recomendado (todo sin escribir valores en el repositorio, en chats ni en tickets):

| Secreto | Cómo generar el nuevo | Dónde se cambia | Efecto |
| --- | --- | --- | --- |
| `JWT_SECRET` | `openssl rand -base64 48` (más de 32 caracteres aleatorios) | Render → *Environment* | Cierra todas las sesiones. |
| Contraseña de la base (`DB_PASSWORD`) | La genera Aiven al restablecer la contraseña del usuario (o `openssl rand -base64 32`) | Aiven → *Users*, luego `DB_PASSWORD` en Render y `AIVEN_DB_PASSWORD` en GitHub | El backend se reinicia con la nueva. |
| Contraseña de aplicación de Gmail (`MAIL_PASSWORD`) | Cuenta de Google → Seguridad → *Contraseñas de aplicaciones*: revocar la anterior y crear otra | Render → `MAIL_PASSWORD` | Los correos usan la nueva. |
| `GEMINI_API_KEY` | Google AI Studio: borrar la clave y crear otra (con cuota o presupuesto fijado) | Render → `GEMINI_API_KEY` | El chat vuelve a funcionar con la nueva. |
| `BACKUP_PASSPHRASE` | `openssl rand -base64 48` (32 caracteres o más) | GitHub → *Secrets* | Los respaldos nuevos usan la nueva; **guarda también la vieja** para abrir los anteriores. |
| `CLOUDINARY_API_SECRET` y `CLOUDINARY_API_KEY` | Consola de Cloudinary → *API Keys* | Render | Las subidas firmadas usan la nueva. |
| `REDIS_PASSWORD`, `METRICAS_TOKEN` (si se usan) | `openssl rand -base64 32` | Render / Prometheus | — |

Después: revisar que no queden valores reales en `.env`, `k8s/secret.yaml` ni `docker-compose.yml` dentro de git (`k8s/secret.yaml` y `.env` están en `.gitignore`; solo van en git las plantillas `*.example`). El `docker-compose.yml` ya no trae contraseñas por defecto: exige `MYSQL_ROOT_PASSWORD`, `DB_PASSWORD`, `RABBITMQ_PASSWORD`, `GRAFANA_PASSWORD` y `JWT_SECRET` en el `.env`. En `application.yaml` no hay valores por defecto para `JWT_SECRET`, `DB_*` ni `MAIL_*`, y el correo que recibe los avisos (`APP_CONTACTO_CORREO_AVISO`) ya no tiene valor por defecto: **debe definirse en Render**, o los avisos de solicitudes no se envían.

### 7.2 Migración V13 (aplicar a mano en Aiven)

Flyway está apagado en Render: antes de desplegar esta versión ejecutar en Aiven (Workbench, pestaña *Aiven*) el contenido de `src/main/resources/db/migration/V13__seguridad_sesiones_y_retencion.sql`:

```sql
-- Sesiones: los tokens emitidos antes de este instante dejan de valer (cambio de contraseña/rol, desactivación)
ALTER TABLE usuarios ADD COLUMN token_valido_desde DATETIME NULL;

-- Tratamiento de datos (Ley 1581): versión de la política aceptada y momento del consentimiento
ALTER TABLE solicitudes_contacto
    ADD COLUMN version_politica VARCHAR(20) NULL,
    ADD COLUMN fecha_consentimiento DATETIME NULL;
```

Sin esto el backend no arranca (`Schema-validation: missing column`). Las filas existentes quedan con `NULL` y todo sigue funcionando.

### 7.3 Sesiones, bloqueos y límites (cómo se comporta)

- **Sesión**: el token dura 1 hora y el panel lo renueva mientras la persona trabaja, pero la sesión tiene una vida **máxima absoluta de 12 horas** (`JWT_SESION_MAXIMA_HORAS`); pasado ese tiempo hay que iniciar sesión de nuevo. Cambiar la contraseña, el rol o desactivar la cuenta cierra **todas** las sesiones de esa persona al instante (`usuarios.token_valido_desde`). Los tokens revocados al cerrar sesión se guardan en Redis/memoria solo como huella SHA-256.
- **Inicio de sesión**: 10 intentos por minuto por IP en `/api/auth/login`, `/forgot-password`, `/reset-password` y `/renovar`. Tras 5 fallos seguidos, ese correo queda bloqueado 15 minutos **desde esa IP** (así quien adivina contraseñas no deja fuera a la persona real que entra desde otra IP); además, 30 fallos desde cualquier IP bloquean el correo 15 minutos. Contraseña incorrecta y cuenta desactivada dan la misma respuesta (401).
- **Recuperación de contraseña**: 3 solicitudes por hora y por correo (exista o no); la respuesta es siempre la misma (200) y los fallos de correo solo van al log. Pedir un enlace nuevo invalida los anteriores; al restablecer, se invalidan todos los pendientes y se cierran las sesiones.
- **IP del cliente**: el límite usa `request.getRemoteAddr()`; detrás de Render/Kubernetes la resuelve Spring con `server.forward-headers-strategy: framework`. La plataforma debe **sobrescribir** `X-Forwarded-For` (Render lo hace); si algún día se publica la API directamente a internet sin proxy, cambiar la estrategia a `none`.
- **Tamaños**: cuerpo máximo de 64 KB en `/api/**` (`APP_MAX_CUERPO_BYTES`), respondiendo 413.
- **Reportes**: máximo 50.000 filas por reporte/Excel/PDF; si se excede, error claro pidiendo afinar los filtros.
- **Salud**: `/actuator/health` (sondas) es público; el detalle (base de datos, disco) solo lo ve un ADMINISTRADOR.

### 7.4 Formulario público de contacto y datos personales

- El formulario valida tamaños y caracteres de control, descarta con respuesta normal pero **sin guardar ni enviar nada** cualquier envío que traiga texto en el campo trampa `sitioWeb` (honeypot), y la IP y el user-agent los fija el servidor (no se aceptan del cuerpo).
- Cada solicitud guarda la versión de la política aceptada (`APP_LEGAL_VERSION_POLITICA`, por defecto `2026-10`) y la fecha del consentimiento. **Al publicar una política nueva, actualizar esa variable.**
- La confirmación al visitante no repite su mensaje; solo lo saluda con su nombre saneado. Hay tope global de confirmaciones (`APP_CONTACTO_CONFIRMACIONES_MAX_HORA`, por defecto 60, y `..._MAX_DIA`, por defecto 300) además de 1 por hora por dirección.
- **Retención**: cada día a las 03:00 se eliminan las solicitudes con más de `APP_RETENCION_SOLICITUDES_MESES` meses (por defecto 24; `0` desactiva); en el log solo queda la cantidad.
- **Atender a un titular de datos** (consulta, supresión): `DELETE /api/solicitudes-contacto/por-correo?correo=persona@ejemplo.com` con sesión de administrador; borra todas las solicitudes de ese correo, responde `{"eliminadas": N}` y queda en la auditoría.
- **Captcha (Cloudflare Turnstile, implementado y desactivado hasta tener las llaves)**: `CaptchaService` verifica en el servidor el token del widget contra `https://challenges.cloudflare.com/turnstile/v0/siteverify` (timeout de 5 s) con la IP real del visitante. Se evalúa en `POST /api/solicitudes-contacto/publico` **después** del honeypot y **antes** de guardar nada o enviar correos. Si falla responde 400 con el mensaje genérico "Verificación de seguridad no superada". El token y el secreto nunca se escriben en el log.
  - **Sin `TURNSTILE_SECRET` el captcha queda desactivado** (todo envío pasa), así el sitio sigue funcionando hasta que existan las llaves.
  - Si Cloudflare no responde: por defecto (`TURNSTILE_FALLO_ABIERTO=false`) se rechaza con 503 "No se pudo verificar; intenta de nuevo". Con `true` se deja pasar el formulario (prefiere disponibilidad sobre protección; los demás límites siguen activos).
  - **Crear el widget**: Cloudflare → *Turnstile → Add widget*; nombre "Arquinova contacto"; dominios (hostnames) del sitio (el de Vercel y, cuando exista, el dominio propio; añade `localhost` solo para desarrollo); modo *Managed*. Cloudflare entrega una **Site key** (pública) y una **Secret key**.
  - **Backend (Render → Environment)**: `TURNSTILE_SECRET` = la Secret key (y opcionalmente `TURNSTILE_FALLO_ABIERTO`). En local, en `.env`. Nunca en git.
  - **Frontend**: la Site key (pública) se pone en `src/environments/environment*.ts`; el formulario carga el script de Turnstile, muestra el widget y envía el token como campo `captchaToken` en el cuerpo de `POST /api/solicitudes-contacto/publico`. Añadir `https://challenges.cloudflare.com` a `script-src`, `frame-src` y `connect-src` de la CSP de `vercel.json`. Para probar con llaves de prueba de Cloudflare: site key `1x00000000000000000000AA` (siempre pasa) o `2x00000000000000000000AB` (siempre falla), con el secreto de pruebas de la documentación de Cloudflare.
  - Orden recomendado: desplegar el frontend con el widget **primero**, y solo después poner `TURNSTILE_SECRET` en Render; si se hace al revés, el formulario público devolverá 400 hasta que el frontend envíe el token.
  - Mientras tanto, y además del captcha, protegen el formulario: límite por IP (100/min) y global (60/min), honeypot, topes de confirmación y el límite de tamaño.

### 7.5 Variables de entorno nuevas (todas opcionales, con valor por defecto)

`JWT_SESION_MAXIMA_HORAS` (12), `APP_MAX_CUERPO_BYTES` (65536), `APP_LEGAL_VERSION_POLITICA` (2026-10), `APP_RETENCION_SOLICITUDES_MESES` (24), `APP_RETENCION_CRON` (`0 0 3 * * *`), `APP_CONTACTO_CONFIRMACIONES_MAX_HORA` (60), `APP_CONTACTO_CONFIRMACIONES_MAX_DIA` (300), `GEMINI_RESPUESTA_SEGUNDOS` (10), `GEMINI_CONCURRENCIA_MAXIMA` (5), `GEMINI_CUOTA_DIARIA` (1000; 0 = sin tope), `TURNSTILE_SECRET` (vacío = captcha desactivado), `TURNSTILE_FALLO_ABIERTO` (false).
`APP_CONTACTO_CORREO_AVISO` **ya no tiene valor por defecto**: definirlo en Render (correo que recibe los avisos de nuevas solicitudes).

## 8. Antes de publicar el sitio de verdad

- [ ] Dominio propio con HTTPS, y actualizar todo lo de la fila "Cambia el dominio".
- [ ] Planes de pago o equivalentes: base de datos que no se apague, API que no se duerma, SMTP que funcione.
- [ ] Datos legales completos (domicilio, responsable de los datos, licencias) y revisión del abogado, incluida la versión en inglés.
- [ ] Textos en inglés revisados por la empresa.
- [ ] Borrar los proyectos y los datos de prueba.
- [ ] Respaldo automático en verde y una restauración de prueba hecha.
- [ ] Monitoreo con alertas llegando a una persona que las atienda.
- [ ] Cookie HttpOnly para la sesión (fase 2 del plan de endurecimiento), cuando el sitio y la API compartan dominio.

## Códigos QR

Los QR (`Documentacion/QR_Arquinova`) apuntan a enlaces cortos del sitio (`/qr/inicio`, `/qr/encanto`, `/qr/brochure`) y estos se redirigen a la página de inicio, a El Encanto y al PDF del brochure (`public/documentos/` del frontend). La regla está en `vercel.json` (`redirects`, redirección temporal): cambiar el destino de un QR ya impreso es editar esa regla y desplegar el frontend. Con dominio nuevo hay que volver a generar los QR (`generador/generar_qr.py`).

## Asistente de IA (chat del sitio)

El chat usa la API de Gemini desde `AIService`. Variables de entorno (nunca en `application.yaml`):

| Variable | Para qué |
| --- | --- |
| `GEMINI_API_KEY` | Clave de Google AI Studio. Sin ella el chat responde que no está disponible (503). En local va en `.env`; en la demo, en Render. |
| `GEMINI_MODEL` | Opcional. Por defecto `gemini-3.1-flash-lite`. Google retira modelos con el tiempo: si el chat deja de responder, mira el log del backend (`Gemini respondió 404`) y cambia el modelo. La lista vigente se consulta con `GET https://generativelanguage.googleapis.com/v1beta/models` y la cabecera `x-goog-api-key`. |
| `GEMINI_MODEL_RESPALDO` | Opcional. Por defecto `gemini-3.6-flash`. Se usa cuando el principal falla (retirado o con alta demanda). |

- El bot responde solo con datos reales y públicos (proyectos publicados, villas, amenidades, ubicación y lotes por estado, guardados 10 minutos) y tiene prohibido dar precios, plazos o asesoría legal: para eso remite al WhatsApp.
- `/api/chat` es público: 10 preguntas por minuto por IP (y 120 por minuto entre todas las IP), 500 caracteres por pregunta, como máximo 5 consultas a la vez a la IA (si no hay cupo responde 429 de inmediato) y una cuota diaria global (`GEMINI_CUOTA_DIARIA`; al agotarse responde 503). Conviene fijar también una cuota o un presupuesto en la consola de Google para la clave. Los datos de los proyectos van a la IA entre etiquetas `<datos>` y se le indica que son solo información, no instrucciones.
- Si la clave se expone (por ejemplo, se sube a un repositorio), hay que revocarla en Google AI Studio y crear otra.
- Ante un fallo pasajero de Google (429 o 5xx) el backend reintenta una vez por modelo (2 reintentos en total, con espera de 0,5 s y tiempo máximo de respuesta de 10 s) y, si sigue fallando, prueba el modelo de respaldo; solo después el visitante ve el aviso de "no puedo responder". Si en el log aparecen muchos `Gemini respondió 429`, la cuota de la clave es insuficiente (los planes gratuitos tienen pocas peticiones por minuto): conviene activar la facturación en Google AI Studio.
- El bot está instruido para interpretar mensajes con faltas de ortografía, sin tildes o abreviados ("q proyectos tienen", "donde keda").

## Reportes (Excel y PDF)

Pantalla: **Admin → Reportes**. Seis reportes (lotes, proyectos, etapas, estados comerciales, solicitudes y actividad), cada uno con
sus filtros, y descarga en Excel (`/api/documentos/excel/<reporte>`) y en PDF (`/api/documentos/pdf/<reporte>`) con los mismos
filtros. La ficha técnica de un proyecto es `/api/documentos/pdf/proyecto/{id}/ficha-tecnica`.

- Por defecto los reportes cuentan solo lo **activo**, igual que los indicadores del panel; `incluirInactivos=true` agrega lo inactivo.
- El Excel trae una hoja con el reporte (filtros de columna, encabezado fijo, totales que se ajustan al filtrar) y una hoja "Resumen".
- El PDF no corta la lista: continúa en las hojas necesarias, con "Página X de N".
- Los reportes no incluyen precios.
- El código está en `documento/` (`Columna`, `ExcelReporte`, `PdfReporte`) y `ReporteDocumentoService`: cada reporte se define una sola vez y de
  ahí salen sus dos formatos. `ReporteIntegracionTest` genera todos los archivos con la base real.

