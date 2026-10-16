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