import utest._
import TestUtils.withDB
import ru.ifmo.backend_2021.quill.Queries

object QueriesTest extends TestSuite {
  def tests: Tests = Tests {
    test("Top ten by cities") - withDB { ctx =>
      val result = Queries.topTenLanguagesSpokenByCities(ctx)
      assert(
        result ==
          List(
            ("Chinese", 1083L),
            ("German", 885L),
            ("Spanish", 881L),
            ("Italian", 857L),
            ("English", 823L),
            ("Japanese", 774L),
            ("Portuguese", 629L),
            ("Korean", 608L),
            ("Polish", 557L),
            ("French", 467L)
          )
      )
    }
    test("Top ten by population") - withDB { ctx =>
      val result = Queries.topTenLanguagesSpokenByPopulation(ctx)
      assert(
        result ==
          List(
            ("Chinese", 167506950L),
            ("Spanish", 166421508L),
            ("English", 111168888L),
            ("Portuguese", 85295407L),
            ("Japanese", 77764391L),
            ("Russian", 72084610L),
            ("Arabic", 66680912L),
            ("Hindi", 49277310L),
            ("Korean", 46057820L),
            ("German", 28483738L)
          )
      )
    }
  }
}
