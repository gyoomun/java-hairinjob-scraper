package org.nicenomu.hairinjob.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class GlobalExceptionHandler {

    private static final String ERROR_LOG_DIR = "error-logs";
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static Integer currentSessionId = null;

    public static void setCurrentSessionId(Integer sessionId) {
        currentSessionId = sessionId;
    }

    public static void clearCurrentSessionId() {
        currentSessionId = null;
    }

    public static void handleException(Exception e) {
        handleException(e, null);
    }

    public static void handleException(Exception e, String additionalContext) {
        try {
            // 에러 로그 디렉토리 생성
            Path errorLogDir = Paths.get(ERROR_LOG_DIR);
            if (!Files.exists(errorLogDir)) {
                Files.createDirectories(errorLogDir);
            }

            // 파일명 생성: YYYYMMDD-HHmmss-session{id}.txt
            String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
            String sessionPart = currentSessionId != null ? "session" + currentSessionId : "unknown";
            String fileName = String.format("%s-%s.txt", timestamp, sessionPart);
            Path errorLogFile = errorLogDir.resolve(fileName);

            // 에러 내용 작성
            StringBuilder errorContent = new StringBuilder();
            errorContent.append("=".repeat(80)).append("\n");
            errorContent.append("에러 발생 시각: ").append(LocalDateTime.now()).append("\n");
            errorContent.append("세션 ID: ").append(currentSessionId != null ? currentSessionId : "없음 (세션 생성 전)").append("\n");
            if (additionalContext != null && !additionalContext.isEmpty()) {
                errorContent.append("추가 정보: ").append(additionalContext).append("\n");
            }
            errorContent.append("=".repeat(80)).append("\n\n");

            // 예외 메시지
            errorContent.append("에러 메시지: ").append(e.getMessage()).append("\n\n");

            // 스택 트레이스
            errorContent.append("스택 트레이스:\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            errorContent.append(sw.toString());

            // 파일에 저장
            Files.writeString(errorLogFile, errorContent.toString());

            log.error("에러 로그 저장 완료: {}", errorLogFile.toAbsolutePath());

        } catch (IOException ioException) {
            log.error("에러 로그 파일 저장 실패", ioException);
        }
    }
}
