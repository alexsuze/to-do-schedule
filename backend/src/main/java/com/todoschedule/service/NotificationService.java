package com.todoschedule.service;

import com.todoschedule.config.AppProperties;
import com.todoschedule.model.Board;
import com.todoschedule.model.Notification;
import com.todoschedule.model.Settings;
import com.todoschedule.model.Task;
import com.todoschedule.repository.BoardRepository;
import com.todoschedule.repository.NotificationRepository;
import com.todoschedule.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通知排程（需求書 3.5.3～3.5.5）：
 * - reschedule(task)：取消該事項尚未寄出的通知，依目前狀態重新建立
 * - 每分鐘寄送到期的通知，失敗每 5 分鐘重試最多 3 次
 * - 每日摘要：依設定時間寄一封今日到期 / 逾期彙整
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RETRY = 3;
    private static final Duration RETRY_DELAY = Duration.ofMinutes(5);

    private final NotificationRepository repo;
    private final TaskRepository tasks;
    private final BoardRepository boards;
    private final SettingsService settings;
    private final EmailService email;
    private final AppProperties props;

    public NotificationService(NotificationRepository repo, TaskRepository tasks, BoardRepository boards,
                               SettingsService settings, EmailService email, AppProperties props) {
        this.repo = repo;
        this.tasks = tasks;
        this.boards = boards;
        this.settings = settings;
        this.email = email;
        this.props = props;
    }

    /* ---------- 排程建立 / 取消（由 Task / Board / Trash service 呼叫） ---------- */

    public void reschedule(Task t) {
        List<Notification> existing = repo.findByTaskId(t.id);
        cancelPending(existing);
        if (!eligible(t)) return;

        Settings s = settings.get();
        List<String> leads = (t.notificationOverride != null && !t.notificationOverride.leadTimes.isEmpty())
                ? t.notificationOverride.leadTimes : s.defaultLeadTimes;
        Instant now = Instant.now();
        List<Notification> created = new ArrayList<>();
        for (String lead : leads) {
            Instant at = t.anchorAt().minus(parseLeadTime(lead));
            // 同一事項同一提前時間、同一到期只寄一次
            boolean alreadySent = existing.stream().anyMatch(n -> Notification.SENT.equals(n.status)
                    && lead.equals(n.leadTime) && at.equals(n.scheduledAt));
            if (alreadySent) continue;
            Notification n = new Notification();
            n.taskId = t.id;
            n.leadTime = lead;
            n.scheduledAt = at;              // 已過的時間會在下一輪掃描立即補寄（事項尚未到期）
            n.subject = subjectFor(t, s);
            created.add(n);
        }
        if (!created.isEmpty()) repo.saveAll(created);
        if (now.isAfter(t.anchorAt())) log.debug("事項 {} 已到期，不建立通知", t.id);
    }

    public void cancel(String taskId) {
        cancelPending(repo.findByTaskId(taskId));
    }

    public void cancelAll(List<Task> list) {
        if (list.isEmpty()) return;
        cancelPending(repo.findByTaskIdInAndStatus(list.stream().map(t -> t.id).toList(), Notification.PENDING));
    }

    public void rescheduleAll(List<Task> list) {
        for (Task t : list) reschedule(t);
    }

    public List<Notification> recent() {
        return repo.findTop50ByOrderByCreatedAtDesc();
    }

    /** 有基準時間（任務：到期；會議：開始）、未刪除、不在完成欄、基準時間未過、事項未關閉通知。 */
    boolean eligible(Task t) {
        Instant at = t.anchorAt();
        if (at == null || t.deletedAt != null || t.completedAt != null) return false;
        if (at.isBefore(Instant.now())) return false;
        if (t.notificationOverride != null && !t.notificationOverride.enabled) return false;
        return boards.findById(t.boardId).filter(b -> b.deletedAt == null)
                .map(b -> b.columns.stream().noneMatch(c -> c.id.equals(t.columnId) && c.isDoneType))
                .orElse(false);
    }

    private void cancelPending(List<Notification> list) {
        List<Notification> pending = list.stream().filter(n -> Notification.PENDING.equals(n.status)).toList();
        for (Notification n : pending) n.status = Notification.CANCELLED;
        if (!pending.isEmpty()) repo.saveAll(pending);
    }

    static Duration parseLeadTime(String lead) {
        long n = Long.parseLong(lead.substring(0, lead.length() - 1));
        return switch (lead.charAt(lead.length() - 1)) {
            case 'm' -> Duration.ofMinutes(n);
            case 'h' -> Duration.ofHours(n);
            case 'd' -> Duration.ofDays(n);
            case 'w' -> Duration.ofDays(7 * n);
            default -> throw new IllegalArgumentException("提前時間不正確：" + lead);
        };
    }

    /* ---------- 每分鐘寄送 ---------- */

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void sendDue() {
        Settings s = settings.get();
        if (!s.notificationsEnabled) return;
        Instant now = Instant.now();
        for (Notification n : repo.findByStatusAndScheduledAtLessThanEqual(Notification.PENDING, now)) {
            Optional<Task> task = tasks.findById(n.taskId);
            if (task.isEmpty() || !eligible(task.get())) {
                // 事項已完成 / 刪除 / 到期：已到期者交給每日摘要的逾期提醒
                n.status = Notification.CANCELLED;
                repo.save(n);
                continue;
            }
            deliver(n, () -> reminderBody(task.get(), s), s);
        }
        sendDigestIfDue(s, now);
    }

    private void deliver(Notification n, java.util.function.Supplier<String[]> body, Settings s) {
        try {
            String[] b = body.get(); // [subject, html, text]
            n.subject = b[0];
            n.resendMessageId = email.send(s.notifyEmail, b[0], b[1], b[2]);
            n.status = Notification.SENT;
            n.sentAt = Instant.now();
            n.errorMessage = null;
        } catch (RuntimeException e) {
            n.retryCount++;
            n.errorMessage = e.getMessage();
            if (n.retryCount >= MAX_RETRY) {
                n.status = Notification.FAILED;
                log.warn("通知 {} 寄送失敗，已達重試上限：{}", n.id, e.getMessage());
            } else {
                n.scheduledAt = Instant.now().plus(RETRY_DELAY);
            }
        }
        repo.save(n);
    }

    /* ---------- 每日摘要 ---------- */

    private void sendDigestIfDue(Settings s, Instant now) {
        if (!s.dailyDigestEnabled) return;
        ZoneId zone = ZoneId.of(s.timezone);
        LocalDate today = LocalDate.now(zone);
        Instant at = today.atTime(LocalTime.parse(s.dailyDigestTime)).atZone(zone).toInstant();
        if (now.isBefore(at) || repo.existsByTypeAndScheduledAt(Notification.TYPE_DIGEST, at)) return;

        Notification n = new Notification();
        n.type = Notification.TYPE_DIGEST;
        n.scheduledAt = at;
        deliver(n, () -> digestBody(s, zone, today), s);
    }

    /* ---------- 信件內容 ---------- */

    private String subjectFor(Task t, Settings s) {
        return t.isMeeting()
                ? "[會議提醒] " + t.title + " 將於 " + fmt(t.startAt, s.timezone) + " 開始"
                : "[工作提醒] " + t.title + " 將於 " + fmt(t.dueAt, s.timezone) + " 到期";
    }

    private String[] reminderBody(Task t, Settings s) {
        Board b = boards.findById(t.boardId).orElse(null);
        String boardName = b == null ? "" : b.name;
        String status = b == null ? "" : b.columns.stream().filter(c -> c.id.equals(t.columnId)).map(c -> c.name).findFirst().orElse("");
        String desc = t.description == null ? "" : (t.description.length() > 200 ? t.description.substring(0, 200) + "…" : t.description);
        String link = taskLink(t);
        String subject = subjectFor(t, s);
        String when = t.isMeeting()
                ? "開始：" + fmt(t.startAt, s.timezone) + (t.dueAt == null ? "" : "　結束：" + fmt(t.dueAt, s.timezone))
                : "到期：" + fmt(t.dueAt, s.timezone);
        String text = String.join("\n",
                "看板：" + boardName,
                "標題：" + t.title,
                desc.isEmpty() ? "" : "描述：" + desc,
                "狀態：" + status,
                "優先度：" + priorityLabel(t.priority),
                when,
                "開啟事項：" + link);
        String html = "<h3>" + esc(t.title) + "</h3>"
                + "<p>看板：" + esc(boardName) + "<br>狀態：" + esc(status)
                + "<br>優先度：" + priorityLabel(t.priority)
                + "<br><b>" + when + "</b></p>"
                + (desc.isEmpty() ? "" : "<p style=\"white-space:pre-wrap\">" + esc(desc) + "</p>")
                + "<p><a href=\"" + link + "\">開啟事項</a></p>";
        return new String[] { subject, html, text };
    }

    private String[] digestBody(Settings s, ZoneId zone, LocalDate today) {
        Instant now = Instant.now();
        Instant endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant();
        Map<String, Board> boardMap = new HashMap<>();
        boards.findByDeletedAtIsNullOrderByOrderAsc().forEach(b -> boardMap.put(b.id, b));

        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        List<Task> meetings = new ArrayList<>();
        List<Task> due = new ArrayList<>();
        List<Task> overdue = new ArrayList<>();
        for (Board b : boardMap.values()) {
            for (Task t : tasks.findByBoardIdAndDeletedAtIsNullOrderByOrderAsc(b.id)) {
                if (t.completedAt != null || t.anchorAt() == null) continue;
                if (t.isMeeting()) {
                    // 會議只列今天要開的，不算逾期
                    if (!t.startAt.isBefore(startOfDay) && t.startAt.isBefore(endOfDay)) meetings.add(t);
                } else if (t.dueAt.isBefore(now)) overdue.add(t);
                else if (t.dueAt.isBefore(endOfDay)) due.add(t);
            }
        }
        Comparator<Task> byAnchor = Comparator.comparing(Task::anchorAt);
        meetings.sort(byAnchor);
        due.sort(byAnchor);
        overdue.sort(byAnchor);

        String subject = "[每日摘要] " + today + " 今日會議 " + meetings.size() + " 場、到期 " + due.size() + " 項、逾期 " + overdue.size() + " 項";
        StringBuilder text = new StringBuilder();
        StringBuilder html = new StringBuilder();
        for (var section : List.of(Map.entry("今日會議", meetings), Map.entry("今日到期", due), Map.entry("逾期", overdue))) {
            text.append("\n【").append(section.getKey()).append("】").append(section.getValue().isEmpty() ? " 無" : "").append('\n');
            html.append("<h3>").append(section.getKey()).append("</h3>");
            if (section.getValue().isEmpty()) html.append("<p>無</p>");
            else html.append("<ul>");
            for (Task t : section.getValue()) {
                String bn = Optional.ofNullable(boardMap.get(t.boardId)).map(b -> b.name).orElse("");
                String line = fmt(t.anchorAt(), s.timezone) + "  " + t.title + "（" + bn + "）";
                text.append("- ").append(line).append("  ").append(taskLink(t)).append('\n');
                html.append("<li><a href=\"").append(taskLink(t)).append("\">").append(esc(line)).append("</a></li>");
            }
            if (!section.getValue().isEmpty()) html.append("</ul>");
        }
        return new String[] { subject, html.toString(), text.toString() };
    }

    private String taskLink(Task t) {
        String base = props.frontendUrl().endsWith("/") ? props.frontendUrl() : props.frontendUrl() + "/";
        return base + "#/boards/" + t.boardId + "?task=" + t.id;
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static String fmt(Instant at, String tz) {
        return FMT.withZone(ZoneId.of(tz)).format(at);
    }

    private static String priorityLabel(String p) {
        return switch (p == null ? "" : p) { case "high" -> "高"; case "low" -> "低"; default -> "中"; };
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
