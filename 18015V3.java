import java.sql.*;
import java.util.Scanner;
import java.util.Vector;

class Pizza {
    private String size;
    private Vector<String> toppings;

    public Pizza(String size) {
        this.size = size;
        this.toppings = new Vector<>();
    }

    public void addTopping(String topping) {
        toppings.add(topping);
    }

    public String getSize() {
        return size;
    }

    public Vector<String> getToppings() {
        return toppings;
    }

    @Override
    public String toString() {
        return "Size: " + size + ", Toppings: " + String.join(", ", toppings);
    }
}

class PizzaShop {
    private Connection conn;

    public PizzaShop() {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Connect to the MySQL database
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3307/PizzaShopDB", "root", "");
            System.out.println("Connected to the database!");
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    public void addOrder(Vector<Pizza> order) {
        try {
            String orderQuery = "INSERT INTO Orders (order_number) VALUES (NULL)";
            PreparedStatement orderStmt = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS);
            orderStmt.executeUpdate();
            
            ResultSet generatedKeys = orderStmt.getGeneratedKeys();
            int orderId = 0;
            if (generatedKeys.next()) {
                orderId = generatedKeys.getInt(1);
            }

            String pizzaQuery = "INSERT INTO Pizzas (order_id, size, toppings) VALUES (?, ?, ?)";
            for (Pizza pizza : order) {
                PreparedStatement pizzaStmt = conn.prepareStatement(pizzaQuery);
                pizzaStmt.setInt(1, orderId);
                pizzaStmt.setString(2, pizza.getSize());
                pizzaStmt.setString(3, String.join(", ", pizza.getToppings()));
                pizzaStmt.executeUpdate();
            }
            System.out.println("Order placed successfully.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void addPizzaToOrder(int orderId, Pizza pizza) {
        try {
            String query = "INSERT INTO Pizzas (order_id, size, toppings) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, orderId);
            stmt.setString(2, pizza.getSize());
            stmt.setString(3, String.join(", ", pizza.getToppings()));
            stmt.executeUpdate();
            System.out.println("Pizza added to Order #" + orderId);
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void deletePizzaFromOrder(int orderId, int pizzaId) {
        try {
            String query = "DELETE FROM Pizzas WHERE order_id = ? AND id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, orderId);
            stmt.setInt(2, pizzaId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Pizza #" + pizzaId + " has been deleted from Order #" + orderId + ".");
            } else {
                System.out.println("Invalid pizza number.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void deleteOrder(int orderId) {
        try {
            String pizzaQuery = "DELETE FROM Pizzas WHERE order_id = ?";
            PreparedStatement pizzaStmt = conn.prepareStatement(pizzaQuery);
            pizzaStmt.setInt(1, orderId);
            pizzaStmt.executeUpdate();

            String orderQuery = "DELETE FROM Orders WHERE id = ?";
            PreparedStatement orderStmt = conn.prepareStatement(orderQuery);
            orderStmt.setInt(1, orderId);
            int rowsAffected = orderStmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Order #" + orderId + " has been deleted.");
            } else {
                System.out.println("Invalid order number.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void displayOrders() {
        try {
            String orderQuery = "SELECT * FROM Orders";
            Statement orderStmt = conn.createStatement();
            ResultSet orderRs = orderStmt.executeQuery(orderQuery);

            if (!orderRs.isBeforeFirst()) {
                System.out.println("No orders placed.");
                return;
            }

            while (orderRs.next()) {
                int orderId = orderRs.getInt("id");
                System.out.println("Order #" + orderId + ":");
                String pizzaQuery = "SELECT * FROM Pizzas WHERE order_id = " + orderId;
                Statement pizzaStmt = conn.createStatement();
                ResultSet pizzaRs = pizzaStmt.executeQuery(pizzaQuery);
                while (pizzaRs.next()) {
                    int pizzaId = pizzaRs.getInt("id");
                    String size = pizzaRs.getString("size");
                    String toppings = pizzaRs.getString("toppings");
                    System.out.println("  Pizza #" + pizzaId + ": Size: " + size + ", Toppings: " + toppings);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

public class PizzaShopApp {
    private static final int PLACE_ORDER = 1;
    private static final int ADD_PIZZA = 2;
    private static final int DELETE_PIZZA = 3;
    private static final int DELETE_ORDER = 4;
    private static final int DISPLAY_ORDERS = 5;
    private static final int EXIT = 6;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        PizzaShop pizzaShop = new PizzaShop();

        System.out.println("Welcome to the Pizza Shop!");

        while (true) {
            System.out.println("\nChoose an option:");
            System.out.println(PLACE_ORDER + ". Place a new order");
            System.out.println(ADD_PIZZA + ". Add a pizza to an existing order");
            System.out.println(DELETE_PIZZA + ". Delete a pizza from an existing order");
            System.out.println(DELETE_ORDER + ". Delete an existing order");
            System.out.println(DISPLAY_ORDERS + ". Display all orders");
            System.out.println(EXIT + ". Exit");

            int choice = getValidInput(scanner);
            switch (choice) {
                case PLACE_ORDER:
                    Vector<Pizza> currentOrder = new Vector<>();
                    while (true) {
                        System.out.print("Enter pizza size (small, medium, large) or 'done' to finish this order: ");
                        String size = scanner.nextLine();
                        if (size.equalsIgnoreCase("done")) {
                            break;
                        }

                        Pizza pizza = new Pizza(size);
                        while (true) {
                            System.out.print("Enter topping to add (Onions, Black Olives, Mushrooms, Extra Cheese, Pepperoni, Sausage, Bell Pepper) or 'done' to finish adding toppings: ");
                            String topping = scanner.nextLine();
                            if (topping.equalsIgnoreCase("done")) {
                                break;
                            }
                            pizza.addTopping(topping);
                        }

                        currentOrder.add(pizza);
                    }
                    pizzaShop.addOrder(currentOrder);
                    break;

                case ADD_PIZZA:
                    System.out.print("Enter the order number to which you want to add a pizza: ");
                    int orderNumberToAdd = getValidOrderNumber(scanner);
                    System.out.print("Enter pizza size (small, medium, large): ");
                    String sizeToAdd = scanner.nextLine();
                    Pizza pizzaToAdd = new Pizza(sizeToAdd);

                    while (true) {
                        System.out.print("Enter topping to add (or 'done' to finish adding toppings): ");
                        String toppingToAdd = scanner.nextLine();
                        if (toppingToAdd.equalsIgnoreCase("done")) {
                            break;
                        }
                        pizzaToAdd.addTopping(toppingToAdd);
                    }

                    pizzaShop.addPizzaToOrder(orderNumberToAdd, pizzaToAdd);
                    break;

                case DELETE_PIZZA:
                    System.out.print("Enter the order number to delete a pizza from: ");
                    int orderNumberToDeleteFrom = getValidOrderNumber(scanner);
                    System.out.print("Enter the pizza number to delete: ");
                    int pizzaNumberToDelete = getValidPizzaNumber(scanner);
                    pizzaShop.deletePizzaFromOrder(orderNumberToDeleteFrom, pizzaNumberToDelete);
                    break;

                case DELETE_ORDER:
                    System.out.print("Enter the order number to delete: ");
                    int orderNumberToDelete = getValidOrderNumber(scanner);
                    pizzaShop.deleteOrder(orderNumberToDelete);
                    break;

                case DISPLAY_ORDERS:
                    pizzaShop.displayOrders();
                    break;

                case EXIT:
                    System.out.println("Thank you for using our app!");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static int getValidInput(Scanner scanner) {
        while (true) {
            try {
                int input = Integer.parseInt(scanner.nextLine());
                return input;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static int getValidOrderNumber(Scanner scanner) {
        while (true) {
            int orderNumber = getValidInput(scanner);
            if (orderNumber >= 1) {
                return orderNumber;
            } else {
                System.out.println("Invalid order number. Please try again.");
            }
        }
    }

    private static int getValidPizzaNumber(Scanner scanner) {
        while (true) {
            int pizzaNumber = getValidInput(scanner);
            if (pizzaNumber >= 1) {
                return pizzaNumber;
            } else {
                System.out.println("Invalid pizza number. Please try again.");
            }
        }
    }
}