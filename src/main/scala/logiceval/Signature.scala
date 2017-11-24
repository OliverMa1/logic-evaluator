package logiceval

trait Signature {

  def loookupType(name: String): Type

  def lookupConstant(name: String): Type

}
