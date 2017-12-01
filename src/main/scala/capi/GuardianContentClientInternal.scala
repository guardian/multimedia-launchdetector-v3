package capi

import com.gu.contentapi.client.GuardianContentClient

/* override the client to hit the internal api endpoints, only possible from within GU networks */
class GuardianContentClientInternal(apiKey:String) extends GuardianContentClient(apiKey:String) {
  override val targetUrl: String = "https://internal.content.guardianapis.com"
}
