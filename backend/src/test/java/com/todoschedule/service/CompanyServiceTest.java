package com.todoschedule.service;

import com.todoschedule.model.Company;
import com.todoschedule.repository.CompanyRepository;
import com.todoschedule.service.CompanyService.CompanyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 公司：沒有時自動建立「我的公司」、驗證、至少留一家未封存、只能動自己的。不需要 MongoDB。 */
class CompanyServiceTest {

    private static final String ME = "u1";
    private static final String OTHER = "u2";

    private final CompanyRepository repo = mock(CompanyRepository.class);
    private final CompanyService service = new CompanyService(repo);
    private final List<Company> cs = new ArrayList<>();

    CompanyServiceTest() {
        when(repo.save(any(Company.class))).thenAnswer(inv -> {
            Company c = inv.getArgument(0);
            if (c.id == null) { c.id = "k" + (cs.size() + 1); cs.add(c); }
            return c;
        });
        when(repo.findById(anyString())).thenAnswer(inv -> cs.stream().filter(c -> c.id.equals(inv.getArgument(0))).findFirst());
        when(repo.findByUserIdOrderByOrderAsc(anyString())).thenAnswer(inv -> cs.stream()
                .filter(c -> c.userId.equals(inv.getArgument(0))).sorted((a, b) -> a.order - b.order).toList());
    }

    @Test
    void defaultCompanyCreatedOnce() {
        List<Company> first = service.list(ME);
        assertEquals(1, first.size());
        assertEquals("我的公司", first.get(0).name);
        assertEquals(1, service.list(ME).size(), "不重複建立");
        assertEquals(first.get(0).id, service.primary(ME).id);
    }

    @Test
    void validateArchiveAndOwnership() {
        Company mine = service.primary(ME);
        assertThrows(ResponseStatusException.class, () -> service.create(ME, new CompanyRequest(" ", "#FFFFFF", null)));
        assertThrows(ResponseStatusException.class, () -> service.create(ME, new CompanyRequest("A", "red", null)));
        assertThrows(ResponseStatusException.class, () -> service.update(ME, mine.id, new CompanyRequest("我的公司", "#FFC49E", true)), "最後一家不能封存");

        Company b = service.create(ME, new CompanyRequest(" B 公司 ", "#8ee3b8", null));
        assertEquals("B 公司", b.name);
        assertEquals("#8EE3B8", b.color);
        assertEquals(1, b.order);
        service.update(ME, mine.id, new CompanyRequest("我的公司", "#FFC49E", true));
        assertTrue(mine.archived);
        assertEquals(b.id, service.primary(ME).id, "封存後第一家改為 B");
        assertThrows(ResponseStatusException.class, () -> service.update(ME, b.id, new CompanyRequest("B 公司", "#8EE3B8", true)));
        assertFalse(b.archived);

        assertThrows(ResponseStatusException.class, () -> service.get(OTHER, b.id), "別人的公司");
        assertThrows(ResponseStatusException.class, () -> service.update(OTHER, b.id, new CompanyRequest("改", "#8EE3B8", null)));
    }

    @Test
    void reorder() {
        Company a = service.primary(ME);
        Company b = service.create(ME, new CompanyRequest("B", "#8EE3B8", null));
        service.reorder(ME, List.of(b.id, a.id));
        assertEquals(0, b.order);
        assertEquals(1, a.order);
        assertThrows(ResponseStatusException.class, () -> service.reorder(ME, List.of(b.id)));
    }
}
