import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * PEGen — MSS301 Microservices project scaffolder (single-file, zero-dependency).
 * <p>
 * Run (offline, JDK 21):
 * java PEGen.java pe-config.properties
 * <p>
 * Reads pe-config.properties and generates 3 ready-to-import Maven projects
 * (Service A, Service B with OpenFeign, Gateway) + the SQL script.
 * Change the config to adapt to a renamed/restructured exam — no code change needed.
 */
public class PEGen {

    static final String OUTPUT_MARKER = ".mss301-pegen-engine-output";
    static final String OUTPUT_MARKER_CONTENT = "MSS301_PEGEN_ENGINE_OUTPUT_V1";
    static Properties cfg = new Properties();
    static String GROUP, GROUP_PATH, SBV, SCV, SDV, DBNAME, DBHOST, DBPORT, DBUSER, DBPASS, RESPONSE_STYLE;
    static Path OUT;

    // ------------------------------------------------------------------ models
    static class Field {
        String name, kind = "STRING", column, pattern, after, geField, fk, sqlType, jsonFormat;
        int max = -1, beforeDays = -1;
        boolean required, unique, email, enforceFk = true;
        List<String> enumValues;

        String type() {
            return switch (kind) {
                case "DATE", "DATETIME" -> "Date";
                case "INTEGER" -> "Integer";
                case "LONG" -> "Long";
                default -> "String"; // STRING, ENUM, STATUS
            };
        }
    }

    static class Ref {
        boolean enabled;
        boolean inputId = true, inputObject = true;
        boolean enforceFk = true;
        String column, idProp, objectProp, basePath, serviceUrlProp, serviceUrl, dtoClass, sqlType = "BIGINT";
    }

    static class Filter {
        String query, field;
        boolean status;

        Filter(String query, String field, boolean status) {
            this.query = query;
            this.field = field;
            this.status = status;
        }
    }

    static class Lookup {
        boolean enabled;
        String entity, table, idName, idColumn, idSqlType = "BIGINT", basePath;
        List<Field> fields = new ArrayList<>();
    }

    static class Service {
        String key, project, pkg, port, entity, table, idName, idColumn, idSqlType = "BIGINT", basePath, controller, listNameField;
        String statusField, statusDefault, statusInactive;
        boolean statusDefaultOnCreate = true;
        List<String> statusValues = new ArrayList<>();
        boolean hasStatus;
        boolean foodResponse;
        List<String> foodResponseFields = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        List<Filter> filters = new ArrayList<>();
        Lookup lookup = new Lookup();
        Ref ref = new Ref();
        Service base; // for B: the referenced service A
    }

    // ------------------------------------------------------------------ config
    static String g(String k, String def) {
        return cfg.getProperty(k, def);
    }

    static String r(String k) {
        String v = cfg.getProperty(k);
        if (v == null) throw new RuntimeException("Missing config key: " + k);
        return v.trim();
    }

    static Field parseField(String prefix, String name) {
        Field f = new Field();
        f.name = name;
        String spec = r(prefix + ".field." + name);
        for (String tokRaw : spec.split(",")) {
            String tok = tokRaw.trim();
            if (tok.isEmpty()) continue;
            String key = tok, val = null;
            int eq = tok.indexOf('=');
            if (eq >= 0) {
                key = tok.substring(0, eq).trim();
                val = tok.substring(eq + 1).trim();
            }
            switch (key) {
                case "string" -> f.kind = "STRING";
                case "date" -> f.kind = "DATE";
                case "datetime" -> f.kind = "DATETIME";
                case "int", "integer" -> f.kind = "INTEGER";
                case "long" -> f.kind = "LONG";
                case "status" -> f.kind = "STATUS";
                case "enum" -> {
                    if (val == null || val.isBlank())
                        throw new RuntimeException("enum requires values for " + prefix + ".field." + name);
                    f.kind = "ENUM";
                    f.enumValues = Arrays.asList(val.split("\\|"));
                }
                case "required" -> f.required = true;
                case "unique" -> f.unique = true;
                case "email" -> f.email = true;
                case "max" -> f.max = Integer.parseInt(val);
                case "pattern" -> f.pattern = val;
                case "after" -> f.after = val;
                case "beforeDays" -> f.beforeDays = Integer.parseInt(val);
                case "geField" -> f.geField = val;
                case "fk" -> f.fk = val;
                case "enforceFk" -> f.enforceFk = Boolean.parseBoolean(val);
                case "column" -> f.column = val;
                case "sql" -> f.sqlType = safeSqlType(val);
                case "json" -> {
                    if (val == null || val.isBlank())
                        throw new RuntimeException("json requires a date pattern for " + prefix + ".field." + name);
                    f.jsonFormat = val;
                }
                default ->
                        throw new RuntimeException("Unknown field token '" + key + "' in " + prefix + ".field." + name);
            }
        }
        if (f.column == null) f.column = name;
        return f;
    }

    static Service loadService(String k) {
        Service s = new Service();
        s.key = k;
        s.project = r(k + ".project");
        s.pkg = r(k + ".pkg");
        s.port = r(k + ".port");
        s.entity = r(k + ".entity");
        s.table = r(k + ".table");
        s.idName = r(k + ".id.name");
        s.idColumn = r(k + ".id.column");
        s.idSqlType = safeSqlType(g(k + ".id.sqlType", "BIGINT"));
        s.basePath = r(k + ".basePath");
        s.controller = r(k + ".controller");
        s.listNameField = g(k + ".list.nameField", "");
        s.statusField = g(k + ".status.field", "");
        s.hasStatus = !s.statusField.isEmpty();
        if (s.hasStatus) {
            s.statusValues = Arrays.asList(r(k + ".status.values").split(","));
            s.statusDefault = r(k + ".status.default");
            s.statusInactive = r(k + ".status.inactive");
            s.statusDefaultOnCreate = Boolean.parseBoolean(g(k + ".status.defaultOnCreate", "true"));
        }
        for (String fn : r(k + ".fields").split(",")) {
            String name = fn.trim();
            if (!name.isEmpty()) s.fields.add(parseField(k, name));
        }
        if (g(k + ".ref.enabled", "false").equalsIgnoreCase("true")) {
            Ref rf = s.ref;
            rf.enabled = true;
            rf.column = r(k + ".ref.column");
            rf.idProp = r(k + ".ref.idProp");
            rf.objectProp = r(k + ".ref.objectProp");
            rf.basePath = r(k + ".ref.basePath");
            rf.serviceUrlProp = r(k + ".ref.serviceUrlProp");
            rf.serviceUrl = r(k + ".ref.serviceUrl");
            rf.sqlType = safeSqlType(g(k + ".ref.sqlType", "BIGINT"));
            rf.enforceFk = Boolean.parseBoolean(g(k + ".ref.enforceFk", "true"));
            rf.inputId = Boolean.parseBoolean(g(k + ".ref.inputId", "true"));
            rf.inputObject = Boolean.parseBoolean(g(k + ".ref.inputObject", "true"));
        }
        s.foodResponse = Boolean.parseBoolean(g(k + ".food.response.enabled", "false"));
        if (s.foodResponse) {
            for (String name : r(k + ".food.response.fields").split(","))
                if (!name.isBlank()) s.foodResponseFields.add(name.trim());
        }
        if (g(k + ".lookup.enabled", "false").equalsIgnoreCase("true")) {
            s.lookup.enabled = true;
            s.lookup.entity = r(k + ".lookup.entity");
            s.lookup.table = r(k + ".lookup.table");
            s.lookup.idName = r(k + ".lookup.id.name");
            s.lookup.idColumn = r(k + ".lookup.id.column");
            s.lookup.idSqlType = safeSqlType(g(k + ".lookup.id.sqlType", "BIGINT"));
            s.lookup.basePath = r(k + ".lookup.basePath");
            for (String fn : r(k + ".lookup.fields").split(",")) {
                String name = fn.trim();
                if (!name.isEmpty()) s.lookup.fields.add(parseField(k + ".lookup", name));
            }
        }
        String filterSpec = g(k + ".list.filters", "").trim();
        if (!filterSpec.isEmpty()) {
            for (String raw : filterSpec.split(",")) {
                String[] p = raw.trim().split(":");
                if (p.length < 2 || p[0].isBlank() || p[1].isBlank())
                    throw new RuntimeException("Invalid " + k + ".list.filters entry: " + raw);
                s.filters.add(new Filter(p[0].trim(), p[1].trim(), p.length > 2 && p[2].trim().equalsIgnoreCase("status")));
            }
        } else {
            if (!s.listNameField.isEmpty()) s.filters.add(new Filter("name", s.listNameField, false));
            if (s.hasStatus) s.filters.add(new Filter("status", s.statusField, true));
        }
        return s;
    }

    // ------------------------------------------------------------------ helpers
    static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String javaStr(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static String safeSqlType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase();
        if (!Set.of("INT", "BIGINT", "SMALLINT", "TINYINT", "BIT", "DATE", "DATETIME", "DATETIME2",
                "NVARCHAR", "VARCHAR", "NCHAR", "CHAR", "DECIMAL", "NUMERIC", "FLOAT", "REAL").contains(type))
            throw new RuntimeException("Unsupported SQL type in config: " + value);
        return type;
    }

    static void write(String project, String relPath, String content) throws IOException {
        Path file = OUT.resolve(project).resolve(relPath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    static void writeRaw(String relPath, String content) throws IOException {
        Path file = OUT.resolve(relPath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    static String javaDir(Service s) {
        return "src/main/java/" + GROUP_PATH + "/" + s.pkg;
    }

    static String gettersSetters(String type, String name) {
        String C = cap(name);
        return "    public " + type + " get" + C + "() { return " + name + "; }\n" +
                "    public void set" + C + "(" + type + " v) { this." + name + " = v; }\n";
    }

    // ------------------------------------------------------------------ main
    public static void main(String[] args) throws Exception {
        String configPath = args.length > 0 ? args[0] : findDefaultConfig();
        if (!Files.exists(Paths.get(configPath))) {
            System.err.println("Config not found: " + configPath);
            System.err.println("Usage: java PEGen.java <pe-config.properties>");
            System.err.println("Tip: run from the folder containing pe-config.properties (e.g. 'tools').");
            return;
        }
        System.out.println("PEGen: using config '" + Paths.get(configPath).toAbsolutePath() + "'");
        try (InputStream in = Files.newInputStream(Paths.get(configPath))) {
            cfg.load(in);
        }
        GROUP = r("groupId");
        GROUP_PATH = GROUP.replace('.', '/');
        SBV = r("springBootVersion");
        SCV = r("springCloudVersion");
        SDV = r("springdocVersion");
        RESPONSE_STYLE = g("api.response.style", "timestamp").trim().toLowerCase();
        DBNAME = r("db.name");
        DBHOST = r("db.host");
        DBPORT = r("db.port");
        DBUSER = r("db.user");
        DBPASS = r("db.password");
        OUT = Paths.get(g("outputDir", "PEGEN_OUTPUT"));
        if (args.length >= 2 && !args[1].isBlank())
            OUT = Paths.get(args[1].trim()); // CLI override: java PEGen.java <config> <outDir>

        Service a = loadService("a");
        Service b = loadService("b");
        b.base = a;
        b.ref.dtoClass = a.entity + "DTO"; // B mirrors A's DTO under this class name
        validateConfiguration(a, b);
        claimOutputDirectory();

        // Safe clean: OUT must have an exact ownership marker; unrelated siblings are preserved.
        for (String sub : new String[]{a.project, b.project, r("g.project")})
            deleteRec(OUT.resolve(sub));

        writeSql(a, b);
        writeDataService(a);
        writeDataService(b);
        writeGateway();
        writeReadme(a, b);

        System.out.println("PEGen: generated into '" + OUT.toAbsolutePath() + "'");
        System.out.println("  - database/" + DBNAME + ".sql");
        System.out.println("  - " + a.project + "  (port " + a.port + ")");
        System.out.println("  - " + b.project + "  (port " + b.port + ", OpenFeign -> " + a.project + ")");
        System.out.println("  - " + r("g.project") + "  (port " + r("g.port") + ")");
    }

    /** Best-effort clean of previously generated SOURCES; never touches build/IDE dirs, never crashes. */
    /**
     * Find pe-config.properties whether PEGen is launched from project root, from tools/, etc.
     */
    static String findDefaultConfig() {
        for (String c : new String[]{"pe-config.properties", "tools/pe-config.properties", "../tools/pe-config.properties"})
            if (Files.exists(Paths.get(c))) return c;
        return "pe-config.properties";
    }

    static void deleteRec(Path p) {
        try {
            if (Files.isDirectory(p)) {
                String n = p.getFileName() == null ? "" : p.getFileName().toString();
                if (n.equals("target") || n.equals(".idea") || n.equals(".git")) return;
                try (var s = Files.list(p)) {
                    for (Path c : s.toList()) deleteRec(c);
                }
            }
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // file locked (e.g. open in IDE) — overwrite-on-write still keeps output correct
        }
    }

    static void claimOutputDirectory() throws IOException {
        Path absolute = OUT.toAbsolutePath().normalize();
        if (Files.exists(absolute) && !Files.isDirectory(absolute))
            throw new IOException("Output path is not a directory: " + absolute);
        Files.createDirectories(absolute);
        Path marker = absolute.resolve(OUTPUT_MARKER);
        if (Files.exists(marker)) {
            String content = Files.readString(marker, StandardCharsets.UTF_8).trim();
            if (!content.equals(OUTPUT_MARKER_CONTENT))
                throw new IOException("Output ownership marker is invalid: " + marker);
            return;
        }
        boolean empty;
        try (var children = Files.list(absolute)) {
            empty = children.findAny().isEmpty();
        }
        if (!empty)
            throw new IOException("Refusing to overwrite a non-empty unowned output directory: " + absolute
                    + ". Choose an empty directory.");
        Files.writeString(marker, OUTPUT_MARKER_CONTENT + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    static void validateConfiguration(Service a, Service b) {
        requireIdentifier(DBNAME, "db.name");
        if (!GROUP.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*"))
            throw new RuntimeException("Invalid groupId/package: " + GROUP);
        validateService(a);
        validateService(b);
        requireIdentifier(r("g.project"), "g.project");
        requireIdentifier(r("g.pkg"), "g.pkg");
        requirePort(r("g.port"), "g.port");
        Path absoluteOut = OUT.toAbsolutePath().normalize();
        if (absoluteOut.getParent() == null)
            throw new RuntimeException("Refusing to generate directly into a filesystem root: " + absoluteOut);
    }

    static void validateService(Service s) {
        requireIdentifier(s.project, s.key + ".project");
        requireIdentifier(s.pkg, s.key + ".pkg");
        requireIdentifier(s.entity, s.key + ".entity");
        requireIdentifier(s.controller, s.key + ".controller");
        requireIdentifier(s.table, s.key + ".table");
        requireIdentifier(s.idName, s.key + ".id.name");
        requireIdentifier(s.idColumn, s.key + ".id.column");
        requirePort(s.port, s.key + ".port");
        for (Field f : s.fields) {
            requireIdentifier(f.name, s.key + ".field name");
            requireIdentifier(f.column, s.key + ".field." + f.name + " column");
        }
        if (s.ref.enabled) requireIdentifier(s.ref.column, s.key + ".ref.column");
        if (s.lookup.enabled) {
            requireIdentifier(s.lookup.entity, s.key + ".lookup.entity");
            requireIdentifier(s.lookup.table, s.key + ".lookup.table");
            requireIdentifier(s.lookup.idName, s.key + ".lookup.id.name");
            requireIdentifier(s.lookup.idColumn, s.key + ".lookup.id.column");
            for (Field f : s.lookup.fields) requireIdentifier(f.column, s.key + ".lookup field column");
        }
    }

    static void requireIdentifier(String value, String key) {
        if (value == null || !value.matches("[A-Za-z_$][A-Za-z0-9_$]*"))
            throw new RuntimeException("Unsafe or unsupported identifier for " + key + ": " + value);
    }

    static void requirePort(String value, String key) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Invalid port for " + key + ": " + value);
        }
    }

    // ============================================================ SQL
    static void writeSql(Service a, Service b) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("-- SAFETY: this generated script never drops an existing exam table.\n")
                .append("-- Prefer the official SQL files when they were supplied with the question.\n")
                .append("-- If any target table already exists, use a fresh database or review it manually.\n\n");
        sb.append("IF DB_ID(N'").append(DBNAME).append("') IS NULL\nBEGIN\n    CREATE DATABASE ")
                .append(DBNAME).append(";\nEND\nGO\n\n");
        sb.append("USE ").append(DBNAME).append(";\nGO\n\n");
        sb.append("IF (OBJECT_ID(N'").append(b.table).append("', N'U') IS NOT NULL")
                .append(" OR OBJECT_ID(N'").append(a.table).append("', N'U') IS NOT NULL");
        if (a.lookup.enabled) sb.append(" OR OBJECT_ID(N'").append(a.lookup.table).append("', N'U') IS NOT NULL");
        sb.append(")\nBEGIN\n    THROW 50001, 'PEGen refused to overwrite an existing table. Use a fresh database or the official exam SQL.', 1;\nEND;\nGO\n\n");
        if (a.lookup.enabled) sb.append(createTable(asService(a.lookup)));
        sb.append(createTable(a));
        sb.append(createTable(b));
        writeRaw("database/" + DBNAME + ".sql", sb.toString());
    }

    static Service asService(Lookup l) {
        Service s = new Service();
        s.entity = l.entity;
        s.table = l.table;
        s.idName = l.idName;
        s.idColumn = l.idColumn;
        s.idSqlType = l.idSqlType;
        s.fields = l.fields;
        for (Field f : l.fields) {
            if (f.kind.equals("STATUS")) {
                s.hasStatus = true;
                s.statusField = f.name;
            }
        }
        return s;
    }

    /**
     * Column length for STRING/ENUM/STATUS — never smaller than the longest allowed enum/status value.
     */
    static int stringColLen(Field f, Service s) {
        int len = f.max > 0 ? f.max : (f.kind.equals("STATUS") ? 10 : 255);
        List<String> vals = f.kind.equals("STATUS") ? s.statusValues
                : (f.kind.equals("ENUM") ? f.enumValues : null);
        if (vals != null) {
            int longest = vals.stream().mapToInt(v -> v.trim().length()).max().orElse(0);
            if (longest > len) len = longest;
        }
        return len;
    }

    static String createTable(Service s) {
        List<String> cols = new ArrayList<>();
        List<String> cons = new ArrayList<>();
        cols.add("    " + s.idColumn + " " + s.idSqlType + " IDENTITY(1,1) NOT NULL");
        cons.add("    CONSTRAINT PK_" + s.table + " PRIMARY KEY (" + s.idColumn + ")");
        for (Field f : s.fields) {
            String nn = f.required ? " NOT NULL" : " NULL";
            String def = "";
            if (f.kind.equals("STATUS") && s.statusDefault != null && s.statusDefaultOnCreate)
                def = " CONSTRAINT DF_" + s.table + "_" + f.column + " DEFAULT N'" + s.statusDefault + "'";
            cols.add("    " + f.column + " " + fieldSqlType(f, s) + nn + def);
            if (f.unique)
                cons.add("    CONSTRAINT UQ_" + s.table + "_" + f.column + " UNIQUE (" + f.column + ")");
            if (f.pattern != null)
                cons.add("    CONSTRAINT CK_" + s.table + "_" + f.column +
                        "_fmt CHECK (" + f.column + " COLLATE Latin1_General_BIN2 NOT LIKE '%[^A-Za-z0-9]%')");
            if (f.kind.equals("STATUS") || f.kind.equals("ENUM")) {
                List<String> vals = f.kind.equals("STATUS") ? s.statusValues : f.enumValues;
                String in = String.join(", ", vals.stream().map(v -> "N'" + v.trim() + "'").toList());
                String guard = f.required ? "" : f.column + " IS NULL OR ";
                cons.add("    CONSTRAINT CK_" + s.table + "_" + f.column + " CHECK (" + guard + f.column + " IN (" + in + "))");
            }
            if (f.after != null)
                cons.add("    CONSTRAINT CK_" + s.table + "_" + f.column +
                        "_after CHECK (" + f.column + " IS NULL OR " + f.column + " > '" + f.after + "')");
            if (f.geField != null) {
                Field ge = s.fields.stream().filter(x -> x.name.equals(f.geField)).findFirst().orElse(null);
                if (ge != null)
                    cons.add("    CONSTRAINT CK_" + s.table + "_" + f.column +
                            "_order CHECK (" + f.column + " IS NULL OR " + f.column + " >= " + ge.column + ")");
            }
            if (f.fk != null && f.enforceFk) {
                String[] parts = f.fk.split("\\.");
                cons.add("    CONSTRAINT FK_" + s.table + "_" + f.column +
                        " FOREIGN KEY (" + f.column + ") REFERENCES " + parts[0] + "(" + parts[1] + ")");
            }
        }
        if (s.ref.enabled) {
            cols.add("    " + s.ref.column + " " + s.ref.sqlType + " NOT NULL");
            if (s.ref.enforceFk)
                cons.add("    CONSTRAINT FK_" + s.table + "_" + s.base.table +
                        " FOREIGN KEY (" + s.ref.column + ") REFERENCES " + s.base.table + "(" + s.base.idColumn + ")");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(s.table).append(" (\n");
        List<String> all = new ArrayList<>(cols);
        all.addAll(cons);
        sb.append(String.join(",\n", all)).append("\n);\nGO\n\n");
        return sb.toString();
    }

    static String fieldSqlType(Field f, Service s) {
        String type = f.sqlType;
        if (type == null) {
            type = switch (f.kind) {
                case "DATE" -> "DATE";
                case "DATETIME" -> "DATETIME2";
                case "INTEGER" -> "INT";
                case "LONG" -> "BIGINT";
                default -> "NVARCHAR";
            };
        }
        if (Set.of("NVARCHAR", "VARCHAR", "NCHAR", "CHAR").contains(type))
            return type + "(" + stringColLen(f, s) + ")";
        return type;
    }

    // ============================================================ DATA SERVICE
    static void writeDataService(Service s) throws IOException {
        writePom(s);
        writeProperties(s);
        writeMainApp(s);
        writeCommon(s);
        writeApiResponse(s.project, s.pkg);
        writePageDto(s.project, s.pkg);
        writeEntityDto(s);
        if (s.ref.enabled) writeRefDto(s);
        if (s.foodResponse) writeFoodResponseDtos(s);
        writeEntity(s);
        writeRepository(s);
        writeServiceInterface(s);
        writeServiceImpl(s);
        writeController(s);
        writeCors(s.project, s.pkg, false);
        writeOpenApi(s.project, s.pkg, s.project + " API");
        if (s.ref.enabled) writeFeignClient(s);
        if (s.lookup.enabled) writeLookup(s);
    }

    static void writePom(Service s) throws IOException {
        boolean feign = s.ref.enabled;
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>%SBV%</version>
                        <relativePath/>
                    </parent>
                    <groupId>%GROUP%</groupId>
                    <artifactId>%PROJECT%</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <name>%PROJECT%</name>
                    <packaging>jar</packaging>
                    <properties>
                        <java.version>21</java.version>
                """.replace("%SBV%", SBV).replace("%GROUP%", GROUP).replace("%PROJECT%", s.project));
        if (feign) sb.append("        <spring-cloud.version>").append(SCV).append("</spring-cloud.version>\n");
        sb.append("        <springdoc.version>").append(SDV).append("</springdoc.version>\n");
        sb.append("    </properties>\n");
        if (feign) sb.append("""
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.cloud</groupId>
                                <artifactId>spring-cloud-dependencies</artifactId>
                                <version>${spring-cloud.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                """);
        sb.append("    <dependencies>\n");
        sb.append(dep("org.springframework.boot", "spring-boot-starter-web", null, null));
        sb.append(dep("org.springframework.boot", "spring-boot-starter-data-jpa", null, null));
        sb.append(dep("org.springframework.boot", "spring-boot-starter-validation", null, null));
        sb.append(dep("org.projectlombok", "lombok", null, "provided"));
        sb.append(dep("org.springframework.boot", "spring-boot-devtools", null, "runtime"));
        if (feign) sb.append(dep("org.springframework.cloud", "spring-cloud-starter-openfeign", null, null));
        sb.append(dep("com.microsoft.sqlserver", "mssql-jdbc", null, "runtime"));
        sb.append(dep("org.springdoc", "springdoc-openapi-starter-webmvc-ui", "${springdoc.version}", null));
        sb.append(dep("org.springframework.boot", "spring-boot-starter-test", null, "test"));
        sb.append("    </dependencies>\n");
        sb.append(buildPlugins());
        sb.append("</project>\n");
        write(s.project, "pom.xml", sb.toString());
    }

    static String dep(String gid, String aid, String ver, String scope) {
        StringBuilder d = new StringBuilder("        <dependency>\n");
        d.append("            <groupId>").append(gid).append("</groupId>\n");
        d.append("            <artifactId>").append(aid).append("</artifactId>\n");
        if (ver != null) d.append("            <version>").append(ver).append("</version>\n");
        if (scope != null) d.append("            <scope>").append(scope).append("</scope>\n");
        d.append("        </dependency>\n");
        return d.toString();
    }

    static String buildPlugins() {
        return """
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <configuration>
                                    <release>21</release>
                                    <parameters>true</parameters>
                                </configuration>
                            </plugin>
                            <plugin>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-maven-plugin</artifactId>
                            </plugin>
                        </plugins>
                    </build>
                """;
    }

    static void writeProperties(Service s) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("spring.application.name=").append(s.project).append("\n");
        sb.append("server.port=").append(s.port).append("\n\n");
        sb.append("spring.datasource.url=jdbc:sqlserver://").append(DBHOST).append(":").append(DBPORT)
                .append(";databaseName=").append(DBNAME).append(";encrypt=false;\n");
        sb.append("spring.datasource.username=").append(DBUSER).append("\n");
        sb.append("spring.datasource.password=").append(DBPASS).append("\n");
        sb.append("spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver\n\n");
        sb.append("spring.jpa.hibernate.ddl-auto=none\n");
        sb.append("spring.jpa.open-in-view=false\n");
        sb.append("spring.jpa.show-sql=true\n");
        sb.append("spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect\n\n");
        sb.append("spring.jackson.time-zone=UTC\n");
        sb.append("spring.jackson.date-format=yyyy-MM-dd\n\n");
        if (s.ref.enabled)
            sb.append(s.ref.serviceUrlProp).append("=").append(s.ref.serviceUrl).append("\n\n");
        sb.append("springdoc.api-docs.path=/v3/api-docs\n");
        sb.append("springdoc.swagger-ui.path=/swagger-ui.html\n");
        write(s.project, "src/main/resources/application.properties", sb.toString());
    }

    static void writeMainApp(Service s) throws IOException {
        String cls = s.project + "Application";
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(GROUP).append(".").append(s.pkg).append(";\n\n");
        sb.append("import org.springframework.boot.SpringApplication;\n");
        sb.append("import org.springframework.boot.autoconfigure.SpringBootApplication;\n");
        if (s.ref.enabled) sb.append("import org.springframework.cloud.openfeign.EnableFeignClients;\n");
        sb.append("\n@SpringBootApplication\n");
        if (s.ref.enabled)
            sb.append("@EnableFeignClients(basePackages = \"").append(GROUP).append(".").append(s.pkg).append(".config\")\n");
        sb.append("public class ").append(cls).append(" {\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        SpringApplication.run(").append(cls).append(".class, args);\n");
        sb.append("    }\n}\n");
        write(s.project, javaDir(s) + "/" + cls + ".java", sb.toString());
    }

    static void writeCommon(Service s) throws IOException {
        String base = GROUP + "." + s.pkg + ".common";
        // ResponseStatuses (always include 0..4)
        write(s.project, javaDir(s) + "/common/ResponseStatuses.java",
                "package " + base + ";\n\n" +
                        "public final class ResponseStatuses {\n" +
                        "    public static final int INTERNAL_ERROR = 0;\n" +
                        "    public static final int SUCCESS = 1;\n" +
                        "    public static final int VALIDATION_FAILED = 2;\n" +
                        "    public static final int DUPLICATE_CODE = 3;\n" +
                        "    public static final int NOT_FOUND = 4;\n" +
                        "    private ResponseStatuses() {}\n}\n");
        // BusinessException
        write(s.project, javaDir(s) + "/common/BusinessException.java",
                "package " + base + ";\n\n" +
                        "import org.springframework.http.HttpStatus;\n\n" +
                        "public class BusinessException extends RuntimeException {\n" +
                        "    private final int apiStatus;\n" +
                        "    private final HttpStatus httpStatus;\n" +
                        "    public BusinessException(int apiStatus, HttpStatus httpStatus, String message) {\n" +
                        "        super(message);\n" +
                        "        this.apiStatus = apiStatus;\n" +
                        "        this.httpStatus = httpStatus;\n" +
                        "    }\n" +
                        "    public int getApiStatus() { return apiStatus; }\n" +
                        "    public HttpStatus getHttpStatus() { return httpStatus; }\n}\n");
        // GlobalExceptionHandler
        write(s.project, javaDir(s) + "/common/GlobalExceptionHandler.java",
                "package " + base + ";\n\n" +
                        "import " + GROUP + "." + s.pkg + ".dto.ApiResponseDTO;\n" +
                        "import jakarta.validation.ConstraintViolationException;\n" +
                        "import org.springframework.http.HttpStatus;\n" +
                        "import org.springframework.http.ResponseEntity;\n" +
                        "import org.springframework.http.converter.HttpMessageNotReadableException;\n" +
                        "import org.springframework.web.bind.MethodArgumentNotValidException;\n" +
                        "import org.springframework.web.bind.annotation.ExceptionHandler;\n" +
                        "import org.springframework.web.bind.annotation.RestControllerAdvice;\n" +
                        "import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;\n\n" +
                        "@RestControllerAdvice\n" +
                        "public class GlobalExceptionHandler {\n\n" +
                        "    @ExceptionHandler(BusinessException.class)\n" +
                        "    public ResponseEntity<ApiResponseDTO<Void>> handleBusiness(BusinessException ex) {\n" +
                        "        return ResponseEntity.status(ex.getHttpStatus()).body(ApiResponseDTO.of(ex.getApiStatus(), null));\n" +
                        "    }\n\n" +
                        "    @ExceptionHandler({\n" +
                        "        MethodArgumentNotValidException.class, ConstraintViolationException.class,\n" +
                        "        MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,\n" +
                        "        IllegalArgumentException.class\n" +
                        "    })\n" +
                        "    public ResponseEntity<ApiResponseDTO<Void>> handleValidation(Exception ex) {\n" +
                        "        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponseDTO.of(ResponseStatuses.VALIDATION_FAILED, null));\n" +
                        "    }\n\n" +
                        "    @ExceptionHandler(Exception.class)\n" +
                        "    public ResponseEntity<ApiResponseDTO<Void>> handleOther(Exception ex) {\n" +
                        "        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseDTO.of(ResponseStatuses.INTERNAL_ERROR, null));\n" +
                        "    }\n}\n");
    }

    static void writeApiResponse(String project, String pkg) throws IOException {
        String base = "src/main/java/" + GROUP_PATH + "/" + pkg + "/dto/ApiResponseDTO.java";
        if (RESPONSE_STYLE.equals("message")) {
            write(project, base,
                    "package " + GROUP + "." + pkg + ".dto;\n\n" +
                            "import com.fasterxml.jackson.annotation.JsonInclude;\n\n" +
                            "@JsonInclude(JsonInclude.Include.ALWAYS)\n" +
                            "public class ApiResponseDTO<T> {\n" +
                            "    private int status;\n" +
                            "    private String message;\n" +
                            "    private T data;\n\n" +
                            "    public ApiResponseDTO() {}\n" +
                            "    public ApiResponseDTO(int status, T data) {\n" +
                            "        this.status = status; this.message = messageFor(status); this.data = data;\n" +
                            "    }\n" +
                            "    public static <T> ApiResponseDTO<T> success(T data) { return new ApiResponseDTO<>(1, data); }\n" +
                            "    public static <T> ApiResponseDTO<T> of(int status, T data) { return new ApiResponseDTO<>(status, data); }\n" +
                            "    private static String messageFor(int status) { return switch (status) {\n" +
                            "        case 1 -> \"Successful\"; case 2 -> \"Data validation failed\";\n" +
                            "        case 3 -> \"Duplicated data\"; case 4 -> \"Data is not found\";\n" +
                            "        default -> \"Internal server error\"; }; }\n\n" +
                            "    public int getStatus() { return status; } public void setStatus(int v) { status = v; }\n" +
                            "    public String getMessage() { return message; } public void setMessage(String v) { message = v; }\n" +
                            "    public T getData() { return data; } public void setData(T v) { data = v; }\n}\n");
            return;
        }
        write(project, base,
                "package " + GROUP + "." + pkg + ".dto;\n\n" +
                        "import com.fasterxml.jackson.annotation.JsonInclude;\n" +
                        "import java.time.Instant;\n" +
                        "import java.time.temporal.ChronoUnit;\n\n" +
                        "@JsonInclude(JsonInclude.Include.ALWAYS)\n" +
                        "public class ApiResponseDTO<T> {\n" +
                        "    private int status;\n" +
                        "    private T data;\n" +
                        "    private String timestamp;\n\n" +
                        "    public ApiResponseDTO() {}\n" +
                        "    public ApiResponseDTO(int status, T data) {\n" +
                        "        this.status = status; this.data = data;\n" +
                        "        this.timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();\n" +
                        "    }\n" +
                        "    public static <T> ApiResponseDTO<T> success(T data) { return new ApiResponseDTO<>(1, data); }\n" +
                        "    public static <T> ApiResponseDTO<T> of(int status, T data) { return new ApiResponseDTO<>(status, data); }\n\n" +
                        "    public int getStatus() { return status; } public void setStatus(int v) { status = v; }\n" +
                        "    public T getData() { return data; } public void setData(T v) { data = v; }\n" +
                        "    public String getTimestamp() { return timestamp; } public void setTimestamp(String v) { timestamp = v; }\n}\n");
    }

    static void writePageDto(String project, String pkg) throws IOException {
        write(project, "src/main/java/" + GROUP_PATH + "/" + pkg + "/dto/PageDTO.java",
                "package " + GROUP + "." + pkg + ".dto;\n\n" +
                        "import org.springframework.data.domain.Page;\n" +
                        "import java.util.List;\n\n" +
                        "public class PageDTO<T> {\n" +
                        "    private int size;\n    private int page;\n    private int totalPages;\n" +
                        "    private long totalElements;\n    private boolean first;\n    private boolean last;\n" +
                        "    private List<T> content;\n\n" +
                        "    public PageDTO() {}\n" +
                        "    public PageDTO(Page<T> p) {\n" +
                        "        this.size = p.getSize();\n        this.page = p.getNumber();\n" +
                        "        this.totalPages = p.getTotalPages();\n        this.totalElements = p.getTotalElements();\n" +
                        "        this.first = p.isFirst();\n        this.last = p.isLast();\n        this.content = p.getContent();\n" +
                        "    }\n" +
                        "    public int getSize() { return size; } public void setSize(int v) { this.size = v; }\n" +
                        "    public int getPage() { return page; } public void setPage(int v) { this.page = v; }\n" +
                        "    public int getTotalPages() { return totalPages; } public void setTotalPages(int v) { this.totalPages = v; }\n" +
                        "    public long getTotalElements() { return totalElements; } public void setTotalElements(long v) { this.totalElements = v; }\n" +
                        "    public boolean isFirst() { return first; } public void setFirst(boolean v) { this.first = v; }\n" +
                        "    public boolean isLast() { return last; } public void setLast(boolean v) { this.last = v; }\n" +
                        "    public List<T> getContent() { return content; } public void setContent(List<T> v) { this.content = v; }\n}\n");
    }

    static String dtoBody(String pkg, String className, String idName, List<Field> fields, Ref ref) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(GROUP).append(".").append(pkg).append(".dto;\n\n");
        sb.append("import com.fasterxml.jackson.annotation.JsonFormat;\n");
        sb.append("import jakarta.validation.constraints.Email;\n");
        sb.append("import jakarta.validation.constraints.Pattern;\n");
        sb.append("import jakarta.validation.constraints.Size;\n");
        sb.append("import lombok.Getter;\n");
        sb.append("import lombok.Setter;\n");
        sb.append("import java.util.Date;\n\n");
        sb.append("@Getter\n@Setter\n");
        sb.append("public class ").append(className).append(" {\n");
        sb.append("    private Long ").append(idName).append(";\n");
        for (Field f : fields) {
            if (f.kind.equals("DATE"))
                sb.append("    @JsonFormat(pattern = \"").append(javaStr(f.jsonFormat == null ? "yyyy-MM-dd" : f.jsonFormat)).append("\", timezone = \"UTC\")\n");
            if (f.kind.equals("DATETIME"))
                sb.append("    @JsonFormat(pattern = \"").append(javaStr(f.jsonFormat == null ? "yyyy-MM-dd'T'HH:mm:ssX" : f.jsonFormat)).append("\", timezone = \"UTC\")\n");
            if (f.max > 0 && (f.kind.equals("STRING") || f.kind.equals("ENUM") || f.kind.equals("STATUS")))
                sb.append("    @Size(max = ").append(f.max).append(")\n");
            if (f.email) sb.append("    @Email\n");
            if (f.pattern != null) sb.append("    @Pattern(regexp = \"").append(javaStr(f.pattern)).append("\")\n");
            sb.append("    private ").append(f.type()).append(" ").append(f.name).append(";\n");
        }
        if (ref != null && ref.enabled) {
            if (ref.inputId) sb.append("    private Long ").append(ref.idProp).append(";\n");
            if (ref.inputObject)
                sb.append("    private ").append(ref.dtoClass).append(" ").append(ref.objectProp).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    static void writeEntityDto(Service s) throws IOException {
        write(s.project, javaDir(s) + "/dto/" + s.entity + "DTO.java",
                dtoBody(s.pkg, s.entity + "DTO", s.idName, s.fields, s.ref.enabled ? s.ref : null));
    }

    static void writeRefDto(Service s) throws IOException {
        // mirror base service DTO under B's dto package
        Service a = s.base;
        write(s.project, javaDir(s) + "/dto/" + s.ref.dtoClass + ".java",
                dtoBody(s.pkg, s.ref.dtoClass, a.idName, a.fields, null));
    }

    static void writeFoodResponseDtos(Service s) throws IOException {
        StringBuilder dto = new StringBuilder();
        dto.append("package ").append(GROUP).append(".").append(s.pkg).append(".dto;\n\n")
                .append("import com.fasterxml.jackson.annotation.JsonFormat;\nimport lombok.Getter;\nimport lombok.Setter;\nimport java.util.Date;\n\n")
                .append("@Getter\n@Setter\npublic class ").append(s.entity).append("ResponseDTO {\n")
                .append("    private Long ").append(s.idName).append(";\n");
        for (String name : s.foodResponseFields) {
            Field f = s.fields.stream().filter(x -> x.name.equals(name)).findFirst()
                    .orElseThrow(() -> new RuntimeException("Unknown food response field: " + name));
            if (f.kind.equals("DATE"))
                dto.append("    @JsonFormat(pattern = \"").append(javaStr(f.jsonFormat == null ? "yyyy-MM-dd" : f.jsonFormat)).append("\", timezone = \"UTC\")\n");
            if (f.kind.equals("DATETIME"))
                dto.append("    @JsonFormat(pattern = \"").append(javaStr(f.jsonFormat == null ? "yyyy-MM-dd'T'HH:mm:ssX" : f.jsonFormat)).append("\", timezone = \"UTC\")\n");
            dto.append("    private ").append(f.type()).append(" ").append(f.name).append(";\n");
        }
        dto.append("    private ").append(s.ref.dtoClass).append(" ").append(s.ref.objectProp).append(";\n}\n");
        write(s.project, javaDir(s) + "/dto/" + s.entity + "ResponseDTO.java", dto.toString());

        write(s.project, javaDir(s) + "/dto/" + s.entity + "ListDTO.java",
                "package " + GROUP + "." + s.pkg + ".dto;\n\n" +
                        "import org.springframework.data.domain.Page;\nimport java.util.List;\n\n" +
                        "public class " + s.entity + "ListDTO {\n" +
                        "    private int pageSize; private int pageNo; private int totalPages;\n" +
                        "    private boolean first; private boolean last;\n" +
                        "    private List<" + s.entity + "ResponseDTO> " + s.entity.toLowerCase() + "s;\n" +
                        "    public " + s.entity + "ListDTO() {}\n" +
                        "    public " + s.entity + "ListDTO(Page<" + s.entity + "ResponseDTO> p) {\n" +
                        "        pageSize=p.getSize(); pageNo=p.getNumber(); totalPages=p.getTotalPages();\n" +
                        "        first=p.isFirst(); last=p.isLast(); " + s.entity.toLowerCase() + "s=p.getContent();\n" +
                        "    }\n" +
                        "    public int getPageSize(){return pageSize;} public void setPageSize(int v){pageSize=v;}\n" +
                        "    public int getPageNo(){return pageNo;} public void setPageNo(int v){pageNo=v;}\n" +
                        "    public int getTotalPages(){return totalPages;} public void setTotalPages(int v){totalPages=v;}\n" +
                        "    public boolean isFirst(){return first;} public void setFirst(boolean v){first=v;}\n" +
                        "    public boolean isLast(){return last;} public void setLast(boolean v){last=v;}\n" +
                        "    public List<" + s.entity + "ResponseDTO> get" + s.entity + "s(){return " + s.entity.toLowerCase() + "s;}\n" +
                        "    public void set" + s.entity + "s(List<" + s.entity + "ResponseDTO> v){" + s.entity.toLowerCase() + "s=v;}\n" +
                        "}\n");
    }

    static void writeEntity(Service s) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(GROUP).append(".").append(s.pkg).append(".entity;\n\n");
        sb.append("import jakarta.persistence.*;\n");
        sb.append("import lombok.Getter;\nimport lombok.Setter;\n");
        sb.append("import java.util.Date;\n\n");
        sb.append("@Getter\n@Setter\n@Entity\n@Table(name = \"").append(s.table).append("\")\n");
        sb.append("public class ").append(s.entity).append(" {\n\n");
        sb.append("    @Id\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
        sb.append("    @Column(name = \"").append(s.idColumn).append("\")\n");
        sb.append("    private Long ").append(s.idName).append(";\n\n");
        for (Field f : s.fields) {
            if (f.kind.equals("DATE")) sb.append("    @Temporal(TemporalType.DATE)\n");
            if (f.kind.equals("DATETIME")) sb.append("    @Temporal(TemporalType.TIMESTAMP)\n");
            StringBuilder col = new StringBuilder("    @Column(name = \"" + f.column + "\"");
            if (f.required) col.append(", nullable = false");
            if (f.unique) col.append(", unique = true");
            if (!f.kind.equals("DATE") && !f.kind.equals("DATETIME") && !f.kind.equals("LONG") && !f.kind.equals("INTEGER") && f.max > 0)
                col.append(", length = ").append(f.max);
            col.append(")\n");
            sb.append(col);
            sb.append("    private ").append(f.type()).append(" ").append(f.name).append(";\n\n");
        }
        if (s.ref.enabled) {
            sb.append("    @Column(name = \"").append(s.ref.column).append("\", nullable = false)\n");
            sb.append("    private Long ").append(s.ref.idProp).append(";\n\n");
        }
        sb.append("}\n");
        write(s.project, javaDir(s) + "/entity/" + s.entity + ".java", sb.toString());
    }

    static void writeRepository(Service s) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(GROUP).append(".").append(s.pkg).append(".repository;\n\n");
        sb.append("import ").append(GROUP).append(".").append(s.pkg).append(".entity.").append(s.entity).append(";\n");
        sb.append("import org.springframework.data.jpa.repository.JpaRepository;\n");
        sb.append("import org.springframework.data.jpa.repository.JpaSpecificationExecutor;\n\n");
        sb.append("public interface ").append(s.entity).append("Repository extends JpaRepository<")
                .append(s.entity).append(", Long>, JpaSpecificationExecutor<").append(s.entity).append("> {\n");
        for (Field f : s.fields) {
            if (f.unique) {
                String U = cap(f.name), Id = cap(s.idName);
                sb.append("    boolean existsBy").append(U).append("IgnoreCase(String v);\n");
                sb.append("    boolean existsBy").append(U).append("IgnoreCaseAnd").append(Id).append("Not(String v, Long id);\n");
            }
        }
        sb.append("}\n");
        write(s.project, javaDir(s) + "/repository/" + s.entity + "Repository.java", sb.toString());
    }

    static void writeServiceInterface(Service s) throws IOException {
        String E = s.entity;
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(GROUP).append(".").append(s.pkg).append(".service;\n\n");
        sb.append("import ").append(GROUP).append(".").append(s.pkg).append(".dto.*;\n\n");
        sb.append("public interface ").append(E).append("Service {\n");
        sb.append("    ").append(E).append("DTO create(").append(E).append("DTO dto);\n");
        sb.append("    ").append(E).append("DTO update(Long id, ").append(E).append("DTO dto);\n");
        sb.append("    ").append(s.foodResponse ? E + "ResponseDTO" : E + "DTO").append(" get(Long id);\n");
        if (s.hasStatus) sb.append("    void deactivate(Long id);\n");
        if (s.foodResponse) sb.append("    ").append(E).append("ListDTO list(Integer page, Integer size");
        else sb.append("    PageDTO<").append(E).append("DTO> list(Integer page, Integer size");
        for (Filter f : s.filters) sb.append(", String ").append(f.query);
        sb.append(");\n");
        sb.append("}\n");
        write(s.project, javaDir(s) + "/service/" + E + "Service.java", sb.toString());
    }

    // ---- the heart: service implementation ----
    static void writeServiceImpl(Service s) throws IOException {
        String E = s.entity, P = GROUP + "." + s.pkg;
        boolean feign = s.ref.enabled;
        Field unique = s.fields.stream().filter(f -> f.unique).findFirst().orElse(null);
        Field lookupFk = s.lookup.enabled
                ? s.fields.stream().filter(f -> f.fk != null && f.fk.toLowerCase().startsWith(s.lookup.table.toLowerCase() + ".")).findFirst().orElse(null)
                : null;
        Field selfFk = s.fields.stream()
                .filter(f -> f.fk != null && f.enforceFk && f.fk.toLowerCase().startsWith(s.table.toLowerCase() + "."))
                .findFirst().orElse(null);
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(P).append(".service.impl;\n\n");
        if (feign) {
            sb.append("import com.fasterxml.jackson.databind.ObjectMapper;\n");
            sb.append("import feign.FeignException;\n");
        }
        sb.append("import ").append(P).append(".common.BusinessException;\n");
        sb.append("import ").append(P).append(".common.ResponseStatuses;\n");
        if (feign) sb.append("import ").append(P).append(".config.").append(s.base.entity).append("Client;\n");
        sb.append("import ").append(P).append(".dto.*;\n");
        sb.append("import ").append(P).append(".entity.").append(E).append(";\n");
        sb.append("import ").append(P).append(".repository.").append(E).append("Repository;\n");
        if (lookupFk != null)
            sb.append("import ").append(P).append(".repository.").append(s.lookup.entity).append("Repository;\n");
        sb.append("import ").append(P).append(".service.").append(E).append("Service;\n");
        sb.append("import jakarta.persistence.criteria.Predicate;\n");
        sb.append("import org.springframework.data.domain.*;\n");
        sb.append("import org.springframework.data.jpa.domain.Specification;\n");
        sb.append("import org.springframework.http.HttpStatus;\n");
        sb.append("import org.springframework.stereotype.Service;\n");
        sb.append("import org.springframework.transaction.annotation.Transactional;\n\n");
        sb.append("import java.time.*;\n");
        sb.append("import java.util.*;\n");
        sb.append("import java.util.regex.Pattern;\n\n");

        sb.append("@Service\n");
        sb.append("public class ").append(E).append("ServiceImpl implements ").append(E).append("Service {\n\n");
        sb.append("    private static final int MAX_PAGE_SIZE = 100;\n");
        sb.append("    private static final Pattern EMAIL_PATTERN = Pattern.compile(\"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$\");\n");
        if (s.hasStatus) {
            String vals = String.join(", ", s.statusValues.stream().map(v -> "\"" + v.trim() + "\"").toList());
            sb.append("    private static final Set<String> STATUS_VALUES = Set.of(").append(vals).append(");\n");
            sb.append("    private static final String STATUS_DEFAULT = \"").append(s.statusDefault).append("\";\n");
            sb.append("    private static final String STATUS_INACTIVE = \"").append(s.statusInactive).append("\";\n");
        }
        for (Field f : s.fields) {
            if (f.pattern != null)
                sb.append("    private static final Pattern ").append(f.name.toUpperCase()).append("_PATTERN = Pattern.compile(\"")
                        .append(javaStr(f.pattern)).append("\");\n");
            if (f.kind.equals("ENUM")) {
                String vals = String.join(", ", f.enumValues.stream().map(v -> "\"" + v.trim() + "\"").toList());
                sb.append("    private static final Set<String> ").append(f.name.toUpperCase()).append("_VALUES = Set.of(").append(vals).append(");\n");
            }
        }
        sb.append("\n");
        // constructor / fields
        sb.append("    private final ").append(E).append("Repository repo;\n");
        if (lookupFk != null)
            sb.append("    private final ").append(s.lookup.entity).append("Repository lookupRepo;\n");
        if (feign) {
            sb.append("    private final ").append(s.base.entity).append("Client refClient;\n");
            sb.append("    private final ObjectMapper mapper;\n");
            sb.append("    public ").append(E).append("ServiceImpl(").append(E).append("Repository repo, ")
                    .append(s.base.entity).append("Client refClient, ObjectMapper mapper");
            if (lookupFk != null) sb.append(", ").append(s.lookup.entity).append("Repository lookupRepo");
            sb.append(") {\n");
            sb.append("        this.repo = repo; this.refClient = refClient; this.mapper = mapper;");
            if (lookupFk != null) sb.append(" this.lookupRepo = lookupRepo;");
            sb.append("\n    }\n\n");
        } else {
            sb.append("    public ").append(E).append("ServiceImpl(").append(E).append("Repository repo");
            if (lookupFk != null) sb.append(", ").append(s.lookup.entity).append("Repository lookupRepo");
            sb.append(") { this.repo = repo;");
            if (lookupFk != null) sb.append(" this.lookupRepo = lookupRepo;");
            sb.append(" }\n\n");
        }

        String refObjArg = feign ? ", refObj" : "";
        String refDtoDecl = feign ? s.ref.dtoClass + " refObj" : "";

        // create
        sb.append("    @Override @Transactional\n");
        sb.append("    public ").append(E).append("DTO create(").append(E).append("DTO dto) {\n");
        sb.append("        validateCreate(dto);\n");
        if (lookupFk != null)
            sb.append("        validateLookup(dto.get").append(cap(lookupFk.name)).append("());\n");
        if (selfFk != null)
            sb.append("        validateSelfReference(dto.get").append(cap(selfFk.name)).append("(), null);\n");
        if (unique != null) {
            sb.append("        String _u = clean(dto.get").append(cap(unique.name)).append("());\n");
            sb.append("        if (repo.existsBy").append(cap(unique.name)).append("IgnoreCase(_u)) throw duplicate();\n");
        }
        if (feign) {
            sb.append("        Long _refId = resolveRefId(dto, true);\n");
            sb.append("        ").append(s.ref.dtoClass).append(" refObj = fetchRef(_refId);\n");
        }
        sb.append("        ").append(E).append(" e = new ").append(E).append("();\n");
        for (Field f : s.fields) sb.append(setterFromDto(f, "e", "dto", unique));
        if (s.hasStatus) {
            if (s.statusDefaultOnCreate)
                sb.append("        e.set").append(cap(s.statusField)).append("(STATUS_DEFAULT);\n");
            else
                sb.append("        e.set").append(cap(s.statusField)).append("(normalizeStatus(dto.get")
                        .append(cap(s.statusField)).append("()));\n");
        }
        if (feign)
            sb.append("        e.set").append(cap(s.ref.idProp)).append("(refObj.get").append(cap(s.base.idName)).append("());\n");
        sb.append("        return toDTO(repo.save(e)").append(refObjArg).append(");\n");
        sb.append("    }\n\n");

        // update
        sb.append("    @Override @Transactional\n");
        sb.append("    public ").append(E).append("DTO update(Long id, ").append(E).append("DTO dto) {\n");
        sb.append("        ").append(E).append(" e = findOrThrow(id);\n");
        sb.append("        validateUpdate(dto, e);\n");
        if (lookupFk != null)
            sb.append("        if (dto.get").append(cap(lookupFk.name)).append("() != null) validateLookup(dto.get")
                    .append(cap(lookupFk.name)).append("());\n");
        if (selfFk != null)
            sb.append("        if (dto.get").append(cap(selfFk.name)).append("() != null) validateSelfReference(dto.get")
                    .append(cap(selfFk.name)).append("(), id);\n");
        if (unique != null) {
            sb.append("        if (dto.get").append(cap(unique.name)).append("() != null) {\n");
            sb.append("            String candidate = clean(dto.get").append(cap(unique.name)).append("());\n");
            sb.append("            if (repo.existsBy").append(cap(unique.name)).append("IgnoreCaseAnd")
                    .append(cap(s.idName)).append("Not(candidate, id)) throw duplicate();\n");
            sb.append("        }\n");
        }
        if (feign) {
            sb.append("        ").append(s.ref.dtoClass).append(" refObj = null;\n");
            sb.append("        Long _refId = resolveRefId(dto, false);\n");
            sb.append("        if (_refId != null) { refObj = fetchRef(_refId); e.set").append(cap(s.ref.idProp)).append("(refObj.get").append(cap(s.base.idName)).append("()); }\n");
        }
        for (Field f : s.fields) {
            if (f.kind.equals("STATUS")) {
                sb.append("        if (dto.get").append(cap(f.name)).append("() != null) e.set")
                        .append(cap(f.name)).append("(normalizeStatus(dto.get").append(cap(f.name)).append("()));\n");
            } else {
                sb.append("        if (dto.get").append(cap(f.name)).append("() != null) ")
                        .append(setterExpr(f, "e", "dto")).append(";\n");
            }
        }
        if (feign) {
            sb.append("        ").append(E).append(" saved = repo.save(e);\n");
            sb.append("        return toDTO(saved, refObj == null ? fetchRef(saved.get").append(cap(s.ref.idProp)).append("()) : refObj);\n");
        } else {
            sb.append("        return toDTO(repo.save(e));\n");
        }
        sb.append("    }\n\n");

        // get
        sb.append("    @Override @Transactional(readOnly = true)\n");
        sb.append("    public ").append(s.foodResponse ? E + "ResponseDTO" : E + "DTO").append(" get(Long id) {\n");
        if (feign) {
            sb.append("        ").append(E).append(" e = findOrThrow(id);\n");
            sb.append("        return ").append(s.foodResponse ? "toResponseDTO" : "toDTO").append("(e, fetchRef(e.get")
                    .append(cap(s.ref.idProp)).append("()));\n");
        } else {
            sb.append("        return toDTO(findOrThrow(id));\n");
        }
        sb.append("    }\n\n");

        // deactivate
        if (s.hasStatus) {
            sb.append("    @Override @Transactional\n");
            sb.append("    public void deactivate(Long id) {\n");
            sb.append("        ").append(E).append(" e = findOrThrow(id);\n");
            sb.append("        e.set").append(cap(s.statusField)).append("(STATUS_INACTIVE);\n");
            sb.append("        repo.save(e);\n    }\n\n");
        }

        // list
        sb.append("    @Override @Transactional(readOnly = true)\n");
        if (s.foodResponse) sb.append("    public ").append(E).append("ListDTO list(Integer page, Integer size");
        else sb.append("    public PageDTO<").append(E).append("DTO> list(Integer page, Integer size");
        for (Filter f : s.filters) sb.append(", String ").append(f.query);
        sb.append(") {\n");
        sb.append("        int p = page == null ? 0 : page;\n");
        sb.append("        int sz = size == null ? 10 : size;\n");
        sb.append("        validatePagination(p, sz);\n");
        for (Filter f : s.filters) {
            String local = "filter" + cap(f.query);
            if (f.status)
                sb.append("        String ").append(local).append(" = (").append(f.query).append(" == null || ").append(f.query)
                        .append(".isBlank()) ? null : normalizeStatus(").append(f.query).append(");\n");
            else
                sb.append("        String ").append(local).append(" = (").append(f.query).append(" == null || ").append(f.query)
                        .append(".isBlank()) ? null : ").append(f.query).append(".trim().toLowerCase(Locale.ROOT);\n");
        }
        sb.append("        Specification<").append(E).append("> spec = (root, q, cb) -> {\n");
        sb.append("            List<Predicate> ps = new ArrayList<>();\n");
        for (Filter f : s.filters) {
            String local = "filter" + cap(f.query);
            if (f.status)
                sb.append("            if (").append(local).append(" != null) ps.add(cb.equal(root.get(\"").append(f.field)
                        .append("\"), ").append(local).append("));\n");
            else
                sb.append("            if (").append(local).append(" != null) ps.add(cb.like(cb.lower(root.get(\"").append(f.field)
                        .append("\")), \"%\" + ").append(local).append(" + \"%\"));\n");
        }
        sb.append("            return cb.and(ps.toArray(new Predicate[0]));\n");
        sb.append("        };\n");
        sb.append("        Page<").append(s.foodResponse ? E + "ResponseDTO" : E + "DTO")
                .append("> result = repo.findAll(spec, PageRequest.of(p, sz, Sort.by(\"").append(s.idName).append("\").ascending()))\n");
        if (feign)
            sb.append("                .map(e -> ").append(s.foodResponse ? "toResponseDTO" : "toDTO")
                    .append("(e, fetchRef(e.get").append(cap(s.ref.idProp)).append("())));\n");
        else
            sb.append("                .map(this::toDTO);\n");
        if (s.foodResponse) sb.append("        return new ").append(E).append("ListDTO(result);\n");
        else sb.append("        return new PageDTO<>(result);\n");
        sb.append("    }\n\n");

        // validateCreate
        sb.append("    private void validateCreate(").append(E).append("DTO dto) {\n");
        sb.append("        if (dto == null) throw validationFailed();\n");
        for (Field f : s.fields) sb.append(validateLine(f, true, s));
        if (feign) sb.append("        resolveRefId(dto, true);\n");
        sb.append("    }\n\n");

        // validateUpdate
        sb.append("    private void validateUpdate(").append(E).append("DTO dto, ").append(E).append(" current) {\n");
        sb.append("        if (dto == null) throw validationFailed();\n");
        for (Field f : s.fields) sb.append(validateLine(f, false, s));
        sb.append("    }\n\n");

        // findOrThrow
        sb.append("    private ").append(E).append(" findOrThrow(Long id) {\n");
        sb.append("        if (id == null || id <= 0) throw notFound();\n");
        sb.append("        return repo.findById(id).orElseThrow(this::notFound);\n    }\n\n");

        if (unique != null) {
            // duplicate check on update handled via re-check helper used inside update setter? simpler: check in update before set.
        }

        // ref helpers
        if (feign) {
            sb.append("    private Long resolveRefId(").append(E).append("DTO dto, boolean required) {\n");
            if (s.ref.inputId)
                sb.append("        Long id = dto.get").append(cap(s.ref.idProp)).append("();\n");
            else
                sb.append("        Long id = null;\n");
            if (s.ref.inputObject)
                sb.append("        if (id == null && dto.get").append(cap(s.ref.objectProp)).append("() != null) id = dto.get")
                        .append(cap(s.ref.objectProp)).append("().get").append(cap(s.base.idName)).append("();\n");
            sb.append("        if (id == null) { if (required) throw validationFailed(); return null; }\n");
            sb.append("        if (id <= 0) throw validationFailed();\n");
            sb.append("        return id;\n    }\n\n");
            sb.append("    private ").append(s.ref.dtoClass).append(" fetchRef(Long id) {\n");
            sb.append("        try {\n");
            sb.append("            ApiResponseDTO<Object> r = refClient.get").append(s.base.entity).append("(id);\n");
            sb.append("            if (r == null || r.getStatus() != ResponseStatuses.SUCCESS || r.getData() == null) throw refNotFound();\n");
            sb.append("            return mapper.convertValue(r.getData(), ").append(s.ref.dtoClass).append(".class);\n");
            sb.append("        } catch (FeignException.BadRequest | FeignException.NotFound ex) {\n");
            sb.append("            throw refNotFound();\n");
            sb.append("        } catch (BusinessException ex) {\n            throw ex;\n");
            sb.append("        } catch (FeignException ex) {\n");
            sb.append("            throw new BusinessException(ResponseStatuses.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, \"Reference service error\");\n");
            sb.append("        }\n    }\n\n");
            sb.append("    private BusinessException refNotFound() { return new BusinessException(ResponseStatuses.NOT_FOUND, HttpStatus.BAD_REQUEST, \"")
                    .append(cap(s.ref.idProp)).append(" is not found\"); }\n\n");
        }

        // toDTO
        if (feign) {
            sb.append("    private ").append(E).append("DTO toDTO(").append(E).append(" e, ").append(refDtoDecl).append(") {\n");
        } else {
            sb.append("    private ").append(E).append("DTO toDTO(").append(E).append(" e) {\n");
        }
        sb.append("        ").append(E).append("DTO dto = new ").append(E).append("DTO();\n");
        sb.append("        dto.set").append(cap(s.idName)).append("(e.get").append(cap(s.idName)).append("());\n");
        for (Field f : s.fields)
            sb.append("        dto.set").append(cap(f.name)).append("(e.get").append(cap(f.name)).append("());\n");
        if (feign) {
            if (s.ref.inputId)
                sb.append("        dto.set").append(cap(s.ref.idProp)).append("(e.get").append(cap(s.ref.idProp)).append("());\n");
            if (s.ref.inputObject)
                sb.append("        dto.set").append(cap(s.ref.objectProp)).append("(refObj);\n");
        }
        sb.append("        return dto;\n    }\n\n");

        if (s.foodResponse) {
            sb.append("    private ").append(E).append("ResponseDTO toResponseDTO(").append(E).append(" e, ")
                    .append(s.ref.dtoClass).append(" refObj) {\n");
            sb.append("        ").append(E).append("ResponseDTO dto = new ").append(E).append("ResponseDTO();\n");
            sb.append("        dto.set").append(cap(s.idName)).append("(e.get").append(cap(s.idName)).append("());\n");
            for (String name : s.foodResponseFields) {
                Field f = s.fields.stream().filter(x -> x.name.equals(name)).findFirst().orElseThrow();
                sb.append("        dto.set").append(cap(f.name)).append("(e.get").append(cap(f.name)).append("());\n");
            }
            sb.append("        dto.set").append(cap(s.ref.objectProp)).append("(refObj);\n")
                    .append("        return dto;\n    }\n\n");
        }

        if (lookupFk != null) {
            sb.append("    private void validateLookup(Long id) {\n")
                    .append("        if (id == null || id <= 0 || !lookupRepo.existsById(id))\n")
                    .append("            throw new BusinessException(ResponseStatuses.NOT_FOUND, HttpStatus.BAD_REQUEST, \"")
                    .append(s.lookup.entity).append(" ID is not found\");\n")
                    .append("    }\n\n");
        }
        if (selfFk != null) {
            sb.append("    private void validateSelfReference(Number value, Long currentId) {\n")
                    .append("        if (value == null) return;\n")
                    .append("        long referencedId = value.longValue();\n")
                    .append("        if (referencedId <= 0 || (currentId != null && referencedId == currentId)) throw validationFailed();\n")
                    .append("        if (!repo.existsById(referencedId)) throw notFound();\n")
                    .append("    }\n\n");
        }

        // toolkit
        sb.append(toolkit(s, unique));
        sb.append("}\n");
        write(s.project, javaDir(s) + "/service/impl/" + E + "ServiceImpl.java", sb.toString());
    }

    static String setterExpr(Field f, String e, String dto) {
        String get = dto + ".get" + cap(f.name) + "()";
        String set = e + ".set" + cap(f.name);
        return switch (f.kind) {
            case "ENUM" -> set + "(normalizeEnum(" + get + ", " + f.name.toUpperCase() + "_VALUES))";
            case "STATUS" -> set + "(normalizeStatus(" + get + "))";
            case "DATE", "DATETIME", "LONG", "INTEGER" -> set + "(" + get + ")";
            default -> set + "(" + (f.required ? "clean(" : "cleanNullable(") + get + "))";
        };
    }

    static String setterFromDto(Field f, String e, String dto, Field unique) {
        if (f.kind.equals("STATUS")) return ""; // status is handled separately on create
        if (f == unique) return "        " + e + ".set" + cap(f.name) + "(_u);\n";
        return "        " + setterExpr(f, e, dto) + ";\n";
    }

    static String validateLine(Field f, boolean create, Service s) {
        String get = "dto.get" + cap(f.name) + "()";
        StringBuilder b = new StringBuilder();
        String ind = "        ";
        if (f.kind.equals("STATUS")) {
            if (create && f.required && !s.statusDefaultOnCreate)
                b.append(ind).append("if (").append(get).append(" == null) throw validationFailed();\n");
            b.append(ind).append("validateStatusIfPresent(").append(get).append(");\n");
            return b.toString();
        }
        if (f.kind.equals("LONG") || f.kind.equals("INTEGER")) {
            if (create && f.required)
                b.append(ind).append("if (").append(get).append(" == null) throw validationFailed();\n");
            return b.toString();
        }
        if (f.kind.equals("DATE") || f.kind.equals("DATETIME")) {
            if (create) {
                if (f.required)
                    b.append(ind).append("if (").append(get).append(" == null) throw validationFailed();\n");
                if (f.after != null || f.beforeDays > 0)
                    b.append(ind).append("validateDateBounds(").append(get).append(", ")
                            .append(f.after == null ? "null" : "\"" + f.after + "\"").append(", ").append(f.beforeDays).append(");\n");
                if (f.geField != null)
                    b.append(ind).append("validateGe(").append(get).append(", dto.get").append(cap(f.geField)).append("());\n");
            } else {
                if (f.after != null || f.beforeDays > 0)
                    b.append(ind).append("if (").append(get).append(" != null) validateDateBounds(").append(get).append(", ")
                            .append(f.after == null ? "null" : "\"" + f.after + "\"").append(", ").append(f.beforeDays).append(");\n");
                if (f.geField != null) {
                    Field ge = s.fields.stream().filter(x -> x.name.equals(f.geField)).findFirst().orElse(null);
                    String geGet = ge == null ? "null" : "dto.get" + cap(ge.name) + "() != null ? dto.get" + cap(ge.name) + "() : current.get" + cap(ge.name) + "()";
                    String myGet = get + " != null ? " + get + " : current.get" + cap(f.name) + "()";
                    b.append(ind).append("validateGe(").append(myGet).append(", ").append(geGet).append(");\n");
                }
            }
            return b.toString();
        }
        // STRING or ENUM
        String body = strValidate(f);
        if (create) {
            b.append(body);
        } else {
            // wrap required/enum checks in null-guard for partial update
            b.append(ind).append("if (").append(get).append(" != null) {\n");
            b.append("    ").append(body);
            b.append(ind).append("}\n");
        }
        return b.toString();
    }

    static String strValidate(Field f) {
        String get = "dto.get" + cap(f.name) + "()";
        StringBuilder b = new StringBuilder();
        String ind = "        ";
        int max = f.max;
        if (f.required)
            b.append(ind).append("validateRequiredString(").append(get).append(", ").append(max).append(");\n");
        else b.append(ind).append("validateOptionalString(").append(get).append(", ").append(max).append(");\n");
        if (f.pattern != null)
            b.append(ind).append("validatePattern(").append(get).append(", ").append(f.name.toUpperCase()).append("_PATTERN);\n");
        if (f.email) b.append(ind).append("validateEmail(").append(get).append(");\n");
        if (f.kind.equals("ENUM"))
            b.append(ind).append("if (").append(get).append(" != null) normalizeEnum(").append(get).append(", ").append(f.name.toUpperCase()).append("_VALUES);\n");
        return b.toString();
    }

    static String toolkit(Service s, Field unique) {
        StringBuilder b = new StringBuilder();
        String notFoundMsg = s.entity + " is not found";
        b.append("    private void validateRequiredString(String v, int max) { if (v == null || v.trim().isEmpty() || (max > 0 && v.trim().length() > max)) throw validationFailed(); }\n");
        b.append("    private void validateOptionalString(String v, int max) { if (v != null && max > 0 && v.trim().length() > max) throw validationFailed(); }\n");
        b.append("    private void validatePattern(String v, Pattern p) { if (v != null && !p.matcher(v.trim()).matches()) throw validationFailed(); }\n");
        b.append("    private void validateEmail(String v) { if (v != null && !EMAIL_PATTERN.matcher(v.trim()).matches()) throw validationFailed(); }\n");
        if (s.hasStatus) {
            b.append("    private String normalizeStatus(String x) { String n = clean(x).toUpperCase(Locale.ROOT); if (!STATUS_VALUES.contains(n)) throw validationFailed(); return n; }\n");
            b.append("    private void validateStatusIfPresent(String x) { if (x != null) normalizeStatus(x); }\n");
        }
        b.append("    private String normalizeEnum(String v, Set<String> allowed) { String c = clean(v); for (String a : allowed) if (a.equalsIgnoreCase(c)) return a; throw validationFailed(); }\n");
        b.append("    private void validateDateBounds(java.util.Date d, String afterIso, int beforeDays) { if (d == null) return; LocalDate ld = toLocal(d); if (afterIso != null && !ld.isAfter(LocalDate.parse(afterIso))) throw validationFailed(); if (beforeDays > 0 && !ld.isBefore(LocalDate.now().plusDays(beforeDays))) throw validationFailed(); }\n");
        b.append("    private void validateGe(java.util.Date later, java.util.Date earlier) { if (later == null || earlier == null) return; if (toLocal(later).isBefore(toLocal(earlier))) throw validationFailed(); }\n");
        b.append("    private LocalDate toLocal(java.util.Date d) { return Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalDate(); }\n");
        b.append("    private void validatePagination(int p, int sz) { if (p < 0 || sz < 1 || sz > MAX_PAGE_SIZE) throw validationFailed(); }\n");
        b.append("    private String clean(String v) { return v == null ? null : v.trim(); }\n");
        b.append("    private String cleanNullable(String v) { String c = clean(v); return (c == null || c.isEmpty()) ? null : c; }\n");
        b.append("    private BusinessException validationFailed() { return new BusinessException(ResponseStatuses.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, \"Data validation failed\"); }\n");
        b.append("    private BusinessException duplicate() { return new BusinessException(ResponseStatuses.DUPLICATE_CODE, HttpStatus.BAD_REQUEST, \"Code is duplicated\"); }\n");
        b.append("    private BusinessException notFound() { return new BusinessException(ResponseStatuses.NOT_FOUND, HttpStatus.BAD_REQUEST, \"").append(javaStr(notFoundMsg)).append("\"); }\n");
        return b.toString();
    }

    static void writeController(Service s) throws IOException {
        String E = s.entity;
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(GROUP).append(".").append(s.pkg).append(".controller;\n\n");
        sb.append("import ").append(GROUP).append(".").append(s.pkg).append(".dto.*;\n");
        sb.append("import ").append(GROUP).append(".").append(s.pkg).append(".service.").append(E).append("Service;\n");
        sb.append("import jakarta.validation.Valid;\n");
        sb.append("import org.springframework.http.HttpStatus;\n");
        sb.append("import org.springframework.http.MediaType;\n");
        sb.append("import org.springframework.http.ResponseEntity;\n");
        sb.append("import org.springframework.web.bind.annotation.*;\n\n");
        sb.append("@RestController\n");
        sb.append("@RequestMapping(value = \"").append(s.basePath).append("\", produces = MediaType.APPLICATION_JSON_VALUE)\n");
        sb.append("public class ").append(s.controller).append(" {\n\n");
        sb.append("    private final ").append(E).append("Service service;\n");
        sb.append("    public ").append(s.controller).append("(").append(E).append("Service service) { this.service = service; }\n\n");
        sb.append("    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)\n");
        sb.append("    public ResponseEntity<ApiResponseDTO<").append(E).append("DTO>> create(@Valid @RequestBody ").append(E).append("DTO dto) {\n");
        sb.append("        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.success(service.create(dto)));\n    }\n\n");
        sb.append("    @PutMapping(value = \"/{id}\", consumes = MediaType.APPLICATION_JSON_VALUE)\n");
        sb.append("    public ResponseEntity<ApiResponseDTO<").append(E).append("DTO>> update(@PathVariable Long id, @Valid @RequestBody ").append(E).append("DTO dto) {\n");
        sb.append("        return ResponseEntity.ok(ApiResponseDTO.success(service.update(id, dto)));\n    }\n\n");
        sb.append("    @GetMapping(\"/{id}\")\n");
        sb.append("    public ResponseEntity<ApiResponseDTO<").append(s.foodResponse ? E + "ResponseDTO" : E + "DTO")
                .append(">> detail(@PathVariable Long id) {\n");
        sb.append("        return ResponseEntity.ok(ApiResponseDTO.success(service.get(id)));\n    }\n\n");
        if (s.hasStatus) {
            sb.append("    @DeleteMapping(\"/{id}\")\n");
            sb.append("    public ResponseEntity<ApiResponseDTO<Void>> deactivate(@PathVariable Long id) {\n");
            sb.append("        service.deactivate(id);\n");
            sb.append("        return ResponseEntity.ok(ApiResponseDTO.success(null));\n    }\n\n");
        }
        sb.append("    @GetMapping\n");
        if (s.foodResponse)
            sb.append("    public ResponseEntity<ApiResponseDTO<").append(E).append("ListDTO>> list(\n");
        else
            sb.append("    public ResponseEntity<ApiResponseDTO<PageDTO<").append(E).append("DTO>>> list(\n");
        sb.append("            @RequestParam(required = false) Integer page,\n");
        sb.append("            @RequestParam(required = false) Integer size");
        for (Filter f : s.filters)
            sb.append(",\n            @RequestParam(required = false) String ").append(f.query);
        sb.append(") {\n");
        sb.append("        return ResponseEntity.ok(ApiResponseDTO.success(service.list(page, size");
        for (Filter f : s.filters) sb.append(", ").append(f.query);
        sb.append(")));\n    }\n");
        sb.append("}\n");
        write(s.project, javaDir(s) + "/controller/" + s.controller + ".java", sb.toString());
    }

    static void writeCors(String project, String pkg, boolean reactive) throws IOException {
        write(project, "src/main/java/" + GROUP_PATH + "/" + pkg + "/config/CorsConfig.java",
                "package " + GROUP + "." + pkg + ".config;\n\n" +
                        "import org.springframework.context.annotation.Configuration;\n" +
                        "import org.springframework.web.servlet.config.annotation.CorsRegistry;\n" +
                        "import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;\n\n" +
                        "@Configuration\n" +
                        "public class CorsConfig implements WebMvcConfigurer {\n" +
                        "    @Override\n" +
                        "    public void addCorsMappings(CorsRegistry registry) {\n" +
                        "        registry.addMapping(\"/**\").allowedOriginPatterns(\"*\")\n" +
                        "                .allowedMethods(\"GET\", \"POST\", \"PUT\", \"DELETE\", \"OPTIONS\").allowedHeaders(\"*\");\n" +
                        "    }\n}\n");
    }

    static void writeOpenApi(String project, String pkg, String title) throws IOException {
        write(project, "src/main/java/" + GROUP_PATH + "/" + pkg + "/config/OpenApiConfig.java",
                "package " + GROUP + "." + pkg + ".config;\n\n" +
                        "import io.swagger.v3.oas.models.OpenAPI;\n" +
                        "import io.swagger.v3.oas.models.info.Info;\n" +
                        "import org.springframework.context.annotation.Bean;\n" +
                        "import org.springframework.context.annotation.Configuration;\n\n" +
                        "@Configuration\n" +
                        "public class OpenApiConfig {\n" +
                        "    @Bean\n" +
                        "    public OpenAPI apiInfo() {\n" +
                        "        return new OpenAPI().info(new Info().title(\"" + javaStr(title) + "\").version(\"1.0\"));\n" +
                        "    }\n}\n");
    }

    static void writeFeignClient(Service s) throws IOException {
        String C = s.base.entity + "Client";
        write(s.project, javaDir(s) + "/config/" + C + ".java",
                "package " + GROUP + "." + s.pkg + ".config;\n\n" +
                        "import " + GROUP + "." + s.pkg + ".dto.ApiResponseDTO;\n" +
                        "import org.springframework.cloud.openfeign.FeignClient;\n" +
                        "import org.springframework.web.bind.annotation.GetMapping;\n" +
                        "import org.springframework.web.bind.annotation.PathVariable;\n\n" +
                        "@FeignClient(name = \"" + s.base.pkg + "-service\", url = \"${" + s.ref.serviceUrlProp + ":" + s.ref.serviceUrl + "}\")\n" +
                        "public interface " + C + " {\n" +
                        "    @GetMapping(\"" + s.ref.basePath + "/{id}\")\n" +
                        "    ApiResponseDTO<Object> get" + s.base.entity + "(@PathVariable(\"id\") Long id);\n}\n");
    }

    /**
     * Optional lookup table exposed by service A (for example Category in RestaurantService).
     */
    static void writeLookup(Service owner) throws IOException {
        Lookup l = owner.lookup;
        String P = GROUP + "." + owner.pkg;
        String dir = javaDir(owner);

        write(owner.project, dir + "/dto/" + l.entity + "DTO.java",
                dtoBody(owner.pkg, l.entity + "DTO", l.idName, l.fields, null));

        StringBuilder entity = new StringBuilder();
        entity.append("package ").append(P).append(".entity;\n\n")
                .append("import jakarta.persistence.*;\nimport lombok.Getter;\nimport lombok.Setter;\nimport java.util.Date;\n\n")
                .append("@Getter\n@Setter\n@Entity\n@Table(name = \"").append(l.table).append("\")\n")
                .append("public class ").append(l.entity).append(" {\n")
                .append("    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)\n")
                .append("    @Column(name = \"").append(l.idColumn).append("\")\n")
                .append("    private Long ").append(l.idName).append(";\n");
        for (Field f : l.fields) {
            if (f.kind.equals("DATE")) entity.append("    @Temporal(TemporalType.DATE)\n");
            if (f.kind.equals("DATETIME")) entity.append("    @Temporal(TemporalType.TIMESTAMP)\n");
            entity.append("    @Column(name = \"").append(f.column).append("\"");
            if (f.required) entity.append(", nullable = false");
            if (f.unique) entity.append(", unique = true");
            if (f.max > 0 && !f.kind.equals("DATE") && !f.kind.equals("DATETIME") && !f.kind.equals("LONG") && !f.kind.equals("INTEGER"))
                entity.append(", length = ").append(f.max);
            entity.append(")\n    private ").append(f.type()).append(" ").append(f.name).append(";\n");
        }
        entity.append("}\n");
        write(owner.project, dir + "/entity/" + l.entity + ".java", entity.toString());

        write(owner.project, dir + "/repository/" + l.entity + "Repository.java",
                "package " + P + ".repository;\n\n" +
                        "import " + P + ".entity." + l.entity + ";\n" +
                        "import org.springframework.data.jpa.repository.JpaRepository;\n\n" +
                        "public interface " + l.entity + "Repository extends JpaRepository<" + l.entity + ", Long> {}\n");

        write(owner.project, dir + "/service/" + l.entity + "Service.java",
                "package " + P + ".service;\n\n" +
                        "import " + P + ".dto." + l.entity + "DTO;\n" +
                        "import " + P + ".dto.PageDTO;\n\n" +
                        "public interface " + l.entity + "Service { PageDTO<" + l.entity + "DTO> list(Integer page, Integer size); }\n");

        StringBuilder mapping = new StringBuilder();
        mapping.append("        ").append(l.entity).append("DTO dto = new ").append(l.entity).append("DTO();\n")
                .append("        dto.set").append(cap(l.idName)).append("(e.get").append(cap(l.idName)).append("());\n");
        for (Field f : l.fields)
            mapping.append("        dto.set").append(cap(f.name)).append("(e.get").append(cap(f.name)).append("());\n");
        mapping.append("        return dto;\n");
        write(owner.project, dir + "/service/impl/" + l.entity + "ServiceImpl.java",
                "package " + P + ".service.impl;\n\n" +
                        "import " + P + ".common.BusinessException;\n" +
                        "import " + P + ".common.ResponseStatuses;\n" +
                        "import " + P + ".dto.*;\n" +
                        "import " + P + ".entity." + l.entity + ";\n" +
                        "import " + P + ".repository." + l.entity + "Repository;\n" +
                        "import " + P + ".service." + l.entity + "Service;\n" +
                        "import org.springframework.data.domain.*;\n" +
                        "import org.springframework.http.HttpStatus;\n" +
                        "import org.springframework.stereotype.Service;\n" +
                        "import org.springframework.transaction.annotation.Transactional;\n\n" +
                        "@Service\npublic class " + l.entity + "ServiceImpl implements " + l.entity + "Service {\n" +
                        "    private final " + l.entity + "Repository repo;\n" +
                        "    public " + l.entity + "ServiceImpl(" + l.entity + "Repository repo) { this.repo = repo; }\n" +
                        "    @Override @Transactional(readOnly = true)\n" +
                        "    public PageDTO<" + l.entity + "DTO> list(Integer page, Integer size) {\n" +
                        "        int p = page == null ? 0 : page; int sz = size == null ? 10 : size;\n" +
                        "        if (p < 0 || sz < 1 || sz > 100) throw new BusinessException(ResponseStatuses.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, \"Data validation failed\");\n" +
                        "        return new PageDTO<>(repo.findAll(PageRequest.of(p, sz, Sort.by(\"" + l.idName + "\"))).map(this::toDTO));\n" +
                        "    }\n" +
                        "    private " + l.entity + "DTO toDTO(" + l.entity + " e) {\n" + mapping + "    }\n}\n");

        write(owner.project, dir + "/controller/" + l.entity + "Controller.java",
                "package " + P + ".controller;\n\n" +
                        "import " + P + ".dto.*;\n" +
                        "import " + P + ".service." + l.entity + "Service;\n" +
                        "import org.springframework.http.ResponseEntity;\n" +
                        "import org.springframework.web.bind.annotation.*;\n\n" +
                        "@RestController\n@RequestMapping(\"" + l.basePath + "\")\n" +
                        "public class " + l.entity + "Controller {\n" +
                        "    private final " + l.entity + "Service service;\n" +
                        "    public " + l.entity + "Controller(" + l.entity + "Service service) { this.service = service; }\n" +
                        "    @GetMapping\n" +
                        "    public ResponseEntity<ApiResponseDTO<PageDTO<" + l.entity + "DTO>>> list(\n" +
                        "            @RequestParam(required=false) Integer page, @RequestParam(required=false) Integer size) {\n" +
                        "        return ResponseEntity.ok(ApiResponseDTO.success(service.list(page, size)));\n" +
                        "    }\n}\n");
    }

    // ============================================================ GATEWAY
    static void writeGateway() throws IOException {
        String project = r("g.project"), pkg = r("g.pkg"), port = r("g.port");
        String javaDir = "src/main/java/" + GROUP_PATH + "/" + pkg;

        // pom
        StringBuilder pom = new StringBuilder();
        pom.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>%SBV%</version>
                        <relativePath/>
                    </parent>
                    <groupId>%GROUP%</groupId>
                    <artifactId>%PROJECT%</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <name>%PROJECT%</name>
                    <packaging>jar</packaging>
                    <properties>
                        <java.version>21</java.version>
                        <spring-cloud.version>%SCV%</spring-cloud.version>
                        <springdoc.version>%SDV%</springdoc.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.cloud</groupId>
                                <artifactId>spring-cloud-dependencies</artifactId>
                                <version>${spring-cloud.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <dependencies>
                """.replace("%SBV%", SBV).replace("%GROUP%", GROUP).replace("%PROJECT%", project)
                .replace("%SCV%", SCV).replace("%SDV%", SDV));
        pom.append(dep("org.springframework.cloud", "spring-cloud-starter-gateway-server-webflux", null, null));
        pom.append(dep("org.springframework.boot", "spring-boot-starter-security", null, null));
        pom.append(dep("org.springdoc", "springdoc-openapi-starter-webflux-ui", "${springdoc.version}", null));
        pom.append(dep("org.springframework.boot", "spring-boot-starter-test", null, "test"));
        pom.append("    </dependencies>\n");
        pom.append(buildPlugins());
        pom.append("</project>\n");
        write(project, "pom.xml", pom.toString());

        // application.properties
        StringBuilder ap = new StringBuilder();
        ap.append("spring.application.name=").append(project).append("\n");
        ap.append("server.port=").append(port).append("\n\n");
        String cors = "spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].";
        ap.append("spring.cloud.gateway.server.webflux.globalcors.add-to-simple-url-handler-mapping=true\n");
        ap.append(cors).append("allowedOriginPatterns=*\n");
        ap.append(cors).append("allowedMethods=GET,POST,PUT,DELETE,OPTIONS\n");
        ap.append(cors).append("allowedHeaders=*\n");
        ap.append(cors).append("allowCredentials=false\n\n");
        ap.append("springdoc.api-docs.path=/v3/api-docs\n");
        ap.append("springdoc.swagger-ui.path=/swagger-ui.html\n");
        write(project, "src/main/resources/application.properties", ap.toString());

        // main app
        String cls = project + "Application";
        write(project, javaDir + "/" + cls + ".java",
                "package " + GROUP + "." + pkg + ";\n\n" +
                        "import org.springframework.boot.SpringApplication;\n" +
                        "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n" +
                        "@SpringBootApplication\n" +
                        "public class " + cls + " {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        SpringApplication.run(" + cls + ".class, args);\n    }\n}\n");

        // routes
        write(project, javaDir + "/config/GatewayRouteConfig.java",
                "package " + GROUP + "." + pkg + ".config;\n\n" +
                        "import org.springframework.cloud.gateway.route.RouteLocator;\n" +
                        "import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;\n" +
                        "import org.springframework.context.annotation.Bean;\n" +
                        "import org.springframework.context.annotation.Configuration;\n\n" +
                        "@Configuration\n" +
                        "public class GatewayRouteConfig {\n" +
                        "    @Bean\n" +
                        "    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {\n" +
                        "        return builder.routes()\n" +
                        "            .route(\"service-a\", r -> r.path(\"" + r("g.routeA.path") + "\").uri(\"" + r("g.routeA.uri") + "\"))\n" +
                        "            .route(\"service-b\", r -> r.path(\"" + r("g.routeB.path") + "\").uri(\"" + r("g.routeB.uri") + "\"))\n" +
                        "            .build();\n    }\n}\n");

        // security
        write(project, javaDir + "/config/SecurityConfig.java",
                "package " + GROUP + "." + pkg + ".config;\n\n" +
                        "import org.springframework.context.annotation.Bean;\n" +
                        "import org.springframework.context.annotation.Configuration;\n" +
                        "import org.springframework.security.config.web.server.ServerHttpSecurity;\n" +
                        "import org.springframework.security.web.server.SecurityWebFilterChain;\n" +
                        "import org.springframework.web.cors.CorsConfiguration;\n" +
                        "import org.springframework.web.cors.reactive.CorsWebFilter;\n" +
                        "import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;\n\n" +
                        "@Configuration\n" +
                        "public class SecurityConfig {\n" +
                        "    @Bean\n" +
                        "    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {\n" +
                        "        return http\n" +
                        "            .csrf(ServerHttpSecurity.CsrfSpec::disable)\n" +
                        "            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)\n" +
                        "            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)\n" +
                        "            .authorizeExchange(ex -> ex.anyExchange().permitAll())\n" +
                        "            .build();\n    }\n\n" +
                        "    @Bean\n" +
                        "    public CorsWebFilter corsWebFilter() {\n" +
                        "        CorsConfiguration c = new CorsConfiguration();\n" +
                        "        c.addAllowedOriginPattern(\"*\");\n" +
                        "        c.addAllowedMethod(\"*\");\n" +
                        "        c.addAllowedHeader(\"*\");\n" +
                        "        c.setAllowCredentials(false);\n" +
                        "        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();\n" +
                        "        source.registerCorsConfiguration(\"/**\", c);\n" +
                        "        return new CorsWebFilter(source);\n    }\n}\n");

        // openapi
        write(project, javaDir + "/config/OpenApiConfig.java",
                "package " + GROUP + "." + pkg + ".config;\n\n" +
                        "import io.swagger.v3.oas.models.OpenAPI;\n" +
                        "import io.swagger.v3.oas.models.info.Info;\n" +
                        "import org.springframework.context.annotation.Bean;\n" +
                        "import org.springframework.context.annotation.Configuration;\n\n" +
                        "@Configuration\n" +
                        "public class OpenApiConfig {\n" +
                        "    @Bean\n" +
                        "    public OpenAPI apiInfo() {\n" +
                        "        return new OpenAPI().info(new Info().title(\"" + javaStr(project + " API") + "\").version(\"1.0\"));\n" +
                        "    }\n}\n");
    }

    // ============================================================ README
    static void writeReadme(Service a, Service b) throws IOException {
        String gp = r("g.project");
        String md = "# Generated by PEGen\n\n" +
                "Sinh tu `pe-config.properties`. 3 project + script SQL.\n\n" +
                "## Thu tu chay\n" +
                "1. Chay `database/" + DBNAME + ".sql` trong SQL Server.\n" +
                "2. " + a.project + " (port " + a.port + ")\n" +
                "3. " + b.project + " (port " + b.port + ")\n" +
                "4. " + gp + " (port " + r("g.port") + ")\n\n" +
                "## Build/Run mot project\n" +
                "```\nmvn spring-boot:run\n```\n\n" +
                "## Swagger\n" +
                "- http://localhost:" + a.port + "/swagger-ui.html\n" +
                "- http://localhost:" + b.port + "/swagger-ui.html\n" +
                "- http://localhost:" + r("g.port") + "/swagger-ui.html\n";
        writeRaw("README.md", md);
    }
}
