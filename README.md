
<channel id="bookingConfirmationRequests"/>
<service-activator input-channel="bookingConfirmationRequests" output-channel="chargedBookings" ref="billForBookingService" />
<channel id="chargedBookings" />
<service-activator input-channel="chargedBookings" output-channel="emailConfirmationRequests" ref="seatAvailabilityService" />
<channel id="emailConfirmationRequests" />
<outbound-channel-adapter channel="emailConfirmationRequests" ref="emailConfirmationService" />

Dodanie queue do channel powoduje, że wątek wysyłający do emailConfirmationrequests nie jest blokowany i nie oczekuje na zakończenie wysyłki maila. 

<channel id="bookingConfirmationRequests"/>
<service-activator input-channel="bookingConfirmationRequests" output-channel="chargedBookings" ref="billForBookingService" />
<channel id="chargedBookings" />
<service-activator input-channel="chargedBookings" output-channel="emailConfirmationRequests" ref="seatAvailabilityService" />
<channel id="emailConfirmationRequests">
  <queue />
</channel>
<outbound-channel-adapter channel="emailConfirmationRequests" ref="emailConfirmationService" />

Dostarczenie wiadomości do więcej niż jednego konsumenta - wprowadzenie kanału publikuj-subskrybuj (publish-subscribe channel)
Kanał publish-subscribe nie wspiera kolejek.
Wspiera operacje asynchroniczne, jeżeli dostarczony zostanie egzekutor, dostarczający wiadomości od każdego subskrybenta w osobnym wątku.
To podejście może zablokować główny wątek wysyłający wiadomości przez kanał w przypadku gdy:
-task executor jest skonfigurowany do korzystania z wątku wywołującego
-kiedy pula wątku jest wyczerpana (chyba chodzi o pulę na wiadomości)

Czyli aby wysyłanie wiadomości potwierdzających nie blokowało wątku wysyłającego, ani całej puli przeznaczonej na task executora można wykorzystać 'bridge'

Bridge:
Wzorzec integracji. Wspiera połaczenie dwóch kanałow.
Dzięki temu można połączyć kanał typu publis-subscribe, aby dostarczał wiadomości do kolejki, a wątek od razu zwracał.

<channel id="bookingConfirmationRequests"/>
<service-activator input-channel="bookingConfirmationRequests" output-channel="chargedBookings" ref="billForBookingService" />
<channel id="chargedBookings" />
<service-activator input-channel="chargedBookings" output-channel="completedBookings" ref="seatAvailabilityService" />
<publish-subscribe-channel id="completedBookings" />
<bridge input-channel="completedBookings" output-channel="emailConfirmationRequests" />
<channel id="emailConfirmationRequests">
  <queue />
</channel>
<outbound-channel-adapter channel="emailConfirmationRequests" ref="emailConfirmationService" />

Rozumiem, teraz można podłączyć do kanału completedBookings wielu odbiorców? (może również za pomocą bridge?)

Priorytety:
<channel id="bookingConfirmationRequests">
  <priority-queue comparator="customerPriorityComparator" />
</channel>
<service-activator input-channel="bookingConfirmationRequests" output-channel="chargedBookings" ref="billForBookingService" />
<channel id="chargedBookings" />
<service-activator input-channel="chargedBookings" output-channel="completedBookings" ref="seatAvailabilityService" />
<publish-subscribe-channel id="completedBookings" />
<bridge input-channel="completedBookings" output-channel="emailConfirmationRequests" />
<channel id="emailConfirmationRequests">
<queue />
</channel>
<outbound-channel-adapter channel="emailConfirmationRequests" ref="emailConfirmationService" />

### MessageDispatcher
Dwa scenariusze:
-nadawanie (broadcast) - wszyscy odbiory otrzymuja wiadomość
-współzawodnictwo (competing) - jeden z odbiorców otrzymuje wiadomość

Strategia w jaki sposób kanał wysyła wiadomości jest zdefiniowana w interfejsie:
package org.springframework.integration.dispatcher;
public interface MessageDispatcher {
  boolean addHandler(MessageHandler handler);
  boolean removeHandler(MessageHandler handler);
  boolean dispatch(Message<?> message);
}

Dwie implementacje:
UnicastingDispatcher - do jednego handlera
BroadcastingDispatcher - do zero lub więcej handlerów

UnicastingDispatcher - dostarcza (w jaki sposób dostarcza???) dodatkowy interfejs strategii - LoadBalancingStrategy, który jest implementowany przez RoundRobinLoadBalancingStrategy (jedna wiadomość dla każdego w jednej turze).

package org.springframework.integration.dispatcher;
public interface LoadBalancingStrategy {
  public Iterator<MessageHandler> getHandlerIterator(Message<?> message, List<MessageHandler> handlers);
}

Przykład dispatchera:
```
public class ServiceLevelAgreementAwareMessageDispatcher
implements MessageDispatcher {
  private List<MessageHandler> highPriorityHandlers;
  private List<MessageHandler> lowPriorityHandlers;
  public boolean dispatch(Message<?> message){
    boolean highPriority = isHighPriority(message);
    boolean delivered = false;
    if(highPriority){
      delivered = attemptDelivery(highPriorityHandlers);
    }
    if(!delivered){
      delivered = attemptDelivery(lowPriorityHandlers);
    }
    return delivered;
  }
  ...
}
```

### 3.3.2 ChannelInterceptor

package org.springframework.integration.channel;
public interface ChannelInterceptor {
  Message<?> preSend(Message<?> message, MessageChannel channel);
  void postSend(Message<?> message, MessageChannel channel, boolean sent);
  boolean preReceive(MessageChannel channel);
  Message<?> postReceive(Message<?> message, MessageChannel channel);
}

- preSend is invoked before a message is sent and returns the message that will be
sent to the channel when the method returns. If the method returns null, noth-
ing is sent. This allows the implementation to control what gets sent to the
channel, effectively filtering the messages.
- postSend is invoked after an attempt to send the message has been made. It
indicates whether the attempt was successful through the boolean flag it passes
as an argument. This allows the implementation to monitor the message flow
and learn which messages are sent and which ones fail.
- preReceive applies only if the channel is pollable. It’s invoked when a compo-
nent calls receive() on the channel, but before a Message is actually read from
that channel. It allows implementers to decide whether the channel can return
a message to the caller.
- postReceive , like preReceive , applies only to pollable channels. It’s invoked
after a message is read from a channel but before it’s returned to the component
that called receive() . If it returns null, then no message is received. This allows
the implementer to control what, if anything, is actually received by the poller.

Przykład implementacji:
package siia.channels;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
public class ChannelAuditor extends ChannelInterceptorAdapter {
  private AuditService auditService;
  public void setAuditService(AuditService auditService) {
    this.auditService = auditService;
  }
  public Message<?> preSend(Message<?> message,
  MessageChannel channel) {
    this.auditService.audit(message.getPayload());
    return message;
  }
}


Konfiguracja interceptora:

<beans:bean id="auditInterceptor" class="siia.channels.ChannelAuditor">
  <beans:property name="auditService" ref="auditService"/>
</beans:bean>

<beans:bean id="auditService" class="siia.channels.AuditService"/>
  <channel id="chargedBookings">
  ....
  <interceptors>
    <beans:ref bean="auditInterceptor"/>
  </interceptors>
</channel>

#### WireTap
Przesyła kopie wiadomości do osobnego kanału (w celu monitoringu?)
<channel id="monitoringChannel/>
<channel id="chargedBookings">
  ....
  <interceptors>
    <wire-tap channel="monitoringChannel"/>
  </interceptors>
</channel>

#### Filtering - MessageSelectingInterceptor
MessageSelectingInterceptor - czy wiadomość jest akceptowana.

public interface MessageSelector {
  boolean accept(Message<?> message);
}

Framework dostarcza PyloadTypeSelector, który implementuje MessageSelector.

Połączenie MessageSelectingInterceptor i MessageSelector daje wzorzec projektowy Datatype Channel tylko wiadomości określonego typu mogą być przesyłane przez kanał.
<beans:bean id="typeSelector" class="org.springframework.integration.selector.PayloadTypeSelector">
  <beans:constructor-arg value="siia.channels.ChargedBooking" />
</beans:bean>
<beans:bean" id="typeSelectingInterceptor" class="org.springframework.integration.channel.interceptor.MessageSelectingInterceptor">
  <beans:constructor-arg ref="typeSelector"/>
</beans:bean>
<channel id="chargedBookings">
...
  <interceptors>
    <ref bean="typeSelectingInterceptor"/>
  </interceptors>
</channel>






# MessageEndpoints
Channel types:
1 subscribers, event driven, synchronous invocation of a handler, transaction spans both producer and consumer
2 requires polling consumer, loosly coupled interaction - break transaction boundary across separate threads for producer and sender(? - not consumer?)

MessageHandler:
package org.springframework.integration.core;
public interface MessageHandler {
  void handleMessage(Message<?> message);
}

All message handling components (transformers, splitters, routers) in spring implements MessageHandler interface
Implementations are reacting to a received message (same way as JMS - Java Message Service - MessageListener).

Those message handlers can be connected to any channel.
To connect message handler, adapter (message endpoint) that understand how to interact with given channel, is needed


Channel subscribable - handler invoked within sender thread, or by the channel's TaskExecutor
Channel buffers message in a queue (ex. QueueChannel) - polling is necessary

Is Reply required?
Is Reply send back to caller or passed to the next component within a linear pipe?
Component - unidirectional (channel adapter) or bidirectional (gateway)

Characteristics of channels:
Polling or event-driven
Inbound or outbound
Unidirectional or bidirectional
Internal or external

In combination - 16 names for endpoints. Some of them are redundant:

<inbound-channel-adapter> Polling Inbound Unidirectional Internal
<outbound-channel-adapter> Either Outbound Unidirectional Internal
<gateway> Event-driven Inbound Bidirectional Internal
<service-activator> Either Outbound Bidirectional Internal
<http:outbound-gateway> Either Outbound Bidirectional External
<amqp:inbound-channel-adapter> Event-driven Inbound Unidirectional External

### 4.1.1. Poll or not to poll
#### Polling endpoint
Polling endpoint will actively request new data to process.
The endpoint needs at least single thread to perform polling.
Handcode example - infinite loop with receive() method.

#### Event driven (passive) endpoints
Are used when asynchronous handoff isn't required.
Don't take responsibility for thread management.

### 4.1.2 - Inbound endpoints
#### polling inbound endpoints:
When external system is passive.
In Spring Integration, several polling endpoints are provided: inbound channel
adapters for files, JMS , email, and a generic method-invoking variant.

#### event-driven inbound endpoints
When external system is active.
In Spring Integration: web services (through spring ws), RMI, JMS (where spring's listener container takes care of polling ???? - nie czaje)
Custom event-driven endpoint using @Gateway annotation.


### 4.1.3 - Outbound endpoints
#### polling outbound endpoints
Endpoint connected to PollableChannel. Must invoke receive method (step 1 is triggered by endpoint).

#### event-driven outbound endpoints
Endpoint connected to SubscribableChannel can be passive. 
The channel ensures that a thread exists to invoke the endpoint when a message
arrives. In many cases, the thread that invoked the send method on the channel is
used, but it could also be handled by a thread pool managed at the channel level.
The advantage of using a thread pool is that the sender doesn’t have to wait, but the disad-
vantage is that a transactional boundary would be broken because the transaction con-
text is associated with the thread.


### 4.1.4 - Unidirectional and bidirectional endpoint


## 4.2 - Transactions 
The transaction boundary is broken as soon as you add a task executor or a queue to a channel.




















