package demo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

public class App {
    private static final Logger log = LogManager.getLogger(App.class);

    private static final String DB_PASSWORD = "Summer2024!";

    public ResultSet findUser(String name) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:mysql://localhost/shop", "admin", DB_PASSWORD);
        Statement s = c.createStatement();
        return s.executeQuery("SELECT * FROM users WHERE name = '" + name + "'");
    }

    public void ping(String host) throws Exception {
        Runtime.getRuntime().exec("ping -c 1 " + host);
    }

    public byte[] download(String fileName) throws Exception {
        return Files.readAllBytes(new File("/var/app/uploads/" + fileName).toPath());
    }

    public String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return new String(md.digest(password.getBytes()));
    }

    public String resetToken() {
        return Long.toString(new Random().nextLong());
    }

    public Object loadSession(byte[] data) throws Exception {
        return new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
    }

    public void parseXml(InputStream in) throws Exception {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
    }

    public Object parseConfig(String yaml) {
        return new Yaml().load(yaml);
    }

    public String greet(String template) {
        return StringSubstitutor.createInterpolator().replace(template);
    }

    public void login(String user) {
        log.error("Login failed for user " + user);
    }
}
