-- FR-O28 — Điều phối phòng khi quá tải
--
-- Ba bảng phục vụ màn "Điều phối phòng" của Organizer:
--   * event_overflow_rooms            — phòng dự phòng organizer khai báo thêm
--   * event_notifications             — thông báo đã gửi cho người tham dự
--   * event_notification_recipients   — từng người nhận (để user đọc lại)
--
-- Idempotent: chạy nhiều lần không lỗi, không sửa dữ liệu sẵn có.

CREATE TABLE IF NOT EXISTS event_overflow_rooms (
    overflow_room_id BIGINT       NOT NULL AUTO_INCREMENT,
    event_id         BIGINT       NOT NULL,
    room_name        VARCHAR(120) NOT NULL,
    capacity         INT          NOT NULL,
    note             VARCHAR(500) NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (overflow_room_id),
    KEY idx_overflow_room_event (event_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS event_notifications (
    notification_id   BIGINT        NOT NULL AUTO_INCREMENT,
    event_id          BIGINT        NOT NULL,
    created_by_user_id BIGINT       NOT NULL,
    title             VARCHAR(200)  NOT NULL,
    body              VARCHAR(2000) NOT NULL,
    audience          VARCHAR(40)   NOT NULL DEFAULT 'REGISTERED_NOT_CHECKED_IN',
    source            VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
    recipient_count   INT           NOT NULL DEFAULT 0,
    sent_at           DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (notification_id),
    KEY idx_event_notification_event (event_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS event_notification_recipients (
    recipient_id    BIGINT      NOT NULL AUTO_INCREMENT,
    notification_id BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    delivered_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (recipient_id),
    UNIQUE KEY uk_notification_user (notification_id, user_id),
    KEY idx_notification_recipient_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
