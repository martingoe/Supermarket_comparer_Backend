import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.concurrent.Executors;

public class Main {
    private Connection connection;

    private Main() {
        try {
            String[] info = Files.readAllLines(Paths.get("src/password.txt")).toArray(new String[0]);
            connection = DriverManager.getConnection("jdbc:mysql://" + info[0] + ":3306/supermarket_comparer",
                    info[1], info[2]);

            HttpServer server = HttpServer.create(new InetSocketAddress(2000), 0);

            server.createContext("/addSupermarket", new NewSupermarket());
            server.createContext("/removeSupermarket", new DeleteSupermarket());
            server.createContext("/getSupermarkets", new GetSupermarkets());
            server.createContext("/getSupermarketById", new GetSupermarketById());
            server.createContext("/getSupermarketsByCountry", new GetSupermarketsByCountry());
            server.createContext("/updateSupermarket", new UpdateSupermarket());

            server.createContext("/addCountry", new NewCountry());
            server.createContext("/removeCountry", new DeleteCountry());
            server.createContext("/getCountries", new GetCountries());
            server.createContext("/getCountryById", new GetCountryById());
            server.createContext("/updateCountry", new UpdateCountry());

            server.createContext("/addPrice", new NewPrice());
            server.createContext("/removePrice", new DeletePrice());
            server.createContext("/getPrices", new GetPrices());

            server.createContext("/addSale", new NewSale());
            server.createContext("/removeSale", new DeleteSale());
            server.createContext("/getSalesBySupermarket", new getSalesBySupermarket());
            server.createContext("/getSalesByItem", new getSalesByItem());

            server.createContext("/addItem", new NewItem());
            server.createContext("/removeItem", new DeleteItem());
            server.createContext("/getItems", new GetItems());
            server.createContext("/getItemsByCompanyId", new GetItemsByCompanyId());
            server.createContext("/updateItem", new UpdateItem());

            server.createContext("/addItem_company", new NewItem_Company());
            server.createContext("/removeItem_company", new DeleteItem_Company());
            server.createContext("/getItem_Companies", new GetItem_Companies());
            server.createContext("/getItem_CompanyById", new GetItem_CompanyById());
            server.createContext("/updateItem_Company", new UpdateItem_Company());

            server.createContext("/addLocation", new NewLocation());
            server.createContext("/removeLocation", new DeleteLocation());
            server.createContext("/updateLocation", new UpdateLocation());

            server.createContext("/addUser", new NewUser());
            server.createContext("/removeUser", new DeleteUser());
            server.createContext("/login", new Login());

            server.createContext("/search", new SearchHandler());


            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Main();
    }


    //SUPERMARKET
    private class NewSupermarket implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            String name = query.get("name");
            byte[] logos = query.get("logo").getBytes();
            try {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO supermarket VALUES (DEFAULT, ?, ?);");
                statement.setString(1, name);
                statement.setBytes(2, logos);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully added a new supermarket\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class DeleteSupermarket implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());


            int id = Integer.parseInt(query.get("id"));
            try {
                PreparedStatement statement = connection.prepareStatement("DELETE FROM supermarket WHERE id = ?");
                statement.setInt(1, id);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully removed a supermarket\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetSupermarkets implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                ResultSet resultSet = connection.prepareStatement("SELECT * FROM supermarket").executeQuery();


                if (!Utilities.exists(resultSet)) {
                    Utilities.write(exchange, 404, "{\"error\":\"There are no Supermarkets yet\"}");
                } else {
                    JSONArray jsonArray = new JSONArray();
                    resultSet.beforeFirst();
                    Utilities.getIdNameLogos(resultSet, jsonArray);
                    Utilities.write(exchange, 200, jsonArray.toString());
                }
            } catch (SQLException | JSONException e) {
                e.printStackTrace();
                Utilities.write(exchange, 400, "{\"error\":\"Something went wrong\"}");
            }

        }


    }

    private class GetSupermarketById implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());
            int id = Integer.parseInt(query.get("id"));

            try {
                ResultSet supermarketSet = connection.prepareStatement("SELECT * FROM supermarket WHERE id=" + id + ";").executeQuery();
                ResultSet locationsSet = connection.prepareStatement("SELECT location.*, countries.* FROM location LEFT JOIN countries ON countries.id=location.countryId WHERE location.supermarketId=" + id + ";").executeQuery();
                ResultSet itemSet = connection.prepareStatement("SELECT price, item.*, item_company.* FROM prices JOIN item ON prices.itemId=item.id JOIN item_company ON item.companyId=item_company.id WHERE prices.supermarketId=" + id + ";").executeQuery();

                if (!Utilities.exists(supermarketSet)) {
                    Utilities.write(exchange, 404, "{\"error\":\"The supermarket doesn't exist\"}");
                } else {

                    JSONObject returnObject = new JSONObject();
                    returnObject.put("supermarketId", supermarketSet.getInt(1));
                    returnObject.put("supermarketName", supermarketSet.getString(2));
                    returnObject.put("supermarketLogo", new String(supermarketSet.getBytes(3)));

                    JSONArray jsonArray = new JSONArray();
                    if (Utilities.exists(locationsSet)) {
                        locationsSet.beforeFirst();

                        while (locationsSet.next()) {
                            JSONObject jsonObject = new JSONObject();

                            jsonObject.put("id", locationsSet.getInt(1));
                            jsonObject.put("address", locationsSet.getString(4));
                            jsonObject.put("countryId", locationsSet.getInt(5));
                            jsonObject.put("countryName", locationsSet.getString(6));
                            jsonObject.put("flag", new String(locationsSet.getBytes(7)));

                            jsonArray.put(jsonObject);
                        }
                    }
                    returnObject.put("locations", jsonArray);

                    JSONArray pricesArray = new JSONArray();
                    if (Utilities.exists(itemSet)) {
                        itemSet.beforeFirst();

                        while (itemSet.next()) {
                            JSONObject jsonObject = new JSONObject();

                            jsonObject.put("price", itemSet.getInt(1));
                            jsonObject.put("itemId", itemSet.getInt(2));
                            jsonObject.put("name", itemSet.getString(3));
                            jsonObject.put("companyId", itemSet.getInt(6));
                            jsonObject.put("company", itemSet.getString(7));
                            jsonObject.put("logo", new String(itemSet.getBytes(8)));

                            pricesArray.put(jsonObject);
                        }
                    }
                    returnObject.put("prices", pricesArray);

                    Utilities.write(exchange, 200, returnObject.toString());
                }
            } catch (SQLException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetSupermarketsByCountry implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int countryId = Integer.parseInt(query.get("countryId"));

            try {
                PreparedStatement countryStatement = connection.prepareStatement("SELECT * FROM countries WHERE id=?");
                countryStatement.setInt(1, countryId);
                ResultSet set = countryStatement.executeQuery();


                PreparedStatement statement = connection.prepareStatement("SELECT supermarket.* FROM location LEFT JOIN supermarket ON location.supermarketId Where countryId=?");
                statement.setInt(1, countryId);
                ResultSet resultSet = statement.executeQuery();

                if (!Utilities.exists(resultSet)) {
                    Utilities.write(exchange, 404, "{\"error\":\"there are no supermarkets in the country\"}");
                }
                resultSet.beforeFirst();

                JSONObject jsonObject = new JSONObject();
                JSONObject object = new JSONObject();

                object.put("countryId", countryId);
                object.put("name", set.getString(2));
                object.put("flag", set.getBytes(3));

                jsonObject.put("country", object);

                JSONArray jsonArray = new JSONArray();
                while (resultSet.next()) {
                    JSONObject supermarketObject = new JSONObject();

                    supermarketObject.put("supermarketId", resultSet.getInt(1));
                    supermarketObject.put("supermarketName", resultSet.getString(2));
                    supermarketObject.put("supermarketLogo", resultSet.getBytes(3));


                    jsonArray.put(supermarketObject);
                }
                jsonObject.put("supermarkets", jsonArray);

                Utilities.write(exchange, 200, jsonObject.toString());
            } catch (SQLException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class UpdateSupermarket implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            try {
                PreparedStatement statement = connection.prepareStatement("UPDATE supermarket SET logo=?, name=? WHERE id=?;");
                statement.setBytes(1, query.get("logo").getBytes());
                statement.setString(2, query.get("name"));
                statement.setInt(3, Integer.parseInt(query.get("id")));
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully updated the name and the logo\"}");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    //COUNTRY
    private class NewCountry implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            String name = query.get("name");
            byte[] flag = query.get("flag").getBytes();
            try {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO countries VALUES (DEFAULT, ?, ?);");
                statement.setString(1, name);
                statement.setBytes(2, flag);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully added a new country\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class DeleteCountry implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());


            int id = Integer.parseInt(query.get("id"));
            try {
                PreparedStatement statement = connection.prepareStatement("DELETE FROM countries WHERE id = ?");
                statement.setInt(1, id);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully removed a country\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetCountries implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                ResultSet resultSet = connection.prepareStatement("SELECT countries.* FROM countries").executeQuery();

                if (!Utilities.exists(resultSet)) {
                    Utilities.write(exchange, 404, "{\"error\":\"There are no countries registered yet\"}");
                } else {

                    resultSet.beforeFirst();
                    JSONArray jsonArray = new JSONArray();
                    while (resultSet.next()) {
                        JSONObject jsonObject = new JSONObject();

                        jsonObject.put("id", resultSet.getInt(1));
                        jsonObject.put("name", resultSet.getString(2));
                        jsonObject.put("flag", new String(resultSet.getBytes(3)));

                        jsonArray.put(jsonObject);
                    }
                    Utilities.write(exchange, 200, jsonArray.toString());
                }
            } catch (SQLException | JSONException e) {
                e.printStackTrace();
                Utilities.write(exchange, 400, "{\"error\":\"Something went wrong\"}");
            }
        }
    }

    private class GetCountryById implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            int id = Integer.parseInt(Utilities.queryToMap(exchange.getRequestURI().getQuery()).get("id"));

            try {
                PreparedStatement statement = connection.prepareStatement("SELECT *FROM countries WHERE id=?;");
                statement.setInt(1, id);
                ResultSet resultSet = statement.executeQuery();

                PreparedStatement preparedStatement = connection.prepareStatement("SELECT DISTINCT location.supermarketId, supermarket.* FROM location LEFT JOIN supermarket ON location.supermarketId=supermarket.id WHERE countryId=?;");
                preparedStatement.setInt(1, id);
                ResultSet set = preparedStatement.executeQuery();

                JSONObject jsonObject = new JSONObject();

                resultSet.next();
                jsonObject.put("name", resultSet.getString(2));
                jsonObject.put("flag", new String(resultSet.getBytes(3)));


                JSONArray jsonArray = new JSONArray();
                while (set.next()) {
                    JSONObject object = new JSONObject();

                    object.put("id", set.getInt(1));
                    object.put("name", set.getString(3));
                    object.put("logo", new String(set.getBytes(4)));

                    jsonArray.put(object);
                }
                jsonObject.put("supermarkets", jsonArray);

                Utilities.write(exchange, 200, jsonObject.toString());
            } catch (SQLException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class UpdateCountry implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            try {
                PreparedStatement statement = connection.prepareStatement("UPDATE countries SET flag=?, name=? WHERE id=?;");
                statement.setBytes(1, query.get("flag").getBytes());
                statement.setString(2, query.get("name"));
                statement.setInt(3, Integer.parseInt(query.get("id")));
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully updated the name and the flag\"}");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    //PRICE
    private class NewPrice implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int supermarketId = Integer.parseInt(query.get("supermarketId"));
            int itemId = Integer.parseInt(query.get("itemId"));
            double price = Double.parseDouble(query.get("price"));
            try {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO prices VALUES (DEFAULT, ?, ?, ?);");
                statement.setInt(1, supermarketId);
                statement.setInt(2, itemId);
                statement.setDouble(3, price);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully added a new price\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class DeletePrice implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int id = Integer.parseInt(query.get("id"));
            try {
                PreparedStatement statement = connection.prepareStatement("DELETE FROM prices WHERE id = ?");
                statement.setInt(1, id);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully removed a price\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetPrices implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int itemId = Integer.parseInt(query.get("itemId"));

            try {
                JSONObject jsonObject = new JSONObject();

                PreparedStatement preparedStatement = connection.prepareStatement("SELECT item.*, item_company.* FROM item LEFT JOIN item_company ON item.companyId=item_company.id WHERE item.id=?;");

                preparedStatement.setInt(1, itemId);
                ResultSet set = preparedStatement.executeQuery();
                set.next();
                Utilities.setIdNameDescr(set, jsonObject);
                jsonObject.put("companyId", set.getInt(4));
                jsonObject.put("companyName", set.getString(6));
                jsonObject.put("companyLogo", new String(set.getBytes(7)));


                PreparedStatement statement = connection.prepareStatement("SELECT prices.*, supermarket.* FROM prices LEFT JOIN supermarket ON prices.supermarketId=supermarket.id WHERE prices.itemId=?;");
                statement.setInt(1, itemId);
                ResultSet resultSet = statement.executeQuery();

                JSONArray pricesArray = new JSONArray();

                while (resultSet.next()) {
                    JSONObject priceObject = new JSONObject();

                    priceObject.put("priceId", resultSet.getInt(1));
                    priceObject.put("price", resultSet.getDouble(4));
                    priceObject.put("supermarketId", resultSet.getInt(5));
                    priceObject.put("supermarketName", resultSet.getString(6));
                    priceObject.put("supermarketLogo", new String(resultSet.getBytes(7)));

                    pricesArray.put(priceObject);
                }


                Utilities.write(exchange, 200, jsonObject.toString() + "::::" + pricesArray.toString());
            } catch (SQLException | JSONException e) {
                e.printStackTrace();
            }
        }
    }


    //SALE
    private class NewSale implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) {
            HashMap<String, String> query = Utilities.queryToMap(httpExchange.getRequestURI().getQuery());
            try {
                int supermarketId = Integer.parseInt(query.get("supermarketId"));
                int itemId = Integer.parseInt(query.get("itemId"));
                double newPrice = Double.parseDouble(query.get("newPrice"));

                String expirationDateString = query.get("expiration");

                SimpleDateFormat format = new SimpleDateFormat("dd:MM:yyyy");


                java.util.Date parsedDate = format.parse(expirationDateString);

                java.sql.Date date = new java.sql.Date(parsedDate.getTime());

                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO sale VALUES (DEFAULT, ?, ?, ?, ?)");
                preparedStatement.setDouble(1, newPrice);
                preparedStatement.setInt(2, itemId);
                preparedStatement.setInt(3, supermarketId);
                preparedStatement.setDate(4, date);

                preparedStatement.execute();

                Utilities.write(httpExchange, 200, "{\"result\":\"Successfully added a new sale\"}");

            } catch (ParseException | SQLException e) {
                e.printStackTrace();
                Utilities.write(httpExchange, 400, "{\"result\":\"Could not add a new sale\"}");
            }


        }
    }

    private class DeleteSale implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) {
            HashMap<String, String> query = Utilities.queryToMap(httpExchange.getRequestURI().getQuery());

            int id = Integer.parseInt(query.get("id"));

            try {
                PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM sale WHERE id=?");
                preparedStatement.setInt(1, id);

                preparedStatement.execute();

                Utilities.write(httpExchange, 200, "{\"result\":\"Successfully removed a new sale\"}");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class getSalesBySupermarket implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int supermarketId = Integer.parseInt(query.get("supermarketId"));

            try {
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT sale.*, item.*, " +
                        "item_company.*, prices.* FROM sale LEFT JOIN item ON sale.itemId LEFT JOIN item_company ON " +
                        "item.companyId=item_company.id JOIN prices ON sale.itemId=prices.itemId AND sale.supermarketId=prices.supermarketId " +
                        "WHERE sale.supermarketId=?");
                preparedStatement.setInt(1, supermarketId);
                ResultSet resultSet = preparedStatement.executeQuery();


                if (!Utilities.exists(resultSet)) {
                    Utilities.write(exchange, 404, "{\"error\":\"There are no sales for this supermarket yet\"}");
                } else {
                    resultSet.beforeFirst();

                    JSONArray jsonArray = new JSONArray();

                    while (resultSet.next()) {
                        JSONObject jsonObject = new JSONObject();

                        jsonObject.put("id", resultSet.getInt(1));
                        jsonObject.put("price", resultSet.getDouble(2));
                        jsonObject.put("itemId", resultSet.getInt(3));
                        jsonObject.put("date", resultSet.getDate(5).toString());


                        jsonObject.put("itemId", resultSet.getInt(6));
                        jsonObject.put("itemName", resultSet.getString(7));
                        jsonObject.put("itemDescription", resultSet.getString(8));

                        jsonObject.put("companyId", resultSet.getInt(9));
                        jsonObject.put("companyName", resultSet.getString(11));
                        jsonObject.put("companyLogo", resultSet.getString(12));
                        jsonObject.put("companyDescription", resultSet.getString(13));


                        jsonObject.put("normalPrice", resultSet.getDouble(17));


                        jsonArray.put(jsonObject);
                    }
                    Utilities.write(exchange, 200, jsonArray.toString());
                }

            } catch (SQLException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class getSalesByItem implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int itemId = Integer.parseInt(query.get("itemId"));

            try {
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT sale.*, supermarket.*, " +
                        "prices.* FROM sale JOIN supermarket ON sale.supermarketId=supermarket.id JOIN prices ON " +
                        "sale.itemId=prices.itemId AND sale.supermarketId=prices.supermarketId WHERE sale.supermarketId=?;");


                preparedStatement.setInt(1, itemId);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (!Utilities.exists(resultSet)) {
                    Utilities.write(exchange, 404, "{\"error\":\"There are no sales for this item yet\"}");
                } else {
                    resultSet.beforeFirst();

                    JSONArray jsonArray = new JSONArray();
                    while (resultSet.next()) {
                        JSONObject jsonObject = new JSONObject();

                        jsonObject.put("id", resultSet.getInt(1));
                        jsonObject.put("price", resultSet.getDouble(2));
                        jsonObject.put("supermarketId", resultSet.getInt(4));
                        jsonObject.put("expiration", resultSet.getDate(5).toString());

                        jsonObject.put("supermarketName", resultSet.getString(7));
                        jsonObject.put("supermarketLogo", resultSet.getString(8));

                        jsonObject.put("normalPrice", resultSet.getDouble(12));

                        jsonArray.put(jsonObject);
                    }
                    Utilities.write(exchange, 200, jsonArray.toString());
                }
            } catch (SQLException | JSONException e) {
                e.printStackTrace();
            }

        }


    }

    //ITEM
    private class NewItem implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            String name = query.get("name");
            String description = query.get("description");
            int companyId = Integer.parseInt(query.get("companyId"));
            try {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO item VALUES (DEFAULT, ?, ?, ?);");
                statement.setString(1, name);
                statement.setString(2, description);
                statement.setInt(3, companyId);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully added a new item\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class DeleteItem implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int id = Integer.parseInt(query.get("id"));
            try {
                PreparedStatement statement = connection.prepareStatement("DELETE FROM item WHERE id = ?");
                statement.setInt(1, id);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully removed a item\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetItems implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                ResultSet resultSet = connection.prepareStatement("SELECT item.*, item_company.name, item_company.logo FROM item LEFT JOIN item_company ON item.companyId=item_company.id").executeQuery();

                if (!Utilities.exists(resultSet)) {
                    Utilities.write(exchange, 404, "{\"error\":\"There are no items yet\"}");
                } else {
                    resultSet.beforeFirst();
                    JSONArray jsonArray = new JSONArray();
                    while (resultSet.next()) {
                        JSONObject jsonObject = new JSONObject();

                        Utilities.setIdNameDescr(resultSet, jsonObject);

                        jsonObject.put("companyId", resultSet.getInt(4));
                        jsonObject.put("company", resultSet.getString(5));
                        jsonObject.put("logo", new String(resultSet.getBytes(6)));

                        jsonArray.put(jsonObject);
                    }
                    Utilities.write(exchange, 200, jsonArray.toString());
                }
            } catch (SQLException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetItemsByCompanyId implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) {
            HashMap<String, String> query = Utilities.queryToMap(httpExchange.getRequestURI().getQuery());

            try {
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM item WHERE companyId=?");
                preparedStatement.setInt(1, Integer.parseInt(query.get("id")));
                ResultSet resultSet = preparedStatement.executeQuery();

                JSONArray jsonArray = new JSONArray();
                Utilities.setIdAndName(resultSet, jsonArray);

                Utilities.write(httpExchange, 200, jsonArray.toString());

            } catch (SQLException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class UpdateItem implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) {
            HashMap<String, String> query = Utilities.queryToMap(httpExchange.getRequestURI().getQuery());
            try {
                PreparedStatement statement = connection.prepareStatement("UPDATE item SET name=?, description=? WHERE id=?;");
                statement.setString(1, query.get("name"));
                statement.setInt(3, Integer.parseInt(query.get("id")));
                statement.setString(2, query.get("description"));
                statement.executeUpdate();

                Utilities.write(httpExchange, 200, "{\"result\":\"Successfully updated the name and the description\"}");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    //ITEM_COMPANY
    private class NewItem_Company implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            String name = query.get("name");
            byte[] logo = query.get("logo").getBytes();
            String description = query.get("description");

            try {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO item_company VALUES (DEFAULT, ?, ?, ?);");
                statement.setString(1, name);
                statement.setBytes(2, logo);
                statement.setString(3, description);

                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully added a new Item company\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class DeleteItem_Company implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int id = Integer.parseInt(query.get("id"));
            try {
                PreparedStatement statement = connection.prepareStatement("DELETE FROM item_company WHERE id = ?");
                statement.setInt(1, id);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully removed an item company\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetItem_Companies implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {

            try {
                ResultSet resultSet = connection.prepareStatement("SELECT * FROM item_company").executeQuery();

                if (!Utilities.exists(resultSet)) {
                    Utilities.write(exchange, 404, "{\"error\":\"There are no companies yet\"}");
                } else {
                    resultSet.beforeFirst();
                    JSONArray jsonArray = new JSONArray();
                    while (resultSet.next()) {
                        JSONObject jsonObject = new JSONObject();

                        Utilities.setLogoIdNameDescr(resultSet, jsonObject);


                        jsonArray.put(jsonObject);
                    }
                    Utilities.write(exchange, 200, jsonArray.toString());
                }

            } catch (SQLException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetItem_CompanyById implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int id = Integer.parseInt(query.get("id"));

            try {
                PreparedStatement statement = connection.prepareStatement("SELECT item_company.* FROM item_company WHERE id=?");
                statement.setInt(1, id);
                ResultSet resultSet = statement.executeQuery();

                PreparedStatement preparedStatement = connection.prepareStatement("SELECT item.* FROM item WHERE companyId=?");
                preparedStatement.setInt(1, id);
                ResultSet set = preparedStatement.executeQuery();

                JSONObject jsonObject = new JSONObject();

                if (Utilities.exists(resultSet)) {
                    resultSet.beforeFirst();
                    resultSet.next();
                    Utilities.setLogoIdNameDescr(resultSet, jsonObject);

                    JSONArray jsonArray = new JSONArray();
                    while (set.next()) {
                        JSONObject object = new JSONObject();

                        Utilities.setIdNameDescr(set, object);

                        jsonArray.put(object);
                    }
                    jsonObject.put("items", jsonArray);

                    Utilities.write(exchange, 200, jsonObject.toString());
                } else {
                    Utilities.write(exchange, 404, "{\"error\":\"The company doesn't exist\"}");
                }
            } catch (SQLException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class UpdateItem_Company implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            try {
                PreparedStatement statement = connection.prepareStatement("UPDATE item_company SET logo=?, name=?, description=? WHERE id=?;");
                statement.setBytes(1, query.get("logo").getBytes());
                statement.setString(2, query.get("name"));
                statement.setInt(4, Integer.parseInt(query.get("id")));
                statement.setString(3, query.get("description"));
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully updated the name and the logo\"}");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    //LOCATION
    private class NewLocation implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int countryId = Integer.parseInt(query.get("countryId"));
            String address = query.get("address");
            int supermarketId = Integer.parseInt(query.get("supermarketId"));
            try {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO location VALUES (DEFAULT, ?, ?, ?);");
                statement.setInt(1, countryId);
                statement.setInt(2, supermarketId);
                statement.setString(3, address);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully added a new location\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class DeleteLocation implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int id = Integer.parseInt(query.get("id"));
            try {
                PreparedStatement statement = connection.prepareStatement("DELETE FROM location WHERE id = ?");
                statement.setInt(1, id);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully removed a location\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class UpdateLocation implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            try {
                PreparedStatement statement = connection.prepareStatement("UPDATE location SET address=?, countryId=? WHERE id=?;");
                statement.setString(1, query.get("address"));
                statement.setInt(2, Integer.parseInt(query.get("countryId")));
                statement.setInt(3, Integer.parseInt(query.get("id")));
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully updated the location\"}");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    //USER
    private class NewUser implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            String name = query.get("name");
            String email = query.get("email");
            String password = Utilities.storingPepper(query.get("password"));
            try {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO user VALUES (DEFAULT, ?, ?, ?, ?);");
                statement.setString(1, name);
                statement.setString(2, password);
                statement.setInt(3, 0);
                statement.setString(3, email);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully added a new user\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class DeleteUser implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            int id = Integer.parseInt(query.get("id"));
            try {
                PreparedStatement statement = connection.prepareStatement("DELETE FROM user WHERE id = ?");
                statement.setInt(1, id);
                statement.executeUpdate();

                Utilities.write(exchange, 200, "{\"result\":\"Successfully removed an user\"}");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class Login implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            String email = query.get("email");
            String password = query.get("password");

            int id = Utilities.notStoringPepper(password, email, connection);
            if (id != 0) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT * FROM user WHERE id=?");
                    statement.setInt(1, id);
                    ResultSet resultSet = statement.executeQuery();
                    resultSet.next();

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("result", "Successfully logged in");
                    jsonObject.put("Id", resultSet.getString(1));
                    jsonObject.put("Name", resultSet.getString(2));
                    jsonObject.put("Security Level", resultSet.getInt(4));
                    jsonObject.put("Email", resultSet.getString(5));

                    Utilities.write(exchange, 200, jsonObject.toString());
                } catch (SQLException | JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Utilities.write(exchange, 401, "{\"error\":\"Something went wrong\"}");
            }
        }
    }


    //SEARCH
    private class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());

            String searchWord = query.get("searchWord");
            try {
                ResultSet countryResults = connection.prepareStatement("SELECT id, name FROM countries where name LIKE '%" + searchWord + "%';").executeQuery();

                ResultSet supermarketRestults = connection.prepareStatement("SELECT id, name FROM supermarket where name LIKE '%" + searchWord + "%';").executeQuery();

                ResultSet itemResults = connection.prepareStatement("SELECT id, name FROM item where name LIKE '%" + searchWord + "%' OR description LIKE '%\" + query.get(\"searchWord\") + \"%';").executeQuery();

                ResultSet companyResults = connection.prepareStatement("SELECT id, name FROM item_company where name LIKE '%" + searchWord + "%' OR description LIKE '%\" + query.get(\"searchWord\") + \"%';").executeQuery();

                JSONArray countryArray = new JSONArray();
                getSearchResults(countryResults, countryArray);

                JSONArray supermarketsArray = new JSONArray();
                getSearchResults(supermarketRestults, supermarketsArray);

                JSONArray itemsArray = new JSONArray();
                getSearchResults(itemResults, itemsArray);

                JSONArray companyArray = new JSONArray();
                getSearchResults(companyResults, companyArray);

                JSONObject returnObject = new JSONObject();
                returnObject.put("companies", companyArray);
                returnObject.put("items", itemsArray);
                returnObject.put("supermarkets", supermarketsArray);
                returnObject.put("countries", countryArray);
                Utilities.write(exchange, 200, returnObject.toString());
            } catch (SQLException | JSONException e) {
                e.printStackTrace();
            }

        }

        private void getSearchResults(ResultSet countryResults, JSONArray countryArray) throws SQLException, JSONException {
            Utilities.setIdAndName(countryResults, countryArray);
        }
    }
}