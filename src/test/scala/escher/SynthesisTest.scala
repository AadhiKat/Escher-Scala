package escher

import escher.DSL._
import escher.Synthesis.BufferedOracle
import org.scalatest.WordSpec


class SynthesisTest extends WordSpec{
  import DSL._
  import SynthesisTyped._

  import escher.Term.Component

  "typesForCosts check" in {
    val st = new SynthesisTyped(Config(), print)
    import st._

    val state = new SynthesisState(
      IS(),
      TypeMap.empty(1),
      tyVar(0)
    )

    state.openToLevel(2)
    state.registerTermAtLevel(1, tyInt, "one" $(), IS())
    state.registerTermAtLevel(2, tyList(tyInt), "intList" $(), IS())
    state.registerTermAtLevel(2, tyList(tyVar(0)), "nil" $(), IS())
    state.registerTermAtLevel(3, tyPair(tyInt, tyBool), "intBool" $(), IS())

    assert {
      typesForCosts(state, IS(1, 1), IS(tyInt, tyInt), tyBool).toSet === Set((IS(tyInt, tyInt), tyBool))
    }

    assert {
      typesForCosts(state, IS(1, 2), IS(tyInt, tyList(tyVar(0))), tyBool).toSet ===
        Set((IS(tyInt, tyList(tyInt)), tyBool), (IS(tyInt, tyList(tyVar(0))), tyBool))
    }

    assert {
      typesForCosts(state, IS(1, 2), IS(tyInt, tyList(tyVar(0))), tyVar(0)).toSet ===
        Set((IS(tyInt, tyList(tyInt)), tyInt), (IS(tyInt, tyList(tyVar(0))), tyVar(0)))
    }

    assert {
      typesForCosts(state, IS(3, 1), IS(tyPair(tyVar(0), tyVar(0)), tyVar(0)), tyList(tyVar(1))).toSet === Set()
    }

    assert {
      typesForCosts(state, IS(3, 1), IS(tyPair(tyVar(0), tyVar(1)), tyVar(0)), tyList(tyVar(2))).toSet ===
        Set((IS(tyPair(tyInt, tyBool), tyInt), tyList(tyVar(0))))
    }

    assert {
      // check nested list
      typesForCosts(state, IS(2,2), IS(tyVar(0), tyList(tyVar(0))), tyList(tyInt)).toSet ===
        Set(IS(tyList(tyInt), tyList(tyVar(0)))-> tyList(tyInt),
          IS(tyList(tyVar(0)), tyList(tyVar(0))) -> tyList(tyInt))
    }

    assert {
      typesForCosts(state, IS(2,2), IS(tyVar(0), tyList(tyVar(0))), tyList(tyVar(0))).toSet ===
        Set(IS(tyList(tyInt), tyList(tyVar(0))) -> tyList(tyList(tyInt)),
          IS(tyList(tyVar(0)), tyList(tyVar(0))) -> tyList(tyList(tyVar(0))))
    }
  }

  "ValueMap" should {
    import escher.Synthesis.ValueMap._

    def map(xs: (Int, TermValue)*) = Map[Int, TermValue](xs :_*)

    "split correctly" in {
      assert {
        splitValueMap(Map(1 -> 1, 2 -> 2, 3 -> 0, 4 -> 0), IS(0, 1, 2, 3, 4, 5, 6)).get ===
          (map(1 -> true, 2 -> true, 3 -> false, 4 -> false), map(1 -> 1, 2 -> 2), map(3 -> 0, 4 -> 0))
      }

      assert {
        splitValueMap(Map(1 -> 1, 2->0, 3 ->0, 4->4), IS(0,1,2,3,4,5,6)).get ===
          (map(1 -> true, 2 -> false, 3 -> false, 4 -> true), map(1->1, 4->4), map(2->0, 3->0))
      }
    }
  }
}
