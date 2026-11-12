package com.todoschedule.service;

import com.todoschedule.model.Company;
import com.todoschedule.repository.CompanyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 公司管理（需求書 12.3）：新增、改名、改色、排序、封存；不做刪除。只能操作自己的公司。
 */
@Service
public class CompanyService {

    static final String DEFAULT_NAME = "我的公司";
    static final String DEFAULT_COLOR = "#FFC49E";
    private static final Pattern HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final CompanyRepository repo;

    public CompanyService(CompanyRepository repo) {
        this.repo = repo;
    }

    /** archived 為 null 時不變更封存狀態。 */
    public record CompanyRequest(String name, String color, Boolean archived) {}

    /** 一家都沒有時自動建立「我的公司」（新使用者、舊資料搬移）。 */
    public List<Company> list(String userId) {
        List<Company> all = repo.findByUserIdOrderByOrderAsc(userId);
        if (!all.isEmpty()) return all;
        Company c = new Company();
        c.userId = userId;
        c.name = DEFAULT_NAME;
        c.color = DEFAULT_COLOR;
        repo.save(c);
        return List.of(c);
    }

    /** 排序第一家未封存的公司；舊資料沒有公司時歸到這裡。 */
    public Company primary(String userId) {
        List<Company> all = list(userId);
        return all.stream().filter(c -> !c.archived).findFirst().orElse(all.get(0));
    }

    public Company create(String userId, CompanyRequest req) {
        Company c = new Company();
        c.userId = userId;
        apply(c, req);
        c.order = list(userId).stream().mapToInt(x -> x.order + 1).max().orElse(0);
        return repo.save(c);
    }

    public Company update(String userId, String id, CompanyRequest req) {
        Company c = get(userId, id);
        apply(c, req);
        if (Boolean.TRUE.equals(req.archived()) && !c.archived) {
            boolean othersActive = list(userId).stream().anyMatch(x -> !x.id.equals(c.id) && !x.archived);
            if (!othersActive) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少要保留一家未封存的公司");
        }
        if (req.archived() != null) c.archived = req.archived();
        return repo.save(c);
    }

    /** ids 為新的完整順序。 */
    public List<Company> reorder(String userId, List<String> ids) {
        List<Company> all = list(userId);
        if (ids == null || ids.size() != all.size() || !ids.containsAll(all.stream().map(c -> c.id).toList())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "公司順序與現有公司不符，請重新整理");
        }
        all.forEach(c -> c.order = ids.indexOf(c.id));
        repo.saveAll(all);
        return repo.findByUserIdOrderByOrderAsc(userId);
    }

    /** 別人的公司一律當作找不到。 */
    public Company get(String userId, String id) {
        return repo.findById(id == null ? "" : id).filter(c -> userId.equals(c.userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "公司不存在，請重新選擇"));
    }

    private static void apply(Company c, CompanyRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty() || name.length() > 30) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "公司名稱需為 1～30 字");
        if (req.color() == null || !HEX.matcher(req.color()).matches()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "顏色格式錯誤");
        c.name = name;
        c.color = req.color().toUpperCase();
    }
}
