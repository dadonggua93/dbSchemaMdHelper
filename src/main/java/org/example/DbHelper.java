package org.example;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class DbHelper {
    public static void main(String[] args) throws SQLException, IOException {
        String url = "jdbc:mysql://mysql1.projects.chilunyc.cn:3306/skf_solution_dev?useUnicode=true&characterEncoding=utf8&useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC&createDatabaseIfNotExist=true&useInformationSchema=true";

        DbHelper dbHelper = new DbHelper(url, "skf_solution_dev", "skf_dev@dev-projects", "SKFGear!1");
        dbHelper.addTable("demand_collect","admin_user","scheme_material","luber_scheme_pdf");
        dbHelper.execute();
    }

    private final String url;
    private final String schemaName;
    private final String username;
    private final String password;

    private String outputPath = "E:/output" + System.currentTimeMillis() + ".md";
    private List<String> requireTableName = new ArrayList<>();
    private Connection connection;
    private List<TableItem> tableItems;

    public DbHelper(String url, String schemaName, String username, String password) {
        this.url = url;
        this.schemaName = schemaName;
        this.username = username;
        this.password = password;
    }

    public void addTable(String... tableNames) {
        requireTableName.addAll(Arrays.asList(tableNames));
    }

    private void initConnection() throws SQLException {
        if (connection == null) {
            connection = DriverManager.getConnection(this.url, this.username, this.password);
        }
    }

    public void getTableSchema() throws SQLException {
        this.tableItems = new ArrayList<>();
        DatabaseMetaData dbMetData = connection.getMetaData();

        ResultSet rs = dbMetData.getTables(connection.getCatalog(), this.schemaName, "%", new String[]{"TABLE"});
        ResultSet columns = dbMetData.getColumns(connection.getCatalog(), this.schemaName, "%", "%");

        Map<String, List<ColumnItem>> columnsHash = new HashMap<>(65536);
        try {
            while (columns.next()) {
                String tableName = columns.getString("TABLE_NAME");
                if (!this.requireTableFlag(tableName)) {
                    continue;
                }
                columnsHash.compute(tableName, (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    try {
                        ColumnItem columnItem = new ColumnItem();
                        JDBCType jdbcType = JDBCType.valueOf(columns.getInt("DATA_TYPE"));

                        columnItem.setPrimary("false");
                        columnItem.setRemark(columns.getString("REMARKS"));
                        columnItem.setName(columns.getString("COLUMN_NAME"));
                        columnItem.setType(jdbcType.getName());
                        columnItem.setLength(columns.getInt("COLUMN_SIZE"));
                        columnItem.setNullable(columns.getString("IS_NULLABLE"));
                        columnItem.setScale(columns.getInt("DECIMAL_DIGITS"));

                        v.add(columnItem);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return v;
                });
            }

            while (rs.next()) {
                String tableRemark = rs.getString("REMARKS");
                String tableName = rs.getString("TABLE_NAME");
                if (!this.requireTableFlag(tableName)) {
                    continue;
                }


                List<ColumnItem> fieldMap = columnsHash.getOrDefault(tableName, null);
                TableItem tableItem = new TableItem();
                tableItem.setTableName(tableName);
                tableItem.setRemark(tableRemark);
                tableItem.setColumns(fieldMap);

                ResultSet primaryKeys = dbMetData.getPrimaryKeys(null, this.schemaName, tableName);
                while (primaryKeys.next()) {
                    String pk = primaryKeys.getString(4);
                    for (ColumnItem columnItem : fieldMap) {
                        if (columnItem.getName().equals(pk)) {
                            columnItem.setPrimary("true");
                            break;
                        }
                    }
                }
                this.tableItems.add(tableItem);
            }
        } finally {

        }

    }

    private void output() throws IOException {
        StringBuffer str = new StringBuffer();
        for (TableItem t : this.tableItems) {
            if (!this.requireTableFlag(t.getTableName())) {
                continue;
            }
            str.append("\n\n");
            str.append(String.format("### 表名：%s (%s) \n\n",t.getTableName(),t.getRemark() ));
            str.append("|字段名| 类型 | 精度 | 是否主键| 是否为空   |字段描述             |\n");
            str.append("|------|------|------|------|------|------|\n");
            List<ColumnItem> columns = t.getColumns();
            for (ColumnItem column : columns) {
                String type = column.getType() + String.format("(%s)", column.getLength());
                String info = String.format("|%s|%s|%s|%s|%s|%s|", column.getName(),type,column.getScale(),column.getPrimary(),column.getNullable(),column.getRemark());
                str.append(info).append("\n");
            }
        }

        FileUtils.writeStringToFile(new File(this.outputPath), str.toString());
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.connection.close();
    }

    private boolean requireTableFlag(String tableName) {
        return requireTableName.isEmpty() || requireTableName.contains(tableName);
    }

    public void execute() throws SQLException, IOException {
        this.initConnection();
        this.getTableSchema();
        this.output();
    }


}
