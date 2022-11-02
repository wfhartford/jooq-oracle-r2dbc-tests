package com.ze.jooq.oracletest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.testcontainers.containers.OracleContainer
import java.time.Instant

class OracleR2dbcSpec : FunSpec() {
  companion object {
    const val slimImage = "gvenzl/oracle-xe:18.4.0-slim"
    const val fastImage = "gvenzl/oracle-xe:18.4.0-slim-faststart"
    val keyValueTable = DSL.table("KEY_VALUE_TABLE")
    val keyTimeTable = DSL.table("KEY_TIME_TABLE")
    val keyField = DSL.field("KEY", SQLDataType.VARCHAR(32).notNull())
    val valueField = DSL.field("VALUE", SQLDataType.VARCHAR(32))
    val timeField = DSL.field("TIME", SQLDataType.INSTANT)
  }
  init {
    // the fastImage is much larger, but slightly faster to start at about 20s
    // the slimImage is much smaller, and slightly slower to start at about 30s
    // either image produces the same test results.
    val container = OracleContainer(fastImage)

    lateinit var dslContext: DSLContext
    beforeSpec {
      container.start()
      val connectionFactory = ConnectionFactories.get(
        ConnectionFactoryOptions.builder()
          .option(ConnectionFactoryOptions.DRIVER, "oracle")
          .option(ConnectionFactoryOptions.HOST, container.host)
          .option(ConnectionFactoryOptions.PORT, container.oraclePort)
          .option(ConnectionFactoryOptions.USER, container.username)
          .option(ConnectionFactoryOptions.PASSWORD, container.password)
          .option(ConnectionFactoryOptions.DATABASE, container.databaseName)
          .build()
      )
      dslContext = DSL.using(connectionFactory)
      dslContext.createTable(keyValueTable)
        .column(keyField)
        .column(valueField)
        .primaryKey(keyField)
        .awaitFirstOrNull()
      dslContext.createTable(keyTimeTable)
        .column(keyField)
        .column(timeField)
        .primaryKey(keyField)
        .awaitFirstOrNull()
    }
    afterSpec { container.close() }
    afterEach {
      dslContext.truncateTable(keyValueTable).awaitFirstOrNull()
      dslContext.truncateTable(keyTimeTable).awaitFirstOrNull()
    }

    context("statements resulting in a row count") {
      test("insert without assigning result") {
        // this test is expected to pass

        dslContext.insertInto(keyValueTable)
          .columns(keyField, valueField)
          .values("one", "1")
          .awaitSingle()

        val rows = dslContext.select(keyField, valueField)
          .from(keyValueTable)
          .asFlow()
          .toList()

        rows.size shouldBe 1
      }
      test("insert assigning result") {
        // ClassCastException is thrown due to this assignment
        val result = dslContext.insertInto(keyValueTable)
          .columns(keyField, valueField)
          .values("one", "1")
          .awaitSingle()

        // This check is never reached
        result shouldBe 1
      }
      test("update without assigning result") {
        // as with the insert case, this test passes
        dslContext.update(keyValueTable)
          .set(valueField, "1")
          .where(keyField.eq("one"))
          .awaitSingle()
      }
      test("update assigning result") {
        // as with the insert case, this test throws ClassCastException
        val result = dslContext.update(keyValueTable)
          .set(valueField, "1")
          .where(keyField.eq("one"))
          .awaitSingle()

        result shouldBe 0
      }
    }

    context("table with an instant") {
      // All of these tests fail, there seems to be a bug binding an instant to a JDBC statement
      test("select all") {
        dslContext.select(keyField)
          .where(timeField.lt(Instant.now()))
          .asFlow()
          .toList()
      }
      test("insert a row") {
        dslContext.insertInto(keyTimeTable)
          .columns(keyField, timeField)
          .values("now", Instant.now())
          .awaitSingle()
      }
      test("update a time") {
        dslContext.update(keyTimeTable)
          .set(timeField, Instant.now())
          .where(keyField.eq("now"))
          .awaitSingle()
      }
      test("update where time") {
        dslContext.update(keyTimeTable)
          .set(keyField, "foo")
          .where(timeField.eq(Instant.now()))
          .awaitSingle()
      }
    }
  }
}
