package services

import cats.effect.{MonadCancel, Resource}
import cats.{Applicative, Monad, MonadError}
import com.datastax.oss.driver.api.core.{CqlSession}
import cats.implicits._
import com.datastax.oss.driver.api.core.cql.{ResultSet}
import com.datastax.oss.driver.api.core.data.UdtValue

import scala.collection.mutable
import java.util.UUID
import domain.Advert.AdVariant
import domain.ABTest._
import domain.Brand._
import persistence.CassandraDb
import persistence.PracticeDb._

import scala.jdk.CollectionConverters._
import java.time.{LocalDateTime, ZoneOffset}

object TestService {

  // Algebra
  trait TestRoutesAlgebra[F[_]] {
    def addTest(newTest: NewTest): F[QueryResult]
    def updateTestName(testId: TestId, newTestName: TestName): F[mutable.ArrayBuffer[Test]]
    def updateTestStatus(testId: TestId): F[mutable.ArrayBuffer[Test]]
    def getTestById(testId: TestId): F[Either[QueryResult, ResultSet]]
    def getTestsByBrand(brand: BrandName): F[Option[List[Test]]]
  }

  def createTest(newTest: NewTest): Test = {
    val testId: TestId = TestId(UUID.randomUUID())
    val variantSpend: BigDecimal = newTest.testSpend / newTest.adTextVariants.length
    val adVariants: Vector[AdVariant] = newTest.adTextVariants.map(adText => AdVariant(UUID.randomUUID(), adText, variantSpend, false))
    val testSubmissionDate: LocalDateTime = LocalDateTime.now()
    val testStartDate: LocalDateTime = emptyDateTime
    val testStatus: TestStatus = Pending
    val testUpdate: TestUpdate = NoUpdate
    Test(testId, newTest.brandName, newTest.testName, adVariants, newTest.testSpend, newTest.testDuration,
      testSubmissionDate, testStartDate, testStatus, testUpdate)
  }

  sealed trait QueryResult
  final case object DatabaseConnectionError extends QueryResult
  final case object AddTestQuerySuccess extends QueryResult
  final case object AddTestQueryError extends QueryResult
  final case object TestIdNotFoundError extends QueryResult

  // Interpreter
  case class TestRoutesInterpreter[F[_]]()(implicit mc: MonadCancel[F, Throwable]) extends TestRoutesAlgebra[F] {
    def dbConnection(database: CassandraDb): Resource[F, CqlSession] = {
      Resource.make(MonadError[F, Throwable].catchNonFatal(database.session())) {
        connection => Applicative[F].pure(println(s"Releasing database connection: '$connection''"))
      }
    }
    val database = new CassandraDb()
    private val connectionDb = dbConnection(database)

    def addTest(newTest: NewTest): F[QueryResult] = {
      val test = createTest(newTest)

      connectionDb.use { connection =>
        executeAddTestQuery(connection, test)
          .map(_ => AddTestQuerySuccess: QueryResult)
      }.handleErrorWith { error =>
        println(error)
        Monad[F].pure(AddTestQueryError: QueryResult)
      }
    }

    def executeAddTestQuery(connection: CqlSession, test: Test): F[ResultSet] = {
      val statement = connection.prepare(
        """
       INSERT INTO abtest.test(testid, brandname, testname, advariants, testduration, testspend,
       testsubmissiondate, teststartdate, teststatus, testupdate)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """)
      val udtType = connection.getMetadata.getKeyspace("abtest").flatMap(_.getUserDefinedType("advariant")).get
      val udtValue: UdtValue = udtType.newValue()

      val adVariantsList = test.adVariants.toList.map(adVariant => {
        udtValue.setUuid("variantid", adVariant.variantid)
        udtValue.setString("adtext", adVariant.adtext)
        udtValue
      })

      val boundStatement = statement.bind(
        test.testId.testId, test.brandName.brandName, test.testName.testName, adVariantsList.asJava, test.testDuration, test.testSpend.bigDecimal,
        test.testSubmissionDate.toInstant(ZoneOffset.UTC), null, test.testStatus.toString, test.testUpdate.toString
      )

      val queryResult = connection.execute(boundStatement)
      Monad[F].pure(queryResult)
    }

    def updateTestName(testId: TestId, newTestName: TestName): F[mutable.ArrayBuffer[Test]] = {
      ???
    }

    def updateTestStatus(testId: TestId): F[mutable.ArrayBuffer[Test]] = {
      ???
    }

    def getTestById(testId: TestId): F[Either[QueryResult, ResultSet]] = {
      connectionDb.use { connection =>
        executeGetTestByIdQuery(connection, testId).map[Either[QueryResult, ResultSet]] { result =>
          if (result.getAvailableWithoutFetching >= 1) {
            Right(result)
          } else {
            Left(TestIdNotFoundError)
          }
        }
      }.handleErrorWith { error =>
        println(error)
        Monad[F].pure(Left(DatabaseConnectionError))
      }
    }

    def executeGetTestByIdQuery(connection: CqlSession, testId: TestId): F[ResultSet] = {
      val statement = connection.prepare("SELECT * FROM abtest.test WHERE testid = ?")
      val boundStatement = statement.bind(testId.testId)
      println(s"Executing CQL query with parameters $testId")
      val queryResult = connection.execute(boundStatement)
      println(s"Query results: $queryResult")
      Monad[F].pure(queryResult)
    }

    def getTestsByBrand(brandName: BrandName): F[Option[List[Test]]] = {
      val testsByBrand = brandMappedDb().get(brandName).map(_.toList)
      Monad[F].pure(testsByBrand)
    }
  }
}

