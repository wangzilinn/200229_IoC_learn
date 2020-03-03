public class Main {
    public static void main(String[] args) {
        ClassPathXMLApplicationContext context = new ClassPathXMLApplicationContext(
                "context.xml");
        UserService userService = (UserService) context.getBean("userService");
        userService.show();
    }
}
