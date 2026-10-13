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