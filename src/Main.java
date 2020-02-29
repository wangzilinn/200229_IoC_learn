public class Main {
    public static void main(String[] args) {
        ClassPathXMLApplicationContext path = new ClassPathXMLApplicationContext(
                "context.xml");
        UserService userService = (UserService) path.getBean("userService");
        userService.show();

    }
}
