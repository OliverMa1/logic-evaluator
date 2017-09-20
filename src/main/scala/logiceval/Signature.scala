package logiceval

import logiceval.AbstractSyntax._

trait Signature {

  def loookupType(name: String): Type

  def lookupConstant(name: String): Type

}
