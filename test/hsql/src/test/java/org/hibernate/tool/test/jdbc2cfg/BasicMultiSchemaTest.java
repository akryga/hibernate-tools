/*
 * Created on 2004-11-23
 *
 */
package org.hibernate.tool.test.jdbc2cfg;

import java.sql.SQLException;

import org.hibernate.cfg.Environment;
import org.hibernate.cfg.JDBCMetaDataConfiguration;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.tools.test.util.JUnitUtil;
import org.hibernate.tools.test.util.JdbcUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author max
 * @author koen
 * 
 */
public class BasicMultiSchemaTest {

	private static final String[] CREATE_SQL = new String[] {
				"create table basic ( a int not null, name varchar(20), primary key (a)  )",
				"create table somecolumnsnopk ( pk varchar(25) not null, b char, c int not null, aBoolean boolean )",
				"create table multikeyed ( orderid varchar(10), customerid varchar(10), name varchar(10), primary key(orderid, customerid) )",
				"create schema otherschema",
				"create table otherschema.basic ( a int not null, name varchar(20), primary key (a)  )",
			};

	private static final String[] DROP_SQL = new String[]  {
	        "drop table basic", 
	        "drop table somecolumnsnopk",
			"drop table multikeyed",
			"drop table otherschema.basic",
			"drop schema otherschema"
		};

	private JDBCMetaDataConfiguration jmdcfg = null;

	@Before
	public void setUp() {
		JdbcUtil.establishJdbcConnection(this);
		JdbcUtil.executeDDL(this, CREATE_SQL);
		jmdcfg = new JDBCMetaDataConfiguration();
		jmdcfg.setProperty(Environment.DEFAULT_SCHEMA, "PUBLIC");
		jmdcfg.readFromJDBC();
	}

	@After
	public void tearDown() {
		JdbcUtil.executeDDL(this, DROP_SQL);
		JdbcUtil.releaseJdbcConnection(this);
	}

	@Test
	public void testBasic() throws SQLException {

		JUnitUtil.assertIteratorContainsExactly(
				"There should be three tables!", 
				jmdcfg.getMetadata().getEntityBindings().iterator(),
				3);

		Table table = jmdcfg.getTable( JdbcUtil.toIdentifier(this, "basic" ) );

		Assert.assertEquals( 
				JdbcUtil.toIdentifier(this, "basic"), 
				JdbcUtil.toIdentifier(this, table.getName()) );
		Assert.assertEquals( 2, table.getColumnSpan() );

		Column basicColumn = table.getColumn( 0 );
		Assert.assertEquals( 
				JdbcUtil.toIdentifier(this, "a"), 
				JdbcUtil.toIdentifier(this, basicColumn.getName() ));
		
		// TODO: we cannot call getSqlType(dialect,cfg) without a
		// MappingassertEquals("INTEGER", basicColumn.getSqlType() ); // at
		// least on hsqldb
		// assertEquals(22, basicColumn.getLength() ); // at least on oracle

		PrimaryKey key = table.getPrimaryKey();
		Assert.assertNotNull( "There should be a primary key!", key );
		Assert.assertEquals( key.getColumnSpan(), 1 );

		Column column = key.getColumn( 0 );
		Assert.assertTrue( column.isUnique() );

		Assert.assertSame( basicColumn, column );

	}

	public void testScalePrecisionLength() {
		Table table = jmdcfg.getTable( JdbcUtil.toIdentifier(this, "basic" ) );
		Column nameCol = table.getColumn( new Column( JdbcUtil.toIdentifier(this, "name" ) ) );
		Assert.assertEquals( nameCol.getLength(), 20 );
		Assert.assertEquals( nameCol.getPrecision(), Column.DEFAULT_PRECISION );
		Assert.assertEquals( nameCol.getScale(), Column.DEFAULT_SCALE );
	}

	
/*	public void testAutoDetectSingleSchema() {
		
		//read single schema without default schema: result = no schema info in tables.
		JDBCMetaDataConfiguration mycfg = new JDBCMetaDataConfiguration();
		mycfg.setReverseEngineeringStrategy(new DelegatingReverseEngineeringStrategy(new DefaultReverseEngineeringStrategy()) {
			public boolean excludeTable(TableIdentifier ti) {
				return !"PUBLIC".equals(ti.getSchema());				
			}
		});
		mycfg.getProperties().remove(org.hibernate.cfg.Environment.DEFAULT_SCHEMA);
		mycfg.readFromJDBC();			
		
		Table table = getTable(mycfg, identifier("otherschema"));
		assertNull("rev.eng.strategy should have excluded this table",table);
		
		table = getTable(mycfg, identifier("basic"));
		assertNotNull(table);
		assertNull(table.getSchema());
		
		
		//read single schema with default schema: result = no schema info in tables.
		
		//read other single schema than default schema: result = schema info in tables.
		
	}*/
	
	/*
	 * public void testGetTables() {
	 * 
	 * Table table = new Table(); table.setName("dummy"); cfg.addTable(table);
	 * 
	 * Table foundTable = cfg.getTable(null,null,"dummy");
	 * 
	 * assertSame(table,foundTable);
	 * 
	 * foundTable = cfg.getTable(null,"dschema", "dummy");
	 * 
	 * assertNotSame(table, foundTable); }
	 */

	public void testCompositeKeys() {
		Table table = jmdcfg.getTable( JdbcUtil.toIdentifier(this, "multikeyed"));
		PrimaryKey primaryKey = table.getPrimaryKey();
		Assert.assertEquals( 2, primaryKey.getColumnSpan() );
	}

}
