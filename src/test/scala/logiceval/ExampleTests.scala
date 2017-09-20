package logiceval

import logiceval.AbstractSyntax._
import logiceval.Dsl._
import org.scalatest.FunSuite

class ExampleTests extends FunSuite {

  test("CRDT ") {

    val structure = new Structure {
      override def values(typ: AbstractSyntax.Type): Stream[Any] = typ match {
        case SetType(elementType) => ???
        case MapType(keyType, valueType) => ???
        case DataType(name, fields) => ???
        case CustomType(name) => ???
      }

      override def interpretConstant(constantName: String): Any = constantName match {
        case "visibleCalls" => Set("C1", "C2", "C3", "C4")
        case "happensBefore" => Set(

        )
      }
    }

    val c1: Varuse = "c1"
    val c2: Varuse = "c2"
    val visibleCalls: Varuse = "visibleCalls"
    val happensBefore: Varuse = "happensBefore"
    val call = CustomType("call")

    val formula: Expr =
      exists("c1" :: call)(
        (c1 in visibleCalls)
        &&& (GetField("c1", "op") =:= ConstructDatatype("add", Map("value" -> "x")))
        &&& forall("c2" :: call)(
          (c2 in visibleCalls
            &&& (GetField("c1", "op") =:= ConstructDatatype("add", Map("value" -> "x"))))
            ==> (ConstructDatatype("pair", Map("1" -> c1, "2" -> c2)) in happensBefore)
        )
      )

    val res = Evaluate.eval(formula, structure)

  }

}
