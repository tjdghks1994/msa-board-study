package kuke.board.hotarticle.service.eventhandler;

import kuke.board.common.event.Event;
import kuke.board.common.event.EventPayload;

public interface EventHandler< T extends EventPayload> {
    void handle(Event<T> event);    // 이벤트를 처리

    boolean supports(Event<T> event);   // 이벤트를 지원하는지 확인

    Long findArticleId(Event<T> event); // 이벤트가 어떤 아티클에 대한건지 아티클 아이디를 조회
}
