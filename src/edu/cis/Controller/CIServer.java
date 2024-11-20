/*
 * File: CIServer.java
 * ------------------------------
 * This program implements a basic ecommerce network management server.
 */

package edu.cis.Controller;

import acm.program.*;
import edu.cis.Model.*;
import edu.cis.Utils.SimpleServer;

import java.util.ArrayList;
import java.util.Objects;

public class CIServer extends ConsoleProgram implements SimpleServerListener {

    private static final int PORT = 8000;
    private SimpleServer server = new SimpleServer(this, PORT);
    private ArrayList<CISUser> users = new ArrayList<>();
    private Menu menu = new Menu(new ArrayList<>());

    public void run() {
        println("Starting server on port " + PORT);
        server.start();
    }

    public String requestMade(Request request) {
        String cmd = request.getCommand();
        println(request.toString());

        switch (cmd) {
            case CISConstants.PING:
                return handlePing();
            case CISConstants.CREATE_USER:
                return createUser(request);
            case CISConstants.ADD_MENU_ITEM:
                return addMenuItem(request);
            case CISConstants.PLACE_ORDER:
                return placeOrder(request);
            case CISConstants.DELETE_ORDER:
                return deleteOrder(request);
            case CISConstants.GET_ORDER:
                return getOrder(request);
            case CISConstants.GET_ITEM:
                return getItem(request);
            case CISConstants.GET_USER:
                return getUser(request);
            case CISConstants.GET_CART:
                return getCart(request);
            default:
                return "Error: Unknown command " + cmd + ".";
        }
    }

    private String handlePing() {
        String pingMsg = "Hello, internet";
        println("   => " + pingMsg);
        return pingMsg;
    }

    private String createUser(Request req) {
        String userName = req.getParam(CISConstants.USER_NAME_PARAM);
        String userId = req.getParam(CISConstants.USER_ID_PARAM);
        String yearLevel = req.getParam(CISConstants.YEAR_LEVEL_PARAM);

        if (isParamMissing(userName, userId, yearLevel)) {
            return CISConstants.PARAM_MISSING_ERR;
        }

        if (userExists(userId)) {
            return CISConstants.DUP_USER_ERR;
        }

        CISUser newUser = new CISUser(userId, userName, yearLevel, 50.0);
        users.add(newUser);
        return CISConstants.SUCCESS;
    }

    private boolean isParamMissing(String... params) {
        for (String param : params) {
            if (param == null || param.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean userExists(String userId) {
        return users.stream().anyMatch(u -> Objects.equals(u.getUserID(), userId));
    }

    private String addMenuItem(Request req) {
        String itemName = req.getParam(CISConstants.ITEM_NAME_PARAM);
        String description = req.getParam(CISConstants.DESC_PARAM);
        double price;

        try {
            price = Double.parseDouble(req.getParam(CISConstants.PRICE_PARAM));
        } catch (NumberFormatException e) {
            return "Error: Invalid price format.";
        }

        String itemType = req.getParam(CISConstants.ITEM_TYPE_PARAM);
        String itemId = req.getParam(CISConstants.ITEM_ID_PARAM);

        MenuItem newItem = new MenuItem(itemId, itemName, description, price, itemType);
        menu.addEadriumItem(newItem);
        return "success";
    }

    private String placeOrder(Request req) {
        String orderId = req.getParam(CISConstants.ORDER_ID_PARAM);
        String menuItemId = req.getParam(CISConstants.ITEM_ID_PARAM);
        String userId = req.getParam(CISConstants.USER_ID_PARAM);
        String orderType = req.getParam(CISConstants.ORDER_TYPE_PARAM);

        if (orderId == null) {
            return CISConstants.ORDER_INVALID_ERR;
        }
        if (menu.getEadriumItems().isEmpty()) {
            return CISConstants.EMPTY_MENU_ERR;
        }

        CISUser user = findUser(userId);
        if (user == null) {
            return CISConstants.USER_INVALID_ERR;
        }

        if (orderExistsForUser(user, orderId)) {
            return CISConstants.DUP_ORDER_ERR;
        }

        MenuItem item = findMenuItem(menuItemId);
        if (item == null) {
            return CISConstants.INVALID_MENU_ITEM_ERR;
        }

        if (item.getAmountAvailable() < 1) {
            return CISConstants.SOLD_OUT_ERR;
        }

        if (item.getPrice() > user.getMoney()) {
            return CISConstants.USER_BROKE_ERR;
        }

        Order order = new Order(menuItemId, orderType, orderId);
        item.minusAmountAvailable();
        user.getOrders().add(order);
        user.spend(item.getPrice());

        return CISConstants.SUCCESS;
    }

    private CISUser findUser(String userId) {
        return users.stream().filter(u -> Objects.equals(u.getUserID(), userId)).findFirst().orElse(null);
    }

    private boolean orderExistsForUser(CISUser user, String orderId) {
        return user.getOrders().stream().anyMatch(o -> Objects.equals(o.getOrderID(), orderId));
    }

    private MenuItem findMenuItem(String itemId) {
        return menu.getEadriumItems().stream().filter(m -> m.getId().equals(itemId)).findFirst().orElse(null);
    }

    public String deleteOrder(Request req) {
        String orderId = req.getParam(CISConstants.ORDER_ID_PARAM);
        String userId = req.getParam(CISConstants.USER_ID_PARAM);
        CISUser user = findUser(userId);

        if (user == null) {
            return CISConstants.USER_INVALID_ERR;
        }

        Order order = findOrder(user, orderId);
        if (order == null) {
            return CISConstants.ORDER_INVALID_ERR;
        }

        user.getOrders().remove(order);
        return CISConstants.SUCCESS;
    }

    private Order findOrder(CISUser user, String orderId) {
        return user.getOrders().stream().filter(o -> Objects.equals(o.getOrderID(), orderId)).findFirst().orElse(null);
    }

    public String getOrder(Request req) {
        String orderId = req.getParam(CISConstants.ORDER_ID_PARAM);
        String userId = req.getParam(CISConstants.USER_ID_PARAM);
        CISUser user = findUser(userId);

        if (user == null) {
            return "Error: user doesn't exist";
        }

        Order order = findOrder(user, orderId);
        if (order == null) {
            return "Error: order doesn't exist";
        }

        return order.toString();
    }

    public String getUser(Request req) {
        String userId = req.getParam(CISConstants.USER_ID_PARAM);
        CISUser user = findUser(userId);
        return (user != null) ? user.toString() : CISConstants.USER_INVALID_ERR;
    }

    public String getItem(Request req) {
        String itemId = req.getParam(CISConstants.ITEM_ID_PARAM);
        MenuItem item = findMenuItem(itemId);
        return (item != null) ? item.toString() : CISConstants.INVALID_MENU_ITEM_ERR;
    }

    public String getCart(Request req) {
        String userId = req.getParam(CISConstants.USER_ID_PARAM);
        CISUser user = findUser(userId);
        return (user != null) ? "orders=" + user.orderToString() : CISConstants.USER_INVALID_ERR;
    }

    public static void main(String[] args) {
        CIServer server = new CIServer();
        server.start(args);
    }
}