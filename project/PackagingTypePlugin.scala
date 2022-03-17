import sbt.AutoPlugin

//https://github.com/sbt/sbt/issues/3618
object PackagingTypePlugin extends AutoPlugin {
    override val buildSettings = {
        sys.props += "packaging.type" -> "jar"
        Nil
    }
}