# DOSW Laboratory 4 — README Project

**Course:** DOSW — Software Development and Operations

**Institution:** Escuela Colombiana de Ingeniería Julio Garavito

**Activity:** BACKEND DEVELOPMENT GUIDE TEST


### Team Information & Contributions

| Name                            | GitHub Username |
|---------------------------------|-----------------|

| Daniel Ruiz Patiño        | `DaniRuiz11`    |

# Manual del Back-end: ToDo API (Spring Boot + PostgreSQL + Docker)

Manual paso a paso de cómo se construyó el back-end del laboratorio. Está pensado para replicarlo desde cero en otro proyecto (por ejemplo, un microservicio en el parcial): cambiando la entidad y los nombres, la estructura es la misma.

**Stack usado**

| Tecnología | Versión / detalle |
|---|---|
| Java | compila con `release 17` (el equipo corre Java 25) |
| Spring Boot | 4.1.1 |
| Hibernate | 7.4.5 |
| Tomcat | 11 (embebido) |
| Base de datos | PostgreSQL 17 (imagen `postgres:17-alpine`) en Docker |
| Build | Maven |
| Pruebas | JUnit 5, Mockito, AssertJ, MockMvc |
| Cobertura | JaCoCo 0.8.13 |

**Paquete base:** `edu.eci.dosw.todo`
**Puerto de la API:** `8080`
**Base de datos:** `todo_db` / usuario `todo_user` / clave `todo_password`

---

## Índice

1. [Arquitectura y estructura de carpetas](#1-arquitectura-y-estructura-de-carpetas)
2. [Crear el proyecto](#2-crear-el-proyecto)
3. [PostgreSQL con Docker](#3-postgresql-con-docker)
4. [Crear la tabla](#4-crear-la-tabla)
5. [application.properties](#5-applicationproperties)
6. [Enums y entidad (JPA)](#6-enums-y-entidad-jpa)
7. [Repository](#7-repository)
8. [DTOs](#8-dtos)
9. [Excepción y manejador global](#9-excepción-y-manejador-global)
10. [Service (interfaz e implementación)](#10-service-interfaz-e-implementación)
11. [Controller](#11-controller)
12. [Pruebas unitarias del Service](#12-pruebas-unitarias-del-service)
13. [Pruebas del Controller](#13-pruebas-del-controller)
14. [JaCoCo (cobertura)](#14-jacoco-cobertura)
15. [Ejecutar y probar la API](#15-ejecutar-y-probar-la-api)
16. [Comandos principales (chuleta)](#16-comandos-principales-chuleta)
17. [Errores comunes y soluciones](#17-errores-comunes-y-soluciones)
18. [Checklist para el parcial (microservicio nuevo)](#18-checklist-para-el-parcial-microservicio-nuevo)

---

## 1. Arquitectura y estructura de carpetas

Arquitectura en capas. Cada capa solo habla con la siguiente:

```
Cliente HTTP  ->  Controller  ->  Service  ->  Repository  ->  PostgreSQL
                  (DTOs)         (DTOs <-> Entity)   (Entity)
```

- **Controller**: recibe HTTP, valida (`@Valid`), delega al service y decide el código de estado.
- **Service**: lógica de negocio, valores por defecto, convierte DTO <-> Entity, lanza excepciones.
- **Repository**: acceso a datos (Spring Data JPA).
- **Entity**: clase mapeada a la tabla.
- **DTO**: objetos de entrada/salida de la API. La entidad nunca sale al cliente.

```
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/edu/eci/dosw/todo/
    │   │   ├── TodoApiApplication.java
    │   │   ├── controller/
    │   │   │   └── TaskController.java
    │   │   ├── dto/
    │   │   │   ├── TaskCreateRequest.java
    │   │   │   ├── TaskUpdateRequest.java
    │   │   │   └── TaskResponse.java
    │   │   ├── entity/
    │   │   │   ├── TaskEntity.java
    │   │   │   ├── TaskPriority.java
    │   │   │   └── TaskStatus.java
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   └── TaskNotFoundException.java
    │   │   ├── repository/
    │   │   │   └── TaskRepository.java
    │   │   └── service/
    │   │       ├── TaskService.java
    │   │       └── TaskServiceImpl.java
    │   └── resources/
    │       └── application.properties
    └── test/java/edu/eci/dosw/todo/
        ├── TodoApiApplicationTests.java
        ├── controller/TaskControllerTest.java
        └── service/TaskServiceTest.java
```

**Regla clave:** la clase principal (`TodoApiApplication`) debe estar en el paquete raíz (`edu.eci.dosw.todo`). Spring escanea ese paquete y todos los de abajo. Si pones una clase fuera de él, no la encuentra.

---

## 2. Crear el proyecto

En https://start.spring.io:

- Project: **Maven**, Language: **Java**
- Spring Boot: **4.1.1** (o la que indique el parcial)
- Group: `edu.eci.dosw`, Artifact: `todo-api`
- Java: 17
- Dependencias a marcar:
  - **Spring Web**
  - **Spring Data JPA**
  - **PostgreSQL Driver**
  - **Validation**

Descomprimir, y trabajar dentro de la carpeta `backend`.

> **Importante:** los nombres exactos de los artifacts (`spring-boot-starter-...`) cambiaron en Spring Boot 4. No los copies de memoria de tutoriales viejos: usa los que genera Initializr en tu `pom.xml`. Si falta alguno, agrégalo desde Initializr con "Explore" o "Add dependencies".

Dependencias mínimas que debe tener el `pom.xml` (los nombres pueden variar levemente según la versión de Boot, verifica en el tuyo):

- Web (servidor REST)
- Data JPA
- Driver de PostgreSQL (`org.postgresql:postgresql`, scope `runtime`)
- Validation (`spring-boot-starter-validation`)
- Starter de pruebas (`spring-boot-starter-test`) y, para `@WebMvcTest` en Boot 4, el starter de pruebas de WebMVC (`spring-boot-starter-webmvc-test`, scope `test`)

Ejemplo de las dos que más se olvidan:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Si tras editar el `pom.xml` VS Code muestra errores rojos en imports, acepta "Reload" en el aviso de configuración del proyecto Java.

---

## 3. PostgreSQL con Docker

PostgreSQL **no se instala en Windows**: corre dentro de un contenedor. Lo único que se instala es Docker Desktop (una sola vez).

### 3.1 Instalar Docker Desktop (solo si `docker` no se reconoce)

```powershell
winget install -e --id Docker.DockerDesktop
```

Reiniciar si lo pide, abrir Docker Desktop, esperar a que diga "Engine running", abrir una PowerShell **nueva**:

```powershell
docker --version
```

### 3.2 Descargar la imagen

```powershell
docker pull postgres:17-alpine
docker images
```

### 3.3 Crear y arrancar el contenedor (solo la primera vez)

```powershell
docker run --name todo-postgres `
  -e POSTGRES_DB=todo_db `
  -e POSTGRES_USER=todo_user `
  -e POSTGRES_PASSWORD=todo_password `
  -p 5432:5432 `
  -d postgres:17-alpine
```

(En PowerShell la continuación de línea es el acento grave `` ` ``. En una sola línea también funciona.)

### 3.4 Uso diario

```powershell
docker start todo-postgres     # encender (si ya fue creado)
docker ps                      # verificar que está "Up"
docker stop todo-postgres      # apagar
docker logs todo-postgres      # ver logs si algo falla
```

Si hay que rehacerlo desde cero con otras credenciales (las variables `POSTGRES_*` solo se aplican la primera vez que se crea):

```powershell
docker rm -f todo-postgres
```

y volver a ejecutar el `docker run` del punto 3.3. **Esto borra los datos.**

---

## 4. Crear la tabla

Con `ddl-auto=validate` Hibernate **no crea tablas**: solo verifica que existan y coincidan con la entidad. Hay que crearla a mano.

Entrar a `psql` dentro del contenedor:

```powershell
docker exec -it todo-postgres psql -U todo_user -d todo_db
```

Pegar el SQL:

```sql
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_task_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT chk_task_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
);
```

Comandos útiles dentro de `psql`:

| Comando | Qué hace |
|---|---|
| `\dt` | listar tablas |
| `\d tasks` | describir la tabla |
| `SELECT * FROM tasks;` | ver los datos |
| `\q` | salir |

Alternativa: guardar el SQL en un archivo `schema.sql` y ejecutarlo sin entrar a psql:

```powershell
Get-Content schema.sql | docker exec -i todo-postgres psql -U todo_user -d todo_db
```

---

## 5. application.properties

`src/main/resources/application.properties`:

```properties
spring.application.name=todo-api
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/todo_db}
spring.datasource.username=${DB_USER:todo_user}
spring.datasource.password=${DB_PASSWORD:todo_password}

spring.jpa.hibernate.ddl-auto=validate

spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

- `${DB_URL:valor}` usa la variable de entorno `DB_URL` si existe; si no, el valor por defecto. Así no se suben credenciales reales a GitHub.
- `ddl-auto=validate`: Hibernate valida que la tabla coincida con la entidad y no modifica nada.
- `show-sql` y `format_sql`: imprimen el SQL en consola (útil para depurar).
- Para cambiar el puerto: `server.port=8081` (por defecto es 8080).

**Qué debe coincidir con el contenedor:** nombre de BD (`todo_db`), usuario (`todo_user`), clave (`todo_password`) y puerto (`5432`).

---

## 6. Enums y entidad (JPA)

### `entity/TaskStatus.java`

```java
package edu.eci.dosw.todo.entity;

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
```

### `entity/TaskPriority.java`

```java
package edu.eci.dosw.todo.entity;

public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}
```

### `entity/TaskEntity.java`

```java
package edu.eci.dosw.todo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TaskEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

### Por qué está mapeado así (esto es lo que más se pregunta)

| Decisión | Razón |
|---|---|
| `GenerationType.IDENTITY` | `BIGSERIAL` es autoincremental en PostgreSQL. Con `AUTO`/`SEQUENCE` Hibernate busca una secuencia que no existe y falla. |
| `@Enumerated(EnumType.STRING)` | Guarda `'PENDING'` como texto, que es lo que exigen los `CHECK`. Con el valor por defecto (`ORDINAL`) guardaría `0,1,2` y la BD lo rechaza. |
| `nullable = false`, `length = n` | Reflejan `NOT NULL` y `VARCHAR(n)` de la tabla. |
| Valores iniciales en Java (`= TaskStatus.PENDING`) | Hibernate envía todas las columnas en el `INSERT`; si `status` fuera `null`, el `DEFAULT` de SQL no se aplicaría y fallaría el `NOT NULL`. |
| `@PrePersist` en `createdAt` | Misma razón: se rellena justo antes del insert. `updatable = false` evita cambiarlo después. |
| `LocalDate` / `LocalDateTime` | Tipos Java equivalentes a `DATE` y `TIMESTAMP`. |
| `@Column(name = "due_date")` | Mapea el campo `dueDate` (camelCase) a la columna `due_date` (snake_case). |

---

## 7. Repository

### `repository/TaskRepository.java`

```java
package edu.eci.dosw.todo.repository;

import edu.eci.dosw.todo.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
}
```

`JpaRepository<Entidad, TipoDelId>` genera la implementación en tiempo de ejecución. Métodos gratis:

| Método | Qué hace |
|---|---|
| `save(entity)` | inserta (id nulo) o actualiza (id existente) |
| `findById(id)` | devuelve `Optional<TaskEntity>` |
| `findAll()` | lista todos |
| `deleteById(id)` | elimina por id |
| `existsById(id)` | `true` / `false` |
| `count()` | cantidad de registros |

Consultas derivadas (Spring las genera a partir del nombre del método; no se usaron en el laboratorio, pero sirven en un parcial):

```java
List<TaskEntity> findByStatus(TaskStatus status);
List<TaskEntity> findByPriority(TaskPriority priority);
List<TaskEntity> findByStatusAndPriority(TaskStatus status, TaskPriority priority);
List<TaskEntity> findByDueDateBefore(LocalDate date);
```

---

## 8. DTOs

Son `record` (inmutables, sin getters/setters). Las validaciones usan `jakarta.validation.constraints` (requieren `spring-boot-starter-validation`).

### `dto/TaskCreateRequest.java`

No recibe `id`, `status` ni `createdAt`: los genera el servidor.

```java
package edu.eci.dosw.todo.dto;

import edu.eci.dosw.todo.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskCreateRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 120, message = "El título no puede superar 120 caracteres")
        String title,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String description,

        TaskPriority priority,

        LocalDate dueDate
) {
}
```

### `dto/TaskUpdateRequest.java`

```java
package edu.eci.dosw.todo.dto;

import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskUpdateRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 120, message = "El título no puede superar 120 caracteres")
        String title,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String description,

        @NotNull(message = "El estado es obligatorio")
        TaskStatus status,

        @NotNull(message = "La prioridad es obligatoria")
        TaskPriority priority,

        LocalDate dueDate
) {
}
```

### `dto/TaskResponse.java`

```java
package edu.eci.dosw.todo.dto;

import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        LocalDateTime createdAt
) {
    public static TaskResponse from(TaskEntity task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt()
        );
    }
}
```

Notas:

- Las validaciones **solo se activan** si el controller usa `@Valid` en el parámetro.
- En la creación `priority` es opcional (el service pone `MEDIUM`). En la actualización `status` y `priority` son obligatorios porque la entidad no admite `null` en esas columnas.
- `TaskResponse.from(entity)` es el mapeo Entity -> DTO.

---

## 9. Excepción y manejador global

### `exception/TaskNotFoundException.java`

```java
package edu.eci.dosw.todo.exception;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long id) {
        super("Task with id " + id + " was not found");
    }
}
```

Extiende `RuntimeException` (no checked), así no hay que declarar `throws` en cada método.

### `exception/GlobalExceptionHandler.java`

```java
package edu.eci.dosw.todo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(int status, String message) {
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
```

Respuesta que produce:

```json
{
  "status": 404,
  "message": "Task with id 99 was not found"
}
```

Puntos clave:

- **Sin este handler, lanzar la excepción devuelve 500**, no 404. Alguien tiene que traducirla.
- `@RestControllerAdvice` aplica a todos los controllers.
- El `record ErrorResponse` debe ser `public` para que Jackson pueda serializarlo.
- Para manejar otro error, se agrega otro método `@ExceptionHandler(OtraExcepcion.class)` en la misma clase.
- Los errores de validación (`@Valid`) por ahora devuelven 400 con el cuerpo por defecto de Spring (`{"timestamp":...,"status":400,"error":"Bad Request","path":...}`), no con este formato.

---

## 10. Service (interfaz e implementación)

### `service/TaskService.java`

```java
package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;

import java.util.List;

public interface TaskService {

    List<TaskResponse> findAll();

    TaskResponse findById(Long id);

    TaskResponse create(TaskCreateRequest request);

    TaskResponse update(Long id, TaskUpdateRequest request);

    void delete(Long id);
}
```

### `service/TaskServiceImpl.java`

```java
package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.exception.TaskNotFoundException;
import edu.eci.dosw.todo.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public List<TaskResponse> findAll() {
        return taskRepository.findAll().stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Override
    public TaskResponse findById(Long id) {
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return TaskResponse.from(task);
    }

    @Override
    public TaskResponse create(TaskCreateRequest request) {
        TaskEntity task = new TaskEntity();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setStatus(TaskStatus.PENDING);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);

        return TaskResponse.from(taskRepository.save(task));
    }

    @Override
    public TaskResponse update(Long id, TaskUpdateRequest request) {
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());

        return TaskResponse.from(taskRepository.save(task));
    }

    @Override
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }
}
```

Qué cumple:

- `@Service` + **inyección por constructor** (campo `final`; con un solo constructor no hace falta `@Autowired`).
- **Valores por defecto al crear:** `status = PENDING`; `priority = MEDIUM` si el usuario no la envía (si la envía, se respeta); `createdAt` lo pone `@PrePersist` en la entidad.
- **Validar existencia:** `findById(...).orElseThrow(...)` y `existsById` en el delete.
- **Update:** modifica solo los 5 campos editables; `id` y `createdAt` no se tocan.
- No usa `@Transactional`: por eso en `update` el `save()` es necesario para persistir el cambio.

---

## 11. Controller

### `controller/TaskController.java`

```java
package edu.eci.dosw.todo.controller;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> findAll() {
        return ResponseEntity.ok(taskService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Endpoints

| Operación | Método | Endpoint | Código |
|---|---|---|---|
| Listar tareas | GET | `/api/v1/tasks` | 200 |
| Consultar tarea | GET | `/api/v1/tasks/{id}` | 200 / 404 |
| Crear tarea | POST | `/api/v1/tasks` | 201 / 400 |
| Actualizar tarea | PUT | `/api/v1/tasks/{id}` | 200 / 404 / 400 |
| Eliminar tarea | DELETE | `/api/v1/tasks/{id}` | 204 / 404 |

Anotaciones clave:

- `@RestController` = `@Controller` + `@ResponseBody` (devuelve JSON).
- `@RequestMapping("/api/v1/tasks")`: prefijo común.
- `@PathVariable`: toma `{id}` de la URL.
- `@RequestBody`: convierte el JSON del cuerpo al DTO.
- `@Valid`: dispara las validaciones del DTO (si fallan, 400 antes de llegar al service).
- `ResponseEntity`: permite elegir el código de estado (`ok`, `status(CREATED)`, `noContent`).

---

## 12. Pruebas unitarias del Service

Prueba el service **aislado**: el repository es un mock, no se levanta Spring ni la BD (no necesita Docker).

### `src/test/java/edu/eci/dosw/todo/service/TaskServiceTest.java`

```java
package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.exception.TaskNotFoundException;
import edu.eci.dosw.todo.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private TaskEntity buildTask(Long id, String title) {
        TaskEntity task = new TaskEntity();
        task.setId(id);
        task.setTitle(title);
        task.setDescription("Descripción de " + title);
        task.setStatus(TaskStatus.PENDING);
        task.setPriority(TaskPriority.MEDIUM);
        return task;
    }

    @Test
    void findAll_shouldReturnTasks() {
        when(taskRepository.findAll())
                .thenReturn(List.of(buildTask(1L, "Tarea 1"), buildTask(2L, "Tarea 2")));

        List<TaskResponse> result = taskService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TaskResponse::title)
                .containsExactly("Tarea 1", "Tarea 2");
    }

    @Test
    void findById_shouldReturnTaskWhenExists() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(buildTask(1L, "Tarea 1")));

        TaskResponse result = taskService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Tarea 1");
    }

    @Test
    void findById_shouldThrowExceptionWhenTaskDoesNotExist() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_shouldCreateTask() {
        TaskCreateRequest request = new TaskCreateRequest(
                "Nueva tarea", "Descripción", TaskPriority.HIGH, LocalDate.of(2026, 12, 1));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TaskResponse result = taskService.create(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Nueva tarea");
        assertThat(result.description()).isEqualTo("Descripción");
        assertThat(result.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void create_shouldAssignDefaultStatus() {
        TaskCreateRequest request = new TaskCreateRequest("Nueva tarea", null, null, null);
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.create(request);

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(captor.getValue().getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    void update_shouldUpdateExistingTask() {
        TaskEntity existing = buildTask(1L, "Original");
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Actualizada", "Nueva descripción", TaskStatus.COMPLETED,
                TaskPriority.LOW, LocalDate.of(2026, 11, 15));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.update(1L, request);

        assertThat(result.title()).isEqualTo("Actualizada");
        assertThat(result.description()).isEqualTo("Nueva descripción");
        assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(result.priority()).isEqualTo(TaskPriority.LOW);
        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    void update_shouldThrowExceptionWhenTaskDoesNotExist() {
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Actualizada", null, TaskStatus.PENDING, TaskPriority.LOW, null);
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(99L, request))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).save(any(TaskEntity.class));
    }

    @Test
    void delete_shouldDeleteExistingTask() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.delete(1L);

        verify(taskRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowExceptionWhenTaskDoesNotExist() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.delete(99L))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).deleteById(any());
    }
}
```

Piezas del patrón (memorízalas):

| Pieza | Para qué sirve |
|---|---|
| `@ExtendWith(MockitoExtension.class)` | activa Mockito en JUnit 5 |
| `@Mock` | crea el repository falso |
| `@InjectMocks` | crea `TaskServiceImpl` e inyecta el mock por constructor |
| `when(...).thenReturn(...)` | define qué responde el mock |
| `when(...).thenAnswer(...)` | respuesta calculada (ej. asignar un id al guardar) |
| `verify(mock).metodo(...)` | comprueba que se llamó |
| `verify(mock, never())...` | comprueba que NO se llamó |
| `ArgumentCaptor` | captura el objeto que se le pasó al mock para inspeccionarlo |
| `assertThat(...)` / `assertThatThrownBy(...)` | aserciones de AssertJ |

Nombre de los tests: `metodo_shouldHacerAlgoCuandoCondicion`.

---

## 13. Pruebas del Controller

Usa `@WebMvcTest`: levanta solo la capa web (controller + handler), con el **service simulado**. No necesita BD.

### `src/test/java/edu/eci/dosw/todo/controller/TaskControllerTest.java`

```java
package edu.eci.dosw.todo.controller;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.exception.TaskNotFoundException;
import edu.eci.dosw.todo.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final String BASE_URL = "/api/v1/tasks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    private TaskResponse buildResponse(Long id) {
        return new TaskResponse(id, "Tarea " + id, "Descripción", TaskStatus.PENDING,
                TaskPriority.HIGH, LocalDate.of(2026, 12, 1), LocalDateTime.of(2026, 10, 12, 10, 0));
    }

    @Test
    void findAll_shouldReturn200() throws Exception {
        when(taskService.findAll()).thenReturn(List.of(buildResponse(1L), buildResponse(2L)));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void findById_shouldReturn200WhenTaskExists() throws Exception {
        when(taskService.findById(1L)).thenReturn(buildResponse(1L));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Tarea 1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void findById_shouldReturn404WhenTaskDoesNotExist() throws Exception {
        when(taskService.findById(99L)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task with id 99 was not found"));
    }

    @Test
    void create_shouldReturn201WhenRequestIsValid() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest(
                "Nueva tarea", "Descripción", TaskPriority.HIGH, LocalDate.of(2026, 12, 1));
        when(taskService.create(any(TaskCreateRequest.class))).thenReturn(buildResponse(1L));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_shouldReturn400WhenRequestIsInvalid() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest("", null, null, null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn200WhenTaskExists() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Actualizada", "Nueva descripción", TaskStatus.IN_PROGRESS,
                TaskPriority.LOW, LocalDate.of(2026, 11, 15));
        when(taskService.update(eq(1L), any(TaskUpdateRequest.class))).thenReturn(buildResponse(1L));

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_shouldReturn404WhenTaskDoesNotExist() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Actualizada", null, TaskStatus.PENDING, TaskPriority.LOW, null);
        when(taskService.update(eq(99L), any(TaskUpdateRequest.class)))
                .thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(put(BASE_URL + "/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void delete_shouldReturn204WhenTaskExists() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());

        verify(taskService).delete(1L);
    }
}
```

### Diferencias de Spring Boot 4 (si copias de tutoriales de Boot 3, esto falla)

| En Boot 3 | En Boot 4 (este proyecto) |
|---|---|
| `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| `@MockBean` | `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`) |
| `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` (Jackson 3) |

Si los imports de `...webmvc.test...` no resuelven, agrega al `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

Piezas del patrón:

| Pieza | Para qué sirve |
|---|---|
| `@WebMvcTest(TaskController.class)` | levanta solo el controller (y el `GlobalExceptionHandler`) |
| `MockMvc` | simula peticiones HTTP sin servidor real |
| `@MockitoBean` | registra el service simulado en el contexto de Spring |
| `ObjectMapper` | convierte objetos Java a JSON para el body |
| `jsonPath("$.campo")` | verifica campos del JSON de respuesta |
| `thenThrow(...)` | el mock lanza la excepción, el handler la convierte en 404 |

El test `create_shouldReturn400WhenRequestIsInvalid` envía `title` vacío: `@Valid` + `@NotBlank` lo rechazan antes de llegar al service.

Total de pruebas del proyecto: **18** (9 del service + 8 del controller + 1 `TodoApiApplicationTests` generado por Initializr, que levanta el contexto completo y por eso necesita la BD arriba).

---

## 14. JaCoCo (cobertura)

Agregar dentro de `<build><plugins>` del `pom.xml`, junto al `spring-boot-maven-plugin`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.13</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Ejecutar y ver el reporte:

```powershell
cd backend
mvn clean verify
start target\site\jacoco\index.html
```

- El reporte se genera en la fase `verify` (con `mvn test` solo no se genera el HTML).
- Meta sugerida: **80 %**.
- Si da error "Unsupported class file major version", subir la versión de JaCoCo.

---

## 15. Ejecutar y probar la API

### Orden de arranque

1. Docker Desktop abierto.
2. Contenedor arriba:
   ```powershell
   docker start todo-postgres
   docker ps
   ```
3. Desde la carpeta `backend`:
   ```powershell
   mvn spring-boot:run
   ```
   Esperar `Started TodoApiApplication`. **Esa terminal queda ocupada**: no escribas ahí ni presiones `Ctrl + C` (eso apaga la app).
4. Abrir una **segunda terminal** para las pruebas.
5. Detener la app: `Ctrl + C` en la primera terminal (si pregunta `Terminate batch job (Y/N)?`, responder `y`).

### Pruebas con curl (PowerShell)

En PowerShell hay que escribir **`curl.exe`** (con `.exe`). `curl` a secas es un alias de `Invoke-WebRequest` y se comporta distinto.

El JSON de POST/PUT va en una variable con **comillas simples** y se envía por la tubería con `-d "@-"`. Escapar comillas con `\"` en PowerShell 5.1 rompe el JSON cuando hay espacios y la API responde 400.

**1. Crear** (esperado `201`)
```powershell
$json = '{"title":"Terminar laboratorio DOSW","description":"Completar pruebas del backend","priority":"HIGH","dueDate":"2026-09-25"}'
$json | curl.exe -i -X POST http://localhost:8080/api/v1/tasks -H "Content-Type: application/json" -d "@-"
```

**2. Listar** (esperado `200`)
```powershell
curl.exe -i http://localhost:8080/api/v1/tasks
```

**3. Consultar una** (esperado `200`)
```powershell
curl.exe -i http://localhost:8080/api/v1/tasks/1
```

**4. Actualizar** (esperado `200`)
```powershell
$json = '{"title":"Terminar laboratorio DOSW","description":"Backend y Front-end terminados","status":"IN_PROGRESS","priority":"HIGH","dueDate":"2026-09-25"}'
$json | curl.exe -i -X PUT http://localhost:8080/api/v1/tasks/1 -H "Content-Type: application/json" -d "@-"
```

**5. Eliminar** (esperado `204`)
```powershell
curl.exe -i -X DELETE http://localhost:8080/api/v1/tasks/1
```

**6. Id inexistente** (esperado `404`)
```powershell
curl.exe -i http://localhost:8080/api/v1/tasks/99999
```
Respuesta: `{"status":404,"message":"Task with id 99999 was not found"}`

**7. Validación** (esperado `400`)
```powershell
$json = '{"title":"","description":"sin titulo"}'
$json | curl.exe -i -X POST http://localhost:8080/api/v1/tasks -H "Content-Type: application/json" -d "@-"
```

> El `id` lo asigna PostgreSQL y **no se reutiliza**. Si borraste tareas antes, la nueva no tendrá `id: 1`. Usa el `id` que devuelva el POST en las URLs.

### Alternativa nativa de PowerShell (también funciona)

```powershell
$body = @{
    title       = "Terminar laboratorio DOSW"
    description = "Completar pruebas del backend"
    priority    = "HIGH"
    dueDate     = "2026-09-25"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/tasks -ContentType "application/json" -Body $body
Invoke-RestMethod -Uri http://localhost:8080/api/v1/tasks
Invoke-RestMethod -Method Delete -Uri http://localhost:8080/api/v1/tasks/1
```

`Invoke-RestMethod` no muestra el código HTTP. Para verlo: `(Invoke-WebRequest -UseBasicParsing -Method Delete -Uri http://localhost:8080/api/v1/tasks/1).StatusCode`.

### Ejemplos JSON

Petición POST:
```json
{
  "title": "Terminar laboratorio DOSW",
  "description": "Completar pruebas del backend",
  "priority": "HIGH",
  "dueDate": "2026-09-25"
}
```

Respuesta `201 Created`:
```json
{
  "id": 1,
  "title": "Terminar laboratorio DOSW",
  "description": "Completar pruebas del backend",
  "status": "PENDING",
  "priority": "HIGH",
  "dueDate": "2026-09-25",
  "createdAt": "2026-09-21T10:30:00"
}
```

### Clientes gráficos

Postman, Bruno o Insomnia: crear una petición por operación (método + URL + body JSON). Ejemplo de instalación de Bruno: `winget install -e --id Bruno.Bruno`.

---

## 16. Comandos principales (chuleta)

### Maven

| Comando | Qué hace |
|---|---|
| `mvn clean compile` | compila (ideal para saber si el error es del código o del editor) |
| `mvn test` | ejecuta las pruebas |
| `mvn clean verify` | pruebas + reporte JaCoCo |
| `mvn spring-boot:run` | arranca la API |
| `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` | arranca en otro puerto sin tocar archivos |
| `mvn clean package` | genera el `.jar` en `target/` |
| `mvn clean package -DskipTests` | igual, sin pruebas |

### Docker

| Comando | Qué hace |
|---|---|
| `docker pull postgres:17-alpine` | descarga la imagen |
| `docker images` | lista imágenes |
| `docker run --name todo-postgres -e POSTGRES_DB=todo_db -e POSTGRES_USER=todo_user -e POSTGRES_PASSWORD=todo_password -p 5432:5432 -d postgres:17-alpine` | crea y arranca el contenedor |
| `docker ps` | contenedores corriendo |
| `docker ps -a` | todos los contenedores |
| `docker start todo-postgres` | arranca el existente |
| `docker stop todo-postgres` | lo detiene |
| `docker logs todo-postgres` | ver logs |
| `docker rm -f todo-postgres` | borra el contenedor (y sus datos) |
| `docker exec -it todo-postgres psql -U todo_user -d todo_db` | entra a psql |

### psql

`\dt` (tablas), `\d tasks` (estructura), `SELECT * FROM tasks;`, `\q` (salir).

### Puertos en Windows

| Comando | Qué hace |
|---|---|
| `netstat -ano \| findstr :8080` | ver quién usa el puerto 8080 (última columna = PID) |
| `tasklist /FI "PID eq <PID>"` | ver qué programa es |
| `taskkill /PID <PID> /F` | matarlo (si da "Access denied", abrir PowerShell como administrador) |

### Git

El `.gitignore` combinado del proyecto está en la raíz. Ignora `target/`, `.idea/`, `.vscode/`, `*.class`, `*.log`, etc.

---

## 17. Errores comunes y soluciones

| Síntoma | Causa | Solución |
|---|---|---|
| `Port 8080 was already in use` | otro programa usa el 8080 (en este equipo fue el servicio `NI Application Web Server` de National Instruments, PID con "Access denied") | usar otro puerto (`server.port=8081`), o detener el servicio como administrador: `Stop-Service -Name NIApplicationWebServer` |
| `docker` no se reconoce | Docker no instalado o terminal abierta antes de instalar | instalar Docker Desktop y abrir una PowerShell nueva |
| `error during connect ... dockerDesktopLinuxEngine` | Docker Desktop cerrado | abrirlo y esperar "Engine running" |
| `Connection refused` / no conecta a la BD | contenedor apagado | `docker start todo-postgres` y `docker ps` |
| `password authentication failed` | credenciales distintas entre `application.properties` y el contenedor | igualarlas; si hay que recrear: `docker rm -f todo-postgres` y volver a ejecutar `docker run` |
| `missing table [tasks]` o `Schema-validation` | no se creó la tabla (con `validate` Hibernate no la crea) | ejecutar el `CREATE TABLE` (sección 4) |
| La app no arranca y solo hay `spring.application.name` y `ddl-auto` en properties | falta la configuración de `spring.datasource.*` | completar las propiedades (sección 5) |
| Imports en rojo en VS Code pero Maven compila | el servidor de Java del editor está desincronizado | `Ctrl + Shift + P` -> **Java: Clean Java Language Server Workspace** -> Restart and delete. Confirmar antes con `mvn clean compile` |
| `Unable to connect to the remote server` al probar | la app está apagada (se hizo `Ctrl + C`) | arrancarla en la terminal 1 y probar desde la terminal 2 |
| POST devuelve 400 con curl y errores `Could not resolve host` | comillas mal escapadas en PowerShell | JSON en variable con comillas simples + `-d "@-"` (sección 15) |
| `curl` muestra aviso "Security Warning: Script Execution Risk" | `curl` en PowerShell es `Invoke-WebRequest` | usar `curl.exe`, o `-UseBasicParsing` |
| Lanzar la excepción da 500 en vez de 404 | no hay `@RestControllerAdvice` que la traduzca | crear `GlobalExceptionHandler` (sección 9) |
| El `INSERT` falla con `status`/`priority` nulos | la entidad no inicializa los defaults | valores iniciales en la entidad y/o asignarlos en el service |
| Error al guardar un enum (violación de CHECK) | falta `@Enumerated(EnumType.STRING)` | agregarla |
| `@WebMvcTest` o `@MockitoBean` no se encuentran | imports de Boot 3 | usar los de Boot 4 (sección 13) y el starter `spring-boot-starter-webmvc-test` |
| `ObjectMapper` no se inyecta en el test | se importó `com.fasterxml...` | usar `tools.jackson.databind.ObjectMapper` |
| Reporte JaCoCo no aparece | se ejecutó `mvn test` | usar `mvn clean verify` |
| Validaciones no funcionan | falta `@Valid` en el controller o falta la dependencia `spring-boot-starter-validation` | agregar ambos |

---

## 18. Checklist para el parcial (microservicio nuevo)

Orden recomendado. Cada paso se prueba antes del siguiente.

1. **Proyecto**: Initializr con Web, Data JPA, PostgreSQL Driver, Validation. Paquete base correcto.
2. **BD**: `docker run` de PostgreSQL con nombre de BD/usuario/clave del enunciado; `docker ps` para verificar.
3. **Tabla**: escribir el `CREATE TABLE` (con `BIGSERIAL`, `NOT NULL`, `DEFAULT`, `CHECK`) y ejecutarlo con `docker exec -it ... psql`.
4. **`application.properties`**: datasource + `ddl-auto=validate` (+ `server.port` si hace falta).
5. **Enums** (si hay estados/tipos).
6. **Entidad**: `@Entity`, `@Table(name=...)`, `@Id`, `@GeneratedValue(strategy = IDENTITY)`, `@Column(name=..., nullable=..., length=...)`, `@Enumerated(EnumType.STRING)`, defaults en Java y `@PrePersist` para fechas de creación.
7. **Repository**: `interface XRepository extends JpaRepository<XEntity, Long>`.
8. **DTOs** (`record`): `CreateRequest` (sin id ni campos generados), `UpdateRequest`, `Response` (con `static from(entity)`), con validaciones `@NotBlank`, `@NotNull`, `@Size`.
9. **Excepción**: `XNotFoundException extends RuntimeException`.
10. **`GlobalExceptionHandler`**: `@RestControllerAdvice` + `@ExceptionHandler` -> 404 con `{status, message}`.
11. **Service**: interfaz + `XServiceImpl` con `@Service`, constructor, defaults, `orElseThrow`, `existsById`.
12. **Controller**: `@RestController`, `@RequestMapping("/api/v1/...")`, `@Valid @RequestBody`, códigos 200/201/204.
13. **Compilar y arrancar**: `mvn clean compile`, luego `mvn spring-boot:run`.
14. **Probar** los 6 casos con curl: crear (201), listar (200), consultar (200), actualizar (200), eliminar (204), id inexistente (404), y un body inválido (400).
15. **Pruebas unitarias**: `ServiceTest` (Mockito) y `ControllerTest` (MockMvc), con casos de éxito y de error.
16. **JaCoCo**: plugin + `mvn clean verify` + revisar `target/site/jacoco/index.html`.

### Cómo adaptar este código a otra entidad (ej. `Order`, `Product`, `Student`)

Reemplazar de forma consistente:

| En este proyecto | En el parcial |
|---|---|
| `Task` / `tasks` | nombre de la nueva entidad / tabla |
| `edu.eci.dosw.todo` | paquete base del enunciado |
| `/api/v1/tasks` | endpoint pedido |
| campos `title`, `description`, `status`, `priority`, `dueDate`, `createdAt` | los campos del enunciado |
| `TaskStatus`, `TaskPriority` | los enums del enunciado (o ninguno) |

Si el enunciado agrega **búsquedas por filtro**, agregar el método derivado en el repository (`findByStatus(...)`), un método en la interfaz del service y un `@GetMapping` con `@RequestParam` en el controller.

Si el enunciado pide **otro tipo de error** (por ejemplo 409 o 400 por regla de negocio): crear la excepción y agregar un `@ExceptionHandler` más en `GlobalExceptionHandler`.

### Regla de oro de las pruebas

- Prueba de **service**: mock del repository, sin Spring.
- Prueba de **controller**: `@WebMvcTest` + `@MockitoBean` del service, sin BD.
- Por cada operación: un caso de éxito y un caso de error (404 / 400).
---
---
---
---
---
---
---
---
---
---
# Para qué sirve cada cosa del Back-end

Este documento complementa al manual paso a paso (`README.md`). Aquí no hay pasos: explica **qué es y para qué sirve** cada herramienta, cada archivo, cada anotación y cada comando que usamos.

---

## 1. Visión general: cómo viaja una petición

Cuando alguien hace `POST /api/v1/tasks`, esto es lo que pasa:

```
Cliente (curl / Postman / Front-end)
        │  JSON
        ▼
  TaskController      recibe la petición HTTP, valida el JSON y elige el código de respuesta
        │  TaskCreateRequest (DTO)
        ▼
  TaskServiceImpl     aplica la lógica: valores por defecto, buscar, actualizar, lanzar errores
        │  TaskEntity (Entity)
        ▼
  TaskRepository      habla con la base de datos (guarda, busca, borra)
        │  SQL
        ▼
  PostgreSQL (Docker) guarda los datos en la tabla "tasks"
```

La respuesta hace el camino inverso: Entity -> `TaskResponse` (DTO) -> JSON.

**Por qué en capas:** cada clase tiene una sola responsabilidad. Si cambias la base de datos solo tocas el repository; si cambias el formato del JSON solo tocas los DTOs y el controller. También permite probar cada capa por separado.

---

## 2. Herramientas

### Java y Maven

| Herramienta | Para qué sirve |
|---|---|
| **Java** | El lenguaje en el que está escrito todo. Se compila con `release 17`, aunque el equipo tenga una versión más nueva. |
| **Maven** | Herramienta de construcción. Descarga las librerías que necesitas, compila el código, ejecuta las pruebas y empaqueta la aplicación. Todo lo configuras en el `pom.xml`. |
| **`pom.xml`** | El archivo de configuración de Maven. Dice: qué librerías usa el proyecto (dependencias), qué plugins se ejecutan (JaCoCo, Spring Boot) y la versión de Java. |

### Spring Boot y su ecosistema

| Herramienta | Para qué sirve |
|---|---|
| **Spring Boot** | Framework que arma la aplicación casi sin configuración. Levanta un servidor web, crea los objetos (beans) y los conecta entre sí. Es lo que hace que `mvn spring-boot:run` arranque la API. |
| **Spring Web (MVC)** | La parte de Spring que permite crear una API REST: recibir peticiones HTTP y devolver JSON. Aquí vienen `@RestController`, `@GetMapping`, etc. |
| **Tomcat** | El servidor web que viene embebido dentro de la aplicación. Es quien escucha en el puerto 8080. No instalas nada aparte. |
| **Spring Data JPA** | Permite hablar con la base de datos sin escribir SQL. Tú defines una interfaz y Spring crea la implementación. |
| **JPA** | Es la *especificación* estándar de Java para mapear clases a tablas (ORM). Define anotaciones como `@Entity` y `@Id`. |
| **Hibernate** | Es la *implementación* de JPA que Spring usa por debajo. Es quien realmente genera el SQL. |
| **PostgreSQL Driver** | La librería (`org.postgresql:postgresql`) que sabe comunicarse con PostgreSQL. Sin ella, Java no puede conectarse a la base de datos. |
| **HikariCP** | Administra un "pool" de conexiones a la BD (las reutiliza en vez de abrir una nueva en cada consulta). Viene incluido; lo ves en el log como `HikariPool-1`. |
| **Bean Validation (`spring-boot-starter-validation`)** | Librería para validar datos con anotaciones (`@NotBlank`, `@Size`, `@NotNull`). |
| **Jackson** | Convierte objetos Java a JSON y JSON a objetos Java automáticamente. En Spring Boot 4 es Jackson 3 (paquete `tools.jackson`). |

### Base de datos y contenedores

| Herramienta | Para qué sirve |
|---|---|
| **PostgreSQL** | La base de datos relacional donde se guardan las tareas. |
| **Docker** | Programa que ejecuta aplicaciones dentro de contenedores aislados. Aquí lo usamos para **no instalar PostgreSQL en Windows**: corre dentro de un contenedor. |
| **Docker Desktop** | La aplicación de Windows que instala y administra Docker. Debe estar abierta para usar `docker`. |
| **Imagen (`postgres:17-alpine`)** | La "plantilla" descargable con PostgreSQL ya listo. `alpine` indica que es la versión liviana. |
| **Contenedor (`todo-postgres`)** | Una instancia en ejecución creada a partir de la imagen. Es la base de datos real a la que se conecta tu API. |
| **`psql`** | Cliente de línea de comandos de PostgreSQL. Lo usas con `docker exec` para entrar a la BD y crear la tabla o consultar datos. |

### Pruebas

| Herramienta | Para qué sirve |
|---|---|
| **JUnit 5** | El framework base para escribir y ejecutar pruebas. Aporta `@Test` y el ciclo de ejecución. |
| **Mockito** | Crea objetos falsos ("mocks") para aislar lo que pruebas. Por ejemplo, un `TaskRepository` falso para probar el service sin base de datos. |
| **AssertJ** | Librería de aserciones legibles: `assertThat(result).hasSize(2)`. Compara el resultado real con el esperado. |
| **MockMvc** | Simula peticiones HTTP contra el controller sin levantar un servidor real. |
| **`spring-boot-starter-test`** | Paquete que trae JUnit, Mockito, AssertJ y las utilidades de pruebas de Spring ya incluidos. |
| **JaCoCo** | Mide la **cobertura**: qué porcentaje de tu código se ejecutó durante las pruebas. Genera un reporte HTML que muestra línea por línea qué quedó sin probar. La meta sugerida es 80 %. |

### Herramientas para probar la API a mano

| Herramienta | Para qué sirve |
|---|---|
| **curl / `curl.exe`** | Envía peticiones HTTP desde la terminal. En PowerShell se escribe `curl.exe` porque `curl` es un alias de otro comando. |
| **`Invoke-RestMethod` / `Invoke-WebRequest`** | Equivalentes nativos de PowerShell para hacer peticiones HTTP. |
| **Postman / Bruno / Insomnia** | Aplicaciones gráficas para armar y enviar peticiones; más cómodas para capturas de pantalla. |

---

## 3. Archivos del proyecto

### Configuración

| Archivo | Para qué sirve |
|---|---|
| **`pom.xml`** | Dependencias, plugins y versión de Java del proyecto. |
| **`application.properties`** | Configuración de la aplicación: conexión a la BD, modo de Hibernate, puerto. Ver detalle abajo. |
| **`.gitignore`** | Lista de archivos que Git no debe subir: `target/` (lo compilado), `.idea/` y `.vscode/` (configuración de editores), `*.class`, `*.log`, etc. |
| **`TodoApiApplication.java`** | La clase principal. Tiene `@SpringBootApplication` y el `main`. Es el punto de arranque: Spring escanea su paquete y todos los de abajo en busca de componentes. |

### Detalle de `application.properties`

| Línea | Para qué sirve |
|---|---|
| `spring.application.name` | Nombre de la aplicación (aparece en los logs). |
| `spring.datasource.url` | Dirección de la BD: `jdbc:postgresql://localhost:5432/todo_db` (host, puerto, nombre de la BD). |
| `spring.datasource.username` / `password` | Credenciales de la BD. Deben coincidir con las del contenedor. |
| `${DB_URL:valor}` | Usa la variable de entorno si existe; si no, el valor por defecto. Evita subir credenciales reales a GitHub. |
| `spring.jpa.hibernate.ddl-auto=validate` | Hibernate **solo verifica** que la tabla coincida con la entidad; no crea ni modifica nada. |
| `spring.jpa.show-sql` / `format_sql` | Imprime el SQL que genera Hibernate, ordenado, para depurar. |
| `server.port` | Cambia el puerto de la API (por defecto 8080). |

Valores posibles de `ddl-auto`: `none` (no hace nada), `validate` (verifica), `update` (modifica la tabla), `create` (la recrea al arrancar), `create-drop` (la crea y la borra al cerrar). Con `validate` la tabla la controlas tú con SQL.

### Capa `entity/` (modelo de datos)

| Archivo | Para qué sirve |
|---|---|
| **`TaskEntity.java`** | Representa **una fila de la tabla `tasks`**. Cada campo de la clase corresponde a una columna. Es el objeto que Hibernate guarda y lee. |
| **`TaskStatus.java`** | Enum con los estados válidos de una tarea: `PENDING`, `IN_PROGRESS`, `COMPLETED`. Impide valores inventados. |
| **`TaskPriority.java`** | Enum con las prioridades válidas: `LOW`, `MEDIUM`, `HIGH`. |

### Capa `repository/` (acceso a datos)

| Archivo | Para qué sirve |
|---|---|
| **`TaskRepository.java`** | Es la capa que **hace las consultas a la base de datos**. Al extender `JpaRepository<TaskEntity, Long>`, Spring genera sin que escribas código: guardar, buscar por id, listar, borrar, contar. El `Long` es el tipo del `id`. También permite crear consultas con solo nombrar el método (`findByStatus`). |

### Capa `dto/` (objetos de entrada y salida)

Un **DTO** (Data Transfer Object) es la forma que tiene el JSON que entra o sale de la API. Se separan de la entidad para no exponer la estructura interna de la base de datos y para controlar qué puede enviar el cliente.

| Archivo | Para qué sirve |
|---|---|
| **`TaskCreateRequest.java`** | Lo que el cliente envía para **crear**: `title`, `description`, `priority`, `dueDate`. No incluye `id` ni `createdAt` porque los genera el servidor. |
| **`TaskUpdateRequest.java`** | Lo que el cliente envía para **actualizar**: título, descripción, estado, prioridad y fecha. |
| **`TaskResponse.java`** | Lo que la API **devuelve**: todos los campos, incluyendo `id` y `createdAt`. Su método `from(entity)` convierte una `TaskEntity` en respuesta. |

Están escritos como `record`: clases inmutables de Java que ya traen constructor, getters, `equals` y `toString` sin escribirlos.

### Capa `service/` (lógica de negocio)

| Archivo | Para qué sirve |
|---|---|
| **`TaskService.java`** | La **interfaz**: el contrato. Lista qué operaciones existen (`findAll`, `findById`, `create`, `update`, `delete`) sin decir cómo se hacen. El controller depende de ella, no de la implementación. |
| **`TaskServiceImpl.java`** | La **implementación**: aquí está la lógica. Busca en el repository, convierte entre DTO y entidad, asigna valores por defecto (`PENDING`, `MEDIUM`), verifica que la tarea exista y lanza excepciones si no. |

### Capa `controller/` (API REST)

| Archivo | Para qué sirve |
|---|---|
| **`TaskController.java`** | Es la **puerta de entrada HTTP**. Define las URLs (`/api/v1/tasks`), recibe el JSON, activa las validaciones con `@Valid`, llama al service y decide el código de estado (200, 201, 204). No contiene lógica de negocio. |

### Capa `exception/` (manejo de errores)

| Archivo | Para qué sirve |
|---|---|
| **`TaskNotFoundException.java`** | Excepción propia que significa "esa tarea no existe". El service la lanza cuando un id no se encuentra. |
| **`GlobalExceptionHandler.java`** | Atrapa las excepciones lanzadas en cualquier controller y las convierte en una respuesta HTTP ordenada. Sin él, `TaskNotFoundException` produciría un error 500; con él devuelve `404` y un JSON `{"status":404,"message":"..."}`. |

### Pruebas (`src/test/java/...`)

| Archivo | Para qué sirve |
|---|---|
| **`TaskServiceTest.java`** | Prueba la **lógica del service** de forma aislada: usa un repository falso (mock), así que no necesita Spring ni base de datos. Verifica que se aplican los valores por defecto, que se lanzan excepciones, etc. |
| **`TaskControllerTest.java`** | Prueba la **capa web**: que cada endpoint responda el código correcto (200, 201, 204, 400, 404) y el JSON esperado. Usa un service falso. |
| **`TodoApiApplicationTests.java`** | Lo genera Spring Initializr. Solo comprueba que el contexto completo de la aplicación **arranca** (por eso necesita la BD encendida). |

### Carpetas generadas

| Carpeta | Para qué sirve |
|---|---|
| **`target/`** | Todo lo que Maven genera: clases compiladas, el `.jar`, reportes de pruebas y de JaCoCo. Se puede borrar con `mvn clean`; por eso está en `.gitignore`. |
| **`target/site/jacoco/index.html`** | El reporte de cobertura. Se abre en el navegador. |
| **`src/main/resources/`** | Archivos de configuración y recursos (aquí, `application.properties`). |

---

## 4. Anotaciones (qué hace cada una)

### En la entidad (JPA)

| Anotación | Qué hace |
|---|---|
| `@Entity` | Marca la clase como una entidad: Hibernate la mapea a una tabla. |
| `@Table(name = "tasks")` | Indica el nombre exacto de la tabla. |
| `@Id` | Marca el campo que es la llave primaria. |
| `@GeneratedValue(strategy = IDENTITY)` | El id lo genera la base de datos (autoincremental, como `BIGSERIAL`). |
| `@Column(name, nullable, length)` | Configura la columna: nombre, si admite nulos, tamaño máximo. |
| `@Enumerated(EnumType.STRING)` | Guarda el enum como texto (`'PENDING'`) y no como número (`0`). |
| `@PrePersist` | Ejecuta un método justo antes del primer `INSERT`. Aquí rellena `createdAt` con la fecha y hora actual. |

### En el repository, service y controller (Spring)

| Anotación | Qué hace |
|---|---|
| `@SpringBootApplication` | Marca la clase principal; activa el escaneo de componentes y la configuración automática. |
| `@Repository` | Marca la clase/interfaz como componente de acceso a datos. |
| `@Service` | Marca la clase como componente de lógica de negocio; Spring crea una instancia y la inyecta donde se necesite. |
| `@RestController` | Marca la clase como controller REST: lo que devuelven sus métodos se convierte en JSON. |
| `@RequestMapping("/api/v1/tasks")` | Prefijo de URL común para todos los endpoints del controller. |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | Asocian un método a un verbo HTTP (y opcionalmente a una subruta como `/{id}`). |
| `@PathVariable` | Toma un valor de la URL (el `{id}`) y lo pasa como parámetro. |
| `@RequestBody` | Convierte el JSON del cuerpo de la petición en un objeto Java (DTO). |
| `@Valid` | Ejecuta las validaciones del DTO antes de entrar al método. Si fallan, responde 400. |
| `@RestControllerAdvice` | Clase que intercepta excepciones de todos los controllers. |
| `@ExceptionHandler(X.class)` | Método que se ejecuta cuando se lanza la excepción `X`. |

### En los DTOs (validaciones)

| Anotación | Qué hace |
|---|---|
| `@NotBlank` | El texto no puede ser nulo, vacío ni solo espacios. |
| `@NotNull` | El valor no puede ser nulo. |
| `@Size(max = 120)` | Limita la longitud del texto. |

### En las pruebas

| Anotación | Qué hace |
|---|---|
| `@Test` | Marca un método como prueba. |
| `@ExtendWith(MockitoExtension.class)` | Activa Mockito dentro de JUnit 5. |
| `@Mock` | Crea un objeto falso (por ejemplo, un `TaskRepository` que no toca la BD). |
| `@InjectMocks` | Crea la clase a probar (`TaskServiceImpl`) e inyecta dentro los mocks. |
| `@WebMvcTest(TaskController.class)` | Levanta solo la capa web (controller y handler), sin base de datos. |
| `@MockitoBean` | Registra un mock dentro del contexto de Spring (reemplaza al antiguo `@MockBean`). |
| `@Autowired` | Pide a Spring que inyecte un objeto ya creado (por ejemplo `MockMvc`). |

---

## 5. Conceptos clave

| Concepto | Explicación |
|---|---|
| **API REST** | Forma de exponer operaciones sobre recursos mediante URLs y verbos HTTP: GET (leer), POST (crear), PUT (actualizar), DELETE (borrar). |
| **CRUD** | Create, Read, Update, Delete: las cuatro operaciones básicas sobre datos. Es lo que implementa esta API. |
| **ORM** | Object-Relational Mapping: técnica para tratar filas de una tabla como objetos Java. JPA/Hibernate lo implementan. |
| **Inyección de dependencias** | En vez de crear tú los objetos con `new`, Spring los crea y los entrega. Aquí se entrega por constructor: `TaskServiceImpl` recibe el `TaskRepository` que necesita. |
| **Bean** | Cualquier objeto creado y administrado por Spring (los marcados con `@Service`, `@Repository`, `@RestController`...). |
| **Mock** | Objeto falso que simula a uno real y permite controlar sus respuestas. Sirve para probar una capa sin depender de las demás. |
| **Prueba unitaria** | Prueba una pieza pequeña y aislada (una clase). Es rápida y no necesita infraestructura. |
| **Cobertura** | Porcentaje del código que las pruebas ejecutan. No garantiza que las pruebas sean buenas, pero muestra qué zonas nunca se probaron. |
| **Valor por defecto** | Lo que se asigna cuando el cliente no lo envía: al crear, `status = PENDING` y `priority = MEDIUM`. |
| **Idempotencia (opcional)** | `PUT`, `GET` y `DELETE` repetidos dejan el sistema en el mismo estado; `POST` repetido crea más registros. |

### Códigos HTTP usados

| Código | Significado | Cuándo aparece |
|---|---|---|
| **200 OK** | Todo bien | listar, consultar, actualizar |
| **201 Created** | Recurso creado | POST exitoso |
| **204 No Content** | Éxito sin cuerpo de respuesta | DELETE exitoso |
| **400 Bad Request** | La petición es inválida | falló una validación (ej. título vacío) o el JSON está mal formado |
| **404 Not Found** | No existe el recurso | consultar, actualizar o borrar un id inexistente |
| **500 Internal Server Error** | Error no controlado del servidor | una excepción que nadie traduce |

### Comandos y qué hacen

| Comando | Para qué sirve |
|---|---|
| `mvn clean compile` | Borra lo anterior y compila. Sirve para saber si el código está bien, sin correr nada. |
| `mvn test` | Compila y ejecuta todas las pruebas. |
| `mvn clean verify` | Ejecuta las pruebas y genera el reporte de JaCoCo. |
| `mvn spring-boot:run` | Arranca la API. |
| `docker pull` | Descarga una imagen. |
| `docker run` | Crea y arranca un contenedor nuevo desde una imagen (solo la primera vez). |
| `docker start` / `stop` | Enciende y apaga un contenedor ya creado. |
| `docker ps` | Muestra los contenedores que están corriendo. |
| `docker exec -it <contenedor> <comando>` | Ejecuta un comando dentro del contenedor (por ejemplo, abrir `psql`). |
| `docker rm -f` | Borra el contenedor y sus datos. |
| `netstat -ano \| findstr :8080` | Muestra qué proceso usa el puerto 8080. |
| `taskkill /PID <n> /F` | Fuerza el cierre de un proceso. |

---

## 6. Resumen en una frase por pieza

- **Entity**: la fila de la tabla convertida en objeto Java.
- **Enum**: la lista cerrada de valores permitidos.
- **Repository**: quien habla con la base de datos.
- **DTO**: la forma del JSON que entra y sale.
- **Service**: donde vive la lógica del negocio.
- **Controller**: la puerta de entrada HTTP.
- **Exception + Handler**: el error propio y quien lo convierte en un 404 ordenado.
- **`application.properties`**: cómo se conecta la app a la base de datos.
- **`pom.xml`**: qué librerías y plugins usa el proyecto.
- **Docker**: ejecuta PostgreSQL sin instalarlo en Windows.
- **JUnit / Mockito / AssertJ / MockMvc**: escriben y ejecutan las pruebas.
- **JaCoCo**: mide cuánto del código cubren las pruebas.
- **Maven**: compila, prueba y ejecuta todo.
