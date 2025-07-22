package sprout.core.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilterChainTest {

    @Mock HttpRequest<?> request;
    @Mock HttpResponse    response;
    @Mock Dispatcher      dispatcher;

    interface FilterMock extends Filter { }   // Mockito 를 위한 헬퍼

    /* ----------  필터가 없는 경우 ---------- */

    @Test @DisplayName("필터가 없으면 바로 dispatcher 호출")
    void noFilters_callsDispatcher() throws IOException {
        FilterChain chain = new FilterChain(List.of(), dispatcher);

        chain.doFilter(request, response);

        verify(dispatcher).dispatch(request, response);
        verifyNoMoreInteractions(dispatcher);
    }

    /* ---------- 모든 필터가 체인을 이어 주는 경우 ---------- */

    @Test @DisplayName("필터 N개가 순서대로 실행되고 마지막에 dispatcher 호출")
    void filtersRunInOrder_thenDispatcher() throws IOException {
        FilterMock f1 = mock(FilterMock.class);
        FilterMock f2 = mock(FilterMock.class);

        // 각 필터가 체인을 계속 진행하도록
        doAnswer(inv -> {
            FilterChain c = inv.getArgument(2);
            c.doFilter(request, response);
            return null;
        }).when(f1).doFilter(eq(request), eq(response), any());
        doAnswer(inv -> {
            FilterChain c = inv.getArgument(2);
            c.doFilter(request, response);
            return null;
        }).when(f2).doFilter(eq(request), eq(response), any());

        FilterChain chain = new FilterChain(List.of(f1, f2), dispatcher);

        chain.doFilter(request, response);

        // 실행 순서 검증
        InOrder inOrder = inOrder(f1, f2, dispatcher);
        inOrder.verify(f1).doFilter(eq(request), eq(response), any());
        inOrder.verify(f2).doFilter(eq(request), eq(response), any());
        inOrder.verify(dispatcher).dispatch(request, response);
    }

    /* ---------- 중간 필터가 체인 진행을 중단하는 경우 ---------- */

    @Test @DisplayName("중간 필터가 chain.doFilter 를 호출하지 않으면 이후 실행 중단")
    void filterStopsChain_dispatcherNotCalled() throws IOException {
        FilterMock stoppingFilter = mock(FilterMock.class);
        FilterMock neverCalled    = mock(FilterMock.class);

        // stoppingFilter 는 체인 진행을 멈춤 (do nothing)
        // neverCalled 가 호출되지 않는지 검증
        FilterChain chain = new FilterChain(List.of(stoppingFilter, neverCalled), dispatcher);

        chain.doFilter(request, response);

        verify(stoppingFilter).doFilter(eq(request), eq(response), any());
        verifyNoInteractions(neverCalled);
        verifyNoInteractions(dispatcher);
    }
}
