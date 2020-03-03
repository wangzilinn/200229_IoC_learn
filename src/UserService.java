public class UserService {

    public UserDAO userDAO;
    public ChatDAO chatDAO;

    //字段注入
    @Resource
    public CardDAO cardDAO;

    @Resource(id = "userDAO")
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    //无参数
    @Resource
    public void setChatDAO(ChatDAO chatDAO) {
        this.chatDAO = chatDAO;
    }

    public void show() {
        userDAO.show();
        chatDAO.show();
        cardDAO.show();
    }
}
