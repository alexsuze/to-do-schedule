package com.todoschedule.service;

import com.todoschedule.config.AppProperties;
import com.todoschedule.model.Settings;
import com.todoschedule.model.Task;
import com.todoschedule.model.TimeBlock;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.todoschedule.service.TimeBlockService.ZONE;

/**
 * Email 每日摘要（需求書第 5 節）：今天到期、已逾期、今天的時段。三項都沒有就不寄。
 * 每分鐘檢查一次，過了設定時間且今天還沒處理過就處理（伺服器晚啟動也會補寄）。
 */
@Service
public class DigestService {

    private static final Logger log = LoggerFactory.getLogger(DigestService.class);
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MD_HM = DateTimeFormatter.ofPattern("M/dd HH:mm");
    private static final String WEEKDAYS = "一二三四五六日";

    private final SettingsService settings;
    private final TaskRepository taskRepo;
    private final TimeBlockService blocks;
    private final EmailService email;
    private final AppProperties props;

    public DigestService(SettingsService settings, TaskRepository taskRepo, TimeBlockService blocks,
                         EmailService email, AppProperties props) {
        this.settings = settings;
        this.taskRepo = taskRepo;
        this.blocks = blocks;
        this.email = email;
        this.props = props;
    }

    /**
     * 摘要內容。line = 已排好的一行文字（時間＋標題）。
     * weekMinutes = 本週已過去天數（週一～昨天）的工時；只是附加資訊，不影響要不要寄。
     */
    record Digest(ZonedDateTime day, List<String> dueToday, List<String> overdue, List<String> schedule, long weekMinutes) {
        boolean isEmpty() {
            return dueToday.isEmpty() && overdue.isEmpty() && schedule.isEmpty();
        }
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Taipei")
    public void tick() {
        run(ZonedDateTime.now(ZONE));
    }

    void run(ZonedDateTime now) {
        Settings s = settings.get();
        String today = now.toLocalDate().toString();
        if (!s.digestEnabled || s.email.isBlank() || today.equals(s.lastDigestDate)) return;
        if (now.toLocalTime().isBefore(LocalTime.parse(s.digestTime))) return;

        Digest d = build(now.toInstant());
        String result;
        if (d.isEmpty()) {
            result = "沒有到期、逾期或排程，未寄送";
        } else {
            try {
                email.send(s.email, subject(d), html(d), text(d));
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

    Digest build(Instant now) {
        ZonedDateTime day = now.atZone(ZONE).toLocalDate().atStartOfDay(ZONE);
        Instant start = day.toInstant();
        Instant end = day.plusDays(1).toInstant();

        List<Task> tasks = taskRepo.findByDeletedAtIsNull();
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

        Map<String, Task> byId = tasks.stream().collect(Collectors.toMap(t -> t.id, Function.identity()));
        List<TimeBlock> all = blocks.list().stream().filter(b -> byId.containsKey(b.taskId)).toList();
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
        return new Digest(day, dueToday, overdue, schedule, weekMinutes);
    }

    private static ZonedDateTime weekStart(ZonedDateTime day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** 本週累計工時那一行；沒有工時（含週一）回傳 null（不列）。例：週三寄 → 本週累計工時 5.5h（10/26–10/27） */
    String weekLine(Digest d) {
        if (d.weekMinutes() == 0) return null;
        String hours = BigDecimal.valueOf(d.weekMinutes()).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
        return "本週累計工時 %sh（%s–%s）".formatted(hours, md(weekStart(d.day())), md(d.day().minusDays(1)));
    }

    private static String md(ZonedDateTime d) {
        return d.getMonthValue() + "/" + "%02d".formatted(d.getDayOfMonth());
    }

    String subject(Digest d) {
        return "[每日摘要] %s 今日到期 %d 項、逾期 %d 項、排程 %d 段"
                .formatted(dayLabel(d), d.dueToday().size(), d.overdue().size(), d.schedule().size());
    }

    private String html(Digest d) {
        StringBuilder sb = new StringBuilder("<div style=\"font-family:sans-serif;line-height:1.6\">");
        sb.append("<h2 style=\"margin:0 0 12px\">").append(dayLabel(d)).append(" 每日摘要</h2>");
        String week = weekLine(d);
        if (week != null) sb.append("<p style=\"margin:0 0 8px;font-weight:bold\">").append(week).append("</p>");
        section(sb, "今天到期", d.dueToday());
        section(sb, "已逾期", d.overdue());
        section(sb, "今天的時程", d.schedule());
        sb.append("<p><a href=\"").append(HtmlUtils.htmlEscape(props.frontendUrl())).append("\">打開工作事項管理</a></p></div>");
        return sb.toString();
    }

    private static void section(StringBuilder sb, String title, List<String> lines) {
        if (lines.isEmpty()) return;
        sb.append("<h3 style=\"margin:16px 0 4px\">").append(title).append("（").append(lines.size()).append("）</h3><ul>");
        lines.forEach(l -> sb.append("<li>").append(HtmlUtils.htmlEscape(l)).append("</li>"));
        sb.append("</ul>");
    }

    private String text(Digest d) {
        StringBuilder sb = new StringBuilder(dayLabel(d)).append(" 每日摘要\n");
        String week = weekLine(d);
        if (week != null) sb.append(week).append("\n");
        for (var e : List.of(Map.entry("今天到期", d.dueToday()), Map.entry("已逾期", d.overdue()), Map.entry("今天的時程", d.schedule()))) {
            if (e.getValue().isEmpty()) continue;
            sb.append("\n").append(e.getKey()).append("（").append(e.getValue().size()).append("）\n");
            e.getValue().forEach(l -> sb.append("・").append(l).append("\n"));
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
