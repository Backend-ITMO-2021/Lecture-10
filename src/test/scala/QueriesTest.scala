import utest._
import TestUtils.withDB
import ru.ifmo.backend_2021.quill.Queries

object QueriesTest extends TestSuite {
  def tests: Tests = Tests {
    test("Top ten by cities") - withDB { ctx => 
      val result = Queries.topTenLanguagesSpokenByCities(ctx)
      assert(result ==
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
    // test("Top ten by population") - withDB { ctx =>
    //   val result = Queries.topTenLanguagesSpokenByPopulation(ctx)
    //   assert(result ==
    //     List(
    //       ("Chinese", 16750695023L),
    //       ("Spanish", 16642150880L),
    //       ("English", 11116888890L),
    //       ("Portuguese", 8529540725L),
    //       ("Japanese", 7776439163L),
    //       ("Russian", 7208461083L),
    //       ("Arabic", 6668091278L),
    //       ("Hindi", 4927731070L),
    //       ("Korean", 4605782000L),
    //       ("German", 2848373842L)
    //     )
    //   )
    // }
  }
}
