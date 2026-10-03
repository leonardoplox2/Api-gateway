# Orders Platform — API Gateway + Microservicios + OAuth 2.0

API backend para registrar, consultar, cancelar y ver el historial de pedidos de productos, construida como una arquitectura de microservicios detrás de un API Gateway con seguridad OAuth 2.0 / OIDC (JWT) vía Keycloak.

## Arquitectura

```
Postman (Cliente API)
        |
        v
   API Gateway  (OAuth2 + rutas + X-Trace-Id)  --- puerto 8080
        |            \
        v             v
 Order Service    Inventory Service
 (pedidos +        (stock +
  historial)       disponibilidad)
 puerto 8082        puerto 8083
        |                |
        v                v
   order-db         inventory-db
  (Postgres)         (Postgres)

   Auth Server: Keycloak (puerto 8081)
```

- El **API Gateway** es el único punto público del sistema. No contiene reglas de negocio: solo valida el JWT, enruta las solicitudes y genera/propaga el header `X-Trace-Id`.
- **Order Service** administra pedidos, sus items y su historial de cambios de estado.
- **Inventory Service** administra productos, stock y disponibilidad.
- Cada microservicio tiene su propia base de datos — no hay acceso cruzado a datos entre servicios, toda comunicación entre ellos es vía HTTP reactivo (`WebClient`).
- La comunicación Order → Inventory es síncrona (petición-respuesta), sin colas de mensajes ni Saga, para mantener el diseño simple.

## Stack técnico

- **Java 17**, Spring Boot 4.1.1
- **Spring Cloud Gateway** (reactivo, sobre WebFlux) para el API Gateway
- **Spring WebFlux** en los 3 servicios
- **Spring Data JPA** + **PostgreSQL 16** para persistencia (JPA es bloqueante; se combina con WebFlux envolviendo las llamadas en `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`)
- **Spring Security (OAuth2 Resource Server)** con JWT
- **Keycloak 26** como Authorization Server
- **AOP** (AspectJ) para logging transversal de la capa de servicio
- **Lombok** y **Jackson**
- **springdoc-openapi 3.1.1** (OpenAPI 3 / Swagger UI)
- **JUnit 5** + **Mockito** para pruebas
- **Docker** y **Docker Compose** para orquestar todo el entorno
- **Logback** con `logback-spring.xml` propio en cada microservicio

## Estructura del repositorio

```
orders-platform/
├── api-gateway/          # Spring Cloud Gateway: seguridad, ruteo, trace-id
├── order-service/        # Microservicio de pedidos
├── inventory-service/    # Microservicio de inventario
├── postman/              # Coleccion de Postman lista para importar
├── docker-compose.yml    # Orquesta los 6 contenedores (2 DB + Keycloak + 3 servicios)
└── README.md
```

## Cómo levantar el proyecto

### 1. Requisitos

Solo **Docker Desktop**. No se necesita tener Java, Maven ni Postgres instalados localmente.

### 2. Levantar todo con Docker Compose

```bash
docker compose up --build
```

Esto construye las 3 imágenes (Maven multi-stage build) y levanta 6 contenedores: `order-db`, `inventory-db`, `keycloak`, `order-service`, `inventory-service`, `api-gateway`. La primera vez tarda varios minutos (descarga de imágenes base + compilación).

### 3. Configurar el realm de Keycloak (solo la primera vez)

> Esta es una configuración manual pendiente de automatizar (ver sección "Mejoras futuras"). Sin este paso, la autenticación no va a funcionar.

1. Abrir `http://localhost:8081` → login `admin` / `admin`.
2. **Create Realm** → nombre `pedidos-realm`.
3. Dentro del realm → **Clients → Create client**:
   - Client ID: `postman-client`
   - Client authentication: `ON`
   - Authentication flow: marcar solo **Service accounts roles**
4. Pestaña **Credentials** del client → copiar el **Client Secret**.

### 4. Sembrar un producto de prueba

Inventory Service no expone un endpoint para crear productos (queda fuera del alcance funcional del proyecto), así que se inserta directo por SQL:

```bash
docker exec inventory-db psql -U inventory_user -d inventory_db -c "INSERT INTO products (id, name, description, price, available_quantity, version) VALUES (gen_random_uuid(), 'Laptop', 'Laptop de prueba', 1500.00, 10, 0);"
```

Consultar el id generado:
```bash
docker exec inventory-db psql -U inventory_user -d inventory_db -c "SELECT id, name, available_quantity FROM products;"
```

## Autenticación

Flujo **Client Credentials** de OAuth 2.0 contra Keycloak:

```bash
curl -X POST http://localhost:8081/realms/pedidos-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=postman-client" \
  -d "client_secret=<tu-secret>"
```

El `access_token` devuelto se envía en el header `Authorization: Bearer <token>` en cada request al Gateway (`http://localhost:8080`).

## Endpoints

Todos se consumen a través del Gateway (`http://localhost:8080`), con excepción de `/actuator/health`.

| Método | Ruta | Descripción | Auth |
|---|---|---|---|
| GET | `/actuator/health` | Health check del Gateway | No |
| POST | `/api/orders` | Crear un pedido (reserva stock automáticamente) | Sí |
| GET | `/api/orders/{id}` | Consultar un pedido | Sí |
| POST | `/api/orders/{id}/cancel` | Cancelar un pedido (libera el stock si estaba CONFIRMED) | Sí |
| GET | `/api/orders/{id}/history` | Historial de cambios de estado | Sí |
| POST | `/api/inventory/reserve` | Reservar stock de un producto | Sí |
| POST | `/api/inventory/release` | Liberar (devolver) stock de un producto | Sí |

`POST /api/inventory/release` es invocado internamente por Order Service cuando se cancela un pedido que ya estaba `CONFIRMED` — así el stock disponible en Inventory Service siempre queda consistente con los pedidos realmente activos.

Ejemplo de body para crear un pedido:
```json
{
  "items": [
    { "productId": "<uuid-del-producto>", "quantity": 2, "unitPrice": 1500.00 }
  ]
}
```

## Documentación interactiva

- **Swagger UI**:
  - Order Service: `http://localhost:8082/swagger-ui.html`
  - Inventory Service: `http://localhost:8083/swagger-ui.html`
  - (Se accede directo a cada microservicio, sin pasar por el Gateway ni requerir token, para poder probar la lógica de negocio en aislamiento.)
- **Colección de Postman**: [`postman/orders-api.postman_collection.json`](postman/orders-api.postman_collection.json) — incluye carpetas para autenticación, el flujo feliz completo (vía Gateway), casos de error, e invocación directa a cada microservicio para debug.

## Trazabilidad (X-Trace-Id)

El API Gateway genera un `X-Trace-Id` (UUID) si el cliente no lo envía, o respeta el que reciba. Ese identificador:
- Se agrega como header a la petición antes de rutearla.
- Se devuelve en la respuesta al cliente.
- Se propaga a Order Service e Inventory Service, que lo leen y lo incluyen en sus logs y en el body de sus respuestas de error.

Esto permite rastrear una misma petición a través de los 3 servicios buscando ese identificador en los logs de cada uno.

## Modelo de estados del pedido

```
PENDING ──(stock OK)──> CONFIRMED ──(cancelar)──> CANCELLED
PENDING ──(sin stock)──> FAILED
PENDING ──(cancelar)───> CANCELLED
```

- `FAILED` y `CANCELLED` son estados terminales: no permiten ninguna transición posterior.
- Un intento de transición no permitida (ej. cancelar un pedido ya cancelado) lanza `InvalidOrderTransitionException`, mapeada a `409 INVALID_TRANSITION`.
- Cada transición queda registrada en la tabla `order_status_history`, con el estado anterior, el nuevo, la razón, y la fecha.

La regla de transiciones válidas vive centralizada en `OrderStatusTransitions` (un mapa estático de estado → estados permitidos), consultada por la entidad `Order` antes de aplicar cualquier cambio de estado (patrón **State**).

## Manejo de errores

Formato uniforme en los 3 servicios:

```json
{
  "timestamp": "2026-09-13T20:00:00Z",
  "status": 409,
  "code": "STOCK_INSUFFICIENT",
  "message": "Stock insuficiente para el producto ...",
  "traceId": "a1b2c3d4-..."
}
```

| Código | HTTP | Cuándo ocurre |
|---|---|---|
| `INVALID_TOKEN` | 401 | Token ausente, inválido o expirado (Gateway) |
| `FORBIDDEN` | 403 | Token válido pero sin el permiso requerido (Gateway) |
| `ORDER_NOT_FOUND` | 404 | No existe un pedido con el id solicitado |
| `PRODUCT_NOT_FOUND` | 404 | No existe un producto con el id solicitado |
| `INVALID_TRANSITION` | 409 | Transición de estado no permitida (ej. cancelación duplicada) |
| `STOCK_INSUFFICIENT` | 409 | No hay stock suficiente para reservar |

## Patrones de diseño y principios SOLID aplicados

| Patrón / Principio | Dónde |
|---|---|
| **State** | `Order` + `OrderStatusTransitions`: las transiciones de estado están encapsuladas en la entidad, que consulta una tabla de reglas centralizada antes de cambiar de estado |
| **Dependency Inversion (DIP)** | `OrderService` depende de la interfaz `InventoryClient`, no de `WebClient` directamente; `InventoryWebClientAdapter` es la única implementación concreta |
| **Factory Method** | `Order.createNew()` garantiza que todo pedido nace en un estado consistente (`PENDING`); `ErrorResponse.of(...)` centraliza la construcción del error |
| **Single Responsibility (SRP)** | Separación entre `RoutesConfig`, `SecurityConfig`, `JacksonConfig`, `WebClientConfig`, `TraceIdWebFilter`, `LoggingAspect` — cada clase tiene una única razón de cambio |
| **Repository Pattern** | `OrderRepository`, `ProductRepository` vía Spring Data JPA |
| **AOP (cross-cutting concerns)** | `LoggingAspect` separa el logging de entrada/salida y tiempos de ejecución de la lógica de negocio de los servicios |
| **Open/Closed** | Agregar un nuevo estado o código de error no requiere modificar la lógica de las excepciones existentes, solo extender las tablas/handlers |

## Decisiones de diseño y trade-offs

- **`ddl-auto=update` en vez de Flyway/Liquibase**: pragmático para esta etapa del proyecto. En un entorno real se usarían migraciones versionadas.
- **Sin compensación/rollback entre items de un mismo pedido**: si un pedido tiene varios items y uno falla al reservar stock después de que otro ya se reservó, no se revierte la reserva previa. Implementar Saga/compensaciones distribuidas queda fuera del alcance actual.
- **`FetchType.EAGER`** en las colecciones `items` y `statusHistory` de `Order`: evita `LazyInitializationException` al acceder a esas colecciones fuera de la sesión de Hibernate en un flujo reactivo (WebFlux no soporta "Open Session In View" como sí lo hace un stack MVC tradicional).
- **Order Service e Inventory Service no re-validan el JWT**: confían en que toda petición que les llega ya pasó por el Gateway. Es la opción "simple" frente a la "robusta" (donde cada microservicio también valida el token) — válida para el alcance actual, documentada aquí como decisión consciente.
- **Configuración de Keycloak manual**: el realm no se auto-importa al levantar los contenedores (ver "Mejoras futuras").

## Cómo correr los tests

```bash
cd order-service && ./mvnw test
cd inventory-service && ./mvnw test
```

Cobertura actual: la máquina de estados completa (`OrderStatusTransitionsTest`), el comportamiento de la entidad `Order` (confirmar, cancelar, historial, transiciones inválidas y cancelaciones duplicadas), y la lógica de `InventoryService` (reserva exitosa, stock insuficiente, producto inexistente) con Mockito.

## Mejoras futuras (próximos pasos)

- Automatizar la carga del realm de Keycloak (`--import-realm` + export del JSON del realm), para que `docker compose up --build` deje todo funcionando sin pasos manuales.
- Endpoint de administración de productos en Inventory Service (crear/reabastecer stock), hoy solo se siembra por SQL directo.
- Circuit breaker (Resilience4j) en el `InventoryClient` para mayor resiliencia ante caídas de Inventory Service.
- Propagación de `X-Trace-Id` vía MDC/Reactor Context para que aparezca en *todos* los logs de forma automática (hoy se loguea explícitamente al entrar cada request, una simplificación consciente frente a la complejidad de Context Propagation en WebFlux).
