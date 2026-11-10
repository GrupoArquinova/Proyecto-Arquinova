# Guía: volver privado el repositorio y limpiar el historial de git

Esta guía **no se ejecuta sola**: son pasos manuales que haces tú, en orden. No contiene valores reales de contraseñas ni claves; los textos sensibles se escriben **solo en un archivo local** que nunca se sube.

Contexto: en el historial del repositorio quedaron archivos con datos sensibles (por ejemplo `k8s/secret.yaml` y valores por defecto en `application.yaml`). Mientras el repositorio sea público, **cualquier persona puede verlos en el historial aunque el archivo ya no exista en la rama actual**. Por eso el orden importa: primero se vuelve inútil lo expuesto (rotar), después se reduce la exposición (privado) y, al final, se limpia el historial.

## 1. Orden recomendado

1. **Rotar todos los secretos** (sección 2). Es lo único que de verdad neutraliza una filtración: un secreto que ya estuvo público se debe dar por comprometido aunque borres el historial (hay forks, cachés y rastreadores automáticos). Procedimiento detallado en `docs/OPERACION.md` §7.1.
2. **Volver el repositorio privado** (sección 3).
3. **Opcional: limpiar el historial** con `git filter-repo` (sección 4).
4. **Verificar** con gitleaks/trufflehog (sección 5).

No limpies el historial *antes* de rotar: reescribir git no recupera nada que ya se copió.

## 2. Rotar los secretos primero

Rota todo lo que haya estado en git alguna vez, aunque creas que ya no sirve (lista y comandos en `docs/OPERACION.md` §7.1). Como mínimo:

- Contraseña de la base de datos (Aiven) y, si existe, la de Redis.
- `JWT_SECRET` (al cambiarlo se cierran todas las sesiones, es lo esperado).
- Credenciales de correo (SMTP), Cloudinary y `GEMINI_API_KEY`.
- Contraseñas de los usuarios administradores de la aplicación.
- Claves y secretos de los respaldos (`BACKUP_*`, `AWS_*`) si alguna vez estuvieron en el repo.

Actualiza los valores nuevos en Render, GitHub (*Secrets*) y tu `.env` local, y comprueba que el sitio, el correo y el respaldo siguen funcionando.

## 3. Hacer privado el repositorio

GitHub → el repositorio → *Settings* → pestaña *General* → bajar hasta **Danger Zone** → **Change repository visibility** → *Make private* → escribir el nombre para confirmar.

Qué cambia:

- **GitHub Actions**: en repositorios privados de una cuenta personal los minutos gratuitos son limitados (2.000 min/mes en el plan Free; los de macOS y Windows cuentan con multiplicador). Los workflows de este proyecto usan `ubuntu`, que cuesta 1x. Revisa *Settings → Billing → Usage* durante el primer mes. El respaldo diario consume pocos minutos.
- **Respaldos**: con el repositorio privado, el artefacto de Actions deja de ser público (ver "Respaldos: cifrado y destino" en `docs/OPERACION.md`), pero sigue siendo preferible el bucket.
- **Render y Vercel**: pierden el acceso hasta que **re-autorices la app de GitHub** en cada uno (Render: *Account Settings → GitHub → Configure*; Vercel: *Settings → Git → Connected Git Repository*; en GitHub: *Settings → Applications → Installed GitHub Apps → Configure → Repository access*, e incluye este repositorio). Si no lo haces, los despliegues automáticos fallan con error de acceso. Comprueba que un push de prueba despliega.
- **Forks**: los forks públicos existentes **no se vuelven privados**: se desconectan y siguen siendo públicos con una copia completa del historial. Revisa *Insights → Forks* antes de cambiar la visibilidad y, si hay forks ajenos, pide a sus dueños que los borren (o contacta a GitHub Support). Por esto rotar (sección 2) es indispensable.
- **Estrellas y seguidores** se pierden; los enlaces públicos al repositorio (README, QR, documentos) dejan de funcionar para quien no tenga acceso.
- **GitHub Pages / badges / releases** públicos dejan de ser visibles.
- Los colaboradores conservan el acceso; revisa quién tiene acceso en *Settings → Collaborators*.

## 4. Opcional: limpiar el historial con `git filter-repo`

Hazlo solo si necesitas que el historial quede limpio (por ejemplo, para volver a hacerlo público o compartirlo). **Reescribe todos los commits**: cambian todos los identificadores y los colaboradores deberán volver a clonar.

### 4.0 Preparación

```bash
# Herramienta (requiere Python 3): https://github.com/newren/git-filter-repo
pip install git-filter-repo

# Trabaja sobre un clon espejo NUEVO, nunca sobre tu carpeta de trabajo
git clone --mirror https://github.com/<USUARIO>/<REPOSITORIO>.git repo-limpio.git
cd repo-limpio.git

# Copia de seguridad del espejo original por si algo sale mal (guárdala fuera de internet)
cp -r . ../repo-respaldo-sin-limpiar.git
```

### 4.1 Quitar `k8s/secret.yaml` de todo el historial

```bash
git filter-repo --invert-paths --path k8s/secret.yaml
```

Si el archivo estuvo en otras rutas o con otros nombres, añade más `--path` (puedes repetir la opción). Para listar candidatos:

```bash
git log --all --name-only --pretty=format: | sort -u | grep -iE 'secret|\.env|credential|password|\.pem|\.key'
```

### 4.2 Reemplazar los textos sensibles de `application.yaml` (y de cualquier otro archivo)

Crea **localmente** un archivo `reemplazos.txt` **fuera del repositorio** (por ejemplo en tu escritorio) con una línea por cada texto sensible, con este formato (una regla por línea, el texto a la izquierda del `==>`):

```
TEXTO_SENSIBLE_EXACTO==>***REMOVIDO***
```

- Escribe cada valor real tal como aparece en el historial. **No** subas este archivo a git, no lo pegues en un chat ni lo guardes en la nube; bórralo al terminar (`shred -u reemplazos.txt`, o en Windows `cipher /w:` sobre la carpeta).
- Para valores que se repiten con pequeñas variaciones puedes usar expresiones regulares: `regex:patrón==>***REMOVIDO***`.
- Cada valor debe ser lo bastante específico: un texto muy corto (por ejemplo una palabra común) dañaría otros archivos.

Aplícalo:

```bash
git filter-repo --replace-text /ruta/a/reemplazos.txt
```

Puedes ejecutar 4.1 y 4.2 en la misma llamada (`git filter-repo --invert-paths --path k8s/secret.yaml --replace-text /ruta/a/reemplazos.txt`) o por separado. El código actual de `application.yaml` debe leer los secretos de variables de entorno sin valores por defecto sensibles (revísalo antes de reescribir).

### 4.3 Revisar el resultado antes de publicar

```bash
git log --oneline | head              # los identificadores cambiaron
git log --all --oneline -- k8s/secret.yaml   # no debe imprimir nada
```

Usa gitleaks sobre el espejo (sección 5) **antes** de subirlo.

### 4.4 Subir con force-push (todas las ramas y etiquetas)

`git filter-repo` quita el remoto `origin` por seguridad; vuelve a añadirlo:

```bash
git remote add origin https://github.com/<USUARIO>/<REPOSITORIO>.git
git push --force --mirror origin
```

`--mirror` sube todas las ramas y etiquetas reescritas y **borra del remoto las que ya no existan**. Si el repositorio tiene ramas protegidas, desactiva temporalmente la protección (*Settings → Branches*) y vuélvela a activar después.

Las referencias de pull requests (`refs/pull/*`) no se pueden sobrescribir con push: los commits viejos seguirán accesibles por esas referencias y por su identificador **mientras GitHub no los purgue** (ver 4.6).

### 4.5 Avisar a colaboradores

Cada persona que tenga el repositorio clonado debe **volver a clonar** (no `git pull`: si hace pull o merge, vuelve a introducir el historial viejo):

```bash
# Borra o renombra la carpeta antigua y clona de nuevo
git clone https://github.com/<USUARIO>/<REPOSITORIO>.git
```

Si tenían trabajo sin subir, que lo copie a mano al clon nuevo (cherry-pick no sirve: los commits viejos tienen otros identificadores). Haz lo mismo con tu propia carpeta de trabajo y con los clones del servidor de despliegue, si hay.

### 4.6 Borrar ramas viejas y limpiar la caché de GitHub

1. Borra las ramas remotas que no uses (*Code → Branches*, o `git push origin --delete <rama>`), incluidas ramas `feature/*` antiguas, porque cada rama vieja conserva commits sin limpiar si no se reescribieron.
2. Cierra o elimina los pull requests antiguos que contengan los datos sensibles (los `refs/pull/*` los conservan).
3. GitHub sigue sirviendo commits viejos por su identificador (URL directa) y desde forks hasta que ejecuta su recolección de basura. Para forzar la purga abre un caso en **GitHub Support** (https://support.github.com/contact) pidiendo "remove cached views and purge dangling commits from a repository after a history rewrite", indicando el repositorio y, si lo conoces, los identificadores de los commits viejos. Mientras no lo hagan, trata los datos como expuestos (por eso se rota primero).
4. Si hubo forks públicos, pide su eliminación o inclúyelos en la solicitud a GitHub Support.

## 5. Lista de verificación posterior

- [ ] Todos los secretos de la sección 2 están rotados y los valores viejos ya no funcionan (prueba iniciar sesión en Aiven/Cloudinary/etc. con los viejos: deben fallar).
- [ ] El repositorio aparece como **Private** en *Settings → General*.
- [ ] Render y Vercel despliegan un commit nuevo sin errores de acceso.
- [ ] El respaldo diario corre en verde (*Actions → Respaldo diario → Run workflow*).
- [ ] gitleaks local sin hallazgos sobre todo el historial:
  ```bash
  # https://github.com/gitleaks/gitleaks  (winget install gitleaks / brew install gitleaks)
  gitleaks git --redact -v .          # revisa todo el historial; --redact oculta los valores en pantalla
  gitleaks dir --redact -v .          # revisa también los archivos actuales
  ```
- [ ] trufflehog local como segunda opinión (verifica además si el secreto sigue activo):
  ```bash
  # https://github.com/trufflesecurity/trufflehog
  trufflehog git file://. --only-verified
  ```
- [ ] Búsqueda manual de los textos reemplazados: `git log --all -S"TEXTO_SENSIBLE" --oneline` (no debe devolver commits; escribe el texto solo en tu terminal).
- [ ] El workflow `.github/workflows/secretos.yml` (gitleaks en cada push y PR) está en verde y es obligatorio antes de fusionar (*Settings → Branches → Require status checks*).
- [ ] `.env` y `respaldos/` están en `.gitignore` y no hay archivos de secretos versionados (`git ls-files | grep -iE 'secret|\.env|\.pem|\.key'`).
- [ ] Borrado de `reemplazos.txt` y de la copia de seguridad del espejo cuando ya no hagan falta (la copia contiene los datos sensibles).
- [ ] Colaboradores avisados y con clones nuevos.
