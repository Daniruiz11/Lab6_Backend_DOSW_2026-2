package edu.eci.dosw.todo.exception;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long id) {
        super("No se encontró la tarea con id " + id);
    }
}