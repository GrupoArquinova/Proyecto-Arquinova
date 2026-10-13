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
`FLYWAY_ENABLED` (false en Render), `APP_SITIO_URL` (cuando haya dominio).
**No** definir `APP_DOCS_PUBLICAS` (Swagger debe quedar cerrado). `METRICAS_TOKEN` solo si hay un Prometheus que lo use.
Cuidado al pegar valores: sin espacios sobrantes y **sin los signos `< >` de las plantillas** (una vez `CLOUDINARY_CLOUD_NAME` quedó como `<dn3s7utod>` y las subidas fallaron con 401).

**Vercel (frontend)**: ninguna. La dirección de la API y los datos públicos de contacto están en `src/environments/environment.ts`.

**GitHub (Settings → Secrets and variables → Actions)**, para el respaldo:
`AIVEN_DB_HOST`, `AIVEN_DB_PORT`, `AIVEN_DB_USER`, `AIVEN_DB_PASSWORD`, `AIVEN_DB_NAME`, `BACKUP_PASSPHRASE`.
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
- **Automático** (`.github/workflows/respaldo-bd.yml`): todos los días a las 03:30 (hora de Colombia). Hace una copia completa de la base, **comprueba que se puede restaurar** (la carga en un MySQL temporal y cuenta tablas, usuarios y proyectos), la **cifra con AES-256** y la guarda **30 días** como artefacto del workflow.
  El repositorio es público y la base tiene datos personales y hashes de contraseñas: por eso el archivo nunca sale sin cifrar y la clave (`BACKUP_PASSPHRASE`) solo existe en los secrets.
- **Manual**: `scripts\respaldar-aiven.ps1` (Windows). Pide la contraseña en pantalla y deja el archivo en `respaldos\` (ignorada por git).
- **Aiven**: la consola tiene una pestaña *Backups*. Revisa allí qué conserva tu plan; no la tomes como único respaldo.
- **No están en la base**: las imágenes (Cloudinary guarda las suyas; no las borres desde su consola) y el código (GitHub).

### Puesta en marcha (una sola vez)
1. Crear la clave de cifrado y guardarla **también** en un gestor de contraseñas (sin ella los respaldos no se pueden abrir):
   ```bash
   openssl rand -base64 48
   ```
2. En GitHub → *Settings → Secrets and variables → Actions → New repository secret*: crear los seis secrets de la sección 2.
   Para `AIVEN_DB_*` usa los datos de *Connection information* de Aiven.
   Idealmente crea en Aiven un usuario aparte solo para esto, en vez de usar `avnadmin`: le bastan los permisos `SELECT`, `SHOW VIEW`, `TRIGGER` y `EVENT` sobre la base.
3. Los workflows con horario **solo corren desde la rama por defecto del repositorio** (`main`/`master`). Hasta que ese archivo llegue allí, no se ejecutan solos.
   Para probar el respaldo: pestaña *Actions → Respaldo diario → Run workflow* (también necesita el archivo en la rama por defecto).
4. Revisar que el primer respaldo salga en verde y que en el resumen aparezcan las tablas, usuarios y proyectos restaurados.

### Descargar y abrir un respaldo
1. GitHub → *Actions* → el último "Respaldo diario" en verde → *Artifacts* → `respaldo-bd-cifrado`.
2. Descifrar (en Git Bash o WSL, que traen `openssl`):
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
4. `BACKUP_PASSPHRASE`: al cambiarla, los respaldos anteriores siguen necesitando la clave vieja; conserva ambas anotadas.

## 8. Antes de publicar el sitio de verdad

- [ ] Dominio propio con HTTPS, y actualizar todo lo de la fila "Cambia el dominio".
- [ ] Planes de pago o equivalentes: base de datos que no se apague, API que no se duerma, SMTP que funcione.
- [ ] Datos legales completos (domicilio, responsable de los datos, licencias) y revisión del abogado, incluida la versión en inglés.
- [ ] Textos en inglés revisados por la empresa.
- [ ] Borrar los proyectos y los datos de prueba.
- [ ] Respaldo automático en verde y una restauración de prueba hecha.
- [ ] Monitoreo con alertas llegando a una persona que las atienda.
- [ ] Cookie HttpOnly para la sesión (fase 2 del plan de endurecimiento), cuando el sitio y la API compartan dominio.
