import com.typesafe.config.{ConfigFactory, ConfigValue, ConfigValueFactory}
import org.scalatest.{BeforeAndAfterAll, FunSuite, MustMatchers, WordSpecLike}
import actors.messages.{MasterNotFound, SuccessfulSend}
import akka.testkit.{ImplicitSender, TestActors, TestKit, TestProbe}
import akka.actor.{ActorSystem, Props}
import com.gu.contentatom.thrift.{ChangeRecord, User}
import scala.concurrent.duration._

class TestUnattachedAtomActor extends TestKit(ActorSystem("TestUnattachedAtomActor"))
  with WordSpecLike with MustMatchers with BeforeAndAfterAll with TestDynamoDB {

  private val config = ConfigFactory.empty()
    .withValue("unattached_atoms_table",ConfigValueFactory.fromAnyRef(s"testUnattachedTable"))
    .withValue("region",ConfigValueFactory.fromAnyRef("eu-west-1"))

  override def beforeAll(): Unit = {
    if(sys.props.get("DISABLE_DYNAMOTESTS").isEmpty)
      createTestTable(getClient(config.getString("region")), config.getString("unattached_atoms_table"))
  }

  override def afterAll(): Unit = {
    if(sys.props.get("DISABLE_DYNAMOTESTS").isEmpty)
      deleteTestTable(getClient(config.getString("region")), config.getString("unattached_atoms_table"))
    TestKit.shutdownActorSystem(system)
  }

  "UnattachedAtomActor" must {
    "log an entry to dynamo" in {
      if(sys.props.get("DISABLE_DYNAMOTESTS").isEmpty) {
        val actor = system.actorOf(Props(new actors.UnattachedAtomActor(config)), "UnattachedAtomActor")

        val createdChange = ChangeRecord(1509631574000L, Some(User(email = "bob@smith.com")))
        val updatedChange = ChangeRecord(1509631874000L, Some(User(email = "jen@smith.com")))

        val sender = TestProbe("ProbeUnattachedAtom")
        implicit val senderRef = sender.ref

        actor ! MasterNotFound("atomidxxxxxx", Some(createdChange), Some(updatedChange), 1)

        sender.expectMsg(30 seconds, SuccessfulSend)
      } else {
        1 must be(1)
      }
    }
  }
}
