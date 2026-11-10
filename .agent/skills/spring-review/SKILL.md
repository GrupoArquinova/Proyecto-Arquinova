\---

name: spring-review

description: Optimiza y revisa código de Spring Boot 3, JPA/Hibernate y MySQL sin sobrecargar el contexto. Usar cuando el usuario pida revisar o refactorizar entidades, repositorios, controladores o configuraciones de Spring.

\---



\# Reglas de Revisión para Spring Boot



Al ejecutar esta skill, debes seguir estrictamente estas directrices para mantener la velocidad y ahorrar tokens:



1\. \*\*Concisión Máxima\*\*: Entrega el código corregido de manera directa. Evita introducciones extensas, explicaciones teóricas o resúmenes innecesarios.

2\. \*\*Spring Boot 3 \& Java 21\*\*:

&#x20;  \* Asegura el uso correcto de Jakarta Persistence (`jakarta.persistence.\*`).

&#x20;  \* Valida firmas en Spring Data JPA (por ejemplo, usar `findByLote\_Id` cuando se consulta por la relación id).

&#x20;  \* En consultas con dialecto MySQL, prefiere sintaxis optimizada para MySQL 8+.

3\. \*\*Manejo de Errores y Seguridad\*\*:

&#x20;  \* Verifica la correcta configuración de filtros JWT y excepciones en Spring Security.

&#x20;  \* Si sugieres cambios en `application.yml`, incluye la propiedad `spring.jpa.open-in-view: false`.

