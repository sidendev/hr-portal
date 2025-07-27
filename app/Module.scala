import com.google.inject.AbstractModule
import utils.Startup

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Startup]).asEagerSingleton()
  }
}
