# LittleHorse Connectors for Kafka Connect

## WfRunSinkConnector Configurations

``lhc.api.host``
  The bootstrap host for the LittleHorse Server.

  * Type: string
  * Importance: high

``lhc.api.port``
  The bootstrap port for the LittleHorse Server.

  * Type: int
  * Importance: high

``wf.spec.name``
  The name of the WfSpec to run.

  * Type: string
  * Importance: high

``lhc.api.protocol``
  The bootstrap protocol for the LittleHorse Server.

  * Type: string
  * Default: PLAINTEXT
  * Valid Values: [PLAINTEXT, TLS]
  * Importance: high

``lhc.tenant.id``
  Tenant ID which represents a logically isolated environment within LittleHorse.

  * Type: string
  * Default: default
  * Importance: medium

``lhc.ca.cert``
  Optional location of CA Cert file that issued the server side certificates. For TLS and mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.cert``
  Optional location of Client Cert file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.key``
  Optional location of Client Private Key file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.grpc.keepalive.time.ms``
  Time in milliseconds to configure keepalive pings on the grpc client.

  * Type: long
  * Default: 45000 (45 seconds)
  * Importance: low

``lhc.grpc.keepalive.timeout.ms``
  Time in milliseconds to configure the timeout for the keepalive pings on the grpc client.

  * Type: long
  * Default: 5000 (5 seconds)
  * Importance: low

``lhc.oauth.access.token.url``
  Optional Access Token URL provided by the OAuth Authorization Server. Used by the Worker to obtain a token using client credentials flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.id``
  Optional OAuth2 Client Id. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.secret``
  Optional OAuth2 Client Secret. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: password
  * Default: null
  * Importance: low

``wf.run.parent.id``
  Optionally specify the parent WfRunId

  * Type: string
  * Default: null
  * Importance: low

``wf.spec.major.version``
  Optionally specify the major version of the WfSpec to run. This guarantees that the "signature" of the WfSpec (i.e. the required input variables, and searchable variables) will not change for this app.

  * Type: int
  * Default: null
  * Importance: low

``wf.spec.revision``
  Optionally specify the specific revision of the WfSpec to run. It is not recommended to use this in practice, as the WfSpec logic should be de-coupled from the applications that run WfRun's.

  * Type: int
  * Default: null
  * Importance: low

## ExternalEventSinkConnector Configurations

``external.event.name``
  The name of the ExternalEventDef.

  * Type: string
  * Importance: high

``lhc.api.host``
  The bootstrap host for the LittleHorse Server.

  * Type: string
  * Importance: high

``lhc.api.port``
  The bootstrap port for the LittleHorse Server.

  * Type: int
  * Importance: high

``lhc.api.protocol``
  The bootstrap protocol for the LittleHorse Server.

  * Type: string
  * Default: PLAINTEXT
  * Valid Values: [PLAINTEXT, TLS]
  * Importance: high

``lhc.tenant.id``
  Tenant ID which represents a logically isolated environment within LittleHorse.

  * Type: string
  * Default: default
  * Importance: medium

``lhc.ca.cert``
  Optional location of CA Cert file that issued the server side certificates. For TLS and mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.cert``
  Optional location of Client Cert file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.key``
  Optional location of Client Private Key file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.grpc.keepalive.time.ms``
  Time in milliseconds to configure keepalive pings on the grpc client.

  * Type: long
  * Default: 45000 (45 seconds)
  * Importance: low

``lhc.grpc.keepalive.timeout.ms``
  Time in milliseconds to configure the timeout for the keepalive pings on the grpc client.

  * Type: long
  * Default: 5000 (5 seconds)
  * Importance: low

``lhc.oauth.access.token.url``
  Optional Access Token URL provided by the OAuth Authorization Server. Used by the Worker to obtain a token using client credentials flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.id``
  Optional OAuth2 Client Id. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.secret``
  Optional OAuth2 Client Secret. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: password
  * Default: null
  * Importance: low

## CorrelatedEventSinkConnector Configurations

``external.event.name``
  The name of the ExternalEventDef.

  * Type: string
  * Importance: high

``lhc.api.host``
  The bootstrap host for the LittleHorse Server.

  * Type: string
  * Importance: high

``lhc.api.port``
  The bootstrap port for the LittleHorse Server.

  * Type: int
  * Importance: high

``lhc.api.protocol``
  The bootstrap protocol for the LittleHorse Server.

  * Type: string
  * Default: PLAINTEXT
  * Valid Values: [PLAINTEXT, TLS]
  * Importance: high

``lhc.tenant.id``
  Tenant ID which represents a logically isolated environment within LittleHorse.

  * Type: string
  * Default: default
  * Importance: medium

``lhc.ca.cert``
  Optional location of CA Cert file that issued the server side certificates. For TLS and mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.cert``
  Optional location of Client Cert file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.client.key``
  Optional location of Client Private Key file for mTLS connection.

  * Type: string
  * Default: null
  * Importance: low

``lhc.grpc.keepalive.time.ms``
  Time in milliseconds to configure keepalive pings on the grpc client.

  * Type: long
  * Default: 45000 (45 seconds)
  * Importance: low

``lhc.grpc.keepalive.timeout.ms``
  Time in milliseconds to configure the timeout for the keepalive pings on the grpc client.

  * Type: long
  * Default: 5000 (5 seconds)
  * Importance: low

``lhc.oauth.access.token.url``
  Optional Access Token URL provided by the OAuth Authorization Server. Used by the Worker to obtain a token using client credentials flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.id``
  Optional OAuth2 Client Id. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: string
  * Default: null
  * Importance: low

``lhc.oauth.client.secret``
  Optional OAuth2 Client Secret. Used by the Worker to identify itself at an Authorization Server. Client Credentials Flow.

  * Type: password
  * Default: null
  * Importance: low

## FilterByFieldPredicate Configurations

``field``
  Field name.

  * Type: string
  * Importance: high

``pattern``
  Java regex pattern.

  * Type: string
  * Importance: high

``operation``
  Operation type, include or exclude message.

  * Type: string
  * Default: exclude
  * Valid Values: [include, exclude]
  * Importance: high
