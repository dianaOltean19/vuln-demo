package demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXParseException;
import org.yaml.snakeyaml.error.YAMLException;

class AppForgeTest {

    private final App app = new App();

    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(o);
        }
        return bos.toByteArray();
    }

    private static ByteArrayInputStream xml(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    // ---- hashPassword ----

    @Test
    void hashPassword_isDeterministicForSamePassword() throws Exception {
        assertEquals(app.hashPassword("secret"), app.hashPassword("secret"));
    }

    @Test
    void hashPassword_differentPasswordsGiveDifferentHashes() throws Exception {
        assertNotEquals(app.hashPassword("secret"), app.hashPassword("Secret"));
    }

    @Test
    @Disabled("Suspected bug: hash is new String(digest) with the platform charset, not a printable hex/Base64 encoding")
    void hashPassword_resultIsPrintableHex() throws Exception {
        assertTrue(app.hashPassword("password").matches("^[0-9a-f]+$"));
    }

    @Test
    @Disabled("Suspected bug: passwords are hashed with unsalted MD5; same password should not yield the same hash")
    void hashPassword_isSalted() throws Exception {
        assertNotEquals(app.hashPassword("password"), app.hashPassword("password"));
    }

    @Test
    @Disabled("Suspected bug: result equals raw unsalted MD5 of the password")
    void hashPassword_isNotPlainMd5() throws Exception {
        byte[] md5 = MessageDigest.getInstance("MD5").digest("password".getBytes());
        assertNotEquals(new String(md5), app.hashPassword("password"));
    }

    // ---- resetToken ----

    @Test
    void resetToken_isNonEmpty() {
        assertTrue(!app.resetToken().isEmpty());
    }

    @Test
    void resetToken_differsBetweenCalls() {
        assertNotEquals(app.resetToken(), app.resetToken());
    }

    @Test
    @Disabled("Suspected bug: token comes from java.util.Random with only 64 bits, may be negative")
    void resetToken_hasAtLeast128BitsAndNoSign() {
        String token = app.resetToken();
        assertTrue(token.length() >= 22);
        assertTrue(!token.startsWith("-"));
    }

    // ---- loadSession ----

    @Test
    void loadSession_roundTripsSerializedList() throws Exception {
        ArrayList<String> list = new ArrayList<>(List.of("a", "b", "c"));
        assertEquals(list, app.loadSession(serialize(list)));
    }

    @Test
    void loadSession_emptyInputThrowsEof() {
        assertThrows(EOFException.class, () -> app.loadSession(new byte[0]));
    }

    @Test
    void loadSession_nonSerializationBytesThrowStreamCorrupted() {
        assertThrows(StreamCorruptedException.class, () -> app.loadSession(new byte[] {1, 2, 3, 4, 5, 6}));
    }

    @Test
    @Disabled("Suspected bug: any serializable class is instantiated; classes outside an allow-list should be rejected")
    void loadSession_rejectsClassesOutsideAllowList() throws Exception {
        byte[] data = serialize(new HashMap<String, String>());
        assertThrows(java.io.InvalidClassException.class, () -> app.loadSession(data));
    }

    // ---- parseXml ----

    @Test
    void parseXml_acceptsWellFormedDocument() {
        assertDoesNotThrow(() -> app.parseXml(xml("<a><b/></a>")));
    }

    @Test
    void parseXml_malformedDocumentThrowsSaxParseException() {
        assertThrows(SAXParseException.class, () -> app.parseXml(xml("<a>")));
    }

    @Test
    @Disabled("Suspected bug: DOCTYPE with external entity is accepted and resolved (XXE)")
    void parseXml_rejectsExternalEntity(@TempDir Path dir) throws Exception {
        Path secret = dir.resolve("secret.txt");
        Files.writeString(secret, "top-secret");
        String doc = "<?xml version=\"1.0\"?><!DOCTYPE a [<!ENTITY x SYSTEM \"" + secret.toUri() + "\">]><a>&x;</a>";
        assertThrows(SAXParseException.class, () -> app.parseXml(xml(doc)));
    }

    @Test
    void parseXml_entityExpansionBombIsRejected() {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\"?><!DOCTYPE lolz [<!ENTITY lol0 \"lol\">");
        for (int i = 1; i <= 9; i++) {
            sb.append("<!ENTITY lol").append(i).append(" \"");
            for (int j = 0; j < 10; j++) {
                sb.append("&lol").append(i - 1).append(";");
            }
            sb.append("\">");
        }
        sb.append("]><lolz>&lol9;</lolz>");
        assertThrows(SAXParseException.class, () -> app.parseXml(xml(sb.toString())));
    }

    // ---- parseConfig ----

    @Test
    void parseConfig_parsesMapWithScalarAndList() {
        Object result = app.parseConfig("a: 1\nb: [x, y]");
        Map<String, Object> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("b", List.of("x", "y"));
        assertEquals(expected, result);
    }

    @Test
    void parseConfig_emptyStringReturnsNull() {
        assertNull(app.parseConfig(""));
    }

    @Test
    void parseConfig_invalidYamlThrowsYamlException() {
        assertThrows(YAMLException.class, () -> app.parseConfig("a: ["));
    }

    @Test
    void parseConfig_globalTagIsRejected() {
        assertThrows(YAMLException.class, () -> app.parseConfig("!!javax.script.ScriptEngineManager []"));
    }

    // ---- greet ----

    @Test
    void greet_plainStringUnchanged() {
        assertEquals("Hello, world", app.greet("Hello, world"));
    }

    @Test
    void greet_unknownVariableStaysLiteral() {
        assertEquals("Hi ${foo}", app.greet("Hi ${foo}"));
    }

    @Test
    @Disabled("Suspected bug: user-supplied ${sys:...} is expanded to a system property")
    void greet_doesNotExpandSystemPropertyLookups() {
        assertEquals("${sys:user.home}", app.greet("${sys:user.home}"));
    }

    @Test
    @Disabled("Suspected bug: user-supplied ${env:...} leaks environment variables")
    void greet_doesNotExpandEnvironmentLookups() {
        assertEquals("${env:PATH}", app.greet("${env:PATH}"));
    }

    // ---- download ----

    @Test
    void download_missingFileThrowsNoSuchFile() {
        assertThrows(NoSuchFileException.class, () -> app.download("no-such-file-xyz"));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    @Disabled("Suspected bug: '..' segments escape the uploads directory instead of being rejected")
    void download_rejectsPathTraversal() {
        Exception e = assertThrows(Exception.class, () -> app.download("../../../etc/hostname"));
        assertTrue(e instanceof SecurityException || e instanceof IllegalArgumentException);
    }

    // ---- findUser ----

    @Test
    void findUser_withoutJdbcDriverThrowsSqlException() {
        assertThrows(SQLException.class, () -> app.findUser("x"));
    }
}

