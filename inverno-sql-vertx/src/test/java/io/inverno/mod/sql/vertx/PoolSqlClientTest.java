package io.inverno.mod.sql.vertx;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;

import io.inverno.mod.sql.PreparedStatement;
import io.inverno.mod.sql.Row;
import io.inverno.mod.sql.RowMetadata;
import io.inverno.mod.sql.SqlClient;
import io.inverno.mod.sql.SqlResult;
import io.inverno.mod.sql.Statement;
import io.vertx.core.Future;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@TestMethodOrder(OrderAnnotation.class)
@EnabledIf( value = "isEnabled", disabledReason = "Failed to connect to test Postgres database" )
public class PoolSqlClientTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	private static final PgConnectOptions PG_CONNECT_OPTIONS = new PgConnectOptions()
			.setHost("localhost")
			.setPort(5432)
			.setDatabase("postgres")
			.setUser("postgres")
			.setPassword("password");
	
	private static final PoolOptions POOL_OPTIONS = new PoolOptions().setMaxSize(1);
	
	private SqlClient createClient() {
		return new PoolSqlClient(PgPool.pool(PG_CONNECT_OPTIONS, POOL_OPTIONS));
	}
	
	public static boolean isEnabled() throws InterruptedException, ExecutionException {
		PgPool pool = PgPool.pool(PG_CONNECT_OPTIONS, POOL_OPTIONS);
		
		try {
			return pool.getConnection().transform(ar -> {
				if(ar.succeeded()) {
					ar.result().close();
					return Future.succeededFuture(true);
				}
				else {
					return Future.succeededFuture(false);
				}
			}).toCompletionStage().toCompletableFuture().get();
		}
		finally {
			pool.close();
		}
	}
	
	@Test
	@Order(0)
	public void testCreateTable() {
		SqlClient client = this.createClient();
		
		try {
			// These actually return results which is not natural, it should complete successfully or not
			Flux.from(client.statement("DROP TABLE IF EXISTS test;").execute()).blockLast();  
			Flux.from(client.statement("CREATE TABLE test (id integer NOT NULL, message varchar(2048) NOT NULL, PRIMARY KEY (id))").execute()).blockLast();
		}
		finally {
			client.close();
		}
	}
	
	@Test
	@Order(1)
	public void testInsert() {
		SqlClient client = this.createClient();
		
		try {
			Statement statement = client.statement("INSERT INTO test VALUES(1, 'message 1')");
			
			List<SqlResult> results = Flux.from(statement.execute()).collectList().block();
			
			Assertions.assertEquals(1, results.size());
			Assertions.assertEquals(1, results.get(0).rowsUpdated().block());
		}
		finally {
			client.close();
		}
	}
	
	@Test
	@Order(2)
	public void testQuery() {
		SqlClient client = this.createClient();
		
		try {
			Statement statement = client.statement("select * from test");
			
			List<Row> results = Flux.from(statement.execute())
				.flatMap(SqlResult::rows)
				.collectList()
				.block();
				
			Assertions.assertEquals(1, results.size());
			
			Assertions.assertEquals(1, results.get(0).getInteger("id"));
			Assertions.assertEquals("message 1", results.get(0).getString("message"));
		}
		finally {
			client.close().block();
		}
	}
	
	@Test
	@Order(3)
	public void testQueryBatch() {
		SqlClient client = this.createClient();
		
		try {
			Statement queryStatement = client.statement("select * from test").and("select message from test");
			
			List<SqlResult> queryResults = Flux.from(queryStatement.execute())
				.collectList()
				.block();
				
			Assertions.assertEquals(2, queryResults.size());
			
			Iterator<SqlResult> queryResultsIterator = queryResults.iterator();
			
			SqlResult result = queryResultsIterator.next();
			
			List<Row> rows = Flux.from(result.rows()).collectList().block();
			
			Assertions.assertEquals(1, rows.size());
			
			RowMetadata metadata = result.getRowMetadata();
			Assertions.assertArrayEquals(new String[] {"id", "message"}, metadata.getColumnNames().stream().toArray(String[]::new));
			
			Assertions.assertEquals(1, rows.get(0).getInteger("id"));
			Assertions.assertEquals("message 1", rows.get(0).getString("message"));
			
			result = queryResultsIterator.next();
			
			rows = Flux.from(result.rows()).collectList().block();
			
			Assertions.assertEquals(1, rows.size());
			
			metadata = result.getRowMetadata();
			Assertions.assertArrayEquals(new String[] {"message"}, metadata.getColumnNames().stream().toArray(String[]::new));
			
			Assertions.assertEquals("message 1", rows.get(0).getString("message"));
		}
		finally {
			client.close().block();
		}
	}
	
	@Test
	@Order(4)
	public void testPreparedInsert() {
		SqlClient client = this.createClient();
		
		try {
			// TODO the API is fluent, calling bind multiple time change the state of the object: we don't create a new object each time bind is invoked
			// It might be interesting to do so: it is maybe more natural and the name prepared statement say it all: it is a prepared statement however a preparedstatement is linked to a connection so...
			// At least this has to be clearly explained in the doc
			PreparedStatement insertStatement = client
				.preparedStatement("INSERT INTO test VALUES($1, $2)")
				.bind(2, "message 2");
			
			List<SqlResult> insertResults = Flux.from(insertStatement.execute())
				.collectList()
				.block();
				
			Assertions.assertEquals(1, insertResults.size());
			Assertions.assertEquals(1, insertResults.get(0).rowsUpdated().block());
			
			Statement queryStatement = client.statement("select * from test");
			
			List<Row> queryResults = Flux.from(queryStatement.execute())
				.flatMap(SqlResult::rows)
				.collectList()
				.block();
				
			Assertions.assertEquals(2, queryResults.size());
			
			Assertions.assertEquals(1, queryResults.get(0).getInteger("id"));
			Assertions.assertEquals("message 1", queryResults.get(0).getString("message"));
			
			Assertions.assertEquals(2, queryResults.get(1).getInteger("id"));
			Assertions.assertEquals("message 2", queryResults.get(1).getString("message"));
		}
		finally {
			client.close().block();
		}
	}
	
	@Test
	@Order(5)
	public void testPreparedQuery() {
		SqlClient client = this.createClient();
		
		try {
			// TODO the API is fluent, calling bind multiple time change the state of the object: we don't create a new object each time bind is invoked
			// It might be interesting to do so: it is maybe more natural and the name prepared statement say it all: it is a prepared statement however a preparedstatement is linked to a connection so...
			// At least this has to be clearly explained in the doc
			PreparedStatement queryStatement = client
				.preparedStatement("select * from test where id = $1")
				.bind(2);
			
			List<Row> queryResults = Flux.from(queryStatement.execute())
				.flatMap(SqlResult::rows)
				.collectList()
				.block();
				
			Assertions.assertEquals(1, queryResults.size());
			
			Assertions.assertEquals(2, queryResults.get(0).getInteger("id"));
			Assertions.assertEquals("message 2", queryResults.get(0).getString("message"));
		}
		finally {
			client.close().block();
		}
	}
	
	@Test
	@Order(6)
	public void testPreparedInsertBatch() {
		SqlClient client = this.createClient();
		
		try {
			// TODO the API is fluent, calling bind multiple time change the state of the object: we don't create a new object each time bind is invoked
			// It might be interesting to do so: it is maybe more natural and the name prepared statement say it all: it is a prepared statement however a preparedstatement is linked to a connection so...
			// At least this has to be clearly explained in the doc
			PreparedStatement insertStatement = client
				.preparedStatement("INSERT INTO test VALUES($1, $2)")
				.bind(3, "message 3")
				.and().bind(4, "message 4");
				
			
			List<SqlResult> insertResults = Flux.from(insertStatement.execute())
				.collectList()
				.block();
				
			Assertions.assertEquals(2, insertResults.size());
			Assertions.assertEquals(1, insertResults.get(0).rowsUpdated().block());
			Assertions.assertEquals(1, insertResults.get(1).rowsUpdated().block());
			
			Statement queryStatement = client.statement("select * from test");
			
			List<Row> queryResults = Flux.from(queryStatement.execute())
				.flatMap(SqlResult::rows)
				.collectList()
				.block();
				
			Assertions.assertEquals(4, queryResults.size());
			
			Iterator<Row>  queryResultsIterator = queryResults.iterator();
			
			Row row = queryResultsIterator.next();
			Assertions.assertEquals(1, row.getInteger("id"));
			Assertions.assertEquals("message 1", row.getString("message"));
			
			row = queryResultsIterator.next();
			Assertions.assertEquals(2, row.getInteger("id"));
			Assertions.assertEquals("message 2", row.getString("message"));
			
			row = queryResultsIterator.next();
			Assertions.assertEquals(3, row.getInteger("id"));
			Assertions.assertEquals("message 3", row.getString("message"));
			
			row = queryResultsIterator.next();
			Assertions.assertEquals(4, row.getInteger("id"));
			Assertions.assertEquals("message 4", row.getString("message"));
		}
		finally {
			client.close().block();
		}
	}
	
	@Test
	@Order(6)
	public void testPreparedQueryBatch() {
		SqlClient client = this.createClient();
		
		try {
			// TODO the API is fluent, calling bind multiple time change the state of the object: we don't create a new object each time bind is invoked
			// It might be interesting to do so: it is maybe more natural and the name prepared statement say it all: it is a prepared statement however a preparedstatement is linked to a connection so...
			// At least this has to be clearly explained in the doc
			PreparedStatement queryStatement = client
				.preparedStatement("select * from test where id = $1")
				.bind(2)
				.and().bind(4);
			
			List<Row> queryResults = Flux.from(queryStatement.execute())
				.flatMap(SqlResult::rows)
				.collectList()
				.block();
				
			Assertions.assertEquals(2, queryResults.size());
			
			Assertions.assertEquals(2, queryResults.get(0).getInteger("id"));
			Assertions.assertEquals("message 2", queryResults.get(0).getString("message"));
			
			Assertions.assertEquals(4, queryResults.get(1).getInteger("id"));
			Assertions.assertEquals("message 4", queryResults.get(1).getString("message"));
		}
		finally {
			client.close().block();
		}
	}
	
	@Test
	@Order(7)
	public void testTransactionCommit() {
		SqlClient client = this.createClient();
		
		try {
			Flux.from(client.transaction(ops -> Flux
				.from(ops.statement("INSERT INTO test VALUES(5, 'message 5')").execute())
				.thenMany(ops.statement("SELECT * FROM test where id = 5").execute())
				.flatMap(SqlResult::rows)
				.count()
				.doOnSuccess(count -> Assertions.assertEquals(1, count))
			)).collectList().block();
			
			Assertions.assertEquals(1, Flux.from(client.statement("SELECT * FROM test where id = 5").execute())
				.flatMap(SqlResult::rows)
				.count()
				.block());
		}
		finally {
			client.close();
		}
	}
	
	@Test
	@Order(8)
	public void testTransactionRollback() {
		SqlClient client = this.createClient();
		
		try {
			try {
				Flux.from(client.transaction(ops -> Flux
					.from(ops.statement("INSERT INTO test VALUES(6, 'message 6')").execute())
					.thenMany(ops.statement("SELECT * FROM test where id = 6").execute())
					.flatMap(SqlResult::rows)
					.count()
					.doOnSuccess(count -> Assertions.assertEquals(1, count))
					.then(Mono.error(() -> new RuntimeException("Fail the transaction which must rollback")))
				)).collectList().block();
			}
			catch (RuntimeException e) {
				Assertions.assertEquals("Fail the transaction which must rollback", e.getMessage());
			}
			
			Assertions.assertEquals(0, Flux.from(client.statement("SELECT * FROM test where id = 6").execute())
				.flatMap(SqlResult::rows)
				.count()
				.block());
		}
		finally {
			client.close();
		}
	}
	
	@Test
	@Order(9)
	public void testStream() {
		// Stream only works within a transaction using a prepared statement with no batch
		SqlClient client = this.createClient();
		
		try {
			List<Row> queryResults = Flux.from(client.transaction(ops -> Flux
				.from(ops.preparedStatement("SELECT * FROM test").execute())
				.flatMap(SqlResult::rows)
			)).collectList().block();
			
			Assertions.assertEquals(5, queryResults.size());
			
			Iterator<Row>  queryResultsIterator = queryResults.iterator();
			
			Row row = queryResultsIterator.next();
			Assertions.assertEquals(1, row.getInteger("id"));
			Assertions.assertEquals("message 1", row.getString("message"));
			
			row = queryResultsIterator.next();
			Assertions.assertEquals(2, row.getInteger("id"));
			Assertions.assertEquals("message 2", row.getString("message"));
			
			row = queryResultsIterator.next();
			Assertions.assertEquals(3, row.getInteger("id"));
			Assertions.assertEquals("message 3", row.getString("message"));
			
			row = queryResultsIterator.next();
			Assertions.assertEquals(4, row.getInteger("id"));
			Assertions.assertEquals("message 4", row.getString("message"));
			
			row = queryResultsIterator.next();
			Assertions.assertEquals(5, row.getInteger("id"));
			Assertions.assertEquals("message 5", row.getString("message"));
		}
		finally {
			client.close();
		}
	}
	
	@Test
	@Order(10)
	public void testDropTable() {
		SqlClient client = this.createClient();
		
		try {
			Flux.from(client.statement("DROP TABLE test;").execute()).blockLast();  
		}
		finally {
			client.close();
		}
	}
}
