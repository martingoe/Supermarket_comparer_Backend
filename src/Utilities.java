import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Random;

public class Utilities {
    public static HashMap<String, String> queryToMap(String query) {
        HashMap<String, String> result = new HashMap<>();

        for (String s : query.split("&")) {
            String[] strings = s.split("=");
            result.put(strings[0], strings[1]);
        }
        return result;
    }

    static void getIdNameLogos(ResultSet resultSet, JSONArray jsonArray) throws SQLException, JSONException {
        while (resultSet.next()) {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("id", resultSet.getInt(1));
            jsonObject.put("name", resultSet.getString(2));
            jsonObject.put("logo", new String(resultSet.getBytes(3)));

            jsonArray.put(jsonObject);
        }
    }

    static void write(HttpExchange exchange, int responseCode, String text){
        try {
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(responseCode, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(text.getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean nullOrEmpty(String s) {
        return s == null || s.equals("");
    }

    public static boolean exists(ResultSet set) {
        try {
            return set.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String storingPepper(String password) {
        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,-äüöÄÜÖ!\"§$%&/()=?{[]}\\";
        Random random = new Random();
        int i = random.nextInt(letters.length());
        char pepper = letters.charAt(i);
        password += pepper;

        return hash(password);
    }

    public static int notStoringPepper(String password, String email, Connection connection) {
        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,-äüöÄÜÖ!\"§$%&/()=?{[]}\\";
        for (char c : letters.toCharArray()) {
            try {
                String hash = hash(password + c);
                PreparedStatement statement = connection.prepareStatement("SELECT id FROM user WHERE password = ? AND email = ?;");
                statement.setString(1, hash);
                statement.setString(2, email);
                ResultSet s = statement.executeQuery();

                if (exists(s)){
                    return getId(s);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return 0;
    }

    public static int getId(ResultSet set) {
        try {
            set.last();
            return set.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] bytes = digest.digest(password.getBytes());

            StringBuilder returnString = new StringBuilder();

            for (byte s : bytes)
                returnString.append(Integer.toString(s + 0x100, 16).substring(1));

            return returnString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
