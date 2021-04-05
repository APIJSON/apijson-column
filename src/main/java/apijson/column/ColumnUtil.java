/*Copyright ©2021 TommyLemon(https://github.com/APIJSON/apijson-column)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package apijson.column;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import apijson.RequestMethod;
import apijson.StringUtil;
import apijson.orm.AbstractSQLConfig;
import apijson.orm.AbstractSQLExecutor;


/**表字段相关工具类
 * @author Lemon
 * @see 先提前配置 {@link #VERSIONED_TABLE_COLUMN_MAP}, {@link #VERSIONED_KEY_COLUMN_MAP} 等，然后调用相关方法。
 * 不支持直接关联 database, schema, datasource，可以把这些与 table 拼接为一个字符串传给参数 table，格式可以是 database-schema-datasource-table
 */
public class ColumnUtil {

	/**带版本的表和字段一对多对应关系，用来做 反选字段
	 * Map<version, Map<table, [column0, column1...]>>
	 */
	public static final Map<Integer, Map<String, List<String>>> VERSIONED_TABLE_COLUMN_MAP;

	/**带版本的 JSON key 和表字段一对一对应关系，用来做字段名映射
	 * Map<version, Map<table, Map<key, column>>>
	 */
	public static final Map<Integer, Map<String, Map<String, String>>> VERSIONED_KEY_COLUMN_MAP;

	/**带版本的 JSON key 和表字段一对一对应关系，用来做字段名映射，与 VERSIONED_KEY_COLUMN_MAP 相反
	 * Map<version, Map<table, Map<column, key>>>
	 */
	private static Map<Integer, Map<String, Map<String, String>>> VERSIONED_COLUMN_KEY_MAP;
	static {
		VERSIONED_TABLE_COLUMN_MAP = new HashMap<>();
		VERSIONED_KEY_COLUMN_MAP = new HashMap<>();
		VERSIONED_COLUMN_KEY_MAP = new HashMap<>();
	}

	/**初始化
	 */
	public static void init() {
		VERSIONED_COLUMN_KEY_MAP.clear();

		Set<Entry<Integer, Map<String, Map<String, String>>>> set = VERSIONED_KEY_COLUMN_MAP.entrySet();
		if (set != null && set.isEmpty() == false) {

			Map<Integer, Map<String, Map<String, String>>> map = new HashMap<>();

			for (Entry<Integer, Map<String, Map<String, String>>> entry : set) {

				Map<String, Map<String, String>> tableKeyColumnMap = entry == null ? null : entry.getValue();
				Set<Entry<String, Map<String, String>>> tableKeyColumnSet = tableKeyColumnMap == null ? null : tableKeyColumnMap.entrySet();

				if (tableKeyColumnSet != null && tableKeyColumnSet.isEmpty() == false) {

					Map<String, Map<String, String>> tableColumnKeyMap = new HashMap<>();

					for (Entry<String, Map<String, String>> tableKeyColumnEntry : tableKeyColumnSet) {

						Map<String, String> keyColumnMap = tableKeyColumnEntry == null ? null : tableKeyColumnEntry.getValue();
						Set<Entry<String, String>> keyColumnSet = keyColumnMap == null ? null : keyColumnMap.entrySet();

						if (keyColumnSet != null && keyColumnSet.isEmpty() == false) {
							Map<String, String> columnKeyMap = new HashMap<>();
							for (Entry<String, String> keyColumnEntry : keyColumnSet) {
								if (keyColumnEntry == null) {
									continue;
								}

								columnKeyMap.put(keyColumnEntry.getValue(), keyColumnEntry.getKey());
							}

							tableColumnKeyMap.put(tableKeyColumnEntry.getKey(), columnKeyMap);
						}
					}

					map.put(entry.getKey(), tableColumnKeyMap);
				}
			}

			VERSIONED_COLUMN_KEY_MAP = map;
		}
	}

	/**适配请求参数 JSON 中 @column:value 的 value 中的 key。支持 !key 反选字段 和 字段名映射
	 * @param columns
	 * @param table
	 * @param method
	 * @return
	 */
	public static List<String> compatInputColumn(List<String> columns, String table, RequestMethod method) {
		return compatInputColumn(columns, table, method, null);
	}

	/**适配请求参数 JSON 中 @column:value 的 value 中的 key。支持 !key 反选字段 和 字段名映射
	 * @param columns
	 * @param table
	 * @param method
	 * @param version
	 * @return
	 * @see 先提前配置 {@link #VERSIONED_TABLE_COLUMN_MAP}，然后在 {@link AbstractSQLConfig} 的子类重写 {@link AbstractSQLConfig#setColumn } 并调用这个方法，例如
	 * <pre >
	   public AbstractSQLConfig setColumn(List<String> column) { <br>
	  	 return super.setColumn(ColumnUtil.compatInputColumn(column, getTable(), version)); <br>
	   }
	 * </pre>
	 */
	public static List<String> compatInputColumn(List<String> columns, String table, RequestMethod method, Integer version) {
		String[] keys = columns == null ? null : columns.toArray(new String[]{});  // StringUtil.split(c, ";");
		if (keys == null || keys.length <= 0) {
			return columns;
		}

		//		boolean isQueryMethod = RequestMethod.isQueryMethod(method);

		List<String> exceptColumns = new ArrayList<>(); // Map<String, String> exceptColumnMap = new HashMap<>();
		List<String> newColumns = new ArrayList<>();

		Map<String, Map<String, String>> tableKeyColumnMap = VERSIONED_KEY_COLUMN_MAP == null || VERSIONED_KEY_COLUMN_MAP.isEmpty() ? null : VERSIONED_KEY_COLUMN_MAP.get(version);
		Map<String, String> keyColumnMap = tableKeyColumnMap == null || tableKeyColumnMap.isEmpty() ? null : tableKeyColumnMap.get(table);

		String expression;
		//...;fun0(arg0,arg1,...):fun0;fun1(arg0,arg1,...):fun1;...
		for (int i = 0; i < keys.length; i++) {

			//!column,column2,!column3,column4:alias4;fun(arg0,arg1,...)
			expression = keys[i];
			if (expression.contains("(") || expression.contains(")")) {
				newColumns.add(expression);
				continue;
			}

			String[] ckeys = StringUtil.split(expression);
			if (ckeys != null && ckeys.length > 0) {
				for (int j = 0; j < ckeys.length; j++) {
					String ck = ckeys[j];

					if (ck.startsWith("!")) {
						if (ck.length() <= 1) {
							throw new IllegalArgumentException("@column:value 的 value 中 " + ck + " 不合法！ !column 不允许 column 为空字符串！column,!column2,!column3,column4:alias4 中所有 column 必须符合变量名格式！");
						}
						String c = ck.substring(1);
						if (StringUtil.isName(c) == false) {
							throw new IllegalArgumentException("@column:value 的 value 中 " + c + " 不合法！ column,!column2,!column3,column4:alias4 中所有 column 必须符合变量名格式！");
						}

						String rc = keyColumnMap == null || keyColumnMap.isEmpty() ? null : keyColumnMap.get(c);
						exceptColumns.add(rc == null ? c : rc);  // 不使用数据库别名，以免 JOIN 等复杂查询情况下报错字段不存在	exceptColumnMap.put(nc == null ? c : nc, c);  // column:alias
					}
					else {
						String rc = keyColumnMap == null || keyColumnMap.isEmpty() ? null : keyColumnMap.get(ck);
						newColumns.add(rc == null ? ck : rc);  // 不使用数据库别名，以免 JOIN 等复杂查询情况下报错字段不存在 newColumns.add(rc == null ? ck : (isQueryMethod ? (rc + ":" + ck) : rc));
					}
				}
			}
		}

		boolean isEmpty = exceptColumns == null || exceptColumns.isEmpty();  // exceptColumnMap == null || exceptColumnMap.isEmpty();
		Map<String, List<String>> map = isEmpty || VERSIONED_TABLE_COLUMN_MAP == null || VERSIONED_TABLE_COLUMN_MAP.isEmpty() ? null : VERSIONED_TABLE_COLUMN_MAP.get(version);
		List<String> allColumns = map == null || map.isEmpty() ? null : map.get(table);

		if (allColumns != null && allColumns.isEmpty() == false) {

			// 不使用数据库别名，以免 JOIN 等复杂查询情况下报错字段不存在
			//			Map<String, Map<String, String>> tableColumnKeyMap = VERSIONED_COLUMN_KEY_MAP == null || VERSIONED_COLUMN_KEY_MAP.isEmpty() ? null : VERSIONED_COLUMN_KEY_MAP.get(version);
			//			Map<String, String> columnKeyMap = tableColumnKeyMap == null || tableColumnKeyMap.isEmpty() ? null : tableColumnKeyMap.get(table);

			for (String c : allColumns) {
				if (c != null && exceptColumns.contains(c) == false) {  // column:alias
					// 不使用数据库别名，以免 JOIN 等复杂查询情况下报错字段不存在		String alias = isQueryMethod == false || columnKeyMap == null || columnKeyMap.isEmpty() ? null : columnKeyMap.get(c);
					newColumns.add(c); // newColumns.add(alias == null ? c : (c + ":" + alias));
				}
			}
		}

		return newColumns;
	}


	/**适配请求参数 JSON 中 条件/赋值 键值对的 key
	 * @param key
	 * @param table
	 * @param method
	 * @return
	 */
	public static String compatInputKey(String key, String table, RequestMethod method) {
		return compatInputKey(key, table, method, null);
	}
	/**适配请求参数 JSON 中 条件/赋值 键值对的 key
	 * @param key
	 * @param table
	 * @param method
	 * @param version
	 * @return
	 * @see 先提前配置 {@link #VERSIONED_KEY_COLUMN_MAP}，然后在 {@link AbstractSQLConfig} 的子类重写 {@link AbstractSQLConfig#getKey } 并调用这个方法，例如
	 * <pre >
	   public String getKey(String key) { <br>
	  	 return ColumnUtil.compatInputKey(super.getKey(key), getTable(), version); <br>
	   }
	 * </pre>
	 */
	public static String compatInputKey(String key, String table, RequestMethod method, Integer version) {
		Map<String, Map<String, String>> tableKeyColumnMap = VERSIONED_KEY_COLUMN_MAP == null || VERSIONED_KEY_COLUMN_MAP.isEmpty() ? null : VERSIONED_KEY_COLUMN_MAP.get(version);
		Map<String, String> keyColumnMap = tableKeyColumnMap == null || tableKeyColumnMap.isEmpty() ? null : tableKeyColumnMap.get(table);
		String alias = keyColumnMap == null || keyColumnMap.isEmpty() ? null : keyColumnMap.get(key);
		return alias == null ? key : alias;
	}
	
	/**适配返回结果 JSON 中键值对的 key。可能通过不传 @column 等方式来返回原始字段名，这样就达不到隐藏真实字段名的需求了，所以只有最终这个兜底方式靠谱。
	 * @param key
	 * @param table
	 * @param method
	 * @return
	 */
	public static String compatOutputKey(String key, String table, RequestMethod method) {
		return compatOutputKey(key, table, method, null);
	}
	/**适配返回结果 JSON 中键值对的 key。可能通过不传 @column 等方式来返回原始字段名，这样就达不到隐藏真实字段名的需求了，所以只有最终这个兜底方式靠谱。
	 * @param key
	 * @param table
	 * @param method
	 * @param version
	 * @return
	 * @see 先提前配置 {@link #VERSIONED_COLUMN_KEY_MAP}，然后在 {@link AbstractSQLExecutor} 的子类重写 {@link AbstractSQLExecutor#getKey } 并调用这个方法，例如
	 * <pre >
		protected String getKey(SQLConfig config, ResultSet rs, ResultSetMetaData rsmd, int tablePosition, JSONObject table,
				int columnIndex, Map<String, JSONObject> childMap) throws Exception { <br>
			return ColumnUtil.compatOutputKey(super.getKey(config, rs, rsmd, tablePosition, table, columnIndex, childMap), config.getTable(), config.getMethod(), version); <br>
		}
	 * </pre>
	 */
	public static String compatOutputKey(String key, String table, RequestMethod method, Integer version) {
		Map<String, Map<String, String>> tableColumnKeyMap = VERSIONED_COLUMN_KEY_MAP == null || VERSIONED_COLUMN_KEY_MAP.isEmpty() ? null : VERSIONED_COLUMN_KEY_MAP.get(version);
		Map<String, String> columnKeyMap = tableColumnKeyMap == null || tableColumnKeyMap.isEmpty() ? null : tableColumnKeyMap.get(table);
		String alias = columnKeyMap == null || columnKeyMap.isEmpty() ? null : columnKeyMap.get(key);
		return alias == null ? key : alias;
	}


	/**把多个表名相关属性拼接成一个表名
	 * @param database
	 * @param schema
	 * @param datasource
	 * @param table
	 * @return
	 */
	public static String concat(String database, String schema, String datasource, String table) {
		return database + "-" + schema + "-" + datasource + "-" + table;
	}


}
