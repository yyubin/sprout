package sprout.context;

public interface ApplicationContext extends BeanFactory{
    void refresh() throws Exception; // bootstrap()을 대체할 메서드
    void close(); // 애플리케이션 종료 시 호출
}
