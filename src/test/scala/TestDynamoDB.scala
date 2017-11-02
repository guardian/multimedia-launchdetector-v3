import java.net.InetAddress
import java.time.LocalDateTime

import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.amazonaws.services.dynamodbv2.model.{AttributeDefinition, CreateTableRequest, KeySchemaElement, ProvisionedThroughput}

trait TestDynamoDB {
  def getMyHostname:String = {
    val localhost = InetAddress.getLocalHost
    localhost.getCanonicalHostName
  }

  def getTimestamp:String = LocalDateTime.now().toString

  def getTableStatus(client:AmazonDynamoDB, tableName:String):String = {
    val result = client.describeTable(tableName)
    result.getTable.getTableStatus
  }

  def createTestTable(client:AmazonDynamoDB, tableName:String):Unit = {
    val rq = new CreateTableRequest()
      .withKeySchema(new KeySchemaElement("AtomID","HASH"))
      .withAttributeDefinitions(new AttributeDefinition("AtomID","S"))
      .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L))
      .withTableName(tableName)

    try {
      val createResult = client.createTable(rq)
    } catch {
      case e:com.amazonaws.services.dynamodbv2.model.ResourceInUseException=>
        createTestTable(client,tableName + "x") //if table already exists just add a new name
        return
    }
    var status = "CREATING"
    while(status=="CREATING"){
      status = getTableStatus(client,tableName)
      Thread.sleep(1000)
    }
    if(status!="ACTIVE")
      throw new RuntimeException(s"Table status was $status")
  }

  def deleteTestTable(client:AmazonDynamoDB, tableName:String):Unit = {
    client.deleteTable(tableName)
  }

  protected def getClient(region:String):AmazonDynamoDB = {
    val chain = new AWSCredentialsProviderChain(
      new EnvironmentVariableCredentialsProvider(),
      new ProfileCredentialsProvider("multimedia"), //try named profile first, default if it does not work
      new ProfileCredentialsProvider(),
      new InstanceProfileCredentialsProvider()
    )

    AmazonDynamoDBClientBuilder.standard()
      .withCredentials(chain)
      .withRegion(region)
      .build()
  }

}
