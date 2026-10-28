package com.todoschedule.service;

import com.todoschedule.model.Task;
import com.todoschedule.model.TimeBlock;
import com.todoschedule.repository.TaskRepository;
import com.todoschedule.repository.TimeBlockRepository;
import com.todoschedule.service.TimeBlockService.BlockRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 時段規則：30 分鐘、不跨日、不重疊、回收桶任務不佔用、還原時丟掉被佔用的時段。不需要 MongoDB。 */
class TimeBlockServiceTest {

    private final TimeBlockRepository repo = mock(TimeBlockRepository.class);
    private final TaskRepository taskRepo = mock(TaskRepository.class);
    private final TimeBlockService service = new TimeBlockService(repo, taskRepo);
    private final List<TimeBlock> blocks = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();

    TimeBlockServiceTest() {
        when(repo.findAll()).thenAnswer(inv -> new ArrayList<>(blocks));
        when(repo.save(any(TimeBlock.class))).thenAnswer(inv -> {
            TimeBlock b = inv.getArgument(0);
            if (b.id == null) { b.id = "b" + (blocks.size() + 1); blocks.add(b); }
            return b;
        });
        when(repo.findById(anyString())).thenAnswer(inv ->
                blocks.stream().filter(b -> b.id.equals(inv.getArgument(0))).findFirst());
        when(repo.findByTaskId(anyString())).thenAnswer(inv ->
                blocks.stream().filter(b -> b.taskId.equals(inv.getArgument(0))).toList());
        doAnswer(inv -> { blocks.removeAll((Collection<?>) inv.getArgument(0)); return null; }).when(repo).deleteAll(any());
        when(taskRepo.findById(anyString())).thenAnswer(inv ->
                tasks.stream().filter(t -> t.id.equals(inv.getArgument(0))).findFirst());
        when(taskRepo.findByDeletedAtIsNotNullOrderByDeletedAtDesc()).thenAnswer(inv ->
                tasks.stream().filter(t -> t.deletedAt != null).toList());
        tasks.add(task("A"));
        tasks.add(task("B"));
    }

    private static Task task(String id) {
        Task t = new Task();
        t.id = id;
        return t;
    }

    /** 台北時間 2026-10-28 的 hh:mm */
    private static Instant at(int h, int m) {
        return LocalDateTime.of(2026, 10, 28, 0, 0).plusHours(h).plusMinutes(m).atZone(TimeBlockService.ZONE).toInstant();
    }

    private TimeBlock create(String taskId, Instant s, Instant e) {
        return service.create(new BlockRequest(taskId, s, e));
    }

    @Test
    void rules() {
        TimeBlock a = create("A", at(9, 0), at(10, 30));
        assertThrows(ResponseStatusException.class, () -> create("B", at(10, 0), at(11, 0)), "重疊");
        assertThrows(ResponseStatusException.class, () -> create("B", at(11, 15), at(12, 0)), "沒對齊 30 分鐘");
        assertThrows(ResponseStatusException.class, () -> create("B", at(23, 30), at(24, 30)), "跨日");
        assertThrows(ResponseStatusException.class, () -> create("B", at(12, 0), at(12, 0)), "長度 0");

        create("B", at(10, 30), at(11, 0)); // 緊接著可以
        create("B", at(23, 30), at(24, 0)); // 結束在午夜可以
        service.update(a.id, new BlockRequest(null, at(8, 0), at(10, 30))); // 自己延展不算重疊
        assertEquals(at(8, 0), a.startAt);
    }

    @Test
    void trashedTaskFreesTimeAndRestoreDropsConflicts() {
        create("A", at(9, 0), at(10, 0));
        create("A", at(14, 0), at(15, 0));
        tasks.get(0).deletedAt = Instant.now();

        assertEquals(0, service.list().size());
        create("B", at(9, 0), at(9, 30)); // A 在回收桶，這段時間可以用

        tasks.get(0).deletedAt = null;
        assertEquals(1, service.dropConflicts("A"));
        assertEquals(2, service.list().size());
    }
}
