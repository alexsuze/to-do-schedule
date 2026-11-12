package com.todoschedule.service;

import com.todoschedule.config.AppProperties;
import com.todoschedule.model.Settings;
import com.todoschedule.model.Task;
import com.todoschedule.model.TimeBlock;
import com.todoschedule.model.User;
import com.todoschedule.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.todoschedule.service.TimeBlockService.ZONE;

/**
 * Email 每日摘要（需求書第 5 節、9.6、12.7）：依公司分段列出今天到期、已逾期、今天的時段、本週累計工時。
 * 所有公司的前三項都沒有就不寄。
 * 每分鐘檢查一次每位可用摘要的使用者，過了各自設定的時間且今天還沒處理過就處理（伺服器晚啟動也會補寄）。
 * 收件人為帳號的 Google Email。
 */
@Service
public class DigestService {

    private static final Logger log = LoggerFactory.getLogger(DigestService.class);
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MD_HM = DateTimeFormatter.ofPattern("M/dd HH:mm");
    private static final String WEEKDAYS = "一二三四五六日";

    private final SettingsService settings;
    private final UserService users;
    private final TaskRepository taskRepo;
    private final TimeBlockService blocks;
    private final CompanyService companies;
    private final EmailService email;
    private final AppProperties props;

    public DigestService(SettingsService settings, UserService users, TaskRepository taskRepo, TimeBlockService blocks,
                         CompanyService companies, EmailService email, AppProperties props) {
        this.settings = settings;
        this.users = users;
        this.taskRepo = taskRepo;
        this.blocks = blocks;
        this.companies = companies;
        this.email = email;
        this.props = props;
    }

    /**
     * 一家公司的摘要（需求書 12.7）。line = 已排好的一行文字（時間＋標題）。
     * weekMinutes = 本週已過去天數（週一～昨天）的工時；只是附加資訊，不影響要不要寄。
     */
    record Section(String company, List<String> dueToday, List<String> overdue, List<String> schedule, long weekMinutes) {
        boolean hasItems() {
            return !dueToday.isEmpty() || !overdue.isEmpty() || !schedule.isEmpty();
        }
    }

    /** sections 依公司排序；沒有任何內容（含本週工時）的公司不列。 */
    record Digest(ZonedDateTime day, List<Section> sections) {
        boolean isEmpty() {
            return sections.stream().noneMatch(Section::hasItems);
        }

        int count(Function<Section, List<String>> f) {
            return sections.stream().mapToInt(x -> f.apply(x).size()).sum();
        }
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Taipei")
    public void tick() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        for (User u : users.active()) {
            if (!settings.digestAvailable(u)) continue;
            try {
                run(u, now);
            } catch (RuntimeException e) {
                log.warn("使用者 {} 的每日摘要處理失敗", u.id, e); // 一個人出錯不影響其他人
            }
        }
    }

    void run(User u, ZonedDateTime now) {
        Settings s = settings.get(u.id);
        String today = now.toLocalDate().toString();
        if (!s.digestEnabled || u.email == null || u.email.isBlank() || today.equals(s.lastDigestDate)) return;
        if (now.toLocalTime().isBefore(LocalTime.parse(s.digestTime))) return;

        Digest d = build(u.id, now.toInstant());
        String result;
        if (d.isEmpty()) {
            result = "沒有到期、逾期或排程，未寄送";
        } else {
            try {
                email.send(u.email, subject(d), html(d), text(d));
                result = "已寄出";
            } catch (IllegalStateException e) {
                // ponytail: 失敗不重試（避免每分鐘重寄），結果顯示在設定頁
                log.warn("每日摘要寄送失敗", e);
                result = "寄送失敗：" + e.getMessage();
            }
        }
        s.lastDigestDate = today;
        s.lastDigestResult = now.format(MD_HM) + " " + result;
        settings.save(s);
    }

    Digest build(String userId, Instant now) {
        ZonedDateTime day = now.atZone(ZONE).toLocalDate().atStartOfDay(ZONE);
        List<Task> tasks = taskRepo.findByUserIdAndDeletedAtIsNull(userId);
        Map<String, Task> byId = tasks.stream().collect(Collectors.toMap(t -> t.id, Function.identity()));
        List<TimeBlock> all = blocks.list(userId).stream().filter(b -> byId.containsKey(b.taskId)).toList();

        // 依公司排序；找不到公司的（理論上不會有）放最後
        var list = companies.list(userId);
        List<String> order = new ArrayList<>(list.stream().map(c -> c.id).toList());
        Map<String, String> names = new HashMap<>();
        list.forEach(c -> names.put(c.id, c.name));
        tasks.stream().map(t -> t.companyId).filter(c -> !order.contains(c)).distinct().forEach(order::add);

        List<Section> sections = new ArrayList<>();
        for (String c : order) {
            List<Task> mine = tasks.stream().filter(t -> Objects.equals(t.companyId, c)).toList();
            List<TimeBlock> myBlocks = all.stream().filter(b -> Objects.equals(byId.get(b.taskId).companyId, c)).toList();
            Section sec = section(names.getOrDefault(c, "未指定公司"), mine, myBlocks, byId, day, now);
            if (sec.hasItems() || sec.weekMinutes() > 0) sections.add(sec);
        }
        return new Digest(day, sections);
    }

    private static Section section(String company, List<Task> tasks, List<TimeBlock> all, Map<String, Task> byId,
                                   ZonedDateTime day, Instant now) {
        Instant start = day.toInstant();
        Instant end = day.plusDays(1).toInstant();
        List<Task> open = tasks.stream()
                .filter(t -> !t.done && t.deadline != null)
                .sorted(Comparator.comparing((Task t) -> t.deadline))
                .toList();
        List<String> dueToday = open.stream()
                .filter(t -> !t.deadline.isBefore(now) && t.deadline.isBefore(end))
                .map(t -> fmt(t.deadline, HM) + " " + t.title).toList();
        List<String> overdue = open.stream()
                .filter(t -> t.deadline.isBefore(now))
                .map(t -> fmt(t.deadline, MD_HM) + " " + t.title).toList();
        List<String> schedule = all.stream()
                .filter(b -> !b.startAt.isBefore(start) && b.startAt.isBefore(end))
                .sorted(Comparator.comparing((TimeBlock b) -> b.startAt))
                .map(b -> {
                    Task t = byId.get(b.taskId);
                    return fmt(b.startAt, HM) + "–" + fmt(b.endAt, HM) + " " + t.title + (t.done ? "（已完成）" : "");
                }).toList();

        // 只算本週已經過去的天數（週一～昨天），今天和之後的不算
        Instant weekStart = weekStart(day).toInstant();
        long weekMinutes = all.stream()
                .filter(b -> !b.startAt.isBefore(weekStart) && b.startAt.isBefore(start))
                .mapToLong(b -> Duration.between(b.startAt, b.endAt).toMinutes())
                .sum();
        return new Section(company, dueToday, overdue, schedule, weekMinutes);
    }

    private static ZonedDateTime weekStart(ZonedDateTime day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** 本週累計工時那一行；沒有工時（含週一）回傳 null（不列）。例：週三寄 → 本週累計工時 5.5h（10/26–10/27） */
    String weekLine(Section s, ZonedDateTime day) {
        if (s.weekMinutes() == 0) return null;
        String hours = BigDecimal.valueOf(s.weekMinutes()).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
        return "本週累計工時 %sh（%s–%s）".formatted(hours, md(weekStart(day)), md(day.minusDays(1)));
    }

    private static String md(ZonedDateTime d) {
        return d.getMonthValue() + "/" + "%02d".formatted(d.getDayOfMonth());
    }

    /** 主旨的數字為所有公司合計。 */
    String subject(Digest d) {
        return "[每日摘要] %s 今日到期 %d 項、逾期 %d 項、排程 %d 段"
                .formatted(dayLabel(d), d.count(Section::dueToday), d.count(Section::overdue), d.count(Section::schedule));
    }

    String html(Digest d) {
        StringBuilder sb = new StringBuilder("<div style=\"font-family:sans-serif;line-height:1.6\">");
        sb.append("<h2 style=\"margin:0 0 12px\">").append(dayLabel(d)).append(" 每日摘要</h2>");
        for (Section s : d.sections()) {
            sb.append("<h3 style=\"margin:20px 0 4px;padding:4px 10px;background:#FFF1DC;border-radius:8px\">")
                    .append(HtmlUtils.htmlEscape(s.company())).append("</h3>");
            String week = weekLine(s, d.day());
            if (week != null) sb.append("<p style=\"margin:4px 0;font-weight:bold\">").append(week).append("</p>");
            list(sb, "今天到期", s.dueToday());
            list(sb, "已逾期", s.overdue());
            list(sb, "今天的時程", s.schedule());
        }
        sb.append("<p><a href=\"").append(HtmlUtils.htmlEscape(props.frontendUrl())).append("\">打開工作事項管理</a></p></div>");
        return sb.toString();
    }

    private static void list(StringBuilder sb, String title, List<String> lines) {
        if (lines.isEmpty()) return;
        sb.append("<h4 style=\"margin:12px 0 4px\">").append(title).append("（").append(lines.size()).append("）</h4><ul>");
        lines.forEach(l -> sb.append("<li>").append(HtmlUtils.htmlEscape(l)).append("</li>"));
        sb.append("</ul>");
    }

    String text(Digest d) {
        StringBuilder sb = new StringBuilder(dayLabel(d)).append(" 每日摘要\n");
        for (Section s : d.sections()) {
            sb.append("\n■ ").append(s.company()).append("\n");
            String week = weekLine(s, d.day());
            if (week != null) sb.append(week).append("\n");
            for (var e : List.of(Map.entry("今天到期", s.dueToday()), Map.entry("已逾期", s.overdue()), Map.entry("今天的時程", s.schedule()))) {
                if (e.getValue().isEmpty()) continue;
                sb.append(e.getKey()).append("（").append(e.getValue().size()).append("）\n");
                e.getValue().forEach(l -> sb.append("・").append(l).append("\n"));
            }
        }
        return sb.append("\n").append(props.frontendUrl()).toString();
    }

    private static String dayLabel(Digest d) {
        return md(d.day()) + "（" + WEEKDAYS.charAt(d.day().getDayOfWeek().getValue() - 1) + "）";
    }

    private static String fmt(Instant t, DateTimeFormatter f) {
        return t.atZone(ZONE).format(f);
    }
}
